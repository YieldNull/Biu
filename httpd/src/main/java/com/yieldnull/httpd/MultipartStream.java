package com.yieldnull.httpd;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import static java.lang.String.format;


/**
 * 读取Http Request Body
 */
class MultipartStream {

    private static final Log LOGGER = Log.of(MultipartStream.class);

    // ASCII character value.
    public static final byte CR = 0x0D;     // \n
    public static final byte LF = 0x0A;     // \r
    public static final byte DASH = 0x2D;   // -


    /**
     * Multipart Header 的最大长度
     */
    public static final int HEADER_PART_SIZE_MAX = 10240;


    /**
     * 默认缓冲区大小
     */
    public static final int DEFAULT_BUFSIZE = 4096;


    /**
     * MultipartEntity Header 与 Body 的分隔符
     */
    public static final byte[] HEADER_SEPARATOR = {CR, LF, CR, LF};


    /**
     * boundary之后，另一段Entity之前
     */
    public static final byte[] FIELD_SEPARATOR = {CR, LF};


    /**
     * 加在boundary之后，表示MultiPart结束
     */
    public static final byte[] STREAM_TERMINATOR = {DASH, DASH};


    /**
     * 加在boundary之前，表示Entity开始，包含上一行的换行符
     */
    public static final byte[] BOUNDARY_PREFIX = {CR, LF, DASH, DASH};


    /**
     * 数据流
     */
    private final InputStream input;


    /**
     * “CRLF--boundary”
     */
    private byte[] delimiter;


    /**
     * length of "CRLF--boundary"
     */
    private int delimiterLength;


    /**
     * The amount of data, in bytes, that must be kept in the buffer in order
     * to detect delimiters reliably.
     */
    private int keepRegion; // == boundaryLength


    /**
     * 缓冲区
     */
    private final int bufSize;
    private final byte[] buffer;

    /**
     * The index of first valid character in the buffer.
     * <br>
     * 0 <= head < bufSize
     */
    private int head;

    /**
     * The index of last valid character in the buffer + 1.
     * <br>
     * 0 <= tail <= bufSize
     */
    private int tail;


    /**
     * Entity Header编码
     */
    private String headerEncoding;


    /**
     * Specifies the character getEncoding to be used when reading the getHeaders of
     * individual parts. When not specified, or <code>null</code>, the platform
     * default getEncoding is used.
     *
     * @param encoding The getEncoding used to read part getHeaders.
     */
    public void setHeaderEncoding(String encoding) {
        headerEncoding = encoding;
    }


    public MultipartStream(InputStream pInputStream, byte[] pBoundary) {
        head = 0;
        tail = 0;

        input = pInputStream;
        bufSize = DEFAULT_BUFSIZE;
        buffer = new byte[bufSize];

        delimiterLength = BOUNDARY_PREFIX.length + pBoundary.length;
        delimiter = new byte[delimiterLength];
        keepRegion = delimiter.length;

        // 把 “CRLF--boundary” 写入 delimiter
        System.arraycopy(BOUNDARY_PREFIX, 0, delimiter, 0, BOUNDARY_PREFIX.length);
        System.arraycopy(pBoundary, 0, delimiter, BOUNDARY_PREFIX.length, pBoundary.length);
    }


    /**
     * 寻找第一个Entity.
     *
     * @return 是否找到
     */
    public boolean skipPreamble() {

        // 没有Preamble的话，delimiter前面没有换行符
        System.arraycopy(delimiter, 2, delimiter, 0, delimiter.length - 2);
        delimiterLength = delimiter.length - 2;

        try {
            // Discard all data up to the delimiter.
            discardBodyData();

            // Read boundary - if succeeded, the getStream contains an
            // encapsulation.
            return readBoundary();
        } catch (IOException e) {
            LOGGER.w(e.getMessage(), e);
            return false;
        } finally {
            // Restore delimiter.
            System.arraycopy(delimiter, 0, delimiter, 2, delimiter.length - 2);
            delimiterLength = delimiter.length;
            delimiter[0] = CR;
            delimiter[1] = LF;
        }
    }


    /**
     * Discard all data up to the delimiter
     *
     * @return The amount of data discarded.
     * @throws IOException if an i/o error occurs.
     */
    public int discardBodyData() throws IOException {
        return readBodyData(null);
    }


    /**
     * 将Entity Body 写入到输出流
     *
     * @param output 可以为空，表示舍弃之，相当于{@link #discardBodyData()}
     * @return 读取总量
     * @throws IOException
     */
    public int readBodyData(OutputStream output) throws IOException {
        final InputStream inputStream = newEntityStream();
        return (int) Streams.copy(inputStream, output, false, null);
    }


    /**
     * 读取boundary，并检查之后是否还有Entity
     *
     * @return 是否还有
     * @throws IOException
     */
    public boolean readBoundary() throws IOException {

        byte[] marker = new byte[2];
        boolean nextChunk;

        head += delimiterLength;
        try {
            marker[0] = readByte();
            if (marker[0] == LF) {
                // Work around IE5 Mac bug with input type=image.
                // Because the boundary delimiter, not including the trailing
                // CRLF, must not appear within any file (RFC 2046, section
                // 5.1.1), we know the missing CR is due to a buggy browser
                // rather than a file containing something similar to a
                // boundary.
                return true;
            }

            marker[1] = readByte();
            if (arrayEquals(marker, STREAM_TERMINATOR, 2)) {
                nextChunk = false;
            } else if (arrayEquals(marker, FIELD_SEPARATOR, 2)) {
                nextChunk = true;
            } else {
                throw new IOException("Unexpected characters follow a boundary");
            }
        } catch (IOException e) {
            throw new IOException("Stream ended unexpectedly");
        }
        return nextChunk;
    }


    /**
     * 设置boundary
     * <p/>
     * 由于multipart可以嵌套，因此有更换boundary的需求。
     * 内外两层的boundary长度需要一致，且处理完内层之后要将boundary恢复为外层
     *
     * @param boundary 内层boundary
     */
    public void setBoundary(byte[] boundary) throws IOException {
        if (boundary.length != delimiterLength - BOUNDARY_PREFIX.length) {
            throw new IOException("The length of a boundary token can not be changed");
        }
        System.arraycopy(boundary, 0, this.delimiter, BOUNDARY_PREFIX.length,
                boundary.length);
    }


    /**
     * 读取MultipartEntity的header
     *
     * @return 读取到的header部分
     * @throws IOException
     */
    public String readHeaders() throws IOException {
        int i = 0;
        byte b;

        // to support multi-byte characters
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int size = 0;
        while (i < HEADER_SEPARATOR.length) {
            b = readByte();

            if (++size > HEADER_PART_SIZE_MAX) {
                throw new IOException(
                        format("Header section has more than %s bytes (maybe it is not properly terminated)",
                                HEADER_PART_SIZE_MAX));
            }

            if (b == HEADER_SEPARATOR[i]) {
                i++;
            } else {
                i = 0;
            }

            baos.write(b);
        }

        String headers;
        if (headerEncoding != null) {
            try {
                headers = baos.toString(headerEncoding);
            } catch (UnsupportedEncodingException e) {
                // Fall back to platform default if specified getEncoding is not
                // supported.
                headers = baos.toString();
            }
        } else {
            headers = baos.toString();
        }

        return headers;
    }


    /**
     * 从buffer读取一个字节，必要时从输入源将缓冲区填满
     *
     * @return 下一个字节
     * @throws IOException EOF
     */
    public byte readByte() throws IOException {
        // Buffer depleted ?
        if (head == tail) {
            head = 0;
            // Refill.
            tail = input.read(buffer, head, bufSize);
            if (tail == -1) {
                // No more data available.
                throw new IOException("No more data is available");
            }
        }
        return buffer[head++];
    }


    /**
     * 获取 Entity body 数据流
     *
     * @return 输入流
     */
    ItemInputStream newEntityStream() {
        return new ItemInputStream();
    }


    /**
     * 比较两个数组的前<code>count</code>个字节是否相等
     *
     * @param a     array1
     * @param b     array2
     * @param count 前几个字节需要参与比较
     * @return 是否相等
     */
    public static boolean arrayEquals(byte[] a, byte[] b, int count) {
        for (int i = 0; i < count; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }


    /**
     * 在buf的有效区域[head,tail]内，寻找到delimiter的第一个字符在buf中的位置
     *
     * @return 找不到则返回-1
     */
    protected int findDelimiter() {
        int first;
        int match = 0;
        int maxPos = tail - delimiterLength; //delimiter第一个字符的最大位置，不然buffer就爆了

        for (first = head; first <= maxPos && match != delimiterLength; first++) {
            first = findByte(delimiter[0], first); // 先找到delimiter的第一个字符
            if (first == -1 || first > maxPos) {
                return -1;
            }

            // 然后匹配剩下的字符
            // 要是能完全匹配，则match==delimiterLength
            // 否则(部分匹配),跳出内循环，继续外循环
            for (match = 1; match < delimiterLength; match++) {
                if (buffer[first + match] != delimiter[match]) {
                    break;
                }
            }
        }

        if (match == delimiterLength) {
            return first - 1; // 跳出循环时自增了，所以要-1
        }
        return -1;
    }


    /**
     * 在buffer中从position处开始查找指定字节
     *
     * @param value 指定字节
     * @param pos   开始位置
     * @return 若找到，为其在buffer中的位置。否则为-1
     */
    protected int findByte(byte value, int pos) {
        for (int i = pos; i < tail; i++) {
            if (buffer[i] == value) {
                return i;
            }
        }
        return -1;
    }


    /**
     * 读取整个Multipart，将各个Entity连起来读
     */
    public class ItemInputStream extends InputStream implements Closeable {

        /**
         * The number of bytes, which must be hold, because
         * they might be a part of the boundary.
         */
        private int pad;


        /**
         * 当前Entity delimiter 在buffer中的位置，不存在于buffer中则为-1
         */
        private int end;


        /**
         * Whether the getStream is already closed.
         */
        private boolean closed;


        /**
         * Offset when converting negative bytes to integers.
         */
        private static final int BYTE_POSITIVE_OFFSET = 256;


        /**
         * Creates a new instance.
         */
        public ItemInputStream() {
            findDelimiter();
        }


        /**
         * 寻找下一个 delimiter
         */
        private void findDelimiter() {
            end = MultipartStream.this.findDelimiter();
            if (end == -1) {
                if (tail - head > keepRegion) {
                    pad = keepRegion;
                } else {
                    pad = tail - head;
                }
            }
        }


        /**
         * 返回输入流中的下一个byte,非负代表正常，-1代表EOF
         */
        @Override
        public int read() throws IOException {

            if (available() == 0 && makeAvailable() == 0) {
                return -1;

            }

            int b = buffer[head++];
            if (b >= 0) {
                return b;
            }
            return b + BYTE_POSITIVE_OFFSET;
        }


        @SuppressWarnings("NullableProblems")
        @Override
        public int read(byte[] buffer, int off, int len) throws IOException {
            if (len == 0) {
                return 0;
            }
            int res = available();
            if (res == 0) {
                res = makeAvailable();
                if (res == 0) {
                    return -1;
                }
            }
            res = Math.min(res, len);
            System.arraycopy(MultipartStream.this.buffer, head, buffer, off, res);
            head += res;
            return res;
        }


        @Override
        public long skip(long bytes) throws IOException {
            int av = available();
            if (av == 0) {
                av = makeAvailable();
                if (av == 0) {
                    return 0;
                }
            }
            long res = Math.min(av, bytes);
            head += res;
            return res;
        }

        @Override
        public int available() throws IOException {
            if (end == -1) {
                return tail - head - pad;
            }
            return end - head;
        }


        /**
         * Closes the input getStream.
         *
         * @throws IOException An I/O error occurred.
         */
        @Override
        public void close() throws IOException {
            close(false);
        }


        /**
         * Closes the input getStream.
         *
         * @param pCloseUnderlying Whether to close the underlying getStream
         *                         (hard close)
         * @throws IOException An I/O error occurred.
         */
        @SuppressWarnings("ResultOfMethodCallIgnored")
        public void close(boolean pCloseUnderlying) throws IOException {
            if (closed) {
                return;
            }
            if (pCloseUnderlying) {
                closed = true;
                input.close();
            } else {
                while (true) {
                    int av = available();
                    if (av == 0) {
                        av = makeAvailable();
                        if (av == 0) {
                            break;
                        }
                    }
                    skip(av);
                }
            }
            closed = true;
        }


        /**
         * Attempts to read more data.
         *
         * @return Number of available bytes
         * @throws IOException An I/O error occurred.
         */
        private int makeAvailable() throws IOException {
            if (end != -1) {
                return 0;
            }

            // Move the data to the beginning of the buffer.
            System.arraycopy(buffer, tail - pad, buffer, 0, pad);

            // Refill buffer with new data.
            head = 0;
            tail = pad;

            while (true) {
                int bytesRead = input.read(buffer, tail, bufSize - tail);
                if (bytesRead == -1) {
                    // The last pad amount is left in the buffer.
                    // Boundary can't be in there so signal an error
                    // condition.
                    throw new IOException("Stream ended unexpectedly");
                }
                tail += bytesRead;

                findDelimiter();
                int av = available();

                if (av > 0 || end != -1) {
                    return av;
                }
            }
        }
    }

}

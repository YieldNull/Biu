package com.bbbbiu.biu.lib.httpd.upload;

import com.bbbbiu.biu.lib.httpd.upload.exceptions.FileUploadIOException;
import com.bbbbiu.biu.lib.httpd.upload.interfaces.FileItemStream;
import com.bbbbiu.biu.lib.httpd.upload.interfaces.Closeable;
import com.bbbbiu.biu.lib.util.ProgressNotifier;
import com.bbbbiu.biu.lib.util.Streams;

import static java.lang.String.format;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class MultipartStream {

    // ASCII character value.
    public static final byte CR = 0x0D;     // \n
    public static final byte LF = 0x0A;     // \r
    public static final byte DASH = 0x2D;   // -

    /**
     * The maximum length of <code>header-part</code> that will be
     * processed (10 kilobytes = 10240 bytes.).
     */
    public static final int HEADER_PART_SIZE_MAX = 10240;

    public static final int DEFAULT_BUFSIZE = 4096;


    public static final byte[] HEADER_SEPARATOR = {CR, LF, CR, LF};

    public static final byte[] FIELD_SEPARATOR = {CR, LF};


    // 跟在最后一个文件段boundary 后面的 结束字符 --
    public static final byte[] STREAM_TERMINATOR = {DASH, DASH};

    // boundary之前的字符 CRLF--
    public static final byte[] BOUNDARY_PREFIX = {CR, LF, DASH, DASH};


    private final InputStream input;


    private int boundaryLength; // length of "CRLF--boundary"
    private byte[] boundary;    // “CRLF--boundary”


    /**
     * The amount of data, in bytes, that must be kept in the buffer in order
     * to detect delimiters reliably.
     */
    private int keepRegion; // == boundaryLength


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

    private String headerEncoding;

    private final ProgressNotifier notifier;


    /**
     * Retrieves the character encoding used when reading the headers of an
     * individual part. When not specified, or <code>null</code>, the platform
     * default encoding is used.
     *
     * @return The encoding used to read part headers.
     */
    public String getHeaderEncoding() {
        return headerEncoding;
    }

    /**
     * Specifies the character encoding to be used when reading the headers of
     * individual parts. When not specified, or <code>null</code>, the platform
     * default encoding is used.
     *
     * @param encoding The encoding used to read part headers.
     */
    public void setHeaderEncoding(String encoding) {
        headerEncoding = encoding;
    }


    public MultipartStream(InputStream input,
                           byte[] boundary,
                           ProgressNotifier pNotifier) {

        this.input = input;
        this.bufSize = DEFAULT_BUFSIZE;
        this.buffer = new byte[bufSize];
        this.notifier = pNotifier;

        // We prepend CR/LF to the boundary to chop trailing CR/LF from
        // body-data tokens.
        this.boundaryLength = BOUNDARY_PREFIX.length + boundary.length;
        this.boundary = new byte[this.boundaryLength];
        this.keepRegion = this.boundary.length;


        // 把 “CRLF--boundary” 写入 this.boundary
        System.arraycopy(BOUNDARY_PREFIX, 0, this.boundary, 0,
                BOUNDARY_PREFIX.length);
        System.arraycopy(boundary, 0, this.boundary, BOUNDARY_PREFIX.length,
                boundary.length);

        head = 0;
        tail = 0;
    }


    /**
     * Finds the beginning of the first <code>encapsulation</code>.
     *
     * @return <code>true</code> if an <code>encapsulation</code> was found in
     * the stream.
     * @throws IOException if an i/o error occurs.
     */
    public boolean skipPreamble() throws IOException {
        // First delimiter may be not preceeded with a CRLF.
        System.arraycopy(boundary, 2, boundary, 0, boundary.length - 2);
        boundaryLength = boundary.length - 2;
        try {
            // Discard all data up to the delimiter.
            discardBodyData();

            // Read boundary - if succeeded, the stream contains an
            // encapsulation.
            return readBoundary();
        } catch (MalformedStreamException e) {
            return false;
        } finally {
            // Restore delimiter.
            System.arraycopy(boundary, 0, boundary, 2, boundary.length - 2);
            boundaryLength = boundary.length;
            boundary[0] = CR;
            boundary[1] = LF;
        }
    }

    /**
     * <p> Reads <code>body-data</code> from the current
     * <code>encapsulation</code> and discards it.
     * <p>
     * <p>Use this method to skip encapsulations you don't need or don't
     * understand.
     *
     * @return The amount of data discarded.
     * @throws MalformedStreamException if the stream ends unexpectedly.
     * @throws IOException              if an i/o error occurs.
     */
    public int discardBodyData() throws IOException {
        return readBodyData(null);
    }


    /**
     * <p>Reads <code>body-data</code> from the current
     * <code>encapsulation</code> and writes its contents into the
     * output <code>Stream</code>.
     * <p>
     * <p>Arbitrary large amounts of data can be processed by this
     * method using a constant size buffer. (see {@link
     * ProgressNotifier) constructor}).
     *
     * @param output The <code>Stream</code> to write data into. May
     *               be null, in which case this method is equivalent
     *               to {@link #discardBodyData()}.
     * @return the amount of data written.
     * @throws MalformedStreamException if the stream ends unexpectedly.
     * @throws IOException              if an i/o error occurs.
     */
    public int readBodyData(OutputStream output) throws IOException {
        final InputStream inputStream = newInputStream();
        return (int) Streams.copy(inputStream, output, false, null);
    }


    /**
     * Skips a <code>boundary</code> token, and checks whether more
     * <code>encapsulations</code> are contained in the stream.
     *
     * @return <code>true</code> if there are more encapsulations in
     * this stream; <code>false</code> otherwise.
     * <p>
     * \\@throws FileUploadIOException if the bytes read from the stream exceeded the size limits
     * @throws MalformedStreamException if the stream ends unexpectedly or
     *                                  fails to follow required syntax.
     */
    public boolean readBoundary()
            throws FileUploadIOException, MalformedStreamException {
        byte[] marker = new byte[2];
        boolean nextChunk = false;

        head += boundaryLength;
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
                throw new MalformedStreamException(
                        "Unexpected characters follow a boundary");
            }
        } catch (FileUploadIOException e) {
            // wraps a SizeException, re-throw as it will be unwrapped later
            throw e;
        } catch (IOException e) {
            throw new MalformedStreamException("Stream ended unexpectedly");
        }
        return nextChunk;
    }

    /**
     * <p>Changes the boundary token used for partitioning the stream.
     * <p>
     * <p>This method allows single pass processing of nested multipart
     * streams.
     * <p>
     * <p>The boundary token of the nested stream is <code>required</code>
     * to be of the same length as the boundary token in parent stream.
     * <p>
     * <p>Restoring the parent stream boundary token after processing of a
     * nested stream is left to the application.
     *
     * @param boundary The boundary to be used for parsing of the nested
     *                 stream.
     * @throws IllegalBoundaryException if the <code>boundary</code>
     *                                  has a different length than the one
     *                                  being currently parsed.
     */
    public void setBoundary(byte[] boundary)
            throws IllegalBoundaryException {
        if (boundary.length != boundaryLength - BOUNDARY_PREFIX.length) {
            throw new IllegalBoundaryException(
                    "The length of a boundary token can not be changed");
        }
        System.arraycopy(boundary, 0, this.boundary, BOUNDARY_PREFIX.length,
                boundary.length);
    }

    /**
     * <p>Reads the <code>header-part</code> of the current
     * <code>encapsulation</code>.
     * <p>
     * <p>Headers are returned verbatim to the input stream, including the
     * trailing <code>CRLF</code> marker. Parsing is left to the
     * application.
     * <p>
     * <p><strong>TODO</strong> allow limiting maximum header size to
     * protect against abuse.
     *
     * @return The <code>header-part</code> of the current encapsulation.
     * <p>
     * \\\@throws MalformedStreamException if the stream ends unexpectedly.
     */
    public String readHeaders() throws FileUploadIOException, MalformedStreamException {
        int i = 0;
        byte b;
        // to support multi-byte characters
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int size = 0;
        while (i < HEADER_SEPARATOR.length) {
            try {
                b = readByte();
            } catch (FileUploadIOException e) {
                // wraps a SizeException, re-throw as it will be unwrapped later
                throw e;
            } catch (IOException e) {
                throw new MalformedStreamException("Stream ended unexpectedly");
            }
            if (++size > HEADER_PART_SIZE_MAX) {
                throw new MalformedStreamException(
                        format("Header section has more than %s bytes (maybe it is not properly terminated)",
                                Integer.valueOf(HEADER_PART_SIZE_MAX)));
            }
            if (b == HEADER_SEPARATOR[i]) {
                i++;
            } else {
                i = 0;
            }
            baos.write(b);
        }

        String headers = null;
        if (headerEncoding != null) {
            try {
                headers = baos.toString(headerEncoding);
            } catch (UnsupportedEncodingException e) {
                // Fall back to platform default if specified encoding is not
                // supported.
                headers = baos.toString();
            }
        } else {
            headers = baos.toString();
        }

        return headers;
    }


    /**
     * Reads a byte from the <code>buffer</code>, and refills it as
     * necessary.
     *
     * @return The next byte from the input stream.
     * @throws IOException if there is no more data available.
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
            if (notifier != null) {
                notifier.noteBytesRead(tail);
            }
        }
        return buffer[head++];
    }


    ItemInputStream newInputStream() {
        return new ItemInputStream();
    }


    /**
     * Compares <code>count</code> first bytes in the arrays
     * <code>a</code> and <code>b</code>.
     *
     * @param a     The first array to compare.
     * @param b     The second array to compare.
     * @param count How many bytes should be compared.
     * @return <code>true</code> if <code>count</code> first bytes in arrays
     * <code>a</code> and <code>b</code> are equal.
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
     * 在buf的有效区域[head,tail]内，寻找到boundary的第一个字符在buf中的位置
     *
     * @return 找不到则返回-1
     */
    protected int findSeparator() {
        int first;
        int match = 0;
        int maxpos = tail - boundaryLength; // boundary第一个字符的最大位置

        for (first = head; first <= maxpos && match != boundaryLength; first++) {
            first = findByte(boundary[0], first); // 先找到boundary的第一个字符
            if (first == -1 || first > maxpos) {
                return -1;
            }

            // 然后匹配剩下的字符
            // 要是能完全匹配，则match==boundaryLength
            // 否则(部分匹配),跳出内循环，继续外循环
            for (match = 1; match < boundaryLength; match++) {
                if (buffer[first + match] != boundary[match]) {
                    break;
                }
            }
        }

        if (match == boundaryLength) {
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


    public class ItemInputStream extends InputStream implements Closeable {

        private long total; // 已读byte数

        /**
         * The number of bytes, which must be hold, because
         * they might be a part of the boundary.
         */
        private int pad;

        /**
         * The current offset in the buffer.
         */
        private int pos;

        /**
         * Whether the stream is already closed.
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
            findSeparator();
        }

        public boolean isClosed() {
            return closed;
        }


        /**
         * Called for finding the separator.
         */
        private void findSeparator() {
            pos = MultipartStream.this.findSeparator();
            if (pos == -1) {
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
            if (closed) {
                throw new FileItemStream.ItemSkippedException();
            }

            if (available() == 0 && makeAvailable() == 0) {
                return -1;

            }
            ++total;
            int b = buffer[head++];
            if (b >= 0) {
                return b;
            }
            return b + BYTE_POSITIVE_OFFSET;
        }

        /**
         * Reads bytes into the given buffer.
         *
         * @param b   The destination buffer, where to write to.
         * @param off Offset of the first byte in the buffer.
         * @param len Maximum number of bytes to read.
         * @return Number of bytes, which have been actually read,
         * or -1 for EOF.
         * @throws IOException An I/O error occurred.
         */
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (closed) {
                throw new FileItemStream.ItemSkippedException();
            }
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
            System.arraycopy(buffer, head, b, off, res);
            head += res;
            total += res;
            return res;
        }

        /**
         * Closes the input stream.
         *
         * @throws IOException An I/O error occurred.
         */
        @Override
        public void close() throws IOException {
            close(false);
        }

        /**
         * Closes the input stream.
         *
         * @param pCloseUnderlying Whether to close the underlying stream
         *                         (hard close)
         * @throws IOException An I/O error occurred.
         */
        public void close(boolean pCloseUnderlying) throws IOException {
            if (closed) {
                return;
            }
            if (pCloseUnderlying) {
                closed = true;
                input.close();
            } else {
                for (; ; ) {
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
         * Skips the given number of bytes.
         *
         * @param bytes Number of bytes to skip.
         * @return The number of bytes, which have actually been
         * skipped.
         * @throws IOException An I/O error occurred.
         */
        @Override
        public long skip(long bytes) throws IOException {
            if (closed) {
                throw new FileItemStream.ItemSkippedException();
            }
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
            if (pos == -1) {
                return tail - head - pad;
            }
            return pos - head;
        }


        /**
         * Attempts to read more data.
         *
         * @return Number of available bytes
         * @throws IOException An I/O error occurred.
         */
        private int makeAvailable() throws IOException {
            if (pos != -1) {
                return 0;
            }

            // Move the data to the beginning of the buffer.
            total += tail - head - pad;
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
                    final String msg = "Stream ended unexpectedly";
                    throw new MalformedStreamException(msg);
                }
                if (notifier != null) {
                    notifier.noteBytesRead(bytesRead);
                }
                tail += bytesRead;

                findSeparator();
                int av = available();

                if (av > 0 || pos != -1) {
                    return av;
                }
            }
        }
    }

    /**
     * Thrown to indicate that the input stream fails to follow the
     * required syntax.
     */
    public static class MalformedStreamException extends IOException {

        /**
         * The UID to use when serializing this instance.
         */
        private static final long serialVersionUID = 6466926458059796677L;

        /**
         * Constructs a <code>MalformedStreamException</code> with no
         * detail message.
         */
        public MalformedStreamException() {
            super();
        }

        /**
         * Constructs an <code>MalformedStreamException</code> with
         * the specified detail message.
         *
         * @param message The detail message.
         */
        public MalformedStreamException(String message) {
            super(message);
        }

    }

    /**
     * Thrown upon attempt of setting an invalid boundary token.
     */
    public static class IllegalBoundaryException extends IOException {

        /**
         * The UID to use when serializing this instance.
         */
        private static final long serialVersionUID = -161533165102632918L;

        /**
         * Constructs an <code>IllegalBoundaryException</code> with no
         * detail message.
         */
        public IllegalBoundaryException() {
            super();
        }

        /**
         * Constructs an <code>IllegalBoundaryException</code> with
         * the specified detail message.
         *
         * @param message The detail message.
         */
        public IllegalBoundaryException(String message) {
            super(message);
        }

    }
}

package com.bbbbiu.biu.server;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.GZIPOutputStream;

public class HttpResponse implements Closeable {

    private static final String TAG = HttpResponse.class.getSimpleName();


    /**
     * 返回状态码
     */
    public enum Status {
        SWITCH_PROTOCOL(101, "Switching Protocols"),
        OK(200, "OK"),
        CREATED(201, "Created"),
        ACCEPTED(202, "Accepted"),
        NO_CONTENT(204, "No Content"),
        PARTIAL_CONTENT(206, "Partial Content"),
        MULTI_STATUS(207, "Multi-Status"),
        REDIRECT(301, "Moved Permanently"),
        REDIRECT_SEE_OTHER(303, "See Other"),
        NOT_MODIFIED(304, "Not Modified"),
        BAD_REQUEST(400, "Bad Request"),
        UNAUTHORIZED(401, "Unauthorized"),
        FORBIDDEN(403, "Forbidden"),
        NOT_FOUND(404, "Not Found"),
        METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
        NOT_ACCEPTABLE(406, "Not Acceptable"),
        REQUEST_TIMEOUT(408, "Request Timeout"),
        CONFLICT(409, "Conflict"),
        RANGE_NOT_SATISFIABLE(416, "Requested Range Not Satisfiable"),
        INTERNAL_ERROR(500, "Internal Server Error"),
        NOT_IMPLEMENTED(501, "Not Implemented"),
        UNSUPPORTED_HTTP_VERSION(505, "HTTP Version Not Supported");

        private final int requestStatus;

        private final String description;

        Status(int requestStatus, String description) {
            this.requestStatus = requestStatus;
            this.description = description;
        }

        public String getDescription() {
            return "" + this.requestStatus + " " + this.description;
        }
    }


    private Status status;
    private String mimeType;

    private InputStream data;
    private long contentLength;

    /**
     * 返回头
     */
    private final Map<String, String> header = new HashMap<String, String>() {

        public String put(String key, String value) {
            lowerCaseHeader.put(key == null ? null : key.toLowerCase(), value);
            return super.put(key, value);
        }

    };

    /**
     * 返回头-小写形式
     */
    private final Map<String, String> lowerCaseHeader = new HashMap<>();

    private HttpRequest.Method requestMethod;

    private boolean chunkedTransfer;
    private boolean encodeAsGzip;
    private boolean keepAlive;


    public InputStream getData() {
        return this.data;
    }

    public String getHeader(String name) {
        return this.lowerCaseHeader.get(name.toLowerCase());
    }

    public String getMimeType() {
        return this.mimeType;
    }

    public void setGzipEncoding(boolean encodeAsGzip) {
        this.encodeAsGzip = encodeAsGzip;
    }

    public void setKeepAlive(boolean useKeepAlive) {
        this.keepAlive = useKeepAlive;
    }

    public void setChunkedTransfer(boolean chunkedTransfer) {
        this.chunkedTransfer = chunkedTransfer;
    }

    public void setData(InputStream data) {
        this.data = data;
    }

    public void setRequestMethod(HttpRequest.Method requestMethod) {
        this.requestMethod = requestMethod;
    }


    private HttpResponse(Status status, String mimeType, InputStream data, long totalBytes) {
        this.status = status == null ? Status.OK : status;
        this.mimeType = mimeType;
        if (data == null) {
            this.data = new ByteArrayInputStream(new byte[0]);
            contentLength = 0L;
        } else {
            this.data = data;
            this.contentLength = totalBytes;
        }
        chunkedTransfer = contentLength < 0; // 不知道totalBytes的时候采用chunkedTransfer
        keepAlive = true;
    }


    /**
     * 返回已知大小纯文本
     */
    public static HttpResponse newFixedLengthResponse(String msg) {
        return newFixedLengthResponse(HttpResponse.Status.OK, ContentType.MIME_HTML, msg);
    }


    /**
     * 返回已知大小的纯文本，指定返回码，文本类型
     */
    public static HttpResponse newFixedLengthResponse(HttpResponse.Status status, String mimeType, String txt) {
        ContentType contentType = new ContentType(mimeType);

        if (txt == null) {
            return newFixedLengthResponse(status, mimeType, new ByteArrayInputStream(new byte[0]), 0);
        } else {
            byte[] bytes;
            try {
                CharsetEncoder newEncoder = Charset.forName(contentType.getEncoding()).newEncoder();
                if (!newEncoder.canEncode(txt)) {
                    contentType = contentType.tryUTF8();
                }
                bytes = txt.getBytes(contentType.getEncoding());
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Encoding problem, Responding nothing ", e);
                bytes = new byte[0];
            }
            return newFixedLengthResponse(status, contentType.getContentTypeHeader(),
                    new ByteArrayInputStream(bytes), bytes.length);
        }
    }

    /**
     * 知道返回体的大小,以流的形式返回
     */
    public static HttpResponse newFixedLengthResponse(HttpResponse.Status status, String mimeType, InputStream data, long totalBytes) {
        return new HttpResponse(status, mimeType, data, totalBytes);
    }

    /**
     * 不知道返回体的大小
     */
    public static HttpResponse newChunkedResponse(HttpResponse.Status status, String mimeType, InputStream data) {
        return new HttpResponse(status, mimeType, data, -1);
    }

    @Override
    public void close() throws IOException {
        if (data != null) {
            data.close();
        }
    }


    /**
     * 发送返回的数据到客户端
     */
    public void send(OutputStream outputStream) throws IOException {
        SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));

        PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, new ContentType(this.mimeType).getEncoding())), false);

        writer.append("HTTP/1.1 ").append(this.status.getDescription()).append(" \r\n");
        if (this.mimeType != null) {
            printHeader(writer, "Content-Type", this.mimeType);
        }
        if (getHeader("date") == null) {
            printHeader(writer, "Date", gmtFrmt.format(new Date()));
        }
        for (Map.Entry<String, String> entry : this.header.entrySet()) {
            printHeader(writer, entry.getKey(), entry.getValue());
        }
        if (getHeader("connection") == null) {
            printHeader(writer, "Connection", (this.keepAlive ? "keep-alive" : "close"));
        }
        if (getHeader("content-length") != null) {
            encodeAsGzip = false;
        }
        if (encodeAsGzip) {
            printHeader(writer, "Content-Encoding", "gzip");
            setChunkedTransfer(true);
        }
        long pending = this.data != null ? this.contentLength : 0;
        if ((requestMethod != HttpRequest.Method.HEAD) && this.chunkedTransfer) {
            printHeader(writer, "Transfer-Encoding", "chunked");
        } else if (!encodeAsGzip) {
            pending = sendContentLengthHeaderIfNotAlreadyPresent(writer, pending);
        }
        writer.append("\r\n");
        writer.flush();
        sendBodyWithCorrectTransferAndEncoding(outputStream, pending);
        outputStream.flush();
        HttpDaemon.safeClose(this.data);
    }

    /**
     * 按照Header的格式将key-value对转换字符串
     *
     * @param writer 写到OutputStreamWriter中
     */
    private void printHeader(PrintWriter writer, String key, String value) {
        writer.append(key).append(": ").append(value).append("\r\n");
    }

    private long sendContentLengthHeaderIfNotAlreadyPresent(PrintWriter pw, long defaultSize) {
        String contentLengthString = getHeader("content-length");
        long size = defaultSize;
        if (contentLengthString != null) {
            try {
                size = Long.parseLong(contentLengthString);
            } catch (NumberFormatException ex) {
                Log.e(TAG, "content-length was no number " + contentLengthString);
            }
        }
        pw.print("Content-Length: " + size + "\r\n");
        return size;
    }

    private void sendBodyWithCorrectTransferAndEncoding(OutputStream outputStream, long pending) throws IOException {
        if (this.requestMethod != HttpRequest.Method.HEAD && this.chunkedTransfer) {
            ChunkedOutputStream chunkedOutputStream = new ChunkedOutputStream(outputStream);
            sendBodyWithCorrectEncoding(chunkedOutputStream, -1);
            chunkedOutputStream.finish();
        } else {
            sendBodyWithCorrectEncoding(outputStream, pending);
        }
    }

    private void sendBodyWithCorrectEncoding(OutputStream outputStream, long pending) throws IOException {
        if (encodeAsGzip) {
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
            sendBody(gzipOutputStream, -1);
            gzipOutputStream.finish();
        } else {
            sendBody(outputStream, pending);
        }
    }

    /**
     * Sends the body to the specified OutputStream. The pending parameter
     * limits the maximum amounts of bytes sent unless it is -1, in which
     * case everything is sent.
     *
     * @param outputStream the OutputStream to send data to
     * @param pending      -1 to send everything, otherwise sets a max limit to the
     *                     number of bytes sent
     * @throws IOException if something goes wrong while sending the data.
     */
    private void sendBody(OutputStream outputStream, long pending) throws IOException {
        long BUFFER_SIZE = 16 * 1024;
        byte[] buff = new byte[(int) BUFFER_SIZE];
        boolean sendEverything = pending == -1;
        while (pending > 0 || sendEverything) {
            long bytesToRead = sendEverything ? BUFFER_SIZE : Math.min(pending, BUFFER_SIZE);
            int read = this.data.read(buff, 0, (int) bytesToRead);
            if (read <= 0) {
                break;
            }
            outputStream.write(buff, 0, read);
            if (!sendEverything) {
                pending -= read;
            }
        }
    }


    public static final class ResponseException extends Exception {

        public ResponseException(String message) {
            super(message);
        }
    }

    /**
     * 自动将write的数据发送到output stream
     * <p/>
     * http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.6.1
     */
    private static class ChunkedOutputStream extends FilterOutputStream {

        public ChunkedOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            byte[] data = {
                    (byte) b
            };
            write(data, 0, 1);
        }

        @Override
        public void write(@NonNull byte[] b) throws IOException {
            write(b, 0, b.length);
        }

        @Override
        public void write(@NonNull byte[] b, int off, int len) throws IOException {
            if (len == 0)
                return;
            out.write(String.format("%x\r\n", len).getBytes());
            out.write(b, off, len);
            out.write("\r\n".getBytes());
        }

        public void finish() throws IOException {
            out.write("0\r\n\r\n".getBytes());
        }

    }
}

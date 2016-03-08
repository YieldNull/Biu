package com.bbbbiu.biu.httpd;

import android.support.annotation.NonNull;
import android.util.Log;

import com.bbbbiu.biu.httpd.util.ContentType;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
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

public class HttpResponse {

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


    private boolean chunkedTransfer;
    private boolean encodeAsGzip;
    private boolean keepAlive;

    private HashMap<String, String> headerMap = new HashMap<>();

    public InputStream getData() {
        return this.data;
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

    public void setData(InputStream data) {
        this.data = data;
    }


    public void addHeader(String header, String value) {
        headerMap.put(header, value);
    }

    public Status getStatus() {
        return status;
    }

    /**
     * 私有构造函数，会有工厂方法创建对象
     *
     * @param status     返回状态码,默认为200
     * @param mimeType   返回MIME类型
     * @param data       返回报文输入流
     * @param totalBytes 返回报文大小 “Content-Length"
     */
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

        keepAlive = true; // 默认KeepAlive,之后会根据Request做出相应更改

        Log.d(TAG, String.format("%s Finish generating response. Status:%s",
                Thread.currentThread().getName(), status.getDescription()));
    }


    /**
     * 返回已知大小纯文本
     */
    public static HttpResponse newResponse(String msg) {
        return newResponse(HttpResponse.Status.OK, ContentType.MIME_HTML, msg);
    }


    /**
     * 返回已知大小的纯文本，指定返回码，文本类型
     */
    public static HttpResponse newResponse(Status status, String mimeType, String txt) {
        ContentType contentType = new ContentType(mimeType);

        if (txt == null) {
            return newResponse(status, mimeType, new ByteArrayInputStream(new byte[0]), 0);
        } else {
            byte[] bytes;
            try {
                CharsetEncoder newEncoder = Charset.forName(contentType.getEncoding()).newEncoder();
                if (!newEncoder.canEncode(txt)) {
                    contentType = contentType.tryUTF8();
                }
                bytes = txt.getBytes(contentType.getEncoding());
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, Thread.currentThread().getName() + "Encoding problem, Responding nothing", e);
                bytes = new byte[0];
            }
            return newResponse(status, contentType.getContentTypeHeader(),
                    new ByteArrayInputStream(bytes), bytes.length);
        }
    }

    /**
     * 知道返回体的大小,以流的形式返回
     */
    public static HttpResponse newResponse(Status status, String mimeType, InputStream data, long totalBytes) {
        return new HttpResponse(status, mimeType, data, totalBytes);
    }

    /**
     * 不知道返回体的大小
     */
    public static HttpResponse newChunkedResponse(Status status, String mimeType, InputStream data) {
        return new HttpResponse(status, mimeType, data, -1);
    }


    public static HttpResponse newRedirectResponse(String location) {
        HttpResponse response = newResponse(Status.REDIRECT_SEE_OTHER, ContentType.MIME_HTML, null);
        response.addHeader("location", location);
        return response;
    }

    /**
     * 发送返回的数据到客户端
     */
    public void send(OutputStream outputStream) throws IOException {
        PrintWriter writer = new PrintWriter(
                new BufferedWriter(
                        new OutputStreamWriter(
                                outputStream,
                                new ContentType(mimeType).getEncoding())),
                false);


        // 协议、状态码、状态描述
        writer.append("HTTP/1.1 ").append(status.getDescription()).append(" \r\n");


        // MIME Content-Type
        if (mimeType != null) {
            printHeader(writer, "Content-Type", mimeType);
        }

        // Date
        SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        printHeader(writer, "Date", gmtFrmt.format(new Date()));

        // Connection,Keep-Alive? 默认keep-alive
        printHeader(writer, "Connection", (keepAlive ? "Keep-Alive" : "close"));


        for (Map.Entry<String, String> header : headerMap.entrySet()) {
            printHeader(writer, header.getKey(), header.getValue());
        }

        // Use Gzip? 默认false
        if (encodeAsGzip) {
            printHeader(writer, "Content-Encoding", "gzip");
            chunkedTransfer = true; // Gzip压缩之后就不知道大小了。。。
        }

        // chunked OR specific length
        if (chunkedTransfer) {
            printHeader(writer, "Transfer-Encoding", "chunked");
        } else {
            printHeader(writer, "Content-Length", String.valueOf(contentLength));
        }

        writer.append("\r\n");
        writer.flush();

        sendBodyIfChunked(outputStream);
        outputStream.flush();
    }

    /**
     * 按照Header的格式将key-value对转换字符串
     *
     * @param writer 写到OutputStreamWriter中
     */
    private void printHeader(PrintWriter writer, String key, String value) {
        writer.append(key).append(": ").append(value).append("\r\n");
    }


    /**
     * 按 Chunked Transfer 传输 或普通传输
     *
     * @param outputStream 输出流
     * @throws IOException
     */
    private void sendBodyIfChunked(OutputStream outputStream) throws IOException {
        if (chunkedTransfer) {
            ChunkedOutputStream chunkedOutputStream = new ChunkedOutputStream(outputStream);
            sendBodyIfGzip(chunkedOutputStream);
            chunkedOutputStream.finish();
        } else {
            sendBodyIfGzip(outputStream);
        }
    }

    /**
     * 按Gzip压缩后传输或普通传输
     *
     * @param outputStream 输出流
     * @throws IOException
     */
    private void sendBodyIfGzip(OutputStream outputStream) throws IOException {
        if (encodeAsGzip) {
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
            sendBody(gzipOutputStream);
            gzipOutputStream.finish();
        } else {
            sendBody(outputStream);
        }
    }

    /**
     * 将data中的数据全部读出，写入输出流
     *
     * @param outputStream 输出流
     * @throws IOException
     */
    private void sendBody(OutputStream outputStream) throws IOException {
        int BUFFER_SIZE = 16 * 1024;
        byte[] buff = new byte[BUFFER_SIZE];

        while (true) {
            int read = data.read(buff, 0, BUFFER_SIZE);
            if (read <= 0) {
                break;
            }
            outputStream.write(buff, 0, read);
        }
    }


    /**
     * 自动将write的数据发送到output stream
     * <p>
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

    public static final class ResponseException extends Exception {

        public ResponseException(String message) {
            super(message);
        }
    }
}

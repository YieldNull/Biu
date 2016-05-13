package com.bbbbiu.biu.lib.httpd;

import android.util.Log;

import com.bbbbiu.biu.lib.util.ProgressNotifier;
import com.bbbbiu.biu.lib.util.ProgressListener;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
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
        OK(200, "OK"),
        REDIRECT_SEE_OTHER(303, "See Other"),
        BAD_REQUEST(400, "Bad Request"),
        NOT_FOUND(404, "Not Found"),
        METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
        INTERNAL_ERROR(500, "Internal Server Error");

        private final int status;

        private final String description;

        Status(int status, String description) {
            this.status = status;
            this.description = description;
        }

        public String getDescription() {
            return "" + this.status + " " + this.description;
        }
    }

    /**
     * 回车换行：Carriage Return & Line Feed
     */
    private static final String CRLF = "\r\n";

    /**
     * 空格
     */
    private static final String SPACE = " ";

    /**
     * 返回状态码
     */
    private Status status;


    /**
     * 返回数据对应的MIME TYPE
     */
    private String mimeType;

    /**
     * 将要发送的数据
     */
    private InputStream dataStream;

    /**
     * 数据长度
     */
    private long contentLength;

    /**
     * 是否用Gzip发送
     */
    private boolean encodeAsGzip;

    /**
     * 监听发送进度
     */
    private ProgressNotifier progressNotifier;

    /**
     * HTTP Headers
     */
    private HashMap<String, String> headerMap = new HashMap<>();


    /**
     * 获取返回数据的 MIME TYPE
     *
     * @return MIME TYPE
     */
    public String getMimeType() {
        return this.mimeType;
    }

    /**
     * 获取返回状态码
     *
     * @return {@link Status}
     */
    public Status getStatus() {
        return status;
    }

    /**
     * 设置是否使用GZIP 发送数据
     *
     * @param encodeAsGzip 是否使用GZIP
     */
    public void setGzipEncoding(boolean encodeAsGzip) {
        this.encodeAsGzip = encodeAsGzip;
    }


    /**
     * 设置发送进度监听
     *
     * @param progressNotifier {@link ProgressListener}
     */
    public void setProgressNotifier(ProgressNotifier progressNotifier) {
        this.progressNotifier = progressNotifier;
    }

    /**
     * 设置HTTP Header。会覆盖原有值
     *
     * @param header header
     * @param value  value
     */
    public void addHeader(String header, String value) {
        headerMap.put(header, value);
    }

    /**
     * 私有构造函数，会有工厂方法创建对象.
     * <p/>
     * {@link HttpResponse#newResponse(String)}
     * <p/>
     * {@link HttpResponse#newResponse(Status, String)}
     * <p/>
     * {@link HttpResponse#newResponse(InputStream, long)}
     * <p/>
     * {@link HttpResponse#newResponse(Status, String, InputStream, long)}
     * <p/>
     * {@link HttpResponse#newRedirectResponse(String)}
     *
     * @param status     返回状态码,默认为200
     * @param mimeType   返回MIME类型
     * @param dataStream 返回报文输入流
     * @param totalBytes 返回报文大小 “Content-Length"
     */
    private HttpResponse(Status status, String mimeType, InputStream dataStream, long totalBytes) {
        this.status = status == null ? Status.OK : status;
        this.mimeType = mimeType;

        if (dataStream == null) {
            this.dataStream = new ByteArrayInputStream(new byte[0]);
            contentLength = 0L;
        } else {
            this.dataStream = dataStream;
            this.contentLength = totalBytes;
        }

        Log.d(TAG, String.format("%s Finish generating response. Status:%s",
                Thread.currentThread().getName(), status.getDescription()));
    }


    /**
     * 返回HTML源码,状态码为200
     *
     * @param html 源码
     * @return {@link HttpResponse}
     */
    public static HttpResponse newResponse(String html) {
        return newResponse(HttpResponse.Status.OK, html);
    }


    /**
     * 返回指定状态码的HTML源码
     *
     * @param status 状态码 {@link Status}
     * @param html   源码
     * @return {@link HttpResponse}
     */
    public static HttpResponse newResponse(Status status, String html) {
        ContentType contentType = new ContentType(ContentType.MIME_HTML);

        if (html == null) {
            return newResponse(status, ContentType.MIME_HTML, new ByteArrayInputStream(new byte[0]), 0);
        } else {
            byte[] bytes;
            try {
                CharsetEncoder newEncoder = Charset.forName(contentType.getEncoding()).newEncoder();
                if (!newEncoder.canEncode(html)) {
                    contentType = contentType.tryUTF8();
                }
                bytes = html.getBytes(contentType.getEncoding());
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, Thread.currentThread().getName() + "Encoding problem, Responding nothing", e);
                bytes = new byte[0];
            }
            return newResponse(status, contentType.getContentTypeHeader(),
                    new ByteArrayInputStream(bytes), bytes.length);
        }
    }

    /**
     * 以 {@link ContentType#MIME_STREAM} 形式返回，状态码200
     *
     * @param data       输入流
     * @param totalBytes length
     * @return {@link HttpResponse}
     */
    public static HttpResponse newResponse(InputStream data, long totalBytes) {
        return newResponse(Status.OK, ContentType.MIME_STREAM, data, totalBytes);
    }

    /**
     * 从从输入流读取数据
     *
     * @param status     状态码，{@link Status}
     * @param mimeType   MIME ，{@link ContentType}
     * @param data       输入流
     * @param totalBytes length
     * @return {@link HttpResponse}
     */
    public static HttpResponse newResponse(Status status, String mimeType, InputStream data, long totalBytes) {
        return new HttpResponse(status, mimeType, data, totalBytes);
    }

    /**
     * 重定向
     *
     * @param location 重定向地点，http header "location"
     * @return {@link HttpResponse}
     */
    public static HttpResponse newRedirectResponse(String location) {
        HttpResponse response = newResponse(Status.REDIRECT_SEE_OTHER, null);
        response.addHeader("location", location);
        return response;
    }

    /**
     * 将要发送给客户端的数据写入输出流。
     * <p/>
     *
     * @param outputStream OutputStream
     * @throws IOException
     */
    public void send(OutputStream outputStream) throws IOException {
        PrintWriter writer = new PrintWriter(
                new BufferedWriter(
                        new OutputStreamWriter(
                                outputStream,
                                new ContentType(mimeType).getEncoding())),
                false);


        // 协议、状态码、状态描述
        writer.append("HTTP/1.1 ").append(status.getDescription()).append(SPACE + CRLF);


        // MIME Content-Type
        if (mimeType != null) {
            printHeader(writer, "Content-Type", mimeType);
        }

        // Date
        SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        printHeader(writer, "Date", gmtFrmt.format(new Date()));

        // Connection,Keep-Alive?
        printHeader(writer, "Connection", "close");

        // content-length
        printHeader(writer, "Content-Length", String.valueOf(contentLength));

        // Use Gzip?
        if (encodeAsGzip) {
            printHeader(writer, "Content-Encoding", "gzip");
        }

        // other headers
        for (Map.Entry<String, String> header : headerMap.entrySet()) {
            printHeader(writer, header.getKey(), header.getValue());
        }

        writer.append(CRLF);
        writer.flush();

        sendBodyIfGzip(outputStream);
        outputStream.flush();
    }

    /**
     * 按照Header的格式将key-value对转换字符串
     *
     * @param writer 写到writer里面
     * @param key    key
     * @param value  value
     */
    private void printHeader(PrintWriter writer, String key, String value) {
        writer.append(key).append(": ").append(value).append(CRLF);
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
            int read = dataStream.read(buff, 0, BUFFER_SIZE);
            if (read <= 0) {
                break;
            }
            outputStream.write(buff, 0, read);

            if (progressNotifier != null) {
                progressNotifier.noteBytesRead(read);
            }
        }
    }
}

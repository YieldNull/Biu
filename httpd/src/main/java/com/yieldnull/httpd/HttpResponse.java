package com.yieldnull.httpd;


import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.GZIPOutputStream;

public class HttpResponse {

    private static final Log LOGGER = Log.of(HttpResponse.class);

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

        public int getStatus() {
            return this.status;
        }
    }


    /**
     * 返回体达到一定大小之后就使用GZip
     */
    private static final long GZIP_THRESHOLD = 64;


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
    private Status mStatus;


    /**
     * 返回数据对应的MIME TYPE
     */
    private String mMimeType;


    /**
     * 将要发送的数据
     */
    private InputStream mDataStream;


    /**
     * 数据长度
     */
    private long mContentLength;


    /**
     * 是否用Gzip发送
     */
    private boolean mEncodeAsGzip;


    /**
     * 监听发送进度
     */
    private ProgressNotifier progressNotifier;


    /**
     * HTTP Headers
     */
    private HashMap<String, String> mHeaders = new HashMap<>();


    /**
     * 获取返回数据的 MIME TYPE
     *
     * @return MIME TYPE
     */
    public String getMimeType() {
        return this.mMimeType;
    }


    /**
     * 获取返回状态码
     *
     * @return {@link Status}
     */
    public Status getStatus() {
        return mStatus;
    }


    /**
     * 设置是否使用GZIP 发送数据
     *
     * @param encodeAsGzip 是否使用GZIP
     */
    public void setGzipEncoding(boolean encodeAsGzip) {
        if (mContentLength > GZIP_THRESHOLD) {
            this.mEncodeAsGzip = encodeAsGzip;
        }
    }


    /**
     * 设置HTTP Header。会覆盖原有值
     *
     * @param header header
     * @param value  value
     */
    public void addHeader(String header, String value) {
        mHeaders.put(header, value);
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
     * 私有构造函数，会有工厂方法创建对象.
     *
     * @param status     返回状态码,默认为200
     * @param mimeType   返回MIME类型
     * @param dataStream 返回报文输入流
     * @param totalBytes 返回报文大小 “Content-Length"
     */
    private HttpResponse(Status status, String mimeType, InputStream dataStream, long totalBytes) {
        this.mStatus = status == null ? Status.OK : status;
        this.mMimeType = mimeType;

        if (dataStream == null) {
            this.mDataStream = new ByteArrayInputStream(new byte[0]);
            mContentLength = 0L;
        } else {
            this.mDataStream = dataStream;
            this.mContentLength = totalBytes;
        }
        LOGGER.i("Finish generating response.");
    }


    /**
     * 返回HTML源码(UTF-8),状态码为200
     *
     * @param html 源码
     * @return {@link HttpResponse}
     */
    public static HttpResponse newResponse(String html) {
        return newResponse(Status.OK, html);
    }


    /**
     * 返回指定状态码的HTML源码(UTF-8)
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
                bytes = html.getBytes(contentType.getEncoding());
            } catch (UnsupportedEncodingException e) {
                LOGGER.w("Encoding problem, Responding nothing", e);

                bytes = new byte[0];
            }
            return newResponse(status, ContentType.MIME_HTML,
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
     * 指定MIME TYPE，并将指定输入流的数据发送到输出流，状态码200
     *
     * @param mimeType   mMimeType
     * @param data       输入流
     * @param totalBytes length
     * @return {@link HttpResponse}
     */
    public static HttpResponse newResponse(String mimeType, InputStream data, long totalBytes) {
        return newResponse(Status.OK, mimeType, data, totalBytes);
    }


    /**
     * 返回指定MIME TYPE 的文本内容(UTF-8)，状态码200
     *
     * @param mime    类型
     * @param content 源码
     * @return {@link HttpResponse}
     */
    public static HttpResponse newResponse(String mime, String content) {
        byte[] bytes = content.getBytes();
        return newResponse(Status.OK, mime, new ByteArrayInputStream(bytes), bytes.length);
    }


    /**
     * 指定MIME TYPE，并将指定输入流的数据发送到输出流，指定状态码
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
                                new ContentType(mMimeType).getEncoding())),
                false);


        // 协议、状态码、状态描述
        writer.append("HTTP/1.1 ").append(mStatus.getDescription()).append(SPACE + CRLF);


        // MIME Content-Type
        if (mMimeType != null) {
            printHeader(writer, "Content-Type", mMimeType);
        }

        // Date
        SimpleDateFormat format = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        printHeader(writer, "Date", format.format(new Date()));

        // Connection,Keep-Alive?
        printHeader(writer, "Connection", "close");

        // content-length
        printHeader(writer, "Content-Length", String.valueOf(mContentLength));

        // Use Gzip?
        if (mEncodeAsGzip) {
            printHeader(writer, "Content-Encoding", "gzip");
        }

        // other getHeaders
        for (Map.Entry<String, String> header : mHeaders.entrySet()) {
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
        if (mEncodeAsGzip) {
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
        int BUFFER_SIZE = 8 * 1024;
        byte[] buff = new byte[BUFFER_SIZE];

        while (true) {
            int read = mDataStream.read(buff, 0, BUFFER_SIZE);
            if (read <= 0) {
                break;
            }
            outputStream.write(buff, 0, read);

            if (progressNotifier != null) {
                progressNotifier.noteBytesRead(read);
            }
        }

        outputStream.flush();
    }

}
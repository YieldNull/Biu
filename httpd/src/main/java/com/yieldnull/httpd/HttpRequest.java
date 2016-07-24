package com.yieldnull.httpd;


import com.yieldnull.httpd.util.Streams;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HTTP 请求，解析Http Header之后，交给Servlet处理
 */
public class HttpRequest {
    private static final Logger LOGGER = Logger.getLogger(Streams.class.getName());


    public static final int BUFSIZE = 8192; // 8KB


    private final BufferedInputStream mInputStream;

    private String mUri;
    private Method mMethod;

    private Map<String, String> mParams = new HashMap<>();
    private Map<String, String> mHeaders = new HashMap<>();


    private String mClientIp; // 客户端ip
    private String protocolVersion;
    private boolean mKeepAlive;

    public enum Method {
        GET,
        PUT,
        POST,
        DELETE,
        HEAD,
        OPTIONS,
        TRACE,
        CONNECT,
        PATCH,
        PROPFIND,
        PROPPATCH,
        MKCOL,
        MOVE,
        COPY,
        LOCK,
        UNLOCK, Method;

        public static HttpRequest.Method lookup(String method) {
            if (method == null)
                return null;

            try {
                return HttpRequest.Method.valueOf(method);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }


    private HttpRequest(InputStream inputStream, InetAddress inetAddress) {

        this.mInputStream = new BufferedInputStream(inputStream, HttpRequest.BUFSIZE);

        this.mClientIp = inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress() ?
                "127.0.0.1" : inetAddress.getHostAddress();

        this.mHeaders = new HashMap<>();
    }

    public final InputStream getInputStream() {
        return this.mInputStream;
    }

    public final Map<String, String> getHeaders() {
        return this.mHeaders;
    }


    public final Method getMethod() {
        return this.mMethod;
    }


    public final String getUri() {
        return this.mUri;
    }

    public String getClientIp() {
        return mClientIp;
    }

    public boolean isKeepAlive() {
        return mKeepAlive;
    }

    public long contentLength() {
        long size;
        try {
            String cl1 = getHeaders().get("content-length");
            size = Long.parseLong(cl1);
        } catch (NumberFormatException var4) {
            size = -1L;
        }

        return size;
    }

    // TODO
    public String getCharacterEncoding() {
        return "UTF-8";
    }

    public String getContentType() {
        return getHeaders().get("content-type");
    }

    public int getContentLength() {
        return (int) contentLength();
    }

    public String getText() {
        if (mInputStream == null) {
            return null;
        }

        InputStreamReader reader;
        try {
            reader = new InputStreamReader(mInputStream, getCharacterEncoding());
        } catch (UnsupportedEncodingException e) {
            LOGGER.log(Level.WARNING, e.toString());
            reader = new InputStreamReader(mInputStream);
        }

        StringBuilder stringBuilder = new StringBuilder();
        int c;

        try {
            try {
                while ((c = reader.read()) >= 0) {
                    stringBuilder.append((char) c);
                }
            } catch (SocketTimeoutException e) { // 浏览器使用Keep-Alive，发完之后不关闭连接。。。
                LOGGER.log(Level.WARNING, e.toString());
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e.toString());
        }

        return stringBuilder.toString();
    }

    public static HttpRequest parseRequest(InputStream inputStream, InetAddress inetAddress) {
        HttpRequest request = new HttpRequest(inputStream, inetAddress);

        try {
            request.parseRequestHeader();
            return request;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, Thread.currentThread().getName() + "Exception when parsing request", e);
        }
        return null;
    }

    private void parseRequestHeader() throws IOException {
        byte[] headerBuf = new byte[HttpRequest.BUFSIZE]; //请求头缓冲区，最大不超过 HttpRequest.BUFSIZE
        int headerEndIndex = 0;
        int readCount = 0;

        mInputStream.mark(HttpRequest.BUFSIZE);

        int read = mInputStream.read(headerBuf, 0, HttpRequest.BUFSIZE);

        // 表示一次可能读不完，分多次读，读到两个CRLF 或者读满buf就结束循环
        while (read != -1) {
            readCount += read;
            headerEndIndex = findHeaderEnd(headerBuf, readCount);

            if (headerEndIndex > 0) {
                break;
            }

            // 最多读 HttpRequest.BUFSIZE - this.mReadCount 个字节
            // 存入 buf 时，从 mReadCount 开始

            read = mInputStream.read(headerBuf, readCount, HttpRequest.BUFSIZE - readCount);
        }

        if (headerEndIndex < readCount) {
            mInputStream.reset(); // 回到未读取状态
            mInputStream.skip(headerEndIndex); // 跳过请求头，为后面的Body处理做准备
        } // endIndex可能比已经读到Bytes数大（当头大于Buf size的时候）就不需要移动到EndIndex,直接往下读就行了

        // 提取Header中的信息
        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(headerBuf, 0, readCount)));
        decodeHeader(reader);

        String connection = mHeaders.get("connection");
        mKeepAlive = "HTTP/1.1".equals(protocolVersion) && (connection == null || !connection.matches("(?i).*close.*"));
    }


    /**
     * 解析请求头
     *
     * @param reader reader
     * @throws IOException
     */
    private void decodeHeader(BufferedReader reader) throws IOException {

        // 请求的URL，请求方式，HTTP协议版本
        String line = reader.readLine();
        if (line == null) {
            return;
        }

        String[] tokens = line.split("\\s+");

        if (tokens.length != 3) {
            LOGGER.log(Level.WARNING, Thread.currentThread().getName() + "Invalid HTTP Request Header");
            throw new IOException(HttpResponse.Status.BAD_REQUEST.getDescription());
        }

        mMethod = Method.lookup(tokens[0]);
        String uri = tokens[1];
        protocolVersion = tokens[2];

        // 提取URL中的参数
        int qmi = uri.indexOf('?');
        if (qmi >= 0) {
            decodeParams(uri.substring(qmi + 1));
            mUri = decodeUrl(uri.substring(0, qmi));
        } else {
            mUri = decodeUrl(uri);
        }


        // 提取请求头
        line = reader.readLine();
        while (line != null && !line.trim().isEmpty()) {
            int p = line.indexOf(':');
            if (p >= 0) {
                mHeaders.put(line.substring(0, p).trim().toLowerCase(Locale.US), line.substring(p + 1).trim());
            }
            line = reader.readLine();
        }

    }

    /**
     * 提取URL中的参数
     *
     * @param params URL中‘?’之后的参数
     */
    private void decodeParams(String params) {
        if (params == null) {
            return;
        }

        StringTokenizer st = new StringTokenizer(params, "&");
        while (st.hasMoreTokens()) {
            String e = st.nextToken();
            int sep = e.indexOf('=');
            if (sep >= 0) {
                mParams.put(decodeUrl(e.substring(0, sep)).trim(), decodeUrl(e.substring(sep + 1)));
            } else {
                mParams.put(decodeUrl(e).trim(), "");
            }
        }
    }

    /**
     * 将URL中转义过的字符解码
     */
    private String decodeUrl(String str) {
        String decoded = null;
        try {
            decoded = URLDecoder.decode(str, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
            LOGGER.log(Level.WARNING, Thread.currentThread().getName() + "Encoding not supported, ignored ", ignored);
        }
        return decoded == null ? "/" : decoded;
    }


    /**
     * 找到分割HTTP Header与Body的位置，从头开始找
     * <p/>
     * 请求头与请求体由两个CRLF('\r\n')分割
     * 即求第二个'\n'的位置
     *
     * @param buf       header buffer
     * @param readCount buf中的有效Byte数量
     * @return 找到的位置，否则为-1
     */
    private int findHeaderEnd(final byte[] buf, int readCount) {
        int endIndex = 0;
        while (endIndex < readCount - 1) {

            if (buf[endIndex] == '\r' && buf[endIndex + 1] == '\n' &&
                    endIndex + 3 < readCount &&
                    buf[endIndex + 2] == '\r' && buf[endIndex + 3] == '\n') {
                return endIndex + 4;
            }

            endIndex++;
        }
        return -1;
    }
}

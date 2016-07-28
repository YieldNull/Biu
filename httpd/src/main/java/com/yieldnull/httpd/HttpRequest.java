package com.yieldnull.httpd;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

/**
 * 解析Http请求
 */
public class HttpRequest {
    private static final Logger LOGGER = Logger.getLogger(Streams.class.getName());


    /**
     * Http request methods
     */
    public enum Method {
        OPTIONS,
        GET,
        HEAD,
        POST,
        PUT,
        DELETE,
        TRACE,
        CONNECT;

        /**
         * 把String mapping 到 Method
         *
         * @param method 方法
         * @return 不存在该方法则返回 Null
         */
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


    /**
     * 处理请求报文，解析请求头。
     *
     * @param inputStream 报文输入流
     * @param inetAddress 请求来源
     * @return HttpRequest
     */
    public static HttpRequest parseRequest(InputStream inputStream, InetAddress inetAddress) {
        HttpRequest request = new HttpRequest(inputStream, inetAddress);

        try {
            request.parseRequest();
            //System.out.println(Thread.currentThread().getName() + "Finish parsing request header");
            return request;
        } catch (IOException e) {
            //System.out.println(Thread.currentThread().getName() + "Exception when parsing request");
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 请求头缓冲区，最大不超过 BUFSIZE
     */
    private static final int BUFSIZE = 8192; // 8KB


    /**
     * Http 请求报文输入流
     */
    private final BufferedInputStream stream;


    /**
     * URI
     */
    private String uri;


    /**
     * Request method
     */
    private Method method;


    /**
     * 请求头，Key-Value pair
     */
    private Map<String, String> mHeaders = new HashMap<>();


    /**
     * 客户端ip
     */
    private String clientIp;


    /**
     * 协议版本
     */
    private String protocolVersion;


    /**
     * URL 中的Key-Value pair
     */
    private Map<String, String> args = new HashMap<>();


    /**
     * 表单各项
     */
    private Map<String, String> form = new HashMap<>();


    /**
     * 文件列表
     */
    private List<File> files = new ArrayList<>();


    /**
     * 请求体的纯文本形式
     */
    private String text;


    /**
     * 构造函数
     *
     * @param inputStream Http 请求报文输入流
     * @param inetAddress 请求来源
     */
    private HttpRequest(InputStream inputStream, InetAddress inetAddress) {

        this.stream = new BufferedInputStream(inputStream, BUFSIZE);

        this.clientIp = inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress() ?
                "127.0.0.1" : inetAddress.getHostAddress();

        this.mHeaders = new HashMap<>();
    }


    /**
     * 获取请求来源IP
     *
     * @return 请求来源IP
     */
    public String clientIp() {
        return clientIp;
    }


    /**
     * 协议版本
     *
     * @return 协议版本
     */
    public String protocol() {
        return protocolVersion;
    }


    /**
     * 获取请求方法
     *
     * @return 请求方法
     */
    public Method method() {
        return method;
    }

    /**
     * 获取请求的URI
     *
     * @return URI
     */
    public String uri() {
        return uri;
    }

    /**
     * 获取请求头的各个Key-Value pair。 Key全部小写
     *
     * @return Key-Value pair
     */
    public Map<String, String> headers() {
        return mHeaders;
    }


    /**
     * 获取contentType
     *
     * @return contentType
     */
    public String contentType() {
        return headers().get("content-type");
    }


    /**
     * 获取请求体的长度
     *
     * @return 长度
     */
    public long contentLength() {
        long size;
        try {
            String cl1 = headers().get("content-length");
            size = Long.parseLong(cl1);
        } catch (NumberFormatException var4) {
            size = -1L;
        }

        return size;
    }


    /**
     * 获取 发起请求的User-Agent
     *
     * @return User-Agent, 默认为空串“”
     */
    public String userAgent() {
        String ua = mHeaders.get("user-agent");
        return ua == null ? "" : ua;
    }


    /**
     * 获取 Http 请求体的输入流（请求头已解析，剩下Body部分）
     *
     * @return 输入流
     */
    public InputStream stream() {
        return stream;
    }


    /**
     * 获取URL中的 Query params.
     *
     * @return Key-Value pair
     */
    public Map<String, String> args() {
        return args;
    }


    /**
     * 获取表单中的Key-Value
     *
     * @return Key-Value pair,不包括文件
     */
    public Map<String, String> form() {
        return form;
    }


    /**
     * 文件列表
     *
     * @return 文件列表，请先调用{@link #parseMultipartBody(File, ProgressListener)}
     */
    public List<File> files() {
        return files;
    }


    /**
     * 以文本形式读取请求体
     *
     * @return 请求体的内容
     */
    public String text() {
        return text;
    }


    /**
     * 解析请求
     * <p/>
     * 先解析请求头，如果请求体是文本形式或者JSON，则把请求体也读取完
     *
     * @throws IOException
     */
    private void parseRequest() throws IOException {
        parseRequestHeader();

        String contentType = contentType();

        if (contentType != null && (contentType.startsWith(ContentType.TEXT)
                || contentType.startsWith(ContentType.JSON))) {
            parseRawBody();
        }
    }


    /**
     * 解析Multipart请求体
     *
     * @param repository 存放文件
     * @param listener   监听进度
     */
    public void parseMultipartBody(File repository, ProgressListener listener) {
        if (method().equals(Method.POST) && contentType() != null
                && contentType().startsWith(ContentType.MULTIPART)) {

            boolean successful = false;

            try {
                MultipartParser iterator = new MultipartParser(this);

                while (iterator.hasNext()) {
                    final MultipartEntity item = iterator.next();

                    if (item.isFile()) {
                        File file = new File(repository, item.fileName());
                        files.add(file);

                        OutputStream fileOutStream = new FileOutputStream(file);

                        // 获取表单中的fileUri，没有则用文件名代替
                        String fileUri = form.get("fileUri");
                        fileUri = fileUri == null ? item.fileName() : fileUri;

                        long length = item.contentLength();
                        length = length == -1 ? contentLength() : length;
                        Streams.copy(item.stream(), fileOutStream, true,
                                new ProgressNotifier(fileUri, listener, length));


                    } else {
                        form.put(item.fieldName(), item.fieldValue());
                    }

                }

                successful = true;
            } catch (IOException ignored) {

            } finally {
                if (!successful) {
                    for (File file : files) {
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                    }

                    files.clear();
                }
            }
        }
    }


    /**
     * 解析请求头
     *
     * @throws IOException
     */
    private void parseRequestHeader() throws IOException {

        byte[] headerBuf = new byte[BUFSIZE]; //请求头缓冲区，最大不超过 BUFSIZE
        int headerEndIndex = 0;
        int readCount = 0;

        stream.mark(BUFSIZE);

        int read = stream.read(headerBuf, 0, BUFSIZE);

        // 表示一次可能读不完，分多次读，读到两个CRLF 或者读满buf就结束循环
        while (read != -1) {
            readCount += read;
            headerEndIndex = findHeaderEnd(headerBuf, readCount);

            if (headerEndIndex > 0) {
                break;
            }

            // 最多读 BUFSIZE - this.mReadCount 个字节
            // 存入 buf 时，从 mReadCount 开始

            read = stream.read(headerBuf, readCount, BUFSIZE - readCount);
        }

        if (headerEndIndex < readCount) {
            stream.reset(); // 回到未读取状态
            //noinspection ResultOfMethodCallIgnored
            stream.skip(headerEndIndex); // 跳过请求头，为后面的Body处理做准备

        } // endIndex可能比已经读到Bytes数大（当头大于Buf size的时候）就不需要移动到EndIndex,直接往下读就行了

        // 提取Header中的信息
        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(headerBuf, 0, readCount)));
        decodeHeader(reader);
    }


    /**
     * 以文本形式解析请求体
     */
    private void parseRawBody() {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[512];
        int length;
        try {
            while ((length = stream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
        } catch (IOException ignored) {
        }

        text = result.toString();
    }


    /**
     * 解析请求头(to 小写）
     *
     * @param reader BufferedReader
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
            //System.out.println(Thread.currentThread().getName() + "Invalid HTTP Request Header");
            throw new IOException(HttpResponse.Status.BAD_REQUEST.getDescription());
        }

        method = Method.lookup(tokens[0]);
        String uri = tokens[1];
        protocolVersion = tokens[2];

        // 提取URL中的参数
        int queryIndex = uri.indexOf('?');

        if (queryIndex >= 0) {
            decodeParams(uri.substring(queryIndex + 1));
            this.uri = decodeUrl(uri.substring(0, queryIndex));
        } else {
            this.uri = decodeUrl(uri);
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
                args.put(decodeUrl(e.substring(0, sep)).trim(), decodeUrl(e.substring(sep + 1)));
            } else {
                args.put(decodeUrl(e).trim(), "");
            }
        }
    }


    /**
     * 将URL中转义过的字符解码
     *
     * @param str 原URL
     * @return 解码后的URL
     */
    private String decodeUrl(String str) {
        String decoded = null;
        try {
            decoded = URLDecoder.decode(str, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
            //System.out.println(Thread.currentThread().getName() + "Encoding not supported, ignored ");
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

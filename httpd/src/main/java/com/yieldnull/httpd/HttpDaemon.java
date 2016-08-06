package com.yieldnull.httpd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * HTTP服务器。
 * <p/>
 * RequestListener 监听TCP端口，等待请求到来
 * RequestManager 管理请求，每个请求开一个线程处理
 * RequestHandler 处理请求，并返回
 */
public class HttpDaemon {
    private static final Log LOGGER = Log.of(HttpDaemon.class);


    /**
     * 关联URL与其对应的{@link HttpServlet}
     */
    private static HashMap<String, HttpServlet> sServletMap = new HashMap<>();


    /**
     * 注册 Servlet
     *
     * @param urlPattern servlet处理的URL（正则表达式）
     * @param servlet    servlet实例
     */
    public static void registerServlet(String urlPattern, HttpServlet servlet) {
        sServletMap.put(urlPattern, servlet);
    }


    /**
     * 单例
     */
    private static HttpDaemon sHttpDaemon;


    /**
     * 端口号
     */
    public static int sPort = 5050;

    public static int getPort() {
        return sPort;
    }

    public static HttpDaemon getSingleton() {
        if (sHttpDaemon == null) {
            sHttpDaemon = new HttpDaemon();
        }
        return sHttpDaemon;
    }


    /**
     * Socket 读取超时时间
     */
    private static final int SOCKET_READ_TIMEOUT = 4000;


    /**
     * 默认端口号
     */
    private static final int DEFAULT_PORT = 5050;


    /**
     * 监听请求的线程
     */
    private Thread mListenThread;


    /**
     * ServerSocket
     */
    private ServerSocket mServerSocket;


    /**
     * 管理请求
     */
    private RequestManager mRequestManager;


    /**
     * 指定监听端口
     */
    public HttpDaemon(int port) {
        sPort = port;
        mRequestManager = new RequestManager();
    }

    public HttpDaemon() {
        this(DEFAULT_PORT);
    }


    /**
     * 启动服务器
     *
     * @throws IOException 端口已被使用
     */
    public void start() throws IOException {
        mServerSocket = new ServerSocket();
        mServerSocket.setReuseAddress(true);

        RequestListener requestListener = new RequestListener();
        mListenThread = new Thread(requestListener);
        mListenThread.setName("Httpd Main Listener");
        mListenThread.start();

        // 等待另一个线程中的 ServerSocket 绑定端口成功
        while ((!requestListener.wasBound()) && requestListener.getBindException() == null) {
            try {
                Thread.sleep(10L);
            } catch (Throwable ignored) {
            }
        }

        LOGGER.i("Httpd started, listening port " + sPort);

        // 端口已被占用
        if (requestListener.getBindException() != null) {
            LOGGER.e("Port " + sPort + " in use");

            throw requestListener.bindException;
        }
    }

    /**
     * 关闭服务器，所有后台线程
     */
    public void stop() {
        try {
            mServerSocket.close();
            mRequestManager.closeAll();
            if (mListenThread != null) {
                mListenThread.join();
            }

            LOGGER.i("Httpd stopped");

        } catch (Exception e) {
            LOGGER.w("Could not stop all connections", e);
        }
    }

    /**
     * 是否Alive
     */
    public boolean isAlive() {
        return isStarted() && !this.mServerSocket.isClosed() && this.mListenThread.isAlive();
    }

    /**
     * 是否启动了
     */
    public boolean isStarted() {
        return this.mServerSocket != null && this.mListenThread != null;
    }


    /**
     * 当前Alive的请求数
     *
     * @return 请求数
     */
    public int aliveRequests() {
        return mRequestManager.aliveCount();
    }

    /**
     * 服务器主程序，监听请求
     */
    private class RequestListener implements Runnable {
        private boolean bound;


        IOException bindException;

        boolean wasBound() {
            return bound;
        }

        IOException getBindException() {
            return bindException;
        }


        @Override
        public void run() {
            try {
                mServerSocket.bind(new InetSocketAddress(sPort)); //监听端口
                bound = true;
            } catch (IOException e) {
                this.bindException = e;
                LOGGER.w(e.getMessage(), e);
                return;
            }
            do {
                try {
                    final Socket finalAccept = mServerSocket.accept(); //等待请求
                    finalAccept.setSoTimeout(SOCKET_READ_TIMEOUT);

                    //获取HTTP Request输入流
                    final InputStream inputStream = finalAccept.getInputStream();

                    // 开线程，处理请求
                    mRequestManager.handleRequest(inputStream, finalAccept);

                } catch (IOException e) {
                    LOGGER.w(e.getMessage());
                }
            } while (!mServerSocket.isClosed()); //不断等待请求
        }
    }


    /**
     * 分配，管理请求
     */
    private class RequestManager {
        private long requestCount;

        private final List<RequestHandler> handlerList = Collections.synchronizedList(new ArrayList<RequestHandler>());

        void handleRequest(InputStream inputStream, Socket acceptSocket) {
            RequestHandler requestHandler = new RequestHandler(inputStream, acceptSocket);
            handlerList.add(requestHandler);
            requestCount++;

            LOGGER.i(String.format("Request #%s comes from %s",
                    requestCount, acceptSocket.getInetAddress().getHostAddress()));

            Thread thread = new Thread(requestHandler);
            thread.setDaemon(true);
            thread.setName("Handler #" + this.requestCount);
            thread.start();
        }


        int aliveCount() {
            return handlerList.size();
        }

        void close(RequestHandler requestHandler) {
            handlerList.remove(requestHandler);
        }

        void closeAll() {
            // copy of the list for concurrency
            for (RequestHandler requestHandler : new ArrayList<>(handlerList)) {
                requestHandler.close();
            }
        }
    }


    /**
     * 处理请求
     */
    private class RequestHandler implements Runnable {

        private InputStream inputStream;
        private OutputStream outputStream;
        private Socket acceptSocket;

        RequestHandler(InputStream inputStream, Socket acceptSocket) {
            this.inputStream = inputStream;
            this.acceptSocket = acceptSocket;
            outputStream = null;
        }

        void close() {
            if (outputStream != null) {
                Streams.safeClose(outputStream);
            }
            Streams.safeClose(inputStream);
            Streams.safeClose(acceptSocket);
        }


        @Override
        public void run() {
            try {
                outputStream = acceptSocket.getOutputStream();

                HttpRequest request = HttpRequest.parseRequest(inputStream, acceptSocket.getInetAddress());
                HttpResponse response;


                if (request == null) {
                    response = HttpResponse.newResponse(HttpResponse.Status.BAD_REQUEST,
                            HttpResponse.Status.BAD_REQUEST.getDescription());
                    response.send(outputStream);

                    LOGGER.w("Error in parsing request");

                    return;
                }


                String acceptEncoding = request.headers().get("accept-encoding");

                response = handleRequest(request);


                // 返回用GZip压缩？
                boolean useGzip = response.getMimeType() != null && response.getMimeType().toLowerCase().contains("text/");
                response.setGzipEncoding(useGzip && acceptEncoding != null && acceptEncoding.contains("gzip"));

                // 以下才会抛异常
                response.send(outputStream);

                LOGGER.i(
                        String.format(Locale.ENGLISH, "%s -- [%s] \"%s %s %s\" %d \"%s\"",
                                request.clientIp(),
                                new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.ENGLISH)
                                        .format(Calendar.getInstance().getTime()),
                                request.method().toString(),
                                request.protocol(),
                                request.uri(),
                                response.getStatus().getStatus(),
                                request.userAgent()
                        )
                );

            } catch (Exception e) {
                LOGGER.w(e.getMessage(), e);
            } finally {
                Streams.safeClose(inputStream);
                Streams.safeClose(outputStream);
                Streams.safeClose(acceptSocket);
                mRequestManager.close(this);
            }
        }

        /**
         * 根据URL，分发请求
         */
        private HttpResponse handleRequest(HttpRequest request) {
            String uri = request.uri();
            HttpRequest.Method method = request.method();

            if (uri == null || method == null) {
                return HttpResponse.newResponse(HttpResponse.Status.BAD_REQUEST,
                        HttpResponse.Status.BAD_REQUEST.getDescription());
            }

            HttpServlet servlet = null;

            // 从已注册的Servlet中找出与url匹配的
            for (Map.Entry<String, HttpServlet> entry : sServletMap.entrySet()) {
                if (uri.matches(entry.getKey())) {
                    servlet = entry.getValue();
                    break;
                }
            }

            // 分发请求
            HttpResponse response;
            if (servlet != null) {
                LOGGER.i("Handing request via " + servlet.getClass().getSimpleName());

                if (method == HttpRequest.Method.GET) {
                    response = servlet.doGet(request);
                } else {
                    response = servlet.doPost(request);
                }
            } else {
                // 没有匹配的Servlet
                LOGGER.i("No request handler found. Raising 404");

                response = HttpResponse.newResponse(HttpResponse.Status.NOT_FOUND,
                        HttpResponse.Status.NOT_FOUND.getDescription());
            }

            // 没有对应方法的Handler
            if (response == null) {
                return HttpResponse.newResponse(HttpResponse.Status.METHOD_NOT_ALLOWED,
                        HttpResponse.Status.METHOD_NOT_ALLOWED.getDescription());
            } else {
                return response;
            }
        }
    }
}

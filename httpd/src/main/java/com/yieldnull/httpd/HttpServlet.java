package com.yieldnull.httpd;


/**
 * 处理{@link HttpRequest}，并返回{@link HttpResponse}
 */
public abstract class HttpServlet {

    public HttpServlet() {

    }

    /**
     * 处理GET请求
     *
     * @param request {@link HttpRequest}
     * @return {@link HttpResponse} or null if method is not allowed
     */
    public abstract HttpResponse doGet(HttpRequest request);

    /**
     * 处理POST请求
     *
     * @param request {@link HttpRequest}
     * @return {@link HttpResponse} or null if method is not allowed
     */
    public abstract HttpResponse doPost(HttpRequest request);

}

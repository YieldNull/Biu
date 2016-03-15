package com.bbbbiu.biu.httpd;

import android.content.Context;

public class HttpServlet {
    protected Context context;

    public HttpServlet(Context context) {
        this.context = context;
    }

    public HttpResponse doGet(HttpRequest request) {
        return HttpResponse.newResponse(HttpResponse.Status.NOT_FOUND,
                ContentType.MIME_PLAINTEXT, "Not Found");
    }

    public HttpResponse doPost(HttpRequest request) {
        return HttpResponse.newResponse(HttpResponse.Status.NOT_FOUND,
                ContentType.MIME_PLAINTEXT, "Not Found");
    }
}

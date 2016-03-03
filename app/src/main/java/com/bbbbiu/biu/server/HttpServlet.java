package com.bbbbiu.biu.server;

import android.content.Context;

public class HttpServlet {
    protected Context context;

    public HttpServlet(Context context){
        this.context=context;
    }
    public HttpResponse doGet(HttpRequest request) {
        return HttpResponse.newFixedLengthResponse(HttpResponse.Status.NOT_FOUND, ContentType.MIME_PLAINTEXT, "Not Found");
    }

    public HttpResponse doPost(HttpRequest request) {
        return HttpResponse.newFixedLengthResponse(HttpResponse.Status.NOT_FOUND, ContentType.MIME_PLAINTEXT, "Not Found");
    }
}

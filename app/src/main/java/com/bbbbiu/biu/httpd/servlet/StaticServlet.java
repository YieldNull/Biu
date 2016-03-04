package com.bbbbiu.biu.httpd.servlet;

import android.content.Context;

import com.bbbbiu.biu.httpd.HttpRequest;
import com.bbbbiu.biu.httpd.HttpResponse;
import com.bbbbiu.biu.httpd.HttpServlet;
import com.bbbbiu.biu.httpd.util.ContentType;
import com.bbbbiu.biu.httpd.util.HtmlReader;

public class StaticServlet extends HttpServlet {
    public StaticServlet(Context context) {
        super(context);
    }

    @Override
    public HttpResponse doGet(HttpRequest request) {
        String uri = request.getUri().replaceFirst("/", "");
        String mime = uri.contains("css") ? ContentType.MIME_CSS : ContentType.MIME_JAVASCRIPT;

        String html = HtmlReader.readAll(context, uri);
        return HttpResponse.newResponse(HttpResponse.Status.OK, mime, html);
    }
}

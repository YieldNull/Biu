package com.bbbbiu.biu.http.server.servlets;

import android.content.Context;

import com.bbbbiu.biu.http.server.HttpRequest;
import com.bbbbiu.biu.http.server.HttpResponse;
import com.bbbbiu.biu.http.server.HttpServlet;
import com.bbbbiu.biu.http.server.ContentType;
import com.bbbbiu.biu.http.util.HtmlReader;

public class StaticServlet extends HttpServlet {
    public StaticServlet(Context context) {
        super(context);
    }

    @Override
    public HttpResponse doGet(HttpRequest request) {
        String uri = request.getUri().replaceFirst("/", "");
        String mime;

        if (uri.endsWith(".css")) {
            mime = ContentType.MIME_CSS;
        } else if (uri.endsWith(".js")) {
            mime = ContentType.MIME_JAVASCRIPT;
        } else {
            mime = ContentType.MIME_STREAM;
        }

        String html = HtmlReader.readAll(context, uri);
        return HttpResponse.newResponse(HttpResponse.Status.OK, mime, html);
    }
}

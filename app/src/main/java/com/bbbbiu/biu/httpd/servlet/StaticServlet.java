package com.bbbbiu.biu.httpd.servlet;

import android.content.Context;

import com.bbbbiu.biu.httpd.HttpRequest;
import com.bbbbiu.biu.httpd.HttpResponse;
import com.bbbbiu.biu.httpd.HttpServlet;

public class StaticServlet extends HttpServlet {
    public StaticServlet(Context context) {
        super(context);
    }

    @Override
    public HttpResponse doGet(HttpRequest request) {
        return super.doGet(request);
    }
}

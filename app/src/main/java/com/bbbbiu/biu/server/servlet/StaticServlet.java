package com.bbbbiu.biu.server.servlet;

import android.content.Context;

import com.bbbbiu.biu.server.HttpRequest;
import com.bbbbiu.biu.server.HttpResponse;
import com.bbbbiu.biu.server.HttpServlet;

public class StaticServlet extends HttpServlet {
    public StaticServlet(Context context) {
        super(context);
    }

    @Override
    public HttpResponse doGet(HttpRequest request) {
        return super.doGet(request);
    }
}

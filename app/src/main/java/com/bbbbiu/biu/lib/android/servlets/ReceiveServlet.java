package com.bbbbiu.biu.lib.android.servlets;

import android.content.Context;
import android.os.ResultReceiver;

import com.bbbbiu.biu.lib.httpd.HttpRequest;
import com.bbbbiu.biu.lib.httpd.HttpResponse;
import com.bbbbiu.biu.lib.httpd.HttpServlet;

/**
 * Created by YieldNull at 4/22/16
 */
public class ReceiveServlet extends HttpServlet {
    private static final String TAG = ReceiveServlet.class.getSimpleName();

    private ResultReceiver resultReceiver;

    public ReceiveServlet(Context context, ResultReceiver resultReceiver) {
        super(context);

        this.resultReceiver = resultReceiver;
    }

    @Override
    public HttpResponse doGet(HttpRequest request) {
        return null;
    }

    @Override
    public HttpResponse doPost(HttpRequest request) {
        return null;
    }
}

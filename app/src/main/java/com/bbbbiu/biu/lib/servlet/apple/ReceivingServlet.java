package com.bbbbiu.biu.lib.servlet.apple;

import android.content.Context;

import com.bbbbiu.biu.lib.httpd.HttpDaemon;
import com.bbbbiu.biu.lib.httpd.HttpRequest;
import com.bbbbiu.biu.lib.httpd.HttpResponse;
import com.bbbbiu.biu.lib.servlet.ReceivingBaseServlet;
import com.bbbbiu.biu.lib.util.HtmlReader;
import com.bbbbiu.biu.lib.util.HttpConstants;

public class ReceivingServlet extends ReceivingBaseServlet {
    private static final String TAG = ReceivingServlet.class.getSimpleName();

    private static ReceivingServlet sReceiveServlet;

    public static void register(Context context) {
        HttpDaemon.registerServlet(HttpConstants.Apple.URL_UPLOAD, getSingleton(context));
        HttpDaemon.registerServlet("/", getSingleton(context));
    }

    public static ReceivingServlet getSingleton(Context context) {
        return sReceiveServlet != null ? sReceiveServlet : (sReceiveServlet = new ReceivingServlet(context));
    }

    private ReceivingServlet(Context context) {
        super(context);
    }

    @Override
    public HttpResponse doGet(HttpRequest request) {
        String html = HtmlReader.readAll(context, "upload.html");
        return HttpResponse.newResponse(html);
    }

    @Override
    public HttpResponse doPost(HttpRequest request) {
        super.doPost(request);
        return HttpResponse.newRedirectResponse("/");
    }
}

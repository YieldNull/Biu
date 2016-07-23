package com.bbbbiu.biu.lib.servlet.apple;

import android.content.Context;
import com.bbbbiu.biu.lib.servlet.ReceivingBaseServlet;
import com.bbbbiu.biu.lib.util.HtmlReader;
import com.bbbbiu.biu.lib.util.HttpConstants;
import com.yieldnull.httpd.HttpDaemon;
import com.yieldnull.httpd.HttpRequest;
import com.yieldnull.httpd.HttpResponse;

public class ReceivingServlet extends ReceivingBaseServlet {
    private static final String TAG = ReceivingServlet.class.getSimpleName();


    public static void register(Context context) {
        ReceivingServlet receivingServlet = new ReceivingServlet(context);

        HttpDaemon.registerServlet(HttpConstants.Apple.URL_UPLOAD, receivingServlet);
        HttpDaemon.registerServlet("/", receivingServlet);
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
        return HttpResponse.newResponse("200 OK");
    }
}

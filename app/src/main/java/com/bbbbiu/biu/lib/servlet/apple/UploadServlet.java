package com.bbbbiu.biu.lib.servlet.apple;

import android.content.Context;

import com.bbbbiu.biu.lib.servlet.ReceivingBaseServlet;
import com.bbbbiu.biu.lib.util.HtmlReader;
import com.bbbbiu.biu.lib.util.HttpConstants;
import com.yieldnull.httpd.HttpDaemon;
import com.yieldnull.httpd.HttpRequest;
import com.yieldnull.httpd.HttpResponse;

/**
 * 显示上传页面，并处理POST文件上传
 */
public class UploadServlet extends ReceivingBaseServlet {

    public static void register(Context context) {
        UploadServlet receivingServlet = new UploadServlet(context);

        HttpDaemon.registerServlet(HttpConstants.Apple.URL_UPLOAD, receivingServlet);
        HttpDaemon.registerServlet("/", receivingServlet);
    }

    private UploadServlet(Context context) {
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

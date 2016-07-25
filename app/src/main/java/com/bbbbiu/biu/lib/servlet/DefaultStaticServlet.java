package com.bbbbiu.biu.lib.servlet;

import android.content.Context;
import android.webkit.MimeTypeMap;

import com.bbbbiu.biu.lib.util.HtmlReader;
import com.yieldnull.httpd.HttpDaemon;
import com.yieldnull.httpd.HttpRequest;
import com.yieldnull.httpd.HttpResponse;
import com.yieldnull.httpd.HttpServlet;

/**
 * 默认的静态资源 Servlet。处理的Uri为 “/static/*"
 */
public class DefaultStaticServlet extends HttpServlet {

    public static void register(Context context) {
        HttpServlet servlet = new DefaultStaticServlet(context);
        HttpDaemon.registerServlet("^/static/((?!/).)+/((?!/).)+$", servlet);
    }

    private Context context;

    private DefaultStaticServlet(Context context) {
        this.context = context;
    }

    @Override
    public HttpResponse doGet(HttpRequest request) {
        String uri = request.uri();

        String mime = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri));


        String html = HtmlReader.readAll(context, uri.replaceFirst("/", ""));

        if (html != null) {
            return HttpResponse.newResponse(mime, html);
        } else {
            return HttpResponse.newResponse(HttpResponse.Status.NOT_FOUND,
                    HttpResponse.Status.NOT_FOUND.getDescription());
        }
    }

    @Override
    public HttpResponse doPost(HttpRequest request) {
        return null;
    }
}

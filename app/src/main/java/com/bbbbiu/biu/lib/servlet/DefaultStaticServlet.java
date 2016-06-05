package com.bbbbiu.biu.lib.servlet;

import android.content.Context;
import android.webkit.MimeTypeMap;

import com.bbbbiu.biu.lib.httpd.HttpDaemon;
import com.bbbbiu.biu.lib.httpd.HttpRequest;
import com.bbbbiu.biu.lib.httpd.HttpResponse;
import com.bbbbiu.biu.lib.httpd.HttpServlet;
import com.bbbbiu.biu.lib.util.HtmlReader;

/**
 * 默认的静态资源 Servlet。处理的Uri为 “/static/*"
 */
public class DefaultStaticServlet extends HttpServlet {

    public static void register(Context context) {
        HttpServlet servlet = new DefaultStaticServlet(context);
        HttpDaemon.registerServlet("^/static/((?!/).)+/((?!/).)+$", servlet);
    }

    private DefaultStaticServlet(Context context) {
        super(context);
    }

    @Override
    public HttpResponse doGet(HttpRequest request) {
        String uri = request.getUri();

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

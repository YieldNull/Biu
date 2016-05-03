package com.bbbbiu.biu.lib.httpd.util;

import android.content.Context;
import android.webkit.MimeTypeMap;

import com.bbbbiu.biu.lib.httpd.ContentType;
import com.bbbbiu.biu.lib.httpd.HttpDaemon;
import com.bbbbiu.biu.lib.httpd.HttpRequest;
import com.bbbbiu.biu.lib.httpd.HttpResponse;
import com.bbbbiu.biu.lib.httpd.HttpServlet;
import com.bbbbiu.biu.lib.util.HtmlReader;

/**
 * 默认的静态资源 Servlet。处理的Uri为 “/static/*"
 */
public class DefaultStaticServlet extends HttpServlet {
    public DefaultStaticServlet(Context context) {
        super(context);
    }

    @Override
    public HttpResponse doGet(HttpRequest request) {
        String uri = request.getUri();

        String mime = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri));


        String html = HtmlReader.readAll(context, uri);

        if (html != null) {
            return HttpResponse.newResponse(HttpResponse.Status.OK, mime, html);
        } else {
            return HttpResponse.newResponse(HttpResponse.Status.NOT_FOUND, ContentType.MIME_PLAINTEXT,
                    HttpResponse.Status.NOT_FOUND.getDescription());
        }
    }

    @Override
    public HttpResponse doPost(HttpRequest request) {
        return null;
    }

    public static void register(Context context) {
        HttpServlet servlet = new DefaultStaticServlet(context);
        HttpDaemon.registerServlet(HttpDaemon.STATIC_FILE_REG, servlet);
    }
}

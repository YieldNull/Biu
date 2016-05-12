package com.bbbbiu.biu.lib.servlet.apple;

import android.content.Context;

import com.bbbbiu.biu.lib.httpd.HttpDaemon;
import com.bbbbiu.biu.lib.httpd.HttpRequest;
import com.bbbbiu.biu.lib.httpd.HttpResponse;
import com.bbbbiu.biu.lib.httpd.HttpServlet;

/**
 * Created by YieldNull at 5/13/16
 */
public class FileServlet extends HttpServlet {
    private static final String TAG = FileServlet.class.getSimpleName();

    private static FileServlet sFileServlet;

    public static void register(Context context) {
        HttpDaemon.registerServlet("/download/.+", getSingleton(context));
    }

    public static FileServlet getSingleton(Context context) {
        if (sFileServlet == null) {
            sFileServlet = new FileServlet(context);
        }

        return sFileServlet;
    }

    private FileServlet(Context context) {
        super(context);
    }

    @Override
    public HttpResponse doGet(HttpRequest request) {
        return HttpResponse.newResponse(request.getUri());
    }

    @Override
    public HttpResponse doPost(HttpRequest request) {
        return null;
    }
}

package com.bbbbiu.biu.lib.servlet.apple;

import android.content.Context;
import android.util.Log;

import com.bbbbiu.biu.lib.httpd.HttpDaemon;
import com.bbbbiu.biu.lib.httpd.HttpRequest;
import com.bbbbiu.biu.lib.httpd.HttpResponse;
import com.bbbbiu.biu.lib.httpd.HttpServlet;
import com.bbbbiu.biu.lib.util.HtmlReader;
import com.bbbbiu.biu.util.PreferenceUtil;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by YieldNull at 5/12/16
 */
public class DownloadServlet extends HttpServlet {
    private static final String TAG = DownloadServlet.class.getSimpleName();


    private static DownloadServlet sDownloadServlet;

    public static void register(Context context) {
        HttpDaemon.registerServlet("/download", getSingleton(context));
        HttpDaemon.registerServlet("/", getSingleton(context));
    }

    private static DownloadServlet getSingleton(Context context) {
        if (sDownloadServlet == null) {
            sDownloadServlet = new DownloadServlet(context);
        }

        return sDownloadServlet;
    }

    private DownloadServlet(Context context) {
        super(context);
    }

    @Override
    public HttpResponse doGet(HttpRequest request) {
        String html = HtmlReader.readAll(context, "download.html");

        Template template = Mustache.compiler().compile(html);

        List<File> fileList = new ArrayList<>();

        for (String path : PreferenceUtil.getFilesToSend(context)) {
            fileList.add(new File(path));
        }
        Map<String, List<File>> map = new HashMap<>();
        map.put("files", fileList);

        html = template.execute(map);

        Log.i(TAG, html);
        return HttpResponse.newResponse(html);
    }

    @Override
    public HttpResponse doPost(HttpRequest request) {
        return null;
    }

}

package com.bbbbiu.biu.lib.servlet.apple;

import android.content.Context;
import android.util.Log;

import com.bbbbiu.biu.gui.transfer.FileItem;
import com.bbbbiu.biu.lib.util.HtmlReader;
import com.bbbbiu.biu.lib.util.HttpConstants;
import com.bbbbiu.biu.util.PreferenceUtil;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import com.yieldnull.httpd.HttpDaemon;
import com.yieldnull.httpd.HttpRequest;
import com.yieldnull.httpd.HttpResponse;
import com.yieldnull.httpd.HttpServlet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 显示可供IOS端下载的文件清单
 * <p/>
 * Created by YieldNull at 5/12/16
 */
public class DownloadServlet extends HttpServlet {
    private static final String TAG = DownloadServlet.class.getSimpleName();

    public static void register(Context context) {
        DownloadServlet downloadServlet = new DownloadServlet(context);

        HttpDaemon.registerServlet(HttpConstants.Apple.URL_DOWNLOAD, downloadServlet);
        HttpDaemon.registerServlet("/", downloadServlet);
    }

    private Context context;

    private DownloadServlet(Context context) {
        this.context = context;
    }

    @Override
    public HttpResponse doGet(HttpRequest request) {
        String html = HtmlReader.readAll(context, "download.html");

        Template template = Mustache.compiler().compile(html);

        Map<String, List<FileItem>> map = new HashMap<>();
        map.put("files", PreferenceUtil.getFileItemsToSend(context));

        html = template.execute(map);

        Log.i(TAG, html);
        return HttpResponse.newResponse(html);
    }

    @Override
    public HttpResponse doPost(HttpRequest request) {
        return null;
    }

}

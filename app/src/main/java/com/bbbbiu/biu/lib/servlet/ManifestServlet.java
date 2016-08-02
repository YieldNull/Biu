package com.bbbbiu.biu.lib.servlet;

import android.content.Context;
import android.util.Log;

import com.bbbbiu.biu.gui.transfer.FileItem;
import com.bbbbiu.biu.lib.HttpConstants;
import com.bbbbiu.biu.util.StorageUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yieldnull.httpd.HttpDaemon;
import com.yieldnull.httpd.HttpRequest;
import com.yieldnull.httpd.HttpResponse;
import com.yieldnull.httpd.HttpServlet;
import com.yieldnull.httpd.Streams;

import java.util.ArrayList;

/**
 * 苹果，安卓，接收文件清单
 * <p/>
 * Created by YieldNull at 4/22/16
 */
public class ManifestServlet extends HttpServlet {
    private static final String TAG = ManifestServlet.class.getSimpleName();

    public static void register(Context context, boolean forAndroid) {
        HttpDaemon.registerServlet(HttpConstants.URL_MANIFEST, new ManifestServlet(context, forAndroid));
    }

    private Context context;
    private boolean forAndroid;

    private ManifestServlet(Context context, boolean forAndroid) {
        this.context = context;
        this.forAndroid = forAndroid;
    }

    @Override
    public HttpResponse doGet(HttpRequest request) {
        return null;
    }

    @Override
    public HttpResponse doPost(HttpRequest request) {
        Gson gson = new Gson();

        String json = request.text();
        ArrayList<FileItem> manifest = gson.fromJson(json, new TypeToken<ArrayList<FileItem>>() {
        }.getType());

        Log.i(TAG, json);

        for (FileItem item : manifest) {
            String name = Streams.verifyFileName(item.name);

            item.name = Streams.genVersionedFileName(StorageUtil.getDownloadDir(context), name);
        }

        if (forAndroid) {
            com.bbbbiu.biu.gui.transfer.android.ReceivingActivity.startReceiving(context, manifest);
        } else {
            com.bbbbiu.biu.gui.transfer.apple.ReceivingActivity.startReceiving(context, manifest);
        }
        return HttpResponse.newResponse("");
    }
}

package com.bbbbiu.biu.lib.android.servlets;

import android.content.Context;
import android.util.Log;

import com.bbbbiu.biu.lib.httpd.HttpDaemon;
import com.bbbbiu.biu.lib.httpd.HttpRequest;
import com.bbbbiu.biu.lib.httpd.HttpResponse;
import com.bbbbiu.biu.lib.httpd.HttpServlet;
import com.bbbbiu.biu.lib.httpd.upload.FileItem;
import com.bbbbiu.biu.lib.httpd.upload.FileItemFactory;
import com.bbbbiu.biu.lib.httpd.upload.FileUpload;
import com.bbbbiu.biu.lib.httpd.upload.exceptions.FileUploadException;
import com.bbbbiu.biu.lib.util.HttpConstants;
import com.bbbbiu.biu.util.StorageUtil;

import java.io.File;
import java.util.List;

/**
 * Created by YieldNull at 4/22/16
 */
public class ReceiveServlet extends HttpServlet {
    private static final String TAG = ReceiveServlet.class.getSimpleName();

    private static ReceiveServlet sReceiveServlet;

    public static void register(Context context) {
        HttpDaemon.registerServlet(HttpConstants.Android.URL_UPLOAD, getSingleton(context));
    }

    public static ReceiveServlet getSingleton(Context context) {
        return sReceiveServlet != null ? sReceiveServlet : (sReceiveServlet = new ReceiveServlet(context));
    }

    private ReceiveServlet(Context context) {
        super(context);
    }

    @Override
    public HttpResponse doGet(HttpRequest request) {
        return null;
    }

    @Override
    public HttpResponse doPost(HttpRequest request) {
        File downloadDir = StorageUtil.getDownloadDir(context);

        //TODO 判断可用空间大小
        FileItemFactory factory = new FileItemFactory(0, downloadDir);

        FileUpload fileUpload = new FileUpload(factory);



        List<FileItem> items;

        try {
            items = fileUpload.parseRequest(request);
        } catch (FileUploadException e) {
            Log.w(TAG, e.toString());
            return null;
        }

        for (FileItem item : items) {
            Log.i(TAG, "Uploading file " + item.getName() + " to " + downloadDir.getAbsolutePath());
        }

        return HttpResponse.newResponse("200 OK");
    }
}

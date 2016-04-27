package com.bbbbiu.biu.lib.apple.servlets;

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
import com.bbbbiu.biu.lib.util.HtmlReader;
import com.bbbbiu.biu.util.StorageUtil;

import java.io.File;
import java.util.List;

public class UploadServlet extends HttpServlet {
    private static final String TAG = UploadServlet.class.getSimpleName();

    private UploadServlet(Context context) {
        super(context);

    }

    public static void register(Context context) {
        HttpServlet servlet = new UploadServlet(context);
        HttpDaemon.registerServlet("/upload", servlet);
    }

    @Override
    public HttpResponse doGet(HttpRequest request) {
        String html = HtmlReader.readAll(context, "upload.html");
        return HttpResponse.newResponse(html);
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
        return HttpResponse.newResponse("Success Uploaded");
    }
}

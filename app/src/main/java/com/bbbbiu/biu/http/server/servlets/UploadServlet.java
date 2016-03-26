package com.bbbbiu.biu.http.server.servlets;

import android.content.Context;
import android.util.Log;

import com.bbbbiu.biu.http.server.HttpDaemon;
import com.bbbbiu.biu.http.server.HttpRequest;
import com.bbbbiu.biu.http.server.HttpResponse;
import com.bbbbiu.biu.http.server.HttpServlet;
import com.bbbbiu.biu.http.server.upload.FileItem;
import com.bbbbiu.biu.http.server.upload.FileItemFactory;
import com.bbbbiu.biu.http.server.upload.FileUpload;
import com.bbbbiu.biu.http.server.upload.exceptions.FileUploadException;
import com.bbbbiu.biu.http.util.HtmlReader;
import com.bbbbiu.biu.util.Storage;

import java.io.File;
import java.util.List;

public class UploadServlet extends HttpServlet {
    private static final String TAG = UploadServlet.class.getSimpleName();

    private UploadServlet(Context context) {
        super(context);

    }

    public static void register(Context context) {
        HttpServlet servlet = new UploadServlet(context);
        HttpDaemon.regServlet("/upload", servlet);
    }

    @Override
    public HttpResponse doGet(HttpRequest request) {
        String html = HtmlReader.readAll(context, "upload.html");
        return HttpResponse.newResponse(html);
    }

    @Override
    public HttpResponse doPost(HttpRequest request) {
        File downloadDir = Storage.getDownloadDir(context);

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

package com.bbbbiu.biu.server.servlet;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.bbbbiu.biu.server.HttpDaemon;
import com.bbbbiu.biu.server.HttpRequest;
import com.bbbbiu.biu.server.HttpResponse;
import com.bbbbiu.biu.server.HttpServlet;
import com.bbbbiu.biu.server.upload.FileItem;
import com.bbbbiu.biu.server.upload.FileUploadException;
import com.bbbbiu.biu.server.upload.HttpdFileUpload;
import com.bbbbiu.biu.server.upload.disk.DiskFileItemFactory;
import com.bbbbiu.biu.util.StorageManager;

import java.io.File;
import java.util.List;

public class UploadServlet extends HttpServlet {
    private static final String TAG = UploadServlet.class.getSimpleName();

    private UploadServlet(Context context) {
        super(context);

    }

    public static void register(Context context) {
        HttpServlet servlet = new UploadServlet(context);
        HttpDaemon.clearServlet();
        HttpDaemon.regServlet("/", servlet);
        HttpDaemon.regServlet("/upload", servlet);
    }

    @Override
    public HttpResponse doGet(HttpRequest request) {
        String html = "<html charset=\"utf-8\">" +
                "<body><form method=\"POST\"  enctype=\"multipart/form-data\" action=\"/upload\">\n" +
                "  File to upload: <input type=\"file\" name=\"upfile\"><br/>\n" +
                "  <input type=\"submit\" value=\"Press\"> to upload the file!\n" +
                "</form></body></html>";

        return HttpResponse.newFixedLengthResponse(html);
    }

    @Override
    public HttpResponse doPost(HttpRequest request) {
        File downloadDir = StorageManager.getDownloadDir(context);

        //TODO 判断可用空间大小
        DiskFileItemFactory factory = new DiskFileItemFactory(0, downloadDir);

        HttpdFileUpload fileUpload = new HttpdFileUpload(factory);
        List<FileItem> items;

        try {
            items = fileUpload.parseRequest(request);
        } catch (FileUploadException e) {
            Log.w(TAG, e.toString());
            return null;
        }

        for (FileItem item : items) {
            Log.i(TAG, item.getName());

        }

        Log.i(TAG, "Upload file to " + downloadDir.getAbsolutePath());
        return HttpResponse.newFixedLengthResponse("Success Uploaded");
    }
}

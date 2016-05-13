package com.bbbbiu.biu.lib.servlet;

import android.content.Context;
import android.util.Log;

import com.bbbbiu.biu.db.transfer.RevRecord;
import com.bbbbiu.biu.lib.httpd.HttpRequest;
import com.bbbbiu.biu.lib.httpd.HttpResponse;
import com.bbbbiu.biu.lib.httpd.upload.FileItem;
import com.bbbbiu.biu.lib.httpd.upload.FileItemFactory;
import com.bbbbiu.biu.lib.httpd.upload.FileUpload;
import com.bbbbiu.biu.lib.httpd.upload.exceptions.FileUploadException;
import com.bbbbiu.biu.util.StorageUtil;

import java.io.File;
import java.util.List;

/**
 * Created by YieldNull at 5/9/16
 */
public class ReceivingBaseServlet extends ProgressBaseServlet {
    private static final String TAG = ReceivingBaseServlet.class.getSimpleName();

    public ReceivingBaseServlet(Context context) {
        super(context);
    }

    @Override
    public HttpResponse doPost(HttpRequest request) {
        File downloadDir = StorageUtil.getDownloadDir(context);

        //TODO 判断可用空间大小
        FileItemFactory factory = new FileItemFactory(0, downloadDir);

        FileUpload fileUpload = new FileUpload(factory);

        fileUpload.setProgressListener(getProgressListener());

        List<FileItem> items;

        try {
            items = fileUpload.parseRequest(request);
        } catch (FileUploadException e) {
            Log.w(TAG, e.toString());

            sendFailureBroadcast();

            return HttpResponse.newResponse(HttpResponse.Status.INTERNAL_ERROR,
                    HttpResponse.Status.INTERNAL_ERROR.getDescription());
        }

        for (FileItem item : items) {
            Log.i(TAG, "Uploading file " + item.getName() + " to " + downloadDir.getAbsolutePath());


            new RevRecord(
                    System.currentTimeMillis(),
                    item.getName(),
                    item.getSize()).save();
        }

        sendSuccessBroadcast();

        return HttpResponse.newResponse("200 OK");
    }
}

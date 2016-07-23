package com.bbbbiu.biu.lib.servlet;

import android.content.Context;
import android.util.Log;

import com.bbbbiu.biu.db.transfer.RevRecord;
import com.bbbbiu.biu.util.StorageUtil;
import com.yieldnull.httpd.HttpRequest;
import com.yieldnull.httpd.HttpResponse;
import com.yieldnull.httpd.upload.FileItem;
import com.yieldnull.httpd.upload.FileItemFactory;
import com.yieldnull.httpd.upload.FileUpload;

import java.io.File;
import java.io.IOException;
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
        } catch (IOException e) {
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

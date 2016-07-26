package com.bbbbiu.biu.lib.servlet;

import android.content.Context;
import android.util.Log;

import com.bbbbiu.biu.db.transfer.RevRecord;
import com.bbbbiu.biu.lib.util.HttpConstants;
import com.bbbbiu.biu.util.StorageUtil;
import com.yieldnull.httpd.HttpRequest;
import com.yieldnull.httpd.HttpResponse;

import java.io.File;
import java.util.List;

/**
 * 基类：用来接收文件
 * <p/>
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

        request.parseMultipartBody(downloadDir, getProgressListener());

        String fileUri = request.form().get(HttpConstants.FILE_URI);
        List<File> files = request.files();

        if (files.size() != 1) {
            sendFailureBroadcast(fileUri);

            return HttpResponse.newResponse(HttpResponse.Status.BAD_REQUEST,
                    HttpResponse.Status.BAD_REQUEST.getDescription());
        }

        File file = files.get(0);
        Log.i(TAG, "Uploading file " + file.getName() + " to " + downloadDir.getAbsolutePath());

        new RevRecord(
                System.currentTimeMillis(),
                file.getName(),
                file.length()).save();

        sendSuccessBroadcast(fileUri);

        return HttpResponse.newResponse("200 OK");

    }
}

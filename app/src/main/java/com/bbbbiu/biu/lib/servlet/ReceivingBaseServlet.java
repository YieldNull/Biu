package com.bbbbiu.biu.lib.servlet;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.bbbbiu.biu.gui.transfer.TransferBaseActivity;
import com.bbbbiu.biu.lib.httpd.ContentType;
import com.bbbbiu.biu.lib.httpd.HttpRequest;
import com.bbbbiu.biu.lib.httpd.HttpResponse;
import com.bbbbiu.biu.lib.httpd.HttpServlet;
import com.bbbbiu.biu.lib.httpd.upload.FileItem;
import com.bbbbiu.biu.lib.httpd.upload.FileItemFactory;
import com.bbbbiu.biu.lib.httpd.upload.FileUpload;
import com.bbbbiu.biu.lib.httpd.upload.exceptions.FileUploadException;
import com.bbbbiu.biu.lib.httpd.util.ProgressListener;
import com.bbbbiu.biu.lib.util.ProgressListenerImpl;
import com.bbbbiu.biu.util.StorageUtil;

import java.io.File;
import java.util.List;

/**
 * Created by YieldNull at 5/9/16
 */
public class ReceivingBaseServlet extends HttpServlet {
    private static final String TAG = ReceivingBaseServlet.class.getSimpleName();

    public ReceivingBaseServlet(Context context) {
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

        fileUpload.setProgressListener(new ProgressListener() {
            private int mCurrentProgress;

            @Override
            public void update(long pBytesRead, long pContentLength, int pItems) {

                int progress = (int) (pBytesRead * 100.0 / pContentLength);

                // 更新进度(0-100)
                if (progress > mCurrentProgress) {
                    mCurrentProgress = progress;

                    sendProgressBroadcast(progress);
                }
            }
        });


        List<FileItem> items;

        try {
            items = fileUpload.parseRequest(request);
        } catch (FileUploadException e) {
            Log.w(TAG, e.toString());

            sendFailureBroadcast();

            return HttpResponse.newResponse(HttpResponse.Status.INTERNAL_ERROR,
                    ContentType.MIME_PLAINTEXT,
                    HttpResponse.Status.INTERNAL_ERROR.getDescription());
        }

        for (FileItem item : items) {
            Log.i(TAG, "Uploading file " + item.getName() + " to " + downloadDir.getAbsolutePath());
        }

        sendSuccessBroadcast();

        return HttpResponse.newResponse("200 OK");
    }


    protected void sendProgressBroadcast(int progress) {
        Bundle bundle = new Bundle();
        bundle.putInt(ProgressListenerImpl.RESULT_EXTRA_PROGRESS, progress);
        bundle.putString(ProgressListenerImpl.RESULT_EXTRA_FILE_URI, null);


        Intent intent = new Intent(TransferBaseActivity.ACTION_UPDATE_PROGRESS);
        intent.putExtra(TransferBaseActivity.EXTRA_RESULT_CODE, ProgressListenerImpl.RESULT_PROGRESS);
        intent.putExtra(TransferBaseActivity.EXTRA_RESULT_BUNDLE, bundle);

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    protected void sendFailureBroadcast() {
        Bundle bundle = new Bundle();
        bundle.putString(ProgressListenerImpl.RESULT_EXTRA_FILE_URI, null);

        Intent intent = new Intent(TransferBaseActivity.ACTION_UPDATE_PROGRESS);
        intent.putExtra(TransferBaseActivity.EXTRA_RESULT_CODE, ProgressListenerImpl.RESULT_FAILED);
        intent.putExtra(TransferBaseActivity.EXTRA_RESULT_BUNDLE, bundle);

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }


    protected void sendSuccessBroadcast() {
        Bundle bundle = new Bundle();
        bundle.putString(ProgressListenerImpl.RESULT_EXTRA_FILE_URI, null);

        Intent intent = new Intent(TransferBaseActivity.ACTION_UPDATE_PROGRESS);
        intent.putExtra(TransferBaseActivity.EXTRA_RESULT_CODE, ProgressListenerImpl.RESULT_SUCCEEDED);
        intent.putExtra(TransferBaseActivity.EXTRA_RESULT_BUNDLE, bundle);

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}

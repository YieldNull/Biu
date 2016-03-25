package com.bbbbiu.biu.service;

import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;

import com.bbbbiu.biu.gui.ReceiveActivity;
import com.bbbbiu.biu.http.client.FileItem;
import com.bbbbiu.biu.http.client.HttpConstants;
import com.bbbbiu.biu.http.util.ProgressListener;
import com.bbbbiu.biu.http.util.ProgressNotifier;
import com.bbbbiu.biu.http.util.Streams;
import com.bbbbiu.biu.util.StorageUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.Request;
import okhttp3.Response;

public class DownloadService extends Service implements ProgressListener {
    private static final String TAG = DownloadService.class.getSimpleName();

    private static final String EXTRA_FILE_ITEM = "com.bbbbiu.biu.service.extra.FILE_ITEM";
    private static final String EXTRA_RECEIVER = "com.bbbbiu.biu.service.extra.RECEIVER";

    private static final String ACTION_SET_RECEIVER = "com.bbbbiu.biu.service.action.ACTION_SET_RECEIVER";
    private static final String ACTION_START_DOWNLOAD = "com.bbbbiu.biu.service.action.ACTION_START_DOWNLOAD";

    private ResultReceiver mResultReceiver;
    private HandlerThread mWorkerThread;
    private Handler mHandler;

    private FileItem mCurrentFile;
    private int mCurrentProgress;

    public static final int RESULT_PROGRESS = 0;
    public static final int RESULT_FAILED = 1;
    public static final int RESULT_SUCCESS = 2;

    public static void setResultReceiver(Context context, ResultReceiver resultReceiver) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.putExtra(EXTRA_RECEIVER, resultReceiver);
        intent.setAction(ACTION_SET_RECEIVER);
        context.startService(intent);
    }

    public static void startDownload(Context context, FileItem fileItem) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.putExtra(EXTRA_FILE_ITEM, fileItem);
        intent.setAction(ACTION_START_DOWNLOAD);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return Service.START_STICKY;
        }
        if (intent.getAction().equals(ACTION_SET_RECEIVER)) {
            mResultReceiver = intent.getParcelableExtra(EXTRA_RECEIVER);

        } else if (intent.getAction().equals(ACTION_START_DOWNLOAD)) {
            final FileItem fileItem = intent.getParcelableExtra(EXTRA_FILE_ITEM);

            if (mWorkerThread == null) {
                mWorkerThread = new HandlerThread("FileDownloadThread");
                mWorkerThread.start();
                mHandler = new Handler(mWorkerThread.getLooper());
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "Received download command");
                    Log.i(TAG, "Start downloading file " + fileItem.getName());
                    boolean succeeded = downloadFile(fileItem);
                    if (succeeded) {
                        mResultReceiver.send(RESULT_SUCCESS, null);
                    } else {
                        mResultReceiver.send(RESULT_FAILED, null);
                    }
                }
            });
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private boolean downloadFile(FileItem fileItem) {
        Request request = HttpConstants.newFileDownloadRequest(fileItem.getUrl(), fileItem.getUid());
        File reposition = StorageUtil.getDownloadDir(getApplicationContext());
        File destFile = new File(reposition, fileItem.getName());

        mCurrentFile = fileItem;
        mCurrentProgress = 0;

        Response response;

        try {
            response = HttpConstants.getHttpClient().newCall(request).execute();
        } catch (IOException e) {
            Log.i(TAG, "Download file failed. " + destFile.getName() + "  HTTP error" + e.toString());
            return false;
        }

        if (response.code() != 200) {
            Log.i(TAG, "Get file list failed. Response status code " + response.code());
            return false;
        }

        FileOutputStream fileOutStream = null;
        try {
            fileOutStream = new FileOutputStream(destFile);
            ProgressNotifier notifier = new ProgressNotifier(this, fileItem.getSize());
            Streams.copy(response.body().byteStream(), fileOutStream, true, notifier);
            Log.i(TAG, "Finish downloading file " + fileItem.getName());
        } catch (IOException e) {
            Log.w(TAG, "Store file failed", e);
        } finally {
            response.body().close();
        }
        return true;
    }

    @Override
    public void update(long pBytesRead, long pContentLength, int pItems) {
        int progress = (int) (pBytesRead * 100.0 / pContentLength);

        if (progress > mCurrentProgress) {
            mCurrentProgress = progress;

            Bundle bundle = new Bundle();
            bundle.putInt(ReceiveActivity.EXTRA_PROGRESS, progress);
            bundle.putParcelable(ReceiveActivity.EXTRA_FILE_ITEM, mCurrentFile);
            mResultReceiver.send(RESULT_PROGRESS, bundle);
        }
    }
}

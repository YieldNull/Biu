package com.bbbbiu.biu.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;

import com.bbbbiu.biu.gui.UploadActivity;
import com.bbbbiu.biu.http.client.HttpConstants;
import com.bbbbiu.biu.http.util.ProgressListener;

import java.io.File;
import java.io.IOException;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class UploadService extends Service implements ProgressListener {
    private static final String TAG = UploadService.class.getSimpleName();

    private static final String EXTRA_FILE_PATH = "com.bbbbiu.biu.service.UploadService.extra.FILE_PATH";
    private static final String EXTRA_RECEIVER = "com.bbbbiu.biu.service.UploadService.extra.RECEIVER";
    private static final String EXTRA_UID = "com.bbbbiu.biu.service.UploadService.extra.UID";

    private static final String ACTION_SET_RECEIVER = "com.bbbbiu.biu.service.UploadService.action.ACTION_SET_RECEIVER";
    private static final String ACTION_START_UPLOAD = "com.bbbbiu.biu.service.UploadService.action.ACTION_START_UPLOAD";

    private ResultReceiver mResultReceiver;
    private HandlerThread mWorkerThread;
    private Handler mHandler;

    private File mCurrentFile;
    private int mCurrentProgress;

    public static final int RESULT_PROGRESS = 0;
    public static final int RESULT_FAILED = 1;
    public static final int RESULT_SUCCESS = 2;


    public static void setReceiver(Context context, ResultReceiver resultReceiver) {
        Intent intent = new Intent(context, UploadService.class);
        intent.putExtra(EXTRA_RECEIVER, resultReceiver);
        intent.setAction(ACTION_SET_RECEIVER);
        context.startService(intent);
    }

    public static void startUpload(Context context, String uid, String filePath) {
        Intent intent = new Intent(context, UploadService.class);
        intent.putExtra(EXTRA_FILE_PATH, filePath);
        intent.putExtra(EXTRA_UID, uid);
        intent.setAction(ACTION_START_UPLOAD);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return Service.START_STICKY;
        }
        if (intent.getAction().equals(ACTION_SET_RECEIVER)) {
            mResultReceiver = intent.getParcelableExtra(EXTRA_RECEIVER);
            Log.i(TAG, "Set ResultReceiver");

        } else if (intent.getAction().equals(ACTION_START_UPLOAD)) {
            final String uid = intent.getStringExtra(EXTRA_UID);
            final String filePath = intent.getStringExtra(EXTRA_FILE_PATH);
            final File file = new File(filePath);

            if (mWorkerThread == null) {
                mWorkerThread = new HandlerThread("FileUploadThread");
                mWorkerThread.start();
                mHandler = new Handler(mWorkerThread.getLooper());
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "Start uploading file " + file.getName());

                    boolean succeeded = uploadFile(uid, file);
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

    private boolean uploadFile(String uid, File file) {
        mCurrentFile = file;
        mCurrentProgress = 0;

        Request request = HttpConstants.newFileUploadRequest(uid, file);

        Response response;
        ResponseBody body = null;


        try {
            response = HttpConstants.newHttpClient().newCall(request).execute();
            body = response.body();
        } catch (IOException e) {
            Log.i(TAG, "Upload file failed. " + file.getName() + "  HTTP error" + e.toString());
            return false;
        } finally {
            if (body != null) {
                body.close();
            }
        }

        if (response.code() != 200) {
            Log.i(TAG, "Upload file failed. Response status code " + response.code());
            return false;
        }

        Log.i(TAG, "Upload file succeeded " + file.getName());

        return true;
    }

    @Override
    public void update(long pBytesRead, long pContentLength, int pItems) {
        int progress = (int) (pBytesRead * 100.0 / pContentLength);

        if (progress > mCurrentProgress) {
            mCurrentProgress = progress;

            Bundle bundle = new Bundle();
            bundle.putInt(UploadActivity.EXTRA_PROGRESS, progress);
            bundle.putString(UploadActivity.EXTRA_FILE_PATH, mCurrentFile.getAbsolutePath());
            mResultReceiver.send(RESULT_PROGRESS, bundle);
        }
    }
}

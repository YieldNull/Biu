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

import com.bbbbiu.biu.db.transfer.RevRecord;
import com.bbbbiu.biu.lib.util.HttpManager;
import com.bbbbiu.biu.util.StorageUtil;
import com.yieldnull.httpd.ProgressListener;
import com.yieldnull.httpd.ProgressNotifier;
import com.yieldnull.httpd.Streams;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 从服务器下载
 */
public class DownloadService extends Service {
    private static final String TAG = DownloadService.class.getSimpleName();

    private static final String EXTRA_DOWNLOAD_URL = "com.bbbbiu.biu.service.DownloadService.extra.DOWNLOAD_URL";
    private static final String EXTRA_FILE_SIZE = "com.bbbbiu.biu.service.DownloadService.extra.FILE_SIZE";
    private static final String EXTRA_FILE_NAME = "com.bbbbiu.biu.service.DownloadService.extra.FILE_NAME";
    private static final String EXTRA_RESULT_RECEIVER = "com.bbbbiu.biu.service.DownloadService.extra.RECEIVER";

    private static final String ACTION_START_DOWNLOAD = "com.bbbbiu.biu.service.computer.DownloadService.action.ACTION_START_DOWNLOAD";

    private HandlerThread mWorkerThread;
    private Handler mHandler;

    private Call mCurrentHttpCall;


    /**
     * 开始下载
     *
     * @param context        context
     * @param downloadUrl    下载URL
     * @param fileName       存到文件系统中的文件名
     * @param fileSize       文件大小 bytes
     * @param resultReceiver {@link ResultReceiver}
     */
    public static void addTask(Context context, String downloadUrl, String fileName, long fileSize, ResultReceiver resultReceiver) {
        Intent intent = new Intent(context, DownloadService.class);

        intent.putExtra(EXTRA_DOWNLOAD_URL, downloadUrl);
        intent.putExtra(EXTRA_FILE_NAME, fileName);
        intent.putExtra(EXTRA_FILE_SIZE, fileSize);
        intent.putExtra(EXTRA_RESULT_RECEIVER, resultReceiver);

        intent.setAction(ACTION_START_DOWNLOAD);

        context.startService(intent);
    }


    /**
     * 终止下载并关闭Service
     *
     * @param context context
     */
    public static void stopService(Context context) {
        Log.i(TAG,"Stopping Service");

        Intent intent = new Intent(context, UploadService.class);

        context.stopService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return Service.START_STICKY;
        } else if (intent.getAction().equals(ACTION_START_DOWNLOAD)) {
            final String downloadUrl = intent.getStringExtra(EXTRA_DOWNLOAD_URL);
            final String fileName = intent.getStringExtra(EXTRA_FILE_NAME);
            final long fileSize = intent.getLongExtra(EXTRA_FILE_SIZE, 0);
            final ResultReceiver resultReceiver = intent.getParcelableExtra(EXTRA_RESULT_RECEIVER);

            if (mWorkerThread == null) {
                mWorkerThread = new HandlerThread("FileDownloadThread");
                mWorkerThread.start();
                mHandler = new Handler(mWorkerThread.getLooper());
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "Start downloading file " + downloadUrl);

                    ProgressListenerImpl progressListener = new ProgressListenerImpl(downloadUrl, resultReceiver);

                    boolean succeeded = downloadFile(downloadUrl, fileName, fileSize, progressListener);

                    Bundle bundle = new Bundle();
                    bundle.putString(ProgressListenerImpl.RESULT_EXTRA_FILE_URI, downloadUrl);

                    if (succeeded) {
                        Log.i(TAG, "Finish downloading");
                        resultReceiver.send(ProgressListenerImpl.RESULT_SUCCEEDED, bundle);
                    } else {
                        Log.i(TAG, "Downloading failed");

                        resultReceiver.send(ProgressListenerImpl.RESULT_FAILED, bundle);
                    }
                }
            });
        }

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.i(TAG, "onDestroy");

        if (mCurrentHttpCall != null && mCurrentHttpCall.isExecuted()) {
            Log.i(TAG, "Canceling current task");
            mCurrentHttpCall.cancel();
        }

        if (mHandler != null) {
            Log.i(TAG, "Quit working thread");

            mHandler.removeCallbacksAndMessages(null);
            mHandler.getLooper().quit();
        }

    }

    /**
     * 下载文件
     *
     * @param downloadUrl      下载URL
     * @param fileName         文件名
     * @param fileSize         文件大小 bytes
     * @param progressListener 监听下载进度
     * @return 是否成功下载
     */
    private boolean downloadFile(String downloadUrl, String fileName, long fileSize, ProgressListener progressListener) {

        Request request = HttpManager.newRequest(downloadUrl);
        File reposition = StorageUtil.getDownloadDir(getApplicationContext());

        File destFile = new File(reposition, fileName);

        Response response;
        ResponseBody body = null;

        try {
            try {
                mCurrentHttpCall = HttpManager.newHttpClient().newCall(request);
                response = mCurrentHttpCall.execute();

                body = response.body();

            } catch (IOException e) {
                Log.i(TAG, "Download file failed. " + downloadUrl + "  HTTP error" + e.toString());
                return false;
            }

            if (response.code() != 200) {
                Log.i(TAG, "Get file list failed. Response status code " + response.code());
                return false;
            }

            FileOutputStream fileOutStream;
            try {
                fileOutStream = new FileOutputStream(destFile);
                ProgressNotifier notifier = new ProgressNotifier(progressListener, fileSize);

                // copy getStream
                Streams.copy(response.body().byteStream(), fileOutStream, true, notifier);

                Log.i(TAG, "Finish downloading file " + downloadUrl);

            } catch (IOException e) {
                Log.w(TAG, "Store file failed", e);
            }
        } finally {
            if (body != null) {
                body.close();
            }
        }

        //存到数据库
        new RevRecord(
                System.currentTimeMillis(),
                destFile.getName(),
                destFile.length()).save();

        return true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}

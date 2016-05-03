package com.bbbbiu.biu.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bbbbiu.biu.lib.util.HttpManager;
import com.bbbbiu.biu.lib.httpd.util.ProgressListener;
import com.bbbbiu.biu.lib.util.ProgressListenerImpl;
import com.bbbbiu.biu.lib.httpd.util.ProgressNotifier;
import com.bbbbiu.biu.lib.httpd.util.Streams;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class UploadService extends Service {
    private static final String TAG = UploadService.class.getSimpleName();

    private static final String EXTRA_RESULT_RECEIVER = "com.bbbbiu.biu.service.UploadService.extra.RECEIVER";
    private static final String EXTRA_FILE_PATH = "com.bbbbiu.biu.service.UploadService.extra.FILE_PATH";
    private static final String EXTRA_UPLOAD_URL = "com.bbbbiu.biu.service.UploadService.extra.UPLOAD_URL";
    private static final String EXTRA_FORM_DATA = "com.bbbbiu.biu.service.UploadService.extra.FORM_DATA";

    private static final String ACTION_START_UPLOAD = "com.bbbbiu.biu.service.UploadService.action.ACTION_START_UPLOAD";

    private HandlerThread mWorkerThread;
    private Handler mHandler;

    private Call mCurrentHttpCall;


    public UploadService() {
    }


    /**
     * 开始上传文件到uploadUrl
     *
     * @param context        context
     * @param uploadUrl      上传URL
     * @param filePath       文件路径
     * @param formData       http form data
     * @param resultReceiver {@link ResultReceiver}
     */
    public static void startUpload(Context context, String uploadUrl, String filePath,
                                   @Nullable HashMap<String, String> formData, ResultReceiver resultReceiver) {

        Intent intent = new Intent(context, UploadService.class);

        intent.putExtra(EXTRA_UPLOAD_URL, uploadUrl);
        intent.putExtra(EXTRA_FILE_PATH, filePath);
        intent.putExtra(EXTRA_FORM_DATA, formData);
        intent.putExtra(EXTRA_RESULT_RECEIVER, resultReceiver);

        intent.setAction(ACTION_START_UPLOAD);

        context.startService(intent);
    }


    /**
     * 终止上传并关闭Service
     *
     * @param context context
     */
    public static void stopUpload(Context context) {
        Intent intent = new Intent(context, UploadService.class);

        context.stopService(intent);
    }


    @SuppressWarnings("unchecked")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return Service.START_STICKY;

        } else if (intent.getAction().equals(ACTION_START_UPLOAD)) {
            final String filePath = intent.getStringExtra(EXTRA_FILE_PATH);
            final String uploadUrl = intent.getStringExtra(EXTRA_UPLOAD_URL);
            final HashMap<String, String> formData = (HashMap<String, String>) intent.getSerializableExtra(EXTRA_FORM_DATA);
            final ResultReceiver resultReceiver = intent.getParcelableExtra(EXTRA_RESULT_RECEIVER);


            if (mWorkerThread == null) {
                mWorkerThread = new HandlerThread("FileUploadThread");
                mWorkerThread.start();
                mHandler = new Handler(mWorkerThread.getLooper());
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "Start sending file " + filePath);

                    ProgressListenerImpl progressListener = new ProgressListenerImpl(filePath, resultReceiver);

                    boolean succeeded = uploadFile(uploadUrl, filePath, formData, progressListener);

                    if (succeeded) {
                        resultReceiver.send(ProgressListenerImpl.RESULT_SUCCEEDED, null);
                    } else {
                        resultReceiver.send(ProgressListenerImpl.RESULT_FAILED, null);
                    }
                }
            });

        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mCurrentHttpCall != null && mCurrentHttpCall.isExecuted()) {
            mCurrentHttpCall.cancel();
        }

        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler.getLooper().quit();
        }

    }

    /**
     * 传文件
     *
     * @param uploadUrl        uploadUrl
     * @param filePath         文件路径
     * @param formData         http form data
     * @param progressListener {@link ProgressListener} 监听发送进度
     * @return 是否发送成功
     */
    private boolean uploadFile(String uploadUrl, String filePath,
                               @Nullable HashMap<String, String> formData,
                               ProgressListener progressListener) {

        File file = new File(filePath);
        Request request = HttpManager.newFileUploadRequest(uploadUrl, file, formData,
                new ProgressNotifier(progressListener, file.length()));


        Response response;
        ResponseBody body = null;

        try {
            mCurrentHttpCall = HttpManager.newHttpClient().newCall(request);
            response = mCurrentHttpCall.execute();
            body = response.body();

        } catch (IOException e) {
            Log.i(TAG, "Upload file failed. " + filePath + "  HTTP error " + e.toString(), e);
            return false;
        } finally {
            Streams.safeClose(body);
        }

        if (response.code() != 200) {
            Log.i(TAG, "Upload file failed. Response status code " + response.code());
            return false;
        }

        Log.i(TAG, "Upload file succeeded " + filePath);

        return true;
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


}

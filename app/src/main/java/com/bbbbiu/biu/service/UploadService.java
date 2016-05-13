package com.bbbbiu.biu.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bbbbiu.biu.lib.util.HttpManager;
import com.bbbbiu.biu.lib.util.ProgressListener;
import com.bbbbiu.biu.lib.util.ProgressNotifier;
import com.bbbbiu.biu.lib.httpd.util.Streams;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 使用HTTP POST 后台上传、发送文件
 */
public class UploadService extends Service {
    private static final String TAG = UploadService.class.getSimpleName();

    /**
     * Intent extra. {@link ResultReceiver} 接收发送进度等
     */
    private static final String EXTRA_RESULT_RECEIVER = "com.bbbbiu.biu.service.UploadService.extra.RECEIVER";

    /**
     * Intent extra. 要上传的文件的绝对路径。
     */
    private static final String EXTRA_FILE_PATH = "com.bbbbiu.biu.service.UploadService.extra.FILE_PATH";

    /**
     * Intent extra. POST 文件 到该URL
     */
    private static final String EXTRA_UPLOAD_URL = "com.bbbbiu.biu.service.UploadService.extra.UPLOAD_URL";

    /**
     * Intent extra. HTTP POST FORM 中额外的key-value pair.
     */
    private static final String EXTRA_FORM_DATA = "com.bbbbiu.biu.service.UploadService.extra.FORM_DATA";


    /**
     * Intent action. 开始上传
     */
    private static final String ACTION_START_UPLOAD = "com.bbbbiu.biu.service.UploadService.action.ACTION_START_UPLOAD";

    /**
     * 上传线程
     */
    private HandlerThread mWorkerThread;

    /**
     * 上传线程的{@link Handler}
     */
    private Handler mWorkerHandler;


    /**
     * 当前的{@link okhttp3.OkHttpClient} Call，用来终止上传
     */
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
        if (intent != null && intent.getAction().equals(ACTION_START_UPLOAD)) {
            final String fileUri = intent.getStringExtra(EXTRA_FILE_PATH);
            final String uploadUrl = intent.getStringExtra(EXTRA_UPLOAD_URL);
            final HashMap<String, String> formData = (HashMap<String, String>) intent.getSerializableExtra(EXTRA_FORM_DATA);
            final ResultReceiver resultReceiver = intent.getParcelableExtra(EXTRA_RESULT_RECEIVER);


            if (mWorkerThread == null) {
                mWorkerThread = new HandlerThread("FileUploadThread");
                mWorkerThread.start();
                mWorkerHandler = new Handler(mWorkerThread.getLooper());
            }

            mWorkerHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "Start sending file " + fileUri);

                    ProgressListenerImpl progressListener = new ProgressListenerImpl(fileUri, resultReceiver);

                    boolean succeeded = uploadFile(uploadUrl, fileUri, formData, progressListener);

                    Bundle bundle = new Bundle();
                    bundle.putString(ProgressListener.RESULT_EXTRA_FILE_URI, fileUri);

                    if (succeeded) {
                        resultReceiver.send(ProgressListener.RESULT_SUCCEEDED, bundle);
                    } else {
                        resultReceiver.send(ProgressListener.RESULT_FAILED, bundle);
                    }
                }
            });

        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // 取消未完成的任务
        if (mCurrentHttpCall != null && mCurrentHttpCall.isExecuted()) {
            mCurrentHttpCall.cancel();
        }

        // 清空消息队列，退出Looper，进而终止工作线程
        if (mWorkerHandler != null) {
            mWorkerHandler.removeCallbacksAndMessages(null);
            mWorkerHandler.getLooper().quit();
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

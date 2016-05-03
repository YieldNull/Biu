package com.bbbbiu.biu.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;

import com.bbbbiu.biu.gui.transfer.DownloadActivity;
import com.bbbbiu.biu.gui.transfer.FileItem;
import com.bbbbiu.biu.lib.util.HttpManager;
import com.bbbbiu.biu.lib.util.HttpConstants;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 轮询Service，用于电脑将文件先传到公网服务器，然后再从服务器下载的情景。
 * <p>
 * 需要轮询原因：
 * <p>
 * 1.用户上传文件后可以选择继续上传或者从手机端下载文件，不能要求用户再次扫码
 * 2.用户上传两个文件的时间间隔比较大，比如传大文件，一次无法获取所有文件列表
 */
public class PollingService extends Service {
    private static final String TAG = PollingService.class.getSimpleName();

    /**
     * 成功获取到服务器传来的文件列表。
     * <p>
     * 轮询的结果，{@link ResultReceiver} send() 中 statusCode参数
     */
    public static final int RESULT_OK = 0;

    /**
     * 服务器故障
     * <p>
     * 轮询的结果，{@link ResultReceiver} send() 中 statusCode参数
     */
    public static final int RESULT_ERROR = 1;


    /**
     * 从二维码扫描得到的uid
     * <p>
     * intent extra 的字段名， 接收从 Activity 传来的参数
     */
    private static final String EXTRA_UID = "com.bbbbiu.biu.service.PollingService.extra.UID";

    /**
     * 从Activity传来的 {@link ResultReceiver}
     * <p>
     * intent extra 的字段名， 接收从 Activity 传来的参数
     */
    private static final String EXTRA_RECEIVER = "com.bbbbiu.biu.service.PollingService.extra.RECEIVER";


    /**
     * 开始轮询，用于intent.setAction()
     * {@link PollingService#startPolling(Context, String, ResultReceiver)}
     */
    private static final String ACTION_START = "com.bbbbiu.biu.service.PollingService.action.START";

    /**
     * 结束轮询，关闭服务，用于intent.setAction()
     * {@link PollingService#stopPolling(Context)}
     */
    private static final String ACTION_STOP = "com.bbbbiu.biu.service.PollingService.action.STOP";

    /**
     * 从服务器获取文件列表的失败重试次数，超过此次数则停止轮询
     * 失败指的是出现IO异常，HTTP返回码非200，返回的json数据非法等
     * <p>
     * 详见{@link PollingService#downloadFileList()}
     */
    private static final int RETRY_TIME = 20;

    public PollingService() {
    }


    /**
     * 从二维码扫描得到的uid，由开启服务者传来
     */
    private String mUid;

    /**
     * 用于接受伦须结果及数据，由开启服务者传来
     */
    private ResultReceiver mResultReceiver;

    /**
     * 工作线程
     */
    private Thread mPollingThread;

    /**
     * 开始轮询。由开启服务者调用。
     *
     * @param context        调用者的Context
     * @param uid            扫描二维码得到的uid
     * @param resultReceiver 接收结果
     */
    public static void startPolling(Context context, String uid, ResultReceiver resultReceiver) {
        Intent intent = new Intent(context, PollingService.class);
        intent.putExtra(EXTRA_UID, uid);
        intent.putExtra(EXTRA_RECEIVER, resultReceiver);
        intent.setAction(ACTION_START);

        context.startService(intent);
        Log.i(TAG, "Received start command from " + context.getClass().getSimpleName());
    }


    /**
     * 结束轮询，关闭服务。
     *
     * @param context 调用者的Context
     */
    public static void stopPolling(Context context) {
        Intent intent = new Intent(context, PollingService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);

        Log.i(TAG, "Received stop command from " + context.getClass().getSimpleName());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null) {
            return Service.START_STICKY;
        }

        // 当工作线程已经结束时再次开启
        if (intent.getAction().equals(ACTION_START) && mPollingThread == null) {
            mResultReceiver = intent.getParcelableExtra(EXTRA_RECEIVER);
            mUid = intent.getStringExtra(EXTRA_UID);

            mPollingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    polling();
                }
            });

            mPollingThread.start();

        } else if (intent.getAction().equals(ACTION_STOP)) {
            stopSelf();
        }
        return Service.START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        if (mPollingThread != null && mPollingThread.isAlive()) {
            Log.i(TAG, "onDestroy. Interrupting polling thread");
            mPollingThread.interrupt();
        } else {
            Log.i(TAG, "onDestroy. Polling thread was already dead");
        }
    }

    /**
     * 轮询，将相应结果通过{@link ResultReceiver} 发送给对应的Activity。
     */
    private void polling() {
        Log.i(TAG, "Polling started");

        int retryCount = 0;
        while (true) {

            if (mPollingThread.isInterrupted()) {
                Log.i(TAG, "Received interrupt signal. Polling stopped");
                break;
            }

            if (retryCount > RETRY_TIME) { // 服务器故障
                Log.i(TAG, "Server error. Stop self.");
                mResultReceiver.send(RESULT_ERROR, null);
                stopSelf(); // TODO 终止Thread还是Service？
                break;
            }

            Log.i(TAG, "Downloading file list from remote server");
            ArrayList<FileItem> fileItems = downloadFileList();
            if (fileItems == null) {
                retryCount++;
                continue;
            }

            retryCount = 0; // 清零

            if (fileItems.size() == 0) {
                Log.i(TAG, "User has not send files to server. Retrying...");
            } else {

                DownloadActivity.addTask(this, fileItems);
            }
        }
    }

    /**
     * 从服务器下载文件列表
     *
     * @return 文件列表，出现故障则返回null；没有文件则返回的ArrayList.size()为0
     */
    private ArrayList<FileItem> downloadFileList() {
        Request request = HttpManager.newRequest(HttpConstants.Computer.getManifestUrl(mUid));

        Response response;
        ResponseBody body = null;

        try {
            try {
                response = HttpManager.newHttpClient().newCall(request).execute();
                body = response.body();
            } catch (IOException e) {
                Log.i(TAG, "Get file list failed. HTTP error" + e.toString());
                return null;
            }

            if (response.code() != 200) {
                Log.i(TAG, "Get file list failed. Response status code " + response.code());
                return null;
            }

            try {
                Gson gson = new Gson();

                return gson.fromJson(body.charStream(),
                        new TypeToken<ArrayList<FileItem>>() {
                        }.getType());

            } catch (JsonSyntaxException e) {
                Log.i(TAG, "Get file list failed. Response is not a valid json");
                Log.i(TAG, e.toString());
            }
        } finally {
            if (body != null) {
                body.close();
            }
        }
        return null;
    }
}

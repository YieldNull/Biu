package com.bbbbiu.biu.gui.transfer.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.transfer.FileItem;
import com.bbbbiu.biu.gui.transfer.TransferBaseActivity;
import com.bbbbiu.biu.lib.HttpConstants;
import com.bbbbiu.biu.lib.HttpManager;
import com.bbbbiu.biu.lib.WifiApManager;
import com.bbbbiu.biu.service.UploadService;
import com.bbbbiu.biu.util.NetworkUtil;
import com.bbbbiu.biu.util.PreferenceUtil;
import com.google.gson.Gson;
import com.yieldnull.httpd.Streams;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import okhttp3.Request;
import okhttp3.Response;


/**
 * Android 端发送文件给 Android端
 */
public class SendingActivity extends TransferBaseActivity {
    private static final String TAG = SendingActivity.class.getSimpleName();


    /**
     * 连接WIFI重试间隔
     */
    private static final int SCAN_INTERVAL = 1000;


    /**
     * 连接WIFI重试次数上限
     */
    private static final int SCAN_THRESHOLD = 15;


    /**
     * 等待用户手动连接，检查时间间隔
     */
    private static final int WAIT_CONNECTED_INTERVAL = 1000;


    /**
     * 发送文件清单，重试时间间隔
     */
    private static final int SEND_MANIFEST_INTERVAL = 1000;


    /**
     * 连上之后再等一会儿
     */
    private static final long SEND_MANIFEST_DELAY = 1000;


    /**
     * 发送文件清单，重试次数上限
     */
    private static final int SEND_MANIFEST_THRESHOLD = 10;


    private ConnectivityManager mConnManager;
    private WifiManager mWifiManager;
    private Handler mHandler;


    /**
     * 扫描Wifi的task，周期性扫描，直到成功连接
     */
    private Runnable mWifiScanTask;


    /**
     * 等待WIFI成功连接，为CONNECTED状态
     */
    private Runnable mWaitingWifiConnectionTask;


    /**
     * 等待WIFI连接线程
     */
    private Thread mSendingManifestThread;


    /**
     * 接收扫描wifi列表的结果 {@link WifiManager#SCAN_RESULTS_AVAILABLE_ACTION}
     */
    private BroadcastReceiver mWifiListReceiver;


    /**
     * 是否已经连接上接收方开的热点
     */
    private boolean mIsConnected;


    /**
     * 接收方的地址（网关地址）
     */
    private InetAddress mServerAddress;


    /**
     * 文件清单
     */
    private ArrayList<FileItem> mFileManifest;


    /**
     * 要把文件发到哪里呢？
     */
    private String mUploadUrl;


    /**
     * 连接WIFI重试次数
     */
    private int mScanRetryCount;


    /**
     * 开始连接
     *
     * @param context Context
     */
    public static void startConnection(Context context) {
        context.startActivity(new Intent(context, SendingActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            mServerAddress = InetAddress.getByName(HttpConstants.SERVER_ADDRESS);
        } catch (UnknownHostException ignored) {
        }


        mFileManifest = PreferenceUtil.getFileItemsToSend(this);

        mHandler = new Handler();
        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        mConnManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);


        // 接收wifi列表的扫描结果
        mWifiListReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleScanResult();
            }
        };

        // 注册Receiver
        registerReceiver(mWifiListReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));


        // 扫描Task
        mWifiScanTask = new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Scanning wifi list");
                mWifiManager.startScan();
            }
        };

        mWaitingWifiConnectionTask = new Runnable() {
            @Override
            public void run() {
                NetworkInfo info = mConnManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                if (info != null && info.getState() == NetworkInfo.State.CONNECTED
                        && mWifiManager.getConnectionInfo().getSSID()
                        .equals("\"" + WifiApManager.AP_SSID + "\"")) {

                    Log.i(TAG, "Wifi Connected");
                    updateLoadingText(getString(R.string.hint_connect_sending_manifest));
                    onWifiConnected();
                } else {
                    Log.i(TAG, "Wifi not connected. Retry waiting connection");

                    if (mHandler != null) {
                        mHandler.postDelayed(this, WAIT_CONNECTED_INTERVAL);
                    } else {
                        Log.i(TAG, "Activity was already destroyed");
                    }
                }
            }
        };

        mSendingManifestThread = sendManifestThread();


        // 显示正在连接WIFI
        showConnectingAnim();
        updateLoadingText(getString(R.string.hint_connect_scanning_wifi));


        // 准备连接
        Log.i(TAG, "Turn on WIFI");
        NetworkUtil.enableWifi(this);

        WifiInfo info = mWifiManager.getConnectionInfo();


        // 有可能进来之前就已经连上了
        if (info != null && info.getSSID() != null
                && info.getSSID().equals("\"" + WifiApManager.AP_SSID + "\"")) {

            Log.i(TAG, "Already connected to receiver's wifi");

            onWifiConnected();
        } else {
            mHandler.postDelayed(mWifiScanTask, SCAN_INTERVAL);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.i(TAG, "onDestroy. Unregister broadcast receiver and interrupt working thread");

        try {
            unregisterReceiver(mWifiListReceiver); // unregister receiver
        } catch (IllegalArgumentException ignored) {
        }

        if (mSendingManifestThread.isAlive()) {
            mSendingManifestThread.interrupt();
        }

        // 主线程里面的，退了Activity还能在消息队列里面跑
        mHandler.removeCallbacks(mWaitingWifiConnectionTask);
        mHandler = null;
    }


    @Override
    protected void onAddNewTask(ArrayList<FileItem> fileItems) {
        for (FileItem item : fileItems) {
            HashMap<String, String> map = new HashMap<>();
            map.put(HttpConstants.FILE_URI, item.uri);

            UploadService.startUpload(this, mUploadUrl, item, map, mProgressResultReceiver);
        }
    }

    @Override
    protected void onTransferCanceled() {
        onTransferFinished();
    }

    @Override
    protected void onTransferFinished() {
        UploadService.stopUpload(this);
    }

    /**
     * 接收到扫描结果的Broadcast之后，若列表中存在对方手机开的wifi，则连接
     * 否则继续扫描
     */
    private void handleScanResult() {
        if (mIsConnected) {
            Log.i(TAG, "Already connected to receiver's wifi");
            return;
        }

        mScanRetryCount++;

        for (ScanResult result : mWifiManager.getScanResults()) {
            if (result.SSID.contains(WifiApManager.AP_SSID)) {

                Log.i(TAG, "Connecting wifi " + result.SSID);

                WifiConfiguration conf = new WifiConfiguration();
                conf.SSID = "\"" + result.SSID + "\"";

                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

                int netId = mWifiManager.addNetwork(conf);
                if (netId < 0) {
                    Log.i(TAG, "Connect wifi failed: Add network failed: id:" + netId);

                    // 到底加不加 双引号啊，卧槽，给个准信行不行
                    conf.SSID = result.SSID;
                    netId = mWifiManager.addNetwork(conf);

                    if (netId < 0) { // 加不加引号都特么连不上
                        break;
                    }
                }

                mWifiManager.disconnect();
                boolean enabled = mWifiManager.enableNetwork(netId, true);
                mWifiManager.reconnect();

                if (enabled) {
                    unregisterReceiver(mWifiListReceiver); // 不再接受广播
                    Log.i(TAG, "Unregister wifi scan broadcast receiver");

                    waitWifiConnected();
                } else {
                    Log.i(TAG, "Connect wifi failed: Enable network failed");
                }

                return;
            }
        }

        Log.i(TAG, "Wifi list scanned. Not connected.");

        if (mScanRetryCount >= SCAN_THRESHOLD) {
            Log.i(TAG, "Abort retrying. Let user connect wifi manually.");

            // 系统也会扫描，因此直接取消监听
            unregisterReceiver(mWifiListReceiver);
            Log.i(TAG, "Unregister wifi scan broadcast receiver");

            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.hint_connect_wifi_manually_title))
                    .setMessage(getString(R.string.hint_connect_wifi_manually_confirm))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            updateLoadingText(getString(R.string.hint_connect_wifi_conn_manually));
                            waitWifiConnected();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(SendingActivity.this, R.string.hint_connect_aborted,
                                    Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }).setCancelable(false)
                    .show();
        } else {

            Log.i(TAG, "ReScan wifi list. RetryCount: " + mScanRetryCount);
            mHandler.postDelayed(mWifiScanTask, SCAN_INTERVAL);// 继续扫描
        }
    }


    /**
     * 等待用户手动连接WIFI
     */
    private void waitWifiConnected() {
        mHandler.postDelayed(mWaitingWifiConnectionTask, WAIT_CONNECTED_INTERVAL);
    }

    /**
     * 连上对方的Wifi之后，发送清单，发送文件
     */
    private void onWifiConnected() {
        mIsConnected = true;


        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mSendingManifestThread.start();
                    }
                });
            }
        }, SEND_MANIFEST_DELAY); // 连上之后再等一会儿
    }


    /**
     * 发送文件清单的线程，失败即结束运行。因为可能会重试多次，故会多次创建
     *
     * @return 发送文件清单的线程
     */
    private Thread sendManifestThread() {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Sending file manifest");

                int retryCount = 0;
                while (retryCount < SEND_MANIFEST_THRESHOLD) {
                    try {
                        Thread.sleep(SEND_MANIFEST_INTERVAL);
                    } catch (InterruptedException e) {
                        Log.i(TAG, "Interrupted. Abort waiting connection");
                    }

                    if (sendFileManifest()) {
                        break;
                    } else {
                        retryCount++;
                        Log.i(TAG, "Send file manifest to receiver. RETRY COUNT: " + retryCount);
                    }
                }

                if (retryCount == SEND_MANIFEST_THRESHOLD) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(SendingActivity.this)
                                    .setTitle(getString(R.string.hint_connect_manifest_resend_title))
                                    .setMessage(getString(R.string.hint_connect_manifest_resend_confirm))
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            mSendingManifestThread = sendManifestThread();
                                            onWifiConnected();
                                        }
                                    })
                                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Toast.makeText(SendingActivity.this, R.string.hint_connect_aborted,
                                                    Toast.LENGTH_LONG).show();
                                            finish();
                                        }
                                    }).setCancelable(false)
                                    .show();
                        }
                    });
                } else {
                    Log.i(TAG, "Send file manifest successfully");

                    mUploadUrl = HttpConstants.Android.getSendUrl(mServerAddress);

                    if (!Thread.interrupted()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                addTask(mFileManifest);
                            }
                        });
                    }
                }
            }
        });
    }

    /**
     * 发送文件清单，新开一个线程调用
     *
     * @return 是否发送成功
     */
    private boolean sendFileManifest() {
        Gson gson = new Gson();
        String json = gson.toJson(mFileManifest);

        Log.i(TAG, "Sending manifest:" + json);

        Request request = HttpManager.newJsonRequest(HttpConstants.Android.getManifestUrl(mServerAddress), json);

        Response response = null;
        try {

            if (!Thread.interrupted()) {
                response = HttpManager.newHttpClient().newCall(request).execute();
                return response.code() == 200;
            } else {
                Log.i(TAG, "Interrupted. Abort sending manifest");
                return false;
            }
        } catch (IOException e) {
            Log.w(TAG, e.getMessage());
            return false;
        } finally {
            if (response != null) {
                Streams.safeClose(response.body());
            }
        }
    }

}

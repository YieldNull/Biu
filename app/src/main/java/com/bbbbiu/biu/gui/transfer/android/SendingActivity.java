package com.bbbbiu.biu.gui.transfer.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.bbbbiu.biu.gui.transfer.FileItem;
import com.bbbbiu.biu.gui.transfer.TransferBaseActivity;
import com.bbbbiu.biu.lib.util.HttpConstants;
import com.bbbbiu.biu.lib.util.HttpManager;
import com.bbbbiu.biu.lib.util.WifiApManager;
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

    private WifiManager mWifiManager;
    private Handler mHandler;

    /**
     * 扫描Wifi的task，周期性扫描，直到成功连接
     */
    private Runnable mWifiScanTask;

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
    private ArrayList<FileItem> mFileManifest = new ArrayList<>();

    private String mUploadUrl;

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

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mWifiManager.setWifiEnabled(true);


        // 接收wifi列表的扫描结果
        mWifiListReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleScanResult();
            }
        };

        // 注册Receiver
        registerReceiver(mWifiListReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        mWifiScanTask = new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Scanning wifi list");
                mWifiManager.startScan();
            }
        };

        mHandler = new Handler();


        addTask(getIntent());

        showConnectingAnim();
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
    protected void onResume() {
        super.onResume();

        // 避免连接之后又重复扫描
        if (!mIsConnected) {
            NetworkUtil.enableWifi(this);

            WifiInfo info = mWifiManager.getConnectionInfo();

            if (info != null && info.getSSID() != null
                    && info.getSSID().equals("\"" + WifiApManager.AP_SSID + "\"")) {

                Log.i(TAG, "Already connected to receiver's wifi");

                mIsConnected = true;
                onWifiConnected();
            } else {
                mHandler.postDelayed(mWifiScanTask, 1000);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mWifiListReceiver); // unregister receiver
    }

    @Override
    protected void onCancelTransfer() {
        UploadService.stopUpload(this);
    }

    /**
     * 接收到扫描结果的Broadcast之后，若列表中存在对方手机开的wifi，则连接
     * 否则继续扫描
     */
    private void handleScanResult() {
        if (mIsConnected) {
            return;
        }

        for (ScanResult result : mWifiManager.getScanResults()) {
            if (result.SSID.contains(WifiApManager.AP_SSID)) {

                Log.i(TAG, "Connecting wifi " + result.SSID);

                WifiConfiguration conf = new WifiConfiguration();
                conf.SSID = "\"" + result.SSID + "\"";

                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

                int netId = mWifiManager.addNetwork(conf);
                if (netId < 0) {
                    Log.i(TAG, "Connect wifi failed: Add network failed: id:" + netId);

                    conf.SSID = result.SSID;
                    netId = mWifiManager.addNetwork(conf);

                    if (netId < 0) {
                        break;
                    }
                }

                mWifiManager.disconnect();
                mIsConnected = mWifiManager.enableNetwork(netId, true);
                mWifiManager.reconnect();

                if (mIsConnected) {
                    onWifiConnected();

                    Log.i(TAG, "Connected wifi " + result.SSID);
                } else {
                    Log.i(TAG, "Connect wifi failed: Enable network failed");
                }

                return;
            }
        }

        Log.i(TAG, "Wifi list scanned. Not connected.");
        mHandler.postDelayed(mWifiScanTask, 200);// 继续扫描 // TODO 避免无线扫描
    }

    /**
     * 连上对方的Wifi之后，发送清单，发送文件
     */
    private void onWifiConnected() {
        // 读取要发送的文件列表
        mFileManifest.addAll(PreferenceUtil.getFileItemsToSend(this));

        HandlerThread handlerThread = new HandlerThread("SendingManifestThread");
        handlerThread.start();

        new Handler(handlerThread.getLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Sending file manifest");

                int retryCount = 0;
                while (retryCount < 5) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.w(TAG, e);
                    }

                    if (sendFileManifest()) {
                        break;
                    } else {
                        retryCount++;
                        Log.i(TAG, "Send file manifest to receiver. RETRY COUNT: " + retryCount);
                    }
                }

                if (retryCount == 5) {
                    // TODO 发送失败
                } else {
                    Log.i(TAG, "Send file manifest successfully");

                    mUploadUrl = HttpConstants.Android.getSendUrl(mServerAddress);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addTask(mFileManifest);
                        }
                    });
                }
            }
        }, 4000);
    }

    /**
     * 发送文件清单，新开一个线程调用
     *
     * @return 是否发送成功
     */
    private boolean sendFileManifest() {
        Gson gson = new Gson();
        String json = gson.toJson(mFileManifest);

        Log.i(TAG, json);

        Request request = HttpManager.newJsonRequest(HttpConstants.Android.getManifestUrl(mServerAddress), json);

        Response response = null;
        try {
            response = HttpManager.newHttpClient().newCall(request).execute();
            return response.code() == 200;

        } catch (IOException e) {
            Log.w(TAG, e.toString());
            return false;
        } finally {
            if (response != null) {
                Streams.safeClose(response.body());
            }
        }
    }

}

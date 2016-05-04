package com.bbbbiu.biu.gui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.WindowManager;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.transfer.FileItem;
import com.bbbbiu.biu.gui.transfer.UploadActivity;
import com.bbbbiu.biu.lib.android.WifiApManager;
import com.bbbbiu.biu.lib.httpd.util.Streams;
import com.bbbbiu.biu.lib.util.HttpConstants;
import com.bbbbiu.biu.lib.util.HttpManager;
import com.bbbbiu.biu.util.NetworkUtil;
import com.bbbbiu.biu.util.PreferenceUtil;
import com.google.gson.Gson;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 安卓发送方连接接收方。接收方开热点，发送方连接热点，
 * 然后将要发送的文件清单传给接收方，接收方确认接收之后视为连接成功。
 * 然后跳转到发送页面{@link com.bbbbiu.biu.gui.transfer.UploadActivity}
 */
public class ConnectReceiverActivity extends AppCompatActivity {
    private static final String TAG = ConnectReceiverActivity.class.getSimpleName();

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_receiver);

        ButterKnife.bind(this);

        // Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        // android 4.4 状态栏透明
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(getResources().getColor(R.color.colorPrimary));
        }

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

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

        // 读取要发送的文件列表
        genManifest(new ArrayList<>(PreferenceUtil.getFilesToSend(this)));
    }


    @Override
    protected void onResume() {
        super.onResume();

        // 避免连接之后又重复扫描
        if (!mIsConnected) {
            NetworkUtil.enableWifi(ConnectReceiverActivity.this);

            if (!mWifiManager.getConnectionInfo().getSSID().equals("\"" + WifiApManager.AP_SSID + "\"")) {
                mHandler.postDelayed(mWifiScanTask, 1000);
            } else {
                Log.i(TAG, "Already connected to receiver's wifi");
                onWifiConnected();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mWifiListReceiver); // unregister receiver
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
                    Log.i(TAG, "Connect wifi failed. Add network failed: id:" + netId);
                }

                mWifiManager.disconnect();
                mIsConnected = mWifiManager.enableNetwork(netId, true);
                mWifiManager.reconnect();

                if (mIsConnected) {
                    onWifiConnected();

                    Log.i(TAG, "Connected wifi " + result.SSID);
                } else {
                    Log.i(TAG, "Connect wifi failed. Enable network failed");
                }

                return;
            }
        }

        Log.i(TAG, "Scan wifi. not found");
        mHandler.postDelayed(mWifiScanTask, 200);// 继续扫描 // TODO 避免无线扫描
    }

    /**
     * 连上对方的Wifi之后，发送清单，发送文件
     */
    private void onWifiConnected() {
        mServerAddress = genServerAddress();

        HandlerThread handlerThread = new HandlerThread("SendingManifestThread");
        handlerThread.start();

        new Handler(handlerThread.getLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Sending file manifest");

                int retryCount = 0;
                while (retryCount < 5) {
                    try {
                        Thread.sleep(200);
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
                    UploadActivity.startUpload(ConnectReceiverActivity.this,
                            HttpConstants.Android.getSendUrl(mServerAddress), mFileManifest);
                }
            }
        }, 800);
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


    /**
     * 生成要发送文件的清单
     *
     * @param filePathList 要发送的文件绝对路径
     */
    private void genManifest(List<String> filePathList) {
        for (final String filePath : filePathList) {
            File file = new File(filePath);
            mFileManifest.add(new FileItem(file.getAbsolutePath(), file.getName(), file.length()));
        }
    }

    /**
     * 获取接收方的InetAddress（网关地址）
     *
     * @return InetAddress
     */
    private InetAddress genServerAddress() {
        DhcpInfo dhcpInfo = mWifiManager.getDhcpInfo();
        InetAddress inetAddress = NetworkUtil.intToInetAddress(dhcpInfo.serverAddress);

        Log.i(TAG, inetAddress != null ? inetAddress.toString() : null);

        return inetAddress;
    }
}

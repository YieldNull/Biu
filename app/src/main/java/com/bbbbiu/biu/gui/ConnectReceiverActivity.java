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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.WindowManager;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.lib.android.HttpManager;
import com.bbbbiu.biu.lib.android.Manifest;
import com.bbbbiu.biu.lib.android.WifiApManager;
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

public class ConnectReceiverActivity extends AppCompatActivity {
    private static final String TAG = ConnectReceiverActivity.class.getSimpleName();

    private WifiManager mWifiManager;
    private Runnable mWifiScanTask;
    private BroadcastReceiver mWifiListReceiver;

    private boolean mIsConnected;
    private InetAddress mServerAddress;
    private Manifest mFileManifest;

    private Handler mHandler;


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

        // 注册Receiver
        registerReceiver(mWifiListReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        // 接收wifi列表的扫描结果
        mWifiListReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                connectWifi();
            }
        };

        mWifiScanTask = new Runnable() {
            @Override
            public void run() {
                mWifiManager.startScan();
            }
        };

        mHandler = new Handler();


        mFileManifest = genManifest(new ArrayList<>(PreferenceUtil.getFilesToSend(this)));
    }


    @Override
    protected void onResume() {
        super.onResume();

        // 避免连接之后又重复扫描
        if (!mIsConnected) {
            NetworkUtil.enableWifi(ConnectReceiverActivity.this);
            mHandler.postDelayed(mWifiScanTask, 1000);

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
    private void connectWifi() {
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
        mHandler.postDelayed(mWifiScanTask, 200);
    }

    /**
     * 连上对方的Wifi之后，发送清单，发送文件
     */
    private void onWifiConnected() {
        mServerAddress = genServerAddress();

        if (sendFileManifest()) {
        } else {
            // TODO 发送失败
        }
    }

    /**
     * 发送文件清单，要新开一个线程
     *
     * @return 是否发送成功
     */
    private boolean sendFileManifest() {
        Gson gson = new Gson();
        String json = gson.toJson(mFileManifest);

        Log.i(TAG, json);

        Request request = HttpManager.newManifestSendRequest(mServerAddress, json);

        try {
            Response response = HttpManager.newHttpClient().newCall(request).execute();
            return response.code() == 200;

        } catch (IOException e) {
            Log.w(TAG, e.toString());
            return false;
        }
    }


    /**
     * 生成要发送文件的清单
     *
     * @param filePathList 要发送的文件绝对路径
     * @return {@link Manifest}
     */
    private Manifest genManifest(List<String> filePathList) {
        Manifest manifest = new Manifest();

        for (final String filePath : filePathList) {
            File file = new File(filePath);
            Manifest.Item item = new Manifest.Item(file.getAbsolutePath(), file.length());
            manifest.addItem(item);
        }
        return manifest;

    }

    /**
     * 获取接收方的InetAddress（网关地址）
     *
     * @return InetAddress
     */
    private InetAddress genServerAddress() {
        DhcpInfo dhcpInfo = mWifiManager.getDhcpInfo();
        InetAddress inetAddress = NetworkUtil.intToInetAddress(dhcpInfo.gateway);

        Log.i(TAG, inetAddress != null ? inetAddress.toString() : null);

        return inetAddress;
    }
}

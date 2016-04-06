package com.bbbbiu.biu.gui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.bbbbiu.biu.p2p.WifiApManager;
import com.bbbbiu.biu.util.NetworkUtil;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import butterknife.ButterKnife;

public class SendAndroidActivity extends AppCompatActivity {

    private static final String TAG = SendAndroidActivity.class.getSimpleName();

    private WifiManager mWifiManager;
    private Handler mHandler;
    private Runnable mScanTask;
    private BroadcastReceiver mReceiver;

    private boolean connected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_android);

        ButterKnife.bind(this);

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
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                connectWifi();
            }
        };
        registerReceiver(mReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        mScanTask = new Runnable() {
            @Override
            public void run() {
                mWifiManager.startScan();
            }
        };

        mHandler = new Handler();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 避免连接之后又重复扫描
        if (!connected) {
            NetworkUtil.enableWifi(SendAndroidActivity.this);
            mHandler.postDelayed(mScanTask, 1000);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mReceiver); // unregister receiver
    }

    /**
     * 接收到扫描结果的Broadcast之后，若列表中存在对方手机开的wifi，则连接
     * 否则继续扫描
     */
    private void connectWifi() {
        if (connected) {
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
                connected = mWifiManager.enableNetwork(netId, true);
                mWifiManager.reconnect();

                if (connected) {
                    Log.i(TAG, "Connected wifi " + result.SSID);
                } else {
                    Log.i(TAG, "Connect wifi failed. Enable network failed");
                }

                return;
            }
        }

        Log.i(TAG, "Scan wifi. not found");
        mHandler.postDelayed(mScanTask, 200);
    }
}

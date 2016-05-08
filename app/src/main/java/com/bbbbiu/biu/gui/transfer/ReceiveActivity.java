package com.bbbbiu.biu.gui.transfer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;


import com.bbbbiu.biu.lib.android.WifiApManager;
import com.bbbbiu.biu.lib.android.servlets.ManifestServlet;
import com.bbbbiu.biu.lib.android.servlets.ReceiveServlet;
import com.bbbbiu.biu.service.HttpdService;

import java.util.ArrayList;

public class ReceiveActivity extends TransferBaseActivity {
    private static final String TAG = ReceiveActivity.class.getSimpleName();

    public static final String ACTION_RECEIVE_MANIFEST = "com.bbbbiu.biu.gui.transfer.ReceiveActivity.action.RECEIVE_MANIFEST";

    private WifiApManager mApManager;
    private WifiConfiguration mApConfig;

    public static void startConnection(Context context) {
        Intent intent = new Intent(context, ReceiveActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
    }

    public static void finishConnection(Context context, ArrayList<FileItem> manifest) {
        Intent intent = new Intent(ACTION_RECEIVE_MANIFEST);
        intent.putExtra(EXTRA_FILE_ITEM, manifest);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_RECEIVE_MANIFEST)) {
                addTaskItem(intent);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        mApManager = new WifiApManager(this);

        mApConfig = new WifiConfiguration();
        mApConfig.SSID = WifiApManager.AP_SSID;
        mApConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                createHotspot();
            }
        }, 1000);

        //注册BroadCast
        LocalBroadcastManager mBroadcastManager = LocalBroadcastManager.getInstance(this);
        mBroadcastManager.registerReceiver(mBroadcastReceiver, new IntentFilter(ACTION_RECEIVE_MANIFEST));

        // 开HttpServer,注册servlet
        HttpdService.startService(this);
        ManifestServlet.register(this);
        ReceiveServlet.register(this);


        onConnecting();
    }

    @Override
    protected void onAddTaskItem(ArrayList<FileItem> fileItems) {
        for (FileItem item : fileItems) {
            Log.i(TAG, item.uri);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mApManager.setWifiApEnabled(mApConfig, false);
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        HttpdService.stopService(this);

        super.onDestroy();
    }


    /**
     * 开热点
     *
     * @return 是否成功
     */
    protected boolean createHotspot() {
        if (!hasCreated()) {
            boolean succeed = mApManager.setWifiApEnabled(mApConfig, true);

            Log.i(TAG, "Create Wifi Access Point: Succeeded: " + String.valueOf(succeed));

            return succeed;
        } else {
            Log.i(TAG, "Wifi Access Point has already been created");
            return true;
        }
    }

    /**
     * 热点是否已经开启
     *
     * @return 热点是否已经开启
     */
    protected boolean hasCreated() {
        if (mApManager.getWifiApState() == WifiApManager.WIFI_AP_STATE_ENABLED ||
                mApManager.getWifiApState() == WifiApManager.WIFI_AP_STATE_ENABLING) {

            WifiConfiguration config = mApManager.getWifiApConfiguration();

            if (mApConfig.SSID != null && mApConfig.SSID.equals(config.SSID) &&
                    mApConfig.BSSID != null && mApConfig.BSSID.equals(config.BSSID) &&
                    mApConfig.allowedKeyManagement != null &&
                    mApConfig.allowedKeyManagement.equals(config.allowedKeyManagement)) {

                return true;
            }
        }
        return false;
    }
}

package com.bbbbiu.biu.gui.transfer.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


import com.bbbbiu.biu.gui.transfer.FileItem;
import com.bbbbiu.biu.gui.transfer.TransferBaseActivity;
import com.bbbbiu.biu.lib.util.WifiApManager;
import com.bbbbiu.biu.lib.servlet.ManifestServlet;
import com.bbbbiu.biu.lib.servlet.android.ReceivingServlet;
import com.bbbbiu.biu.service.HttpdService;

import java.util.ArrayList;

/**
 * Android 端 接收来自Android 端的文件
 */
public class ReceivingActivity extends TransferBaseActivity {
    private static final String TAG = ReceivingActivity.class.getSimpleName();

    public static final String ACTION_RECEIVE_MANIFEST = "com.bbbbiu.biu.gui.transfer.android.ReceiveActivity.action.RECEIVE_MANIFEST";

    private WifiApManager mApManager;

    public static void startConnection(Context context) {
        Intent intent = new Intent(context, ReceivingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    public static void startReceiving(Context context, ArrayList<FileItem> manifest) {
        Intent intent = new Intent(ACTION_RECEIVE_MANIFEST);
        intent.putExtra(EXTRA_FILE_ITEM, manifest);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_RECEIVE_MANIFEST)) {
                addTask(intent);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        mApManager = new WifiApManager(this);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mApManager.createAp();
            }
        }, 1000);

        //注册BroadCast
        LocalBroadcastManager mBroadcastManager = LocalBroadcastManager.getInstance(this);
        mBroadcastManager.registerReceiver(mBroadcastReceiver, new IntentFilter(ACTION_RECEIVE_MANIFEST));

        // 开HttpServer,注册servlet
        HttpdService.startService(this);
        ManifestServlet.register(this, true);
        ReceivingServlet.register(this);


        showConnectingAnim();
    }

    @Override
    protected void onAddNewTask(ArrayList<FileItem> fileItems) {
        for (FileItem item : fileItems) {
            Log.i(TAG, item.uri);
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        HttpdService.stopService(this);

        mApManager.closeAp();

        super.onDestroy();
    }

    @Override
    protected void onCancelTransfer() {
        HttpdService.stopService(this);
    }
}

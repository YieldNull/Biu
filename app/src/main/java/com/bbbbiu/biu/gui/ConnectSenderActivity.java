package com.bbbbiu.biu.gui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.WindowManager;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.transfer.FileItem;
import com.bbbbiu.biu.gui.transfer.ReceiveActivity;
import com.bbbbiu.biu.lib.android.WifiApManager;
import com.bbbbiu.biu.lib.android.servlets.ManifestServlet;
import com.bbbbiu.biu.lib.android.servlets.ReceiveServlet;
import com.bbbbiu.biu.lib.httpd.HttpDaemon;
import com.bbbbiu.biu.lib.util.HttpConstants;
import com.bbbbiu.biu.service.HttpdService;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.util.ArrayList;

import butterknife.ButterKnife;

/**
 * 安卓接收方连接发送方。
 * 开启热点，然后开启HTTP服务器，
 * 等待发送方发送文件清单，成功接收到文件清单则表示连接成功。
 * <p/>
 * 当接收到Manifest({@link com.bbbbiu.biu.lib.android.servlets.ManifestServlet})时，发送BroadCast，
 * 接收到BroadCast后，跳转到接收页面 {@link com.bbbbiu.biu.gui.transfer.ReceiveActivity}
 */
public class ConnectSenderActivity extends AppCompatActivity {
    private static final String TAG = ConnectSenderActivity.class.getSimpleName();

    public static final String ACTION_RECEIVE_MANIFEST = "com.bbbbiu.biu.gui.ConnectSenderActivity.action.RECEIVE_MANIFEST";
    public static final String EXTRA_FILE_ITEM = "com.bbbbiu.biu.gui.ConnectSenderActivity.extra.FILE_ITEM";

    private WifiApManager mApManager;
    private WifiConfiguration mApConfig;


    public static void finishConnection(Context context, ArrayList<FileItem> manifest) {
        Intent intent = new Intent(ACTION_RECEIVE_MANIFEST);
        intent.putExtra(EXTRA_FILE_ITEM, manifest);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_RECEIVE_MANIFEST)) {
                ReceiveActivity.startTask(ConnectSenderActivity.this, intent.<FileItem>getParcelableArrayListExtra(EXTRA_FILE_ITEM));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_sender);

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
        HttpdService.startService(ConnectSenderActivity.this);
        ManifestServlet.register(this);
        ReceiveServlet.register(this);
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

            if (mApConfig.SSID.equals(config.SSID) &&
                    mApConfig.BSSID.equals(config.BSSID) &&
                    mApConfig.allowedKeyManagement.equals(config.allowedKeyManagement)) {

                return true;
            }
        }
        return false;
    }

}

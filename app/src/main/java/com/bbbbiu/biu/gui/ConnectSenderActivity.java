package com.bbbbiu.biu.gui;
import android.net.wifi.WifiConfiguration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.WindowManager;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.lib.android.WifiApManager;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import butterknife.ButterKnife;

public class ConnectSenderActivity extends AppCompatActivity {
    private static final String TAG = ConnectSenderActivity.class.getSimpleName();

    private WifiApManager mApManager;
    private WifiConfiguration mApConfig;


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

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mApManager.setWifiApEnabled(mApConfig, false);
    }

    protected void createHotspot() {
        boolean succeed = mApManager.setWifiApEnabled(mApConfig, true);

        Log.i(TAG, "Create Wifi Access Point: Succeeded: " + String.valueOf(succeed));
    }

}

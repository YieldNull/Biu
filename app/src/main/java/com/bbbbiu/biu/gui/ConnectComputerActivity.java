package com.bbbbiu.biu.gui;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.WindowManager;

import com.bbbbiu.biu.R;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class ConnectComputerActivity extends AppCompatActivity {

    private static final String ACTION_UPLOAD = "com.bbbbiu.biu.gui.ConnectComputerActivity.action.UPLOAD";
    private static final String ACTION_DOWNLOAD = "com.bbbbiu.biu.gui.ConnectComputerActivity.action.DOWNLOAD";
    private static final String TAG = ConnectComputerActivity.class.getSimpleName();

    private String action;


    public static void connectForUpload(Context context) {
        Intent intent = new Intent(context, ConnectComputerActivity.class);
        intent.setAction(ACTION_UPLOAD);
        context.startActivity(intent);
    }

    public static void connectForDownload(Context context) {
        Intent intent = new Intent(context, ConnectComputerActivity.class);
        intent.setAction(ACTION_DOWNLOAD);
        context.startActivity(intent);
    }

    @OnClick(R.id.button_scan)
    void scanQRCode() {
        if (action.equals(ACTION_DOWNLOAD)) {
            QRCodeScanActivity.scanForDownload(this);
        } else {
            QRCodeScanActivity.scanForUpload(this);
        }
    }


    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_computer);

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

        action = getIntent().getAction();

        Log.i(TAG, "Connecting with computer");
    }
}

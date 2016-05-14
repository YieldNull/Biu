package com.bbbbiu.biu.gui.transfer.computer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.WindowManager;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ConnectingActivity extends AppCompatActivity {
    private static final String TAG = ConnectingActivity.class.getSimpleName();

    private static final String ACTION_SEND = "com.bbbbiu.biu.gui.transfer.computer.ConnectComputerActivity.action.SEND";
    private static final String ACTION_RECEIVE = "com.bbbbiu.biu.gui.transfer.computer.ConnectComputerActivity.action.RECEIVE";

    public static void connectForSending(Context context) {
        Intent intent = new Intent(context, ConnectingActivity.class);
        intent.setAction(ACTION_SEND);
        context.startActivity(intent);
    }

    public static void connectForReceiving(Context context) {
        Intent intent = new Intent(context, ConnectingActivity.class);
        intent.setAction(ACTION_RECEIVE);
        context.startActivity(intent);
    }

    private String mAction;

    @OnClick(R.id.textView_computer_scan)
    void scanQRCode() {
        if (mAction.equals(ACTION_RECEIVE)) {
            QRCodeScanActivity.scanForDownload(this);
        } else {
            QRCodeScanActivity.scanForUpload(this);
        }
    }

    @OnClick(R.id.textView_computer_jump)
    void jumpToWebSending() {
        if (mAction.equals(ACTION_SEND)) {
            com.bbbbiu.biu.gui.transfer.apple.ConnectingActivity.connectForSending(this);
        } else {
            com.bbbbiu.biu.gui.transfer.apple.ConnectingActivity.connectForReceiving(this);
        }
    }

    @Bind(R.id.textView_computer_scan)
    TextView mScanQRCodeTextView;

    @Bind(R.id.textView_computer_jump)
    TextView mJumpTextView;


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

        mAction = getIntent().getAction();


        mScanQRCodeTextView.setPaintFlags(mScanQRCodeTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        mJumpTextView.setPaintFlags(mJumpTextView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
    }
}

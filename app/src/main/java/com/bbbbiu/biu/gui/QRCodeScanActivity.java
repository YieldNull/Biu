package com.bbbbiu.biu.gui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.bbbbiu.biu.client.HttpManager;
import com.google.zxing.Result;

import java.lang.ref.WeakReference;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

/**
 * Created by YieldNull at 3/22/16
 */

public class QRCodeScanActivity extends Activity implements ZXingScannerView.ResultHandler {
    private static final String TAG = QRCodeScanActivity.class.getSimpleName();

    public static final String INTENT_EXTRA_BIND_ACTION = "com.bbbbiu.biu.gui.QRCodeScanActivity.INTENT_EXTRA_BIND_ACTION";
    public static final int BIND_ACTION_UPLOAD = HttpManager.BIND_ACTION_UPLOAD;
    public static final int BIND_ACTION_DOWNLOAD = HttpManager.BIND_ACTION_DOWNLOAD;

    private static final int MSG_ENTER_RECEIVE_ACTIVITY = 0;
    private static final int MSG_ENTER_SEND_ACTIVITY = 1;


    private int mBindAction;
    private ZXingScannerView mScannerView;

    private String mUid;

    private Handler mHandler = new HandlerClass(this);


    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        mScannerView = new ZXingScannerView(this);
        setContentView(mScannerView);

        mScannerView.setResultHandler(this);

        mBindAction = getIntent().getExtras().getInt(INTENT_EXTRA_BIND_ACTION);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    @Override
    public void handleResult(Result rawResult) {
        Log.v(TAG, rawResult.getText());
        mUid = rawResult.getText();

        new Thread(new Runnable() {
            @Override
            public void run() {
                bind();
            }
        }).start();
    }

    private void bind() {
        if (HttpManager.bind(mUid, mBindAction)) {
            int what = mBindAction == BIND_ACTION_DOWNLOAD ? MSG_ENTER_RECEIVE_ACTIVITY : MSG_ENTER_SEND_ACTIVITY;
            mHandler.sendEmptyMessage(what);
        } else {
            Log.i(TAG, "Retry binding");//TODO
        }
    }


    private static class HandlerClass extends Handler {
        private final WeakReference<QRCodeScanActivity> mTarget;

        public HandlerClass(QRCodeScanActivity context) {
            mTarget = new WeakReference<>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            QRCodeScanActivity tar = mTarget.get();
            Intent intent;

            switch (msg.what) {
                case MSG_ENTER_RECEIVE_ACTIVITY:
                    intent = new Intent(tar, ReceiveActivity.class);
                    intent.putExtra(ReceiveActivity.INTENT_EXTRA_UID, tar.mUid);
                    tar.startActivity(intent);
                    break;
                case MSG_ENTER_SEND_ACTIVITY:
                    intent = new Intent(tar, SendActivity.class);
                    intent.putExtra(ReceiveActivity.INTENT_EXTRA_UID, tar.mUid);
                    tar.startActivity(intent);
                    break;
            }
        }
    }
}
package com.bbbbiu.biu.gui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.bbbbiu.biu.http.client.HttpConstants;
import com.google.zxing.Result;

import java.io.IOException;

import me.dm7.barcodescanner.zxing.ZXingScannerView;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by YieldNull at 3/22/16
 */

public class QRCodeScanActivity extends Activity implements ZXingScannerView.ResultHandler {
    private static final String TAG = QRCodeScanActivity.class.getSimpleName();

    public static final String EXTRA_BIND_ACTION = "com.bbbbiu.biu.gui.QRCodeScanActivity.extra.BIND_ACTION";

    public static final int ACTION_UPLOAD = HttpConstants.BIND_ACTION_UPLOAD;
    public static final int ACTION_DOWNLOAD = HttpConstants.BIND_ACTION_DOWNLOAD;

    private static final int MSG_ENTER_RECEIVE_ACTIVITY = 0;
    private static final int MSG_ENTER_SEND_ACTIVITY = 1;
    private static final int MSG_SERVER_ERROR = 2;

    private int mBindAction;
    private ZXingScannerView mScannerView;

    private String uid;

    private Handler mHandler;


    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        mScannerView = new ZXingScannerView(this);
        setContentView(mScannerView);

        mScannerView.setResultHandler(this);

        mBindAction = getIntent().getExtras().getInt(EXTRA_BIND_ACTION);

        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {

                Intent intent;

                switch (msg.what) {
                    case MSG_ENTER_RECEIVE_ACTIVITY:
                        intent = new Intent(QRCodeScanActivity.this, ReceiveActivity.class);
                        intent.putExtra(ReceiveActivity.EXTRA_UID, uid);
                        QRCodeScanActivity.this.startActivity(intent);
                        break;
                    case MSG_ENTER_SEND_ACTIVITY:
                        intent = new Intent(QRCodeScanActivity.this, SendActivity.class);
                        intent.putExtra(ReceiveActivity.EXTRA_UID, uid);
                        QRCodeScanActivity.this.startActivity(intent);
                        break;
                    case MSG_SERVER_ERROR:
                        Toast.makeText(QRCodeScanActivity.this, "服务器无响应，请稍后再试", Toast.LENGTH_SHORT).show();
                        break;
                }
                return false;
            }
        });
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
        uid = rawResult.getText();

        new Thread(new Runnable() {
            @Override
            public void run() {
                bind();
            }
        }).start();
    }

    private void bind() {
        int retry = 0;
        while (true) {
            if (bindServer()) {
                int what = mBindAction == ACTION_DOWNLOAD ? MSG_ENTER_RECEIVE_ACTIVITY : MSG_ENTER_SEND_ACTIVITY;
                mHandler.sendEmptyMessage(what);
                break;
            }
            retry++;

            if (retry > 10) {
                mHandler.sendEmptyMessage(MSG_SERVER_ERROR);
                break;
            }
        }
    }

    private boolean bindServer() {
        Request request = HttpConstants.newBindRequest(uid, mBindAction);
        Response response;
        try {
            response = HttpConstants.getHttpClient().newCall(request).execute();
        } catch (IOException e) {
            Log.i(TAG, "Bind server failed. HTTP error " + e.toString());
            return false;
        }

        if (response.code() == 200) {
            return true;
        } else {
            Log.i(TAG, "Bind server failed. Response status code " + response.code());
            return false;
        }
    }
}
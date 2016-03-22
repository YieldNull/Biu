package com.bbbbiu.biu.gui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.bbbbiu.biu.client.Constants;
import com.google.zxing.Result;

import java.io.IOException;
import java.lang.ref.WeakReference;

import me.dm7.barcodescanner.zxing.ZXingScannerView;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by YieldNull at 3/22/16
 */

public class QRCodeScanActivity extends Activity implements ZXingScannerView.ResultHandler {
    private static final String TAG = QRCodeScanActivity.class.getSimpleName();

    private ZXingScannerView mScannerView;


    private final OkHttpClient client = new OkHttpClient();

    private String uid;


    private Handler handler = new HandlerClass(this);

    private static final int MSG_ENTER_RECEIVE = 0;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        mScannerView = new ZXingScannerView(this);   // Programmatically initialize the scanner view
        setContentView(mScannerView);                // Set the scanner view as the content view

        mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
        mScannerView.startCamera();          // Start camera on resume
    }


    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();           // Stop camera on pause
    }

    @Override
    public void handleResult(Result rawResult) {
        // Do something with the result here
        Log.v(TAG, rawResult.getText()); // Prints scan results
        uid = rawResult.getText();

        new Thread(new Runnable() {
            @Override
            public void run() {
                bind(Constants.ACTION_DOWNLOAD);
            }
        }).start();
    }

    private void bind(String what) {
        Request request = new Request.Builder()
                .url(Constants.URL_BIND + "?uid=" + uid + "&what=" + what)
                .build();

        Response response = null;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            Log.w(TAG, "Http error", e);
            return;
        }

        if (!response.isSuccessful()) {
            Log.w(TAG, "Unexpected code " + response);
        }

        if (response.code() == 200) {
            handler.sendEmptyMessage(MSG_ENTER_RECEIVE);
        } else {
            Log.w(TAG, String.valueOf(response.code()));
        }
    }


    private static class HandlerClass extends Handler {
        private final WeakReference<QRCodeScanActivity> mTarget;

        public HandlerClass(QRCodeScanActivity context) {
            mTarget = new WeakReference<>(context);
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MSG_ENTER_RECEIVE:
                    QRCodeScanActivity tar = mTarget.get();
                    Intent intent = new Intent(tar, ReceiveActivity.class);
                    intent.putExtra(ReceiveActivity.INTENT_UID, tar.uid);

                    tar.startActivity(intent);
                    break;
            }
        }
    }
}
package com.bbbbiu.biu.gui;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.transfer.apple.ReceivingActivity;
import com.bbbbiu.biu.gui.transfer.FileItem;
import com.bbbbiu.biu.lib.util.HttpManager;
import com.bbbbiu.biu.lib.util.HttpConstants;
import com.bbbbiu.biu.util.PreferenceUtil;
import com.google.zxing.Result;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import butterknife.ButterKnife;
import me.dm7.barcodescanner.zxing.ZXingScannerView;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by YieldNull at 3/22/16
 */

public class QRCodeScanActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    private static final String TAG = QRCodeScanActivity.class.getSimpleName();


    public static final String ACTION_UPLOAD = "com.bbbbiu.biu.gui.QRCodeScanActivity.action.UPLOAD";
    public static final String ACTION_DOWNLOAD = "com.bbbbiu.biu.gui.QRCodeScanActivity.action.DOWNLOAD";

    private static final int MSG_ENTER_DOWNLOAD_ACTIVITY = 0;
    private static final int MSG_ENTER_UPLOAD_ACTIVITY = 1;
    private static final int MSG_SERVER_ERROR = 2;

    private String mBindAction;
    private ZXingScannerView mScannerView;

    private String mUid;

    private Handler mHandler;


    public static void scanForDownload(Context context) {
        Intent intent = new Intent(context, QRCodeScanActivity.class);
        intent.setAction(ACTION_DOWNLOAD);
        context.startActivity(intent);
    }

    public static void scanForUpload(Context context) {
        Intent intent = new Intent(context, QRCodeScanActivity.class);
        intent.setAction(ACTION_UPLOAD);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        ButterKnife.bind(this);

        // android 4.4 状态栏透明
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(getResources().getColor(R.color.colorPrimary));
        }

        mScannerView = new ZXingScannerView(this);
        setContentView(mScannerView);

        mScannerView.setResultHandler(this);

        mBindAction = getIntent().getAction();

        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_ENTER_DOWNLOAD_ACTIVITY:
                        Log.i(TAG, "Bind succeeded. Enter download activity");

                        ReceivingActivity.startReceiving(QRCodeScanActivity.this, null);
                        break;

                    case MSG_ENTER_UPLOAD_ACTIVITY:
                        Log.i(TAG, "Bind succeeded. Enter upload activity");

                        ArrayList<FileItem> fileItems = new ArrayList<>();
                        for (String path : PreferenceUtil.getFilesToSend(QRCodeScanActivity.this)) {
                            File file = new File(path);
                            fileItems.add(new FileItem(file.getAbsolutePath(), file.getName(), file.length()));

                        }
                        //UploadActivity.startUpload(QRCodeScanActivity.this, HttpConstants.Computer.getUploadUrl(mUid), fileItems);
                        break;

                    case MSG_SERVER_ERROR:
                        Log.i(TAG, "Server error. Stop retrying");
                        Toast.makeText(QRCodeScanActivity.this, R.string.hint_net_server_error, Toast.LENGTH_SHORT).show();
                        break;
                }
                return false;
            }
        });

        Log.i(TAG, "Scanning QRCode on desktop internet browser");
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

        Log.i(TAG, "The uid in QRCode is " + mUid);

        new Thread(new Runnable() {
            @Override
            public void run() {
                bind();
            }
        }).start();
    }

    private void bind() {
        Log.i(TAG, "Try to bind server");

        int retry = 0;
        while (true) {
            if (bindServer()) {
                int what = mBindAction.equals(ACTION_DOWNLOAD) ? MSG_ENTER_DOWNLOAD_ACTIVITY : MSG_ENTER_UPLOAD_ACTIVITY;
                mHandler.sendEmptyMessage(what);
                break;
            }
            retry++;

            Log.i(TAG, "Retry binding server");

            if (retry > 10) {
                mHandler.sendEmptyMessage(MSG_SERVER_ERROR);
                break;
            }
        }
    }

    private boolean bindServer() {
        String url;
        if (mBindAction.equals(ACTION_DOWNLOAD)) {
            url = HttpConstants.Computer.getBindDownloadUrl(mUid);
        } else {
            url = HttpConstants.Computer.getBindUploadUrl(mUid);
        }

        Request request = HttpManager.newRequest(url);

        Response response;
        ResponseBody body = null;

        try {
            try {
                response = HttpManager.newHttpClient().newCall(request).execute();
                body = response.body();
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
        } finally {
            if (body != null) {
                body.close();
            }
        }
    }
}
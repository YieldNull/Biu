package com.bbbbiu.biu.gui.transfer.computer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.lib.HttpConstants;
import com.bbbbiu.biu.lib.HttpManager;
import com.bbbbiu.biu.util.NetworkUtil;
import com.bbbbiu.biu.util.PreferenceUtil;
import com.google.zxing.Result;

import java.io.IOException;

import butterknife.ButterKnife;
import me.dm7.barcodescanner.zxing.ZXingScannerView;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 扫描二维码连接服务器。
 * <p/>
 * 根据扫描得到的uid，尝试连接服务器。
 * <p/>
 * 若连接成功，则跳转到相应的上传或下载页面。失败{@link #RETRY_THRESHOLD}次则报错。
 * <p/>
 * Created by YieldNull at 3/22/16
 */

public class QRCodeScanActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    private static final String TAG = QRCodeScanActivity.class.getSimpleName();


    public static final String ACTION_UPLOAD = "com.bbbbiu.biu.gui.transfer.computer.QRCodeScanActivity.action.UPLOAD";
    public static final String ACTION_DOWNLOAD = "com.bbbbiu.biu.gui.transfer.computer.QRCodeScanActivity.action.DOWNLOAD";

    private static final int MSG_ENTER_DOWNLOAD_ACTIVITY = 0;
    private static final int MSG_ENTER_UPLOAD_ACTIVITY = 1;
    private static final int MSG_SERVER_ERROR = 2;
    private static final int MSG_DEVICE_OFFLINE = 3;

    /**
     * 连接服务器重试次数
     */
    private static final int RETRY_THRESHOLD = 10;

    private String mBindAction;
    private ZXingScannerView mScannerView;

    private String mUid;

    private Handler mHandler;


    public static void scanForDownload(Context context) {
        Intent intent = new Intent(context, QRCodeScanActivity.class);
        intent.setAction(ACTION_DOWNLOAD);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
    }

    public static void scanForUpload(Context context) {
        Intent intent = new Intent(context, QRCodeScanActivity.class);
        intent.setAction(ACTION_UPLOAD);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        ButterKnife.bind(this);


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
                        DownloadActivity.startDownload(QRCodeScanActivity.this, mUid);
                        break;

                    case MSG_ENTER_UPLOAD_ACTIVITY:
                        Log.i(TAG, "Bind succeeded. Enter upload activity");

                        UploadActivity.startUpload(QRCodeScanActivity.this, mUid,
                                PreferenceUtil.getFileItemsToSend(QRCodeScanActivity.this));
                        break;

                    case MSG_SERVER_ERROR:
                        Log.i(TAG, "Server error. Stop retrying");
                        Toast.makeText(QRCodeScanActivity.this, R.string.hint_connect_server_error, Toast.LENGTH_LONG).show();
                        finish();
                        break;
                    case MSG_DEVICE_OFFLINE:
                        Log.i(TAG, "Device is offline. Stop retrying");
                        Toast.makeText(QRCodeScanActivity.this, R.string.hint_connect_device_offline, Toast.LENGTH_LONG).show();
                        finish();
                        break;
                    default:
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
                tryBind();
            }
        }).start();
    }


    /**
     * 尝试连接服务器，重试一定次数之后就报错
     */
    private void tryBind() {
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

            if (retry > RETRY_THRESHOLD) {

                if (NetworkUtil.isOnline(QRCodeScanActivity.this)) {
                    mHandler.sendEmptyMessage(MSG_SERVER_ERROR);
                } else {
                    mHandler.sendEmptyMessage(MSG_DEVICE_OFFLINE);
                }
                break;
            }
        }
    }


    /**
     * 连接服务器
     *
     * @return 是否连接成功
     */
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
                Log.i(TAG, "Bind server failed. HTTP error " + e.getMessage());
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
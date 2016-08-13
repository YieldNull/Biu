package com.bbbbiu.biu.gui.transfer.apple;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.util.Log;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.transfer.OpenApThread;
import com.bbbbiu.biu.lib.HttpConstants;
import com.bbbbiu.biu.lib.WifiApManager;
import com.bbbbiu.biu.lib.servlet.ManifestServlet;
import com.bbbbiu.biu.lib.servlet.apple.DownloadServlet;
import com.bbbbiu.biu.lib.servlet.apple.FileIconServlet;
import com.bbbbiu.biu.lib.servlet.apple.FileServlet;
import com.bbbbiu.biu.lib.servlet.apple.UploadServlet;
import com.bbbbiu.biu.service.HttpdService;
import com.bbbbiu.biu.util.NetworkUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.yieldnull.httpd.HttpDaemon;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * 连接apple提示界面，开启httpd。
 * <p/>
 * 也是给apple发文件的界面，发文件不显示进度。因为用户要是重复下载两次呢？
 * 用于发文件时，按返回键时要提醒用户。
 */
public class ConnectingActivity extends AppCompatActivity {
    private static final String TAG = ConnectingActivity.class.getSimpleName();

    private static final String ACTION_SENDING = "com.bbbbiu.biu.gui.transfer.apple.ConnectAppleActivity.action.UPLOAD";
    private static final String ACTION_RECEIVING = "com.bbbbiu.biu.gui.transfer.apple.ConnectAppleActivity.action.DOWNLOAD";
    private static final String EXTRA_USING_ROUTER = "com.bbbbiu.biu.gui.transfer.apple.ConnectAppleActivity.extra.ROUTER";

    public static void connectForSending(Context context) {
        connectForSending(context, false);
    }

    public static void connectForReceiving(Context context) {
        connectForReceiving(context, false);
    }

    public static void connectForSending(Context context, boolean router) {
        Intent intent = new Intent(context, ConnectingActivity.class);
        intent.setAction(ACTION_SENDING);
        intent.putExtra(EXTRA_USING_ROUTER, router);

        context.startActivity(intent);
    }

    public static void connectForReceiving(Context context, boolean router) {
        Intent intent = new Intent(context, ConnectingActivity.class);
        intent.setAction(ACTION_RECEIVING);

        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        intent.putExtra(EXTRA_USING_ROUTER, router);
        context.startActivity(intent);
    }


    private String action;

    private boolean usingRouter;


    @Bind(R.id.imageView)
    ImageView mQRCodeImage;

    @Bind(R.id.textView_home)
    TextView mHomeUrlText;

    @Bind(R.id.textView_wifi)
    TextView mWifiNameText;

    private WifiManager mWifiManager;
    private WifiApManager mApManager;


    private boolean mIsWifiOpened;
    private boolean mIsMobileOpened;


    /**
     * 等待WIFI打开
     */
    private Thread mApThread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_apple);
        ButterKnife.bind(this);

        // Toolbar
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
        usingRouter = getIntent().getBooleanExtra(EXTRA_USING_ROUTER, false);

        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo info = mWifiManager.getConnectionInfo();

        String ipAddress = null;
        String wifiName;

        if (usingRouter && info != null) {

            ipAddress = Formatter.formatIpAddress(info.getIpAddress());
            wifiName = info.getSSID();

            Log.i(TAG, "Using router. IP:" + ipAddress);
        } else {
            Log.i(TAG, "Using ap");

            usingRouter = false;
            wifiName = WifiApManager.AP_SSID;
            initAp();
        }


        Log.i(TAG, "Wifi name:" + wifiName);
        mWifiNameText.setText(wifiName);

        final String homeUrl = ipAddress == null ? HttpConstants.Apple.HOME_URL : "http://" + ipAddress + ":" + HttpDaemon.sPort;
        Log.i(TAG, "Web Home URL:" + homeUrl);
        mHomeUrlText.setText(homeUrl);


        // 等待placeholder绘制完成，以计算二维码的长度
        ViewTreeObserver observer = mQRCodeImage.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mQRCodeImage.getViewTreeObserver().removeGlobalOnLayoutListener(this);

                // 非 UI线程生产二维码，然后在UI线程绘制
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final Bitmap qrcode = genQRCode(homeUrl);

                        ConnectingActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mQRCodeImage.setImageBitmap(qrcode);
                            }
                        });
                    }
                }).start();
            }
        });


        // 开HttpServer,注册servlet
        HttpdService.startService(this);

        if (action.equals(ACTION_RECEIVING)) {
            ManifestServlet.register(this, false);
            UploadServlet.register(this);
        } else {
            DownloadServlet.register(this);
            FileServlet.register(this);
        }

        FileIconServlet.register(this);

    }

    @Override
    public void onBackPressed() {
        if (action.equals(ACTION_SENDING) && HttpDaemon.getSingleton().aliveRequests() > 0) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.hint_transfer_abort_confirm))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ConnectingActivity.super.onBackPressed();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (action.equals(ACTION_SENDING)) {
            HttpdService.stopService(this);
        }

        if (!usingRouter) {
            stopAp();
        }
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * 初始化AP，记录当前网络状态
     */
    private void initAp() {
        ConnectivityManager mConnManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        mApManager = new WifiApManager(this);

        mIsWifiOpened = mWifiManager.isWifiEnabled();

        // 检测数据流量是否打开，好像有点测不准2333
        NetworkInfo mobileInfo = mConnManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        String reason = mobileInfo.getReason();
        boolean mobileDisabled = mobileInfo.getState() == NetworkInfo.State.DISCONNECTED
                && (reason == null || reason.equals("specificDisabled"));
        mIsMobileOpened = !mobileDisabled;

        Log.i(TAG, "Is WIFI opened? " + mIsWifiOpened);
        Log.i(TAG, "Is Mobile opened? " + mIsMobileOpened);


        final Handler mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == OpenApThread.MSG_FAILED) {
                    Toast.makeText(ConnectingActivity.this, R.string.hint_connect_ap_create_failed,
                            Toast.LENGTH_LONG).show();

                } else {
                    Toast.makeText(ConnectingActivity.this, R.string.hint_connect_ap_create_succeeded,
                            Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });

        mApThread = new OpenApThread(this, mHandler, mIsMobileOpened);


        // 提交 AP Thread，关数据流量，开启WIFI AP, 知道成功打开AP 为止
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Creating Wifi AP");
                mApThread.start();
            }
        }, 1000);

    }


    /**
     * Destroy 时，关闭AP，恢复之前的网络连接
     */
    private void stopAp() {
        // wifi 没有开启成功，一直在循环等待
        if (mApThread.isAlive()) {
            Log.i(TAG, "Wifi AP was created but has not started. Interrupt ap thread");
            mApThread.interrupt();
        }

        Log.i(TAG, "Closing Wifi AP if it has been created");
        mApManager.closeAp();


        if (mIsMobileOpened) {
            NetworkUtil.setMobileDataEnabled(this, true);
            Log.i(TAG, "Restore mobile state. Opening it");
        }

        if (mIsWifiOpened) {
            mWifiManager.setWifiEnabled(true);

            Log.i(TAG, "Restore wifi state. Opening it");
        }
    }

    /**
     * 生成二维码
     *
     * @param url 二维码包含的URL信息
     * @return Bitmap
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private Bitmap genQRCode(String url) {

        // 等待placeholder绘制完成之后调用
        int width = mQRCodeImage.getMeasuredWidth();

        Bitmap bitmap = null;
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bm = writer.encode(url, BarcodeFormat.QR_CODE, width, width);
            bitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < width; j++) {
                    bitmap.setPixel(i, j, bm.get(i, j) ? Color.BLACK : Color.WHITE);
                }
            }
        } catch (WriterException e) {
            Log.w(TAG, e.toString());
        }
        return bitmap;
    }
}

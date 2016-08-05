package com.bbbbiu.biu.gui.transfer.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.transfer.FileItem;
import com.bbbbiu.biu.gui.transfer.TransferBaseActivity;
import com.bbbbiu.biu.lib.WifiApManager;
import com.bbbbiu.biu.lib.servlet.ManifestServlet;
import com.bbbbiu.biu.lib.servlet.android.ReceivingServlet;
import com.bbbbiu.biu.service.HttpdService;
import com.bbbbiu.biu.util.SizeUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Android 端 接收来自Android 端的文件。
 * <p/>
 * 开启HTTP服务器，开启WIFI热点，关闭数据流量。若WIFI AP一段时间内没有开启，则退出传输
 */
public class ReceivingActivity extends TransferBaseActivity {
    private static final String TAG = ReceivingActivity.class.getSimpleName();

    public static final String ACTION_RECEIVE_MANIFEST = "com.bbbbiu.biu.gui.transfer.android.ReceiveActivity.action.RECEIVE_MANIFEST";


    /**
     * 开始等待连接
     *
     * @param context context
     */
    public static void startConnection(Context context) {
        Intent intent = new Intent(context, ReceivingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    /**
     * 接收到文件清单，准备接收文件
     *
     * @param context  context
     * @param manifest 文件清单
     */
    public static void startReceiving(Context context, ArrayList<FileItem> manifest) {
        Intent intent = new Intent(ACTION_RECEIVE_MANIFEST);
        intent.putExtra(EXTRA_FILE_ITEM, manifest);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }


    /**
     * 接收发送方发过来的文件清单，并提交任务
     */
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_RECEIVE_MANIFEST)) {
                addTask(intent);
            }
        }
    };


    private WifiManager mWifiManager;
    private WifiApManager mApManager;
    private ConnectivityManager mConnManager;


    private boolean mIsWifiOpened;
    private boolean mIsMobileOpened;


    /**
     * 主线程Handler
     */
    private Handler mHandler;

    /**
     * 等待WIFI打开
     */
    private Thread mApThread;


    /**
     * 在 AP Thread 检查WIFI是否开启的时间间隔
     */
    private static final long AP_CHECK_INTERVAL = 1000;


    /**
     * 检查重试次数阈值，达到则放弃传输
     */
    private static final int AP_CHECK_RETRY_THRESHOLD = 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mConnManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
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


        mHandler = new Handler();

        mApThread = new Thread(new Runnable() {
            @Override
            public void run() {

                if (mIsMobileOpened) {
                    Log.i(TAG, "Turn off mobile data");


                    // 尝试关闭数据流量
                    if (!setMobileDataEnabled(false)) {

                        // Android 5.0+ 关闭失败，请用户手动关闭
                        Log.i(TAG, "Failed in closing mobile network");

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast toast = Toast.makeText(ReceivingActivity.this, R.string.hint_connect_mobile_close_manually,
                                        Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                                        0, (int) SizeUtil.convertDpToPixel(10));

                                toast.show();
                            }
                        });

                    } else {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast toast = Toast.makeText(ReceivingActivity.this, R.string.hint_connect_mobile_disabled,
                                        Toast.LENGTH_LONG);

                                toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                                        0, (int) SizeUtil.convertDpToPixel(10));

                                toast.show();
                            }
                        });
                    }
                }


                // 尝试创建WIFI AP， 失败则直接退出传输
                if (!mApManager.createAp()) {
                    openApFailed();
                    return;
                }


                // 等待 WIFI AP成功开启，一段时间内没有成功开启则直接退出传输
                int retryCount = 0;
                while (mApManager.getWifiApState() != WifiApManager.WIFI_AP_STATE_ENABLED) {
                    try {
                        Log.i(TAG, "Wifi AP not started yet. Waiting...");
                        Thread.sleep(AP_CHECK_INTERVAL);
                    } catch (InterruptedException e) {
                        Log.w(TAG, "Ap Thread interrupted. Quit thread");
                        return;
                    }

                    retryCount++;

                    if (retryCount >= AP_CHECK_RETRY_THRESHOLD) {
                        openApFailed();
                        return;
                    }

                    if (Thread.interrupted()) {
                        return;
                    }
                }

                Log.i(TAG, "Wifi AP started. Waiting for connection");
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        updateLoadingText(getString(R.string.hint_connect_ap_opened));
                    }
                });
            }
        });


        // 提交 AP Thread，关数据流量，开启WIFI AP, 知道成功打开AP 为止
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Creating Wifi AP");
                mApThread.start();
            }
        }, 1000);


        // 注册BroadCast
        LocalBroadcastManager mBroadcastManager = LocalBroadcastManager.getInstance(this);
        mBroadcastManager.registerReceiver(mBroadcastReceiver, new IntentFilter(ACTION_RECEIVE_MANIFEST));

        // 开HttpServer,注册servlet
        HttpdService.startService(this);
        ManifestServlet.register(this, true);
        ReceivingServlet.register(this);


        showConnectingAnim();
        updateLoadingText(getString(R.string.hint_connect_ap_opening));
    }

    @Override
    protected void onAddNewTask(ArrayList<FileItem> fileItems) {
        for (FileItem item : fileItems) {
            Log.i(TAG, item.uri);
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        HttpdService.stopService(this);


        // wifi 没有开启成功，一直在循环等待
        if (mApThread.isAlive()) {
            Log.i(TAG, "Wifi AP was created but has not started. Interrupt ap thread");
            mApThread.interrupt();
        }

        Log.i(TAG, "Closing Wifi AP if it has been created");
        mApManager.closeAp();


        if (mIsMobileOpened) {
            setMobileDataEnabled(true);
            Log.i(TAG, "Restore mobile state. Opening it");
        }

        if (mIsWifiOpened) {
            mWifiManager.setWifiEnabled(true);

            Log.i(TAG, "Restore wifi state. Opening it");
        }


        super.onDestroy();
    }

    @Override
    protected void onTransferCanceled() {
        onTransferFinished();
    }

    @Override
    protected void onTransferFinished() {
        HttpdService.stopService(this);
    }


    /**
     * 开启WIFI 热点失败了，退出界面
     */
    private void openApFailed() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ReceivingActivity.this, R.string.hint_connect_ap_create_failed,
                        Toast.LENGTH_LONG).show();

                finish();
            }
        });
    }


    /**
     * 开启或关闭数据流量，只对Android 4 有效
     *
     * @param enabled 是否开启
     * @return 是否执行成功
     */
    boolean setMobileDataEnabled(boolean enabled) {
        try {

            final Class<?> conmanClass = Class.forName(mConnManager.getClass()
                    .getName());
            final Field iConnectivityManagerField = conmanClass
                    .getDeclaredField("mService");
            iConnectivityManagerField.setAccessible(true);
            final Object iConnectivityManager = iConnectivityManagerField
                    .get(mConnManager);
            final Class<?> iConnectivityManagerClass = Class
                    .forName(iConnectivityManager.getClass().getName());
            final Method setMobileDataEnabledMethod = iConnectivityManagerClass
                    .getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
            setMobileDataEnabledMethod.setAccessible(true);
            setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
            return true;

        } catch (Exception e) {
            Log.w("Mobile Data", "error turning on/off data");
            return false;
        }
    }
}

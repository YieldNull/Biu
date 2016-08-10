package com.bbbbiu.biu.gui.transfer;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.lib.WifiApManager;
import com.bbbbiu.biu.util.NetworkUtil;
import com.bbbbiu.biu.util.SizeUtil;

/**
 * 用于开启Wifi 热点的线程
 * <p/>
 * Created by yieldnull on 8/10/16.
 */
public class OpenApThread extends Thread {
    private static final String TAG = OpenApThread.class.getSimpleName();


    /**
     * 在 AP Thread 检查WIFI是否开启的时间间隔
     */
    private static final long AP_CHECK_INTERVAL = 1000;


    /**
     * 检查重试次数阈值，达到则放弃传输
     */
    private static final int AP_CHECK_RETRY_THRESHOLD = 15;


    /**
     * 开启失败
     */
    public static final int MSG_FAILED = 1;

    /**
     * 开启成功
     */
    public static final int MSG_SUCCEEDED = 2;


    private Context context;
    private Handler mHandler;
    private WifiApManager mApManager;

    /**
     * 是否已经打开数据流量
     */
    private boolean mIsMobileOpened;

    public OpenApThread(Context context, Handler handler, boolean isMobileOpened) {
        this.context = context;

        mHandler = handler;
        mIsMobileOpened = isMobileOpened;

        mApManager = new WifiApManager(context);
    }


    @Override
    public void run() {
        if (mIsMobileOpened) {
            Log.i(TAG, "Turn off mobile data");


            // 尝试关闭数据流量
            if (!NetworkUtil.setMobileDataEnabled(context, false)) {

                // Android 5.0+ 关闭失败，请用户手动关闭
                Log.i(TAG, "Failed in closing mobile network");

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast toast = Toast.makeText(context, R.string.hint_connect_mobile_close_manually,
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
                        Toast toast = Toast.makeText(context, R.string.hint_connect_mobile_disabled,
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
            mHandler.sendEmptyMessage(MSG_FAILED);
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
                mHandler.sendEmptyMessage(MSG_FAILED);
                return;
            }

            if (Thread.interrupted()) {
                return;
            }
        }

        Log.i(TAG, "Wifi AP started. Waiting for connection");
        mHandler.sendEmptyMessage(MSG_SUCCEEDED);
    }
}

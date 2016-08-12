package com.bbbbiu.biu.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.format.Formatter;
import android.util.Log;

import com.bbbbiu.biu.lib.servlet.DefaultStaticServlet;
import com.yieldnull.httpd.HttpDaemon;

import java.io.IOException;

/**
 * Httpd Service。启动时注册{@link DefaultStaticServlet}
 */
public class HttpdService extends Service {
    private static final String TAG = HttpdService.class.getSimpleName();

    private static final String ACTION_START = "com.bbbbiu.biu.service.HttpdService.action.START";
    private static final String ACTION_STOP = "com.bbbbiu.biu.service.HttpdService.action.STOP";


    private final HttpDaemon mHttpd = HttpDaemon.getSingleton();

    /**
     * 开启Http 服务器
     *
     * @param context context
     */
    public static void startService(Context context) {
        Intent intent = new Intent(context, HttpdService.class);
        intent.setAction(ACTION_START);
        context.startService(intent);
        Log.i(TAG, "Starting HttpdService");
    }

    /**
     * 关闭Http 服务器
     *
     * @param context context
     */
    public static void stopService(Context context) {
        Intent intent = new Intent(context, HttpdService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
        Log.i(TAG, "Stopping HttpdService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final String action = intent.getAction();
            switch (action) {
                case ACTION_START:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            startHttpd();
                        }
                    }).start();
                    break;
                case ACTION_STOP:
                    stopSelf();
                    break;
                default:
                    break;
            }
        }
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        new Thread(new Runnable() {
            @Override
            public void run() {
                stopHttpd();
            }
        }).start();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 开启 httpd
     */
    private void startHttpd() {
        if (!mHttpd.isAlive()) {
            if (mHttpd.isStarted()) {
                mHttpd.stop();
                Log.i(TAG, "HttpdServer was started but Not alive, Stop it and reStart");
            }

            try {
                mHttpd.start();

                DefaultStaticServlet.register(this); // 默认注册

                Log.i(TAG, "HttpdServer Started at " + getListenAddress());
            } catch (IOException e) {
                Log.e(TAG, "HttpdServer Failed Starting");
            }
        }
    }


    /**
     * 关闭httpd
     */
    private void stopHttpd() {
        mHttpd.stop();
        Log.i(TAG, "HttpdServer Stopped");
    }

    /**
     * 获取httpd bind的地址
     *
     * @return 地址，如192.168.1.1:8080
     */
    private String getListenAddress() {
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        return ip + ":" + HttpDaemon.getPort();
    }
}

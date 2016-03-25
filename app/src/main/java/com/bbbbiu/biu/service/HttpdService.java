package com.bbbbiu.biu.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.format.Formatter;
import android.util.Log;

import com.bbbbiu.biu.http.server.HttpDaemon;
import com.bbbbiu.biu.http.server.HttpServlet;
import com.bbbbiu.biu.http.server.servlets.StaticServlet;

import java.io.IOException;


public class HttpdService extends Service {
    private static final String TAG = HttpdService.class.getSimpleName();


    private final HttpDaemon mHttpd = new HttpDaemon(8080);
    private final IBinder mBinder = new HttpdServiceBinder();

    private static final String ACTION_UPLOAD = "com.bbbbiu.biu.service.HttpdService.ACTION_UPLOAD";
    private static final String ACTION_DOWNLOAD = "com.bbbbiu.biu.service.HttpdService.ACTION_DOWNLOAD";

    public class HttpdServiceBinder extends Binder {
        public HttpdService getService() {
            return HttpdService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_UPLOAD.equals(action)) {
                mHttpd.setIsUpload();
            } else if (ACTION_DOWNLOAD.equals(action)) {
                mHttpd.setIsDownload();
            }
        }
        startHttpd();

        return Service.START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        closeHttpd();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "Service binned");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "Service unBinned");
        return super.onUnbind(intent);
    }

    public static void startUpload(Context context) {
        Intent intent = new Intent(context, HttpdService.class);
        intent.setAction(ACTION_UPLOAD);
        context.startService(intent);
    }

    public static void startDownload(Context context) {
        Intent intent = new Intent(context, HttpdService.class);
        intent.setAction(ACTION_DOWNLOAD);
        context.startService(intent);
    }


    private void startHttpd() {
        if (!mHttpd.isAlive()) {
            if (mHttpd.wasStarted()) {
                mHttpd.stop();
                Log.i(TAG, "HttpdServer was started but Not alive, Stop it and reStart");
            }

            try {
                mHttpd.start();

                HttpServlet servlet = new StaticServlet(this);
                HttpDaemon.regServlet(HttpDaemon.STATIC_FILE_REG, servlet);
                HttpDaemon.regServlet("/", servlet);

                Log.i(TAG, "HttpdServer Started at " + getListenAddress());
            } catch (IOException e) {
                Log.e(TAG, "HttpdServer Start Failed");
            }
        }
    }


    private void closeHttpd() {
        mHttpd.stop();
        Log.i(TAG, "HttpdServer Stopped");
    }

    private String getListenAddress() {
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        return ip + ":" + mHttpd.getPort();
    }
}

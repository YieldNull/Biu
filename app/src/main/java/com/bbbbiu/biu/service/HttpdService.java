package com.bbbbiu.biu.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bbbbiu.biu.httpd.HttpDaemon;

import java.io.IOException;


public class HttpdService extends Service {
    private static final String TAG = HttpdService.class.getSimpleName();


    private final HttpDaemon mHttpd = new HttpDaemon(8080);
    private final IBinder mBinder = new HttpdServiceBinder();

    public class HttpdServiceBinder extends Binder {
        public HttpdService getService() {
            return HttpdService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startHttpd();
        return Service.START_STICKY;
    }

    private void startHttpd() {
        if (!mHttpd.isAlive()) {
            if (mHttpd.wasStarted()) {
                mHttpd.stop();
                Log.i(TAG, "HttpdServer was started but Not alive, Stop it and reStart");
            }

            try {
                mHttpd.start();
                Log.i(TAG, "HttpdServer Started");
            } catch (IOException e) {
                Log.e(TAG, "HttpdServer Start Failed");
            }
        }
    }

    private void closeHttpd() {
        mHttpd.stop();
        Log.i(TAG, "HttpdServer Stopped");
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        closeHttpd();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

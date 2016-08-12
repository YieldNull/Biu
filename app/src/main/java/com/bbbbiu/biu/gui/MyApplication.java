package com.bbbbiu.biu.gui;

import android.app.Application;
import android.util.Log;

import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.tencent.bugly.crashreport.CrashReport;
import com.yieldnull.httpd.Streams;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by YieldNull at 5/14/16
 */
public class MyApplication extends Application {
    private static final String TAG = MyApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        FlowManager.init(new FlowConfig.Builder(this).build());

        Properties properties = new Properties();
        InputStream stream = null;
        try {
            stream = getAssets().open("conf.properties");
            properties.load(stream);
            CrashReport.initCrashReport(getApplicationContext(), properties.getProperty("bugly.appId"), false);
        } catch (IOException e) {
            Log.w(TAG, e);
        } finally {
            Streams.safeClose(stream);
        }
    }
}

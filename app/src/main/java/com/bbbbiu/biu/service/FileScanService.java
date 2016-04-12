package com.bbbbiu.biu.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import com.bbbbiu.biu.util.SearchUtil;

public class FileScanService extends IntentService {

    private static final String TAG = FileScanService.class.getSimpleName();

    private static final long ALARM_INTERVAL = AlarmManager.INTERVAL_HOUR / 2;

    public FileScanService() {
        super("FileScanService");
    }

    /**
     * 启动服务
     *
     * @param context
     */
    public static void startScan(Context context) {
        Intent intent = new Intent(context, FileScanService.class);
        context.startService(intent);

        Log.i(TAG, "FileScanService started");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            SearchUtil.scanDisk(this);
        }
    }

    /**
     * 接收到
     */
    public static class ScanAlarmReceiver extends BroadcastReceiver {

        private static final String TAG = ScanAlarmReceiver.class.getSimpleName();

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Received alarm. Starting FileScanService");

            FileScanService.startScan(context);// 启动服务
        }
    }


    /**
     * 创建PendingIntent，将要发起其对应的广播
     *
     * @param context context
     * @return PendingIndent
     */
    public static PendingIntent createPendingIntent(Context context) {
        Intent intent = new Intent(context, FileScanService.ScanAlarmReceiver.class);

        return PendingIntent.getBroadcast(context, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * 设置Alarm
     *
     * @param context context
     */
    public static void scheduleAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        PendingIntent intent = createPendingIntent(context);
        alarmManager.cancel(intent);

        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(),
                ALARM_INTERVAL,
                createPendingIntent(context)
        );
    }
}

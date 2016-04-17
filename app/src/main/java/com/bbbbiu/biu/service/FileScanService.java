package com.bbbbiu.biu.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import com.bbbbiu.biu.util.SearchUtil;
import com.bbbbiu.biu.util.db.ApkItem;
import com.bbbbiu.biu.util.db.FileItem;
import com.orm.SugarRecord;

public class FileScanService extends IntentService {

    private static final String TAG = FileScanService.class.getSimpleName();

    private static final long ALARM_INTERVAL = AlarmManager.INTERVAL_HOUR / 2;

    public FileScanService() {
        super("FileScanService");
    }

    /**
     * 启动服务
     *
     * @param context context
     */
    public static void startScan(Context context) {
        Intent intent = new Intent(context, FileScanService.class);
        context.startService(intent);

        Log.i(TAG, "FileScanService started");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            Log.i(TAG, "Stating scan disk and apk");

            SearchUtil.startSearch(this);
        }
    }

    /**
     * 接收到
     */
    public static class ScanAlarmReceiver extends BroadcastReceiver {

        private static final String TAG = ScanAlarmReceiver.class.getSimpleName();
        public static final String ACTION_SCAN = "com.bbbbiu.biu.service.FileScanService$ScanAlarmReceiver.action_SCAN";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (action != null) {
                    if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                        Log.i(TAG, "Received Boot Completed Broadcast. Starting FileScanService");
                        FileScanService.startScan(context);// 启动服务，接收Alarm或者Boot BroadCast

                    } else if (action.equals(ACTION_SCAN)) {
                        Log.i(TAG, "Received scan alarm. Starting FileScanService");
                        FileScanService.startScan(context);// 启动服务，接收Alarm或者Boot BroadCast
                    }
                }
            }
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
        intent.setAction(ScanAlarmReceiver.ACTION_SCAN);
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

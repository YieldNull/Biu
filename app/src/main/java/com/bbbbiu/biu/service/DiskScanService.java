package com.bbbbiu.biu.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bbbbiu.biu.util.SearchUtil;

public class DiskScanService extends Service {

    private static final String TAG = DiskScanService.class.getSimpleName();

    private static final String ACTION_START = "com.bbbbiu.service.DiskScanService.action_START";
    private static final String ACTION_STOP = "com.bbbbiu.service.DiskScanService.action_STOP";

    private static final long ALARM_INTERVAL = AlarmManager.INTERVAL_HOUR / 2;


    /**
     * 启动服务
     *
     * @param context context
     */
    public static void startService(Context context) {
        Intent intent = new Intent(context, DiskScanService.class);
        intent.setAction(ACTION_START);
        context.startService(intent);

        Log.i(TAG, "Starting Service");
    }

    public static void stopService(Context context) {
        Intent intent = new Intent(context, DiskScanService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);

        Log.i(TAG, "Stopping Service");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return super.onStartCommand(intent, flags, startId);
        }
        String action = intent.getAction();

        if (action != null && action.equals(ACTION_START)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SearchUtil.startSearch(DiskScanService.this);
                }
            }).start();
        } else {
            stopSelf();
        }

        return START_STICKY;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    /**
     * 设置Alarm
     *
     * @param context context
     */
    public static void scheduleAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        PendingIntent intent = ScanAlarmReceiver.createPendingIntent(context);
        alarmManager.cancel(intent);

        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(),
                ALARM_INTERVAL,
                ScanAlarmReceiver.createPendingIntent(context)
        );
    }

    /**
     * 接收到
     */
    public static class ScanAlarmReceiver extends BroadcastReceiver {

        private static final String TAG = ScanAlarmReceiver.class.getSimpleName();
        public static final String ACTION_SCAN = "com.bbbbiu.biu.service.FileScanService$ScanAlarmReceiver.action_SCAN";

        /**
         * 创建PendingIntent，将要发起其对应的广播
         *
         * @param context context
         * @return PendingIndent
         */
        public static PendingIntent createPendingIntent(Context context) {
            Intent intent = new Intent(context, ScanAlarmReceiver.class);
            intent.setAction(ACTION_SCAN);
            return PendingIntent.getBroadcast(context, 0,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (action != null) {
                    if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                        Log.i(TAG, "Received Boot Completed Broadcast. Starting FileScanService");
                        DiskScanService.startService(context);// 启动服务，接收Alarm或者Boot BroadCast

                    } else if (action.equals(ACTION_SCAN)) {
                        Log.i(TAG, "Received scan alarm. Starting FileScanService");
                        DiskScanService.startService(context);// 启动服务，接收Alarm或者Boot BroadCast
                    }
                }
            }
        }
    }
}

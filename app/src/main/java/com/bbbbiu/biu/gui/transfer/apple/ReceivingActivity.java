package com.bbbbiu.biu.gui.transfer.apple;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.bbbbiu.biu.gui.transfer.FileItem;
import com.bbbbiu.biu.gui.transfer.TransferBaseActivity;
import com.bbbbiu.biu.service.HttpdService;

import java.util.ArrayList;


public class ReceivingActivity extends TransferBaseActivity {
    private static final String TAG = ReceivingActivity.class.getSimpleName();


    private static boolean created;

    public static void startReceiving(Context context, ArrayList<FileItem> fileItems) {
        if (!created) {
            Intent intent = new Intent(context, ReceivingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putParcelableArrayListExtra(EXTRA_FILE_ITEM, fileItems);
            ((Activity) context).startActivityForResult(intent, 0);
        } else {
            Intent intent = new Intent(ACTION_ADD_TASK);
            intent.putExtra(EXTRA_FILE_ITEM, fileItems);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        created = true;

        addTask(getIntent());
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        created = false;

        super.onDestroy();
    }


    @Override
    protected void onAddNewTask(ArrayList<FileItem> fileItems) {

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        addTask(intent);
    }


    @Override
    protected void onTransferCanceled() {
        onTransferFinished();
    }

    @Override
    protected void onTransferFinished() {
        HttpdService.stopService(this);
    }
}

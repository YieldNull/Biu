package com.bbbbiu.biu.gui.transfer;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


import java.util.ArrayList;

public class ReceiveActivity extends TransferBaseActivity {
    private static final String TAG = ReceiveActivity.class.getSimpleName();

    public static final String EXTRA_FILE_ITEM = "com.bbbbiu.biu.gui.transfer.ReceiveActivity.extra.FILE_ITEM";
    public static final String ACTION_ADD_TASK = "com.bbbbiu.biu.gui.transfer.ReceiveActivity.action.ADD_ITEM";


    public static void startTask(Context context, ArrayList<FileItem> fileItems) {
        Intent intent = new Intent(DownloadActivity.ACTION_ADD_TASK);
        intent.putParcelableArrayListExtra(DownloadActivity.EXTRA_FILE_ITEM, fileItems);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    protected void onAddTaskItem(ArrayList<FileItem> fileItems) {
        for (FileItem item : fileItems) {
            Log.i(TAG, item.uri);
        }
    }
}

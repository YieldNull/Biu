package com.bbbbiu.biu.gui.transfer;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.bbbbiu.biu.service.DownloadService;

import java.util.ArrayList;


public class DownloadActivity extends TransferBaseActivity {
    private static final String TAG = DownloadActivity.class.getSimpleName();


    public static void startTask(Context context, ArrayList<FileItem> fileItems) {
        Intent intent = new Intent(context, DownloadActivity.class);
        intent.putParcelableArrayListExtra(EXTRA_FILE_ITEM, fileItems);
        context.startActivity(intent);
    }

    public static void addTask(Context context, ArrayList<FileItem> fileItems) {
        Intent intent = new Intent(DownloadActivity.ACTION_ADD_TASK);
        intent.putParcelableArrayListExtra(DownloadActivity.EXTRA_FILE_ITEM, fileItems);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }


    @Override
    protected void onAddTaskItem(ArrayList<FileItem> fileItems) {

        for (FileItem item : fileItems) {
            DownloadService.addTask(this, item.uri, item.name, item.size, mProgressReceiver);
        }
    }

    @Override
    protected void onDestroy() {
        DownloadService.stopService(this);

        super.onDestroy();
    }
}

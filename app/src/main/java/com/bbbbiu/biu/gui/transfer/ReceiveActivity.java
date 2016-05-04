package com.bbbbiu.biu.gui.transfer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


import java.util.ArrayList;

public class ReceiveActivity extends TransferBaseActivity {
    private static final String TAG = ReceiveActivity.class.getSimpleName();

    public static void startTask(Context context, ArrayList<FileItem> fileItems) {
        Intent intent = new Intent(context, ReceiveActivity.class);
        intent.putParcelableArrayListExtra(EXTRA_FILE_ITEM, fileItems);

        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addTaskItem(getIntent());
    }

    @Override
    protected void onAddTaskItem(ArrayList<FileItem> fileItems) {
        for (FileItem item : fileItems) {
            Log.i(TAG, item.uri);
        }
    }
}

package com.bbbbiu.biu.gui.transfer.apple;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;

import com.bbbbiu.biu.gui.transfer.FileItem;
import com.bbbbiu.biu.gui.transfer.TransferBaseActivity;

import java.util.ArrayList;


public class ReceivingActivity extends TransferBaseActivity {
    private static final String TAG = ReceivingActivity.class.getSimpleName();

    public static void startReceiving(Context context, ArrayList<FileItem> fileItems) {
        Intent intent = new Intent(context, ReceivingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putParcelableArrayListExtra(EXTRA_FILE_ITEM, fileItems);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
    }

    @Override
    protected void onAddTaskItem(ArrayList<FileItem> fileItems) {

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        addTaskItem(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addTaskItem(getIntent());
    }
}

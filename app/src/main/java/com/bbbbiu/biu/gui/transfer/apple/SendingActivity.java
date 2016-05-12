package com.bbbbiu.biu.gui.transfer.apple;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;

import com.bbbbiu.biu.gui.transfer.FileItem;
import com.bbbbiu.biu.gui.transfer.TransferBaseActivity;

import java.util.ArrayList;

public class SendingActivity extends TransferBaseActivity {

    public static void startSending(Context context) {
        Intent intent = new Intent(context, SendingActivity.class);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);

    }

    protected void onAddTaskItem(ArrayList<FileItem> fileItems) {

    }
}

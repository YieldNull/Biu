package com.bbbbiu.biu.gui.transfer.computer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.bbbbiu.biu.gui.transfer.FileItem;
import com.bbbbiu.biu.gui.transfer.TransferBaseActivity;
import com.bbbbiu.biu.lib.util.HttpConstants;
import com.bbbbiu.biu.service.UploadService;

import java.util.ArrayList;

public class UploadActivity extends TransferBaseActivity {
    private static final String EXTRA_UID = "com.bbbbiu.biu.gui.transfer.computer.UploadActivity.extra.UID";
    private String mUID;


    public static void startUpload(Context context, String uploadUrl, ArrayList<FileItem> fileItems) {
        Intent intent = new Intent(context, UploadActivity.class);
        intent.putExtra(EXTRA_UID, uploadUrl);
        intent.putExtra(EXTRA_FILE_ITEM, fileItems);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUID = getIntent().getStringExtra(EXTRA_UID);

        addTask(getIntent());
    }

    @Override
    protected void onCancelTransfer() {
        UploadService.stopUpload(this);
    }

    @Override
    protected void onAddNewTask(ArrayList<FileItem> fileItems) {
        for (FileItem item : fileItems) {
            UploadService.startUpload(
                    this,
                    HttpConstants.Computer.getUploadUrl(mUID),
                    item,
                    HttpConstants.Computer.getUploadFormData(mUID),
                    mProgressResultReceiver);
        }
    }

}

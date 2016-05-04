package com.bbbbiu.biu.gui.transfer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.bbbbiu.biu.service.UploadService;

import java.util.ArrayList;


public class UploadActivity extends TransferBaseActivity {
    private static final String TAG = UploadActivity.class.getSimpleName();

    private static final String EXTRA_UPLOAD_URL = "com.bbbbiu.biu.gui.transfer.UploadActivity.extra.UPLOAD_URL";

    private String mUploadUrl;


    public static void startUpload(Context context, String uploadUrl, ArrayList<FileItem> fileItems) {
        Intent intent = new Intent(context, UploadActivity.class);
        intent.putExtra(EXTRA_UPLOAD_URL, uploadUrl);
        intent.putExtra(EXTRA_FILE_ITEM, fileItems);

        context.startActivity(intent);
    }


    @Override
    protected void onAddTaskItem(ArrayList<FileItem> fileItems) {
        for (FileItem item : fileItems) {
            UploadService.startUpload(this, mUploadUrl, item.uri, null, mProgressReceiver);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        mUploadUrl = getIntent().getStringExtra(EXTRA_UPLOAD_URL);


        super.onCreate(savedInstanceState);
    }

}
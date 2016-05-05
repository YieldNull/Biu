package com.bbbbiu.biu.gui.transfer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.bbbbiu.biu.service.UploadService;

import java.util.ArrayList;


/**
 * 上传、发送文件
 */
public class UploadActivity extends TransferBaseActivity {
    private static final String TAG = UploadActivity.class.getSimpleName();

    /**
     * Intent extra. 上传URL
     */
    private static final String EXTRA_UPLOAD_URL = "com.bbbbiu.biu.gui.transfer.UploadActivity.extra.UPLOAD_URL";

    private String mUploadUrl;


    public static void startUpload(Context context, String uploadUrl, ArrayList<FileItem> fileItems) {
        Intent intent = new Intent(context, UploadActivity.class);
        intent.putExtra(EXTRA_UPLOAD_URL, uploadUrl);
        intent.putExtra(EXTRA_FILE_ITEM, fileItems);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUploadUrl = getIntent().getStringExtra(EXTRA_UPLOAD_URL);
        addTaskItem(getIntent());
    }

    @Override
    protected void onAddTaskItem(ArrayList<FileItem> fileItems) {
        for (FileItem item : fileItems) {
            UploadService.startUpload(this, mUploadUrl, item.uri, null, mProgressReceiver);
        }
    }

}

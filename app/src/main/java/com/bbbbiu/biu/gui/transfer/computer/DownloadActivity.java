package com.bbbbiu.biu.gui.transfer.computer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.widget.Toast;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.transfer.FileItem;
import com.bbbbiu.biu.gui.transfer.TransferBaseActivity;
import com.bbbbiu.biu.lib.HttpConstants;
import com.bbbbiu.biu.service.DownloadService;
import com.bbbbiu.biu.service.PollingService;

import java.util.ArrayList;


/**
 * 通过轮训服务，下载文件，若轮询失败，则报错。
 */
public class DownloadActivity extends TransferBaseActivity {

    private static final String EXTRA_UID = "com.bbbbiu.biu.gui.transfer.computer.DownloadActivity.extra.UID";

    private String mUid;

    public static void startDownload(Context context, String uid) {
        Intent intent = new Intent(context, DownloadActivity.class);
        intent.putExtra(EXTRA_UID, uid);
        context.startActivity(intent);
    }

    private ResultReceiver mPollingResultReceiver = new ResultReceiver(new Handler()) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if (resultCode == PollingService.RESULT_OK) {
                ArrayList<FileItem> fileItems = resultData.getParcelableArrayList(EXTRA_FILE_ITEM);

                if (fileItems != null) {
                    for (FileItem item : fileItems) {
                        item.setUri(HttpConstants.Computer.getDownloadUrl(mUid, item.uri));
                    }
                    addTask(fileItems);
                }
            } else {
                // 轮训失败，多次server  error
                Toast.makeText(DownloadActivity.this, R.string.hint_connect_server_error, Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showConnectingAnim();

        mUid = getIntent().getStringExtra(EXTRA_UID);
        PollingService.startPolling(this, mUid, mPollingResultReceiver);

        updateLoadingText(getString(R.string.hint_connect_waiting_upload));
    }

    @Override
    protected void onDestroy() {
        PollingService.stopPolling(this);

        super.onDestroy();
    }


    @Override
    protected void onAddNewTask(ArrayList<FileItem> fileItems) {
        for (FileItem item : fileItems) {
            DownloadService.addTask(this, item, mProgressResultReceiver);
        }
    }

    @Override
    protected void onTransferCanceled() {
        onTransferFinished();
    }

    @Override
    protected void onTransferFinished() {
        DownloadService.stopService(this);
    }
}

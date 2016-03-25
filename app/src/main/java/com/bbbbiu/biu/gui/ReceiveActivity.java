package com.bbbbiu.biu.gui;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.http.client.FileItem;
import com.bbbbiu.biu.gui.adapters.ReceiveAdapter;
import com.bbbbiu.biu.service.DownloadService;
import com.bbbbiu.biu.service.PollingService;
import com.bbbbiu.biu.util.StorageUtil;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;


public class ReceiveActivity extends AppCompatActivity {
    private static final String TAG = ReceiveActivity.class.getSimpleName();

    public static final String EXTRA_UID = "com.bbbbiu.biu.gui.ReceiveActivity.extra.UID";
    public static final String EXTRA_FILE_LIST = "com.bbbbiu.biu.gui.ReceiveActivity.extra.FILE_LIST";

    public static final String EXTRA_FILE_ITEM = "com.bbbbiu.biu.gui.ReceiveActivity.extra.FILE_ITEM";
    public static final String EXTRA_PROGRESS = "com.bbbbiu.biu.gui.ReceiveActivity.extra.PROGRESS";

    @Bind(R.id.recyclerView_receive)
    RecyclerView mRecyclerView;

    ReceiveAdapter mAdapter;

    @SuppressLint("ParcelCreator")
    private class PollingResultReceiver extends ResultReceiver {

        public PollingResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if (resultCode == PollingService.RESULT_OK) {
                ArrayList<FileItem> fileItems = resultData.getParcelableArrayList(EXTRA_FILE_LIST);
                Log.i(TAG, "Received file list. Count " + fileItems.size());

                mAdapter.addFileList(fileItems);
                mAdapter.notifyDataSetChanged();

                Log.i(TAG, "Starting download file one by one");
                for (FileItem fileItem : fileItems) {
                    DownloadService.startDownload(ReceiveActivity.this, fileItem);
                }
            } else {
                Toast.makeText(ReceiveActivity.this, R.string.net_server_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("ParcelCreator")
    private class ProgressResultReceiver extends ResultReceiver {

        public ProgressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            switch (resultCode) {
                case DownloadService.RESULT_FAILED:
                    break;
                case DownloadService.RESULT_SUCCESS:
                    break;
                case DownloadService.RESULT_PROGRESS:
                    FileItem fileItem = resultData.getParcelable(EXTRA_FILE_ITEM);
                    int progress = resultData.getInt(EXTRA_PROGRESS);


                    int position = mAdapter.getItemPosition(fileItem);
                    ReceiveAdapter.FileItemViewHolder holder = (ReceiveAdapter.FileItemViewHolder)
                            mRecyclerView.findViewHolderForAdapterPosition(position);

                    holder.getProgressBar().setProgress(progress);

                    String read = StorageUtil.getReadableSize((long) (fileItem.getSize() * progress * 0.01));
                    String all = StorageUtil.getReadableSize(fileItem.getSize());

                    holder.setProgressText(String.format("%s/%s", read, all));
                    break;
            }
        }
    }


    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // android 4.4 状态栏透明
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(getResources().getColor(R.color.colorPrimary));
        }

        String uid = getIntent().getExtras().getString(EXTRA_UID);

        mAdapter = new ReceiveAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        ResultReceiver pollingResultReceiver = new PollingResultReceiver(new Handler());
        Log.i(TAG, "onCreate. Starting polling service");
        PollingService.startPolling(this, uid, pollingResultReceiver);

        ResultReceiver progressResultReceiver = new ProgressResultReceiver(new Handler());
        DownloadService.setResultReceiver(this, progressResultReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.i(TAG, "onDestroy. Stopping polling service");
        PollingService.stopPolling(this);
    }
}

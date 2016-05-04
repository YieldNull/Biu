package com.bbbbiu.biu.gui.transfer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.WindowManager;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapters.TransferAdapter;
import com.bbbbiu.biu.lib.util.ProgressListenerImpl;
import com.bbbbiu.biu.util.StorageUtil;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * 上传、下载、接收文件的界面基类
 * <p/>
 * Created by YieldNull at 4/26/16
 */
public abstract class TransferBaseActivity extends AppCompatActivity {
    private static final String TAG = TransferBaseActivity.class.getSimpleName();

    public static final String EXTRA_FILE_ITEM = "com.bbbbiu.biu.gui.transfer.TransferBaseActivity.extra.FILE_ITEM";
    public static final String ACTION_ADD_TASK = "com.bbbbiu.biu.gui.transfer.TransferBaseActivity.action.ADD_TASK";


    @Bind(R.id.recyclerView)
    protected RecyclerView mRecyclerView;

    @Bind(R.id.textView_transfer_speed)
    protected TextView mTransferSpeedText;

    @Bind(R.id.textView_transfer_total)
    protected TextView mTransferTotalText;

    private int mPreviousProgress;
    private long mPreviousTime = System.currentTimeMillis();
    private long mTransferTotal;
    private long mCurrentTaskSize;

    protected TransferAdapter mTransferAdapter;


    protected abstract void onAddTaskItem(ArrayList<FileItem> fileItems);

    protected BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction().equals(ACTION_ADD_TASK)) {
                addTaskItem(intent);
            }
        }
    };


    protected ResultReceiver mProgressReceiver = new ResultReceiver(new Handler()) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            String fileUri = resultData.getString(ProgressListenerImpl.RESULT_EXTRA_FILE_URI);

            switch (resultCode) {
                case ProgressListenerImpl.RESULT_FAILED:
                    mTransferAdapter.setTaskFailed(fileUri);
                    break;
                case ProgressListenerImpl.RESULT_SUCCEEDED:
                    mTransferAdapter.setTaskFinished(fileUri);
                    mPreviousTime = System.currentTimeMillis();
                    mPreviousProgress = 0;
                    mCurrentTaskSize = 0;

                    break;
                case ProgressListenerImpl.RESULT_PROGRESS:
                    int progress = resultData.getInt(ProgressListenerImpl.RESULT_EXTRA_PROGRESS);
                    mTransferAdapter.updateProgress(mRecyclerView, fileUri, progress);

                    if (mCurrentTaskSize == 0) {
                        mCurrentTaskSize = mTransferAdapter.getItem(fileUri).size;
                    }

                    // 计算传输总量
                    long periodBytes = mCurrentTaskSize * (progress - mPreviousProgress) / 100;

                    mTransferTotal += periodBytes;
                    mTransferTotalText.setText(StorageUtil.getReadableSize(mTransferTotal));

                    // 计算网速
                    long subTime = System.currentTimeMillis() - mPreviousTime;

                    if (subTime != 0) {
                        long speed = periodBytes / subTime * 1000;
                        mTransferSpeedText.setText(StorageUtil.getReadableSize(speed) + "/s");
                    }

                    mPreviousProgress = progress;
                    mPreviousTime = System.currentTimeMillis();

                    break;
                default:
                    break;
            }
        }
    };

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_base);
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

        mTransferAdapter = new TransferAdapter(this);
        mRecyclerView.setAdapter(mTransferAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(this).build());

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(ACTION_ADD_TASK));
    }


    protected void addTaskItem(Intent intent) {
        if (intent != null) {
            ArrayList<FileItem> fileItems = intent.getParcelableArrayListExtra(EXTRA_FILE_ITEM);
            if (fileItems != null) {
                mTransferAdapter.addItem(fileItems);
                onAddTaskItem(fileItems);
            }
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }
}

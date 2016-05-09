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
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapter.TransferAdapter;
import com.bbbbiu.biu.lib.util.ProgressListenerImpl;
import com.bbbbiu.biu.util.StorageUtil;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.wang.avi.AVLoadingIndicatorView;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * 上传、下载、接收文件的界面基类
 * <p>
 * Created by YieldNull at 4/26/16
 */
public abstract class TransferBaseActivity extends AppCompatActivity {
    private static final String TAG = TransferBaseActivity.class.getSimpleName();

    public static final String EXTRA_FILE_ITEM = "com.bbbbiu.biu.gui.transfer.TransferBaseActivity.extra.FILE_ITEM";

    public static final String EXTRA_RESULT_CODE = "com.bbbbiu.biu.gui.transfer.TransferBaseActivity.extra.RESULT_CODE";
    public static final String EXTRA_RESULT_BUNDLE = "com.bbbbiu.biu.gui.transfer.TransferBaseActivity.extra.RESULT_BUNDLE";

    public static final String ACTION_ADD_TASK = "com.bbbbiu.biu.gui.transfer.TransferBaseActivity.action.ADD_TASK";
    public static final String ACTION_UPDATE_PROGRESS = "com.bbbbiu.biu.gui.transfer.TransferBaseActivity.action.UPDATE_PROGRESS";


    @Bind(R.id.recyclerView)
    protected RecyclerView mRecyclerView;

    @Bind(R.id.textView_transfer_speed)
    protected TextView mTransferSpeedText;

    @Bind(R.id.textView_transfer_total)
    protected TextView mTransferTotalText;

    @Bind(R.id.loadingIndicatorView)
    protected AVLoadingIndicatorView mLoadingIndicatorView;

    @Bind(R.id.linearLayout_measure)
    protected LinearLayout mMeasureLinearLayout;

    private int mPreviousProgress;
    private long mPreviousTime = System.currentTimeMillis();
    private long mTransferTotal;
    private long mCurrentTaskSize;

    protected TransferAdapter mTransferAdapter;


    protected abstract void onAddTaskItem(ArrayList<FileItem> fileItems);

    protected BroadcastReceiver mTaskBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction().equals(ACTION_ADD_TASK)) {
                Log.i(TAG, "Received adding task message");
                addTaskItem(intent);
            }
        }
    };

    protected BroadcastReceiver mProgressBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction().equals(ACTION_UPDATE_PROGRESS)) {
                updateProgress(intent.getIntExtra(EXTRA_RESULT_CODE, -1), intent.getBundleExtra(EXTRA_RESULT_BUNDLE));
            }
        }
    };

    protected ResultReceiver mProgressResultReceiver = new ResultReceiver(new Handler()) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            updateProgress(resultCode, resultData);
        }
    };

    private void updateProgress(int resultCode, Bundle resultData) {
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

                long periodTime = System.currentTimeMillis() - mPreviousTime;
                long periodBytes = mCurrentTaskSize * (progress - mPreviousProgress) / 100;


                // TODO 总传输时间小于500ms
                if (periodTime > 500) { // 每隔一段时间更新一下网速、下载总量
                    // 计算传输总量
                    mTransferTotal += periodBytes;
                    mTransferTotalText.setText(StorageUtil.getReadableSize(mTransferTotal));

                    // 计算网速
                    long speed = periodBytes / periodTime * 1000;
                    mTransferSpeedText.setText(StorageUtil.getReadableSize(speed) + "/s");

                    mPreviousProgress = progress;
                    mPreviousTime = System.currentTimeMillis();
                }
                break;
            default:
                break;
        }
    }

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

        LocalBroadcastManager.getInstance(this).registerReceiver(mTaskBroadcastReceiver, new IntentFilter(ACTION_ADD_TASK));
        LocalBroadcastManager.getInstance(this).registerReceiver(mProgressBroadcastReceiver,new IntentFilter(ACTION_UPDATE_PROGRESS));

        mLoadingIndicatorView.setVisibility(View.GONE);
    }

    protected void onConnecting() {
        mLoadingIndicatorView.setVisibility(View.VISIBLE);
        mMeasureLinearLayout.setVisibility(View.GONE);
        setTitle(getString(R.string.transfer_connecting));
    }

    protected void onConnected() {
        mLoadingIndicatorView.setVisibility(View.GONE);
        mMeasureLinearLayout.setVisibility(View.VISIBLE);
        setTitle(getString(R.string.transfer_working));
    }

    protected void addTaskItem(Intent intent) {
        if (intent != null) {
            ArrayList<FileItem> fileItems = intent.getParcelableArrayListExtra(EXTRA_FILE_ITEM);
            addTaskItem(fileItems);
        }

    }

    protected void addTaskItem(ArrayList<FileItem> fileItems) {
        Log.i(TAG, "Adding task...");
        if (fileItems != null) {
            mTransferAdapter.addItem(fileItems);
            onAddTaskItem(fileItems);
            onConnected();
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mTaskBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mProgressBroadcastReceiver);

        super.onDestroy();
    }
}

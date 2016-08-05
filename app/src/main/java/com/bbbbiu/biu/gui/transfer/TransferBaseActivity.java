package com.bbbbiu.biu.gui.transfer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapter.TransferAdapter;
import com.bbbbiu.biu.lib.ProgressListenerImpl;
import com.bbbbiu.biu.util.StorageUtil;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * 文件传输的界面基类。显示上传，下载均可。
 * <p/>
 * 使用{@link ResultReceiver} 传递成功或失败信息，使用{@link ProgressListenerImpl}传递进度信息。
 * <p/>
 * 使用{@link LocalBroadcastManager} 发送广播，来提交新的任务；以及更新进度信息（在{@link ProgressListenerImpl}）使用
 * <p/>
 * Created by YieldNull at 4/26/16
 */
public abstract class TransferBaseActivity extends AppCompatActivity {
    private static final String TAG = TransferBaseActivity.class.getSimpleName();

    /**
     * intent action:从Service接收进度
     */
    public static final String ACTION_UPDATE_PROGRESS = "com.bbbbiu.biu.gui.transfer.TransferBaseActivity.action.UPDATE_PROGRESS";

    /**
     * intent extra: 从Service接收进度，
     *
     * @see ProgressListenerImpl#RESULT_FAILED
     * @see ProgressListenerImpl#RESULT_PROGRESS
     * @see ProgressListenerImpl#RESULT_SUCCEEDED
     */
    public static final String EXTRA_RESULT_CODE = "com.bbbbiu.biu.gui.transfer.TransferBaseActivity.extra.RESULT_CODE";


    /**
     * Intent extra:从Service接收进度，内含文件uri,当前进度
     *
     * @see ProgressListenerImpl#RESULT_EXTRA_FILE_URI
     * @see ProgressListenerImpl#RESULT_EXTRA_PROGRESS
     */
    public static final String EXTRA_RESULT_BUNDLE = "com.bbbbiu.biu.gui.transfer.TransferBaseActivity.extra.RESULT_BUNDLE";


    /**
     * intent action，增加新的传输任务
     */
    public static final String ACTION_ADD_TASK = "com.bbbbiu.biu.gui.transfer.TransferBaseActivity.action.ADD_TASK";


    /**
     * Intent extra， 新的传输任务对应的{@link FileItem}
     */
    public static final String EXTRA_FILE_ITEM = "com.bbbbiu.biu.gui.transfer.TransferBaseActivity.extra.FILE_ITEM";


    @Bind(R.id.recyclerView)
    protected RecyclerView mRecyclerView;

    @Bind(R.id.linearLayout_loading)
    protected LinearLayout mLoadingLinearLayout;

    @Bind(R.id.textView_loading)
    protected TextView mLoadingTextView;

    @Bind(R.id.linearLayout_measure)
    protected LinearLayout mMeasureLinearLayout;

    @Bind(R.id.textView_transfer_speed)
    protected TextView mTransferSpeedText;

    @Bind(R.id.textView_transfer_total)
    protected TextView mTransferTotalText;


    protected TransferAdapter mTransferAdapter;


    /*******************************************************
     * ******************测量网速与传输总量*******************
     *******************************************************/

    /**
     * 传输总量
     */
    private long mTransferTotal;

    /**
     * 当前任务总量
     */
    private long mCurrentTaskSize;

    /**
     * 上一次更新前，当前任务的进度
     */
    private int mPreviousProgress;

    //******************************************************//

    /**
     * 上一次更新的时间
     */
    private long mPreviousTime = System.currentTimeMillis();


    /**
     * 接收任务广播
     */
    protected BroadcastReceiver mTaskBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction().equals(ACTION_ADD_TASK)) {
                Log.i(TAG, "Received adding task message");
                addTask(intent);
            }
        }
    };

    /**
     * 接收进度广播
     */
    protected BroadcastReceiver mProgressBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction().equals(ACTION_UPDATE_PROGRESS)) {
                updateProgress(intent.getIntExtra(EXTRA_RESULT_CODE, -1), intent.getBundleExtra(EXTRA_RESULT_BUNDLE));
            }
        }
    };

    /**
     * 接收进度反馈，从Service接收进度时使用
     */
    protected ResultReceiver mProgressResultReceiver = new ResultReceiver(new Handler()) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            updateProgress(resultCode, resultData);
        }
    };

    /**
     * 更新当前进度
     *
     * @param resultCode 失败、成功、或者是新进度
     *                   {@link ProgressListenerImpl#RESULT_FAILED}，
     *                   {@link ProgressListenerImpl#RESULT_SUCCEEDED}，
     *                   {@link ProgressListenerImpl#RESULT_PROGRESS}
     * @param resultData a{@link Bundle},包含以下 EXTRA：
     *                   {@link ProgressListenerImpl#RESULT_EXTRA_FILE_URI}，
     *                   {@link ProgressListenerImpl#RESULT_EXTRA_PROGRESS}，
     */
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
                    FileItem item = mTransferAdapter.getItem(fileUri);

                    if (item != null) {
                        mCurrentTaskSize = item.size;
                    } else {
                        break;
                    }
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

        mLoadingLinearLayout.setVisibility(View.GONE);

        LocalBroadcastManager.getInstance(this).registerReceiver(mTaskBroadcastReceiver, new IntentFilter(ACTION_ADD_TASK));
        LocalBroadcastManager.getInstance(this).registerReceiver(mProgressBroadcastReceiver, new IntentFilter(ACTION_UPDATE_PROGRESS));

    }

    /**
     * 显示正在连接动画
     */
    protected void showConnectingAnim() {
        mLoadingLinearLayout.setVisibility(View.VISIBLE);
        mMeasureLinearLayout.setVisibility(View.GONE);
        setTitle(getString(R.string.title_transfer_connecting));
    }

    /**
     * 关闭正在连接动画
     */
    protected void closeConnectionAnim() {
        mLoadingLinearLayout.setVisibility(View.GONE);
        mMeasureLinearLayout.setVisibility(View.VISIBLE);
        setTitle(getString(R.string.title_transfer_working));
    }

    /**
     * 分配新任务，从Intent中获取任务对象
     *
     * @param intent intent
     */
    protected void addTask(Intent intent) {
        if (intent != null) {
            ArrayList<FileItem> fileItems = intent.getParcelableArrayListExtra(EXTRA_FILE_ITEM);
            addTask(fileItems);
        }
    }

    /**
     * 分配新任务
     *
     * @param fileItems 任务对象
     */
    protected void addTask(ArrayList<FileItem> fileItems) {
        Log.i(TAG, "Adding task...");
        if (fileItems != null) {
            mTransferAdapter.addItem(fileItems); // 显示在界面上
            onAddNewTask(fileItems);  // 基类处理任务
            closeConnectionAnim();
        }
    }

    /**
     * 更新加载提示
     *
     * @param hint 提示文字
     */
    protected void updateLoadingText(String hint) {
        mLoadingTextView.setText(hint);
    }


    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mTaskBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mProgressBroadcastReceiver);

        super.onDestroy();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!mTransferAdapter.finished()) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.hint_transfer_abort_confirm))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            onTransferCanceled();
                            TransferBaseActivity.super.onBackPressed();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .show();
        } else {
            onTransferCanceled();
            super.onBackPressed();
        }
    }

    /**
     * 接收到新的任务，对其进行处理。
     * <p/>
     * 基类可以进行上传、下载，然后进度消息会通过广播传递过来，并显示
     *
     * @param fileItems 任务对象
     */
    protected abstract void onAddNewTask(ArrayList<FileItem> fileItems);


    /**
     * 中途取消任务
     */
    protected abstract void onTransferCanceled();


    /**
     * 任务完成
     */
    protected abstract void onTransferFinished();
}

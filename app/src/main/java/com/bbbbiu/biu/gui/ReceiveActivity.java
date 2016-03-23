package com.bbbbiu.biu.gui;

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.WindowManager;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.client.HttpManager;
import com.bbbbiu.biu.gui.adapters.FileStreamAdapter;
import com.bbbbiu.biu.util.StorageUtil;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;


public class ReceiveActivity extends AppCompatActivity {
    private static final String TAG = ReceiveActivity.class.getSimpleName();

    public static final String INTENT_EXTRA_UID = "com.bbbbiu.biu.gui.ReceiveActivity.INTENT_EXTRA_UID";
    private static final int MSG_GOT_FILE_LIST = 0;
    private static final int MSG_SHOW_FILE_DOWNLOADED = 1;


    private String mUid;

    private Handler mHandler = new HandlerClass(this);


    private List<HttpManager.FileItem> mFileList;

    private RecyclerView mRecyclerView;
    private FileStreamAdapter mAdapter;

    public void setFileList(List<HttpManager.FileItem> mFileList) {
        this.mFileList = mFileList;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);

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

        mUid = getIntent().getExtras().getString(INTENT_EXTRA_UID);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView_receive_computer);

        mAdapter = new FileStreamAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        new Thread(new Runnable() {
            @Override
            public void run() {
                getFileList();
            }
        }).start();

    }

    private void getFileList() {
        List<HttpManager.FileItem> fileList = HttpManager.getFileList(mUid);
        if (fileList != null) {
            Message msg = new Message();
            msg.setTarget(mHandler);

            msg.what = MSG_GOT_FILE_LIST;
            msg.obj = fileList;
            msg.sendToTarget();
        } else {
            Log.i(TAG, "User has not send files to server");
            Log.i(TAG, "Retry waiting");

            getFileList(); // TODO 怎么避免无限递归？
        }
    }

    private void downloadFile() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                File repository = StorageUtil.getDownloadDir(ReceiveActivity.this);
                for (HttpManager.FileItem fileItem : mFileList) {
                    HttpManager.downloadFile(fileItem.url, mUid, new File(repository, fileItem.name));
                }
                Log.i(TAG, "Finish download");
            }
        }).start();
    }

    private void showFileList() {
        mAdapter.setFileList(mFileList);
        mAdapter.notifyDataSetChanged();

    }

    private static class HandlerClass extends Handler {
        private final WeakReference<ReceiveActivity> mTarget;

        public HandlerClass(ReceiveActivity context) {
            mTarget = new WeakReference<>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_GOT_FILE_LIST:
                    mTarget.get().setFileList((List<HttpManager.FileItem>) msg.obj);
                    mTarget.get().showFileList();
                    mTarget.get().downloadFile();
                    break;
            }
        }
    }
}

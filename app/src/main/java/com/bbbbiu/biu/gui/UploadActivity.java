package com.bbbbiu.biu.gui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.WindowManager;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapters.UploadAdapter;
import com.bbbbiu.biu.service.UploadService;
import com.bbbbiu.biu.util.PreferenceUtil;
import com.bbbbiu.biu.util.StorageUtil;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;

public class UploadActivity extends AppCompatActivity {
    private static final String TAG = UploadActivity.class.getSimpleName();

    public static final String EXTRA_FILE_PATH = "com.bbbbiu.biu.gui.UploadActivity.extra.FILE_PATH";
    public static final String EXTRA_PROGRESS = "com.bbbbiu.biu.gui.UploadActivity.extra.PROGRESS";

    private static final String EXTRA_UID = "com.bbbbiu.biu.gui.UploadActivity.extra.UID";

    private String uid;
    private Set<String> filePaths;

    @Bind(R.id.recyclerView)
    RecyclerView mRecyclerView;
    UploadAdapter mAdapter;


    public static void startUpload(Context context, String uid) {
        Intent intent = new Intent(context, UploadActivity.class);
        intent.putExtra(EXTRA_UID, uid);
        context.startActivity(intent);
    }


    @SuppressLint("ParcelCreator")
    private class ProgressResultReceiver extends ResultReceiver {

        public ProgressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            switch (resultCode) {
                case UploadService.RESULT_FAILED:
                    break;
                case UploadService.RESULT_SUCCESS:
                    break;
                case UploadService.RESULT_PROGRESS:
                    String filePath = resultData.getString(EXTRA_FILE_PATH);
                    int progress = resultData.getInt(EXTRA_PROGRESS);

                    int position = mAdapter.getItemPosition(filePath);
                    long length = mAdapter.getItemAt(position).length();

                    UploadAdapter.FileItemViewHolder holder = (UploadAdapter.FileItemViewHolder)
                            mRecyclerView.findViewHolderForAdapterPosition(position);

                    if (holder != null) {
                        holder.getProgressBar().setProgress(progress);
                        String read = StorageUtil.getReadableSize((long) (length * progress * 0.01));
                        String all = StorageUtil.getReadableSize(length);

                        holder.setProgressText(String.format("%s/%s", read, all));
                    }
                    break;
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        ButterKnife.bind(this);

        // android 4.4 状态栏透明
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(getResources().getColor(R.color.colorPrimary));
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        uid = getIntent().getStringExtra(EXTRA_UID);
        filePaths = PreferenceUtil.getFilesToSend(this);


        mAdapter = new UploadAdapter(this);
        mAdapter.addFiles(filePaths);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        ResultReceiver resultReceiver = new ProgressResultReceiver(new Handler());
        UploadService.setReceiver(this, resultReceiver);


        Log.i(TAG, "Start upload files. File Amount " + filePaths.size());

        for (String path : filePaths) {
            UploadService.startUpload(this, uid, path);
        }
    }
}

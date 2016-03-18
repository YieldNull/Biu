package com.bbbbiu.biu.gui;

import android.os.Build;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapters.FileListAdapter;
import com.bbbbiu.biu.gui.adapters.FileOptionAdapter;
import com.bbbbiu.biu.gui.fragments.OnFileChoosingListener;
import com.bbbbiu.biu.gui.fragments.OnFileOptionClickListener;
import com.github.clans.fab.FloatingActionMenu;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.io.File;
import java.util.HashSet;

public class FileChooseActivity extends AppCompatActivity implements
        OnFileChoosingListener, OnFileOptionClickListener {

    private static final String TAG = FileChooseActivity.class.getSimpleName();

    private FileListAdapter mArrayAdapter;
    private RecyclerView mRecyclerView;

    private SlidingUpPanelLayout mSlidingUpPanelLayout;

    private boolean onChoosing;

    private RecyclerView mFileOptionRecyclerView;
    private FileOptionAdapter mFileOptionAdapter;

    private HashSet<File> mChosenFiles = new HashSet<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_choose);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_file_choosing);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.label_choose_file));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // android 4.4 状态栏透明
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(getResources().getColor(R.color.colorPrimary));
        }


        mSlidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        mSlidingUpPanelLayout.setFadeOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
            }
        });
        mFileOptionRecyclerView = (RecyclerView) findViewById(R.id.recyclerView_file_option);

        File rootDir = Environment.getExternalStorageDirectory();
        if (rootDir == null) {
            rootDir = Environment.getRootDirectory();
        }

        mArrayAdapter = new FileListAdapter(this, rootDir);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView_file);
        mRecyclerView.setAdapter(mArrayAdapter);
        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mRecyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(this).build());

        FloatingActionMenu actionMenu = (FloatingActionMenu) findViewById(R.id.float_action_menu_file);
        actionMenu.setIconAnimated(false);
        actionMenu.setClosedOnTouchOutside(true);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_choose, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finish();
                return true;

            case R.id.action_choose_all:
                onChoosing = true;
                mArrayAdapter.setFileAllChosen();
                break;

            case R.id.action_choosing_dismiss:
                onChoosing = !onChoosing;
                mArrayAdapter.dismissChoosing();
                break;
            default:
                break;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (!mArrayAdapter.quitDir()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onFileChosen(File file) {
        mChosenFiles.add(file);
        getSupportActionBar().setTitle(String.format("已选 %d 项", mChosenFiles.size()));
    }

    @Override
    public void onFileDismissed(File file) {
        mChosenFiles.remove(file);
        getSupportActionBar().setTitle(String.format("已选 %d 项", mChosenFiles.size()));
    }

    @Override
    public void onFileOptionClick(File file) {
        // 创建mFileOptionRecyclerView 以及对应的adapter
        if (mFileOptionAdapter == null) {

            mFileOptionAdapter = new FileOptionAdapter(this, file);

            mFileOptionRecyclerView.setAdapter(mFileOptionAdapter);
            mFileOptionRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            mFileOptionRecyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(this)
                    .paintProvider(mFileOptionAdapter)
                    .visibilityProvider(mFileOptionAdapter)
                    .marginProvider(mFileOptionAdapter)
                    .build());
        } else {
            // 更新文件
            mFileOptionAdapter.setFile(file);
            mFileOptionAdapter.notifyDataSetChanged();
        }

        // 展开底部panel
        mSlidingUpPanelLayout.invalidate();
        mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
    }

}

package com.bbbbiu.biu.gui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapters.FileOptionAdapter;
import com.bbbbiu.biu.gui.adapters.FileChoosePagerAdapter;
import com.bbbbiu.biu.gui.fragments.FileFragment;
import com.bbbbiu.biu.gui.fragments.OnBackPressedListener;
import com.bbbbiu.biu.gui.fragments.OnFileChoosingListener;
import com.bbbbiu.biu.gui.fragments.OnFileOptionClickListener;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.io.File;
import java.util.HashSet;

public class FileChooseActivity extends AppCompatActivity implements
        OnFileChoosingListener, OnFileOptionClickListener {

    private static final String TAG = FileChooseActivity.class.getSimpleName();

    private ViewPager mViewPager;
    private FileChoosePagerAdapter mPagerAdapter;

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

        FloatingActionButton fbtn = (FloatingActionButton) findViewById(R.id.fab_file_select);

        // 使用viewPager切换tab
        mPagerAdapter = new FileChoosePagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.viewpager_file_choose);
        mViewPager.setAdapter(mPagerAdapter);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout_file_choose);
        tabLayout.setupWithViewPager(mViewPager);


        mSlidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        mSlidingUpPanelLayout.setFadeOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
            }
        });

        mFileOptionRecyclerView = (RecyclerView) findViewById(R.id.recyclerView_file_option);
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
                invalidateOptionsMenu();
                break;

            case R.id.action_choosing_dismiss:
                onChoosing = !onChoosing;
                invalidateOptionsMenu();
                break;
            default:
                break;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (!((OnBackPressedListener) getCurrentFragment()).onBackPressed()) {
            super.onBackPressed();
        }
    }
    private Fragment getCurrentFragment(){
        return mPagerAdapter.getItem(mViewPager.getCurrentItem());
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

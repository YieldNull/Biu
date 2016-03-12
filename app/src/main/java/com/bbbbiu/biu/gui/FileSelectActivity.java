package com.bbbbiu.biu.gui;

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
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapters.FileOperationAdapter;
import com.bbbbiu.biu.gui.adapters.FileSelectPagerAdapter;
import com.bbbbiu.biu.gui.fragments.FileFragment;
import com.bbbbiu.biu.gui.fragments.OnBackPressedListener;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.File;

public class FileSelectActivity extends AppCompatActivity implements
        FileFragment.OnFileSelectingListener, FileFragment.OnFileOptionClickListener {

    private static final String TAG = FileSelectActivity.class.getSimpleName();

    private ViewPager mViewPager;
    private FileSelectPagerAdapter mPagerAdapter;

    private SlidingUpPanelLayout mSlidingUpPanelLayout;
    private Menu mToolbarMenu;

    private RecyclerView mRecyclerView;
    private FileOperationAdapter mFileOperationAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_select);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_file_select);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.label_select_file));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // android 4.4 状态栏透明
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(getResources().getColor(R.color.colorPrimary));
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_file_select);

        // 使用viewPager切换tab
        mPagerAdapter = new FileSelectPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.viewpager_file_select);
        mViewPager.setAdapter(mPagerAdapter);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout_file_select);
        tabLayout.setupWithViewPager(mViewPager);

        mSlidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView_sliding_up);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_select_toolbar, menu);
        this.mToolbarMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_select_all) {
            MenuItem menuItem = mToolbarMenu.findItem(R.id.action_select_or_dismiss);
            menuItem.setTitle(getString(R.string.action_select_dismiss));
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = mPagerAdapter.getItem(mViewPager.getCurrentItem());

        if (!((OnBackPressedListener) fragment).onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onFileFirstSelected() {
        MenuItem item = mToolbarMenu.findItem(R.id.action_select_or_dismiss);
        item.setTitle(getString(R.string.action_select_dismiss));
    }

    @Override
    public void onFileAllDismissed() {
        MenuItem item = mToolbarMenu.findItem(R.id.action_select_or_dismiss);
        item.setTitle(getString(R.string.action_select));
    }

    @Override
    public void onFileOptionClick(File file) {
        mFileOperationAdapter = new FileOperationAdapter(this,file);
        mRecyclerView.setAdapter(mFileOperationAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
    }
}

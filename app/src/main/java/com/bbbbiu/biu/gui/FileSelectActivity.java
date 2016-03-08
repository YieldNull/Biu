package com.bbbbiu.biu.gui;

import android.os.Build;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapters.FileSelectPagerAdapter;
import com.bbbbiu.biu.gui.fragments.FileFragment;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

public class FileSelectActivity extends AppCompatActivity implements FileFragment.OnFileOptionClickListener, FileFragment.OnOutsideClickListener {

    private static final String TAG = FileSelectActivity.class.getSimpleName();
    private SlidingUpPanelLayout mSlidingPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_select);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_file_select);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("哈哈哈");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // android 4.4 状态栏透明
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(getResources().getColor(R.color.colorPrimary));

            // 获取status bar的高度 然后让tool bar margin 一下
//            FrameLayout.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) findViewById(R.id.appbar_file_select).getLayoutParams();
//            SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
//            p.setMargins(0, config.getStatusBarHeight(), 0, 0);
        }

//        // bottom slidingUpPanel
//        mSlidingPanel = (SlidingUpPanelLayout) findViewById(R.id.slidingUpLayout_file_select);
//        mSlidingPanel.setPanelHeight(0);
//
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_file_select);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Here's a Snackbar", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // 使用viewPager切换tab
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager_file_select);
        viewPager.setAdapter(new FileSelectPagerAdapter(getSupportFragmentManager(), this));
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout_file_select);
        tabLayout.setupWithViewPager(viewPager);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_select_toolbar, menu);
        return true;
    }

    @Override
    public void onFileOptionClicked() {
        mSlidingPanel.setPanelHeight(500);
    }


    @Override
    public void onOutsideClicked() {
        Log.i(TAG, mSlidingPanel.getPanelState().toString());
        if (mSlidingPanel.getPanelState() != SlidingUpPanelLayout.PanelState.COLLAPSED) {
            mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            Log.i(TAG, "Closing");
        }
    }
}

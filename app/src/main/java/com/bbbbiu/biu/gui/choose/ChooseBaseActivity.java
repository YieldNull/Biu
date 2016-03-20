package com.bbbbiu.biu.gui.choose;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapters.PanelBaseAdapter;
import com.github.clans.fab.FloatingActionMenu;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.io.File;

public abstract class ChooseBaseActivity extends AppCompatActivity implements
        OnChoosingListener, OnFileOptionClickListener {

    private static final String TAG = FileChooseActivity.class.getSimpleName();

    protected RecyclerView mContentRecyclerView;
    protected RecyclerView.Adapter mContentAdapter;
    protected RecyclerView.LayoutManager mContentLayoutManager;

    protected SlidingUpPanelLayout mSlidingUpPanelLayout;
    protected RecyclerView mPanelRecyclerView;
    protected PanelBaseAdapter mPanelAdapter;

    protected FloatingActionMenu mFloatingActionMenu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choosing);

        // Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_choosing);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.title_activity_choose));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // android 4.4 状态栏透明
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(getResources().getColor(R.color.colorPrimary));
        }

        // 主内容 RecyclerView
        mContentRecyclerView = (RecyclerView) findViewById(R.id.recyclerView_choosing);

        mContentLayoutManager = onCreateContentLayoutManager();
        if (!(mContentLayoutManager instanceof LinearContentLayoutManager) &&
                !(mContentLayoutManager instanceof GridContentLayoutManager)) {
            throw new RuntimeException(("You must use custom content LayoutManager"));
        }
        mContentRecyclerView.setLayoutManager(mContentLayoutManager);


        mContentAdapter = onCreateContentAdapter();
        if (mContentAdapter == null) {
            throw new NullPointerException("Content adapter can not be null");
        }
        mContentRecyclerView.setAdapter(mContentAdapter);

        RecyclerView.ItemDecoration itemDecoration = onCreateContentItemDecoration();
        if (itemDecoration != null) {
            mContentRecyclerView.addItemDecoration(itemDecoration);
        }

        // 滑动panel RecyclerView
        mPanelRecyclerView = (RecyclerView) findViewById(R.id.recyclerView_bottom_panel);
        mPanelAdapter = onCreatePanelAdapter();

        if (mPanelAdapter == null) {
            throw new NullPointerException("Panel adapter can not be null");
        }
        mPanelRecyclerView.setAdapter(mPanelAdapter);
        mPanelRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mPanelRecyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(this)
                .paintProvider(mPanelAdapter)
                .visibilityProvider(mPanelAdapter)
                .marginProvider(mPanelAdapter)
                .build());

        // floating action menu
        mFloatingActionMenu = (FloatingActionMenu) findViewById(R.id.float_action_menu);
        mFloatingActionMenu.setIconAnimated(false);
        mFloatingActionMenu.setClosedOnTouchOutside(true);


        // sliding up panel
        mSlidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        mSlidingUpPanelLayout.setFadeOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);

                onBottomPanelClose();
            }
        });

        mSlidingUpPanelLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {

            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState,
                                            SlidingUpPanelLayout.PanelState newState) {

                Log.d(TAG, "PanelState pre:" + String.valueOf(previousState));
                Log.d(TAG, "PanelState new:" + String.valueOf(newState));

                if (previousState == SlidingUpPanelLayout.PanelState.DRAGGING
                        && newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {

                    onBottomPanelClose();
                }
            }
        });
    }

    @Override
    public void onFileOptionClick(File file) {
        onPanelRecyclerViewUpdate(file);

        // 展开底部panel
        mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);

        onBottomPanelOpen();
    }

    protected abstract RecyclerView.ItemDecoration onCreateContentItemDecoration();

    protected abstract RecyclerView.LayoutManager onCreateContentLayoutManager();

    protected abstract RecyclerView.Adapter onCreateContentAdapter();

    protected abstract void onPanelRecyclerViewUpdate(File file);

    protected abstract PanelBaseAdapter onCreatePanelAdapter();

    protected void onBottomPanelClose() {
        if (mContentRecyclerView != null) {
            mContentRecyclerView.setEnabled(true);
        }
        if (mContentLayoutManager != null) {
            if (mContentLayoutManager instanceof LinearContentLayoutManager) {
                ((LinearContentLayoutManager) mContentLayoutManager).setCanScroll(true);
            } else {
                ((GridContentLayoutManager) mContentLayoutManager).setCanScroll(true);
            }
        }
    }

    protected void onBottomPanelOpen() {
        if (mContentRecyclerView != null) {
            mContentRecyclerView.setEnabled(false);
        }
        if (mContentLayoutManager != null) {
            if (mContentLayoutManager instanceof LinearContentLayoutManager) {
                ((LinearContentLayoutManager) mContentLayoutManager).setCanScroll(false);
            } else {
                ((GridContentLayoutManager) mContentLayoutManager).setCanScroll(false);
            }
        }
    }

    protected static class LinearContentLayoutManager extends LinearLayoutManager {
        private boolean canScroll = true;

        public void setCanScroll(boolean canScroll) {
            this.canScroll = canScroll;
        }

        public LinearContentLayoutManager(Context context) {
            super(context);
        }

        @Override
        public boolean canScrollVertically() {
            return canScroll;
        }
    }

    protected static class GridContentLayoutManager extends GridLayoutManager {
        private boolean canScroll = true;

        public void setCanScroll(boolean canScroll) {
            this.canScroll = canScroll;
        }

        public GridContentLayoutManager(Context context, int spanCount) {
            super(context, spanCount);
        }

        @Override
        public boolean canScrollVertically() {
            return canScroll;
        }
    }
}

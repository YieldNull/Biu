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
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.Toast;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapters.choose.ContentBaseAdapter;
import com.bbbbiu.biu.gui.adapters.choose.FileContentAdapter;
import com.bbbbiu.biu.gui.adapters.choose.OnChangeDirListener;
import com.bbbbiu.biu.gui.adapters.choose.OnLoadingDataListener;
import com.bbbbiu.biu.gui.adapters.choose.PanelBaseAdapter;
import com.github.clans.fab.FloatingActionMenu;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.squareup.picasso.Picasso;
import com.wang.avi.AVLoadingIndicatorView;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.io.File;
import java.util.Stack;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 选各类文件的基类。
 * <p>
 * 各类文件选择共用一个布局文件“R.layout.activity_choose_base”
 * 界面中主内容为一个RecyclerView，记为 ContentRecyclerView
 * 底部有一个滑出菜单，也是RecyclerView，记为PanelRecyclerView
 * <p>
 * 有五个抽象方法，提供相应实现即可
 */
public abstract class ChooseBaseActivity extends AppCompatActivity implements
        OnChoosingListener, OnItemOptionClickListener, OnLoadingDataListener {

    private static final String TAG = FileChooseActivity.class.getSimpleName();

    @Bind(R.id.sliding_layout)
    protected SlidingUpPanelLayout mSlidingUpPanelLayout;

    @Bind(R.id.recyclerView_content)
    protected RecyclerView mContentRecyclerView;

    @Bind(R.id.recyclerView_panel)
    protected RecyclerView mPanelRecyclerView;

    @Bind(R.id.float_action_menu)
    protected FloatingActionMenu mFloatingActionMenu;

    @Bind(R.id.loadingIndicatorView)
    protected AVLoadingIndicatorView mLoadingIndicatorView;

    protected ContentBaseAdapter mContentAdapter;
    protected RecyclerView.LayoutManager mContentLayoutManager;
    protected PanelBaseAdapter mPanelAdapter;


    /**
     * 纪录滑动的位置，为隐藏、显示floating button用
     */
    private int mPreviousVisibleItem;

    @OnClick(R.id.fbtn_send_android)
    protected void clickSendAndroid() {
        mFloatingActionMenu.toggle(false);
        onSendAndroidClicked();
    }

    @OnClick(R.id.fbtn_send_ios)
    protected void clickSendIOS() {
        mFloatingActionMenu.toggle(false);
        onSendIOSClicked();
    }

    @OnClick(R.id.fbtn_send_computer)
    protected void clickSendComputer() {
        mFloatingActionMenu.toggle(false);
        onSendComputerClicked();
    }


    protected abstract void onSendIOSClicked();

    protected abstract void onSendAndroidClicked();

    protected abstract void onSendComputerClicked();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_base);
        ButterKnife.bind(this);

        // Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
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
        mContentAdapter = onCreateContentAdapter();
        if (mContentAdapter == null) {
            throw new NullPointerException("Content adapter can not be null");
        }
        mContentRecyclerView.setAdapter(mContentAdapter);

        mContentLayoutManager = onCreateContentLayoutManager();
        if (!(mContentLayoutManager instanceof LinearContentLayoutManager) &&
                !(mContentLayoutManager instanceof GridContentLayoutManager)) {
            throw new RuntimeException(("You must use custom content LayoutManager"));
        }
        mContentRecyclerView.setLayoutManager(mContentLayoutManager);

        RecyclerView.ItemDecoration itemDecoration = onCreateContentItemDecoration();
        if (itemDecoration != null) {
            mContentRecyclerView.addItemDecoration(itemDecoration);
        }

        // 滑动panel RecyclerView
        mPanelAdapter = onCreatePanelAdapter();

        if (mPanelAdapter != null) {
            mPanelRecyclerView.setAdapter(mPanelAdapter);
            mPanelRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            mPanelRecyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(this)
                    .paintProvider(mPanelAdapter)
                    .visibilityProvider(mPanelAdapter)
                    .marginProvider(mPanelAdapter)
                    .build());
        }

        // floating action menu
        mFloatingActionMenu.setIconAnimated(false);
        mFloatingActionMenu.setClosedOnTouchOutside(true);
        mFloatingActionMenu.setOnMenuButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mContentAdapter.getChosenFiles().size() == 0) {
                    Toast.makeText(ChooseBaseActivity.this, R.string.hint_choose_file_required, Toast.LENGTH_SHORT).show();
                } else {
                    mFloatingActionMenu.toggle(true);
                }
            }
        });

        // show hide动画
        mFloatingActionMenu.setMenuButtonShowAnimation(AnimationUtils.loadAnimation(this, R.anim.show_from_bottom));
        mFloatingActionMenu.setMenuButtonHideAnimation(AnimationUtils.loadAnimation(this, R.anim.hide_to_bottom));

        // 滑动隐藏 floating button
        mContentRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int firstPosition;

                if (mContentLayoutManager instanceof LinearContentLayoutManager) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) mContentLayoutManager;
                    firstPosition = layoutManager.findFirstVisibleItemPosition();
                } else {
                    GridContentLayoutManager layoutManager = (GridContentLayoutManager) mContentLayoutManager;
                    firstPosition = layoutManager.findFirstVisibleItemPosition();
                }

                if (firstPosition > mPreviousVisibleItem) {
                    mFloatingActionMenu.hideMenu(true);
                } else if (firstPosition < mPreviousVisibleItem) {
                    mFloatingActionMenu.showMenu(true);
                }
                mPreviousVisibleItem = firstPosition;
            }
        });

        // sliding up panel.点击 滑出Panel外部时，panel关闭
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

    /**
     * 底部滑出菜单一般是每个文件都有的，点击时传入对应的File
     * 然后更新PanelRecyclerView(更新对应的文件)
     * <p>
     * 使用方法见 {@link FileContentAdapter}
     *
     * @param file 对应的文件
     */
    @Override
    public void onFileOptionClick(File file) {
        onPanelRecyclerViewUpdate(file);

        // 展开底部panel
        mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);

        onBottomPanelOpen();
    }

    /**
     * 加载数据时显示loading动画
     */
    @Override
    public void OnStartLoadingData() {
        mLoadingIndicatorView.setVisibility(View.VISIBLE);
    }

    /**
     * 数据加载完毕，关闭loading动画
     */
    @Override
    public void OnFinishLoadingData() {
        mLoadingIndicatorView.setVisibility(View.GONE);
    }


    /**
     * Create Adapter for ContentRecyclerView
     *
     * @return ContentAdapter
     */
    protected abstract ContentBaseAdapter onCreateContentAdapter();

    /**
     * ContentRecyclerView 中各项内容的分割线
     * <p>
     * 当LayoutManager使用LinearLayoutManager时，
     * 返回 new HorizontalDividerItemDecoration.Builder(this).build();
     * <p>
     * 当使用GridLayoutManager使，返回相应ItemDecoration
     * <p>
     * 参见开源库 com.yqritc:recyclerview-flexibledivider
     *
     * @return 可以为空
     */
    protected abstract RecyclerView.ItemDecoration onCreateContentItemDecoration();


    /**
     * ContentRecyclerView对应的布局管理
     * 必须使用本类中自定义的两种布局管理器
     *
     * @return ContentLinearLayoutManager or ContentGridLayoutManager
     * @see ChooseBaseActivity.LinearContentLayoutManager
     * @see ChooseBaseActivity.GridContentLayoutManager
     */
    protected abstract RecyclerView.LayoutManager onCreateContentLayoutManager();


    /**
     * Create Adapter for PanelRecyclerView
     * <p>
     * 当需要使用底部的滑出菜单时，创建相应的Adapter。
     * Adapter必须继承PanelBaseAdapter
     *
     * @return adapter 可以为空，表示不使用底部的滑出菜单
     * @see PanelBaseAdapter
     */
    protected abstract PanelBaseAdapter onCreatePanelAdapter();

    /**
     * 底部滑出菜单一般是每个文件都有的，当点击另一个文件的菜单时，
     * 更新PanelRecyclerView的Adapter里面纪录的File即可。依具体实现而定
     *
     * @param file 滑出菜单对应的文件
     */
    protected abstract void onPanelRecyclerViewUpdate(File file);


    /**
     * 当底部滑出Panel关闭后，enable ContentRecyclerView
     */
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

    /**
     * 当底部滑出Panel打开后，disable ContentRecyclerView
     * 否则 SlidingUpLayout的FadeOnClickListener会与RecyclerView相应的监听器冲突
     */
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

    /**
     * 自定义的LinearLayoutManager，实现canScrollVertically的管理
     */
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

    /**
     * 自定义的GridLayoutManager，实现canScrollVertically的管理
     */
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

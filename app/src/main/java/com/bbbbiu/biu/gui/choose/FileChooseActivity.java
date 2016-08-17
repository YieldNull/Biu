package com.bbbbiu.biu.gui.choose;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapter.choose.FileOptionAdapter;
import com.bbbbiu.biu.gui.adapter.choose.content.BaseContentAdapter;
import com.bbbbiu.biu.gui.adapter.choose.content.FileContentAdapter;
import com.bbbbiu.biu.gui.choose.listener.OnChangeDirListener;
import com.bbbbiu.biu.gui.choose.listener.OptionPanelActionListener;
import com.bbbbiu.biu.util.StorageUtil;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Stack;

import butterknife.Bind;

public class FileChooseActivity extends BaseChooseActivity implements
        OnChangeDirListener, OptionPanelActionListener {

    private static final String TAG = FileChooseActivity.class.getSimpleName();

    public static final String EXTRA_ROOT_FILE_PATH = "com.bbbbiu.biu.FileChooseActivity.extra.ROOT_FILE_PATH";

    private FileContentAdapter mFileAdapter;

    /**
     * Stack of 进入的文件夹
     */
    private Stack<File> mDirStack = new Stack<>();
    private Parcelable mRecyclerViewState;

    private Handler mHandler = new Handler();

    private boolean mBottomPanelOpened;

    @Bind(R.id.sliding_layout)
    protected SlidingUpPanelLayout mSlidingUpPanelLayout;

    @Bind(R.id.recyclerView_panel)
    protected RecyclerView mOptionRecyclerView;

    @Bind(R.id.scrollView)
    protected HorizontalScrollView mHeaderScrollView;

    @Bind(R.id.textView_dir)
    protected TextView mHeaderDirText;

    private FileOptionAdapter mOptionAdapter;


    public FileChooseActivity() {
        super(R.layout.activity_choose_file);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFileAdapter = (FileContentAdapter) super.mContentAdapter;
        mDirStack.add(mFileAdapter.getCurrentDir());

        mHeaderScrollView.setVisibility(View.VISIBLE);
        mHeaderDirText.setText(mFileAdapter.getCurrentDir().getAbsolutePath());


        // 底部滑出Panel的 RecyclerView
        mOptionAdapter = new FileOptionAdapter(this);

        mOptionRecyclerView.setAdapter(mOptionAdapter);
        mOptionRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mOptionRecyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(this)
                .paintProvider(mOptionAdapter)
                .visibilityProvider(mOptionAdapter)
                .marginProvider(mOptionAdapter)
                .build());

        // sliding up panel.点击 滑出Panel外部时，panel关闭
        mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        mSlidingUpPanelLayout.setFadeOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeBottomPanel();
            }
        });

        mSlidingUpPanelLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {

            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState,
                                            SlidingUpPanelLayout.PanelState newState) {

                if (previousState == SlidingUpPanelLayout.PanelState.DRAGGING
                        && newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {

                    onBottomPanelClose();
                }
            }
        });
    }


    @Override
    protected int getNormalMenuId() {
        return R.menu.file_normal;
    }

    @Override
    protected int getChosenMenuId() {
        return R.menu.file_chosen;
    }

    @Override
    protected String getNormalTitle() {
        return getString(R.string.title_activity_choose_file);
    }


    @Override
    protected BaseContentAdapter onCreateContentAdapter() {
        Bundle bundle = getIntent().getExtras();
        File rootDir = null;
        if (bundle != null) {
            String path = bundle.getString(EXTRA_ROOT_FILE_PATH);
            if (path == null) {
                throw new RuntimeException("Root file path can not be null");
            }
            rootDir = new File(path);
        }

        return new FileContentAdapter(this, rootDir);
    }

    @Override
    protected LinearLayoutManager onCreateContentLayoutManager() {
        return new LinearContentLayoutManager(this);
    }

    @Override
    protected RecyclerView.ItemDecoration onCreateContentItemDecoration() {
        return new HorizontalDividerItemDecoration.Builder(this).build();
    }


    /****************************************************************************************/

    @Override
    public void onBackPressed() {
        if (isBottomPanelOpened()) {
            closeBottomPanel();
        } else {
            if (mDirStack.size() == 1) {
                super.onBackPressed();
            } else {
                onExitDir(null);
            }
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
        File destDir = null;

        if (data != null) {
            destDir = (File) data.getSerializableExtra(FileMoveActivity.EXTRA_DEST_DIR);
        }

        if (data == null || destDir == null) {
            Toast.makeText(this, R.string.hint_file_operation_canceled, Toast.LENGTH_SHORT).show();
            return;
        }

        // 把File以及其所有父文件夹加入文件夹栈中
        String internalRoot = StorageUtil.getRootDir(this, StorageUtil.STORAGE_INTERNAL).getAbsolutePath();
        String externalRoot = StorageUtil.getRootDir(this, StorageUtil.STORAGE_EXTERNAL).getAbsolutePath();

        String root = destDir.getAbsolutePath().contains(internalRoot) ? internalRoot : externalRoot;

        mDirStack.clear();
        File parent = destDir.getAbsoluteFile();
        mDirStack.push(parent);
        while (!parent.getAbsolutePath().equals(root)) {
            parent = parent.getParentFile();
            mDirStack.push(parent);
        }
        Collections.reverse(mDirStack);

        mFileAdapter.setCurrentDir(destDir);
        mContentRecyclerView.swapAdapter(mFileAdapter, true);

        // 开线程移动文件
        final boolean isCopy = requestCode == FileMoveActivity.REQUEST_COPY;

        showToastInUI(R.string.hint_file_operation_background_running);

        final File src = mOptionAdapter.getBoundFile();
        final File finalDestDir = destDir;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (moveOrCopyFile(src, finalDestDir, isCopy)) {
                    FileChooseActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onOptionModifyContent();
                        }
                    });
                }
            }
        }).start();
    }


    /*********************************************************************************
     * ***************************** {@link OnChangeDirListener}***********************
     *********************************************************************************/

    @Override
    public void onEnterDir(File dir) {
        mRecyclerViewState = mContentRecyclerView.getLayoutManager().onSaveInstanceState();


        // adapter.notifyDatasetChanged()只改变数据，不改变滑动的位置，卧槽
        mDirStack.add(dir);
        mFileAdapter.setCurrentDir(dir);
        mFileAdapter.cancelPicassoTask();
        mContentRecyclerView.swapAdapter(mFileAdapter, true);

        setFileAllDismissed();

        mHeaderDirText.setText(dir.getAbsolutePath());
        mHandler.postDelayed(new Runnable() {
            public void run() {
                mHeaderScrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
            }
        }, 100L);
    }


    @Override
    public void onExitDir(File dir) {
        mDirStack.pop();
        mFileAdapter.setCurrentDir(mDirStack.peek());
        mFileAdapter.cancelPicassoTask();
        mContentRecyclerView.swapAdapter(mFileAdapter, true);

        setFileAllDismissed();

        // 只纪录一次，多次返回则直接默认显示
        if (mRecyclerViewState != null) {
            mContentRecyclerView.getLayoutManager().onRestoreInstanceState(mRecyclerViewState);
        }
        mRecyclerViewState = null;

        mHeaderDirText.setText(mDirStack.peek().getAbsolutePath());
        mHandler.postDelayed(new Runnable() {
            public void run() {
                mHeaderScrollView.fullScroll(HorizontalScrollView.FOCUS_LEFT);
            }
        }, 100L);
    }


    /**************************{@link OptionPanelActionListener}***********************************/

    /**
     * 底部滑出菜单一般是每个文件都有的，点击时传入对应的File
     * 然后更新PanelRecyclerView(更新对应的文件)
     * <p/>
     * 使用方法见 {@link FileContentAdapter}
     *
     * @param file 对应的文件
     */
    @Override
    public void onOptionToggleClicked(File file) {
        onPanelRecyclerViewUpdate(file);

        // 展开底部panel
        mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);

        onBottomPanelOpen();
    }

    @Override
    public void onOptionItemClicked(File file) {
        closeBottomPanel();
    }

    @Override
    public void onOptionModifyContent() {
        mContentAdapter.updateDataSet();
    }

    @Override
    public void onOptionDeleteFile(File file) {
        if (mFileAdapter.isFileChosen(file)) {
            mFileAdapter.setFileChosen(file, false);
            mFileAdapter.notifyFileDismissed(file.getAbsolutePath());
        }
    }

    /********************************************************************************************/

    /********************************************************************************************/

    /**
     * 划出菜单是否已打开
     *
     * @return 是否打开
     */
    private boolean isBottomPanelOpened() {
        return mBottomPanelOpened;
    }

    /**
     * 关闭底部滑出菜单
     */
    private void closeBottomPanel() {
        mSlidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);

        onBottomPanelClose();
    }

    /**
     * 当底部滑出Panel关闭后，enable ContentRecyclerView
     */
    private void onBottomPanelClose() {
        mBottomPanelOpened = false;
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
    private void onBottomPanelOpen() {
        mBottomPanelOpened = true;
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
     * 底部滑出菜单一般是每个文件都有的，当点击另一个文件的菜单时，
     * 更新PanelRecyclerView的Adapter里面纪录的File即可。依具体实现而定
     *
     * @param file 滑出菜单对应的文件
     */
    private void onPanelRecyclerViewUpdate(File file) {
        FileOptionAdapter adapter = mOptionAdapter;
        adapter.setBoundFile(file);
        adapter.notifyDataSetChanged();
    }


    /**
     * 移动或复制文件（夹）
     *
     * @param src    原文件（夹）
     * @param dest   目的文件夹
     * @param isCopy 是否是复制
     * @return 是否成功
     */
    private boolean moveOrCopyFile(File src, File dest, boolean isCopy) {
        try {
            if (isCopy) {
                if (src.isFile()) {
                    FileUtils.copyFileToDirectory(src, dest);
                } else {
                    FileUtils.copyDirectoryToDirectory(src, dest);
                }
            } else {
                FileUtils.moveToDirectory(src, dest, false);
            }
        } catch (FileExistsException e) {
            Log.w(TAG, e.toString());
            if (src.isFile()) {
                showToastInUI(R.string.hint_file_exists_file);
            } else {
                showToastInUI(R.string.hint_file_exists_folder);
            }
            return false;
        } catch (IOException e) {
            Log.w(TAG, e.toString());

            if (isCopy) {
                showToastInUI(R.string.hint_file_copy_failed);
            } else {
                showToastInUI(R.string.hint_file_move_failed);
            }
            return false;
        }

        if (isCopy) {
            showToastInUI(R.string.hint_file_copy_succeeded);
        } else {
            showToastInUI(R.string.hint_file_move_succeeded);
        }

        return true;
    }


    /******************************** LayoutManager *************************************/

    /**
     * 自定义的LinearLayoutManager，实现canScrollVertically的管理
     */
    private static class LinearContentLayoutManager extends LinearLayoutManager {
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
    private static class GridContentLayoutManager extends GridLayoutManager {
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

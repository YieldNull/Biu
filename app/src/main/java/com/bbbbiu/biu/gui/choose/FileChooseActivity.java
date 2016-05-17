package com.bbbbiu.biu.gui.choose;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapter.choose.BaseContentAdapter;
import com.bbbbiu.biu.gui.adapter.choose.FileOptionAdapter;
import com.bbbbiu.biu.gui.choose.listener.OnChangeDirListener;
import com.bbbbiu.biu.gui.adapter.choose.BaseOptionAdapter;
import com.bbbbiu.biu.gui.adapter.choose.FileContentAdapter;
import com.bbbbiu.biu.util.StorageUtil;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Stack;

public class FileChooseActivity extends BaseChooseActivity implements OnChangeDirListener {
    private static final String TAG = FileChooseActivity.class.getSimpleName();

    public static final String EXTRA_ROOT_FILE_PATH = "com.bbbbiu.biu.FileChooseActivity.extra.ROOT_FILE_PATH";

    private FileContentAdapter mFileAdapter;
    private FileOptionAdapter mOptionAdapter;

    /**
     * Stack of 进入的文件夹
     */
    private Stack<File> mDirStack = new Stack<>();
    private Parcelable mRecyclerViewState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFileAdapter = (FileContentAdapter) super.mContentAdapter;
        mOptionAdapter = (FileOptionAdapter) mPanelAdapter;
        mDirStack.add(mFileAdapter.getCurrentDir());

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
    protected RecyclerView.LayoutManager onCreateContentLayoutManager() {
        return new LinearContentLayoutManager(this);
    }

    @Override
    protected RecyclerView.ItemDecoration onCreateContentItemDecoration() {
        return new HorizontalDividerItemDecoration.Builder(this).build();
    }


    @Override
    protected BaseOptionAdapter onCreatePanelAdapter() {
        return new FileOptionAdapter(this);
    }

    @Override
    protected void onPanelRecyclerViewUpdate(File file) {
        FileOptionAdapter adapter = (FileOptionAdapter) mPanelAdapter;
        adapter.setBoundFile(file);
        adapter.notifyDataSetChanged();
    }


    @Override
    public void onEnterDir(File dir) {
        mRecyclerViewState = mContentRecyclerView.getLayoutManager().onSaveInstanceState();


        // adapter.notifyDatasetChanged()只改变数据，不改变滑动的位置，卧槽
        mDirStack.add(dir);
        mFileAdapter.setCurrentDir(dir);
        mFileAdapter.cancelPicassoTask();
        mContentRecyclerView.swapAdapter(mFileAdapter, true);

        setFileAllDismissed();

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
    }

    @Override
    public void onOptionDeleteFile(File file) {
        if (mFileAdapter.isFileChosen(file)) {
            mFileAdapter.setFileChosen(file, false);
            mFileAdapter.notifyFileDismissed(file.getAbsolutePath());
        }
    }

    @Override
    public void onBackPressed() {
        closeBottomPanel();

        if (mDirStack.size() == 1) {
            super.onBackPressed();
        } else {
            onExitDir(null);
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
        File destDir = null;

        if (data != null) {
            destDir = (File) data.getSerializableExtra(FileMoveActivity.EXTRA_DEST_DIR);
        }

        if (data == null || destDir == null) {
            Toast.makeText(this, R.string.file_operation_canceled, Toast.LENGTH_SHORT).show();
            return;
        }

        // 把File以及其所有父文件夹加入文件夹栈中
        String internalRoot = StorageUtil.getRootDir(this, StorageUtil.TYPE_INTERNAL).getAbsolutePath();
        String externalRoot = StorageUtil.getRootDir(this, StorageUtil.TYPE_EXTERNAL).getAbsolutePath();

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

        //showOperationRunningDialog(isCopy);

        showToastInUI(R.string.file_background_running);

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

    /**
     * 显示正在操作的ProgressDialog
     *
     * @param isCopy 是否是复制
     */
    private void showOperationRunningDialog(boolean isCopy) {
        String message = isCopy ?
                getString(R.string.file_copy_running) :
                getString(R.string.file_move_running);


        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);

        progressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showToastInUI(R.string.file_operation_canceled);
                    }
                });
        progressDialog.setButton(ProgressDialog.BUTTON_POSITIVE, getString(R.string.file_background_run),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showToastInUI(R.string.file_background_running);
                    }
                });
        progressDialog.show();
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
                showToastInUI(R.string.file_exists_file);
            } else {
                showToastInUI(R.string.file_exists_folder);
            }
            return false;
        } catch (IOException e) {
            Log.w(TAG, e.toString());

            if (isCopy) {
                showToastInUI(R.string.file_copy_failed);
            } else {
                showToastInUI(R.string.file_move_failed);
            }
            return false;
        }

        if (isCopy) {
            showToastInUI(R.string.file_copy_succeeded);
        } else {
            showToastInUI(R.string.file_move_succeeded);
        }

        return true;
    }

}

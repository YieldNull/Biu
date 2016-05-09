package com.bbbbiu.biu.gui.choose;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapter.choose.ContentBaseAdapter;
import com.bbbbiu.biu.gui.adapter.choose.OnChangeDirListener;
import com.bbbbiu.biu.gui.adapter.choose.PanelBaseAdapter;
import com.bbbbiu.biu.gui.adapter.choose.FileContentAdapter;
import com.bbbbiu.biu.gui.adapter.choose.FilePanelAdapter;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.io.File;
import java.util.Stack;

public class FileChooseActivity extends ChooseBaseActivity implements OnChangeDirListener {
    private static final String TAG = FileChooseActivity.class.getSimpleName();

    public static final String EXTRA_ROOT_FILE_PATH = "com.bbbbiu.biu.FileChooseActivity.extra.ROOT_FILE_PATH";

    private FileContentAdapter mFileAdapter;

    /**
     * Stack of 进入的文件夹
     */
    private Stack<File> mDirStack = new Stack<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFileAdapter = (FileContentAdapter) super.mContentAdapter;
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
    protected ContentBaseAdapter onCreateContentAdapter() {
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
    protected PanelBaseAdapter onCreatePanelAdapter() {
        return new FilePanelAdapter(this);
    }

    @Override
    protected void onPanelRecyclerViewUpdate(File file) {
        FilePanelAdapter adapter = (FilePanelAdapter) mPanelAdapter;
        adapter.setFile(file);
        adapter.notifyDataSetChanged();
    }


    @Override
    public void onEnterDir(File dir) {
        // adapter.notifyDatasetChanged()只改变数据，不改变滑动的位置，卧槽
        mDirStack.add(dir);
        mFileAdapter.setCurrentDir(dir);
        mFileAdapter.cancelPicassoTask();

        mContentRecyclerView.swapAdapter(mFileAdapter, true);
    }


    @Override
    public void onExitDir(File dir) {
        mDirStack.pop();
        mFileAdapter.setCurrentDir(mDirStack.peek());
        mFileAdapter.cancelPicassoTask();

        mContentRecyclerView.swapAdapter(mFileAdapter, true);
    }

    @Override
    public void onBackPressed() {
        if (mDirStack.size() == 1) {
            super.onBackPressed();
        } else {
            onExitDir(null);
        }
    }

}

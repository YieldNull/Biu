package com.bbbbiu.biu.gui.choose;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapters.PanelBaseAdapter;
import com.bbbbiu.biu.gui.adapters.FileAdapter;
import com.bbbbiu.biu.gui.adapters.FilePanelAdapter;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.io.File;

public class FileChooseActivity extends ChooseBaseActivity {

    public static final String INTENT_EXTRA_ROOT_FILE_PATH = "com.bbbbiu.biu.FileChooseActivity.INTENT_EXTRA_ROOT_FILE_PATH";

    private static final String TAG = FileChooseActivity.class.getSimpleName();


    private FileAdapter mFileAdapter;

    private int chosenFileCount;

    private void setTitle() {
        getSupportActionBar().setTitle(String.valueOf(chosenFileCount));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFileAdapter = (FileAdapter) super.mContentAdapter;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (chosenFileCount == 0) {
            getMenuInflater().inflate(R.menu.file_choose, menu);
        } else {
            getMenuInflater().inflate(R.menu.file_chosen, menu);
        }
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
                chosenFileCount = mFileAdapter.setFileAllChosen();
                invalidateOptionsMenu();
                setTitle();
                break;

            case R.id.action_choosing_dismiss:
                mFileAdapter.dismissChoosing();
                chosenFileCount = 0;
                invalidateOptionsMenu();
                getSupportActionBar().setTitle(getString(R.string.title_activity_choose));
                break;
            default:
                break;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (!mFileAdapter.quitDir()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onFileChosen(File file) {
        if (chosenFileCount++ == 0) {
            invalidateOptionsMenu();
        }
        setTitle();

    }

    @Override
    public void onFileDismissed(File file) {
        if (--chosenFileCount == 0) {
            invalidateOptionsMenu();
        }
        setTitle();
    }

    @Override
    protected RecyclerView.Adapter onCreateContentAdapter() {
        Bundle bundle = getIntent().getExtras();
        File rootDir = null;
        if (bundle != null) {
            String path = bundle.getString(INTENT_EXTRA_ROOT_FILE_PATH);
            if (path == null) {
                throw new RuntimeException("Root file path can not be null");
            }
            rootDir = new File(path);
        }

        return new FileAdapter(this, rootDir);
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
}

package com.bbbbiu.biu.gui.choose;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.ConnectComputerActivity;
import com.bbbbiu.biu.gui.adapters.choose.ContentBaseAdapter;
import com.bbbbiu.biu.gui.adapters.choose.PanelBaseAdapter;
import com.bbbbiu.biu.gui.adapters.choose.FileContentAdapter;
import com.bbbbiu.biu.gui.adapters.choose.FilePanelAdapter;
import com.bbbbiu.biu.util.Preference;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.io.File;
import java.util.List;

public class FileChooseActivity extends ChooseBaseActivity {

    public static final String INTENT_EXTRA_ROOT_FILE_PATH = "com.bbbbiu.biu.FileChooseActivity.INTENT_EXTRA_ROOT_FILE_PATH";

    private static final String TAG = FileChooseActivity.class.getSimpleName();


    private FileContentAdapter mFileAdapter;

    private int chosenFileCount;

    @SuppressWarnings("ConstantConditions")
    private void setTitle() {
        if (chosenFileCount == 0) {
            getSupportActionBar().setTitle(getString(R.string.title_activity_choose));
        } else {
            getSupportActionBar().setTitle(String.valueOf(chosenFileCount));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFileAdapter = (FileContentAdapter) super.mContentAdapter;
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

    @SuppressWarnings("ConstantConditions")
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
    protected ContentBaseAdapter onCreateContentAdapter() {
        Bundle bundle = getIntent().getExtras();
        File rootDir = null;
        if (bundle != null) {
            String path = bundle.getString(INTENT_EXTRA_ROOT_FILE_PATH);
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
    protected void onSendIOSClicked() {

    }

    @Override
    protected void onSendAndroidClicked() {

    }

    @Override
    protected void onSendComputerClicked() {

        List<File> files = mFileAdapter.getChosenFiles();
        Preference.storeFilesToSend(this, files);

        Log.i(TAG, "Sending files to computer. File Amount " + files.size());

        ConnectComputerActivity.connectForUpload(this);
    }
}

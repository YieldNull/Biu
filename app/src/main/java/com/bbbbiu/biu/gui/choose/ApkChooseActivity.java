package com.bbbbiu.biu.gui.choose;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.bbbbiu.biu.gui.adapters.choose.ApkContentAdapter;
import com.bbbbiu.biu.gui.adapters.choose.PanelBaseAdapter;
import com.bbbbiu.biu.gui.adapters.choose.ContentBaseAdapter;

import java.io.File;

public class ApkChooseActivity extends ChooseBaseActivity {

    public static String TAG = ApkChooseActivity.class.getSimpleName();

    @Override
    protected RecyclerView.ItemDecoration onCreateContentItemDecoration() {
        return null;
    }

    @Override
    protected RecyclerView.LayoutManager onCreateContentLayoutManager() {
        final GridContentLayoutManager manager = new GridContentLayoutManager(this, 4);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return mContentAdapter.getItemViewType(position) == ContentBaseAdapter.VIEW_TYPE_HEADER ? manager.getSpanCount() : 1;
            }
        });
        return manager;
    }


    @Override
    protected ContentBaseAdapter onCreateContentAdapter() {
        return new ApkContentAdapter(this);
    }

    @Override
    protected void onPanelRecyclerViewUpdate(File file) {

    }

    @Override
    protected PanelBaseAdapter onCreatePanelAdapter() {
        return null;
    }

    @Override
    public void onFileChosen(File file) {

    }

    @Override
    public void onFileDismissed(File file) {

    }

    @Override
    protected void onSendIOSClicked() {

    }

    @Override
    protected void onSendAndroidClicked() {

    }

    @Override
    protected void onSendComputerClicked() {

    }

    @Override
    public void onBackPressed() {
        mContentAdapter.cancelPicassoTask();
        super.onBackPressed();
    }
}

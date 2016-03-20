package com.bbbbiu.biu.gui.choose;

import android.support.v7.widget.RecyclerView;

import com.bbbbiu.biu.gui.adapters.PanelBaseAdapter;

import java.io.File;

public class DocumentChooseActivity extends ChooseBaseActivity {


    @Override
    protected RecyclerView.ItemDecoration onCreateContentItemDecoration() {
        return null;
    }

    @Override
    protected RecyclerView.LayoutManager onCreateContentLayoutManager() {
        return null;
    }

    @Override
    protected RecyclerView.Adapter onCreateContentAdapter() {
        return null;
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
}

package com.bbbbiu.biu.gui.choose;

import android.support.v7.widget.RecyclerView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapters.choose.ContentBaseAdapter;
import com.bbbbiu.biu.gui.adapters.choose.DocumentContentAdapter;
import com.bbbbiu.biu.gui.adapters.choose.PanelBaseAdapter;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.io.File;

/**
 * Created by YieldNull at 4/18/16
 */
public class DocumentChooseActivity extends ChooseBaseActivity {
    private static final String TAG = DocumentChooseActivity.class.getSimpleName();

    @Override
    protected int getNormalMenuId() {
        return R.menu.normal_common;
    }

    @Override
    protected int getChosenMenuId() {
        return R.menu.chosen_common;
    }

    @Override
    protected String getNormalTitle() {
        return getString(R.string.title_activity_choose_document);
    }

    @Override
    protected ContentBaseAdapter onCreateContentAdapter() {
        return new DocumentContentAdapter(this);
    }

    @Override
    protected RecyclerView.ItemDecoration onCreateContentItemDecoration() {
        return new HorizontalDividerItemDecoration.Builder(this).build();
    }

    @Override
    protected RecyclerView.LayoutManager onCreateContentLayoutManager() {
        return new LinearContentLayoutManager(this);
    }

    @Override
    protected PanelBaseAdapter onCreatePanelAdapter() {
        return null;
    }

    @Override
    protected void onPanelRecyclerViewUpdate(File file) {

    }
}

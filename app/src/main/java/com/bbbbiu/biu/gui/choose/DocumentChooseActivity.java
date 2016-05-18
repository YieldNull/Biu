package com.bbbbiu.biu.gui.choose;

import android.support.v7.widget.RecyclerView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapter.choose.content.BaseContentAdapter;
import com.bbbbiu.biu.gui.adapter.choose.content.DocumentContentAdapter;
import com.bbbbiu.biu.gui.adapter.choose.option.BaseOptionAdapter;
import com.bbbbiu.biu.util.StorageUtil;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.io.File;

/**
 * Created by YieldNull at 4/18/16
 */
public class DocumentChooseActivity extends BaseChooseActivity {
    private static final String TAG = DocumentChooseActivity.class.getSimpleName();

    @Override
    protected int getNormalMenuId() {
        return R.menu.common_normal;
    }

    @Override
    protected int getChosenMenuId() {
        return R.menu.common_chosen;
    }

    @Override
    protected String getNormalTitle() {
        return getString(R.string.title_activity_choose_document);
    }

    @Override
    protected BaseContentAdapter onCreateContentAdapter() {
        return new DocumentContentAdapter(this);
    }

    @Override
    public void onOptionToggleClicked(File file) {
        StorageUtil.openFile(this, file);
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
    protected BaseOptionAdapter onCreatePanelAdapter() {
        return null;
    }

    @Override
    protected void onPanelRecyclerViewUpdate(File file) {

    }
}

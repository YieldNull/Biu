package com.bbbbiu.biu.gui.choose;

import android.support.v7.widget.RecyclerView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapter.choose.BaseContentAdapter;
import com.bbbbiu.biu.gui.adapter.choose.MusicContentAdapter;
import com.bbbbiu.biu.gui.adapter.choose.BaseOptionAdapter;
import com.bbbbiu.biu.util.StorageUtil;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.io.File;

public class MusicChooseActivity extends BaseChooseActivity {

    public static String TAG = MusicChooseActivity.class.getSimpleName();

    @Override
    protected RecyclerView.ItemDecoration onCreateContentItemDecoration() {
        return new HorizontalDividerItemDecoration.Builder(this).build();
    }

    @Override
    public void onOptionToggleClicked(File file) {

        StorageUtil.openFile(this, file);
    }

    @Override
    protected RecyclerView.LayoutManager onCreateContentLayoutManager() {
        return new LinearContentLayoutManager(this);
    }

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
        return getString(R.string.title_activity_choose_music);
    }

    @Override
    protected BaseContentAdapter onCreateContentAdapter() {
        return new MusicContentAdapter(this);
    }

    @Override
    protected void onPanelRecyclerViewUpdate(File file) {

    }

    @Override
    protected BaseOptionAdapter onCreatePanelAdapter() {
        return null;
    }
}

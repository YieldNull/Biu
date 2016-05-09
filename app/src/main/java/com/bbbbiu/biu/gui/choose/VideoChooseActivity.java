package com.bbbbiu.biu.gui.choose;

import android.support.v7.widget.RecyclerView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapter.choose.ContentBaseAdapter;
import com.bbbbiu.biu.gui.adapter.choose.PanelBaseAdapter;
import com.bbbbiu.biu.gui.adapter.choose.VideoContentAdapter;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.io.File;

/**
 * Created by YieldNull at 4/18/16
 */
public class VideoChooseActivity extends ChooseBaseActivity {
    private static final String TAG = VideoChooseActivity.class.getSimpleName();

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
        return getString(R.string.title_activity_choose_video);
    }

    @Override
    protected ContentBaseAdapter onCreateContentAdapter() {
        return new VideoContentAdapter(this);
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

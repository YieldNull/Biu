package com.bbbbiu.biu.gui.choose;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapter.choose.content.BaseContentAdapter;
import com.bbbbiu.biu.gui.adapter.choose.content.MusicContentAdapter;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;


/**
 * 选择音乐文件
 */
public class MusicChooseActivity extends BaseChooseActivity {

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
    protected LinearLayoutManager onCreateContentLayoutManager() {
        return new LinearLayoutManager(this);
    }

    @Override
    protected RecyclerView.ItemDecoration onCreateContentItemDecoration() {
        return new HorizontalDividerItemDecoration.Builder(this).build();
    }
}

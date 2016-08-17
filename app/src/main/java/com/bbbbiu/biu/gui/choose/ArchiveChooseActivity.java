package com.bbbbiu.biu.gui.choose;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapter.choose.content.ArchiveContentAdapter;
import com.bbbbiu.biu.gui.adapter.choose.content.BaseContentAdapter;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;


/**
 * 选择压缩文件
 * <p/>
 * Created by YieldNull at 4/18/16
 */
public class ArchiveChooseActivity extends BaseChooseActivity {

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
        return getString(R.string.title_activity_choose_archive);
    }

    @Override
    protected BaseContentAdapter onCreateContentAdapter() {
        return new ArchiveContentAdapter(this);
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

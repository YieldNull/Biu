package com.bbbbiu.biu.gui.choose;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapter.choose.content.BaseContentAdapter;
import com.bbbbiu.biu.gui.adapter.choose.content.VideoContentAdapter;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;


/**
 * 选择视频文件
 * <p/>
 * Created by YieldNull at 4/18/16
 */
public class VideoChooseActivity extends BaseChooseActivity {
    private static final String TAG = VideoChooseActivity.class.getSimpleName();

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
        return getString(R.string.title_activity_choose_video);
    }

    @Override
    protected BaseContentAdapter onCreateContentAdapter() {
        return new VideoContentAdapter(this);
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

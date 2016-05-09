package com.bbbbiu.biu.gui.choose;

import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapter.choose.MusicContentAdapter;
import com.bbbbiu.biu.gui.adapter.choose.PanelBaseAdapter;
import com.bbbbiu.biu.gui.adapter.choose.ContentBaseAdapter;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.io.File;

public class MusicChooseActivity extends ChooseBaseActivity {

    public static String TAG = MusicChooseActivity.class.getSimpleName();

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                break;
            case R.id.action_search:
                break;
            default:
                break;
        }


        return super.onOptionsItemSelected(item);
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
    protected int getNormalMenuId() {
        return R.menu.music_normal;
    }

    @Override
    protected int getChosenMenuId() {
        return R.menu.chosen_common;
    }

    @Override
    protected String getNormalTitle() {
        return getString(R.string.title_activity_choose_music);
    }

    @Override
    protected ContentBaseAdapter onCreateContentAdapter() {
        return new MusicContentAdapter(this);
    }

    @Override
    protected void onPanelRecyclerViewUpdate(File file) {

    }

    @Override
    protected PanelBaseAdapter onCreatePanelAdapter() {
        return null;
    }
}

package com.bbbbiu.biu.gui.choose;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapter.choose.content.BaseContentAdapter;
import com.bbbbiu.biu.gui.adapter.choose.content.ImgContentAdapter;
import com.bbbbiu.biu.gui.adapter.choose.option.BaseOptionAdapter;
import com.bbbbiu.biu.gui.choose.listener.OnChangeDirListener;

import java.io.File;

/**
 * Created by YieldNull at 4/17/16
 */
public class ImgChooseActivity extends BaseChooseActivity implements OnChangeDirListener {
    private static final String TAG = ImgChooseActivity.class.getSimpleName();

    private ImgContentAdapter mImgAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFloatingActionMenu.setVisibility(View.GONE);
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
        return getString(R.string.title_activity_choose_image);
    }

    @Override
    protected BaseContentAdapter onCreateContentAdapter() {
        mImgAdapter = new ImgContentAdapter(this);
        return mImgAdapter;
    }

    @Override
    protected RecyclerView.ItemDecoration onCreateContentItemDecoration() {
        return null;
    }

    @Override
    protected RecyclerView.LayoutManager onCreateContentLayoutManager() {
        return new GridContentLayoutManager(this, 2);
    }

    @Override
    protected BaseOptionAdapter onCreatePanelAdapter() {
        return null;
    }

    @Override
    protected void onPanelRecyclerViewUpdate(File file) {

    }

    @Override
    public void onEnterDir(File dir) {
        mFloatingActionMenu.setVisibility(View.VISIBLE);

        mContentRecyclerView.setLayoutManager(new GridContentLayoutManager(this, 3));

        invalidateOptionsMenu();
    }

    @Override
    public void onExitDir(File dir) {
        mFloatingActionMenu.setVisibility(View.GONE);

        mContentRecyclerView.setLayoutManager(new GridContentLayoutManager(this, 2));

        invalidateOptionsMenu();
        refreshTitle();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return !mImgAdapter.inAlbum() || super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (mImgAdapter.inAlbum()) {
                mImgAdapter.exitDir();
            } else {
                finish();
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onBackPressed() {
        if (mImgAdapter.inAlbum()) {
            mImgAdapter.exitDir();
        } else {
            super.onBackPressed();
        }
    }
}

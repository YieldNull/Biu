package com.bbbbiu.biu.gui.choose;

import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapter.choose.BaseContentAdapter;
import com.bbbbiu.biu.gui.adapter.choose.CommonContentAdapter;
import com.bbbbiu.biu.gui.adapter.choose.ImgContentAdapter;
import com.bbbbiu.biu.gui.adapter.choose.BaseOptionAdapter;

import java.io.File;

/**
 * Created by YieldNull at 4/17/16
 */
public class ImgChooseActivity extends BaseChooseActivity {
    private static final String TAG = ImgChooseActivity.class.getSimpleName();
    private ImgContentAdapter mImgAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mImgAdapter = (ImgContentAdapter) mContentAdapter;
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
        return new ImgContentAdapter(this);
    }

    @Override
    protected RecyclerView.ItemDecoration onCreateContentItemDecoration() {
        return null;
    }

    @Override
    protected RecyclerView.LayoutManager onCreateContentLayoutManager() {
        final GridContentLayoutManager manager = new GridContentLayoutManager(this, 3);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return mImgAdapter.getItemViewType(position) == CommonContentAdapter.VIEW_TYPE_HEADER
                        ? manager.getSpanCount() : 1;
            }
        });
        return manager;
    }

    @Override
    protected BaseOptionAdapter onCreatePanelAdapter() {
        return null;
    }

    @Override
    protected void onPanelRecyclerViewUpdate(File file) {

    }
}

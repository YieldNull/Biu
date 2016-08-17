package com.bbbbiu.biu.gui.choose;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapter.choose.content.BaseContentAdapter;
import com.bbbbiu.biu.gui.adapter.choose.content.DocContentAdapter;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

/**
 * 选择文档
 * <p/>
 * Created by YieldNull at 4/18/16
 */
public class DocChooseActivity extends BaseChooseActivity {
    private static final String TAG = DocChooseActivity.class.getSimpleName();

    private DocContentAdapter mDocumentAdapter;

    @Override
    protected int getNormalMenuId() {
        return R.menu.doc_normal;
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
        mDocumentAdapter = new DocContentAdapter(this);

        return mDocumentAdapter;
    }

    @Override
    protected LinearLayoutManager onCreateContentLayoutManager() {
        return new LinearLayoutManager(this);
    }

    @Override
    protected RecyclerView.ItemDecoration onCreateContentItemDecoration() {
        return new HorizontalDividerItemDecoration.Builder(this).build();
    }

    /****************************************************************************************/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sort_by_folder:
                mDocumentAdapter.sortByFolder();
                return true;
            case R.id.action_sort_by_type:
                mDocumentAdapter.sortByType();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}

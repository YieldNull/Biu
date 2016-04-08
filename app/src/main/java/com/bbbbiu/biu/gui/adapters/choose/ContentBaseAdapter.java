package com.bbbbiu.biu.gui.adapters.choose;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.bbbbiu.biu.R;

import java.io.File;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by YieldNull at 3/26/16
 */
public abstract class ContentBaseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int VIEW_TYPE_HEADER = 0;
    public static final int VIEW_TYPE_ITEM = 1;
    private static final String TAG = ContentBaseAdapter.class.getSimpleName();

    private OnLoadingDataListener loadingDataListener;


    public ContentBaseAdapter(Context context) {
        loadingDataListener = (OnLoadingDataListener) context;
    }

    protected void notifyStartLoadingData() {
        loadingDataListener.OnStartLoadingData();
    }

    protected void notifyFinishLoadingData() {
        loadingDataListener.OnFinishLoadingData();
    }


    protected static class HeaderViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.textView)
        TextView headerText;

        HeaderViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public abstract List<File> getChosenFiles();

    public void cancelPicassoTask() {
    }
}

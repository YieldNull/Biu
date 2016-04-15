package com.bbbbiu.biu.gui.adapters.choose;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.choose.ChooseBaseActivity;
import com.bbbbiu.biu.gui.choose.OnChoosingListener;
import com.bbbbiu.biu.gui.choose.OnItemOptionClickListener;

import java.io.File;
import java.util.List;
import java.util.Set;

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
    private OnChoosingListener onChoosingListener;
    private OnItemOptionClickListener onItemOptionClickListener;

    public ContentBaseAdapter(ChooseBaseActivity context) {
        loadingDataListener = context;
        onChoosingListener = context;
        onItemOptionClickListener = context;
    }


    protected void notifyStartLoadingData() {
        loadingDataListener.OnStartLoadingData();
    }

    protected void notifyFinishLoadingData() {
        loadingDataListener.OnFinishLoadingData();
    }


    // 要在添加或删除文件之后进行notify
    protected void notifyFileChosen(String filePath) {
        onChoosingListener.onFileChosen(filePath);
    }

    protected void notifyFileDismissed(String filePath) {
        onChoosingListener.onFileDismissed(filePath);
    }

    protected void notifyFileItemOptionClicked(File file) {
        onItemOptionClickListener.onFileOptionClick(file);
    }


    public abstract Set<String> getChosenFiles();

    public abstract int getChosenCount();

    public abstract void setFileAllChosen();

    public abstract void setFileAllDismissed();

    public void cancelPicassoTask() {
    }


    protected static class HeaderViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.textView)
        TextView headerText;

        HeaderViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}

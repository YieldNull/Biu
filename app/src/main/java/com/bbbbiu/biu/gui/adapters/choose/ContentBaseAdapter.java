package com.bbbbiu.biu.gui.adapters.choose;

import android.support.v7.widget.RecyclerView;

import java.io.File;
import java.util.List;

/**
 * Created by YieldNull at 3/26/16
 */
public abstract class ContentBaseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = ContentBaseAdapter.class.getSimpleName();

    public abstract List<File> getChosenFiles();
}

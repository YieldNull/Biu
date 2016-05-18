package com.bbbbiu.biu.gui.adapter.util;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bbbbiu.biu.R;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * viewHolder of header in recyclerView
 */
public class HeaderViewHolder extends RecyclerView.ViewHolder {
    @Bind(R.id.textView)
    public TextView headerText;

    public static HeaderViewHolder build(LayoutInflater inflater, ViewGroup parent) {
        View itemView = inflater.inflate(R.layout.list_header_common, parent, false);
        return new HeaderViewHolder(itemView);
    }


    private HeaderViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
}

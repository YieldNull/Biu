package com.bbbbiu.biu.gui.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
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

    public HeaderViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
}

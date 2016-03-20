package com.bbbbiu.biu.gui.adapters;

import android.support.v7.widget.RecyclerView;

import com.yqritc.recyclerviewflexibledivider.FlexibleDividerDecoration;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

public abstract class PanelBaseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements FlexibleDividerDecoration.PaintProvider,
        HorizontalDividerItemDecoration.MarginProvider, FlexibleDividerDecoration.VisibilityProvider {
}

package com.bbbbiu.biu.gui.adapter.choose;

import android.support.v7.widget.RecyclerView;

import com.yqritc.recyclerviewflexibledivider.FlexibleDividerDecoration;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

/**
 * 底部滑出菜单的Adapter,要是不想给每项内容添加分割线，则三个实现项使用默认实现就行
 *
 * @see BaseOptionAdapter
 */
public abstract class BaseOptionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements FlexibleDividerDecoration.PaintProvider,
        HorizontalDividerItemDecoration.MarginProvider,
        FlexibleDividerDecoration.VisibilityProvider {
}

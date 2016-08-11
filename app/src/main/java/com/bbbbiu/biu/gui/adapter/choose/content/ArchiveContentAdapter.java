package com.bbbbiu.biu.gui.adapter.choose.content;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.db.search.FileItem;
import com.bbbbiu.biu.db.search.ModelItem;
import com.bbbbiu.biu.gui.adapter.util.OnViewTouchListener;
import com.bbbbiu.biu.gui.choose.BaseChooseActivity;
import com.bbbbiu.biu.util.SearchUtil;
import com.bbbbiu.biu.util.StorageUtil;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by YieldNull at 4/18/16
 */
public class ArchiveContentAdapter extends CommonSortedAdapter {
    private static final String TAG = ArchiveContentAdapter.class.getSimpleName();

    public ArchiveContentAdapter(BaseChooseActivity context) {
        super(context);
    }

    @Override
    public Comparator<ModelItem> getItemComparator() {
        return getDefaultItemComparator();
    }


    @Override
    protected Map<String, List<ModelItem>> readSortedDataFromDB() {
        return ModelItem.queryItemToDir(StorageUtil.TYPE_ARCHIVE);
    }

    @Override
    protected Map<String, List<ModelItem>> readSortedDataFromSys() {
        return ModelItem.sortItemWithDir(SearchUtil.scanArchiveItem(context));
    }

    @Override
    protected void updateDatabase() {
        SearchUtil.scanArchiveItem(context);
    }

    @Override
    public void cancelPicassoTask() {
    }

    @Override
    public RecyclerView.ViewHolder onCreateItemViewHolder(LayoutInflater inflater, ViewGroup parent) {
        return new ArchiveViewHolder(inflater.inflate(R.layout.list_archive_item, parent, false));
    }

    @Override
    public void onBindItemViewHolder(RecyclerView.ViewHolder hd, int position) {
        ArchiveViewHolder holder = (ArchiveViewHolder) hd;
        final FileItem item = (FileItem) getItemAt(position);

        holder.nameText.setText(item.getFile().getName());
        holder.infoText.setText(item.getSize());

        if (isItemChosen(position)) {
            holder.setItemStyleChosen();
        } else {
            holder.setItemStyleChoosing();
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setItemChosen(item);
            }
        });

        holder.optionImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notifyOptionToggleClicked(item.getFile());
            }
        });

        holder.optionImage.setOnTouchListener(new OnViewTouchListener(context));


        holder.itemView.setOnTouchListener(new OnViewTouchListener(context));
    }

    class ArchiveViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.imageView_icon)
        ImageView iconImg;

        @Bind(R.id.textView_name)
        TextView nameText;

        @Bind(R.id.textView_info)
        TextView infoText;

        @Bind(R.id.imageButton_option)
        ImageButton optionImage;

        public ArchiveViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }

        public void setItemStyleChosen() {
            itemView.setBackgroundColor(context.getResources().getColor(R.color.file_item_chosen));
            iconImg.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_file_chosen));
            iconImg.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.shape_file_icon_bkg_chosen));
        }

        public void setItemStyleChoosing() {
            itemView.setBackgroundColor(context.getResources().getColor(android.R.color.background_light));
            iconImg.setBackgroundDrawable(null);
            iconImg.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_type_archive));
        }
    }
}

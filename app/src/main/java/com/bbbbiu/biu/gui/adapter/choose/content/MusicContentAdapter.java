package com.bbbbiu.biu.gui.adapter.choose.content;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.db.search.MediaItem;
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
 * Created by fangdongliang on 16/3/26.
 * <p/>
 * Updated by YieldNull
 */
public class MusicContentAdapter extends CommonSortedAdapter {
    private static final String TAG = MusicContentAdapter.class.getSimpleName();


    public MusicContentAdapter(final BaseChooseActivity context) {
        super(context);
    }

    @Override
    public Comparator<ModelItem> getItemComparator() {
        return new Comparator<ModelItem>() {
            @Override
            public int compare(ModelItem lhs, ModelItem rhs) {
                MediaItem lMedia = (MediaItem) lhs;
                MediaItem rMedia = (MediaItem) rhs;

                return lMedia.title.compareTo(rMedia.title);
            }
        };
    }

    @Override
    protected Map<String, List<ModelItem>> readSortedDataFromDB() {
        return ModelItem.queryItemToDir(StorageUtil.TYPE_MUSIC);
    }

    @Override
    protected Map<String, List<ModelItem>> readSortedDataFromSys() {
        return ModelItem.sortItemWithDir(SearchUtil.scanMusicItem(context));
    }

    @Override
    protected void updateDatabase() {
        SearchUtil.scanMusicItem(context);
    }

    @Override
    public void cancelPicassoTask() {

    }

    @Override
    public RecyclerView.ViewHolder onCreateItemViewHolder(LayoutInflater inflater, ViewGroup parent) {
        return new MusicViewHolder(inflater.inflate(R.layout.list_music_item, parent, false));
    }

    @Override
    public void onBindItemViewHolder(RecyclerView.ViewHolder hd, int position) {
        MusicViewHolder holder = (MusicViewHolder) hd;

        final MediaItem item = (MediaItem) getItemAt(position);
        holder.title.setText(item.title);
        holder.singer.setText(item.artist);
        holder.size.setText(item.getSize());
        holder.duration.setText(item.duration);

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
        holder.itemView.setOnTouchListener(new OnViewTouchListener(context));
        holder.optionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notifyOptionToggleClicked(item.getFile());
            }
        });

        holder.optionButton.setOnTouchListener(new OnViewTouchListener(context));

    }

    public class MusicViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.textView_title)
        TextView title;

        @Bind(R.id.textView_artist)
        TextView singer;

        @Bind(R.id.textView_duration)
        TextView duration;

        @Bind(R.id.textView_size)
        TextView size;

        @Bind(R.id.imageView_icon)
        ImageView iconImage;

        @Bind(R.id.imageButton_option)
        ImageButton optionButton;

        public MusicViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }

        /**
         * 未选样式
         */
        public void setItemStyleChoosing() {
            itemView.setBackgroundColor(context.getResources().getColor(android.R.color.background_light));
            iconImage.setBackgroundDrawable(null);
            iconImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_type_music));
        }

        /**
         * 已选样式
         */
        public void setItemStyleChosen() {
            itemView.setBackgroundColor(context.getResources().getColor(R.color.file_item_chosen));
            iconImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_file_chosen));
            iconImage.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.shape_file_icon_bkg_chosen));
        }
    }

}
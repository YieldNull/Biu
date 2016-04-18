package com.bbbbiu.biu.gui.adapters.choose;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.choose.ChooseBaseActivity;
import com.bbbbiu.biu.util.db.MediaItem;
import com.bbbbiu.biu.util.db.ModelItem;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by fangdongliang on 16/3/26.
 * <p/>
 * Updated by YieldNull
 */
public class MusicContentAdapter extends ContentBaseAdapter {
    private static final String TAG = MusicContentAdapter.class.getSimpleName();

    private Context context;

    public MusicContentAdapter(final ChooseBaseActivity context) {
        super(context);
        this.context = context;

        if (!queryModelItems(ModelItem.TYPE_MUSIC)) {
        }
    }

    @Override
    public void cancelPicassoTask() {

    }

    @Override
    public RecyclerView.ViewHolder OnCreateItemViewHolder(LayoutInflater inflater, ViewGroup parent) {
        return new MusicViewHolder(inflater.inflate(R.layout.list_music_item, parent, false));
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder hd, final int position) {
        if (getItemViewType(position) == VIEW_TYPE_ITEM) {
            MusicViewHolder holder = (MusicViewHolder) hd;

            MediaItem item = (MediaItem) getItemAt(position);
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
                    setItemChosen(position);
                }
            });

        } else {
            HeaderViewHolder holder = (HeaderViewHolder) hd;
            holder.headerText.setText(getHeaderText(position));
        }

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
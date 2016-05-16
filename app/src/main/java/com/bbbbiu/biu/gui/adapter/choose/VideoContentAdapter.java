package com.bbbbiu.biu.gui.adapter.choose;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapter.util.HeaderViewHolder;
import com.bbbbiu.biu.gui.adapter.util.VideoIconRequestHandler;
import com.bbbbiu.biu.gui.choose.ChooseBaseActivity;
import com.bbbbiu.biu.util.SearchUtil;
import com.bbbbiu.biu.util.SizeUtil;
import com.bbbbiu.biu.db.search.MediaItem;
import com.bbbbiu.biu.db.search.ModelItem;
import com.squareup.picasso.Picasso;


import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by YieldNull at 4/18/16
 */
public class VideoContentAdapter extends ContentBaseAdapter {
    private static final String TAG = VideoContentAdapter.class.getSimpleName();

    private Context context;
    private Picasso mPicasso;

    private static final int THUMB_SIZE = (int) SizeUtil.convertDpToPixel(24);

    public VideoContentAdapter(ChooseBaseActivity context) {
        super(context);
        this.context = context;


        Picasso.Builder builder = new Picasso.Builder(context);
        builder.addRequestHandler(new VideoIconRequestHandler());
        mPicasso = builder.build();

    }


    @Override
    protected boolean readDataFromDB() {
        return queryModelItemFromDb(ModelItem.TYPE_VIDEO);
    }

    @Override
    protected boolean readDataFromSys() {
        return setDataSet(ModelItem.sortItemWithDir(SearchUtil.scanVideoItem(context)));
    }

    @Override
    protected void updateDatabase() {
        SearchUtil.scanVideoItem(context);
    }


    @Override
    public void cancelPicassoTask() {
        mPicasso.cancelTag(PICASSO_TAG);
    }


    @Override
    public RecyclerView.ViewHolder onCreateItemViewHolder(LayoutInflater inflater, ViewGroup parent) {
        return new VideoViewHolder(inflater.inflate(R.layout.list_video_item, parent, false));
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder hd, final int position) {
        if (getItemViewType(position) == VIEW_TYPE_ITEM) {
            VideoViewHolder holder = (VideoViewHolder) hd;

            MediaItem item = (MediaItem) getItemAt(position);

            holder.nameText.setText(item.getFile().getName());
            holder.infoText.setText(String.format("%s  %s", item.duration, item.getSize()));


            if (mChosenItems.contains(item)) {
                holder.setItemStyleChosen();
            } else {
                holder.setItemStyleChoosing();

                mPicasso.load(VideoIconRequestHandler.PICASSO_SCHEME_VIDEO + ":" + item.path)
                        .resize(THUMB_SIZE, THUMB_SIZE)
                        .tag(PICASSO_TAG)
                        .placeholder(R.drawable.ic_type_video)
                        .onlyScaleDown()
                        .into(holder.iconImg);
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

    class VideoViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.imageView_icon)
        ImageView iconImg;

        @Bind(R.id.textView_name)
        TextView nameText;

        @Bind(R.id.textView_info)
        TextView infoText;

        @Bind(R.id.imageButton_option)
        ImageButton optionButton;

        public VideoViewHolder(View itemView) {
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
        }
    }
}

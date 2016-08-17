package com.bbbbiu.biu.gui.adapter.choose.content;

import android.content.Context;
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
import com.bbbbiu.biu.gui.adapter.util.VideoIconRequestHandler;
import com.bbbbiu.biu.gui.choose.BaseChooseActivity;
import com.bbbbiu.biu.util.SearchUtil;
import com.bbbbiu.biu.util.SizeUtil;
import com.bbbbiu.biu.util.StorageUtil;
import com.squareup.picasso.Picasso;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * 选视频
 * <p/>
 * Created by YieldNull at 4/18/16
 */
public class VideoContentAdapter extends CommonSortedAdapter {
    private static final String TAG = VideoContentAdapter.class.getSimpleName();

    private Context context;
    private Picasso mPicasso;

    /**
     * 缩略图大小，根据layout决定的
     */
    private static final int THUMB_SIZE = (int) SizeUtil.convertDpToPixel(24);

    public VideoContentAdapter(BaseChooseActivity context) {
        super(context);
        this.context = context;


        Picasso.Builder builder = new Picasso.Builder(context);
        builder.addRequestHandler(new VideoIconRequestHandler());
        mPicasso = builder.build();

    }

    @Override
    public Comparator<ModelItem> getItemComparator() {
        return getDefaultItemComparator();
    }

    @Override
    protected Map<String, List<ModelItem>> readSortedDataFromDB() {
        return ModelItem.queryItemToDir(StorageUtil.TYPE_VIDEO);
    }

    @Override
    protected Map<String, List<ModelItem>> readSortedDataFromSys() {
        return ModelItem.sortItemWithDir(SearchUtil.scanVideoItem(context));
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
    public void onBindItemViewHolder(RecyclerView.ViewHolder hd, int position) {
        VideoViewHolder holder = (VideoViewHolder) hd;

        final MediaItem item = (MediaItem) getItemAt(position);

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
                setItemChosen(item);
            }
        });
        holder.itemView.setOnTouchListener(new OnViewTouchListener(context));
        holder.optionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StorageUtil.openFile(context, item.getFile());
            }
        });

        holder.optionButton.setOnTouchListener(new OnViewTouchListener(context));
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

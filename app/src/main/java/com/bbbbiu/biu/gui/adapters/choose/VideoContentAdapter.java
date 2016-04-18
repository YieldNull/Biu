package com.bbbbiu.biu.gui.adapters.choose;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.choose.ChooseBaseActivity;
import com.bbbbiu.biu.util.SizeUtil;
import com.bbbbiu.biu.util.db.MediaItem;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by YieldNull at 4/18/16
 */
public class VideoContentAdapter extends ContentBaseAdapter {
    private static final String TAG = VideoContentAdapter.class.getSimpleName();

    private List<MediaItem> mVideoList = new ArrayList<>();
    private List<MediaItem> mChooseVideo = new ArrayList<>();
    private Context context;

    private Picasso mPicasso;
    private static final String PICASSO_TAG = "tag-img";
    private static final String PICASSO_SCHEME_VIDEO = "video-icon";

    private static final int THUMB_SIZE = (int) SizeUtil.convertDpToPixel(24);

    public VideoContentAdapter(ChooseBaseActivity context) {
        super(context);
        this.context = context;


        Picasso.Builder builder = new Picasso.Builder(context);
        builder.addRequestHandler(new VideoIconRequestHandler());
        mPicasso = builder.build();


        if (!readVideoItems()) {

        }
    }


    private boolean readVideoItems() {
        Map<String, List<MediaItem>> mDirFileMap = MediaItem.loadMediaItems(MediaItem.TYPE_VIDEO);

        if (mDirFileMap.size() == 0) {
            return false;
        }

        for (Map.Entry<String, List<MediaItem>> entry : mDirFileMap.entrySet()) {
            List<MediaItem> list = entry.getValue();
            if (list.size() > 0) {
                mVideoList.add(null);
                mVideoList.addAll(list);
            }
        }

        return true;
    }


    @Override
    public void cancelPicassoTask() {
        mPicasso.cancelTag(PICASSO_TAG);
    }

    @Override
    public Set<String> getChosenFiles() {
        Set<String> set = new HashSet<>();
        for (MediaItem item : mChooseVideo) {
            set.add(item.getPath());
        }
        return set;
    }

    @Override
    public int getChosenCount() {
        return mChooseVideo.size();
    }

    @Override
    public void setFileAllChosen() {
        mChooseVideo.clear();

        for (MediaItem item : mVideoList) {
            if (item != null) {
                mChooseVideo.add(item);
            }
        }

        notifyDataSetChanged();
    }

    @Override
    public void setFileAllDismissed() {
        mChooseVideo.clear();
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView;

        if (viewType == VIEW_TYPE_HEADER) {
            itemView = inflater.inflate(R.layout.list_header_common, parent, false);
            return new HeaderViewHolder(itemView);
        } else {
            itemView = inflater.inflate(R.layout.list_video_item, parent, false);
            return new VideoViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder hd, final int position) {
        if (getItemViewType(position) == VIEW_TYPE_ITEM) {
            VideoViewHolder holder = (VideoViewHolder) hd;

            MediaItem item = getItemAt(position);

            holder.nameText.setText(item.getFile().getName());
            holder.infoText.setText(String.format("%s  %s", item.duration, item.getSize()));


            if (mChooseVideo.contains(item)) {
                holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.file_item_chosen));
                holder.iconImg.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_file_chosen));
                holder.iconImg.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.shape_file_icon_bkg_chosen));
            } else {
                holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.background_light));
                holder.iconImg.setBackgroundDrawable(null);

                mPicasso.load(PICASSO_SCHEME_VIDEO + ":" + item.path)
                        .resize(THUMB_SIZE, THUMB_SIZE)
                        .tag(PICASSO_TAG)
                        .placeholder(R.drawable.ic_type_video)
                        .onlyScaleDown()
                        .into(holder.iconImg);
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setFileChosen(position);
                }
            });
        } else {
            HeaderViewHolder holder = (HeaderViewHolder) hd;
            holder.headerText.setText(getItemAt(position + 1).getParentDirName());
        }
    }

    @Override
    public int getItemCount() {
        return mVideoList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return getItemAt(position) == null ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }

    private MediaItem getItemAt(int position) {
        return mVideoList.get(position);
    }

    private void setFileChosen(int position) {
        MediaItem item = getItemAt(position);
        if (!mChooseVideo.contains(item)) {
            mChooseVideo.add(item);
            notifyFileChosen(item.path);
        } else {
            mChooseVideo.remove(item);
            notifyFileDismissed(item.path);
        }
        notifyDataSetChanged();
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
    }

    /**
     * 加载视频缩略图
     */
    class VideoIconRequestHandler extends RequestHandler {

        @Override
        public boolean canHandleRequest(Request data) {
            return PICASSO_SCHEME_VIDEO.equals(data.uri.getScheme());
        }

        @Override
        public Result load(Request request, int networkPolicy) throws IOException {
            String path = request.uri.toString().replace(PICASSO_SCHEME_VIDEO + ":", "");
            Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(path,
                    MediaStore.Video.Thumbnails.MICRO_KIND);

            return new Result(bitmap, Picasso.LoadedFrom.DISK);
        }
    }
}

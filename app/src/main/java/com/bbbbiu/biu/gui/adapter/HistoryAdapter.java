package com.bbbbiu.biu.gui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.db.transfer.RevRecord;
import com.bbbbiu.biu.gui.adapter.util.VideoIconRequestHandler;
import com.bbbbiu.biu.util.StorageUtil;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by YieldNull at 5/9/16
 */
public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = HistoryAdapter.class.getSimpleName();
    private Context context;

    private List<RevRecord> mDataSet = new ArrayList<>();

    private Picasso mVideoPicasso;
    private Picasso mImgPicasso;

    public HistoryAdapter(Context context) {
        this.context = context;


        Picasso.Builder builder = new Picasso.Builder(context);
        builder.addRequestHandler(new VideoIconRequestHandler());
        mVideoPicasso = builder.build();
        mImgPicasso = Picasso.with(context);

        mDataSet.addAll(RevRecord.listAll(RevRecord.class));

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(R.layout.list_transfer_finished, parent, false);
        return new HistoryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder hd, int position) {
        RevRecord revRecord = mDataSet.get(position);
        File revFile = revRecord.getFile(context);

        HistoryViewHolder holder = (HistoryViewHolder) hd;

        if (StorageUtil.isVideoFile(revFile.getPath())) {
            mVideoPicasso.load(VideoIconRequestHandler.PICASSO_SCHEME_VIDEO + ":" + revFile.getAbsolutePath())
                    .resize(VideoIconRequestHandler.THUMB_SIZE, VideoIconRequestHandler.THUMB_SIZE)
                    .placeholder(R.drawable.ic_type_video)
                    .into(holder.iconImage);

        } else if (StorageUtil.isImgFile(revFile.getPath())) {
            mImgPicasso.load(revFile)
                    .resize(VideoIconRequestHandler.THUMB_SIZE, VideoIconRequestHandler.THUMB_SIZE)
                    .placeholder(R.drawable.ic_type_img)
                    .into(holder.iconImage);
        } else {
            holder.iconImage.setImageDrawable(StorageUtil.getFileIcon(context, revRecord.getFile(context)));
        }

        holder.nameText.setText(revRecord.name);

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        holder.infoText.setText(String.format("%s %s",
                format.format(new Date(revRecord.timestamp)),
                StorageUtil.getReadableSize(revRecord.size)));
    }

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }


    class HistoryViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.imageView_icon)
        ImageView iconImage;

        @Bind(R.id.textView_name)
        TextView nameText;

        @Bind(R.id.textView_info)
        TextView infoText;

        @Bind(R.id.imageView_option)
        ImageView optionImage;

        public HistoryViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }
    }

}

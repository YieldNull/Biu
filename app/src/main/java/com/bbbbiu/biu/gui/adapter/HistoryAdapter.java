package com.bbbbiu.biu.gui.adapter;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.db.TransferRecord;
import com.bbbbiu.biu.gui.HistoryActivity;
import com.bbbbiu.biu.gui.adapter.util.OnViewTouchListener;
import com.bbbbiu.biu.gui.adapter.util.VideoIconRequestHandler;
import com.bbbbiu.biu.gui.choose.listener.FileChooser;
import com.bbbbiu.biu.gui.choose.listener.OnChoosingListener;
import com.bbbbiu.biu.util.StorageUtil;
import com.raizlabs.android.dbflow.list.FlowQueryList;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by YieldNull at 5/9/16
 */
public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements FileChooser {
    private static final String TAG = HistoryAdapter.class.getSimpleName();

    private static final String PICASSO_TAG = "tag-img";

    private Activity context;
    private HistoryActivity.PlaceholderFragment mFragment;
    private OnChoosingListener mOnChoosingListener;


    private FlowQueryList<TransferRecord> mDataSet;

    private Picasso mVideoPicasso;
    private Picasso mImgPicasso;

    private Set<TransferRecord> mChosenFiles = new HashSet<>();

    private boolean mOnChoosing;

    public HistoryAdapter(final Activity context, HistoryActivity.PlaceholderFragment placeholderFragment, final boolean showReceived) {
        this.context = context;
        this.mOnChoosingListener = (OnChoosingListener) context;
        this.mFragment = placeholderFragment;

        Picasso.Builder builder = new Picasso.Builder(context);
        builder.addRequestHandler(new VideoIconRequestHandler());
        mVideoPicasso = builder.build();
        mImgPicasso = Picasso.with(context);

        int type = showReceived ? TransferRecord.TYPE_RECEIVED : TransferRecord.TYPE_SENT;
        mDataSet = TransferRecord.query(type);
    }


    public void setOnChoosing() {
        mOnChoosing = true;
        notifyDataSetChanged();
    }

    public boolean isOnChoosing() {
        return mOnChoosing;
    }

    @Override
    public int getChosenCount() {
        return mChosenFiles.size();
    }

    @Override
    public Set<String> getChosenFiles() {
        Set<String> files = new HashSet<>();
        for (TransferRecord record : mChosenFiles) {
            files.add(record.uri);
        }
        return files;
    }


    @Override
    public boolean isFileChosen(File file) {
        // unused
        return false;
    }

    @Override
    public void setFileAllChosen() {
        mChosenFiles.addAll(mDataSet);
        notifyDataSetChanged();

        Log.i(TAG, "All file chosen");
    }

    @Override
    public void setFileAllDismissed() {
        mOnChoosing = false;
        mChosenFiles.clear();
        notifyDataSetChanged();

        Log.i(TAG, "All file dismissed");
    }


    /**
     * 获取已选TransferRecord对象
     *
     * @return 已选对象
     */
    public Set<TransferRecord> getChosenRecords() {
        return mChosenFiles;
    }


    /**
     * 在非UI线程中删除
     */
    public void deleteChosenFiles() {
        List<TransferRecord> toDelete = new ArrayList<>();

        boolean allSucceeded = true;
        for (TransferRecord record : mChosenFiles) {
            if (record.deleteFile(context)) {
                mDataSet.remove(record);
                toDelete.add(record);

                Log.i(TAG, "Delete file and record successfully: " + record.uri);
            } else {
                allSucceeded = false;

                Log.i(TAG, "Failed to delete file and record: " + record.uri);
            }
        }

        mChosenFiles.removeAll(toDelete);
        final int stringId = allSucceeded
                ? R.string.hint_file_delete_succeeded
                : R.string.hint_file_delete_partly_succeeded;

        mDataSet.refresh();

        mOnChoosing = mChosenFiles.size() != 0;


        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
                Toast.makeText(context, stringId, Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * 点击list条目，默认为待选状态
     * <p/>
     * 要是已选择，则取消选择，要是未选择，则选择之
     *
     * @param record 对应的传输纪录
     */
    private void itemClicked(TransferRecord record) {
        if (!mChosenFiles.contains(record)) {
            mChosenFiles.add(record);
            mOnChoosingListener.onFileChosen(record.uri);

        } else {
            mChosenFiles.remove(record);

            mOnChoosingListener.onFileDismissed(record.uri);
        }

        mOnChoosing = mChosenFiles.size() != 0;
        notifyDataSetChanged();

    }


    @Override
    public int getItemCount() {
        return mDataSet.size();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(R.layout.list_history_item, parent, false);
        return new HistoryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder hd, int position) {
        final TransferRecord record = mDataSet.get(position);

        final HistoryViewHolder holder = (HistoryViewHolder) hd;

        if (!record.fileExists(context)) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    mDataSet.remove(record);
                    mDataSet.refresh();
                    notifyItemRemoved(hd.getAdapterPosition());
                }
            });
        }


        holder.nameText.setText(record.name);

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
        holder.infoText.setText(String.format("%s %s",
                format.format(new Date(record.timestamp)),
                StorageUtil.getReadableSize(record.size)));


        if (mChosenFiles.contains(record)) {
            holder.setItemStyleChosen();
        } else if (mOnChoosing) {
            holder.setItemStyleChoosing(record);
        } else {
            holder.setItemStyleNormal(record);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isOnChoosing()) {
                    itemClicked(record);
                } else {
                    Log.i(TAG, "Open file: " + record.uri);
                    StorageUtil.openFile(context, record.getUri(), record.name);
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                itemClicked(record);
                return true;
            }
        });

        holder.itemView.setOnTouchListener(new OnViewTouchListener(context));

        holder.optionToggleImage.setOnTouchListener(new OnViewTouchListener(context));

        holder.optionToggleImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(context)
                        .setMessage(context.getString(R.string.hint_file_delete_confirm))
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if (record.deleteFile(context)) {

                                    mDataSet.remove(record);
                                    mDataSet.refresh();


                                    if (mChosenFiles.contains(record)) {
                                        mChosenFiles.remove(record);

                                        mOnChoosing = mChosenFiles.size() != 0;
                                        mOnChoosingListener.onFileDismissed(record.uri);
                                    }

                                    notifyItemRemoved(hd.getAdapterPosition());

                                    mFragment.checkDataSetAmount();

                                    Log.i(TAG, "Delete file and record successfully: " + record.uri);
                                    Toast.makeText(context, R.string.hint_file_delete_succeeded, Toast.LENGTH_SHORT).show();
                                } else {

                                    Log.i(TAG, "Failed to delete file and record: " + record.uri);

                                    Toast.makeText(context, R.string.hint_file_delete_failed, Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(context, R.string.hint_file_delete_dismissed, Toast.LENGTH_SHORT).show();
                            }
                        }).show();
            }
        });

    }


    class HistoryViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.imageView_icon)
        ImageView iconImage;

        @Bind(R.id.textView_name)
        TextView nameText;

        @Bind(R.id.textView_info)
        TextView infoText;

        @Bind(R.id.imageButton_option)
        ImageButton optionToggleImage;


        public HistoryViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }


        /**
         * 正常显示样式
         *
         * @param record 纪录
         */
        public void setItemStyleNormal(TransferRecord record) {
            itemView.setBackgroundColor(context.getResources().getColor(android.R.color.background_light));
            iconImage.setBackgroundDrawable(null);

            if (record.getFileType() == StorageUtil.TYPE_VIDEO) {
                String realPath = record.getFilePath();

                if (realPath == null) {
                    iconImage.setImageDrawable(StorageUtil.getFileIcon(context, StorageUtil.TYPE_VIDEO));
                } else {
                    mVideoPicasso.load(VideoIconRequestHandler.PICASSO_SCHEME_VIDEO + ":" + realPath)
                            .resize(VideoIconRequestHandler.THUMB_SIZE, VideoIconRequestHandler.THUMB_SIZE)
                            .placeholder(R.drawable.ic_type_video)
                            .tag(PICASSO_TAG)
                            .into(iconImage);
                }

            } else if (record.getFileType() == StorageUtil.TYPE_IMG) {
                mImgPicasso.load(record.getUri())
                        .resize(VideoIconRequestHandler.THUMB_SIZE, VideoIconRequestHandler.THUMB_SIZE)
                        .placeholder(R.drawable.ic_type_img)
                        .tag(PICASSO_TAG)
                        .into(iconImage);
            } else if (record.getFileType() == StorageUtil.TYPE_APK) {
                Drawable drawable = StorageUtil.getApkIcon(context, record.getFilePath());
                if (drawable == null) {
                    drawable = StorageUtil.getFileIcon(context, StorageUtil.TYPE_APK);
                }
                iconImage.setImageDrawable(drawable);
            } else {
                iconImage.setImageDrawable(StorageUtil.getFileIcon(context, record.getFileType()));
            }
        }

        /**
         * 待选样式
         *
         * @param record 纪录
         */
        public void setItemStyleChoosing(TransferRecord record) {
            setItemStyleNormal(record);
            iconImage.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.shape_file_icon_bkg));
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

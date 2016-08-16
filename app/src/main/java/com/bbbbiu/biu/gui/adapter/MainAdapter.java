package com.bbbbiu.biu.gui.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.db.TransferRecord;
import com.bbbbiu.biu.gui.adapter.util.HeaderViewHolder;
import com.bbbbiu.biu.gui.choose.ApkChooseActivity;
import com.bbbbiu.biu.gui.choose.ArchiveChooseActivity;
import com.bbbbiu.biu.gui.choose.BaseChooseActivity;
import com.bbbbiu.biu.gui.choose.DocumentChooseActivity;
import com.bbbbiu.biu.gui.choose.FileChooseActivity;
import com.bbbbiu.biu.gui.choose.ImgChooseActivity;
import com.bbbbiu.biu.gui.choose.MusicChooseActivity;
import com.bbbbiu.biu.gui.choose.VideoChooseActivity;
import com.bbbbiu.biu.util.StorageUtil;
import com.raizlabs.android.dbflow.list.FlowQueryList;

import java.io.File;
import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int SPAN_COUNT = 2;

    private static final int TYPE_STORAGE = 0;
    private static final int TYPE_CATEGORY = 1;
    private static final int TYPE_HISTORY_HEADER = 2;
    private static final int TYPE_HISTORY_ITEM = 3;

    private ArrayList<Integer[]> nameImgMap;
    private Context context;

    private boolean hasExternal;
    private int downloadsStart;
    private FlowQueryList<TransferRecord> recentDownloads;

    public MainAdapter(Context context) {
        this.context = context;
        nameImgMap = new ArrayList<>();

        nameImgMap.add(new Integer[]{R.string.cate_image, R.drawable.ic_cate_image});
        nameImgMap.add(new Integer[]{R.string.cate_music, R.drawable.ic_cate_music});
        nameImgMap.add(new Integer[]{R.string.cate_video, R.drawable.ic_cate_video});
        nameImgMap.add(new Integer[]{R.string.cate_document, R.drawable.ic_cate_document});
        nameImgMap.add(new Integer[]{R.string.cate_archive, R.drawable.ic_cate_archive});
        nameImgMap.add(new Integer[]{R.string.cate_apk, R.drawable.ic_cate_apk});
        nameImgMap.add(new Integer[]{R.string.cate_storage, R.drawable.ic_cate_phone});


        if (StorageUtil.hasRealExternal(context)) {
            nameImgMap.add(new Integer[]{R.string.cate_sdcard, R.drawable.ic_cate_sdcard});
            hasExternal = true;
        }
    }

    public int getSpanSize(int position) {
        return position < 6 ? 1 : 2;
    }


    public void refreshRecentDownloads() {
        recentDownloads = TransferRecord.query(TransferRecord.TYPE_RECEIVED, 3);

        downloadsStart = hasExternal ? 9 : 8;

        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        if (viewType == TYPE_CATEGORY) {
            View itemView = inflater.inflate(R.layout.list_main_item_category, parent, false);
            return new CategoryHolder(itemView);
        } else if (viewType == TYPE_STORAGE) {
            View itemView = inflater.inflate(R.layout.list_main_item_storage, parent, false);
            return new StorageHolder(itemView);
        } else if (viewType == TYPE_HISTORY_HEADER) {
            return HeaderViewHolder.build(inflater, parent);
        } else {
            View itemView = inflater.inflate(R.layout.list_recent_download, parent, false);
            return new RecentDownloadsHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder hd, int position) {
        int viewType = getItemViewType(position);

        if (viewType == TYPE_CATEGORY) {
            final int stringId = getStringId(position);
            int imageId = nameImgMap.get(position)[1];
            final CategoryHolder holder = (CategoryHolder) hd;

            holder.iconImage.setImageDrawable(context.getResources().getDrawable(imageId));
            holder.nameText.setText(context.getString(stringId));

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Class<? extends BaseChooseActivity> theClass = null;
                    switch (getStringId(holder.getAdapterPosition())) {
                        case R.string.cate_image:
                            theClass = ImgChooseActivity.class;
                            break;
                        case R.string.cate_music:
                            theClass = MusicChooseActivity.class;
                            break;
                        case R.string.cate_video:
                            theClass = VideoChooseActivity.class;
                            break;
                        case R.string.cate_document:
                            theClass = DocumentChooseActivity.class;
                            break;
                        case R.string.cate_archive:
                            theClass = ArchiveChooseActivity.class;
                            break;
                        case R.string.cate_apk:
                            theClass = ApkChooseActivity.class;
                            break;
                        default:
                            break;
                    }
                    if (theClass != null) {
                        BaseChooseActivity.startChoosing(context, theClass);
                    }
                }
            });
        } else if (viewType == TYPE_STORAGE) {
            int stringId = getStringId(position);
            int imageId = nameImgMap.get(position)[1];

            StorageHolder holder = (StorageHolder) hd;
            holder.iconImage.setImageDrawable(context.getResources().getDrawable(imageId));
            holder.nameText.setText(context.getString(stringId));
            holder.setPercentage(position);
        } else if (viewType == TYPE_HISTORY_HEADER) {
            HeaderViewHolder holder = (HeaderViewHolder) hd;
            holder.itemView.setBackgroundDrawable(null);
            holder.headerText.setText(R.string.cate_recent_downloads);
        } else {
            RecentDownloadsHolder holder = (RecentDownloadsHolder) hd;
            final TransferRecord record = recentDownloads.get(position - downloadsStart);

            holder.iconImage.setImageDrawable(StorageUtil.getFileIcon(context, record.getFileType()));

            holder.nameText.setText(record.name);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    StorageUtil.openFile(context, record.getUri(), record.name);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        if (recentDownloads == null || recentDownloads.size() == 0) {
            return nameImgMap.size();
        } else {
            return nameImgMap.size() + 1 + recentDownloads.size();
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position < 6) {
            return TYPE_CATEGORY;
        } else if (position == 6 || (hasExternal && position == 7)) {
            return TYPE_STORAGE;
        } else if (position == downloadsStart - 1) {
            return TYPE_HISTORY_HEADER;
        } else {
            return TYPE_HISTORY_ITEM;
        }
    }

    private int getStringId(int position) {
        return nameImgMap.get(position)[0];
    }

    class CategoryHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.imageView_icon)
        ImageView iconImage;

        @Bind(R.id.textView_name)
        TextView nameText;

        CategoryHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    class StorageHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.imageView_icon)
        ImageView iconImage;

        @Bind(R.id.textView_name)
        TextView nameText;

        @Bind(R.id.progressBar)
        ProgressBar progressBar;

        @Bind(R.id.textView_storage)
        TextView storageText;

        StorageHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void setPercentage(int position) {
            int stringId = getStringId(position);
            int type = stringId == R.string.cate_storage ? StorageUtil.STORAGE_INTERNAL : StorageUtil.STORAGE_EXTERNAL;
            final File file = StorageUtil.getRootDir(context, type);

            setStoragePercentage(file);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, FileChooseActivity.class);
                    intent.putExtra(FileChooseActivity.EXTRA_ROOT_FILE_PATH, file.getAbsolutePath());
                    context.startActivity(intent);
                }
            });
        }

        private void setStoragePercentage(File file) {
            long total = file.getTotalSpace();
            long free = file.getFreeSpace();

            progressBar.setMax(100);
            progressBar.setProgress((int) (((total - free) / (total * 1.0)) * 100));

            String percentage = StorageUtil.getReadableSize(total - free) + "/" + StorageUtil.getReadableSize(total);
            storageText.setText(percentage);
        }
    }

    class RecentDownloadsHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.imageView_icon)
        ImageView iconImage;

        @Bind(R.id.textView_name)
        TextView nameText;

        public RecentDownloadsHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }
    }
}

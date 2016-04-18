package com.bbbbiu.biu.gui.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.choose.ApkChooseActivity;
import com.bbbbiu.biu.gui.choose.ArchiveChooseActivity;
import com.bbbbiu.biu.gui.choose.DocumentChooseActivity;
import com.bbbbiu.biu.gui.choose.FileChooseActivity;
import com.bbbbiu.biu.gui.choose.ImgChooseActivity;
import com.bbbbiu.biu.gui.choose.MusicChooseActivity;
import com.bbbbiu.biu.gui.choose.VideoChooseActivity;
import com.bbbbiu.biu.util.StorageUtil;

import java.io.File;
import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int SPAN_COUNT = 2;

    private static final int TYPE_STORAGE = 0;
    private static final int TYPE_CATEGORY = 1;
    private static final String TAG = MainAdapter.class.getSimpleName();

    private ArrayList<Integer[]> nameImgMap;
    private Context context;


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
        }
    }

    public int getSpanSize(int position) {
        int stringId = getStringId(position);
        return stringId == R.string.cate_sdcard || stringId == R.string.cate_storage ? 2 : 1;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        if (viewType == TYPE_CATEGORY) {
            View itemView = inflater.inflate(R.layout.list_main_item_category, parent, false);
            return new CategoryHolder(itemView);
        } else {
            View itemView = inflater.inflate(R.layout.list_main_item_storage, parent, false);
            return new StorageHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder hd, final int position) {
        final int stringId = getStringId(position);
        int imageId = nameImgMap.get(position)[1];

        if (getItemViewType(position) == TYPE_CATEGORY) {
            CategoryHolder holder = (CategoryHolder) hd;
            holder.iconImage.setImageDrawable(context.getResources().getDrawable(imageId));
            holder.nameText.setText(context.getString(stringId));

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Class theClass = null;
                    switch (getStringId(position)) {
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
                        context.startActivity(new Intent(context, theClass));
                    }
                }
            });
        } else {
            StorageHolder holder = (StorageHolder) hd;
            holder.iconImage.setImageDrawable(context.getResources().getDrawable(imageId));
            holder.nameText.setText(context.getString(stringId));
            holder.setPercentage(position);
        }
    }

    @Override
    public int getItemCount() {
        return nameImgMap.size();
    }

    @Override
    public int getItemViewType(int position) {
        int stringId = getStringId(position);
        if (stringId == R.string.cate_storage || stringId == R.string.cate_sdcard) {
            return TYPE_STORAGE;
        } else {
            return TYPE_CATEGORY;
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
            int type = stringId == R.string.cate_storage ? StorageUtil.TYPE_INTERNAL : StorageUtil.TYPE_EXTERNAL;
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

}

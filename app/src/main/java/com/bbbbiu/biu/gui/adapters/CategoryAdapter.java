package com.bbbbiu.biu.gui.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.FileChooseActivity;

import java.util.ArrayList;

public class CategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int SPAN_COUNT = 2;

    private static final int TYPE_STORAGE = 0;
    private static final int TYPE_CATEGORY = 1;

    private ArrayList<Integer[]> nameImgMap;
    private Context context;
    private int spanSize;

    public CategoryAdapter(Context context) {
        this.context = context;
        nameImgMap = new ArrayList<>();

        nameImgMap.add(new Integer[]{R.string.cate_image, R.drawable.ic_cate_image});
        nameImgMap.add(new Integer[]{R.string.cate_music, R.drawable.ic_cate_music});
        nameImgMap.add(new Integer[]{R.string.cate_video, R.drawable.ic_cate_video});
        nameImgMap.add(new Integer[]{R.string.cate_document, R.drawable.ic_cate_document});
        nameImgMap.add(new Integer[]{R.string.cate_archive, R.drawable.ic_cate_archive});
        nameImgMap.add(new Integer[]{R.string.cate_apk, R.drawable.ic_cate_apk});
        nameImgMap.add(new Integer[]{R.string.cate_download, R.drawable.ic_cate_download});
        nameImgMap.add(new Integer[]{R.string.cate_trash, R.drawable.ic_cate_trash});
        nameImgMap.add(new Integer[]{R.string.cate_storage, R.drawable.ic_cate_phone});
        nameImgMap.add(new Integer[]{R.string.cate_sdcard, R.drawable.ic_cate_sdcard});
    }

    public int getSpanSize(int position) {
        int stringId = nameImgMap.get(position)[0];
        return stringId == R.string.cate_sdcard || stringId == R.string.cate_storage ? 2 : 1;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        if (viewType == TYPE_STORAGE) {
            View itemView = inflater.inflate(R.layout.list_main_item_category, parent, false);
            return new CategoryHolder(itemView);
        } else {
            View itemView = inflater.inflate(R.layout.list_main_item_storage, parent, false);
            return new StorageHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder hd, int position) {
        int stringId = nameImgMap.get(position)[0];
        int imageId = nameImgMap.get(position)[1];

        if (getItemViewType(position) == TYPE_STORAGE) {
            CategoryHolder holder = (CategoryHolder) hd;
            holder.cateImage.setImageDrawable(context.getResources().getDrawable(imageId));
            holder.cateText.setText(context.getString(stringId));
        } else {
            StorageHolder holder = (StorageHolder) hd;
            holder.cateImage.setImageDrawable(context.getResources().getDrawable(imageId));
            holder.cateText.setText(context.getString(stringId));
        }
    }

    @Override
    public int getItemCount() {
        return nameImgMap.size();
    }

    @Override
    public int getItemViewType(int position) {
        int stringId = nameImgMap.get(position)[0];
        if (stringId == R.string.cate_storage || stringId == R.string.cate_sdcard) {
            return TYPE_CATEGORY;
        } else {
            return TYPE_STORAGE;
        }
    }

    private class CategoryHolder extends RecyclerView.ViewHolder {
        public ImageView cateImage;
        public TextView cateText;

        public CategoryHolder(View itemView) {
            super(itemView);

            cateImage = (ImageView) itemView.findViewById(R.id.imageView_cate);
            cateText = (TextView) itemView.findViewById(R.id.textView_cate);
        }
    }

    private class StorageHolder extends RecyclerView.ViewHolder {
        public ImageView cateImage;
        public TextView cateText;

        public StorageHolder(View itemView) {
            super(itemView);

            cateImage = (ImageView) itemView.findViewById(R.id.imageView_cate);
            cateText = (TextView) itemView.findViewById(R.id.textView_cate);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    context.startActivity(new Intent(context, FileChooseActivity.class));
                }
            });
        }
    }

}

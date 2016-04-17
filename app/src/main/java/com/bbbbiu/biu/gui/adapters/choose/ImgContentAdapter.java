package com.bbbbiu.biu.gui.adapters.choose;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.choose.ChooseBaseActivity;
import com.bbbbiu.biu.util.SizeUtil;
import com.bbbbiu.biu.util.StorageUtil;
import com.bbbbiu.biu.util.db.FileItem;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by YieldNull at 4/17/16
 */
public class ImgContentAdapter extends ContentBaseAdapter {
    private static final String TAG = ImgContentAdapter.class.getSimpleName();
    private Context
            context;

    private List<FileItem> mImgList = new ArrayList<>();
    private List<FileItem> mChosenImg = new ArrayList<>();

    private Map<String, List<FileItem>> mDirFileMap = new HashMap<>();

    private Picasso mPicasso;
    private static final String PICASSO_TAG = "tag-img";
    private int mImgWidth;
    private Drawable mPlaceholder;

    public ImgContentAdapter(ChooseBaseActivity context) {
        super(context);

        this.context = context;
        mPicasso = Picasso.with(context);


        float screenWidth = SizeUtil.getScreenWidth(context);
        float dpMargin = SizeUtil.convertDpToPixel(4);
        mImgWidth = (int) (screenWidth - dpMargin * 2) / 3;

        Drawable drawable = context.getResources().getDrawable(R.drawable.img_placeholder);
        mPlaceholder = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(mPlaceholder, context.getResources().getColor(R.color.img_placeholder));

        if (!readImgList()) {
            // TODO 空空空啊啊啊啊啊
        }
    }

    private boolean readImgList() {
        return FileItem.loadFile(FileItem.TYPE_IMG, mImgList, mDirFileMap);
    }

    private FileItem getItemAt(int postion) {
        return mImgList.get(postion);
    }

    private void setFileChosen(int position) {
        FileItem item = getItemAt(position);

        if (mChosenImg.contains(item)) {
            mChosenImg.remove(item);
            notifyFileDismissed(item.path);
        } else {
            mChosenImg.add(item);
            notifyFileChosen(item.path);
        }
        notifyDataSetChanged();
    }

    @Override
    public void cancelPicassoTask() {
        mPicasso.cancelTag(PICASSO_TAG);
    }

    @Override
    public Set<String> getChosenFiles() {
        Set<String> set = new HashSet<>();

        for (FileItem item : mChosenImg) {
            set.add(item.path);
        }
        return set;
    }

    @Override
    public int getChosenCount() {
        return mChosenImg.size();
    }

    @Override
    public void setFileAllChosen() {
        mChosenImg.clear();
        for (FileItem fileItem : mImgList) {
            if (fileItem != null) {
                mChosenImg.add(fileItem);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public void setFileAllDismissed() {
        mChosenImg.clear();
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView;

        if (viewType == VIEW_TYPE_HEADER) {
            itemView = inflater.inflate(R.layout.list_header_cate, parent, false);
            return new HeaderViewHolder(itemView);
        } else {
            itemView = inflater.inflate(R.layout.list_img_item, parent, false);
            return new ImgViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder hd, final int position) {
        if (getItemViewType(position) == VIEW_TYPE_ITEM) {
            ImgViewHolder holder = (ImgViewHolder) hd;


            mPicasso.load(getItemAt(position).getFile())
                    .tag(PICASSO_TAG)
                    .resize(mImgWidth, mImgWidth)
                    .placeholder(mPlaceholder)
                    .onlyScaleDown()
                    .centerCrop()
                    .into(holder.imageView);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setFileChosen(position);
                }
            });


            if (mChosenImg.contains(getItemAt(position))) {
                holder.chosenImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_chosen_check));
            } else if (getChosenCount() == 0) {
                holder.chosenImage.setImageDrawable(null);
            } else {
                holder.chosenImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_chossing_circle));
            }

        } else {
            HeaderViewHolder holder = (HeaderViewHolder) hd;

            holder.headerText.setText(mImgList.get(position + 1).getParentDir());
        }

    }

    @Override
    public int getItemViewType(int position) {
        return getItemAt(position) == null ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return mImgList.size();
    }


    class ImgViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.imageView)
        ImageView imageView;


        @Bind(R.id.imageView_circle)
        ImageView chosenImage;

        public ImgViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }
    }
}

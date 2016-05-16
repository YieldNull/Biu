package com.bbbbiu.biu.gui.adapter.choose;

import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapter.util.HeaderViewHolder;
import com.bbbbiu.biu.gui.choose.ChooseBaseActivity;
import com.bbbbiu.biu.util.SearchUtil;
import com.bbbbiu.biu.util.SizeUtil;
import com.bbbbiu.biu.db.search.ModelItem;
import com.squareup.picasso.Picasso;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by YieldNull at 4/17/16
 */
public class ImgContentAdapter extends ContentBaseAdapter {
    private Picasso mPicasso;
    private int mImgWidth;
    private Drawable mPlaceholder;

    public ImgContentAdapter(final ChooseBaseActivity context) {
        super(context);

        this.context = context;
        mPicasso = Picasso.with(context);

        // 计算图片宽度，高度
        float screenWidth = SizeUtil.getScreenWidth(context);
        float dpMargin = SizeUtil.convertDpToPixel(2);
        mImgWidth = (int) (screenWidth - dpMargin * 2) / 3;

        // tint place holder
        Drawable drawable = context.getResources().getDrawable(R.drawable.img_placeholder);
        mPlaceholder = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(mPlaceholder, context.getResources().getColor(R.color.img_placeholder));

    }

    @Override
    protected boolean readDataFromDB() {
        return queryModelItemFromDb(ModelItem.TYPE_IMG);
    }

    @Override
    protected boolean readDataFromSys() {
        return setDataSet(ModelItem.sortItemWithDir(SearchUtil.scanImageItem(context)));
    }

    @Override
    protected void updateDatabase() {
        SearchUtil.scanImageItem(context);
    }

    @Override
    public void cancelPicassoTask() {
        mPicasso.cancelTag(PICASSO_TAG);
    }

    @Override
    public RecyclerView.ViewHolder onCreateItemViewHolder(LayoutInflater inflater, ViewGroup parent) {
        return new ImgViewHolder(inflater.inflate(R.layout.list_img_item, parent, false));
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

            // 设置图片大小，不然高度会出现问题，会多出来一点点
            holder.imageView.setLayoutParams(new FrameLayout.LayoutParams(mImgWidth, mImgWidth));

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setItemChosen(position);
                }
            });


            if (isItemChosen(position)) {
                holder.chosenImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_chosen_check));
            } else if (getChosenCount() == 0) {
                holder.chosenImage.setImageDrawable(null);
            } else {
                holder.chosenImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_choosing_circle));
            }

        } else {
            HeaderViewHolder holder = (HeaderViewHolder) hd;

            holder.headerText.setText(getHeaderText(position));
        }

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

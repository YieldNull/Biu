package com.bbbbiu.biu.gui.adapter.choose.content;

import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.db.search.ModelItem;
import com.bbbbiu.biu.gui.choose.BaseChooseActivity;
import com.bbbbiu.biu.gui.choose.listener.OnChangeDirListener;
import com.bbbbiu.biu.util.SearchUtil;
import com.bbbbiu.biu.util.SizeUtil;
import com.bbbbiu.biu.util.StorageUtil;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * 选图片
 * <p/>
 * Created by yieldnull on 8/16/16.
 */
public class ImgContentAdapter extends CommonContentAdapter {

    private static final String TAG = ImgContentAdapter.class.getSimpleName();

    private Map<String, List<ModelItem>> mDirDataMap = new TreeMap<>(new Comparator<String>() {
        @Override
        public int compare(String lhs, String rhs) {
            int result = ((new File(lhs)).getName()).compareTo(new File(rhs).getName());

            return result == 0 ? lhs.compareTo(rhs) : result;
        }
    });

    private List<File> mAlbums = new ArrayList<>();
    private File mCurrentDir;

    private List<ModelItem> mChosenItems = new ArrayList<>();


    private Picasso mPicasso;
    private Drawable mPlaceholder;
    private int mAlbumWidth;
    private int mImageWidth;

    private OnChangeDirListener mOnChangeDirListener;


    public ImgContentAdapter(BaseChooseActivity context) {
        super(context);

        mOnChangeDirListener = (OnChangeDirListener) context;

        mPicasso = Picasso.with(context);

        // 计算图片宽度，高度
        float screenWidth = SizeUtil.getScreenWidth(context);
        float dpMargin = SizeUtil.convertDpToPixel(2);

        mAlbumWidth = (int) (screenWidth - dpMargin * 2) / 2;
        mImageWidth = (int) (screenWidth - dpMargin * 2) / 3;

        // tint place holder
        Drawable drawable = context.getResources().getDrawable(R.drawable.ph_img);
        assert drawable != null;
        mPlaceholder = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(mPlaceholder, context.getResources().getColor(R.color.img_placeholder));
    }


    @Override
    protected boolean readDataFromDB() {
        return setDataSet(ModelItem.queryItemToDir(StorageUtil.TYPE_IMG));
    }

    @Override
    protected boolean readDataFromSys() {
        return setDataSet(ModelItem.sortItemWithDir(SearchUtil.scanImageItem(context)));
    }

    private boolean setDataSet(Map<String, List<ModelItem>> dirDataMap) {
        mDirDataMap.clear();
        mAlbums.clear();

        for (Map.Entry<String, List<ModelItem>> entry : dirDataMap.entrySet()) {
            mAlbums.add(new File(entry.getKey()));

            List<ModelItem> items = entry.getValue();
            Collections.sort(items, new Comparator<ModelItem>() {  // 按时间降序排列
                @Override
                public int compare(ModelItem lhs, ModelItem rhs) {
                    return Long.valueOf(rhs.getFile().lastModified()).compareTo(lhs.getFile().lastModified());
                }
            });
            mDirDataMap.put(entry.getKey(), items);
        }

        return mAlbums.size() > 0;
    }

    @Override
    protected void updateDatabase() {
        SearchUtil.scanImageItem(context);
    }

    @Override
    public void updateDataSet() {
        readDataFromDB();

        if (mCurrentDir != null && mDirDataMap.get(mCurrentDir.getAbsolutePath()) == null) {
            exitDir();
        } else {
            notifyDataSetChanged();
        }
    }

    @Override
    public void cancelPicassoTask() {

    }

    /***********************************************************************************/

    @Override
    public int getItemCount() {
        if (mCurrentDir == null) {
            return mAlbums.size();
        } else {
            return mDirDataMap.get(mCurrentDir.getAbsolutePath()).size();
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateItemViewHolder(LayoutInflater inflater, ViewGroup parent) {
        return null;
    }

    @Override
    public RecyclerView.ViewHolder onCreateHeaderViewHolder(LayoutInflater inflater, ViewGroup parent) {
        return null;
    }

    @Override
    public boolean isHeaderView(int position) {
        return false;
    }


    private static final int TYPE_ALBUM = 1;
    private static final int TYPE_IMAGE = 2;

    @Override
    public int getItemViewType(int position) {
        return mCurrentDir == null ? TYPE_ALBUM : TYPE_IMAGE;  // ViewType变了才重绘？
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == TYPE_ALBUM) {
            return new AlbumHolder(inflater.inflate(R.layout.list_img_album, parent, false));
        } else {
            return new ImgViewHolder(inflater.inflate(R.layout.list_img_item, parent, false));
        }
    }


    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder hd, int position) {
        ImageView imageView;
        File imageFile;
        int width;

        if (getItemViewType(position) == TYPE_ALBUM) {
            AlbumHolder holder = (AlbumHolder) hd;

            final File file = mAlbums.get(position);

            holder.folderText.setText(file.getName());

            imageView = holder.imageView;
            imageFile = mDirDataMap.get(file.getAbsolutePath()).get(0).getFile();
            width = mAlbumWidth;

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    enterDir(file);
                }
            });

        } else {
            ImgViewHolder holder = (ImgViewHolder) hd;
            final ModelItem item = mDirDataMap.get(mCurrentDir.getAbsolutePath()).get(position);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setItemChosen(item);
                }
            });


            if (isItemChosen(item)) {
                holder.chosenImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_chosen_check));
            } else if (getChosenCount() == 0) {
                holder.chosenImage.setImageDrawable(null);
            } else {
                holder.chosenImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_choosing_circle));
            }

            imageFile = item.getFile();
            imageView = holder.imageView;
            width = mImageWidth;
        }

        mPicasso.load(imageFile)
                .tag(PICASSO_TAG)
                .resize(width, width)
                .placeholder(mPlaceholder)
                .onlyScaleDown()
                .centerCrop()
                .into(imageView);

        // 设置图片大小，不然高度会出现问题，会多出来一点点
        imageView.setLayoutParams(new FrameLayout.LayoutParams(width, width));
    }

    /***********************************************************************************/


    @Override
    public int getChosenCount() {
        return mChosenItems.size();
    }

    @Override
    public Set<String> getChosenFiles() {
        Set<String> set = new HashSet<>();
        for (ModelItem item : mChosenItems) {
            set.add(item.getPath());
        }
        return set;
    }

    @Override
    public boolean isFileChosen(File file) {
        return getChosenFiles().contains(file.getAbsolutePath());
    }

    @Override
    public void setFileAllChosen() {
        mChosenItems.clear();
        mChosenItems.addAll(mDirDataMap.get(mCurrentDir.getAbsolutePath()));
        notifyDataSetChanged();
    }

    @Override
    public void setFileAllDismissed() {
        mChosenItems.clear();
        notifyDataSetChanged();
    }

    /***********************************************************************************/

    /**
     * 是否已进入相册
     *
     * @return 是否已进入相册
     */
    public boolean inAlbum() {
        return mCurrentDir != null;
    }

    public void exitDir() {
        Log.i(TAG, "exitDir");

        mChosenItems.clear();

        mOnChangeDirListener.onExitDir(mCurrentDir);
        mCurrentDir = null;

        notifyDataSetChanged();

        if (getItemCount() == 0) {
            notifyEmptyDataSet();
        } else {
            notifyNonEmptyDataSet();
        }
    }

    private void enterDir(File file) {
        Log.i(TAG, "enterDir");

        mOnChangeDirListener.onEnterDir(file);
        mCurrentDir = file;

        notifyDataSetChanged();
    }

    private void setItemChosen(ModelItem item) {
        if (!mChosenItems.contains(item)) {
            mChosenItems.add(item);
            notifyFileChosen(item.getPath());
        } else {
            mChosenItems.remove(item);
            notifyFileDismissed(item.getPath());
        }

        notifyDataSetChanged();
    }

    private boolean isItemChosen(ModelItem item) {
        return mChosenItems.contains(item);
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

    class AlbumHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.imageView)
        ImageView imageView;

        @Bind(R.id.textView_folder)
        TextView folderText;


        public AlbumHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }
    }
}

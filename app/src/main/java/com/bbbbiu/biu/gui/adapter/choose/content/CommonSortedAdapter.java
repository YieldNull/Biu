package com.bbbbiu.biu.gui.adapter.choose.content;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.db.search.ModelItem;
import com.bbbbiu.biu.gui.choose.BaseChooseActivity;

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
 * 实现了将读取到的数据按文件夹分类,的CommonAdapter
 * <p/>
 * Created by YieldNull at 5/18/16
 */
public abstract class CommonSortedAdapter extends CommonContentAdapter {
    private static final String TAG = CommonSortedAdapter.class.getSimpleName();


    /**
     * 数据集，用于显示
     */
    protected List<ModelItem> mDataSetItems = new ArrayList<>();

    /**
     * 已选项
     */
    protected List<ModelItem> mChosenItems = new ArrayList<>();

    protected List<String> mCollapsedFolderList = new ArrayList<>();
    /**
     * 文件夹：数据
     */
    protected Map<String, List<ModelItem>> mDirDataMap = new TreeMap<>(new Comparator<String>() {
        @Override
        public int compare(String lhs, String rhs) {
            return ((new File(lhs)).getName()).compareTo(new File(rhs).getName());
        }
    });

    private Comparator<ModelItem> mDefaultItemComparator = new Comparator<ModelItem>() {
        @Override
        public int compare(ModelItem lhs, ModelItem rhs) {
            return lhs.getFile().getName().compareTo(rhs.getFile().getName());
        }
    };

    public abstract Comparator<ModelItem> getItemComparator();

    public CommonSortedAdapter(BaseChooseActivity context) {
        super(context);
    }


    /***********************************************************************************
     * ***************实现 {@link CommonContentAdapter}中的抽象方法   *********************
     **********************************************************************************/
    @Override
    protected boolean readDataFromDB() {
        return setDataSet(readSortedDataFromDB());
    }

    @Override
    protected boolean readDataFromSys() {
        return setDataSet(readSortedDataFromSys());
    }

    protected abstract Map<String, List<ModelItem>> readSortedDataFromDB();

    protected abstract Map<String, List<ModelItem>> readSortedDataFromSys();

    /***********************************************************************************
     * ***************实现 {@link BaseContentAdapter}中的抽象方法   *********************
     **********************************************************************************/
    @Override
    public void updateDataSet() {
        readDataFromDB();
        notifyDataSetChanged();
    }

    @Override
    public boolean isHeaderView(int position) {
        return getItemAt(position) instanceof HeaderPlaceHolderItem;
    }

    @Override
    public int getItemCount() {
        return mDataSetItems.size();
    }

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

        for (ModelItem item : mDataSetItems) {
            if (!(item instanceof HeaderPlaceHolderItem)) {
                mChosenItems.add(item);
            }
        }

        notifyDataSetChanged();
    }

    @Override
    public void setFileAllDismissed() {
        mChosenItems.clear();
        notifyDataSetChanged();
    }

    /***********************************************************************************/


    @Override
    public RecyclerView.ViewHolder onCreateHeaderViewHolder(LayoutInflater inflater, ViewGroup parent) {
        View itemView = inflater.inflate(R.layout.list_header_sorted, parent, false);
        return new ExpandableHeaderViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder hd, final int position) {
        if (getItemViewType(position) == VIEW_TYPE_ITEM) {
            onBindItemViewHolder(hd, position);
        } else {
            final ExpandableHeaderViewHolder holder = (ExpandableHeaderViewHolder) hd;
            final HeaderPlaceHolderItem placeHolder = (HeaderPlaceHolderItem) getItemAt(position);

            final String folderName = placeHolder.getName();
            final String folder = placeHolder.getPath();

            holder.nameText.setText(folderName);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCollapsedFolderList.contains(folder)) {
                        holder.arrowImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_arrow_down));
                        mCollapsedFolderList.remove(folder);
                        expandItems(placeHolder);
                    } else {
                        holder.arrowImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_arrow_right));
                        mCollapsedFolderList.add(folder);
                        collapseItems(placeHolder);
                    }
                }
            });

            if (mCollapsedFolderList.contains(folder)) {
                holder.arrowImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_arrow_right));
            } else {
                holder.arrowImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_arrow_down));
            }

            holder.itemView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    int action = event.getAction();

                    if (action == MotionEvent.ACTION_DOWN) {
                        v.setBackgroundColor(context.getResources().getColor(R.color.file_header_pressed));
                    } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        v.setBackgroundColor(context.getResources().getColor(R.color.file_header));
                    }
                    return false;
                }
            });
        }
    }

    public abstract void onBindItemViewHolder(RecyclerView.ViewHolder holder, int position);


    /***********************************************************************************/

    /**
     * 获取数据集中position处的项
     *
     * @param position position
     * @return database model item
     */
    protected ModelItem getItemAt(int position) {
        return mDataSetItems.get(position);
    }

    /**
     * 是否被选
     *
     * @param position position
     * @return 是否被选
     */
    protected boolean isItemChosen(int position) {
        return mChosenItems.contains(getItemAt(position));
    }


    /**
     * 被选,别用position做回调啊，当数据集变了position就会变啊
     *
     * @param item item
     */
    protected void setItemChosen(ModelItem item) {
        if (!mChosenItems.contains(item)) {
            mChosenItems.add(item);
            notifyFileChosen(item.getPath());
        } else {
            mChosenItems.remove(item);
            notifyFileDismissed(item.getPath());
        }
        notifyDataSetChanged();
    }

    /**
     * 设置数据集，先清空再设置
     *
     * @param sortedItems 已经按文件夹分好类的数据
     * @return 设置完成后数据集是否为空
     */
    protected boolean setDataSet(Map<String, List<ModelItem>> sortedItems) {
        mDirDataMap.clear();
        mDirDataMap.putAll(sortedItems);

        // 剔除已收起的，其余全展开
        Set<String> keySet = sortedItems.keySet();
        List<String> toRemove = new ArrayList<>();

        for (String folder : mCollapsedFolderList) {
            if (!keySet.contains(folder)) {
                toRemove.add(folder); // 已经不存在了
            }
        }

        mCollapsedFolderList.removeAll(toRemove);

        onItemRangeChanged();

        return mDataSetItems.size() != 0;
    }

    /**
     * 获取默认排序
     *
     * @return 默认按名称升序
     */
    protected Comparator<ModelItem> getDefaultItemComparator() {
        return mDefaultItemComparator;
    }

    /**
     * 展开文件夹
     *
     * @param item 文件夹PlaceHolderItem
     */
    private void expandItems(HeaderPlaceHolderItem item) {
        onItemRangeChanged();
        notifyItemRangeInserted(mDataSetItems.indexOf(item) + 1, mDirDataMap.get(item.getPath()).size());
    }

    /**
     * 折叠文件夹
     *
     * @param item 文件夹PlaceHolderItem
     */
    private void collapseItems(HeaderPlaceHolderItem item) {
        onItemRangeChanged();
        notifyItemRangeRemoved(mDataSetItems.indexOf(item) + 1, mDirDataMap.get(item.getPath()).size());
    }

    /**
     * 展开，折叠文件夹后，更新数据
     */
    private void onItemRangeChanged() {
        mDataSetItems.clear();

        for (Map.Entry<String, List<ModelItem>> entry : mDirDataMap.entrySet()) {
            List<ModelItem> list = entry.getValue();
            String folder = entry.getKey();

            if (list.size() > 0) {
                mDataSetItems.add(new HeaderPlaceHolderItem(folder));
                if (!mCollapsedFolderList.contains(folder)) {
                    Collections.sort(list, getItemComparator());
                    mDataSetItems.addAll(list);
                }
            }
        }
    }


    /***********************************************************************************/

    /**
     * Header Place Holder
     */
    class HeaderPlaceHolderItem extends ModelItem {
        String folderPath;

        HeaderPlaceHolderItem(String folderPath) {
            this.folderPath = folderPath;
        }

        String getName() {
            return new File(folderPath).getName();
        }


        @Override
        public String getPath() {
            return folderPath;
        }

        @Override
        public File getFile() {
            return null;
        }

        @Override
        public String getSize() {
            return null;
        }

        @Override
        public File getParentFile() {
            return null;
        }

    }

    /**
     * Header ViewHolder
     */
    class ExpandableHeaderViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.imageView_arrow)
        ImageView arrowImage;

        @Bind(R.id.textView_name)
        TextView nameText;

        public ExpandableHeaderViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }
    }
}

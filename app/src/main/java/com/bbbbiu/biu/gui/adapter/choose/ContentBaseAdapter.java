package com.bbbbiu.biu.gui.adapter.choose;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapter.util.HeaderViewHolder;
import com.bbbbiu.biu.gui.choose.ChooseBaseActivity;
import com.bbbbiu.biu.gui.choose.OnChoosingListener;
import com.bbbbiu.biu.gui.choose.OnItemOptionClickListener;
import com.bbbbiu.biu.db.search.ModelItem;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 选择文件时，显示数据的Adapter基类。
 * <p/>
 * 如何获取数据：
 * <p/>
 * - 先从数据库读数据 {@link ContentBaseAdapter#readDataFromDB()}，要是没有则
 * <p/>
 * - 从系统中 {@link ContentBaseAdapter#readDataFromSys()} 读取数据
 * <p/>
 * - 将数据显示（没有则显示空）
 * <p/>
 * - 要数据是从数据库中读取的，那么就后台再扫描一遍MediaStore，存入数据库 {@link ContentBaseAdapter#updateDatabase()}
 * <p/>
 * - 然后更新数据集并显示，{@link ContentBaseAdapter#updateDataSet()}
 * <p/>
 * Created by YieldNull at 3/26/16
 */
public abstract class ContentBaseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = ContentBaseAdapter.class.getSimpleName();

    public static final int VIEW_TYPE_HEADER = 0;
    public static final int VIEW_TYPE_ITEM = 1;

    private OnLoadingDataListener mLoadingDataListener;
    private OnChoosingListener mOnChoosingListener;
    private OnItemOptionClickListener mOnItemOptionClickListener;

    protected Context context;

    /**
     * 数据集，用于显示
     */
    protected List<ModelItem> mDataSetItems = new ArrayList<>();

    /**
     * 已选项
     */
    protected List<ModelItem> mChosenItems = new ArrayList<>();

    /**
     * 为所有Picasso请求打上标签，以便之后取消
     */
    protected static final String PICASSO_TAG = "tag-img";


    /**
     * OnBackPress时，取消当前未完成的PicassoTask
     */
    public abstract void cancelPicassoTask();

    /**
     * 生成数据项的 ViewHolder
     *
     * @param inflater LayoutInflater
     * @param parent   parent
     * @return ViewHolder
     */
    public abstract RecyclerView.ViewHolder onCreateItemViewHolder(LayoutInflater inflater, ViewGroup parent);

    /**
     * 构造函数
     *
     * @param context 需要实现{@link OnLoadingDataListener},{@link OnChoosingListener},{@link OnItemOptionClickListener}
     */
    public ContentBaseAdapter(final ChooseBaseActivity context) {
        mLoadingDataListener = context;
        mOnChoosingListener = context;
        mOnItemOptionClickListener = context;
        this.context = context;

        notifyStartLoadingData();

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean hasData = readDataFromDB();
                boolean loadFromDb = true;

                if (!hasData) {
                    hasData = readDataFromSys();
                    loadFromDb = false;
                }

                final boolean finalHasData = hasData;
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        notifyFinishLoadingData(); // 取消加载动画

                        if (finalHasData) {
                            notifyDataSetChanged();
                        } else {
                            // TODO 显示空界面
                        }
                    }
                });

                if (loadFromDb) {
                    Log.i(TAG, "Updating database");
                    updateDatabase();
                    Log.i(TAG, "Finish updating");

                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "updating dataSet");
                            updateDataSet();
                            notifyDataSetChanged();
                            Log.i(TAG, "notify dataSet updated");
                        }
                    });
                }

            }
        }).start();
    }

    /***********************************获取数据***************************************/

    /**
     * 从数据库读取数据，并设置为Adapter的数据集
     *
     * @return 是否有数据
     */
    protected abstract boolean readDataFromDB();

    /**
     * 直接从MediaStore读取数据，并设置为Adapter的数据集
     *
     * @return 是否有数据
     */
    protected abstract boolean readDataFromSys();

    /**
     * 更新数据库
     */
    protected abstract void updateDatabase();

    /**
     * 更新Adapter的数据集
     */
    protected void updateDataSet() {
        readDataFromDB();
    }

    /***********************************************************************************/

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView;

        if (viewType == VIEW_TYPE_HEADER) {
            itemView = inflater.inflate(R.layout.list_header_common, parent, false);
            return new HeaderViewHolder(itemView);
        } else {
            return onCreateItemViewHolder(inflater, parent);
        }
    }

    @Override
    public int getItemCount() {
        return mDataSetItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return getItemAt(position) == null ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }

    /**
     * 获取已选文件的路径集
     *
     * @return 绝对路径集
     */
    public Set<String> getChosenFiles() {
        Set<String> set = new HashSet<>();
        for (ModelItem item : mChosenItems) {
            set.add(item.getPath());
        }
        return set;
    }

    /**
     * 已选项数量
     *
     * @return 数量
     */
    public int getChosenCount() {
        return mChosenItems.size();
    }

    /**
     * 当前数据集全被选
     */
    public void setFileAllChosen() {
        mChosenItems.clear();

        for (ModelItem item : mDataSetItems) {
            if (item != null) {
                mChosenItems.add(item);
            }
        }

        notifyDataSetChanged();
    }

    /**
     * 清除所有已选项目
     */
    public void setFileAllDismissed() {
        mChosenItems.clear();
        notifyDataSetChanged();
    }


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
     * 被选
     *
     * @param position position
     */
    protected void setItemChosen(int position) {
        ModelItem item = getItemAt(position);
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
     * header text
     *
     * @param position position of header view item
     * @return header text if it's a header at that position else null
     */
    protected String getHeaderText(int position) {
        if (getItemAt(position) == null) {
            return getItemAt(position + 1).getParentDirName();
        } else {
            return null;
        }
    }

    /**
     * 查询数据库中对应类型文件的数据,将查询所得Item按文件夹分类
     *
     * @param type 类型
     * @return 是否没有纪录
     * @see ModelItem#queryItemToDir(int)
     */
    protected boolean queryModelItemFromDb(int type) {
        return setDataSet(ModelItem.queryItemToDir(type));
    }

    /**
     * 设置数据集
     *
     * @param sortedItems 已经按文件夹分好类的数据
     * @return 设置完成后数据集是否为空
     */
    protected boolean setDataSet(Map<String, List<ModelItem>> sortedItems) {
        mDataSetItems.clear();

        for (Map.Entry<String, List<ModelItem>> entry : sortedItems.entrySet()) {
            List<ModelItem> list = entry.getValue();
            if (list.size() > 0) {
                mDataSetItems.add(null);
                mDataSetItems.addAll(list);
            }
        }
        return mDataSetItems.size() != 0;
    }


    /***********************************与Activity进行交互***********************************/

    /**
     * notify正在加载数据
     */
    protected void notifyStartLoadingData() {
        mLoadingDataListener.onStartLoadingData();
    }

    /**
     * notify 数据加载完成
     */
    protected void notifyFinishLoadingData() {
        mLoadingDataListener.onFinishLoadingData();
    }


    /**
     * notify文件被选
     * <p/>
     * 要在添加或删除文件之后进行notify
     *
     * @param filePath 文件路径
     */
    protected void notifyFileChosen(String filePath) {
        mOnChoosingListener.onFileChosen(filePath);
    }

    /**
     * notify文件被取消选择
     *
     * @param filePath 文件路径
     */
    protected void notifyFileDismissed(String filePath) {
        mOnChoosingListener.onFileDismissed(filePath);
    }

    /**
     * notify 文件选项菜单被点击
     *
     * @param file 文件
     */
    protected void notifyFileItemOptionClicked(File file) {
        mOnItemOptionClickListener.onFileOptionClick(file);
    }
}

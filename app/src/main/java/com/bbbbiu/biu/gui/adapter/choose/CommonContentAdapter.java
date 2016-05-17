package com.bbbbiu.biu.gui.adapter.choose;

import android.util.Log;

import com.bbbbiu.biu.gui.choose.BaseChooseActivity;
import com.bbbbiu.biu.gui.choose.listener.OnChoosingListener;
import com.bbbbiu.biu.gui.choose.listener.OnLoadingDataListener;
import com.bbbbiu.biu.gui.choose.listener.OptionPanelActionListener;
import com.bbbbiu.biu.db.search.ModelItem;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 从各个类别选文件时的CommonAdapter
 * <p/>
 * 如何获取数据：
 * <p/>
 * - 先从数据库读数据 {@link CommonContentAdapter#readDataFromDB()}，要是没有则
 * <p/>
 * - 从系统中 {@link CommonContentAdapter#readDataFromSys()} 读取数据
 * <p/>
 * - 将数据显示（没有则显示空）
 * <p/>
 * - 要数据是从数据库中读取的，那么就后台再扫描一遍MediaStore，存入数据库 {@link CommonContentAdapter#updateDatabase()}
 * <p/>
 * - 然后更新数据集并显示，{@link CommonContentAdapter#updateDataSet()}
 * <p/>
 * <p/>
 * 如何显示 TODO
 * <p/>
 * Created by YieldNull at 3/26/16
 */
public abstract class CommonContentAdapter extends BaseContentAdapter {
    private static final String TAG = CommonContentAdapter.class.getSimpleName();


    /**
     * 数据集，用于显示
     */
    protected List<ModelItem> mDataSetItems = new ArrayList<>();

    /**
     * 已选项
     */
    protected List<ModelItem> mChosenItems = new ArrayList<>();


    /**
     * 构造函数
     *
     * @param context 需要实现{@link OnLoadingDataListener},{@link OnChoosingListener},{@link OptionPanelActionListener}
     */
    public CommonContentAdapter(final BaseChooseActivity context) {
        super(context);

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


    /***********************************************************************************/

    /***********************************************************************************
     * ***************   实现 ContentBaseAdapter 中的抽象方法   **************************
     **********************************************************************************/
    @Override
    public void updateDataSet() {
        readDataFromDB();
        notifyDataSetChanged();
    }

    @Override
    public boolean isHeaderView(int position) {
        return getItemAt(position) == null;
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
            if (item != null) {
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
     * 设置数据集，先清空再设置
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


    /**
     * header text，获取文件夹的名称
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
}

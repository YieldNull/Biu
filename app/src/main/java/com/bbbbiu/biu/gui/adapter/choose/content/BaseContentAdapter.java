package com.bbbbiu.biu.gui.adapter.choose.content;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.bbbbiu.biu.gui.adapter.FileChooser;
import com.bbbbiu.biu.gui.choose.BaseChooseActivity;
import com.bbbbiu.biu.gui.choose.listener.OnChoosingListener;
import com.bbbbiu.biu.gui.choose.listener.OnLoadingDataListener;
import com.bbbbiu.biu.gui.choose.listener.OptionPanelActionListener;

import java.io.File;

/**
 * 选择文件时，显示数据的Adapter基类。定义了一些需要在Activity中使用的接口
 * <p/>
 * Created by YieldNull at 5/17/16
 */
public abstract class BaseContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements FileChooser {
    private static final String TAG = BaseContentAdapter.class.getSimpleName();

    private OnLoadingDataListener mLoadingDataListener;
    private OnChoosingListener mOnChoosingListener;
    private OptionPanelActionListener mOptionPanelListener;

    protected Context context;

    /**
     * 为所有Picasso请求打上标签，以便之后取消
     */
    protected static final String PICASSO_TAG = "tag-img";


    /**
     * 构造函数
     *
     * @param context 需要实现{@link OnLoadingDataListener},{@link OnChoosingListener},{@link OptionPanelActionListener}
     */
    public BaseContentAdapter(BaseChooseActivity context) {
        mLoadingDataListener = context;
        mOnChoosingListener = context;
        mOptionPanelListener = context;
        this.context = context;

    }

    /***********************************************************************************
     * *********{@link android.support.v7.widget.RecyclerView.ViewHolder}   ************
     **********************************************************************************/

    public static final int VIEW_TYPE_HEADER = 0;
    public static final int VIEW_TYPE_ITEM = 1;


    /**
     * 生成数据项的 ViewHolder
     *
     * @param inflater LayoutInflater
     * @param parent   parent
     * @return ViewHolder
     */
    public abstract RecyclerView.ViewHolder onCreateItemViewHolder(LayoutInflater inflater, ViewGroup parent);

    public abstract RecyclerView.ViewHolder onCreateHeaderViewHolder(LayoutInflater inflater, ViewGroup parent);

    public abstract boolean isHeaderView(int position);

    @Override
    public int getItemViewType(int position) {
        return isHeaderView(position) ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        if (viewType == VIEW_TYPE_HEADER) {
            return onCreateHeaderViewHolder(inflater, parent);
        } else {
            return onCreateItemViewHolder(inflater, parent);
        }
    }

    /***************************************************************************************/

    /**
     * 更新Adapter的数据集,别忘了调用 {@link RecyclerView.Adapter#notifyDataSetChanged()}
     */
    public abstract void updateDataSet();

    /**
     * OnBackPress时，取消当前未完成的PicassoTask
     */
    public abstract void cancelPicassoTask();


    /***********************************与Activity进行交互***********************************/

    /**
     * notify 正在加载数据
     */
    public void notifyStartLoadingData() {
        mLoadingDataListener.onStartLoadingData();
    }

    /**
     * notify 数据加载完成
     */
    public void notifyFinishLoadingData() {
        mLoadingDataListener.onFinishLoadingData();
    }


    /**
     * 无任何对应的文件
     */
    public void notifyEmptyDataSet() {
        mLoadingDataListener.onEmptyDataSet();
    }

    /**
     * 无任何对应的文件
     */
    public void notifyNonEmptyDataSet() {
        mLoadingDataListener.onNonEmptyDataSet();
    }

    /**
     * notify 文件被选
     * <p/>
     * 要在添加或删除文件之后进行notify
     *
     * @param filePath 文件路径
     */
    public void notifyFileChosen(String filePath) {
        mOnChoosingListener.onFileChosen(filePath);
    }

    /**
     * notify 文件被取消选择
     *
     * @param filePath 文件路径
     */
    public void notifyFileDismissed(String filePath) {
        mOnChoosingListener.onFileDismissed(filePath);
    }

    /**
     * notify 文件选项菜单被点击
     *
     * @param file 文件
     */
    public void notifyOptionToggleClicked(File file) {
        mOptionPanelListener.onOptionToggleClicked(file);
    }
}

package com.bbbbiu.biu.gui.adapter.choose;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapter.util.HeaderViewHolder;
import com.bbbbiu.biu.gui.choose.BaseChooseActivity;
import com.bbbbiu.biu.gui.choose.listener.OnChoosingListener;
import com.bbbbiu.biu.gui.choose.listener.OnLoadingDataListener;
import com.bbbbiu.biu.gui.choose.listener.OptionPanelActionListener;

import java.io.File;
import java.util.Set;

/**
 * 选择文件时，显示数据的Adapter基类。
 * <p/>
 * Created by YieldNull at 5/17/16
 */
public abstract class BaseContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = BaseContentAdapter.class.getSimpleName();

    private OnLoadingDataListener mLoadingDataListener;
    private OnChoosingListener mOnChoosingListener;
    private OptionPanelActionListener mOptionPanelListener;

    protected Context context;

    /**
     * 为所有Picasso请求打上标签，以便之后取消
     */
    protected static final String PICASSO_TAG = "tag-img";

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

    public abstract boolean isHeaderView(int position);

    @Override
    public int getItemViewType(int position) {
        return isHeaderView(position) ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }

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

    /***************************************************************************************/

    /**
     * 更新Adapter的数据集,别忘了调用 {@link RecyclerView.Adapter#notifyDataSetChanged()}
     */
    public abstract void updateDataSet();

    /**
     * OnBackPress时，取消当前未完成的PicassoTask
     */
    public abstract void cancelPicassoTask();

    /**
     * 已选项数量
     *
     * @return 数量
     */
    public abstract int getChosenCount();


    /**
     * 获取已选文件的路径集
     *
     * @return 绝对路径集
     */
    public abstract Set<String> getChosenFiles();


    /**
     * 文件是否已被选中
     *
     * @param file file
     * @return 是否选中
     */
    public abstract boolean isFileChosen(File file);


    /**
     * 当前数据集全被选
     */
    public abstract void setFileAllChosen();

    /**
     * 清除所有已选项目
     */
    public abstract void setFileAllDismissed();


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

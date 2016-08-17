package com.bbbbiu.biu.gui.choose.listener;

/**
 * 加载数据监听
 * Created by YieldNull at 4/7/16
 */
public interface OnLoadingDataListener {

    /**
     * 开始加载数据
     */
    void onStartLoadingData();

    /**
     * 数据加载完成
     */
    void onFinishLoadingData();

    /**
     * 数据集为空
     */
    void onEmptyDataSet();


    /**
     * 数据集不为空
     */
    void onNonEmptyDataSet();
}

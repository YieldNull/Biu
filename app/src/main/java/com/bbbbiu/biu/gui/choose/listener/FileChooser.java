package com.bbbbiu.biu.gui.choose.listener;

import java.io.File;
import java.util.Set;

/**
 * 选择文件
 * <p/>
 * Created by finalize on 7/31/16.
 */
public interface FileChooser {

    /**
     * 已选项数量
     *
     * @return 数量
     */
    int getChosenCount();


    /**
     * 获取已选文件的路径集
     *
     * @return 绝对路径集
     */
    Set<String> getChosenFiles();


    /**
     * 文件是否已被选中
     *
     * @param file file
     * @return 是否选中
     */
    boolean isFileChosen(File file);


    /**
     * 当前数据集全被选
     */
    void setFileAllChosen();


    /**
     * 清除所有已选项目
     */
    void setFileAllDismissed();
}

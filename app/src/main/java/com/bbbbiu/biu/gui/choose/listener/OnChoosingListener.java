package com.bbbbiu.biu.gui.choose.listener;

/**
 * 文件选择监听
 */
public interface OnChoosingListener {

    /**
     * 文件被选
     *
     * @param filePath 文件路径
     */
    void onFileChosen(String filePath);

    /**
     * 文件取消选择
     *
     * @param filePath 文件路径
     */
    void onFileDismissed(String filePath);
}

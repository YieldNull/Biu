package com.bbbbiu.biu.gui.choose.listener;

import java.io.File;

/**
 * 监听底部滑出菜单的操作
 */
public interface OptionPanelActionListener {

    /**
     * 滑出菜单的开关被点击
     *
     * @param file 菜单对应的文件
     */
    void onOptionToggleClicked(File file);

    /**
     * 菜单子项被选择
     *
     * @param file 菜单对应的文件
     */
    void onOptionItemClicked(File file);

    /**
     * 菜单的某项操作更改了显示的数据集
     */
    void onOptionModifyContent();

    /**
     * 菜单的某项操作删除了数据集中的文件
     *
     * @param file 被删文件
     */
    void onOptionDeleteFile(File file);
}

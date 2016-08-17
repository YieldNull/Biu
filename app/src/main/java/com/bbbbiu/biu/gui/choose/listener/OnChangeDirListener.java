package com.bbbbiu.biu.gui.choose.listener;

import java.io.File;

/**
 * 切换目录监听
 * <p/>
 * Created by YieldNull at 4/7/16
 */
public interface OnChangeDirListener {

    /**
     * 进入目录
     *
     * @param dir 目录
     */
    void onEnterDir(File dir);

    /**
     * 退出目录
     *
     * @param dir 目录
     */
    void onExitDir(File dir);
}

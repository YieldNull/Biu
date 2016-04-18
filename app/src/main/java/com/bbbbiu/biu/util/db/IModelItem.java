package com.bbbbiu.biu.util.db;

import java.io.File;

/**
 * Created by YieldNull at 4/18/16
 */
public interface IModelItem {
    int TYPE_MUSIC = 1;
    int TYPE_VIDEO = 2;
    int TYPE_IMG = 3;
    int TYPE_ARCHIVE = 4;
    int TYPE_WORD = 5;
    int TYPE_EXCEL = 6;
    int TYPE_PPT = 7;
    int TYPE_PDF = 8;
    int TYPE_APK = 9; // APK 安装包

    String getPath();

    File getFile();

    String getSize();

    String getParentDirName();

}

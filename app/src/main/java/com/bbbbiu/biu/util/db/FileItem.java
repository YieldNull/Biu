package com.bbbbiu.biu.util.db;

import com.bbbbiu.biu.util.StorageUtil;
import com.orm.SugarRecord;
import com.orm.dsl.Unique;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 文档，压缩包，图片
 * <p/>
 * Created by YieldNull at 4/15/16
 */
public class FileItem extends ModelItem {

    @Unique
    public String path;
    public int type;

    public FileItem() {
    }

    public FileItem(String path, int type) {
        this.path = path;
        this.type = type;
    }

    /**
     * 存储将全盘扫描之后得到的文件分类
     *
     * @param type    文件分类
     * @param pathSet 文件绝对路径集合
     */
    public static void storeFileItems(int type, Set<String> pathSet) {
        for (String path : pathSet) {
            new FileItem(path, type).save();
        }
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public File getFile() {
        return new File(path);
    }

    @Override
    public String getSize() {
        return StorageUtil.getReadableSize(getFile().length());
    }

    @Override
    public String getParentDirName() {
        return getFile().getParentFile().getName();
    }

}

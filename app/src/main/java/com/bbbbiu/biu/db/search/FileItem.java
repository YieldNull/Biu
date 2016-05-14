package com.bbbbiu.biu.db.search;

import com.bbbbiu.biu.util.StorageUtil;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.annotation.Index;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;

import java.io.File;
import java.util.Set;

/**
 * 文档，压缩包，图片
 * <p/>
 * Created by YieldNull at 4/15/16
 */

@Table(database = FileItem.MyDatabase.class)
public class FileItem extends ModelItem {

    @Database(name = MyDatabase.NAME, version = MyDatabase.VERSION)
    public class MyDatabase {

        public static final String NAME = "file";

        public static final int VERSION = 1;
    }

    @PrimaryKey
    @Index
    public String path;

    @Column
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

package com.bbbbiu.biu.db.search;

import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 数据库基类，提供公有接口
 * <p/>
 * Created by YieldNull at 4/18/16
 */
public abstract class ModelItem extends BaseModel {
    public static final int TYPE_MUSIC = 1;
    public static final int TYPE_VIDEO = 2;
    public static final int TYPE_IMG = 3;
    public static final int TYPE_ARCHIVE = 4;
    public static final int TYPE_WORD = 5;
    public static final int TYPE_EXCEL = 6;
    public static final int TYPE_PPT = 7;
    public static final int TYPE_PDF = 8;
    public static final int TYPE_APK = 9; // APK 安装包
    public static final int TYPE_DOC = 10;

    /**
     * 获取对应的路径
     *
     * @return 路径
     */
    public abstract String getPath();

    /**
     * 获取对应的文件
     *
     * @return 文件
     */
    public abstract File getFile();

    /**
     * 获取MediaFile{@link com.bbbbiu.biu.util.StorageUtil#getReadableSize(long)}
     *
     * @return MediaFile
     */
    public abstract String getSize();

    /**
     * 获取父文件夹的名称
     *
     * @return 名称
     */
    public abstract File getParentFile();

    /**
     * 根据path确定唯一性
     *
     * @param o object
     * @return 是否相等
     */
    @Override
    public boolean equals(Object o) {
        return (o instanceof ModelItem) && ((ModelItem) o).getPath().equals(getPath());
    }

    /**
     * 从数据库中读取指定类型的文件
     * 并按文件夹分类
     *
     * @param type 类型
     * @return {“Folder”:itemList}
     */
    public static Map<String, List<ModelItem>> queryItemToDir(int type) {
        List<ModelItem> allRecords = new ArrayList<>();

        if (type == TYPE_MUSIC || type == TYPE_VIDEO) {
            List<MediaItem> records = SQLite.select()
                    .from(MediaItem.class)
                    .where(MediaItem_Table.type.eq(type))
                    .queryList();

            removeNotExisting(records);

            allRecords.addAll(records);
        } else {
            List<FileItem> records = SQLite.select()
                    .from(FileItem.class)
                    .where(FileItem_Table.type.eq(type))
                    .queryList();

            removeNotExisting(records);

            allRecords.addAll(records);
        }

        return sortItemWithDir(allRecords);
    }


    /**
     * 将Item按文件夹分类
     *
     * @param items 待分类列表
     * @param <T>   ModelItem
     * @return {“Folder（绝对路径）”:itemList}
     */
    public static <T extends ModelItem> Map<String, List<ModelItem>> sortItemWithDir(List<T> items) {
        Map<String, List<ModelItem>> dirItemMap = new HashMap<>();


        for (ModelItem item : items) {
            String pDirName = item.getFile().getParentFile().getAbsolutePath();
            List<ModelItem> list = dirItemMap.get(pDirName);

            if (list == null) {
                list = new ArrayList<>();
                dirItemMap.put(pDirName, list);
            }

            list.add(item);
        }

        return dirItemMap;
    }

    /**
     * 剔除不存在的文件
     *
     * @param items ModelItems
     * @param <T>   ModelItems
     */
    public static <T extends ModelItem> void removeNotExisting(List<T> items) {
        List<T> itemsToDelete = new ArrayList<>();

        for (T t : items) {
            File file = new File(t.getPath());
            if (!file.exists()) {
                itemsToDelete.add(t);

                t.delete(); // 删除纪录
            }
        }

        items.removeAll(itemsToDelete);
    }
}

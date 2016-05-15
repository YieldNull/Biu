package com.bbbbiu.biu.db.search;

import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
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

    public abstract String getPath();

    public abstract File getFile();

    public abstract String getSize();

    public abstract String getParentDirName();

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
    public static Map<String, List<ModelItem>> queryModelItems(int type) {
        List<ModelItem> allRecords = new ArrayList<>();

        if (type == TYPE_MUSIC || type == TYPE_VIDEO) {
            List<MediaItem> records = SQLite.select()
                    .from(MediaItem.class)
                    .where(MediaItem_Table.type.eq(type))
                    .queryList();

            filter(records);

            allRecords.addAll(records);
        } else {
            List<FileItem> records = SQLite.select()
                    .from(FileItem.class)
                    .where(FileItem_Table.type.eq(type))
                    .queryList();

            filter(records);

            allRecords.addAll(records);
        }

        return sortModelItems(allRecords);
    }


    /**
     * 将Item按文件夹分类
     *
     * @param items 待分类列表
     * @param <T>   ModelItem
     * @return {“Folder”:itemList}
     */
    public static <T extends ModelItem> Map<String, List<ModelItem>> sortModelItems(List<T> items) {
        Map<String, List<ModelItem>> dirItemMap = new HashMap<>();

        for (ModelItem item : items) {
            String pDirName = item.getParentDirName();
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
    public static <T extends ModelItem> void filter(List<T> items) {
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

    public static <T extends ModelItem> void storeItems(List<T> items) {
        for (ModelItem item : items) {
            item.save();
        }
    }
}

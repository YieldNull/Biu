package com.bbbbiu.biu.db.search;

import com.bbbbiu.biu.util.StorageUtil;
import com.raizlabs.android.dbflow.sql.language.SQLCondition;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库基类，提供公有接口
 * <p/>
 * Created by YieldNull at 4/18/16
 */
public abstract class ModelItem extends BaseModel {

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
        return sortItemWithDir(query(type));
    }


    /**
     * 从数据库中读取指定类型的文件
     *
     * @param type type 类型
     * @return 所有记录，剔除不存在的文件
     */
    public static List<ModelItem> query(int type) {
        List<ModelItem> allRecords = new ArrayList<>();

        if (type == StorageUtil.TYPE_MUSIC || type == StorageUtil.TYPE_VIDEO) {
            List<MediaItem> records = SQLite.select()
                    .from(MediaItem.class)
                    .where(MediaItem_Table.type.eq(type))
                    .queryList();

            removeNotExisting(records);

            allRecords.addAll(records);
        } else {
            SQLCondition condition;
            if (type == StorageUtil.TYPE_DOC) {
                condition = FileItem_Table.type.in(StorageUtil.TYPE_WORD, StorageUtil.TYPE_PPT,
                        StorageUtil.TYPE_EXCEL, StorageUtil.TYPE_PDF);
            } else {
                condition = FileItem_Table.type.eq(type);
            }

            List<FileItem> records = SQLite.select().from(FileItem.class)
                    .where(condition)
                    .queryList();

            removeNotExisting(records);

            allRecords.addAll(records);
        }

        return allRecords;
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
    static <T extends ModelItem> void removeNotExisting(List<T> items) {
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

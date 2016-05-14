package com.bbbbiu.biu.db.search;

import com.orm.SugarRecord;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by YieldNull at 4/18/16
 */
public abstract class ModelItem extends SugarRecord {
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


    /**
     * "重载" {@link SugarRecord#find(Class, String, String...)}
     * <p/>
     * 将在文件系统中不存在的查询结果剔除
     */
    public static <T> List<T> find(Class<T> type, String whereClause, String... whereArgs) {
        List<T> items = SugarRecord.find(type, whereClause, whereArgs, null, null, null);

        List<T> itemsToDelete = new ArrayList<>();

        for (T t : items) {
            File file = new File(((ModelItem) t).getPath());
            if (!file.exists()) {
                itemsToDelete.add(t);
            }
        }

        items.removeAll(itemsToDelete);

        return items;
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
            List<MediaItem> records = find(MediaItem.class, "type=?", String.valueOf(type));
            allRecords.addAll(records);
        } else {
            List<FileItem> records = find(FileItem.class, "type=?", String.valueOf(type));
            allRecords.addAll(records);
        }

        Map<String, List<ModelItem>> dirItemMap = new HashMap<>();

        for (ModelItem item : allRecords) {
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
}

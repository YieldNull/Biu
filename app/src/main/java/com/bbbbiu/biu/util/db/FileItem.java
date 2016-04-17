package com.bbbbiu.biu.util.db;

import com.bbbbiu.biu.util.SearchUtil;
import com.orm.SugarRecord;
import com.orm.dsl.Unique;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by YieldNull at 4/15/16
 */
public class FileItem extends SugarRecord {
    public static final int TYPE_MUSIC = 1;
    public static final int TYPE_VIDEO = 2;
    public static final int TYPE_IMG = 3;
    public static final int TYPE_ARCHIVE = 4;
    public static final int TYPE_WORD = 5;
    public static final int TYPE_EXCEL = 6;
    public static final int TYPE_PPT = 7;
    public static final int TYPE_PDF = 8;

    public static final int TYPE_APK = 9; // APK 安装包

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
     * @param type    在{@link SearchUtil} 中的文件分类
     * @param pathSet 文件绝对路径集合
     */
    public static void storeFile(int type, Set<String> pathSet) {
        for (String path : pathSet) {
            new FileItem(path, type).save();
        }
    }

    public static boolean loadFile(int type, List<FileItem> all, Map<String, List<FileItem>> dirMap) {

        if (all == null) {
            all = new ArrayList<>();
        }

        if (dirMap == null) {
            dirMap = new HashMap<>();
        }

        all.addAll(FileItem.getFileList(type));

        // 从未扫描或者说是没有？TODO 加以区分
        if (all.size() == 0) {
            return false;
        }

        // 按文件夹分类
        dirMap.clear();
        for (FileItem item : all) {
            String pDirName = item.getParentDir();
            List<FileItem> list = dirMap.get(pDirName);

            if (list == null) {
                list = new ArrayList<>();
                dirMap.put(pDirName, list);
            }

            list.add(item);
        }


        // 加到列表中
        all.clear();
        for (Map.Entry<String, List<FileItem>> entry : dirMap.entrySet()) {
            List<FileItem> list = entry.getValue();
            if (list.size() > 0) {
                all.add(null);
                all.addAll(list);
            }
        }


//        Collections.sort(all, mComparator); TODO 排序
        return true;
    }

    /**
     * 获取对应分类的所有文件
     *
     * @param type 在{@link SearchUtil} 中的文件分类
     * @return 文件路径集合。该分类下没有文件则返回空Set()，没有该分类则返回null
     */
    public static List<FileItem> getFileList(int type) {
        return FileItem.find(FileItem.class, "type=?", String.valueOf(type));
    }

    public File getFile() {
        return new File(path);
    }

    public String getFileName() {
        return getFile().getName();
    }

    public String getParentDir() {
        return getFile().getParentFile().getName();
    }

}

package com.bbbbiu.biu.util;

import com.bbbbiu.biu.util.dbmodel.FileCate;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by YieldNull at 4/15/16
 */
public class DBUtil {
    private static final String TAG = DBUtil.class.getSimpleName();

    /**
     * 存储将全盘扫描之后得到的文件分类
     *
     * @param searchType 在{@link SearchUtil} 中的文件分类
     * @param pathSet    文件绝对路径集合
     */
    public static void storeFileToCategory(int searchType, Set<String> pathSet) {
        FileCate.deleteAll(FileCate.class);

        for (String path : pathSet) {
            new FileCate(path, searchType).save();
        }
    }

    /**
     * 获取对应分类的所有文件
     *
     * @param searchType 在{@link SearchUtil} 中的文件分类
     * @return 文件路径集合。该分类下没有文件则返回空Set()，没有该分类则返回null
     */
    public static Set<String> getFileFromCategory(int searchType) {
        Set<String> fileSet = new HashSet<>();

        for (FileCate cate : FileCate.find(FileCate.class, "type=?", String.valueOf(searchType))) {
            fileSet.add(cate.path);
        }
        return fileSet;
    }

    /**
     * 存储已安装的apk 路径
     *
     * @param sysApk    更新过的系统apk
     * @param normalApk 普通apk
     */
    public static void storeApkInstalled(Set<String> sysApk, Set<String> normalApk) {
        for (String path : sysApk) {
            new FileCate(path, SearchUtil.TYPE_APK_SYS).save();
        }
        for (String path : normalApk) {
            new FileCate(path, SearchUtil.TYPE_APK_NORMAL).save();
        }

    }

    /**
     * 获取系统apk
     *
     * @return 已更新过的系统APK
     */
    public static Set<String> getInstalledSysApk() {
        Set<String> fileSet = new HashSet<>();

        for (FileCate cate : FileCate.find(FileCate.class, "type=?", String.valueOf(SearchUtil.TYPE_APK_SYS))) {
            fileSet.add(cate.path);
        }
        return fileSet;
    }

    /**
     * 获取普通APK
     *
     * @return 普通已安装的APK
     */
    public static Set<String> getInstalledNormalApk() {
        Set<String> fileSet = new HashSet<>();

        for (FileCate cate : FileCate.find(FileCate.class, "type=?", String.valueOf(SearchUtil.TYPE_APK_NORMAL))) {
            fileSet.add(cate.path);
        }
        return fileSet;
    }
}

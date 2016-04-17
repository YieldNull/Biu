package com.bbbbiu.biu.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bbbbiu.biu.util.db.ApkItem;
import com.bbbbiu.biu.util.db.FileItem;
import com.google.common.collect.ImmutableMap;
import com.orm.SugarRecord;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * 全盘扫描，并将文件分类
 * 扫描以安装应用
 * <p/>
 * Created by YieldNull at 4/7/16
 */
public class SearchUtil {
    private static final String TAG = SearchUtil.class.getSimpleName();

    /**
     * 文件类型与后缀名
     */
    private static final Map<Integer, List<String>> typeExtensionMap = new ImmutableMap.Builder<Integer, List<String>>().
            put(FileItem.TYPE_APK, StorageUtil.EXTENSION_APK).
            put(FileItem.TYPE_MUSIC, StorageUtil.EXTENSION_MUSIC).
            put(FileItem.TYPE_VIDEO, StorageUtil.EXTENSION_VIDEO).
            put(FileItem.TYPE_IMG, StorageUtil.EXTENSION_IMG).
            put(FileItem.TYPE_ARCHIVE, StorageUtil.EXTENSION_ARCHIVE).
            put(FileItem.TYPE_WORD, StorageUtil.EXTENSION_WORD).
            put(FileItem.TYPE_EXCEL, StorageUtil.EXTENSION_EXCEL).
            put(FileItem.TYPE_PPT, StorageUtil.EXTENSION_PPT).
            put(FileItem.TYPE_PDF, StorageUtil.EXTENSION_PDF)
            .build();

    /**
     * 各类别文件大小的阈值
     */
    private static final long THRESHOLD_IMG = 1024 * 20;// 20KB
    private static final long THRESHOLD_VIDEO = 1024 * 1024 * 5;//5MB
    private static final long THRESHOLD_ARCHIVE = 1024 * 10;//10KB;
    private static final long THRESHOLD_MUSIC = 1024 * 1024;// 1MB;

    /**
     * 文件类型与阈值
     */
    private static final Map<Integer, Long> typeThresholdMap = ImmutableMap.of(
            FileItem.TYPE_IMG, THRESHOLD_IMG,
            FileItem.TYPE_VIDEO, THRESHOLD_VIDEO,
            FileItem.TYPE_ARCHIVE, THRESHOLD_ARCHIVE,
            FileItem.TYPE_MUSIC, THRESHOLD_MUSIC
    );


    /**
     * 全盘扫描各种文件类型，并持久化
     * <p/>
     * 分内部储存和外部储存扫描。不直接从根目录“/”下扫描，
     * 因为有时候会莫名其妙地访问不了或者是出现将SD卡挂载到多个文件夹下的情况
     */
    public static void scanDisk(Context context) {
        Log.i(TAG, currentThread() + "Start scanning disk");

        Integer[] typeArr = typeExtensionMap.keySet().toArray(new Integer[typeExtensionMap.size()]);

        int count = StorageUtil.getExternalDirCount(context);
        Map<Integer, Set<String>> result;

        if (count == 0) {
            result = BFSSearch(new File(StorageUtil.PATH_STORAGE), typeArr);
        } else if (count == 1) {
            result = BFSSearch(StorageUtil.getRootDir(context, StorageUtil.TYPE_EXTERNAL), typeArr);
        } else {
            result = BFSSearch(StorageUtil.getRootDir(context, StorageUtil.TYPE_INTERNAL), typeArr);
            BFSSearch(result, StorageUtil.getRootDir(context, StorageUtil.TYPE_EXTERNAL), typeArr);
        }


        // 清空之前的纪录
        // TODO 有个问题，要是正在用户正在查询的时候进行扫描，把纪录删了怎么办
        SugarRecord.deleteAll(FileItem.class);
        SugarRecord.deleteAll(ApkItem.class);

        Log.i(TAG, currentThread() + "Storing files to Database");

        for (Map.Entry<Integer, Set<String>> cateEntry : result.entrySet()) {
            if (cateEntry.getKey() == FileItem.TYPE_APK) {
                ApkItem.storeApk(context, ApkItem.TYPE_APK_STANDALONE, cateEntry.getValue());
            } else {
                FileItem.storeFile(cateEntry.getKey(), cateEntry.getValue());
            }
        }

        Log.i(TAG, currentThread() + "Finish storing");
    }

    /**
     * 扫描已安装的APK列表，并持久化
     * <p/>
     * 包括更新过的系统应用以及用户安装的普通应用
     *
     * @param context context
     */
    public static void scanApkInstalled(Context context) {
        Log.i(TAG, currentThread() + "Start scanning installed apk");

        PackageManager manager = context.getPackageManager();
        Set<String> sysApkSet = new HashSet<>();
        Set<String> normalApkSet = new HashSet<>();


        List<PackageInfo> infoList = manager.getInstalledPackages(PackageManager.GET_ACTIVITIES);
        for (PackageInfo info : infoList) {

            // 获取已更新的系统应用
            if ((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0 &&
                    (info.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) {
                continue;
            }

            String path = info.applicationInfo.sourceDir;


            if ((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0) {
                sysApkSet.add(path);
            } else {
                normalApkSet.add(path);
            }
        }

        Log.i(TAG, currentThread() + "Storing apk paths to Database");

        ApkItem.storeApk(context, ApkItem.TYPE_APK_SYSTEM, sysApkSet);
        ApkItem.storeApk(context, ApkItem.TYPE_APK_NORMAL, normalApkSet);

        Log.i(TAG, currentThread() + "Finish storing");
    }

    /**
     * 在指定目录搜索指定类型的文件
     *
     * @param root 目录
     * @param type 文件类型
     * @return 文件集合
     * @see FileItem
     */
    public static Set<String> searchFileAt(File root, int type) {
        return BFSSearch(root, type).get(type);
    }

    /**
     * 在指定目录下，宽度优先搜索指定类型的文件
     *
     * @param root     根目录
     * @param typeList 文件类型
     * @return 各种文件类型对应的文件路径集合
     */
    private static Map<Integer, Set<String>> BFSSearch(File root, Integer... typeList) {
        HashMap<Integer, Set<String>> destMap = new HashMap<>();
        BFSSearch(destMap, root, typeList);
        return destMap;
    }

    /**
     * 在指定目录下，宽度优先搜索指定类型的文件。将结果存入destMap
     *
     * @param destMap  结果将存入此Map
     * @param root     根目录
     * @param typeList 文件类型
     */
    private static void BFSSearch(@NonNull Map<Integer, Set<String>> destMap, @NonNull File root, Integer... typeList) {
        Log.i(TAG, currentThread() + "Start scanning all files under \"" + root.getAbsolutePath() + "\"");

        HashMap<String, Integer> extensionMap = new HashMap<>();

        for (int type : typeList) {
            if (destMap.get(type) == null) {
                Set<String> pathSet = new HashSet<>();
                destMap.put(type, pathSet);
            }

            for (String ext : getFileTypeExtension(type)) {
                extensionMap.put(ext, type);
            }
        }

        // BFS
        Queue<File> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            File f = queue.remove();

            if (!f.canRead() || f.isHidden()) { // 不扫描隐藏文件以及不可读的文件
                continue;
            }

            if (f.isDirectory()) {
                // 不知道为什么，空文件夹会返回null "/storage/emulated/0/baidu/pushservice"
                File[] files = f.listFiles();
                if (files == null) {
                    continue;
                }

                // 获取Canonical Path
                Set<File> subFiles = new HashSet<>();


                for (File file : files) {
                    if (f.isHidden()) {
                        continue;
                    }

                    try {
                        String cp = file.getCanonicalPath();
                        if (!cp.equals(StorageUtil.PATH_LEGACY)) { // 排除 LEGACY
                            subFiles.add(new File(cp));
                        }
                    } catch (IOException e) {
                        Log.w(TAG, e);
                    }
                }
                queue.addAll(subFiles);

            } else {
                String path = "";
                try {
                    path = f.getCanonicalPath(); // 获取绝对路径，避免符号链接浪费时间
                } catch (IOException e) {
                    Log.w(TAG, e.toString());
                }
                String extension = StorageUtil.getFileExtension(path);

                Integer type = extensionMap.get(extension); // 获取类别

                // 判断大小
                if (type != null && hasMatchThreshold(type, f.length())) {
                    destMap.get(type).add(path);//加入指定类别
                }
            }
        }

        Log.i(TAG, currentThread() + "Finish scanning");
    }


    /**
     * 获取指定文件类型的后缀名列表
     *
     * @param type 类型
     * @return 后缀名列表
     */
    private static List<String> getFileTypeExtension(int type) {
        List<String> result = typeExtensionMap.get(type);

        if (result == null) {
            result = new ArrayList<>();
        }

        return result;
    }

    /**
     * 图片视频等文件大小是否达到阈值，为了过滤小文件
     *
     * @param type 文件类型
     * @param size 文件大小 bytes
     * @return 是否达到阈值
     */
    private static boolean hasMatchThreshold(Integer type, long size) {
        if (type == null) {
            return false;
        }

        Long threshold = typeThresholdMap.get(type);

        return threshold == null || size >= threshold;
    }

    private static String currentThread() {
        return "[" + Thread.currentThread().getName() + "] ";
    }
}

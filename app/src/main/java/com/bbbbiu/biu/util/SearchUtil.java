package com.bbbbiu.biu.util;

import android.content.Context;
import android.util.Log;

import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Created by YieldNull at 4/7/16
 */
public class SearchUtil {
    private static final String TAG = SearchUtil.class.getSimpleName();

    public static final int TYPE_APK = 0;
    public static final int TYPE_MUSIC = 1;
    public static final int TYPE_VIDEO = 2;
    public static final int TYPE_IMG = 3;
    public static final int TYPE_ARCHIVE = 4;
    public static final int TYPE_DOC = 5;
    public static final int TYPE_WORD = 6;
    public static final int TYPE_EXCEL = 7;
    public static final int TYPE_PPT = 8;
    public static final int TYPE_PDF = 9;

    /**
     * 文件类型与后缀名
     */
    private static final Map<Integer, List<String>> typeExtensionMap = new ImmutableMap.Builder<Integer, List<String>>().
            put(TYPE_APK, StorageUtil.EXTENSION_APK).
            put(TYPE_MUSIC, StorageUtil.EXTENSION_MUSIC).
            put(TYPE_VIDEO, StorageUtil.EXTENSION_VIDEO).
            put(TYPE_IMG, StorageUtil.EXTENSION_IMG).
            put(TYPE_ARCHIVE, StorageUtil.EXTENSION_ARCHIVE).
            put(TYPE_WORD, StorageUtil.EXTENSION_WORD).
            put(TYPE_EXCEL, StorageUtil.EXTENSION_EXCEL).
            put(TYPE_PPT, StorageUtil.EXTENSION_PPT).
            put(TYPE_PDF, StorageUtil.EXTENSION_PDF)
            .build();


    private static final long THRESHOLD_IMG = 1024 * 20;// 20KB
    private static final long THRESHOLD_VIDEO = 1024 * 1024 * 5;//5MB
    private static final long THRESHOLD_ARCHIVE = 1024 * 10;//10KB;
    private static final long THRESHOLD_MUSIC = 1024 * 1024;// 1MB;

    /**
     * 文件类型与阈值
     */
    private static final Map<Integer, Long> typeThresholdMap = ImmutableMap.of(
            TYPE_IMG, THRESHOLD_IMG,
            TYPE_VIDEO, THRESHOLD_VIDEO,
            TYPE_ARCHIVE, THRESHOLD_ARCHIVE,
            TYPE_MUSIC, THRESHOLD_MUSIC
    );


    /**
     * 全盘扫描各种文件类型，存入数据库
     */
    public static void scanDisk(Context context) {
        Log.i(TAG, "Start scanning disk");

        Map<Integer, Set<String>> result = BFSSearch(new File(StorageUtil.PATH_STORAGE),
                typeExtensionMap.keySet().toArray(new Integer[typeExtensionMap.size()]));

        for (Map.Entry<Integer, Set<String>> cateEntry : result.entrySet()) {
            PreferenceUtil.storeFileToCategory(context, cateEntry.getKey(), cateEntry.getValue());
        }

        Log.i(TAG, "Store files to SharedPreference");
    }

    /**
     * 在指定目录搜索指定类型的文件
     *
     * @param root 目录
     * @param type 文件类型 {@value TYPE_APK} ...等等
     * @return 文件集合
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
        Log.i(TAG, "Start scanning all files under \"" + root.getAbsolutePath() + "\"");

        HashMap<Integer, Set<String>> resultMap = new HashMap<>();
        HashMap<String, Integer> extensionMap = new HashMap<>();

        for (int type : typeList) {
            Set<String> pathSet = new HashSet<>();
            resultMap.put(type, pathSet);

            for (String ext : getFileTypeExtension(type)) {
                extensionMap.put(ext, type);
            }
        }


        // BFS
        Queue<File> queue = new LinkedList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            File f = queue.remove();

            if (!f.canRead()) {
                continue;
            }

            if (f.isDirectory()) {
                Collections.addAll(queue, f.listFiles());
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
                    resultMap.get(type).add(path);//加入指定类别

                }
            }
        }

        Log.i(TAG, "Finish scanning");
        return resultMap;
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
}

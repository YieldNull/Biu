package com.bbbbiu.biu.util;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
    public static final int TYPE_DOC = 4;
    public static final int TYPE_ARCHIVE = 5;

    public static Set<String> searchFile(Context context, int type) {
        Set<String> set = searchFileAt(StorageUtil.getRootDir(context, StorageUtil.TYPE_INTERNAL), type);

        if (StorageUtil.getExternalDirCount(context) == 2) {
            Set<String> set2 = SearchUtil.searchFileAt(StorageUtil.getRootDir(context, StorageUtil.TYPE_EXTERNAL), type);
            set.addAll(set2);
        }
        return set;
    }

    public static Set<String> searchFileAt(File root, int type) {
        List<String> extList = getFileTypeExtension(type);
        Log.i(TAG, "Start search files " + extList.toString());

        Set<String> pathSet = new HashSet<>();


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
                    path = f.getCanonicalPath();
                } catch (IOException e) {
                    Log.w(TAG, e.toString());
                }
                String extension = StorageUtil.getFileExtension(path);

                // 匹配后缀名
                if (extList.contains(extension.toLowerCase())) {
                    pathSet.add(path);
                }
            }
        }

        Log.i(TAG, "Finish searching. Count: " + pathSet.size());
        return pathSet;
    }

    private static List<String> getFileTypeExtension(int type) {
        switch (type) {
            case TYPE_APK:
                return StorageUtil.EXTENSION_APK;
            case TYPE_MUSIC:
                return StorageUtil.EXTENSION_MUSIC;
            case TYPE_VIDEO:
                return StorageUtil.EXTENSION_VIDEO;
            case TYPE_IMG:
                return StorageUtil.EXTENSION_IMG;
            case TYPE_DOC:
                return StorageUtil.EXTENSION_DOC;
            case TYPE_ARCHIVE:
                return StorageUtil.EXTENSION_ARCHIVE;
            default:
                return new ArrayList<>();
        }
    }
}

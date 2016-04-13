package com.bbbbiu.biu.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by YieldNull at 3/26/16
 */
public class PreferenceUtil {
    private static final String TAG = PreferenceUtil.class.getSimpleName();

    private static final String SCHEMA_FILES_TO_SEND = "FilesToSend";
    private static final String KEY_FILE_PATHS_TO_SEND = "FILE_TO_SEND";

    private static final String SCHEMA_FILE_CATEGORY = "FileCategory";
    private static final String KEY_CATEGORY_SCAN_TIME = "SCAN_TIME";
    private static final String KEY_CATEGORY_APK = "APK";
    private static final String KEY_CATEGORY_MUSIC = "MUSIC";
    private static final String KEY_CATEGORY_VIDEO = "VIDEO";
    private static final String KEY_CATEGORY_IMG = "IMG";
    private static final String KEY_CATEGORY_ARCHIVE = "ARCHIVE";
    private static final String KEY_CATEGORY_PDF = "PDF";
    private static final String KEY_CATEGORY_WORD = "WORD";
    private static final String KEY_CATEGORY_EXCEL = "EXCEL";
    private static final String KEY_CATEGORY_PPT = "PPT";


    private static final String SCHEMA_APK_INSTALLED = "ApkInstalled";
    private static final String KEY_APK_SCAN_TIME = "SCAN_TIME";
    private static final String KEY_APK_SYSTEM = "SYSTEM";
    private static final String KEY_APK_NORMAL = "NORMAL";

    /**
     * 文件类型与SharedPreferences中的key
     */
    public static final Map<Integer, String> typeKeyMap = new ImmutableMap.Builder<Integer, String>().
            put(SearchUtil.TYPE_APK, KEY_CATEGORY_APK).
            put(SearchUtil.TYPE_MUSIC, KEY_CATEGORY_MUSIC).
            put(SearchUtil.TYPE_VIDEO, KEY_CATEGORY_VIDEO).
            put(SearchUtil.TYPE_IMG, KEY_CATEGORY_IMG).
            put(SearchUtil.TYPE_ARCHIVE, KEY_CATEGORY_ARCHIVE).
            put(SearchUtil.TYPE_WORD, KEY_CATEGORY_WORD).
            put(SearchUtil.TYPE_EXCEL, KEY_CATEGORY_EXCEL).
            put(SearchUtil.TYPE_PPT, KEY_CATEGORY_PPT).
            put(SearchUtil.TYPE_PDF, KEY_CATEGORY_PDF)
            .build();

    /**
     * 因为发送文件到电脑要经过三个Activity，懒得传，因此就先持久化
     *
     * @param context context
     * @param files   files
     */
    public static void storeFilesToSend(Context context, List<File> files) {
        SharedPreferences preferences = context.getSharedPreferences(SCHEMA_FILES_TO_SEND, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        //先清除之前的
        editor.clear();
        editor.apply();

        // 加入当前的
        HashSet<String> filePathSet = new HashSet<>();
        for (File file : files) {
            filePathSet.add(file.getAbsolutePath());
        }

        editor.putStringSet(KEY_FILE_PATHS_TO_SEND, filePathSet);
        editor.apply();
    }

    /**
     * 获取要发送的文件列表
     *
     * @param context context
     * @return 文件集合
     */
    public static Set<String> getFilesToSend(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(SCHEMA_FILES_TO_SEND, Context.MODE_PRIVATE);

        return preferences.getStringSet(KEY_FILE_PATHS_TO_SEND, null);
    }


    /**
     * 存储将全盘扫描之后得到的文件分类
     *
     * @param context    context
     * @param searchType 在{@link SearchUtil} 中的文件分类
     * @param pathSet    文件绝对路径集合
     */
    public static void storeFileToCategory(Context context, int searchType, Set<String> pathSet) {
        SharedPreferences preferences = context.getSharedPreferences(SCHEMA_FILE_CATEGORY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        String category = typeKeyMap.get(searchType);
        editor.putStringSet(category, pathSet);

        editor.putLong(KEY_CATEGORY_SCAN_TIME, System.currentTimeMillis());

        editor.apply();
    }

    /**
     * 获取对应分类的所有文件
     *
     * @param context    context
     * @param searchType 在{@link SearchUtil} 中的文件分类
     * @return 文件路径集合。该分类下没有文件则返回空Set()，没有该分类则返回null
     */
    public static Set<String> getFileFromCategory(Context context, int searchType) {
        SharedPreferences preferences = context.getSharedPreferences(SCHEMA_FILE_CATEGORY, Context.MODE_PRIVATE);

        String category = typeKeyMap.get(searchType);
        return preferences.getStringSet(category, null);
    }

    /**
     * 存储已安装的apk 路径
     *
     * @param context   context
     * @param sysApk    更新过的系统apk
     * @param normalApk 普通apk
     */
    public static void storeApkInstalled(Context context, Set<String> sysApk, Set<String> normalApk) {
        SharedPreferences preferences = context.getSharedPreferences(SCHEMA_APK_INSTALLED, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putStringSet(KEY_APK_SYSTEM, sysApk);
        editor.putStringSet(KEY_APK_NORMAL, normalApk);

        editor.putLong(KEY_APK_SCAN_TIME, System.currentTimeMillis());

        editor.apply();
    }

    /**
     * 获取系统apk
     *
     * @param context context
     * @return 已更新过的系统APK
     */
    public static Set<String> getInstalledSysApk(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(SCHEMA_APK_INSTALLED, Context.MODE_PRIVATE);

        return preferences.getStringSet(KEY_APK_SYSTEM, null);
    }

    /**
     * 获取普通APK
     *
     * @param context context
     * @return 普通已安装的APK
     */
    public static Set<String> getInstalledNormalApk(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(SCHEMA_APK_INSTALLED, Context.MODE_PRIVATE);

        return preferences.getStringSet(KEY_APK_NORMAL, null);
    }

}

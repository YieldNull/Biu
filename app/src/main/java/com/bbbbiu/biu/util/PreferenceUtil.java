package com.bbbbiu.biu.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by YieldNull at 3/26/16
 */
public class PreferenceUtil {
    private static final String TAG = PreferenceUtil.class.getSimpleName();

    private static final String SCHEMA_FILES_TO_SEND = "FilesToSend";
    private static final String KEY_FILE_PATHS_TO_SEND = "com.bbbbiu.biu.util.PreferenceUtil.key.FILE_PATHS_TO_SEND";


    /**
     * 因为发送文件到电脑要经过三个Activity，懒得传，因此就先持久化
     *
     * @param context
     * @param files
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

    public static Set<String> getFilesToSend(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(SCHEMA_FILES_TO_SEND, Context.MODE_PRIVATE);

        return preferences.getStringSet(KEY_FILE_PATHS_TO_SEND, null);
    }
}

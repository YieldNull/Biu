package com.bbbbiu.biu.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Set;

/**
 * Created by YieldNull at 3/26/16
 */
public class PreferenceUtil {
    private static final String TAG = PreferenceUtil.class.getSimpleName();

    private static final String SCHEMA_FILES_TO_SEND = "Temp";
    private static final String KEY_FILE_PATHS_TO_SEND = "FILE_TO_SEND";

    /**
     * 因为发送文件到电脑要经过三个Activity，懒得传，因此就先持久化
     *
     * @param context context
     * @param files   文件路径列表
     */
    public static void storeFilesToSend(Context context, Set<String> files) {
        SharedPreferences preferences = context.getSharedPreferences(SCHEMA_FILES_TO_SEND, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putStringSet(KEY_FILE_PATHS_TO_SEND, files);
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

}

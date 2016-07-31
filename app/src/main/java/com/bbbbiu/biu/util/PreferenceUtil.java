package com.bbbbiu.biu.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.bbbbiu.biu.gui.transfer.FileItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

/**
 * Created by YieldNull at 3/26/16
 */
public class PreferenceUtil {
    private static final String TAG = PreferenceUtil.class.getSimpleName();

    private static final String SCHEMA_HELPER = "Helper";
    private static final String KEY_FILE_PATHS_TO_SEND = "FILE_TO_SEND";

    /**
     * 因为发送文件到电脑要经过三个Activity，懒得传，因此就先持久化
     *
     * @param context context
     * @param files   文件路径列表
     */
    public static void storeFilesToSend(Context context, Set<String> files) {
        SharedPreferences preferences = context.getSharedPreferences(SCHEMA_HELPER, Context.MODE_PRIVATE);
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
        SharedPreferences preferences = context.getSharedPreferences(SCHEMA_HELPER, Context.MODE_PRIVATE);

        return preferences.getStringSet(KEY_FILE_PATHS_TO_SEND, null);
    }

    public static ArrayList<FileItem> getFileItemsToSend(Context context) {
        ArrayList<FileItem> fileItems = new ArrayList<>();
        for (String path : PreferenceUtil.getFilesToSend(context)) {
            File file = new File(path);
            fileItems.add(new FileItem(file.getAbsolutePath(), StorageUtil.getFileNameToDisplay(context, file), file.length()));
        }
        return fileItems;
    }
}

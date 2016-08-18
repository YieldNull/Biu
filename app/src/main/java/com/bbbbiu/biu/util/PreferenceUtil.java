package com.bbbbiu.biu.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.bbbbiu.biu.gui.transfer.FileItem;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by YieldNull at 3/26/16
 */
public class PreferenceUtil {
    private static final String TAG = PreferenceUtil.class.getSimpleName();

    private static final String SCHEMA_HELPER = "Helper";
    private static final String KEY_FILE_URIS_TO_SEND = "FILE_TO_SEND";
    private static final String KEY_FIRST_RUN = "FIRST_RUN";

    /**
     * 是否第一次启动
     *
     * @param context context
     * @return 是否第一次启动
     */
    public static boolean isFirstRun(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(SCHEMA_HELPER, Context.MODE_PRIVATE);

        if (preferences.getBoolean(KEY_FIRST_RUN, true)) { // 首次运行时，返回默认值true

            // 设置为false
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(KEY_FIRST_RUN, false);
            editor.apply();

            return true;
        } else {
            return false;
        }
    }

    /**
     * 因为发送文件到电脑要经过三个Activity，懒得传，因此就先持久化
     *
     * @param context context
     * @param files   文件路径列表
     */
    public static void storeFilesToSend(Context context, Set<String> files) {
        SharedPreferences preferences = context.getSharedPreferences(SCHEMA_HELPER, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        Set<String> uris = new HashSet<>();
        for (String path : files) {
            if (path.startsWith("/")) {
                uris.add("file://" + path);
            } else {
                uris.add(path);
            }
        }

        editor.putStringSet(KEY_FILE_URIS_TO_SEND, uris);
        editor.apply();
    }


    /**
     * 获取要发送的文件
     *
     * @param context context
     * @return 文件列表
     */
    public static ArrayList<FileItem> getFileItemsToSend(Context context) {
        ArrayList<FileItem> fileItems = new ArrayList<>();
        for (String uString : PreferenceUtil.getFilesToSend(context)) {
            Uri uri = Uri.parse(uString);

            long size;
            String name;

            if (uri.getScheme().equals(ContentResolver.SCHEME_FILE)) {
                File file = new File(uri.getPath());
                size = file.length();
                name = StorageUtil.getFileNameToDisplay(context, file);
            } else {
                Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
                if (cursor == null) {
                    continue;
                }

                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                cursor.moveToFirst();

                name = cursor.getString(nameIndex);
                size = cursor.getLong(sizeIndex);

                cursor.close();
            }
            fileItems.add(new FileItem(uString, name, size));
        }
        return fileItems;
    }


    /**
     * 获取要发送的文件Uri
     *
     * @param context context
     * @return 文件Uri集合
     */
    private static Set<String> getFilesToSend(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(SCHEMA_HELPER, Context.MODE_PRIVATE);

        return preferences.getStringSet(KEY_FILE_URIS_TO_SEND, null);
    }
}

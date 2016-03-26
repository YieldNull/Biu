package com.bbbbiu.biu.util;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.ContextCompat;

import java.io.File;

public class Storage {

    private static final String TAG = Storage.class.getSimpleName();

    public static File getDownloadDir(Context context) {
        File downloads;
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            File[] externals = ContextCompat.getExternalFilesDirs(context, "Download");
            if (externals.length > 1 && (externals[1] != null)) {
                downloads = externals[1];
            } else {
                downloads = externals[0];
            }
        } else {
            downloads = context.getExternalFilesDir("Download");
        }

        return downloads;
    }

    public static int getExternalDirCount(Context context) {
        File[] externals = ContextCompat.getExternalFilesDirs(context, null);
        boolean primaryMounted = isPrimaryExternalStorageMounted();

        if (externals.length == 1) {
            return primaryMounted ? 1 : 0;
        } else {
            return externals[1] == null ? 1 : 2;
        }
    }

    public static File getSecondaryExternalDir(Context context) {
        File[] files = ContextCompat.getExternalFilesDirs(context, null);
        if (files.length != 2) {
            return null;
        }
        return ContextCompat.getExternalFilesDirs(context, null)[1];
    }

    public static String getRealFilePath(Context context, Uri uri) {
        return uri.getPath();
    }


    public static String getReadableSize(long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "KMGTPE".charAt(exp - 1) + "";//+ "i";
        return String.format("%.1f%sB", bytes / Math.pow(unit, exp), pre);
    }

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    public static boolean isPrimaryExternalStorageMounted() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }
}

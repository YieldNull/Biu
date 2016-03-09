package com.bbbbiu.biu.util;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import java.io.File;

public class StorageUtil {

    public static File getDownloadDir(Context context) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return null;
        }

        File downloads;
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            File[] externals = context.getExternalFilesDirs("Download");
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

    public static String getRealFilePath(Context context, Uri uri) {
        return uri.getPath();
    }


    public static String getReadableSize(long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "KMGTPE".charAt(exp - 1) + "";//+ "i";
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}

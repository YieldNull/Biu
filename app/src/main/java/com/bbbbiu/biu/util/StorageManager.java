package com.bbbbiu.biu.util;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import com.bbbbiu.biu.httpd.util.ContentType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

public class StorageManager {

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

}

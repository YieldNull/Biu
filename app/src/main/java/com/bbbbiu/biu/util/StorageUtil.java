package com.bbbbiu.biu.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.bbbbiu.biu.R;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StorageUtil {

    private static final String TAG = StorageUtil.class.getSimpleName();

    public static final int TYPE_INTERNAL = 0;
    public static final int TYPE_EXTERNAL = 1;


    public static final List<String> EXTENSION_APK = Collections.singletonList("apk");

    public static final List<String> EXTENSION_MUSIC = Arrays.asList(
            "acc", "flac", "mp3", "ogg", "wav", "ape"
    );

    public static final List<String> EXTENSION_VIDEO = Arrays.asList(
            "3gp", "mp4", "m4v", "mkv", "flv", "rmvb", "rm", "mov", "webm", "avi", "wmv"
    );
    public static final List<String> EXTENSION_IMG = Arrays.asList(
            "jpg", "gif", "png", "bmp", "webp", "jpeg"
    );
    public static final List<String> EXTENSION_DOC = Arrays.asList(
            "doc", "ppt", "xls", "docx", "pptx", "xlsx", "wps", "odt", "ods", "odp", "pdf"
    );
    public static final List<String> EXTENSION_ARCHIVE = Arrays.asList(
            "rar", "zip", "7z", "tar", "gz", "bz2", "xz", "lz", "lzma"
    );

    public static final List<String> EXTENSION_WORD = Arrays.asList("doc", "docx", "odt");
    public static final List<String> EXTENSION_EXCEL = Arrays.asList("xls", "xlsx", "ods");
    public static final List<String> EXTENSION_PPT = Arrays.asList("ppt", "pptx", "odp");
    public static final List<String> EXTENSION_PDF = Collections.singletonList("pdf");
    public static final List<String> EXTENSION_TEXT = Collections.singletonList("txt");


    public static String getRealFilePath(Context context, Uri uri) {
        return uri.getPath();
    }


    /**
     * 根据文件length生产可读字符串，如 **MB
     *
     * @param bytes 文件length，以bytes记
     * @return 字符串
     */
    @SuppressLint("DefaultLocale")
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


    /**
     * 获取下载目录。4.4有两个存储卡，就放到第二个。TODO 在设置里面给用户选择下载目录
     */
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

    /**
     * 获取可使用的（已挂载） ExternalFilesDirs 的数量
     */
    public static int getExternalDirCount(Context context) {
        File[] externals = ContextCompat.getExternalFilesDirs(context, null);
        boolean primaryMounted = isPrimaryExternalStorageMounted();

        if (externals.length == 1) {
            return primaryMounted ? 1 : 0;
        } else {
            return externals[1] == null ? 1 : 2;
        }
    }

    /**
     * 获取4.4的物理外置存储卡。
     */
    public static File getSecondaryExternalDir(Context context) {
        if (getExternalDirCount(context) != 2) {
            return null;
        }
        return ContextCompat.getExternalFilesDirs(context, null)[1];
    }


    /**
     * isPrimaryExternalStorageMounted。primary external storage指的是虚拟储存卡或物理储存卡
     * 各机型不同
     */
    public static boolean isPrimaryExternalStorageMounted() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * 是不是真的有外置物理存储卡，小米等一体机用getExternalStorageDirectory()获取的是手机内置的
     */
    public static boolean hasRealExternal(Context context) {
        int externalDirCount = getExternalDirCount(context);

        if (externalDirCount == 2) {
            return true;
        } else if (externalDirCount == 1) {
            if (!Environment.getExternalStorageDirectory().getAbsolutePath().contains("/emulated")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取手机储存或外置储存的根目录。
     * 如果手机储存有虚拟的存储卡，则返回其根目录，没有则返回File(“/”)
     *
     * @param context context
     * @param type    {@value TYPE_EXTERNAL,TYPE_INTERNAL}
     * @return File
     */
    public static File getRootDir(Context context, int type) {
        File root = null;
        int externalDirCount = getExternalDirCount(context);

        if (type == TYPE_INTERNAL) {
            if (getExternalDirCount(context) == 2) {  // 有两个外置，则用第一个表示手机存储，点击进入第一个外置
                root = Environment.getExternalStorageDirectory();
            } else {
                if (externalDirCount == 1 && (!hasRealExternal(context))) {  // 有一个外置,但是不是真的外置,emulated
                    root = Environment.getExternalStorageDirectory();// 点击进入外置
                } else {

                    // "/storage“ 目录里面有一些文件夹，很多是符号链接的
                    // 什么 usb emulated sdcard0 sdcard1 emmc等
                    File[] storage = new File("/storage").listFiles();
                    for (File f : storage) {
                        if ((f.canRead()) && (f.listFiles().length != 0) && (!f.getName().toLowerCase().contains("sdcard"))) {
                            root = f;
                            break;
                        }
                    }
                    if (root == null) {
                        root = new File("/");
                    }
                }
            }
        } else {
            if (externalDirCount == 2) {
                // 路径 /Android/data/com.bbbbiu.biu/files/
                File f = StorageUtil.getSecondaryExternalDir(context);// 有两个外置，进入第二个
                root = f.getParentFile().getParentFile().getParentFile().getParentFile();
            } else {
                root = Environment.getExternalStorageDirectory();// 有一个外置，进入第一个
            }
        }
        return root;
    }

    /**
     * 获取文件对应的图标
     *
     * @param context context
     * @param file    文件
     * @return drawable
     */
    public static Drawable getFileIcon(Context context, File file) {
        String extension = getFileExtension(file.getAbsolutePath()).toLowerCase();

        if (extension.equals("apk")) {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageArchiveInfo(file.getAbsolutePath(), 0);
            try {
                return pm.getApplicationIcon(info.packageName);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, e.toString());
                return context.getResources().getDrawable(R.drawable.ic_type_apk);
            }
        }


        int id = R.drawable.ic_type_default;

        if (file.isDirectory()) {
            id = R.drawable.ic_type_folder;
        } else if (EXTENSION_ARCHIVE.contains(extension)) {
            id = R.drawable.ic_type_archive;
        } else if (EXTENSION_WORD.contains(extension)) {
            id = R.drawable.ic_type_word;
        } else if (EXTENSION_EXCEL.contains(extension)) {
            id = R.drawable.ic_type_excel;
        } else if (EXTENSION_PPT.contains(extension)) {
            id = R.drawable.ic_type_ppt;
        } else if (EXTENSION_PDF.contains(extension)) {
            id = R.drawable.ic_type_pdf;
        } else if (EXTENSION_TEXT.contains(extension)) {
            id = R.drawable.ic_type_text;
        } else if (EXTENSION_MUSIC.contains(extension)) {
            id = R.drawable.ic_type_music;
        } else if (EXTENSION_VIDEO.contains(extension)) {
            id = R.drawable.ic_type_video;
        } else if (EXTENSION_IMG.contains(extension)) {
            id = R.drawable.ic_type_img;
        }

        return context.getResources().getDrawable(id);
    }

    /**
     * 获取文件扩展名。
     *
     * @param filename 文件名或路径
     * @return 没有则返回空串“”
     */
    public static String getFileExtension(String filename) {
        if (filename == null) {
            return null;
        }

        int extensionPos = filename.lastIndexOf('.');
        int lastSeparator = filename.lastIndexOf('/');
        int index = lastSeparator > extensionPos ? -1 : extensionPos;

        if (index == -1) {
            return "";
        } else {
            return filename.substring(index + 1);
        }
    }

    /**
     * 是否是视频文件
     */
    public static boolean isVideoFile(String path) {
        return EXTENSION_VIDEO.contains(getFileExtension(path));
    }

    /**
     * 是否是图片文件
     */
    public static boolean isImgFile(String path) {
        return EXTENSION_IMG.contains(getFileExtension(path));
    }
}

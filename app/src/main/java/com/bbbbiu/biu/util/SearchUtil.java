package com.bbbbiu.biu.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.bbbbiu.biu.db.search.ApkItem;
import com.bbbbiu.biu.db.search.FileItem;
import com.bbbbiu.biu.db.search.MediaItem;
import com.bbbbiu.biu.db.search.ModelItem;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 扫描各类文件,并存到数据库（{@link ModelItem#save()}）。已安装的APK从包管理器，其它从{@link MediaStore}
 * <p/>
 * 由于{@link MediaStore}中的数据不是实时的，因此查询出来的条目会先剔除出不存在的。
 * <p/>
 * 另因为{@link android.provider.MediaStore.Audio},{@link android.provider.MediaStore.Video}只有“Primary External Storage”中的数据,
 * 当用户有多个存储卡时（“Secondary External Storage”），用{@link android.provider.MediaStore.Files}可以查出其它所有存储器中的数据
 * <p/>
 * 我认为{@link MediaStore}对“Secondary External Storage”中 “Android/data”下面的图片、视频、音频都没有分类到Images，Audio，Video中去，
 * 而是直接纪录到Files里面了。由于聊天软件有很多表情啊什么的图片，就别用{@link android.provider.MediaStore.Files}查图片了。
 * {@link android.provider.MediaStore.Images}能查出绝大部分图片。
 * <p/>
 * Created by YieldNull at 4/7/16
 */
public class SearchUtil {
    private static final String TAG = SearchUtil.class.getSimpleName();


    /**
     * 利用包管理器，获取已安装的APK列表。
     *
     * @param context Context
     * @return map with key set: {@link ApkItem#TYPE_APK_SYSTEM}, {@link ApkItem#TYPE_APK_NORMAL}
     */
    public static Map<Integer, List<ApkItem>> scanInstalledApkItem(Context context) {
        Log.i(TAG, "Start scanning installed apk");

        Map<Integer, List<ApkItem>> map = new HashMap<>();

        List<ApkItem> sysApkList = new ArrayList<>();
        List<ApkItem> normalApkList = new ArrayList<>();

        PackageManager manager = context.getPackageManager();

        List<PackageInfo> infoList = manager.getInstalledPackages(PackageManager.GET_ACTIVITIES);
        for (PackageInfo info : infoList) {

            // 获取已更新的系统应用
            if ((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0 &&
                    (info.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) {
                continue;
            }

            String path = info.applicationInfo.sourceDir;
            String packageName = info.packageName;
            String name = (String) manager.getApplicationLabel(info.applicationInfo);

            ApkItem apkItem;

            if ((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0) {
                apkItem = new ApkItem(path, name, packageName, ApkItem.TYPE_APK_SYSTEM);
                sysApkList.add(apkItem);
            } else {
                apkItem = new ApkItem(path, name, packageName, ApkItem.TYPE_APK_NORMAL);
                normalApkList.add(apkItem);
            }

            apkItem.storeCachedIcon(context);
            apkItem.save(); // 存到数据库
        }


        Log.i(TAG, "Finish Scanning. Amount:" + (sysApkList.size() + normalApkList.size()));

        map.put(ApkItem.TYPE_APK_SYSTEM, sysApkList);
        map.put(ApkItem.TYPE_APK_NORMAL, normalApkList);

        return map;
    }

    /**
     * 从MediaStore读取APK安装包。剔除不存在者,以及已安装的。
     *
     * @param context context
     * @return APK列表
     */
    public static List<ApkItem> scanStandAloneApkItem(Context context) {
        Log.i(TAG, "Start scanning standalone apk");

        List<ApkItem> apkItemList = new ArrayList<>();

        for (String path : scanFileWithExtension(context, StorageUtil.EXTENSION_APK)) {
            String name = StorageUtil.getApkName(context, path);
            String packageName = StorageUtil.getApkPackageName(context, path);

            ApkItem apkItem = new ApkItem(path, name, packageName, ApkItem.TYPE_APK_STANDALONE);

            if (!apkItem.isInstalled(context)) {
                apkItemList.add(apkItem);

                apkItem.storeCachedIcon(context);
                apkItem.save();
            }
        }

        Log.i(TAG, "Finish Scanning. Amount:" + apkItemList.size());

        return apkItemList;
    }

    /**
     * 利用MediaStore扫描图片,剔除不存在者
     *
     * @param context context
     * @return 图片列表
     */
    public static List<FileItem> scanImageItem(Context context) {
        Log.i(TAG, "Start scanning image");

        List<FileItem> fileItems = new ArrayList<>();

        Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver contentResolver = context.getContentResolver();

        //只查询jpeg和png的图片
        Cursor cursor = contentResolver.query(
                contentUri,
                new String[]{MediaStore.Images.Media.DATA},
                MediaStore.Images.Media.MIME_TYPE + "=? or "
                        + MediaStore.Images.Media.MIME_TYPE + "=?",
                new String[]{"image/jpeg", "image/png"},
                null);

        if (cursor == null) {
            return fileItems;
        }

        while (cursor.moveToNext()) {
            //获取图片的路径
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));

            if (!(new File(path).exists())) {
                continue;
            }

            FileItem item = new FileItem(path, ModelItem.TYPE_IMG);
            fileItems.add(item);

            item.save();
        }
        cursor.close();

        Log.i(TAG, "Finish Scanning. Amount:" + fileItems.size());

        return fileItems;
    }

    /**
     * 利用MediaStore 扫描视频。剔除不存在者。
     *
     * @param context context
     * @return 视频列表
     */
    public static List<MediaItem> scanVideoItem(Context context) {
        Log.i(TAG, "Start scanning video");

        if (StorageUtil.hasSecondaryStorage()) {

            Set<String> pathSet = scanFileWithExtension(context, StorageUtil.EXTENSION_VIDEO);
            return getMediaItemFromPath(context, MediaItem.TYPE_VIDEO, pathSet);

        } else {
            List<MediaItem> fileItems = new ArrayList<>();

            Uri contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            ContentResolver contentResolver = context.getContentResolver();

            Cursor cursor = contentResolver.query(
                    contentUri,
                    new String[]{
                            MediaStore.Video.Media.DATA,
                            MediaStore.Video.Media.TITLE,
                            MediaStore.Video.Media.DURATION
                    },
                    null,
                    null,
                    null);

            if (cursor == null) {
                return fileItems;
            }


            while (cursor.moveToNext()) {
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE));
                String duration = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DURATION));

                if (!(new File(path).exists())) {
                    continue;
                }

                MediaItem item = new MediaItem(path, MediaItem.TYPE_VIDEO, title, null, duration);
                fileItems.add(item);

                item.save();

            }
            cursor.close();

            Log.i(TAG, "Finish Scanning. Amount:" + fileItems.size());
            return fileItems;
        }
    }

    /**
     * 从MediaStore扫描音乐。剔除不存在者
     *
     * @param context context
     * @return 音乐列表
     */
    public static List<MediaItem> scanMusicItem(Context context) {
        Log.i(TAG, "Start scanning music");

        if (StorageUtil.hasSecondaryStorage()) {
            Set<String> pathSet = scanFileWithExtension(context, StorageUtil.EXTENSION_MUSIC);
            return getMediaItemFromPath(context, MediaItem.TYPE_MUSIC, pathSet);

        } else {
            List<MediaItem> fileItems = new ArrayList<>();

            Uri contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            ContentResolver contentResolver = context.getContentResolver();

            Cursor cursor = contentResolver.query(
                    contentUri,
                    new String[]{
                            MediaStore.Audio.Media.DATA,
                            MediaStore.Audio.Media.TITLE,
                            MediaStore.Audio.Media.ARTIST,
                            MediaStore.Audio.Media.DURATION
                    },
                    null,
                    null,
                    null);

            if (cursor == null) {
                return fileItems;
            }


            while (cursor.moveToNext()) {
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                String duration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));

                if (!(new File(path).exists())) {
                    continue;
                }

                MediaItem item = new MediaItem(path, MediaItem.TYPE_MUSIC, title, artist, duration);
                fileItems.add(item);

                item.save();

            }
            cursor.close();

            Log.i(TAG, "Finish Scanning. Amount:" + fileItems.size());
            return fileItems;
        }
    }

    /**
     * 从MediaStore读取文档，剔除不存在者
     *
     * @param context context
     * @return 文档列表
     */
    public static List<FileItem> scanDocItem(Context context) {
        Log.i(TAG, "Start scanning doc");

        Set<String> pathList = scanFileWithExtension(context, StorageUtil.EXTENSION_DOC);

        List<FileItem> fileItems = new ArrayList<>();

        for (String path : pathList) {
            FileItem item = new FileItem(path, ModelItem.TYPE_DOC);
            fileItems.add(item);

            item.save();
        }

        Log.i(TAG, "Finish Scanning. Amount:" + fileItems.size());
        return fileItems;
    }

    /**
     * 从MediaStore读取缩文件，剔除不存在者
     *
     * @param context context
     * @return 压缩文件列表
     */
    public static List<FileItem> scanArchiveItem(Context context) {
        Log.i(TAG, "Start scanning archive");

        Set<String> pathList = scanFileWithExtension(context, StorageUtil.EXTENSION_ARCHIVE);

        List<FileItem> fileItems = new ArrayList<>();

        for (String path : pathList) {
            FileItem item = new FileItem(path, ModelItem.TYPE_ARCHIVE);
            fileItems.add(item);

            item.save();
        }

        Log.i(TAG, "Finish Scanning. Amount:" + fileItems.size());
        return fileItems;
    }

    /**
     * 从MediaStore读取相应后缀名的文件路径,剔除不存在的文件
     *
     * @param context    context
     * @param extensions 后缀名列表
     * @return 文件列表
     */
    private static Set<String> scanFileWithExtension(Context context, List<String> extensions) {
        Set<String> resultSet = new HashSet<>();

        List<String> args = new ArrayList<>();
        for (String ext : extensions) {
            args.add("%." + ext);
        }

        if (args.size() == 0) {
            return resultSet;
        }


        StringBuilder selectionBuilder = new StringBuilder();
        selectionBuilder.append(MediaStore.Files.FileColumns.DATA + " LIKE ?");
        for (int i = 1; i < args.size(); i++) {
            selectionBuilder.append(" OR " + MediaStore.Files.FileColumns.DATA + " LIKE ?");
        }

        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = MediaStore.Files.getContentUri("external");

        Cursor cursor = contentResolver.query(
                uri,
                new String[]{MediaStore.Files.FileColumns.DATA},
                selectionBuilder.toString(),
                args.toArray(new String[args.size()]),
                null
        );


        if (cursor == null) {
            return resultSet;
        }

        while (cursor.moveToNext()) {
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));

            File file = new File(path);

            if (file.exists()) {
                resultSet.add(path);
            }
        }

        cursor.close();

        return resultSet;
    }


    /**
     * 从视频、音频文件的路径生成{@link MediaItem}。主要是解析出时长等信息
     *
     * @param context context
     * @param type    类型
     * @param pathSet 文件绝对路径集合
     */
    private static List<MediaItem> getMediaItemFromPath(Context context, int type, Set<String> pathSet) {
        List<MediaItem> mediaItemList = new ArrayList<>();

        // 获取Metadata
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();

        for (String path : pathSet) {
            File file = new File(path);

            Uri uri = Uri.fromFile(file);

            try {
                // 为毛会出现这样的错误啊，卧槽
                mediaMetadataRetriever.setDataSource(context, uri);
            } catch (RuntimeException e) {
                continue;
            }

            String title, artist;

            if (type == ModelItem.TYPE_VIDEO) {
                title = file.getName();
                artist = null;
            } else {
                title = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                artist = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            }

            String duration;
            try {
                duration = formatMediaDuration(Long.valueOf(mediaMetadataRetriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_DURATION)));
            } catch (NumberFormatException e) {
                continue;
            }

            MediaItem item = new MediaItem(path, type, title, artist, duration);
            mediaItemList.add(item);

            item.save();
        }

        return mediaItemList;
    }


    /**
     * 格式化视频、音乐的时间
     *
     * @param time time as long integer
     * @return 格式化之后的时间 如 05:20 表示5min 20sec
     */
    private static String formatMediaDuration(long time) {
        String min = time / (1000 * 60) + "";
        String sec = time % (1000 * 60) + "";
        if (min.length() < 2)
            min = "0" + min;
        if (sec.length() == 4)
            sec = "0" + sec;
        else if (sec.length() <= 3)
            sec = "00" + sec;
        return min + ":" + sec.trim().substring(0, 2);
    }

}

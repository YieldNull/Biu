package com.bbbbiu.biu.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.mtp.MtpConstants;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.bbbbiu.biu.db.search.ApkItem;
import com.bbbbiu.biu.db.search.FileItem;
import com.bbbbiu.biu.db.search.ModelItem;
import com.bbbbiu.biu.db.search.MediaItem;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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

        return fileItems;
    }

    /**
     * 利用MediaStore 扫描视频。剔除不存在者。
     *
     * @param context context
     * @return 视频列表
     */
    public static List<MediaItem> scanVideoItem(Context context) {
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
        Set<String> pathList = scanFileWithExtension(context, StorageUtil.EXTENSION_DOC);

        List<FileItem> fileItems = new ArrayList<>();

        for (String path : pathList) {
            FileItem item = new FileItem(path, ModelItem.TYPE_DOC);
            fileItems.add(item);

            item.save();
        }

        return fileItems;
    }

    /**
     * 从MediaStore读取缩文件，剔除不存在者
     *
     * @param context context
     * @return 压缩文件列表
     */
    public static List<FileItem> scanArchiveItem(Context context) {
        Set<String> pathList = scanFileWithExtension(context, StorageUtil.EXTENSION_ARCHIVE);

        List<FileItem> fileItems = new ArrayList<>();

        for (String path : pathList) {
            FileItem item = new FileItem(path, ModelItem.TYPE_ARCHIVE);
            fileItems.add(item);

            item.save();
        }

        return fileItems;
    }

    /**
     * 从MediaStore读取相应后缀名的文件路径,剔除不存在的文件
     *
     * @param context   context
     * @param extension 后缀名列表
     * @return 文件列表
     */
    private static Set<String> scanFileWithExtension(Context context, List<String> extension) {
        Set<String> resultSet = new HashSet<>();

        Set<String> mimeList = new HashSet<>();

        for (String ext : extension) {
            String mime = MediaFile.getMimeTypeForFile("." + ext);
            if (mime != null) {
                mimeList.add(mime);
            }
        }

        if (mimeList.size() == 0) {
            return resultSet;
        }

        StringBuilder selectionBuilder = new StringBuilder();
        int count = 0;
        for (String ignored : mimeList) {
            if (count != 0) {
                selectionBuilder.append(" OR " + MediaStore.Files.FileColumns.MIME_TYPE + "=?");
            } else {
                selectionBuilder.append(MediaStore.Files.FileColumns.MIME_TYPE + "=?");
            }
            count++;
        }

        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = MediaStore.Files.getContentUri("external");

        Cursor cursor = contentResolver.query(
                uri,
                new String[]{MediaStore.Files.FileColumns.DATA},
                selectionBuilder.toString(),
                mimeList.toArray(new String[mimeList.size()]),
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
            mediaMetadataRetriever.setDataSource(context, uri);

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

    /**
     * 用{@link MimeTypeMap }计算得到的“PPT”的MIME TYPE与 MediaStore查到的不同。故直接用其源码算了
     * <p/>
     * Android MediaScanner Helper Class.
     * <p/>
     * Part of the source of <a href="https://android.googlesource.com/platform/frameworks/base/+/cd92588/media/java/android/media/MediaFile.java">android.media.MediaFile</a>
     * <p/>
     */
    public static class MediaFile {
        // Audio file types
        public static final int FILE_TYPE_MP3 = 1;
        public static final int FILE_TYPE_M4A = 2;
        public static final int FILE_TYPE_WAV = 3;
        public static final int FILE_TYPE_AMR = 4;
        public static final int FILE_TYPE_AWB = 5;
        public static final int FILE_TYPE_WMA = 6;
        public static final int FILE_TYPE_OGG = 7;
        public static final int FILE_TYPE_AAC = 8;
        public static final int FILE_TYPE_MKA = 9;
        public static final int FILE_TYPE_FLAC = 10;
        private static final int FIRST_AUDIO_FILE_TYPE = FILE_TYPE_MP3;
        private static final int LAST_AUDIO_FILE_TYPE = FILE_TYPE_FLAC;
        // MIDI file types
        public static final int FILE_TYPE_MID = 11;
        public static final int FILE_TYPE_SMF = 12;
        public static final int FILE_TYPE_IMY = 13;
        private static final int FIRST_MIDI_FILE_TYPE = FILE_TYPE_MID;
        private static final int LAST_MIDI_FILE_TYPE = FILE_TYPE_IMY;

        // Video file types
        public static final int FILE_TYPE_MP4 = 21;
        public static final int FILE_TYPE_M4V = 22;
        public static final int FILE_TYPE_3GPP = 23;
        public static final int FILE_TYPE_3GPP2 = 24;
        public static final int FILE_TYPE_WMV = 25;
        public static final int FILE_TYPE_ASF = 26;
        public static final int FILE_TYPE_MKV = 27;
        public static final int FILE_TYPE_MP2TS = 28;
        public static final int FILE_TYPE_AVI = 29;
        public static final int FILE_TYPE_WEBM = 30;
        private static final int FIRST_VIDEO_FILE_TYPE = FILE_TYPE_MP4;
        private static final int LAST_VIDEO_FILE_TYPE = FILE_TYPE_WEBM;

        // More video file types
        public static final int FILE_TYPE_MP2PS = 200;
        private static final int FIRST_VIDEO_FILE_TYPE2 = FILE_TYPE_MP2PS;
        private static final int LAST_VIDEO_FILE_TYPE2 = FILE_TYPE_MP2PS;
        // Image file types
        public static final int FILE_TYPE_JPEG = 31;
        public static final int FILE_TYPE_GIF = 32;
        public static final int FILE_TYPE_PNG = 33;
        public static final int FILE_TYPE_BMP = 34;
        public static final int FILE_TYPE_WBMP = 35;
        public static final int FILE_TYPE_WEBP = 36;
        private static final int FIRST_IMAGE_FILE_TYPE = FILE_TYPE_JPEG;
        private static final int LAST_IMAGE_FILE_TYPE = FILE_TYPE_WEBP;

        // Playlist file types
        public static final int FILE_TYPE_M3U = 41;
        public static final int FILE_TYPE_PLS = 42;
        public static final int FILE_TYPE_WPL = 43;
        public static final int FILE_TYPE_HTTPLIVE = 44;
        private static final int FIRST_PLAYLIST_FILE_TYPE = FILE_TYPE_M3U;
        private static final int LAST_PLAYLIST_FILE_TYPE = FILE_TYPE_HTTPLIVE;
        // Drm file types
        public static final int FILE_TYPE_FL = 51;
        private static final int FIRST_DRM_FILE_TYPE = FILE_TYPE_FL;
        private static final int LAST_DRM_FILE_TYPE = FILE_TYPE_FL;
        // Other popular file types
        public static final int FILE_TYPE_TEXT = 100;
        public static final int FILE_TYPE_HTML = 101;
        public static final int FILE_TYPE_PDF = 102;
        public static final int FILE_TYPE_XML = 103;
        public static final int FILE_TYPE_MS_WORD = 104;
        public static final int FILE_TYPE_MS_EXCEL = 105;
        public static final int FILE_TYPE_MS_POWERPOINT = 106;
        public static final int FILE_TYPE_ZIP = 107;

        public static class MediaFileType {
            public final int fileType;
            public final String mimeType;

            MediaFileType(int fileType, String mimeType) {
                this.fileType = fileType;
                this.mimeType = mimeType;
            }
        }

        private static final HashMap<String, MediaFileType> sFileTypeMap
                = new HashMap<String, MediaFileType>();
        private static final HashMap<String, Integer> sMimeTypeMap
                = new HashMap<String, Integer>();
        // maps file extension to MTP format code
        private static final HashMap<String, Integer> sFileTypeToFormatMap
                = new HashMap<String, Integer>();
        // maps mime type to MTP format code
        private static final HashMap<String, Integer> sMimeTypeToFormatMap
                = new HashMap<String, Integer>();
        // maps MTP format code to mime type
        private static final HashMap<Integer, String> sFormatToMimeTypeMap
                = new HashMap<Integer, String>();

        static void addFileType(String extension, int fileType, String mimeType) {
            sFileTypeMap.put(extension, new MediaFileType(fileType, mimeType));
            sMimeTypeMap.put(mimeType, Integer.valueOf(fileType));
        }

        static void addFileType(String extension, int fileType, String mimeType, int mtpFormatCode) {
            addFileType(extension, fileType, mimeType);
            sFileTypeToFormatMap.put(extension, Integer.valueOf(mtpFormatCode));
            sMimeTypeToFormatMap.put(mimeType, Integer.valueOf(mtpFormatCode));
            sFormatToMimeTypeMap.put(mtpFormatCode, mimeType);
        }

        static {
            addFileType("MP3", FILE_TYPE_MP3, "audio/mpeg", MtpConstants.FORMAT_MP3);
            addFileType("MPGA", FILE_TYPE_MP3, "audio/mpeg", MtpConstants.FORMAT_MP3);
            addFileType("M4A", FILE_TYPE_M4A, "audio/mp4", MtpConstants.FORMAT_MPEG);
            addFileType("WAV", FILE_TYPE_WAV, "audio/x-wav", MtpConstants.FORMAT_WAV);
            addFileType("AMR", FILE_TYPE_AMR, "audio/amr");
            addFileType("AWB", FILE_TYPE_AWB, "audio/amr-wb");

            addFileType("OGG", FILE_TYPE_OGG, "audio/ogg", MtpConstants.FORMAT_OGG);
            addFileType("OGG", FILE_TYPE_OGG, "application/ogg", MtpConstants.FORMAT_OGG);
            addFileType("OGA", FILE_TYPE_OGG, "application/ogg", MtpConstants.FORMAT_OGG);
            addFileType("AAC", FILE_TYPE_AAC, "audio/aac", MtpConstants.FORMAT_AAC);
            addFileType("AAC", FILE_TYPE_AAC, "audio/aac-adts", MtpConstants.FORMAT_AAC);
            addFileType("MKA", FILE_TYPE_MKA, "audio/x-matroska");

            addFileType("MID", FILE_TYPE_MID, "audio/midi");
            addFileType("MIDI", FILE_TYPE_MID, "audio/midi");
            addFileType("XMF", FILE_TYPE_MID, "audio/midi");
            addFileType("RTTTL", FILE_TYPE_MID, "audio/midi");
            addFileType("SMF", FILE_TYPE_SMF, "audio/sp-midi");
            addFileType("IMY", FILE_TYPE_IMY, "audio/imelody");
            addFileType("RTX", FILE_TYPE_MID, "audio/midi");
            addFileType("OTA", FILE_TYPE_MID, "audio/midi");
            addFileType("MXMF", FILE_TYPE_MID, "audio/midi");

            addFileType("MPEG", FILE_TYPE_MP4, "video/mpeg", MtpConstants.FORMAT_MPEG);
            addFileType("MPG", FILE_TYPE_MP4, "video/mpeg", MtpConstants.FORMAT_MPEG);
            addFileType("MP4", FILE_TYPE_MP4, "video/mp4", MtpConstants.FORMAT_MPEG);
            addFileType("M4V", FILE_TYPE_M4V, "video/mp4", MtpConstants.FORMAT_MPEG);
            addFileType("3GP", FILE_TYPE_3GPP, "video/3gpp", MtpConstants.FORMAT_3GP_CONTAINER);
            addFileType("3GPP", FILE_TYPE_3GPP, "video/3gpp", MtpConstants.FORMAT_3GP_CONTAINER);
            addFileType("3G2", FILE_TYPE_3GPP2, "video/3gpp2", MtpConstants.FORMAT_3GP_CONTAINER);
            addFileType("3GPP2", FILE_TYPE_3GPP2, "video/3gpp2", MtpConstants.FORMAT_3GP_CONTAINER);
            addFileType("MKV", FILE_TYPE_MKV, "video/x-matroska");
            addFileType("WEBM", FILE_TYPE_WEBM, "video/webm");
            addFileType("TS", FILE_TYPE_MP2TS, "video/mp2ts");
            addFileType("AVI", FILE_TYPE_AVI, "video/avi");

            addFileType("JPG", FILE_TYPE_JPEG, "image/jpeg", MtpConstants.FORMAT_EXIF_JPEG);
            addFileType("JPEG", FILE_TYPE_JPEG, "image/jpeg", MtpConstants.FORMAT_EXIF_JPEG);
            addFileType("GIF", FILE_TYPE_GIF, "image/gif", MtpConstants.FORMAT_GIF);
            addFileType("PNG", FILE_TYPE_PNG, "image/png", MtpConstants.FORMAT_PNG);
            addFileType("BMP", FILE_TYPE_BMP, "image/x-ms-bmp", MtpConstants.FORMAT_BMP);
            addFileType("WBMP", FILE_TYPE_WBMP, "image/vnd.wap.wbmp");
            addFileType("WEBP", FILE_TYPE_WEBP, "image/webp");

            addFileType("M3U", FILE_TYPE_M3U, "audio/x-mpegurl", MtpConstants.FORMAT_M3U_PLAYLIST);
            addFileType("M3U", FILE_TYPE_M3U, "application/x-mpegurl", MtpConstants.FORMAT_M3U_PLAYLIST);
            addFileType("PLS", FILE_TYPE_PLS, "audio/x-scpls", MtpConstants.FORMAT_PLS_PLAYLIST);
            addFileType("WPL", FILE_TYPE_WPL, "application/vnd.ms-wpl", MtpConstants.FORMAT_WPL_PLAYLIST);
            addFileType("M3U8", FILE_TYPE_HTTPLIVE, "application/vnd.apple.mpegurl");
            addFileType("M3U8", FILE_TYPE_HTTPLIVE, "audio/mpegurl");
            addFileType("M3U8", FILE_TYPE_HTTPLIVE, "audio/x-mpegurl");
            addFileType("FL", FILE_TYPE_FL, "application/x-android-drm-fl");
            addFileType("TXT", FILE_TYPE_TEXT, "text/plain", MtpConstants.FORMAT_TEXT);
            addFileType("HTM", FILE_TYPE_HTML, "text/html", MtpConstants.FORMAT_HTML);
            addFileType("HTML", FILE_TYPE_HTML, "text/html", MtpConstants.FORMAT_HTML);
            addFileType("PDF", FILE_TYPE_PDF, "application/pdf");
            addFileType("DOC", FILE_TYPE_MS_WORD, "application/msword", MtpConstants.FORMAT_MS_WORD_DOCUMENT);
            addFileType("XLS", FILE_TYPE_MS_EXCEL, "application/vnd.ms-excel", MtpConstants.FORMAT_MS_EXCEL_SPREADSHEET);
            addFileType("PPT", FILE_TYPE_MS_POWERPOINT, "application/mspowerpoint", MtpConstants.FORMAT_MS_POWERPOINT_PRESENTATION);
            addFileType("FLAC", FILE_TYPE_FLAC, "audio/flac", MtpConstants.FORMAT_FLAC);
            addFileType("ZIP", FILE_TYPE_ZIP, "application/zip");
            addFileType("MPG", FILE_TYPE_MP2PS, "video/mp2p");
            addFileType("MPEG", FILE_TYPE_MP2PS, "video/mp2p");
        }

        private static MediaFileType getFileType(String path) {
            int lastDot = path.lastIndexOf('.');
            if (lastDot < 0)
                return null;
            return sFileTypeMap.get(path.substring(lastDot + 1).toUpperCase(Locale.ROOT));
        }

        public static String getMimeTypeForFile(String path) {
            MediaFileType mediaFileType = getFileType(path);
            return (mediaFileType == null ? null : mediaFileType.mimeType);
        }

    }
}

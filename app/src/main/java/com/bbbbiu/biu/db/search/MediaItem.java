package com.bbbbiu.biu.db.search;


import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.bbbbiu.biu.util.StorageUtil;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.annotation.Index;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;

import java.io.File;

/**
 * 视频，音频等要纪录时长的
 * <p/>
 * Created by YieldNull at 4/18/16
 */

@Table(database = MediaItem.MyDatabase.class)
public class MediaItem extends ModelItem {
    @Database(name = MyDatabase.NAME, version = MyDatabase.VERSION)
    public class MyDatabase {

        public static final String NAME = "media";

        public static final int VERSION = 1;
    }

    @PrimaryKey
    @Index
    public String path;

    @Column
    public int type;

    @Column
    public String title;

    @Column
    public String artist;

    @Column
    public String duration;


    public MediaItem() {
    }


    /**
     * 根据文件解析成MediaItem，适用于视频以及音乐
     *
     * @param context context
     * @param path    文件路径
     * @return 解析成的MediaItem，解析失败则返回Null, title不存在也返回Null
     */
    public static MediaItem newItem(Context context, String path) {
        int type;
        File file = new File(path);
        Uri uri = Uri.fromFile(file);

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
            // 为毛会出现这样的错误啊，卧槽
            retriever.setDataSource(context, uri);
        } catch (RuntimeException e) {
            return null;
        }

        String title, artist;

        if (StorageUtil.isVideoFile(path)) {
            type = StorageUtil.TYPE_VIDEO;
            title = file.getName();
            artist = null;

        } else {
            type = StorageUtil.TYPE_MUSIC;

            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            if (title == null) {
                return null;
            }
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);

        }

        String duration;
        try {
            duration = formatMediaDuration(Long.valueOf(retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION)));
        } catch (NumberFormatException e) {
            return null;
        }

        return new MediaItem(file.getAbsolutePath(), type, title, artist, duration);
    }


    /**
     * 构造函数
     *
     * @param path     路径
     * @param type     类型 {@link StorageUtil#TYPE_MUSIC},{@link StorageUtil#TYPE_VIDEO}
     * @param title    名称
     * @param artist   歌手，艺术家，可为空
     * @param duration 时长，{@link #formatMediaDuration(long)}
     */
    public MediaItem(String path, int type, String title, @Nullable String artist, String duration) {
        this.path = path;
        this.type = type;
        this.title = title.trim();
        this.artist = artist;
        this.duration = duration;
    }


    @Override
    public File getParentFile() {
        return getFile().getParentFile();
    }

    @Override
    public File getFile() {
        return new File(path);
    }

    @Override
    public String getSize() {
        return StorageUtil.getReadableSize(getFile().length());
    }

    @Override
    public String getPath() {
        return path;
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

package com.bbbbiu.biu.db.search;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import com.bbbbiu.biu.util.StorageUtil;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.annotation.Index;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;

import java.io.File;
import java.util.Set;

/**
 * 视频，音频
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


    public MediaItem(String path, int type, String title, String artist, String duration) {
        this.path = path;
        this.type = type;
        this.title = title;
        this.artist = artist;
        this.duration = duration;
    }


    /**
     * 存到数据库
     *
     * @param context context
     * @param type    类型
     * @param pathSet 文件绝对路径集合
     */
    public static void storeMediaItems(Context context, int type, Set<String> pathSet) {

        // 获取Music的Metadata
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();

        for (String path : pathSet) {
            File file = new File(path);

            if (!file.exists()) {
                continue;
            }

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
                duration = formatTime(Long.valueOf(mediaMetadataRetriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_DURATION)));
            } catch (NumberFormatException e) {
                continue;
            }

            new MediaItem(path, type, title, artist, duration).save();
        }
    }

    @Override
    public String getParentDirName() {
        return getFile().getParentFile().getName();
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
     * 格式化时间
     *
     * @param time time as long integer
     * @return 格式化之后的时间 如 05:20 表示5min 20sec
     */
    public static String formatTime(long time) {
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

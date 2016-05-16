package com.bbbbiu.biu.db.search;


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
     * 构造函数
     *
     * @param path     路径
     * @param type     类型{@link ModelItem#TYPE_MUSIC},{@link ModelItem#TYPE_VIDEO}
     * @param title    名称
     * @param artist   歌手，艺术家，可为空
     * @param duration 时长，{@link com.bbbbiu.biu.util.SearchUtil#formatMediaDuration(long)}
     */
    public MediaItem(String path, int type, String title, @Nullable String artist, String duration) {
        this.path = path;
        this.type = type;
        this.title = title;
        this.artist = artist;
        this.duration = duration;
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

}

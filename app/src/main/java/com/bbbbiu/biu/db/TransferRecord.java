package com.bbbbiu.biu.db;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.util.Log;

import com.bbbbiu.biu.db.search.ApkItem;
import com.bbbbiu.biu.db.search.FileItem;
import com.bbbbiu.biu.db.search.MediaItem;
import com.bbbbiu.biu.db.search.ModelItem;
import com.bbbbiu.biu.util.StorageUtil;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.list.FlowQueryList;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.io.File;
import java.util.Arrays;

/**
 * Created by YieldNull at 5/9/16
 */

@Table(database = TransferRecord.MyDatabase.class)
public class TransferRecord extends BaseModel {

    private static final String TAG = TransferRecord.class.getSimpleName();


    @Database(name = MyDatabase.NAME, version = MyDatabase.VERSION)
    public class MyDatabase {

        public static final String NAME = "history";

        public static final int VERSION = 1;
    }

    public static final int TYPE_RECEIVED = 0;
    public static final int TYPE_SENT = 1;

    @PrimaryKey(autoincrement = true)
    public long id;

    @Column
    public long timestamp;

    @Column
    public String path;

    @Column
    public long size;

    @Column
    public int type;

    public TransferRecord() {
    }


    public TransferRecord(long timestamp, String path, long size, int type) {
        this.timestamp = timestamp;
        this.path = path;
        this.size = size;
        this.type = type;
    }

    public File getFile() {
        return new File(path);
    }

    public String getName() {
        return getFile().getName();
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof TransferRecord && o.hashCode() == hashCode();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{timestamp, path, size, type});
    }

    /**
     * 查询已下载或已发送纪录
     *
     * @param type 类型，{@link #TYPE_RECEIVED},{@link #TYPE_SENT}
     * @return 列表
     */
    public static FlowQueryList<TransferRecord> query(int type) {

        return SQLite.select()
                .distinct()
                .from(TransferRecord.class)
                .where(TransferRecord_Table.type.eq(type))
                .orderBy(TransferRecord_Table.timestamp, false)
                .groupBy(TransferRecord_Table.path, TransferRecord_Table.size)
                .flowQueryList();
    }


    /**
     * 保存下载纪录,同时将文件加入MediaStore以及分类数据库
     *
     * @param context Context
     * @param file    下载的文件
     */
    public static void recordReceiving(Context context, File file) {
        new TransferRecord(
                System.currentTimeMillis(),
                file.getAbsolutePath(),
                file.length(), TYPE_RECEIVED).save();


        storeInMediaStore(context, file);
    }


    /**
     * 保存发送纪录
     *
     * @param file 发送的文件
     */
    public static void recordSending(File file) {
        new TransferRecord(
                System.currentTimeMillis(),
                file.getAbsolutePath(),
                file.length(), TYPE_SENT).save();
    }


    /**
     * 把下载后的文件加入{@link android.provider.MediaStore},以及分类数据库
     *
     * @param file 文件
     */
    private static void storeInMediaStore(Context context, File file) {

        // 图片音乐视频什么的只能当做文件来存，
        // 不会写入{@link android.provider.MediaStore.Images}之类的数据库中
        // URI 类似于这样 content://media/external/file/17209 而不是 //media/external/images/media/17209
        // 因此，下载之后通过首页的分类是查不出来的
        // 可能是因为放在了APP专属的目录下面
        // 通过文件管理器把一个包含图片的文件夹放入外置储存卡随便一个目录里面，结果就能识别出来
        // 放入 ”/Android/data/package-name/“下面就跪了
        MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i(TAG, "File stored in MediaStore. path:" + path + " uri:" + uri);
                    }
                });

        // 所以啊，要将纪录直接写入文件分类的数据库
        String ext = StorageUtil.getFileExtension(file.getName());
        String path = file.getAbsolutePath();
        ModelItem item = null;

        if (StorageUtil.getFileType(ext) == StorageUtil.TYPE_MUSIC
                || StorageUtil.getFileType(ext) == StorageUtil.TYPE_VIDEO) {

            item = MediaItem.newItem(context, path);

        } else if (StorageUtil.getFileType(ext) == StorageUtil.TYPE_APK) {
            item = ApkItem.newItem(context, path);

        } else {
            int type = StorageUtil.getFileType(ext);

            if (type > 0) {
                item = new FileItem(path, StorageUtil.getFileType(ext));
            }
        }

        if (item != null) {
            Log.i(TAG, "Save file to searching-database");
            item.save();
        }
    }

}

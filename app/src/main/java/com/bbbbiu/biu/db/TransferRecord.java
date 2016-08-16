package com.bbbbiu.biu.db;

import android.content.ContentResolver;
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
import com.yieldnull.httpd.Streams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
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
    public String name;

    @Column
    public String uri;

    @Column
    public long size;

    @Column
    public int type;

    public TransferRecord() {
    }


    public TransferRecord(long timestamp, String uri, String name, long size, int type) {
        this.timestamp = timestamp;
        this.uri = uri;
        this.size = size;
        this.type = type;
        this.name = name;

    }


    /**
     * 获取对应的文件Uri
     *
     * @return Uri
     */
    public Uri getUri() {
        return Uri.parse(uri);
    }

    /**
     * 获取对应的文件类型
     *
     * @return 文件类型，未知类型则返回-1
     * @see StorageUtil
     */
    public int getFileType() {
        return StorageUtil.getFileType(StorageUtil.getFileExtension(name));
    }


    /**
     * 获取对应的文件路径
     *
     * @return 返回{@link ContentResolver#SCHEME_FILE}对应的路径，{@link ContentResolver#SCHEME_CONTENT}返回null
     */
    public String getFilePath() {
        Uri u = getUri();

        if (u.getScheme().equals(ContentResolver.SCHEME_FILE)) {
            return u.getPath();
        }

        return null;
    }


    /**
     * 对应的文件是否存在
     *
     * @param context context
     * @return 是否存在
     */
    public boolean fileExists(Context context) {
        InputStream stream = null;
        try {
            stream = context.getContentResolver().openInputStream(getUri());
            return true;
        } catch (FileNotFoundException e) {
            return false;
        } finally {
            if (stream != null) {
                Streams.safeClose(stream);
            }
        }
    }


    /**
     * 删除对应的文件
     *
     * @param context context
     * @return 若为 {@link ContentResolver#SCHEME_CONTENT}则返回true，若为{@link ContentResolver#SCHEME_FILE}则看是否删除成功
     */
    public boolean deleteFile(Context context) {
        Uri u = getUri();

        if (u.getScheme().equals(ContentResolver.SCHEME_FILE)) {
            return new File(u.getPath()).delete();
        } else {
            context.getContentResolver().delete(u, null, null);
            return true;
        }
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof TransferRecord && o.hashCode() == hashCode();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{timestamp, uri, size, type});
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
                .groupBy(TransferRecord_Table.uri, TransferRecord_Table.size)
                .flowQueryList();
    }

    /**
     * 查询已下载或已发送纪录
     *
     * @param type   type 类型，{@link #TYPE_RECEIVED},{@link #TYPE_SENT}
     * @param limits 限制数量
     * @return 列表
     */
    public static FlowQueryList<TransferRecord> query(int type, int limits) {
        return SQLite.select()
                .distinct()
                .from(TransferRecord.class)
                .where(TransferRecord_Table.type.eq(type))
                .orderBy(TransferRecord_Table.timestamp, false)
                .groupBy(TransferRecord_Table.uri, TransferRecord_Table.size)
                .limit(limits)
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
                Uri.fromFile(file).toString(),
                file.getName(),
                file.length(), TYPE_RECEIVED).save();


        storeInMediaStore(context, file);
    }


    /**
     * 保存发送纪录
     *
     * @param size 文件大小
     * @param uri  文件Uri
     */
    public static void recordSending(String uri, String name, long size) {
        new TransferRecord(
                System.currentTimeMillis(),
                uri,
                name,
                size, TYPE_SENT).save();
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
        // 放入 ”/Android/data/package-name/“下面就跪了, data 文件夹下面有".nomedia"文件
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

        int fileType = StorageUtil.getFileType(ext);

        if (fileType == StorageUtil.TYPE_MUSIC
                || StorageUtil.getFileType(ext) == StorageUtil.TYPE_VIDEO) {

            item = MediaItem.newItem(context, path);

        } else if (fileType == StorageUtil.TYPE_APK) {
            item = ApkItem.newItem(context, path);
        } else {

            if (fileType > 0) {
                item = new FileItem(path, fileType);
            }
        }

        if (item != null) {
            Log.i(TAG, "Save file to searching-database");
            item.save();
        }
    }

}

package com.bbbbiu.biu.util;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.util.Log;

import com.bbbbiu.biu.db.search.ApkItem;
import com.bbbbiu.biu.db.search.FileItem;
import com.bbbbiu.biu.db.search.MediaItem;
import com.bbbbiu.biu.db.search.ModelItem;
import com.bbbbiu.biu.db.transfer.RevRecord;

import java.io.File;

/**
 * 纪录传输纪录
 * <p/>
 * Created by finalize on 7/30/16.
 */
public class TransferRecorder {
    private static final String TAG = TransferRecorder.class.getSimpleName();


    /**
     * 保存下载纪录
     *
     * @param context Context
     * @param file    下载的文件
     */
    public static void recordReceiving(Context context, File file) {
        new RevRecord(
                System.currentTimeMillis(),
                file.getName(),
                file.length()).save();


        storeInMediaStore(context, file);
    }


    /**
     * 保存发送纪录
     *
     * @param context context
     * @param file    发送的文件
     */
    public static void recordSending(Context context, File file) {

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

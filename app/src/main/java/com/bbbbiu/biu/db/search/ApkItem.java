package com.bbbbiu.biu.db.search;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.bbbbiu.biu.util.StorageUtil;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.Index;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;


/**
 * APK。将应用图标缓存到磁盘中
 */
@Table(database = FileItem.MyDatabase.class)
public class ApkItem extends ModelItem {

    public static final int TYPE_APK_STANDALONE = 0;
    public static final int TYPE_APK_SYSTEM = 1;
    public static final int TYPE_APK_NORMAL = 2;

    private static final String TAG = ApkItem.class.getSimpleName();

    @PrimaryKey
    @Index
    public String path;

    @Column
    public String name;

    @Column
    public String packageName;

    @Column
    public int type;

    public ApkItem() {
    }

    /**
     * 构造函数
     *
     * @param path        路径
     * @param name        应用名
     * @param packageName 包名
     * @param type        类型{@link ApkItem#TYPE_APK_SYSTEM},{@link ApkItem#TYPE_APK_NORMAL},{@link ApkItem#TYPE_APK_STANDALONE}
     */
    public ApkItem(String path, String name, String packageName, int type) {
        this.path = path;
        this.name = name;
        this.packageName = packageName;
        this.type = type;
    }


    /**
     * 通过解析文件生成ApkItem。
     *
     * @param context context
     * @param path    APK 文件路径
     * @return ApkItem 已安装则返回Null，未安装则缓存其ICON
     */
    public static ApkItem newItem(Context context, String path) {
        String name = StorageUtil.getApkName(context, path);
        String packageName = StorageUtil.getApkPackageName(context, path);

        ApkItem apkItem = new ApkItem(path, name, packageName, ApkItem.TYPE_APK_STANDALONE);

        if (!apkItem.isInstalled(context)) {
            apkItem.storeCachedIcon(context);
            return apkItem;
        }

        return null;
    }


    /**
     * 从数据库中读取指定类型的APK
     *
     * @return APK列表
     */
    public static List<ApkItem> queryApkItem(int type) {
        List<ApkItem> items = SQLite.select()
                .from(ApkItem.class)
                .where(ApkItem_Table.type.eq(type))
                .queryList();

        removeNotExisting(items);

        return items;
    }


    @Override
    public String getPath() {
        return path;
    }


    @Override
    public String getSize() {
        return StorageUtil.getReadableSize(getFile().length());
    }

    @Override
    public File getParentFile() {
        return getFile().getParentFile();
    }


    /**
     * 获取对应的Apk文件
     *
     * @return apk 安装包文件
     */
    public File getFile() {
        return new File(path);
    }


    /**
     * 获取ICON
     *
     * @param context context
     * @return Drawable ICON
     */
    public Drawable getIcon(Context context) {
        return StorageUtil.getApkIcon(context, path);
    }


    /**
     * 是否已安装
     *
     * @param context context
     * @return 安装与否
     */
    public boolean isInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }


    /**
     * 获取图标文件
     *
     * @param context context
     * @return 图标文件
     */
    public File getCachedIconFile(Context context) {
        return new File(StorageUtil.getCachedApkIconPath(context, packageName));
    }


    /**
     * 将应用的图标存到内部储存上，以加快图标显示速度
     *
     * @param context context
     * @return 是否储存成功
     */
    public boolean storeCachedIcon(Context context) {
        Drawable drawable = StorageUtil.getApkIcon(context, path);
        if (drawable == null) {
            return false;
        }

        Bitmap bitmap = StorageUtil.drawableToBitmap(drawable);

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(StorageUtil.getCachedApkIconPath(context, packageName));
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            Log.w(TAG, e.toString());
            return false;
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                Log.w(TAG, e.toString());
            }
        }
        return true;
    }

}

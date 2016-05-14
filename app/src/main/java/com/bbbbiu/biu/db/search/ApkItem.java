package com.bbbbiu.biu.db.search;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
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
import java.util.Set;


@Table(database = FileItem.MyDatabase.class)
public class ApkItem extends ModelItem {

    public static final int TYPE_APK_STANDALONE = 0;
    public static final int TYPE_APK_SYSTEM = 1;
    public static final int TYPE_APK_NORMAL = 2;

    private static final String TAG = ApkItem.class.getSimpleName();

    public static final String ICON_DIR = "apk_icon";

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

    public ApkItem(String path, String name, String packageName, int type) {
        this.path = path;
        this.name = name;
        this.packageName = packageName;
        this.type = type;
    }

    /**
     * 将APK信息存到数据库
     *
     * @param context 　context
     * @param type    APK类型 {@value TYPE_APK_NORMAL,TYPE_APK_STANDALONE,TYPE_APK_SYSTEM}
     * @param pathSet 路径集合
     */
    public static void storeApk(Context context, int type, Set<String> pathSet) {
        for (String path : pathSet) {
            String name = StorageUtil.getApkName(context, path);
            String packageName = StorageUtil.getApkPackageName(context, path);

            if (name == null || packageName == null) {
                continue;
            }

            ApkItem apkItem = new ApkItem(path, name, packageName, type);

            if (apkItem.storeIcon(context)) {
                apkItem.save();
            }
        }
    }

    /**
     * 获取系统apk
     *
     * @return 已更新过的系统APK
     */
    public static List<ApkItem> getApkList(int type) {
        List<ApkItem> items = SQLite.select()
                .from(ApkItem.class)
                .where(ApkItem_Table.type.eq(type))
                .queryList();

        filter(items);

        return items;
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

    @Override
    public String getPath() {
        return path;
    }


    @Override
    public String getSize() {
        return StorageUtil.getReadableSize(getFile().length());
    }

    @Override
    public String getParentDirName() {
        return getFile().getParentFile().getName();
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
     * 获取图标文件
     *
     * @param context context
     * @return 图标文件
     */
    public File getCachedIconFile(Context context) {
        return new File(getIconPath(context));
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

    @Override
    public boolean equals(Object o) {
        return o instanceof ApkItem && ((ApkItem) o).path.equals(path);
    }

    /**
     * 获取图标文件的路径
     *
     * @param context context
     * @return 路径
     */
    private String getIconPath(Context context) {
        return new File(
                context.getDir(ICON_DIR, Context.MODE_PRIVATE), packageName)
                .getAbsolutePath();
    }

    /**
     * 将应用的图标存到内部储存上，以加快图标显示速度
     *
     * @param context context
     * @return 是否储存成功
     */
    private boolean storeIcon(Context context) {
        Drawable drawable = StorageUtil.getApkIcon(context, path);
        if (drawable == null) {
            return false;
        }

        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(getIconPath(context));
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

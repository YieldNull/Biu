package com.bbbbiu.biu.gui.adapters.choose;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.util.PreferenceUtil;
import com.bbbbiu.biu.util.SearchUtil;
import com.bbbbiu.biu.util.StorageUtil;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by fangdongliang on 16/3/24.
 * <p/>
 * Update by YiledNull
 */
public class ApkContentAdapter extends ContentBaseAdapter {
    private static final String TAG = ApkContentAdapter.class.getSimpleName();

    private Context context;
    private PackageManager mPackageManager;

    private List<Apk> mApkList = new ArrayList<>();

    private List<Apk> mSystemApkList = new ArrayList<>();
    private List<Apk> mNormalApkList = new ArrayList<>();
    private List<Apk> mStandaloneApkList = new ArrayList<>();

    private List<File> mChosenFiles = new ArrayList<>();


    private Picasso mPicasso;
    private static final String PICASSO_SCHEMA_APP = "app-icon"; // 处理自定义请求
    private static final String PICASSO_TAG = "tag-img"; //所有请求加TAG，退出时cancel all

    // 默认升序排列
    private Comparator<Apk> mComparator = new Comparator<Apk>() {
        @Override
        public int compare(Apk lhs, Apk rhs) {
            String name1 = lhs.name.toLowerCase();
            String name2 = rhs.name.toLowerCase();
            return name1.compareTo(name2); //升序
        }
    };

    /**
     * Created by fangdongliang on 16/3/24.
     * <p/>
     * Update by YiledNull
     */
    class Apk {
        String name;
        String path;

        Apk(String name, String path) {
            this.name = name;
            this.path = path;
        }
    }

    public ApkContentAdapter(final AppCompatActivity context) {
        super(context);
        this.context = context;
        mPackageManager = context.getPackageManager();


        Picasso.Builder builder = new Picasso.Builder(context);
        builder.addRequestHandler(new AppIconRequestHandler());
        mPicasso = builder.build();

        notifyStartLoadingData(); // 显示正在加载的动画

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (readApkList()) {
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            notifyDataSetChanged();
                            notifyFinishLoadingData(); // 取消加载动画
                        }
                    });
                } else {
                    clearAllRecord();

                    // 通过包管理器扫描已安装应用
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            scanInstalledApk();

                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    notifyFinishLoadingData(); // 取消加载动画
                                    notifyDataSetChanged();
                                }
                            });
                        }
                    }).start();

                    // 扫描独立的APK文件，一定会比上一个线程后结束？
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            scanStandaloneApk();

                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    notifyDataSetChanged();
                                }
                            });
                        }
                    }).start();
                }
            }
        }).start();
    }

    @Override
    public void cancelPicassoTask() {
        mPicasso.cancelTag(PICASSO_TAG);
    }


    /**
     * 从持久化储存中读取之前扫描的APK列表
     *
     * @return 之前是否有扫描
     */
    private boolean readApkList() {
        return readInstalledApk() && readStandaloneApk(); // 注意顺序，此为显示顺序
    }

    /**
     * 从持久化储存中读取之前扫描的已安装的APK列表
     *
     * @return 之前是否有扫描
     */
    private boolean readInstalledApk() {
        Log.i(TAG, "Reading installed apk paths from SharedPreferences");

        Set<String> normal = PreferenceUtil.getInstalledNormalApk(context);
        Set<String> system = PreferenceUtil.getInstalledSysApk(context);

        if ((normal == null) || (system == null)) {
            Log.i(TAG, "Has not scanned before");
            return false;
        }

        for (String path : normal) {
            String name = getApkName(path);

            if (name != null) {
                mNormalApkList.add(new Apk(name, path));
            }
        }

        for (String path : system) {
            String name = getApkName(path);

            if (name != null) {
                mSystemApkList.add(new Apk(name, path));
            }
        }

        // 排序
        Collections.sort(mSystemApkList, mComparator);
        Collections.sort(mNormalApkList, mComparator);

        // 注意顺序，此为显示顺序
        // null as header placeholder
        if (mNormalApkList.size() > 0) {
            mApkList.add(null);
            mApkList.addAll(mNormalApkList);
        }

        if (mSystemApkList.size() > 0) {
            mApkList.add(null);
            mApkList.addAll(mSystemApkList);
        }

        Log.i(TAG, "Got installed apk files: " + (mSystemApkList.size() + mNormalApkList.size()));

        return true;
    }

    /**
     * 从持久化储存中读取之前扫描的独立的APK列表
     *
     * @return 之前是否有扫描
     */
    private boolean readStandaloneApk() {
        Log.i(TAG, "Reading standalone apk paths from SharedPreferences");

        Set<String> standalone = PreferenceUtil.getFileFromCategory(context, SearchUtil.TYPE_APK);
        if (standalone == null) {
            Log.i(TAG, "Has not scanned before");
            return false;
        }

        for (String path : standalone) {
            if (!isAppInstalled(path)) {
                String name = getApkName(path);

                if (name != null) {
                    mStandaloneApkList.add(new Apk(name, path));
                }
            }
        }

        Collections.sort(mStandaloneApkList, mComparator);
        if (mStandaloneApkList.size() > 0) {
            mApkList.add(null);
            mApkList.addAll(mStandaloneApkList);
        }

        Log.i(TAG, "Got standalone apk files: " + mStandaloneApkList.size());

        return true;
    }

    /**
     * 扫描已安装，并将扫描结果持久化
     */
    private void scanInstalledApk() {
        Log.i(TAG, "Scanning installed APK");

        SearchUtil.scanApkInstalled(context);
        readInstalledApk();

        Log.i(TAG, "Installed APK amount: " + (mSystemApkList.size() + mNormalApkList.size()));
    }


    /**
     * 扫描未安装，并将扫描结果持久化
     */
    private void scanStandaloneApk() {
        Log.i(TAG, "Scanning standalone APK");

        SearchUtil.scanDisk(context);
        readStandaloneApk();

        Log.i(TAG, "Got standalone apk files amount: " + mStandaloneApkList.size());
    }

    /**
     * 清除所有记录的APK
     */
    private void clearAllRecord() {
        mApkList.clear();
        mNormalApkList.clear();
        mSystemApkList.clear();
        mStandaloneApkList.clear();
    }

    private Apk getApkAt(int position) {
        return mApkList.get(position);
    }

    @Override
    public List<File> getChosenFiles() {
        return mChosenFiles;
    }

    @Override
    public int getItemCount() {
        return mApkList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return getApkAt(position) == null ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView;

        if (viewType == VIEW_TYPE_ITEM) {
            itemView = inflater.inflate(R.layout.list_apk_item, parent, false);
            return new ApkViewHolder(itemView);
        } else {
            itemView = inflater.inflate(R.layout.list_header_cate, parent, false);
            return new HeaderViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder hd, int position) {
        if (getItemViewType(position) == VIEW_TYPE_HEADER) {
            HeaderViewHolder holder = (HeaderViewHolder) hd;
            if (mSystemApkList.contains(getApkAt(position + 1))) {
                holder.headerText.setText(context.getString(R.string.apk_header_system));
            } else if (mNormalApkList.contains(getApkAt(position + 1))) {
                holder.headerText.setText(context.getString(R.string.apk_header_normal));
            } else {
                holder.headerText.setText(context.getString(R.string.apk_header_standalone));
            }
        } else {
            ApkViewHolder holder = (ApkViewHolder) hd;

            Apk apk = mApkList.get(position);
            holder.apkNameText.setText(apk.name);

            File apkFile = new File(apk.path);
            long size = apkFile.length();
            holder.apkSize.setText(StorageUtil.getReadableSize(size));

            mPicasso.load(PICASSO_SCHEMA_APP + ":" + apk.path)
                    .tag(PICASSO_TAG)
                    .into(holder.apkIconImage);
        }
    }


    /***
     * APK item placeholder
     */
    class ApkViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.imageView)
        ImageView apkIconImage;

        @Bind(R.id.textView_name)
        TextView apkNameText;

        @Bind(R.id.textView_size)
        TextView apkSize;

        public ApkViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }
    }


    /**
     * 处理 Picasso 的请求。从PackageManager读取App Icon
     */
    class AppIconRequestHandler extends RequestHandler {
        @Override
        public boolean canHandleRequest(Request data) {
            return PICASSO_SCHEMA_APP.equals(data.uri.getScheme());
        }

        @Override
        public Result load(Request request, int networkPolicy) throws IOException {
            String path = request.uri.toString().replace(PICASSO_SCHEMA_APP + ":", "");

            Bitmap bitmap = getApkIcon(path);

            return new Result(bitmap, Picasso.LoadedFrom.DISK);
        }
    }

    /**
     * 根据应用路径判断其是否已安装
     *
     * @param path 包名
     * @return 安装与否
     */
    private boolean isAppInstalled(String path) {
        PackageInfo packageInfo = mPackageManager.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);
        String packageName = packageInfo.packageName;

        boolean installed;
        try {
            mPackageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            installed = false;
        }
        return installed;
    }

    /**
     * 根据APK文件的路径获取其应用名称
     *
     * @param path 文件路径
     * @return 应用名称
     */
    private String getApkName(String path) {
        PackageInfo packageInfo = mPackageManager.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);

        String name = null;
        if (packageInfo != null) {

            ApplicationInfo appInfo = packageInfo.applicationInfo;

            appInfo.sourceDir = path;
            appInfo.publicSourceDir = path;
            name = (String) mPackageManager.getApplicationLabel(packageInfo.applicationInfo);
        }
        return name;
    }

    /**
     * 根据APK文件的路径获取其ICON
     *
     * @param path 文件路径
     * @return ICON as Bitmap or null
     */
    private Bitmap getApkIcon(String path) {
        PackageInfo packageInfo = mPackageManager.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);

        if (packageInfo != null) {

            ApplicationInfo appInfo = packageInfo.applicationInfo;
            appInfo.sourceDir = path;
            appInfo.publicSourceDir = path;

            Drawable drawable = mPackageManager.getApplicationIcon(appInfo);

            return drawable != null ? ((BitmapDrawable) drawable).getBitmap() : null;
        }
        return null;
    }
}


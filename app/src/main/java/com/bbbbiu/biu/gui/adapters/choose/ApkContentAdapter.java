package com.bbbbiu.biu.gui.adapters.choose;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bbbbiu.biu.R;
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
 */
public class ApkContentAdapter extends ContentBaseAdapter {
    private static final String TAG = ApkContentAdapter.class.getSimpleName();

    private Context context;

    private List<Apk> mApkList = new ArrayList<>();
    private List<Apk> mSystemApkList = new ArrayList<>();
    private List<Apk> mNormalApkList = new ArrayList<>();
    private List<Apk> mNotInstalledApkList = new ArrayList<>();

    private List<File> mChosenFiles = new ArrayList<>();

    private PackageManager mPackageManager;

    private Picasso mPicasso;
    public static final String PICASSO_SCHEMA_APP = "app-icon";
    public static final String PICASSO_TAG = "tag-img";

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

        Picasso.Builder builder = new Picasso.Builder(context);
        builder.addRequestHandler(new AppIconRequestHandler());
        mPicasso = builder.build();

        notifyStartLoadingData();

        new Thread(new Runnable() {
            @Override
            public void run() {
                scanInstalled();

                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        notifyFinishLoadingData();
                        notifyDataSetChanged();
                    }
                });
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                scanNotInstalled();

                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
            }
        }).start();
    }

    @Override
    public void cancelPicassoTask() {
        mPicasso.cancelTag(PICASSO_TAG);
    }

    /**
     * 扫描已安装
     */
    private void scanInstalled() {
        mPackageManager = context.getPackageManager();

        List<PackageInfo> infoList = mPackageManager.getInstalledPackages(0);
        for (PackageInfo info : infoList) {
            if ((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0 &&
                    (info.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) {
                continue;
            }

            String name = (String) mPackageManager.getApplicationLabel(info.applicationInfo);
            String path = info.applicationInfo.sourceDir;

            Apk apk = new Apk(name, path);

            if ((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0) {
                mSystemApkList.add(apk);
            } else {
                mNormalApkList.add(apk);
            }
        }


        Collections.sort(mSystemApkList, mComparator);
        Collections.sort(mNormalApkList, mComparator);


        if (mNormalApkList.size() > 0) {
            mApkList.add(null);
            mApkList.addAll(mNormalApkList);
        }

        if (mSystemApkList.size() > 0) {
            mApkList.add(null);
            mApkList.addAll(mSystemApkList);
        }

        Log.i(TAG, "Installed APK amount: " + mApkList.size());
    }

    /**
     * 扫描未安装
     */
    private void scanNotInstalled() {
        Set<String> set = SearchUtil.searchFile(context, SearchUtil.TYPE_APK);
        for (String path : set) {

            String name = getApkName(path, true);

            if (name != null) {
                mNotInstalledApkList.add(new Apk(name, path));
            }
        }

        Collections.sort(mNotInstalledApkList, mComparator);

        if (mNotInstalledApkList.size() > 0) {
            mApkList.add(null);
            mApkList.addAll(mNotInstalledApkList);
        }
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
                holder.headerText.setText("系统");
            } else if (mNormalApkList.contains(getApkAt(position + 1))) {
                holder.headerText.setText("已安装");
            } else {
                holder.headerText.setText("未安装");
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

            Bitmap bitmap = getApkIcon(path, false);

            return new Result(bitmap, Picasso.LoadedFrom.DISK);
        }
    }

    private boolean isAppInstalled(String packageName) {
        boolean installed;
        try {
            mPackageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            installed = false;
        }
        return installed;
    }

    private String getApkName(String path, boolean requireNotInstall) {
        PackageInfo packageInfo = mPackageManager.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);

        String name = null;
        if (packageInfo != null) {
            if (requireNotInstall && isAppInstalled(packageInfo.packageName)) {
                return null;
            }

            ApplicationInfo appInfo = packageInfo.applicationInfo;

            appInfo.sourceDir = path;
            appInfo.publicSourceDir = path;
            name = (String) mPackageManager.getApplicationLabel(packageInfo.applicationInfo);
        }
        return name;
    }

    private Bitmap getApkIcon(String path, boolean requireNotInstall) {
        PackageInfo packageInfo = mPackageManager.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);

        if (packageInfo != null) {
            if (requireNotInstall && isAppInstalled(packageInfo.packageName)) {
                return null;
            }

            ApplicationInfo appInfo = packageInfo.applicationInfo;
            appInfo.sourceDir = path;
            appInfo.publicSourceDir = path;

            Drawable drawable = mPackageManager.getApplicationIcon(appInfo);

            return drawable != null ? ((BitmapDrawable) drawable).getBitmap() : null;
        }
        return null;
    }
}


package com.bbbbiu.biu.gui.adapter.choose;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapter.util.HeaderViewHolder;
import com.bbbbiu.biu.gui.choose.ChooseBaseActivity;
import com.bbbbiu.biu.util.SearchUtil;
import com.bbbbiu.biu.util.StorageUtil;
import com.bbbbiu.biu.db.search.ApkItem;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
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
    private List<ApkItem> mApkList = new ArrayList<>();

    private List<ApkItem> mSystemApkList = new ArrayList<>();
    private List<ApkItem> mNormalApkList = new ArrayList<>();
    private List<ApkItem> mStandaloneApkList = new ArrayList<>();

    private List<ApkItem> mChosenApks = new ArrayList<>();

    private Picasso mPicasso;
    private static final String PICASSO_TAG = "tag-img"; //所有请求加TAG，退出时cancel all

    // 默认升序排列
    private Comparator<ApkItem> mComparator = new Comparator<ApkItem>() {
        @Override
        public int compare(ApkItem lhs, ApkItem rhs) {
            String name1 = lhs.name.toLowerCase();
            String name2 = rhs.name.toLowerCase();
            return name1.compareTo(name2); //升序
        }
    };


    public ApkContentAdapter(final ChooseBaseActivity context) {
        super(context);
        this.context = context;

        mPicasso = Picasso.with(context);
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
        boolean b = readInstalledApk();
        readStandaloneApk(); // 注意顺序，此为显示顺序

        return b;
    }

    /**
     * 从持久化储存中读取之前扫描的已安装的APK列表
     *
     * @return 之前是否有扫描
     */
    private boolean readInstalledApk() {
        Log.i(TAG, "Reading installed apk paths from Database");

        List<ApkItem> normal = ApkItem.getApkList(ApkItem.TYPE_APK_NORMAL);
        List<ApkItem> system = ApkItem.getApkList(ApkItem.TYPE_APK_SYSTEM);

        if (normal.size() + system.size() == 0) {
            Log.i(TAG, "Has not scanned before");
            return false;
        }

        mNormalApkList.addAll(normal);
        mSystemApkList.addAll(system);

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
        Log.i(TAG, "Reading standalone apk paths from Database");

        List<ApkItem> standalone = ApkItem.getApkList(ApkItem.TYPE_APK_STANDALONE);
        if (standalone.size() == 0) {
            return false;
        }

        for (ApkItem apkItem : standalone) {
            if (!apkItem.isInstalled(context)) {
                mStandaloneApkList.add(apkItem);
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

    private ApkItem getApkAt(int position) {
        return mApkList.get(position);
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
    public RecyclerView.ViewHolder OnCreateItemViewHolder(LayoutInflater inflater, ViewGroup parent) {
        return new ApkViewHolder(inflater.inflate(R.layout.list_apk_item, parent, false));
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
            final ApkViewHolder holder = (ApkViewHolder) hd;
            final ApkItem apkItem = mApkList.get(position);

            // 应用名
            holder.apkNameText.setText(apkItem.name);

            // 文件大小
            long size = apkItem.getFile().length();
            holder.apkSize.setText(StorageUtil.getReadableSize(size));

            // 加载图标
            mPicasso.load(apkItem.getCachedIconFile(context))
                    .tag(PICASSO_TAG)
                    .into(holder.apkIconImage);

            // 选中或不选中的样式
            final CardView cardView = (CardView) holder.itemView;

            if (mChosenApks.contains(apkItem)) {
                cardView.setForeground(context.getResources().getDrawable(R.drawable.ic_chosen_check));
                cardView.setForegroundGravity(Gravity.TOP | Gravity.RIGHT);
            } else {
                cardView.setForeground(null);
            }

            // 监听选择
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onApkClicked(apkItem);
                }
            });
        }
    }

    @Override
    public Set<String> getChosenFiles() {
        Set<String> files = new HashSet<>();
        for (ApkItem apkItem : mChosenApks) {
            files.add(apkItem.path);
        }
        return files;
    }

    @Override
    public int getChosenCount() {
        return mChosenApks.size();
    }

    @Override
    public void setFileAllChosen() {
        for (ApkItem apkItem : mApkList) {
            if (apkItem != null) { // 去掉placeholder
                mChosenApks.add(apkItem);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public void setFileAllDismissed() {
        mChosenApks.clear();
        notifyDataSetChanged();
    }

    public List<ApkItem> getChosenApks() {
        return mChosenApks;
    }

    /**
     * 卸载或删除应用
     *
     * @param apkItem apkItem
     */
    public void deleteApk(ApkItem apkItem) {

        mChosenApks.remove(apkItem);

        mNormalApkList.remove(apkItem);
        mSystemApkList.remove(apkItem);
        mStandaloneApkList.remove(apkItem);

        mApkList.remove(apkItem);

        apkItem.delete();

        notifyDataSetChanged();
    }

    /**
     * APK 被点击
     *
     * @param apkItem apk
     */
    private void onApkClicked(ApkItem apkItem) {
        if (mChosenApks.contains(apkItem)) {
            mChosenApks.remove(apkItem);
            notifyFileDismissed(apkItem.path);
        } else {
            mChosenApks.add(apkItem);
            notifyFileChosen(apkItem.path);
        }

        notifyDataSetChanged();
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
}


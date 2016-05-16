package com.bbbbiu.biu.gui.adapter.choose;

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
import java.util.Map;
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

    private List<ApkItem> mApkDataSet = new ArrayList<>();

    private List<ApkItem> mSystemApkList = new ArrayList<>();
    private List<ApkItem> mNormalApkList = new ArrayList<>();
    private List<ApkItem> mStandaloneApkList = new ArrayList<>();

    private List<ApkItem> mChosenApkList = new ArrayList<>();

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
        mPicasso = Picasso.with(context);
    }

    @Override
    protected boolean readDataFromDB() {
        scanInstalledApk(true);
        scanStandaloneApk(true);
        return mApkDataSet.size() != 0;
    }

    @Override
    protected boolean readDataFromSys() {
        scanInstalledApk(false);
        scanStandaloneApk(false);
        return mApkDataSet.size() != 0;
    }

    @Override
    protected void updateDatabase() {
        SearchUtil.scanInstalledApkItem(context);
        SearchUtil.scanStandAloneApkItem(context);
    }

    @Override
    protected void updateDataSet() {
        mApkDataSet.clear();
        readDataFromDB();
    }

    /**
     * 由包管理器扫描已安装的APK，并显示
     */
    private void scanInstalledApk(boolean fromDatabase) {
        Log.i(TAG, "Scanning installed APK from " + (fromDatabase ? "database" : "system"));

        mNormalApkList.clear();
        mSystemApkList.clear();

        List<ApkItem> systemList, normalList;

        if (fromDatabase) {
            systemList = ApkItem.queryApkItem(ApkItem.TYPE_APK_SYSTEM);
            normalList = ApkItem.queryApkItem(ApkItem.TYPE_APK_NORMAL);
        } else {
            Map<Integer, List<ApkItem>> map = SearchUtil.scanInstalledApkItem(context);

            systemList = map.get(ApkItem.TYPE_APK_SYSTEM);
            normalList = map.get(ApkItem.TYPE_APK_NORMAL);
        }

        mNormalApkList.addAll(normalList);
        mSystemApkList.addAll(systemList);

        // 排序
        Collections.sort(mSystemApkList, mComparator);
        Collections.sort(mNormalApkList, mComparator);

        // 注意顺序，此为显示顺序
        // null as header placeholder
        if (mNormalApkList.size() > 0) {
            mApkDataSet.add(null);
            mApkDataSet.addAll(mNormalApkList);
        }

        if (mSystemApkList.size() > 0) {
            mApkDataSet.add(null);
            mApkDataSet.addAll(mSystemApkList);
        }


        Log.i(TAG, "Installed APK amount: " + (mSystemApkList.size() + mNormalApkList.size()));
    }

    /**
     * 从数据库中读取之前扫描的独立的APK列表
     */
    private void scanStandaloneApk(boolean fromDatabase) {
        Log.i(TAG, "Scanning standalone APK from " + (fromDatabase ? "database" : "system"));

        mStandaloneApkList.clear();

        List<ApkItem> standaloneList;

        if (fromDatabase) {
            standaloneList = ApkItem.queryApkItem(ApkItem.TYPE_APK_STANDALONE);
        } else {
            standaloneList = SearchUtil.scanStandAloneApkItem(context);
        }

        for (ApkItem apkItem : standaloneList) {
            if (!apkItem.isInstalled(context)) {
                mStandaloneApkList.add(apkItem);
            }
        }

        Collections.sort(mStandaloneApkList, mComparator);
        if (mStandaloneApkList.size() > 0) {
            mApkDataSet.add(null);
            mApkDataSet.addAll(mStandaloneApkList);
        }

        Log.i(TAG, "Standalone apk amount: " + mStandaloneApkList.size());
    }

    /**
     * 获取已选择的APK
     *
     * @return APK列表
     */
    public List<ApkItem> getChosenApk() {
        return mChosenApkList;
    }

    /**
     * 卸载或删除应用
     *
     * @param apkItem apkItem
     */
    public void deleteApk(ApkItem apkItem) {

        mChosenApkList.remove(apkItem);

        mNormalApkList.remove(apkItem);
        mSystemApkList.remove(apkItem);
        mStandaloneApkList.remove(apkItem);

        mApkDataSet.remove(apkItem);

        apkItem.delete();

        notifyDataSetChanged();
    }

    @Override
    public void cancelPicassoTask() {
        mPicasso.cancelTag(PICASSO_TAG);
    }


    @Override
    public int getItemCount() {
        return mApkDataSet.size();
    }

    @Override
    public int getItemViewType(int position) {
        return getApkAt(position) == null ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }

    @Override
    public RecyclerView.ViewHolder onCreateItemViewHolder(LayoutInflater inflater, ViewGroup parent) {
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
            final ApkItem apkItem = mApkDataSet.get(position);

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

            if (mChosenApkList.contains(apkItem)) {
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
        for (ApkItem apkItem : mChosenApkList) {
            files.add(apkItem.path);
        }
        return files;
    }

    @Override
    public int getChosenCount() {
        return mChosenApkList.size();
    }

    @Override
    public void setFileAllChosen() {
        for (ApkItem apkItem : mApkDataSet) {
            if (apkItem != null) { // 去掉placeholder
                mChosenApkList.add(apkItem);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public void setFileAllDismissed() {
        mChosenApkList.clear();
        notifyDataSetChanged();
    }

    private ApkItem getApkAt(int position) {
        return mApkDataSet.get(position);
    }


    /**
     * APK 被点击
     *
     * @param apkItem apk
     */
    private void onApkClicked(ApkItem apkItem) {
        if (mChosenApkList.contains(apkItem)) {
            mChosenApkList.remove(apkItem);
            notifyFileDismissed(apkItem.path);
        } else {
            mChosenApkList.add(apkItem);
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


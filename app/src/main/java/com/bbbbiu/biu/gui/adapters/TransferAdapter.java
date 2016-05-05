package com.bbbbiu.biu.gui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.transfer.FileItem;
import com.bbbbiu.biu.util.StorageUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * 上传、下载、接收文件等界面({@link com.bbbbiu.biu.gui.transfer.TransferBaseActivity})的Adapter。
 * <p/>
 * Created by YieldNull at 3/23/16
 */
public class TransferAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = TransferAdapter.class.getSimpleName();

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_WORKING = 1;
    private static final int VIEW_TYPE_WAITING = 2;
    private static final int VIEW_TYPE_FINISHED = 3;


    private final Context context;

    private ArrayList<FileItem> mDataSet = new ArrayList<>();

    private Queue<FileItem> mWaitingQueue = new LinkedList<>();
    private List<FileItem> mFinishedList = new ArrayList<>();
    private FileItem mWorkingItem;


    public TransferAdapter(Context context) {
        this.context = context;
    }


    /**
     * 测试界面
     */
    private void test() {
        File root = StorageUtil.getRootDir(context, StorageUtil.TYPE_EXTERNAL);
        File[] files = root.listFiles();

        mWorkingItem = new FileItem(files[0].getAbsolutePath(), files[0].getName(), files[0].length());

        for (int i = 1; i < files.length / 2; i++) {
            mWaitingQueue.add(new FileItem(files[i].getAbsolutePath(), files[i].getName(), files[i].length()));
        }

        for (int i = files.length / 2; i < files.length; i++) {
            mFinishedList.add(new FileItem(files[i].getAbsolutePath(), files[i].getName(), files[i].length()));
        }

        refreshDataSet();
    }

    /**
     * 刷新数据集，将各文件分类。
     * <p/>
     * 显示时是按 正在下载，等待，已完成的次序显示的，因此加入数据集中的顺序不能乱
     */
    private void refreshDataSet() {
        if (mWorkingItem != null) {
            mDataSet.add(null);
            mDataSet.add(mWorkingItem);
        }

        if (mWaitingQueue.size() != 0) {
            mDataSet.add(null);
            mDataSet.addAll(mWaitingQueue);
        }

        if (mFinishedList.size() != 0) {
            mDataSet.add(null);
            mDataSet.addAll(mFinishedList);
        }

    }

    /**
     * 将任务加入等待队列
     *
     * @param fileItems 亟待完成的FileItem
     */
    public void addItem(ArrayList<FileItem> fileItems) {
        if (fileItems == null || fileItems.size() == 0) {
            return;
        }

        mDataSet.clear();

        mWaitingQueue.addAll(fileItems);

        if (mWorkingItem == null) { // 初始化
            mWorkingItem = mWaitingQueue.poll();
        }

        refreshDataSet();

        notifyDataSetChanged();
    }

    /**
     * 任务完成
     *
     * @param fileUri file uri
     */
    public void setTaskFinished(String fileUri) {

        mDataSet.clear();

        mFinishedList.add(mWorkingItem);

        mWorkingItem = mWaitingQueue.poll(); // TODO 为空，所有任务都完成了

        refreshDataSet();

        notifyDataSetChanged();
    }

    /**
     * 任务失败
     *
     * @param fileUri file uri
     */
    public void setTaskFailed(String fileUri) {
        // TODO 失败后怎么办？
    }

    public void updateProgress(RecyclerView recyclerView, String fileUri, int progress) {
        int position = getPosition(fileUri);
        FileItem fileItem = getItem(fileUri);

        WorkingViewHolder holder;
        try {
            holder = (WorkingViewHolder) recyclerView.findViewHolderForAdapterPosition(position);
        } catch (ClassCastException e) {
            return; // 文件太小 已经完成了才收到
        }

        if (holder != null) { // 文件太小，导致list还没完成更新，此处NullPointer
            holder.progressBar.setProgress(progress);

            String read = StorageUtil.getReadableSize((long) (fileItem.size * progress * 0.01));
            String all = StorageUtil.getReadableSize(fileItem.size);

            holder.infoText.setText(String.format("%s/%s", read, all));
        }
    }


    public int getPosition(String fileUri) {
        return mDataSet.indexOf(new FileItem(fileUri, "", 0));
    }

    public FileItem getItem(String fileUri) {
        return mDataSet.get(getPosition(fileUri));
    }

    public FileItem getItem(int position) {
        return mDataSet.get(position);
    }

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }

    @Override
    public int getItemViewType(int position) {

        FileItem item = getItem(position);

        if (item == null) {
            return VIEW_TYPE_HEADER;
        } else if (item == mWorkingItem) {
            return VIEW_TYPE_WORKING;
        } else if (mWaitingQueue.contains(item)) {
            return VIEW_TYPE_WAITING;
        } else {
            return VIEW_TYPE_FINISHED;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        if (viewType == VIEW_TYPE_HEADER) {
            View itemView = inflater.inflate(R.layout.list_header_common, parent, false);
            return new HeaderViewHolder(itemView);
        } else if (viewType == VIEW_TYPE_WORKING) {
            View itemView = inflater.inflate(R.layout.list_transfer_working, parent, false);
            return new WorkingViewHolder(itemView);
        } else if (viewType == VIEW_TYPE_WAITING) {
            View itemView = inflater.inflate(R.layout.list_transfer_waiting, parent, false);
            return new WaitingViewHolder(itemView);
        } else {
            View itemView = inflater.inflate(R.layout.list_transfer_finished, parent, false);
            return new FinishedViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder hd, int position) {
        int type = getItemViewType(position);
        FileItem fileItem = getItem(position);

        if (type == VIEW_TYPE_HEADER) {
            HeaderViewHolder holder = (HeaderViewHolder) hd;

            fileItem = getItem(position + 1);
            if (mWorkingItem == fileItem) {
                holder.headerText.setText("正在传输");
            } else if (mWaitingQueue.contains(fileItem)) {
                holder.headerText.setText("正在等待");
            } else {
                holder.headerText.setText("已完成");
            }

        } else if (type == VIEW_TYPE_WORKING) {
            WorkingViewHolder holder = (WorkingViewHolder) hd;
            holder.iconImage.setImageDrawable(StorageUtil.getFileIcon(context, new File(fileItem.uri)));
            holder.nameText.setText(fileItem.name);
            holder.infoText.setText(StorageUtil.getReadableSize(fileItem.size));
            holder.progressBar.setMax(100);
            holder.progressBar.setProgress(0);

        } else if (type == VIEW_TYPE_WAITING) {
            WaitingViewHolder holder = (WaitingViewHolder) hd;
            holder.iconImage.setImageDrawable(StorageUtil.getFileIcon(context, new File(fileItem.uri)));
            holder.nameText.setText(fileItem.name);
            holder.sizeText.setText(StorageUtil.getReadableSize(fileItem.size));

        } else {
            FinishedViewHolder holder = (FinishedViewHolder) hd;
            holder.iconImage.setImageDrawable(StorageUtil.getFileIcon(context, new File(fileItem.uri)));
            holder.nameText.setText(fileItem.name);
            holder.infoText.setText(StorageUtil.getReadableSize(fileItem.size));
        }
    }


    class WorkingViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.imageView_icon)
        ImageView iconImage;

        @Bind(R.id.textView_name)
        TextView nameText;

        @Bind(R.id.textView_info)
        TextView infoText;

        @Bind(R.id.progressBar)
        ProgressBar progressBar;

        public WorkingViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }
    }

    class WaitingViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.imageView_icon)
        ImageView iconImage;

        @Bind(R.id.textView_name)
        TextView nameText;

        @Bind(R.id.textView_size)
        TextView sizeText;

        public WaitingViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);

        }
    }

    class FinishedViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.imageView_icon)
        ImageView iconImage;

        @Bind(R.id.textView_name)
        TextView nameText;

        @Bind(R.id.textView_info)
        TextView infoText;

        @Bind(R.id.imageView_option)
        ImageView optionImage;

        public FinishedViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);

        }
    }
}

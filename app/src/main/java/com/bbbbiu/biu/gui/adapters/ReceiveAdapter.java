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
import com.bbbbiu.biu.http.client.FileItem;
import com.bbbbiu.biu.http.util.ProgressListener;
import com.bbbbiu.biu.util.StorageUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by YieldNull at 3/23/16
 */
public class ReceiveAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = ReceiveAdapter.class.getSimpleName();

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;


    private final Context context;
    private ArrayList<FileItem> fileList = new ArrayList<>();

    private HashMap<FileItem, ProgressListener> fileItemProgressListenerHashMap = new HashMap<>();

    public void addFileList(List<FileItem> files) {
        fileList.addAll(files);
    }


    public ReceiveAdapter(Context context) {
        this.context = context;
        fileList.add(null); //Header
    }

    public int getItemPosition(FileItem fileItem) {
        return fileList.indexOf(fileItem);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        if (viewType == VIEW_TYPE_ITEM) {
            View itemView = inflater.inflate(R.layout.list_receive_item, parent, false);
            return new FileItemViewHolder(itemView);
        } else {
            View itemView = inflater.inflate(R.layout.list_receive_header, parent, false);
            return new HeaderViewHolder(itemView);
        }

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder hd, int position) {

        if (getItemViewType(position) == VIEW_TYPE_ITEM) {
            FileItemViewHolder holder = (FileItemViewHolder) hd;
            FileItem fileItem = fileList.get(position);

            holder.fileNameText.setText(fileItem.getName());
            holder.progressText.setText(StorageUtil.getReadableSize(fileItem.getSize()));
            holder.progressBar.setMax(100);

        } else {

        }
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    @Override
    public int getItemViewType(int position) {

        return position == 0 ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }


    public void updateProgress(RecyclerView.ViewHolder holder, int progress) {
        FileItemViewHolder fileItemViewHolder = (FileItemViewHolder) holder;
        fileItemViewHolder.progressBar.setProgress(progress);
    }


    public class FileItemViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.imageView_file_icon)
        ImageView fileIconImage;

        @Bind(R.id.textView_file_name)
        TextView fileNameText;

        @Bind(R.id.textView_file_info)
        TextView progressText;

        @Bind(R.id.progressBar_receive)
        ProgressBar progressBar;

        public ProgressBar getProgressBar() {
            return progressBar;
        }

        public void setProgressText(String text) {
            progressText.setText(text);
        }

        public FileItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.textView_receive_hint)
        TextView hintText;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}

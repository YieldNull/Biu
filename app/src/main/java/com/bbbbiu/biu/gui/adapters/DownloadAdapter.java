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
import com.bbbbiu.biu.util.StorageUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by YieldNull at 3/23/16
 */
public class DownloadAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = DownloadAdapter.class.getSimpleName();

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;


    private final Context context;
    private ArrayList<FileItem> fileList = new ArrayList<>();

    public void addFileList(List<FileItem> files) {
        fileList.addAll(files);
    }


    public DownloadAdapter(Context context) {
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
            View itemView = inflater.inflate(R.layout.list_download_item, parent, false);
            return new FileItemViewHolder(itemView);
        } else {
            View itemView = inflater.inflate(R.layout.list_download_header, parent, false);
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
            holder.progressBar.setProgress(0);

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

    public class FileItemViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.imageView_icon)
        ImageView fileIconImage;

        @Bind(R.id.textView_name)
        TextView fileNameText;

        @Bind(R.id.textView_progress)
        TextView progressText;

        @Bind(R.id.progressBar)
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

        @Bind(R.id.textView)
        TextView hintText;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}

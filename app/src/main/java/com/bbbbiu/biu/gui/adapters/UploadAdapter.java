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
import com.bbbbiu.biu.util.Storage;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by YieldNull at 3/26/16
 */
public class UploadAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = UploadAdapter.class.getSimpleName();

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private final Context context;
    private ArrayList<File> fileList = new ArrayList<>();


    public void addFiles(Set<String> filePaths) {
        for (String path : filePaths) {
            fileList.add(new File(path));
        }
    }


    public UploadAdapter(Context context) {
        this.context = context;
        fileList.add(null); //Header
    }

    public int getItemPosition(File fileItem) {
        return fileList.indexOf(fileItem);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        if (viewType == VIEW_TYPE_ITEM) {
            View itemView = inflater.inflate(R.layout.list_upload_item, parent, false);
            return new FileItemViewHolder(itemView);
        } else {
            View itemView = inflater.inflate(R.layout.list_upload_header, parent, false);
            return new HeaderViewHolder(itemView);
        }

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder hd, int position) {

        if (getItemViewType(position) == VIEW_TYPE_ITEM) {
            FileItemViewHolder holder = (FileItemViewHolder) hd;
            File file = fileList.get(position);

            holder.fileNameText.setText(file.getName());
            holder.progressText.setText(Storage.getReadableSize(file.length()));
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

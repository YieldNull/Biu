package com.bbbbiu.biu.gui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.client.HttpManager;
import com.bbbbiu.biu.util.StorageUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by YieldNull at 3/23/16
 */
public class FileStreamAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = FileStreamAdapter.class.getSimpleName();
    private final Context context;

    private List<HttpManager.FileItem> fileList=new ArrayList<>();

    public void setFileList(List<HttpManager.FileItem> mFileList) {
        this.fileList = mFileList;
    }

    public List<HttpManager.FileItem> getFileList() {
        return fileList;
    }

    public FileStreamAdapter(Context context) {
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(R.layout.list_receive_item, parent, false);

        return new FileItemHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder hd, int position) {
        FileItemHolder holder = (FileItemHolder) hd;
        HttpManager.FileItem fileItem = fileList.get(position);

        holder.fileNameText.setText(fileItem.getName());
        holder.fileSizeText.setText(StorageUtil.getReadableSize(fileItem.getSize()));
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }


    private class FileItemHolder extends RecyclerView.ViewHolder {
        ImageView fileIconImage;
        TextView fileNameText;
        TextView fileSizeText;
        ImageButton fileOptionImage;

        public FileItemHolder(View itemView) {
            super(itemView);

            fileIconImage = (ImageView) itemView.findViewById(R.id.imageView_file_icon);
            fileNameText = (TextView) itemView.findViewById(R.id.textView_file_name);
            fileSizeText = (TextView) itemView.findViewById(R.id.textView_file_description);
            fileOptionImage = (ImageButton) itemView.findViewById(R.id.imageButton_file_option);
        }
    }
}

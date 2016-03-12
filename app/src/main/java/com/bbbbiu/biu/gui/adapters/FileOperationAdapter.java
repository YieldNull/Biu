package com.bbbbiu.biu.gui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bbbbiu.biu.R;

import java.io.File;
import java.util.ArrayList;

public class FileOperationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ArrayList<Integer[]> operations = new ArrayList<>();
    private Context context;
    private final int VIEW_TYPE_HEADER = 0;
    private final int VIEW_TYPE_ITEM = 1;

    private File file;

    public FileOperationAdapter(Context context,File file) {
        this.context = context;
        this.file=file;

        operations.add(new Integer[]{R.string.file_action_info, R.drawable.ic_file_action_info});
        operations.add(new Integer[]{R.string.file_action_open, R.drawable.ic_file_action_open});
        operations.add(new Integer[]{R.string.file_action_rename, R.drawable.ic_file_action_rename});
        operations.add(new Integer[]{R.string.file_action_move, R.drawable.ic_file_action_move});
        operations.add(new Integer[]{R.string.file_action_copy, R.drawable.ic_file_action_copy});
        operations.add(new Integer[]{R.string.file_action_delete, R.drawable.ic_file_action_delete});

    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View viewItem;
        if (viewType == VIEW_TYPE_ITEM) {
            viewItem = inflater.inflate(R.layout.sliding_up_file_option_item, parent, false);
            return new OperationViewHolder(viewItem);

        } else {
            viewItem = inflater.inflate(R.layout.sliding_up_file_option_header, parent, false);
            return new OperationHeaderViewHolder(viewItem);
        }

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder hd, int position) {
        if (getItemViewType(position) == VIEW_TYPE_ITEM) {
            OperationViewHolder holder = (OperationViewHolder) hd;

            int stringId = operations.get(position)[0];
            int drawableId = operations.get(position)[1];

            holder.iconImageView.setImageDrawable(context.getResources().getDrawable(drawableId));
            holder.operationTextView.setText(context.getString(stringId));
        } else {
            OperationHeaderViewHolder holder = (OperationHeaderViewHolder) hd;
            holder.fileIconImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_file_folder));
            holder.fileNameTextView.setText(file.getName());
        }

    }


    @Override
    public int getItemCount() {
        return operations.size();
    }

    public class OperationHeaderViewHolder extends RecyclerView.ViewHolder {
        public ImageView fileIconImageView;
        public TextView fileNameTextView;
        public ImageView fileInfoImageView;

        public OperationHeaderViewHolder(View itemView) {
            super(itemView);

            fileIconImageView = (ImageView) itemView.findViewById(R.id.imageView_file_operation_file_icon);
            fileNameTextView = (TextView) itemView.findViewById(R.id.textView_file_operation_file_name);
            fileInfoImageView = (ImageView) itemView.findViewById(R.id.imageView_file_operation_file_info);
        }
    }

    public class OperationViewHolder extends RecyclerView.ViewHolder {
        public ImageView iconImageView;
        public TextView operationTextView;


        public OperationViewHolder(View itemView) {
            super(itemView);

            iconImageView = (ImageView) itemView.findViewById(R.id.imageView_file_operation_icon);
            operationTextView = (TextView) itemView.findViewById(R.id.textView_file_operation);
        }
    }
}

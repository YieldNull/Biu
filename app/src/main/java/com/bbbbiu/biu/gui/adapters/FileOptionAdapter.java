package com.bbbbiu.biu.gui.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.yqritc.recyclerviewflexibledivider.FlexibleDividerDecoration;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.io.File;
import java.util.ArrayList;

public class FileOptionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements FlexibleDividerDecoration.PaintProvider,
        FlexibleDividerDecoration.VisibilityProvider, HorizontalDividerItemDecoration.MarginProvider {

    private ArrayList<Integer[]> operations = new ArrayList<>();
    private Context context;
    private final int VIEW_TYPE_HEADER = 0;
    private final int VIEW_TYPE_ITEM = 1;

    private File file;

    public FileOptionAdapter(Context context,File file) {
        this.context = context;
        this.file=file;

        operations.add(new Integer[]{R.string.file_option_info, R.drawable.ic_file_option_info});
        operations.add(new Integer[]{R.string.file_option_open, R.drawable.ic_file_option_open});
        operations.add(new Integer[]{R.string.file_option_rename, R.drawable.ic_file_option_rename});
        operations.add(new Integer[]{R.string.file_option_move, R.drawable.ic_file_option_move});
        operations.add(new Integer[]{R.string.file_option_copy, R.drawable.ic_file_option_copy});
        operations.add(new Integer[]{R.string.file_option_delete, R.drawable.ic_file_option_delete});

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
            viewItem = inflater.inflate(R.layout.list_file_option_item, parent, false);
            return new OperationViewHolder(viewItem);

        } else {
            viewItem = inflater.inflate(R.layout.list_file_option_header, parent, false);
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

    public void setFile(File file) {
        this.file = file;
    }


    /**
     * RecyclerView Divider
     */
    @Override
    public Paint dividerPaint(int position, RecyclerView parent) {
        Paint paint = new Paint();
        paint.setColor(context.getResources().getColor(R.color.upPanel_list_divider));
        paint.setStrokeWidth(1);
        return paint;
    }

    @Override
    public boolean shouldHideDivider(int position, RecyclerView parent) {
        switch (position) {
            case 1:
            case 3:
                return true;
        }
        return false;
    }

    @Override
    public int dividerLeftMargin(int position, RecyclerView parent) {
        switch (position) {
            case 0:
                return 0;
            case 2:
                return 140;
            case 4:
                return 140;
        }
        return 0;
    }

    @Override
    public int dividerRightMargin(int position, RecyclerView parent) {
        return 0;
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

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

import java.io.File;
import java.util.ArrayList;

public class FilePanelAdapter extends PanelBaseAdapter {

    private static final String TAG = FilePanelAdapter.class.getSimpleName();

    public int getOptionStringId(int position) {
        return operations.get(position)[0];
    }

    private ArrayList<Integer[]> operations = new ArrayList<>();
    private Context context;
    private final int VIEW_TYPE_HEADER = 0;
    private final int VIEW_TYPE_ITEM = 1;

    public File getFile() {
        return file;
    }

    private File file;

    public FilePanelAdapter(Context context) {
        this.context = context;

        operations.add(new Integer[]{R.string.file_option_info, R.drawable.ic_file_option_info});
        operations.add(new Integer[]{R.string.file_option_rename, R.drawable.ic_file_option_rename});
        operations.add(new Integer[]{R.string.file_option_move, R.drawable.ic_file_option_move});
        operations.add(new Integer[]{R.string.file_option_copy, R.drawable.ic_file_option_copy});
        operations.add(new Integer[]{R.string.file_option_delete, R.drawable.ic_file_option_delete});

    }

    public void setFile(File file) {
        this.file = file;
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
            holder.setOnClickListener(position);

        } else {
            OperationHeaderViewHolder holder = (OperationHeaderViewHolder) hd;
            holder.fileIconImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_type_folder));

            String name = file != null ? file.getName() : "";
            holder.fileNameTextView.setText(name);
        }

    }


    @Override
    public int getItemCount() {
        return operations.size();
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
    public int dividerLeftMargin(int position, RecyclerView parent) {
        return position == 3 ? 140 : 0;
    }

    @Override
    public int dividerRightMargin(int position, RecyclerView parent) {
        return 0;
    }

    @Override
    public boolean shouldHideDivider(int position, RecyclerView parent) {
        return position == 1 || position == 2;
    }

    public class OperationHeaderViewHolder extends RecyclerView.ViewHolder {
        public ImageView fileIconImageView;
        public TextView fileNameTextView;

        public OperationHeaderViewHolder(View itemView) {
            super(itemView);

            fileIconImageView = (ImageView) itemView.findViewById(R.id.imageView_file_operation_file_icon);
            fileNameTextView = (TextView) itemView.findViewById(R.id.textView_file_operation_file_name);
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

        public void setOnClickListener(final int position) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (getOptionStringId(position)) {
                        case R.string.file_option_rename:
                            break;
                        case R.string.file_option_move:
                            break;
                        case R.string.file_option_copy:
                            break;
                        case R.string.file_option_delete:
                            break;
                        default:
                            break;
                    }
                }
            });
        }
    }
}

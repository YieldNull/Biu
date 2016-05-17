package com.bbbbiu.biu.gui.adapter.choose;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapter.util.OnViewTouchListener;
import com.bbbbiu.biu.gui.choose.FileMoveActivity;
import com.bbbbiu.biu.gui.choose.listener.OptionPanelActionListener;
import com.bbbbiu.biu.util.StorageUtil;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class FileOptionAdapter extends BaseOptionAdapter {
    private static final String TAG = FileOptionAdapter.class.getSimpleName();

    private final int VIEW_TYPE_HEADER = 0;
    private final int VIEW_TYPE_ITEM = 1;


    private Context context;
    private File mBoundFile;
    private OptionPanelActionListener mOptionPanelListener;

    private List<Integer[]> mOptions = Arrays.asList(
            new Integer[]{R.string.file_option_info, R.drawable.ic_action_detail},
            new Integer[]{R.string.file_option_rename, R.drawable.ic_action_rename},
            new Integer[]{R.string.file_option_move, R.drawable.ic_action_move},
            new Integer[]{R.string.file_option_copy, R.drawable.ic_action_copy},
            new Integer[]{R.string.file_option_delete, R.drawable.ic_action_delete});


    public FileOptionAdapter(Context context) {
        this.context = context;
        mOptionPanelListener = (OptionPanelActionListener) context;
    }

    /***********************************************************************************
     * ***********{@link android.support.v7.widget.RecyclerView.ViewHolder} ************
     **********************************************************************************/


    @Override
    public int getItemCount() {
        return mOptions.size();
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
            return new HeaderViewHolder(viewItem);
        }

    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder hd, final int position) {

        hd.itemView.setOnTouchListener(OnViewTouchListener.getSingleton(context));

        if (getItemViewType(position) == VIEW_TYPE_ITEM) {
            OperationViewHolder holder = (OperationViewHolder) hd;

            int stringId = mOptions.get(position)[0];
            int drawableId = mOptions.get(position)[1];

            holder.iconImage.setImageDrawable(context.getResources().getDrawable(drawableId));
            holder.operationText.setText(context.getString(stringId));

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOptionPanelListener.onOptionItemClicked(mBoundFile);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            onOptionClicked(position);
                        }
                    }, 200);
                }
            });
        } else {
            HeaderViewHolder holder = (HeaderViewHolder) hd;

            if (mBoundFile == null) {
                holder.iconImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_type_folder));
            } else {
                holder.iconImage.setImageDrawable(StorageUtil.getFileIcon(context, mBoundFile));
            }
            String name = mBoundFile != null ? mBoundFile.getName() : "";
            holder.nameText.setText(name);


            if (mBoundFile != null && mBoundFile.isFile()) {
                holder.openImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_action_open));
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        StorageUtil.openFile(context, mBoundFile);
                    }
                });
            } else {
                holder.openImage.setImageDrawable(null);
                holder.itemView.setOnClickListener(null);
            }
        }
    }

    /***********************************************************************************************/


    /**********************************************************************************************
     * ************** {@link com.yqritc.recyclerviewflexibledivider}*********************************
     ***********************************************************************************************/
    @Override
    public Paint dividerPaint(int position, RecyclerView parent) {
        Paint paint = new Paint();
        paint.setColor(context.getResources().getColor(R.color.option_panel_list_divider));
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

    /***********************************************************************************************/


    public File getBoundFile() {
        return mBoundFile;
    }

    public void setBoundFile(File file) {
        this.mBoundFile = file;
    }


    private void onOptionClicked(int position) {
        switch (mOptions.get(position)[0]) {
            case R.string.file_option_rename:
                renameFile();
                break;
            case R.string.file_option_move:
                moveFile();
                break;
            case R.string.file_option_copy:
                copyFile();
                break;
            case R.string.file_option_delete:
                deleteFile();
                break;
            default:
                break;
        }
    }

    private void deleteFile() {
        new AlertDialog.Builder(context)
                .setTitle(mBoundFile.getName())
                .setMessage(context.getString(R.string.file_confirm_delete))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!FileUtils.deleteQuietly(mBoundFile)) {
                            Toast.makeText(context,
                                    context.getString(R.string.delete_failed),
                                    Toast.LENGTH_SHORT)
                                    .show();
                        } else {
                            Toast.makeText(context,
                                    context.getString(R.string.delete_succeeded),
                                    Toast.LENGTH_SHORT)
                                    .show();

                            mOptionPanelListener.onOptionModifyContent();
                            mOptionPanelListener.onOptionDeleteFile(mBoundFile);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(context,
                                R.string.delete_dismissed,
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                }).show();
    }

    private void copyFile() {
        ((Activity) context).startActivityForResult(
                new Intent(context, FileMoveActivity.class), FileMoveActivity.REQUEST_COPY);
    }

    private void moveFile() {
        ((Activity) context).startActivityForResult(
                new Intent(context, FileMoveActivity.class), FileMoveActivity.REQUEST_MOVE);
    }

    private void renameFile() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_rename, null);
        final EditText editText = (EditText) view.findViewById(R.id.editText);
        editText.setText(mBoundFile.getName());

        builder.setTitle(context.getString(R.string.file_rename_title))
                .setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newName = editText.getText().toString();

                        boolean result = mBoundFile.renameTo(
                                new File(mBoundFile.getParentFile(), editText.getText().toString()));

                        if (result) {
                            Toast.makeText(context, R.string.file_rename_succeeded,
                                    Toast.LENGTH_SHORT).show();

                            if (!newName.equals(mBoundFile.getName())) {
                                mOptionPanelListener.onOptionModifyContent();
                            }
                        } else {
                            Toast.makeText(context, R.string.file_rename_failed,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.imageView_icon)
        ImageView iconImage;

        @Bind(R.id.textView_name)
        TextView nameText;

        @Bind(R.id.imageView_open)
        ImageView openImage;

        HeaderViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }
    }

    class OperationViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.imageView_icon)
        ImageView iconImage;

        @Bind(R.id.textView_operation)
        TextView operationText;


        OperationViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }
    }
}

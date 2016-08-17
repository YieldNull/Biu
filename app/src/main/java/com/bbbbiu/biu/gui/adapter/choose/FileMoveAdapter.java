package com.bbbbiu.biu.gui.adapter.choose;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapter.util.HeaderViewHolder;
import com.bbbbiu.biu.gui.choose.listener.OnChangeDirListener;
import com.bbbbiu.biu.gui.choose.listener.OnLoadingDataListener;
import com.bbbbiu.biu.util.StorageUtil;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by YieldNull at 5/16/16
 */
public class FileMoveAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = FileMoveAdapter.class.getSimpleName();
    private static final int VIEW_TYPE_HEADER = 1;
    private static final int VIEW_TYPE_ITEM = 2;

    private Context context;
    private OnChangeDirListener mChangeDirListener;
    private OnLoadingDataListener mLoadingDataListener;

    private List<File> mDataSet = new ArrayList<>();
    private List<File> mDirList = new ArrayList<>();
    private List<File> mFileList = new ArrayList<>();

    private boolean hasExternal;
    private File internal;
    private File external;

    public FileMoveAdapter(Context context) {
        this.context = context;
        mChangeDirListener = (OnChangeDirListener) context;
        mLoadingDataListener = (OnLoadingDataListener) context;

        init();
    }


    public void setCurrentDir(File rootDir) {
        clear();

        if (rootDir == null) {
            init();
        } else {
            mDirList.addAll(Arrays.asList(rootDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return !pathname.isHidden() && pathname.isDirectory();
                }
            })));

            mFileList.addAll(Arrays.asList(rootDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return !pathname.isHidden() && pathname.isFile();
                }
            })));

            // 默认升序排列
            Comparator<File> comparator = new Comparator<File>() {
                @Override
                public int compare(File lhs, File rhs) {
                    String name1 = lhs.getName().toLowerCase();
                    String name2 = rhs.getName().toLowerCase();
                    return name1.compareTo(name2); //升序
                }
            };

            Collections.sort(mDirList, comparator);
            Collections.sort(mFileList, comparator);

            if (mDirList.size() > 0) {
                mDataSet.add(null);
                mDataSet.addAll(mDirList);
            }

            if (mFileList.size() > 0) {
                mDataSet.add(null);
                mDataSet.addAll(mFileList);
            }
        }

        if (mDataSet.size() == 0) {
            mLoadingDataListener.onEmptyDataSet();
        } else {
            mLoadingDataListener.onNonEmptyDataSet();
        }
    }

    public File getRootDir() {
        if (hasExternal) {
            return null;
        } else {
            return internal;
        }
    }

    public String getRootDirPath() {
        return internal.getParentFile().getAbsolutePath();
    }


    private void init() {
        internal = StorageUtil.getRootDir(context, StorageUtil.STORAGE_INTERNAL);

        if (StorageUtil.hasRealExternal(context)) {
            external = StorageUtil.getRootDir(context, StorageUtil.STORAGE_EXTERNAL);

            mDirList.add(internal);
            mDirList.add(external);

            mDataSet.addAll(mDirList);

            hasExternal = true;
        } else {
            setCurrentDir(StorageUtil.getRootDir(context, StorageUtil.STORAGE_INTERNAL));
            hasExternal = false;
        }
    }

    private void clear() {
        mDataSet.clear();
        mDirList.clear();
        mFileList.clear();
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView;

        if (viewType == VIEW_TYPE_ITEM) {
            itemView = inflater.inflate(R.layout.list_file_item, parent, false);
            return new DirViewHolder(itemView);
        } else {
            return HeaderViewHolder.build(inflater, parent);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder hd, int position) {

        if (getItemViewType(position) == VIEW_TYPE_ITEM) {
            DirViewHolder holder = (DirViewHolder) hd;
            final File file = getItem(position);

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
            String modifyTime = dateFormat.format(new Date(file.lastModified()));

            // 显示内外部储存
            if (hasExternal && mDataSet.size() == 2) {
                if (file.equals(internal)) {
                    holder.nameText.setText(R.string.cate_storage);
                } else if (file.equals(external)) {
                    holder.nameText.setText(R.string.cate_sdcard);
                } else {
                    holder.nameText.setText(file.getName());
                }
            } else {
                holder.nameText.setText(file.getName());
            }

            if (file.isDirectory()) { // 目录
                int itemCount = file.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return !pathname.isHidden();
                    }
                }).length;

                holder.infoText.setText(String.format("%s  %s %d ", modifyTime, context.getString(R.string.header_file), itemCount));
                holder.iconImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_type_folder));

            } else { // 文件
                holder.infoText.setText(String.format("%s  %s", modifyTime, StorageUtil.getReadableSize(file.length())));

                holder.iconImage.setImageDrawable(StorageUtil.getFileIcon(context, file));
            }

            if (file.isDirectory()) {
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mChangeDirListener.onEnterDir(file);
                    }
                });
            } else {
                holder.itemView.setOnClickListener(null);
            }
        } else {
            HeaderViewHolder holder = (HeaderViewHolder) hd;

            if (mDirList.contains(getItem(position + 1))) {
                holder.headerText.setText(R.string.header_folder);
            } else {
                holder.headerText.setText(R.string.header_file);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }


    @Override
    public int getItemViewType(int position) {
        return getItem(position) == null ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }

    private File getItem(int position) {
        return mDataSet.get(position);
    }

    class DirViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.imageView_icon)
        ImageView iconImage;

        @Bind(R.id.textView_name)
        TextView nameText;

        @Bind(R.id.textView_info)
        TextView infoText;

        @Bind(R.id.imageButton_option)
        ImageButton optionToggleImage;

        public DirViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);

            optionToggleImage.setVisibility(View.INVISIBLE);
        }
    }

}

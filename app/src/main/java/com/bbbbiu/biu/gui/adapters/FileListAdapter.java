package com.bbbbiu.biu.gui.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.fragments.FileFragment;
import com.bbbbiu.biu.util.StorageUtil;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class FileListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = FileListAdapter.class.getSimpleName();

    /**
     * ViewType 类型
     */
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private Context context;
    private FileFragment.OnFileSelectingListener mOnFileSelectingListener;
    private FileFragment.OnFileOptionClickListener mOnFileOptionClickListener;

    /**
     * 当前目录下的文件或文件夹
     */
    private File[] dirs;
    private File[] files;

    private List<File> dirEnterStack = new ArrayList<>();
    private List<File> listItems = new ArrayList<>();
    private HashSet<File> selectedFile = new HashSet<>();

    private boolean showHidden;
    private boolean onSelecting;

    public static Comparator<File> COMPARATOR_NAME = new Comparator<File>() {
        @Override
        public int compare(File lhs, File rhs) {
            String name1 = lhs.getName().toLowerCase();
            String name2 = rhs.getName().toLowerCase();
            return name1.compareTo(name2); //升序
        }
    };

    public static Comparator<File> COMPARATOR_TIME = new Comparator<File>() {
        @Override
        public int compare(File lhs, File rhs) {
            return lhs.lastModified() - rhs.lastModified() > 0 ? -1 : 1; // 降序
        }
    };

    public static Comparator<File> COMPARATOR_SIZE = new Comparator<File>() {
        @Override
        public int compare(File lhs, File rhs) {
            if (lhs.isDirectory() && rhs.isDirectory()) {
                return -(lhs.listFiles().length - rhs.listFiles().length);
            } else {
                return lhs.length() - rhs.length() > 0 ? -1 : 1; //降序
            }
        }
    };

    private Comparator<File> sortingComparator = COMPARATOR_NAME;


    public File getItem(int position) {
        return listItems.get(position);
    }

    public boolean isShowHidden() {
        return showHidden;
    }

    public boolean isOnSelecting() {
        return onSelecting;
    }

    public boolean isFileSelected(int position) {
        return selectedFile.contains(getItem(position));
    }


    public void setSortingComparator(Comparator<File> sortingComparator) {
        this.sortingComparator = sortingComparator;
        refreshDir();
    }

    public void setShowHidden(boolean showHidden) {
        this.showHidden = showHidden;
        refreshDir();
    }

    public void setOnSelecting(boolean onSelecting) {
        this.onSelecting = onSelecting;

        if (!this.onSelecting) {
            selectedFile.clear();
        }

        notifyDataSetChanged();
    }


    public void setFileSelected(int position, boolean selected) {
        if (selected) {
            if (selectedFile.size() == 0) {
                mOnFileSelectingListener.onFileFirstSelected();
            }

            selectedFile.add(getItem(position));
            onSelecting = true;
        } else {
            selectedFile.remove(getItem(position));
            if (selectedFile.size() == 0) {
                onSelecting = false;
                mOnFileSelectingListener.onFileAllDismissed();
            }
        }
        notifyDataSetChanged();

    }

    public void setFileAllSelected() {
        for (File file : listItems) {
            if (file != null) {
                selectedFile.add(file);
            }
        }
        onSelecting = true;
        notifyDataSetChanged();
    }

    public FileListAdapter(Context context, File rootDir,
                           FileFragment.OnFileSelectingListener onFileSelectingListener,
                           FileFragment.OnFileOptionClickListener onFileOptionClickListener) {
        this.context = context;
        enterDir(rootDir);

        mOnFileSelectingListener = onFileSelectingListener;
        mOnFileOptionClickListener = onFileOptionClickListener;
    }


    public void enterDir(File rootFile) {
        dirEnterStack.add(rootFile);
        refreshDir();
    }

    public boolean quitDir() {
        if (dirEnterStack.size() == 1) {
            return false;
        } else {
            dirEnterStack.remove(dirEnterStack.size() - 1);
            refreshDir();
            return true;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView;

        if (viewType == VIEW_TYPE_HEADER) {
            itemView = inflater.inflate(R.layout.list_file_header, parent, false);
            return new HeaderViewHolder(itemView);
        } else {
            itemView = inflater.inflate(R.layout.list_file_item, parent, false);
            return new ItemViewHolder(itemView, context);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder h, final int position) {
        int viewType = getItemViewType(position);

        // 判断Layout类型
        if (viewType == VIEW_TYPE_HEADER) {
            HeaderViewHolder holder = (HeaderViewHolder) h;
            if (position == 0 && dirs.length > 0) {
                holder.headerText.setText(context.getString(R.string.list_header_folder));
            } else {
                holder.headerText.setText(context.getString(R.string.list_header_file));
            }
        } else {
            ItemViewHolder holder = (ItemViewHolder) h;
            holder.setPosition(position);

            // 设置文件信息
            final File file = getItem(position);
            String name = file.getName();
            holder.fileNameTextView.setText(name);

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
            String modifyTime = dateFormat.format(new Date(file.lastModified()));

            if (file.isDirectory()) { // 目录
                int itemCount = file.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return !pathname.isHidden();
                    }
                }).length;

                holder.fileInfoTextView.setText(String.format("%s  %s %d ", modifyTime, context.getString(R.string.list_header_file), itemCount));
                holder.fileIconImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_file_folder));

            } else { // 文件
                holder.fileInfoTextView.setText(String.format("%s  %s", modifyTime, StorageUtil.getReadableSize(file.length())));

                holder.fileIconImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_file_default));
            }

            // 设置文件图标背景等样式
            if (isNormal()) {
                holder.setItemStyleNormal(file);
            } else if (isFileSelected(position)) {
                holder.setItemStyleSelected(file);
            } else {
                holder.setItemStyleChoosing(file);
            }

            //事件监听
            holder.optionsImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnFileOptionClickListener.onFileOptionClick(file);
                }
            });

            holder.fileIconImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isNormal() && file.isDirectory()) {
                        enterDir(file);
                    } else {
                        if (isFileSelected(position)) {
                            setFileSelected(position, false);
                        } else {
                            setFileSelected(position, true);
                        }
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return listItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position) == null ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }

    private boolean isNormal() {
        return selectedFile.isEmpty() && !onSelecting;
    }

    private void refreshDir() {
        File rootFile = dirEnterStack.get(dirEnterStack.size() - 1);

        files = rootFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return (showHidden || (!pathname.isHidden())) && pathname.isFile();
            }
        });

        dirs = rootFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {

                return (showHidden || (!pathname.isHidden())) && pathname.isDirectory();
            }
        });


        Arrays.sort(files, sortingComparator);
        Arrays.sort(dirs, sortingComparator);

        listItems.clear();

        if (dirs.length > 0) {
            listItems.add(null);
            Collections.addAll(listItems, dirs);
        }

        if (files.length > 0) {
            listItems.add(null);
            Collections.addAll(listItems, files);
        }
        notifyDataSetChanged();
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public TextView headerText;

        public HeaderViewHolder(View itemView) {
            super(itemView);

            headerText = (TextView) itemView.findViewById(R.id.textView_file_cate);
        }
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public ImageView fileIconImageView;
        public TextView fileNameTextView;
        public TextView fileInfoTextView;
        public ImageButton optionsImageView;

        private int position;
        private Context context;

        public ItemViewHolder(View itemView, Context context) {
            super(itemView);
            this.context = context;

            fileIconImageView = (ImageView) itemView.findViewById(R.id.imageView_file_icon);
            fileNameTextView = (TextView) itemView.findViewById(R.id.textView_file_name);
            fileInfoTextView = (TextView) itemView.findViewById(R.id.textView_file_description);
            optionsImageView = (ImageButton) itemView.findViewById(R.id.imageButton_file_option);

            itemView.setOnClickListener(this);
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public void setItemStyleNormal(File file) {
            itemView.setBackgroundColor(context.getResources().getColor(android.R.color.background_light));
            fileIconImageView.setImageDrawable(getFileIcon(file));
            fileIconImageView.setBackgroundDrawable(null);
        }

        public void setItemStyleChoosing(File file) {
            itemView.setBackgroundColor(context.getResources().getColor(android.R.color.background_light));
            fileIconImageView.setImageDrawable(getFileIcon(file));
            fileIconImageView.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.shape_file_icon_bkg));
        }

        public void setItemStyleSelected(File file) {
            itemView.setBackgroundColor(context.getResources().getColor(R.color.fileItemSelectedBackground));
            fileIconImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_file_selected));
            fileIconImageView.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.shape_file_icon_bkg_selected));
        }

        private Drawable getFileIcon(File file) {
            if (file.isDirectory()) {
                return context.getResources().getDrawable(R.drawable.ic_file_folder);
            } else {
                return context.getResources().getDrawable(R.drawable.ic_file_default);
            }
        }

        @Override
        public void onClick(View v) {
            File file = getItem(position);

            if (isFileSelected(position)) {
                setFileSelected(position, false);
            } else {
                if (file.isDirectory()) {
                    enterDir(file);
                } else {
                    setFileSelected(position, true);
                }
            }
        }
    }

}

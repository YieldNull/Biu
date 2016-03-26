package com.bbbbiu.biu.gui.adapters.choose;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.choose.OnChoosingListener;
import com.bbbbiu.biu.gui.choose.OnItemOptionClickListener;
import com.bbbbiu.biu.util.Storage;

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

public class FileContentAdapter extends ContentBaseAdapter {

    private static final String TAG = FileContentAdapter.class.getSimpleName();

    /**
     * ViewType 类型
     */
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private Context context;
    private OnChoosingListener mOnChoosingListener;
    private OnItemOptionClickListener mOnItemOptionClickListener;

    /**
     * 当前目录下的文件或文件夹
     */
    private File[] dirs;
    private File[] files;

    private List<File> dirEnterStack = new ArrayList<>();
    private List<File> fileDataSet = new ArrayList<>();


    @Override
    public List<File> getChosenFiles() {
        return chosenFiles;
    }

    private ArrayList<File> chosenFiles = new ArrayList<>(); // 没必要用Set吧？ 本来查出来的File就没有重复的啊

    private boolean showHidden;

    public File getFileAt(int position) {
        return fileDataSet.get(position);
    }


    public boolean isFileChosen(int position) {
        return chosenFiles.contains(getFileAt(position));
    }

    public void dismissChoosing() {
        chosenFiles.clear();
        notifyDataSetChanged();
    }

    public void setFileChosen(int position, boolean chosen) {
        File file = getFileAt(position);

        if (chosen) {
            mOnChoosingListener.onFileChosen(file);
            chosenFiles.add(file);
        } else {
            chosenFiles.remove(file);
            mOnChoosingListener.onFileDismissed(file);
        }
        notifyDataSetChanged();

    }

    public int setFileAllChosen() {
        for (File file : fileDataSet) {
            if (file != null) {
                chosenFiles.add(file);
            }
        }

        notifyDataSetChanged();

        return fileDataSet.size();
    }

    public FileContentAdapter(Context context, File rootDir) {
        this.context = context;
        enterDir(rootDir);

        mOnChoosingListener = (OnChoosingListener) context;
        mOnItemOptionClickListener = (OnItemOptionClickListener) context;
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
            final File file = getFileAt(position);
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
                holder.fileIconImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_type_folder));

            } else { // 文件
                holder.fileInfoTextView.setText(String.format("%s  %s", modifyTime, Storage.getReadableSize(file.length())));

                holder.fileIconImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_type_default));
            }

            // 设置文件图标背景等样式
            if (isFileChosen(position)) {
                holder.setItemStyleChosen(file);
            } else {
                holder.setItemStyleChoosing(file);
            }

            //事件监听
            holder.optionsImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemOptionClickListener.onFileOptionClick(file);
                }
            });


            holder.fileIconImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isFileChosen(position)) {
                        setFileChosen(position, false);
                    } else {
                        setFileChosen(position, true);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return fileDataSet.size();
    }

    @Override
    public int getItemViewType(int position) {
        return getFileAt(position) == null ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }


    private void refreshDir() {
        File rootFile = dirEnterStack.get(dirEnterStack.size() - 1);

        files = rootFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return (showHidden || (!pathname.isHidden())) && pathname.isFile() && pathname.canRead();
            }
        });

        dirs = rootFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {

                return (showHidden || (!pathname.isHidden())) && pathname.isDirectory() && pathname.canExecute() && pathname.canRead();
            }
        });


        Comparator<File> comparator = new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                String name1 = lhs.getName().toLowerCase();
                String name2 = rhs.getName().toLowerCase();
                return name1.compareTo(name2); //升序
            }
        };

        Arrays.sort(files, comparator);
        Arrays.sort(dirs, comparator);

        fileDataSet.clear();

        if (dirs.length > 0) {
            fileDataSet.add(null);
            Collections.addAll(fileDataSet, dirs);
        }

        if (files.length > 0) {
            fileDataSet.add(null);
            Collections.addAll(fileDataSet, files);
        }
        notifyDataSetChanged();
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.textView_header)
        TextView headerText;

        HeaderViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @Bind(R.id.imageView_icon)
        ImageView fileIconImageView;

        @Bind(R.id.textView_name)
        TextView fileNameTextView;

        @Bind(R.id.textView_progress)
        TextView fileInfoTextView;

        @Bind(R.id.imageButton_option)
        ImageButton optionsImageView;

        private int position;
        private Context context;

        public ItemViewHolder(View itemView, Context context) {
            super(itemView);
            this.context = context;

            ButterKnife.bind(this, itemView);

            itemView.setOnClickListener(this);
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public void setItemStyleChoosing(File file) {
            itemView.setBackgroundColor(context.getResources().getColor(android.R.color.background_light));
            fileIconImageView.setImageDrawable(getFileIcon(file));
            fileIconImageView.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.shape_file_icon_bkg));
        }

        public void setItemStyleChosen(File file) {
            itemView.setBackgroundColor(context.getResources().getColor(R.color.file_item_chosen));
            fileIconImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_file_chosen));
            fileIconImageView.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.shape_file_icon_bkg_chosen));
        }

        private Drawable getFileIcon(File file) {
            if (file.isDirectory()) {
                return context.getResources().getDrawable(R.drawable.ic_type_folder);
            } else {
                return context.getResources().getDrawable(R.drawable.ic_type_default);
            }
        }

        @Override
        public void onClick(View v) {
            File file = getFileAt(position);

            if (isFileChosen(position)) {
                setFileChosen(position, false);
            } else {
                if (file.isDirectory()) {
                    enterDir(file);
                } else {
                    setFileChosen(position, true);
                }
            }
        }
    }

}

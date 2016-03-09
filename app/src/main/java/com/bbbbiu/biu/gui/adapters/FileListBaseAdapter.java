package com.bbbbiu.biu.gui.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bbbbiu.biu.R;
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

public class FileListBaseAdapter extends BaseAdapter {
    private static final String TAG = FileListBaseAdapter.class.getSimpleName();

    /**
     * ViewType 类型
     */
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private Context context;

    /**
     * 当前目录下的文件或文件夹
     */
    private File[] dirs;
    private File[] files;

    private List<File> dirEnterStack = new ArrayList<>();
    private List<File> listItems = new ArrayList<>();
    private HashSet<File> selectedFile = new HashSet<>();


    private Comparator<File> nameComparator = new Comparator<File>() {
        @Override
        public int compare(File lhs, File rhs) {
            String name1 = lhs.getName().toLowerCase();
            String name2 = rhs.getName().toLowerCase();
            return name1.compareTo(name2);
        }
    };

    private boolean onSelecting;


    public FileListBaseAdapter(Context context, File rootDir) {
        this.context = context;
        enterDir(rootDir);
    }

    public void handleItemClick() {

    }

    public void enterDir(File rootFile) {
        dirEnterStack.add(rootFile);
        refresh();
    }

    public boolean quitDir() {
        if (dirEnterStack.size() == 1) {
            return false;
        } else {
            dirEnterStack.remove(dirEnterStack.size() - 1);
            refresh();
            return true;
        }
    }

    public void refresh() {
        File rootFile = dirEnterStack.get(dirEnterStack.size() - 1);

        files = rootFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return (!pathname.isHidden()) && pathname.isFile();
            }
        });

        dirs = rootFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {

                return (!pathname.isHidden()) && pathname.isDirectory();
            }
        });


        Arrays.sort(files, nameComparator);
        Arrays.sort(dirs, nameComparator);

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

    @Override
    public int getCount() {
        return listItems.size();
    }

    @Override
    public Object getItem(int position) {

        return listItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {

        if (listItems.get(position) == null) {
            return VIEW_TYPE_HEADER;
        } else {
            return VIEW_TYPE_ITEM;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int viewType = getItemViewType(position);

        // 获取或初始化convertView
        if (convertView == null) {
            if (viewType == VIEW_TYPE_HEADER) {
                convertView = LayoutInflater.from(context).inflate(R.layout.list_file_header, parent, false);
            } else {
                convertView = LayoutInflater.from(context).inflate(R.layout.list_file_item, parent, false);
            }
        }

        // 判断Layout类型
        if (viewType == VIEW_TYPE_HEADER) {
            TextView headerText = (TextView) convertView.findViewById(R.id.textView_file_cate);
            if (position == 0 && dirs.length > 0) {
                headerText.setText("文件夹");
            } else {
                headerText.setText("文件");
            }
        } else {
            final ImageView fileIconimageView = (ImageView) convertView.findViewById(R.id.imageView_file_icon);
            TextView fileNameText = (TextView) convertView.findViewById(R.id.textView_file_name);
            TextView fileDescriptionText = (TextView) convertView.findViewById(R.id.textView_file_description);
            ImageView optionImage = (ImageView) convertView.findViewById(R.id.imageButton_file_option);
            LinearLayout fileInfoLayout = (LinearLayout) convertView.findViewById(R.id.linearLayout_file_info);

            // 设置文件信息
            final File file = (File) getItem(position);
            String name = file.getName();
            fileNameText.setText(name);

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String modifyTime = dateFormat.format(new Date(file.lastModified()));

            if (file.isDirectory()) { // 目录
                int itemCount = file.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return !pathname.isHidden();
                    }
                }).length;

                fileDescriptionText.setText(String.format("%s  文件 %d ", modifyTime, itemCount));
                fileIconimageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_file_folder));

            } else { // 文件
                fileDescriptionText.setText(String.format("%s  %s", modifyTime, StorageUtil.getReadableSize(file.length())));

                fileIconimageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_file_default));
            }

            // 设置文件图标背景

            if (onSelecting) {
                fileIconimageView.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.shape_file_icon_bkg));
            } else {
                fileIconimageView.setBackgroundDrawable(null);
            }

            if (selectedFile.contains(file)) {
                fileIconimageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_file_selected));
                fileIconimageView.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.shape_file_icon_bkg_selected));
            }


            //事件监听
            optionImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "嘻嘻嘻嘻");
                }
            });

            fileIconimageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onSelecting) {
                        if (selectedFile.contains(file)) {
                            fileIconimageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_file_default));
                            fileIconimageView.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.shape_file_icon_bkg));
                            selectedFile.remove(file);
                        } else {
                            fileIconimageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_file_selected));
                            fileIconimageView.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.shape_file_icon_bkg_selected));
                        }
                    }
                }
            });
        }
        return convertView;
    }
}

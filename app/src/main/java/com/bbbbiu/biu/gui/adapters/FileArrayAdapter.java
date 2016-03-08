package com.bbbbiu.biu.gui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.MainActivity;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class FileArrayAdapter extends BaseAdapter {
    private static final String TAG = FileArrayAdapter.class.getSimpleName();

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;
    private Context context;
    private File[] dirs;
    private File[] files;

    private List<File> items;

    public FileArrayAdapter(Context context, File rootFile) {
        this.context = context;

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

        items = new ArrayList<>();

        if (dirs.length > 0) {
            items.add(null);
            Collections.addAll(items, dirs);
        }

        if (files.length > 0) {
            items.add(null);
            Collections.addAll(items, files);
        }

    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {

        return items.get(position);
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

        if (items.get(position) == null) {
            return VIEW_TYPE_HEADER;
        } else {
            return VIEW_TYPE_ITEM;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int viewType = getItemViewType(position);

        if (convertView == null) {
            if (viewType == VIEW_TYPE_HEADER) {
                convertView = LayoutInflater.from(context).inflate(R.layout.list_file_header, parent, false);
            } else {
                convertView = LayoutInflater.from(context).inflate(R.layout.list_file_item, parent, false);
            }
        }

        if (viewType == VIEW_TYPE_HEADER) {
            TextView headerText = (TextView) convertView.findViewById(R.id.textView_file_cate);
            if (position == 0 && dirs.length > 0) {
                headerText.setText("文件夹");
            } else {
                headerText.setText("文件");
            }
        } else {
            ImageView imageView = (ImageView) convertView.findViewById(R.id.imageView_file_icon);
            TextView fileName = (TextView) convertView.findViewById(R.id.textView_file_name);
            TextView fileDescription = (TextView) convertView.findViewById(R.id.textView_file_description);
            ImageView option = (ImageView) convertView.findViewById(R.id.imageView_file_option);


            final File file = (File) getItem(position);
            fileName.setText(file.getName());

            SimpleDateFormat dateFormat = new SimpleDateFormat("yy-MM-dd");
            String modifyTime = dateFormat.format(new Date(file.lastModified()));

            if (file.isDirectory()) {
                int itemCount = file.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return !pathname.isHidden();
                    }
                }).length;

                fileDescription.setText(String.format("文件数：%d 修改时间：%s", itemCount, modifyTime));
                imageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_folder_blue_grey_400_24dp));

            } else {
                fileDescription.setText(String.format("大小：%s 修改时间：%s", humanReadableByteCount(file.length()), modifyTime));

                imageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_insert_drive_file_red_600_24dp));
            }

            option.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                }
            });
        }
        return convertView;
    }

    public static String humanReadableByteCount(long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "KMGTPE".charAt(exp - 1) + "i";
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}

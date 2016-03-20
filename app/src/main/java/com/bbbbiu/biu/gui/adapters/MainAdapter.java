package com.bbbbiu.biu.gui.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.choose.ApkChooseActivity;
import com.bbbbiu.biu.gui.choose.ArchiveChooseActivity;
import com.bbbbiu.biu.gui.choose.DocumentChooseActivity;
import com.bbbbiu.biu.gui.choose.DownloadChooseActivity;
import com.bbbbiu.biu.gui.choose.FileChooseActivity;
import com.bbbbiu.biu.gui.choose.ImageChooseActivity;
import com.bbbbiu.biu.gui.choose.MusicChooseActivity;
import com.bbbbiu.biu.gui.choose.VideoChooseActivity;
import com.bbbbiu.biu.util.StorageUtil;

import java.io.File;
import java.util.ArrayList;

public class MainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int SPAN_COUNT = 2;

    private static final int TYPE_STORAGE = 0;
    private static final int TYPE_CATEGORY = 1;
    private static final String TAG = MainAdapter.class.getSimpleName();

    private ArrayList<Integer[]> nameImgMap;
    private Context context;

    private int externalDirCount;
    private boolean hasRealExternal;

    public MainAdapter(Context context) {
        this.context = context;
        nameImgMap = new ArrayList<>();

        nameImgMap.add(new Integer[]{R.string.cate_image, R.drawable.ic_cate_image});
        nameImgMap.add(new Integer[]{R.string.cate_music, R.drawable.ic_cate_music});
        nameImgMap.add(new Integer[]{R.string.cate_video, R.drawable.ic_cate_video});
        nameImgMap.add(new Integer[]{R.string.cate_document, R.drawable.ic_cate_document});
        nameImgMap.add(new Integer[]{R.string.cate_archive, R.drawable.ic_cate_archive});
        nameImgMap.add(new Integer[]{R.string.cate_apk, R.drawable.ic_cate_apk});
        nameImgMap.add(new Integer[]{R.string.cate_download, R.drawable.ic_cate_download});
        nameImgMap.add(new Integer[]{R.string.cate_trash, R.drawable.ic_cate_trash});
        nameImgMap.add(new Integer[]{R.string.cate_storage, R.drawable.ic_cate_phone});

        externalDirCount = StorageUtil.getExternalDirCount(context);
        Log.d(TAG, "External Count" + String.valueOf(externalDirCount));

        if (externalDirCount == 2) {
            hasRealExternal = true;
            nameImgMap.add(new Integer[]{R.string.cate_sdcard, R.drawable.ic_cate_sdcard});
        } else if (externalDirCount == 1) {
            if (!Environment.getExternalStorageDirectory().getAbsolutePath().contains("/emulated")) {
                hasRealExternal = true;
                nameImgMap.add(new Integer[]{R.string.cate_sdcard, R.drawable.ic_cate_sdcard});
            }
        }
    }

    public int getSpanSize(int position) {
        int stringId = getStringId(position);
        return stringId == R.string.cate_sdcard || stringId == R.string.cate_storage ? 2 : 1;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        if (viewType == TYPE_CATEGORY) {
            View itemView = inflater.inflate(R.layout.list_main_item_category, parent, false);
            return new CategoryHolder(itemView);
        } else {
            View itemView = inflater.inflate(R.layout.list_main_item_storage, parent, false);
            return new StorageHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder hd, final int position) {
        final int stringId = getStringId(position);
        int imageId = nameImgMap.get(position)[1];

        if (getItemViewType(position) == TYPE_CATEGORY) {
            CategoryHolder holder = (CategoryHolder) hd;
            holder.cateImage.setImageDrawable(context.getResources().getDrawable(imageId));
            holder.cateText.setText(context.getString(stringId));

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Class theClass = null;
                    switch (getStringId(position)) {
                        case R.string.cate_image:
                            theClass = ImageChooseActivity.class;
                            break;
                        case R.string.cate_music:
                            theClass = MusicChooseActivity.class;
                            break;
                        case R.string.cate_video:
                            theClass = VideoChooseActivity.class;
                            break;
                        case R.string.cate_document:
                            theClass = DocumentChooseActivity.class;
                            break;
                        case R.string.cate_archive:
                            theClass = ArchiveChooseActivity.class;
                            break;
                        case R.string.cate_apk:
                            theClass = ApkChooseActivity.class;
                            break;
                        case R.string.cate_download:
                            theClass = DownloadChooseActivity.class;
                            break;
                        case R.string.cate_trash:
                            theClass = null;
                            break;
                        default:
                            break;
                    }
                    if (theClass != null) {
                        context.startActivity(new Intent(context, theClass));
                    }
                }
            });
        } else {
            StorageHolder holder = (StorageHolder) hd;
            holder.cateImage.setImageDrawable(context.getResources().getDrawable(imageId));
            holder.cateText.setText(context.getString(stringId));
            holder.setPercentage(position);
        }
    }

    @Override
    public int getItemCount() {
        return nameImgMap.size();
    }

    @Override
    public int getItemViewType(int position) {
        int stringId = getStringId(position);
        if (stringId == R.string.cate_storage || stringId == R.string.cate_sdcard) {
            return TYPE_STORAGE;
        } else {
            return TYPE_CATEGORY;
        }
    }

    private int getStringId(int position) {
        return nameImgMap.get(position)[0];
    }

    private class CategoryHolder extends RecyclerView.ViewHolder {
        public ImageView cateImage;
        public TextView cateText;

        public CategoryHolder(View itemView) {
            super(itemView);

            cateImage = (ImageView) itemView.findViewById(R.id.imageView_main_cate);
            cateText = (TextView) itemView.findViewById(R.id.textView_main_cate);
        }
    }

    private class StorageHolder extends RecyclerView.ViewHolder {
        public ImageView cateImage;
        public TextView cateText;
        public ProgressBar progressBar;
        public TextView storageText;

        public StorageHolder(View itemView) {
            super(itemView);

            cateImage = (ImageView) itemView.findViewById(R.id.imageView_main_cate);
            cateText = (TextView) itemView.findViewById(R.id.textView_main_cate);
            progressBar = (ProgressBar) itemView.findViewById(R.id.progressBar_main_storage);
            storageText = (TextView) itemView.findViewById(R.id.textView_main_storage);
        }

        public void setPercentage(int position) {
            int stringId = getStringId(position);
            File file = null;
            File file2Enter = null;

            if (stringId == R.string.cate_storage) {
                if (externalDirCount == 2) {  // 有两个外置，则用第一个表示手机存储，点击进入第一个外置
                    file = Environment.getExternalStorageDirectory();
                    file2Enter = file;
                } else {
                    file = context.getFilesDir();
                    if (externalDirCount == 1 && (!hasRealExternal)) {  // 有一个外置,但是不是真的外置,emulated
                        file2Enter = Environment.getExternalStorageDirectory();// 点击进入外置
                    } else {

                        // "/storage“ 目录里面有一些文件夹，很多是符号链接的
                        // 什么 usb emulated sdcard0 sdcard1 emmc等
                        File[] storage = new File("/storage").listFiles();
                        for (File f : storage) {
                            if ((f.canRead()) && (f.listFiles().length != 0) && (!f.getName().toLowerCase().contains("sdcard"))) {
                                file2Enter = f;
                                break;
                            }
                        }
                        if (file2Enter == null) {
                            file2Enter = new File("/");
                        }
                    }
                }
            } else if (stringId == R.string.cate_sdcard) {
                if (externalDirCount == 2) {
                    file = StorageUtil.getSecondaryExternalDir(context);// 有两个外置，进入第二个
                    file2Enter = file.getParentFile().getParentFile().getParentFile().getParentFile();
                } else {
                    file = Environment.getExternalStorageDirectory();// 有一个外置，进入第一个
                    file2Enter = file;
                }
            }

            Log.d(TAG, "file measure " + file.getAbsolutePath());
            Log.d(TAG, "file enter " + file2Enter.getAbsolutePath());

            setStoragePercentage(file);

            final File finalFile2Enter = file2Enter;
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, FileChooseActivity.class);
                    intent.putExtra(FileChooseActivity.INTENT_EXTRA_ROOT_FILE_PATH, finalFile2Enter.getAbsolutePath());
                    context.startActivity(intent);
                }
            });
        }

        private void setStoragePercentage(File file) {
            long total = file.getTotalSpace();
            long free = file.getFreeSpace();

            progressBar.setMax(100);
            progressBar.setProgress((int) (((total - free) / (total * 1.0)) * 100));

            String percentage = StorageUtil.getReadableSize(total - free) + "/" + StorageUtil.getReadableSize(total);
            storageText.setText(percentage);
        }
    }

}

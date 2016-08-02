package com.bbbbiu.biu.gui.adapter.choose.content;

import android.annotation.SuppressLint;
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
import com.bbbbiu.biu.gui.adapter.util.VideoIconRequestHandler;
import com.bbbbiu.biu.gui.choose.BaseChooseActivity;
import com.bbbbiu.biu.gui.choose.listener.OnChangeDirListener;
import com.bbbbiu.biu.util.StorageUtil;
import com.squareup.picasso.Picasso;

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
import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;

public class FileContentAdapter extends BaseContentAdapter {

    private static final String TAG = FileContentAdapter.class.getSimpleName();

    private OnChangeDirListener mOnChangeDirListener;

    /**
     * 当前目录下的文件或文件夹
     */
    private File mCurrentDir;
    private File[] mSubDirs;
    private File[] mSubFiles;

    /**
     * 当前显示的文件列表
     */
    private List<File> mFileDataSet = new ArrayList<>();

    /**
     * 已选择的文件
     */
    private Set<File> mChosenFiles = new HashSet<>();

    private boolean showHidden;

    private Picasso mVideoPicasso;
    private Picasso mImgPicasso;

    public FileContentAdapter(BaseChooseActivity context, File rootDir) {
        super(context);

        Picasso.Builder builder = new Picasso.Builder(context);
        builder.addRequestHandler(new VideoIconRequestHandler());
        mVideoPicasso = builder.build();
        mImgPicasso = Picasso.with(context);

        mOnChangeDirListener = (OnChangeDirListener) context;

        setCurrentDir(rootDir);
    }

    /***********************************************************************************
     * ***************************** {@link BaseContentAdapter} ************************
     **********************************************************************************/

    @Override
    public void cancelPicassoTask() {
        mVideoPicasso.cancelTag(PICASSO_TAG);
        mImgPicasso.cancelTag(PICASSO_TAG);
    }

    @Override
    public void updateDataSet() {
        setCurrentDir(mCurrentDir);
        notifyDataSetChanged();
    }

    @Override
    public boolean isHeaderView(int position) {
        return getFileAt(position) == null;
    }

    @Override
    public RecyclerView.ViewHolder onCreateItemViewHolder(LayoutInflater inflater, ViewGroup parent) {
        return new ItemViewHolder(inflater.inflate(R.layout.list_file_item, parent, false), context);
    }

    @Override
    public RecyclerView.ViewHolder onCreateHeaderViewHolder(LayoutInflater inflater, ViewGroup parent) {
        return HeaderViewHolder.build(inflater, parent);
    }

    @Override
    public boolean isFileChosen(File file) {
        return mChosenFiles.contains(file);
    }


    @Override
    public Set<String> getChosenFiles() {
        Set<String> list = new HashSet<>();
        for (File file : mChosenFiles) {
            list.add(file.getAbsolutePath());
        }
        return list;
    }

    @Override
    public int getChosenCount() {
        return mChosenFiles.size();
    }

    @Override
    public void setFileAllDismissed() {
        mChosenFiles.clear();
        notifyDataSetChanged();
    }

    @Override
    public void setFileAllChosen() {
        for (File file : mFileDataSet) {
            if (file != null && file.isFile()) { // 禁止选文件夹
                mChosenFiles.add(file);
            }
        }
        notifyDataSetChanged();
    }

    /********************************************************************************************/


    @Override
    public int getItemCount() {
        return mFileDataSet.size();
    }


    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder h, final int position) {
        int viewType = getItemViewType(position);

        // 判断Layout类型
        if (viewType == VIEW_TYPE_HEADER) {
            HeaderViewHolder holder = (HeaderViewHolder) h;
            if (position == 0 && mSubDirs.length > 0) {
                holder.headerText.setText(context.getString(R.string.header_folder));
            } else {
                holder.headerText.setText(context.getString(R.string.header_file));
            }
        } else {
            ItemViewHolder holder = (ItemViewHolder) h;
            holder.setPosition(position);

            // 设置文件信息
            final File file = getFileAt(position);
            String name = file.getName();
            holder.nameText.setText(name);

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
            String modifyTime = dateFormat.format(new Date(file.lastModified()));

            if (file.isDirectory()) { // 目录
                int itemCount = file.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return !pathname.isHidden();
                    }
                }).length;

                holder.infoText.setText(String.format("%s  %s %d ", modifyTime,
                        context.getString(R.string.header_file), itemCount));

                holder.iconImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_type_folder));

            } else { // 文件
                holder.infoText.setText(String.format("%s  %s", modifyTime,
                        StorageUtil.getReadableSize(file.length())));

                holder.iconImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_type_default));
            }

            // 设置文件图标背景等样式
            if (isFileChosen(position)) {
                holder.setItemStyleChosen(file);
            } else {
                holder.setItemStyleChoosing(file);
            }

            //事件监听
            holder.optionToggleImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    notifyOptionToggleClicked(file);
                }
            });

            // 禁止选文件夹好了
            if (file.isFile()) {
                holder.iconImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isFileChosen(position)) {
                            setFileChosen(position, false);
                        } else {
                            setFileChosen(position, true);
                        }
                    }
                });
            } else {
                holder.iconImage.setClickable(false); // ViewHolder是复用的，千万要记得删掉监听啊
            }
        }
    }

    /**************************************************************************************************/

    public File getCurrentDir() {
        return mCurrentDir;
    }

    /**
     * 设置当前文件夹
     */
    public void setCurrentDir(File rootDir) {
        mCurrentDir = rootDir;

        mSubFiles = rootDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return (showHidden || (!pathname.isHidden())) && pathname.isFile() && pathname.canRead();
            }
        });

        mSubDirs = rootDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {

                return (showHidden || (!pathname.isHidden())) && pathname.isDirectory() && pathname.canExecute() && pathname.canRead();
            }
        });

        // 默认升序排列
        Comparator<File> comparator = new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                String name1 = lhs.getName().toLowerCase();
                String name2 = rhs.getName().toLowerCase();
                return name1.compareTo(name2); //升序
            }
        };

        Arrays.sort(mSubFiles, comparator);
        Arrays.sort(mSubDirs, comparator);

        mFileDataSet.clear();

        // 优先显示文件夹，因此先加入文件夹
        if (mSubDirs.length > 0) {
            mFileDataSet.add(null);
            Collections.addAll(mFileDataSet, mSubDirs);
        }

        if (mSubFiles.length > 0) {
            mFileDataSet.add(null);
            Collections.addAll(mFileDataSet, mSubFiles);
        }
    }

    /**
     * 获取当前显示的指定位置的文件
     *
     * @param position 位置
     * @return 文件
     */
    public File getFileAt(int position) {
        return mFileDataSet.get(position);
    }


    /**
     * 文件是否已经被选择
     *
     * @param position 位置
     * @return 是否被选
     */
    public boolean isFileChosen(int position) {
        return isFileChosen(getFileAt(position));
    }


    /**
     * 选择文件或取消选择
     *
     * @param position 位置
     * @param chosen   true为选择，false为取消选择
     */
    public void setFileChosen(int position, boolean chosen) {
        File file = getFileAt(position);

        setFileChosen(file, chosen);
    }

    /**
     * 选择文件或取消选择
     *
     * @param file   文件
     * @param chosen 选择与否
     */
    public void setFileChosen(File file, boolean chosen) {
        if (chosen) {
            mChosenFiles.add(file);
            notifyFileChosen(file.getAbsolutePath());
        } else {
            mChosenFiles.remove(file);
            notifyFileDismissed(file.getAbsolutePath());
        }
        notifyDataSetChanged();
    }

    class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @Bind(R.id.imageView_icon)
        ImageView iconImage;

        @Bind(R.id.textView_name)
        TextView nameText;

        @Bind(R.id.textView_info)
        TextView infoText;

        @Bind(R.id.imageButton_option)
        ImageButton optionToggleImage;

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


        /**
         * 待选样式
         */
        public void setItemStyleChoosing(File file) {
            itemView.setBackgroundColor(context.getResources().getColor(android.R.color.background_light));
//            iconImage.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.shape_file_icon_bkg));
            iconImage.setBackgroundDrawable(null);

            if (StorageUtil.isVideoFile(file.getPath())) {
                mVideoPicasso.load(VideoIconRequestHandler.PICASSO_SCHEME_VIDEO + ":" + file.getAbsolutePath())
                        .resize(VideoIconRequestHandler.THUMB_SIZE, VideoIconRequestHandler.THUMB_SIZE)
                        .placeholder(R.drawable.ic_type_video)
                        .tag(PICASSO_TAG)
                        .into(iconImage);

            } else if (StorageUtil.isImgFile(file.getPath())) {
                mImgPicasso.load(file)
                        .resize(VideoIconRequestHandler.THUMB_SIZE, VideoIconRequestHandler.THUMB_SIZE)
                        .placeholder(R.drawable.ic_type_img)
                        .tag(PICASSO_TAG)
                        .into(iconImage);
            } else {
                iconImage.setImageDrawable(StorageUtil.getFileIcon(context, file));
            }
        }

        /**
         * 已选样式
         */
        public void setItemStyleChosen(File file) {
            itemView.setBackgroundColor(context.getResources().getColor(R.color.file_item_chosen));
            iconImage.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_file_chosen));
            iconImage.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.shape_file_icon_bkg_chosen));
        }


        @Override
        public void onClick(View v) {
            File file = getFileAt(position);

            if (isFileChosen(position)) {
                setFileChosen(position, false); // 取消选择
            } else {
                if (file.isDirectory()) {
                    mOnChangeDirListener.onEnterDir(file); // 进入文件夹
                } else {
                    setFileChosen(position, true); // 被选
                }
            }
        }
    }
}

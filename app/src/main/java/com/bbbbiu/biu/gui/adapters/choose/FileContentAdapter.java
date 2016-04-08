package com.bbbbiu.biu.gui.adapters.choose;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
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
import com.bbbbiu.biu.util.SizeUtil;
import com.bbbbiu.biu.util.StorageUtil;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
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
    private static final int THUMB_SIZE = (int) SizeUtil.convertDpToPixel(24);

    private Context context;
    private OnChoosingListener mOnChoosingListener;
    private OnItemOptionClickListener mOnItemOptionClickListener;
    private OnChangeDirListener mOnChangeDirListener;

    /**
     * 当前目录下的文件或文件夹
     */
    private File[] dirs;
    private File[] files;

    /**
     * 进入的文件夹Stack，栈底为初始进入的文件夹
     */
    private List<File> dirEnterStack = new ArrayList<>();

    /**
     * 当前显示的文件列表
     */
    private List<File> mFileList = new ArrayList<>();

    private File mCurrentDir;

    /**
     * 已选择的文件
     */
    private ArrayList<File> chosenFiles = new ArrayList<>(); // 没必要用Set吧？ 本来查出来的File就没有重复的啊

    private boolean showHidden;


    private Picasso mVideoPicasso;
    private Picasso mImgPicasso;

    private static final String PICASOO_SCHEME_VIDEO = "video-icon";
    private static final String PICASSO_TAG = "tag-img";

    public FileContentAdapter(Context context, File rootDir) {
        super(context);

        this.context = context;

        Picasso.Builder builder = new Picasso.Builder(context);
        builder.addRequestHandler(new VideoIconRequestHandler());
        mVideoPicasso = builder.build();
        mImgPicasso = Picasso.with(context);

        mOnChoosingListener = (OnChoosingListener) context;
        mOnItemOptionClickListener = (OnItemOptionClickListener) context;
        mOnChangeDirListener = (OnChangeDirListener) context;

        setCurrentDir(rootDir);
    }

    public File getCurrentDir() {
        return mCurrentDir;
    }

    /**
     * 设置当前文件夹
     */
    public void setCurrentDir(File rootDir) {
        mCurrentDir = rootDir;

        files = rootDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return (showHidden || (!pathname.isHidden())) && pathname.isFile() && pathname.canRead();
            }
        });

        dirs = rootDir.listFiles(new FileFilter() {
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

        Arrays.sort(files, comparator);
        Arrays.sort(dirs, comparator);

        mFileList.clear();

        // 优先显示文件夹，因此先加入文件夹
        if (dirs.length > 0) {
            mFileList.add(null);
            Collections.addAll(mFileList, dirs);
        }

        if (files.length > 0) {
            mFileList.add(null);
            Collections.addAll(mFileList, files);
        }
    }

    /**
     * 获取当前显示的指定位置的文件
     *
     * @param position 位置
     * @return 文件
     */
    public File getFileAt(int position) {
        return mFileList.get(position);
    }

    /**
     * 文件是否已经被选择
     *
     * @param position 位置
     * @return 是否被选
     */
    public boolean isFileChosen(int position) {
        return chosenFiles.contains(getFileAt(position));
    }


    /**
     * 选择文件
     *
     * @param position 位置
     * @param chosen   true为选择，false为取消选择
     */
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

    /**
     * 取消所有已选文件
     */
    public void dismissChoosing() {
        chosenFiles.clear();
        notifyDataSetChanged();
    }

    /**
     * 全选所有文件
     */
    public int setFileAllChosen() {
        for (File file : mFileList) {
            if (file != null) {
                chosenFiles.add(file);
            }
        }
        notifyDataSetChanged();
        return mFileList.size();
    }

    @Override
    public void cancelPicassoTask() {
        mVideoPicasso.cancelTag(PICASSO_TAG);
        mImgPicasso.cancelTag(PICASSO_TAG);
    }

    @Override
    public List<File> getChosenFiles() {
        return chosenFiles;
    }

    @Override
    public int getItemCount() {
        return mFileList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return getFileAt(position) == null ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView;

        if (viewType == VIEW_TYPE_HEADER) {
            itemView = inflater.inflate(R.layout.list_header_cate, parent, false);
            return new HeaderViewHolder(itemView);
        } else {
            itemView = inflater.inflate(R.layout.list_file_item, parent, false);
            return new ItemViewHolder(itemView, context);
        }
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder h, @SuppressLint("RecyclerView") final int position) {
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
                holder.fileInfoTextView.setText(String.format("%s  %s", modifyTime, StorageUtil.getReadableSize(file.length())));

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


        /**
         * 待选样式
         */
        public void setItemStyleChoosing(File file) {
            itemView.setBackgroundColor(context.getResources().getColor(android.R.color.background_light));
//            fileIconImageView.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.shape_file_icon_bkg));
            fileIconImageView.setBackgroundDrawable(null);

            if (StorageUtil.isVideoFile(file.getPath())) {
                mVideoPicasso.load(PICASOO_SCHEME_VIDEO + ":" + file.getAbsolutePath())
                        .resize(THUMB_SIZE, THUMB_SIZE)
                        .placeholder(R.drawable.ic_type_video)
                        .tag(PICASSO_TAG)
                        .into(fileIconImageView);

            } else if (StorageUtil.isImgFile(file.getPath())) {
                mImgPicasso.load(file)
                        .resize(THUMB_SIZE, THUMB_SIZE)
                        .placeholder(R.drawable.ic_type_img)
                        .tag(PICASSO_TAG)
                        .into(fileIconImageView);
            } else {
                fileIconImageView.setImageDrawable(StorageUtil.getFileIcon(context, file));
            }
        }

        /**
         * 已选样式
         */
        public void setItemStyleChosen(File file) {
            itemView.setBackgroundColor(context.getResources().getColor(R.color.file_item_chosen));
            fileIconImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_file_chosen));
            fileIconImageView.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.shape_file_icon_bkg_chosen));
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


    /**
     * 加载视频缩略图
     */
    class VideoIconRequestHandler extends RequestHandler {

        @Override
        public boolean canHandleRequest(Request data) {
            return PICASOO_SCHEME_VIDEO.equals(data.uri.getScheme());
        }

        @Override
        public Result load(Request request, int networkPolicy) throws IOException {
            String path = request.uri.toString().replace(PICASOO_SCHEME_VIDEO + ":", "");
            Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(path,
                    MediaStore.Video.Thumbnails.MICRO_KIND);

            return new Result(bitmap, Picasso.LoadedFrom.DISK);
        }
    }
}

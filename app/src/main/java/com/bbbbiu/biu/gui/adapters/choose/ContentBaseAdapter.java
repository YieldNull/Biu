package com.bbbbiu.biu.gui.adapters.choose;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.choose.ChooseBaseActivity;
import com.bbbbiu.biu.gui.choose.OnChoosingListener;
import com.bbbbiu.biu.gui.choose.OnItemOptionClickListener;
import com.bbbbiu.biu.util.db.ModelItem;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by YieldNull at 3/26/16
 */
public abstract class ContentBaseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = ContentBaseAdapter.class.getSimpleName();

    public static final int VIEW_TYPE_HEADER = 0;
    public static final int VIEW_TYPE_ITEM = 1;

    private OnLoadingDataListener loadingDataListener;
    private OnChoosingListener onChoosingListener;
    private OnItemOptionClickListener onItemOptionClickListener;

    protected Context context;

    /**
     * 数据集，用于显示
     */
    protected List<ModelItem> mDataSetItems = new ArrayList<>();

    /**
     * 已选项
     */
    protected List<ModelItem> mChosenItems = new ArrayList<>();

    /**
     * 自定义 Picasso Request Handler 时的 Request URI Schema
     */
    protected static final String PICASSO_SCHEME_VIDEO = "video-icon";

    /**
     * 为所有Picasso请求打上标签，以便之后取消
     */
    protected static final String PICASSO_TAG = "tag-img";


    /**
     * 取消当前未完成的PicassoTask
     */
    public abstract void cancelPicassoTask();

    public abstract RecyclerView.ViewHolder OnCreateItemViewHolder(LayoutInflater inflater, ViewGroup parent);

    public ContentBaseAdapter(ChooseBaseActivity context) {
        loadingDataListener = context;
        onChoosingListener = context;
        onItemOptionClickListener = context;
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView;

        if (viewType == VIEW_TYPE_HEADER) {
            itemView = inflater.inflate(R.layout.list_header_common, parent, false);
            return new HeaderViewHolder(itemView);
        } else {
            return OnCreateItemViewHolder(inflater, parent);
        }
    }

    @Override
    public int getItemCount() {
        return mDataSetItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return getItemAt(position) == null ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }

    /**
     * 获取已选文件的路径集
     *
     * @return 绝对路径集
     */
    public Set<String> getChosenFiles() {
        Set<String> set = new HashSet<>();
        for (ModelItem item : mChosenItems) {
            set.add(item.getPath());
        }
        return set;
    }

    /**
     * 已选项数量
     *
     * @return 数量
     */
    public int getChosenCount() {
        return mChosenItems.size();
    }

    /**
     * 获取数据集中position处的项
     *
     * @param position position
     * @return database model item
     */
    protected ModelItem getItemAt(int position) {
        return mDataSetItems.get(position);
    }

    /**
     * 是否被选
     *
     * @param position position
     * @return 是否被选
     */
    protected boolean isItemChosen(int position) {
        return mChosenItems.contains(getItemAt(position));
    }


    /**
     * 被选
     *
     * @param position position
     */
    protected void setItemChosen(int position) {
        ModelItem item = getItemAt(position);
        if (!mChosenItems.contains(item)) {
            mChosenItems.add(item);
            notifyFileChosen(item.getPath());
        } else {
            mChosenItems.remove(item);
            notifyFileDismissed(item.getPath());
        }
        notifyDataSetChanged();
    }

    /**
     * 当前数据集全被选
     */
    public void setFileAllChosen() {
        mChosenItems.clear();

        for (ModelItem item : mDataSetItems) {
            if (item != null) {
                mChosenItems.add(item);
            }
        }

        notifyDataSetChanged();
    }

    /**
     * 清除所有已选项目
     */
    public void setFileAllDismissed() {
        mChosenItems.clear();
        notifyDataSetChanged();
    }


    /**
     * header text
     *
     * @param position position of header view item
     * @return header text if it's a header at that position else null
     */
    protected String getHeaderText(int position) {
        if (getItemAt(position) == null) {
            return getItemAt(position + 1).getParentDirName();
        } else {
            return null;
        }
    }


    /**
     * 查询数据库中对应类型文件的数据
     *
     * @param type 类型
     * @return 是否没有纪录
     * @see ModelItem
     */
    protected boolean queryModelItems(int type) {
        for (Map.Entry<String, List<ModelItem>> entry : ModelItem.queryModelItems(type).entrySet()) {
            List<ModelItem> list = entry.getValue();
            if (list.size() > 0) {
                mDataSetItems.add(null);
                mDataSetItems.addAll(list);
            }
        }
        return mDataSetItems.size() != 0;
    }

    /**
     * notify正在加载
     */
    protected void notifyStartLoadingData() {
        loadingDataListener.OnStartLoadingData();
    }

    /**
     * notify加载完成
     */
    protected void notifyFinishLoadingData() {
        loadingDataListener.OnFinishLoadingData();
    }


    /**
     * notify文件被选
     * <p/>
     * 要在添加或删除文件之后进行notify
     *
     * @param filePath 文件路径
     */
    protected void notifyFileChosen(String filePath) {
        onChoosingListener.onFileChosen(filePath);
    }

    /**
     * notify文件被取消选择
     *
     * @param filePath 文件路径
     */
    protected void notifyFileDismissed(String filePath) {
        onChoosingListener.onFileDismissed(filePath);
    }

    /**
     * notify 文件选项菜单被点击
     *
     * @param file 文件
     */
    protected void notifyFileItemOptionClicked(File file) {
        onItemOptionClickListener.onFileOptionClick(file);
    }


    /**
     * 加载视频缩略图
     */
    protected class VideoIconRequestHandler extends RequestHandler {

        @Override
        public boolean canHandleRequest(Request data) {
            return PICASSO_SCHEME_VIDEO.equals(data.uri.getScheme());
        }

        @Override
        public Result load(Request request, int networkPolicy) throws IOException {
            String path = request.uri.toString().replace(PICASSO_SCHEME_VIDEO + ":", "");
            Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(path,
                    MediaStore.Video.Thumbnails.MICRO_KIND);

            return new Result(bitmap, Picasso.LoadedFrom.DISK);
        }
    }

    /**
     * viewHolder of header in recyclerView
     */
    protected static class HeaderViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.textView)
        TextView headerText;

        HeaderViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}

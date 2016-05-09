package com.bbbbiu.biu.gui.adapter.util;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;

import com.bbbbiu.biu.util.SizeUtil;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

import java.io.IOException;

/**
 * 加载视频缩略图
 */
public class VideoIconRequestHandler extends RequestHandler {

    /**
     * 自定义 Picasso Request Handler 时的 Request URI Schema
     */
    public static final String PICASSO_SCHEME_VIDEO = "video-icon";
    /**
     * ViewType 类型
     */
    public static final int THUMB_SIZE = (int) SizeUtil.convertDpToPixel(24);

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

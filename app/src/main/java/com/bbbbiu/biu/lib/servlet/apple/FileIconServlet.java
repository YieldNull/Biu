package com.bbbbiu.biu.lib.servlet.apple;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.bbbbiu.biu.util.StorageUtil;
import com.yieldnull.httpd.HttpDaemon;
import com.yieldnull.httpd.HttpRequest;
import com.yieldnull.httpd.HttpResponse;
import com.yieldnull.httpd.HttpServlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * 根据文件后缀名获取图标
 * <p/>
 * Created by YieldNull at 6/5/16
 */
public class FileIconServlet extends HttpServlet {

    /**
     * “/icon/filename”，根据filename的后缀名获取图标
     *
     * @param context context
     */
    public static void register(Context context) {
        HttpDaemon.registerServlet("^/icon/((?!/).)+$", new FileIconServlet(context));
    }

    private Context context;

    private FileIconServlet(Context context) {
        this.context = context;
    }

    @Override
    public HttpResponse doGet(HttpRequest request) {
        String uri = request.uri();

        Drawable drawable = StorageUtil.getFileIcon(context, new File(uri));

        BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
        Bitmap bitmap = bitmapDrawable.getBitmap();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] imageInByte = stream.toByteArray();

        return HttpResponse.newResponse("image/png",
                new ByteArrayInputStream(imageInByte), imageInByte.length);
    }

    @Override
    public HttpResponse doPost(HttpRequest request) {
        return null;
    }
}

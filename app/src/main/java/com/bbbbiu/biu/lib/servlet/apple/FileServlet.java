package com.bbbbiu.biu.lib.servlet.apple;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.bbbbiu.biu.db.TransferRecord;
import com.bbbbiu.biu.gui.transfer.FileItem;
import com.bbbbiu.biu.lib.servlet.ProgressBaseServlet;
import com.bbbbiu.biu.lib.util.HttpConstants;
import com.bbbbiu.biu.util.PreferenceUtil;
import com.yieldnull.httpd.HttpDaemon;
import com.yieldnull.httpd.HttpRequest;
import com.yieldnull.httpd.HttpResponse;
import com.yieldnull.httpd.ProgressNotifier;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * IOS端从安卓端以附件形式下载文件
 * <p/>
 * Created by YieldNull at 5/13/16
 */
public class FileServlet extends ProgressBaseServlet {
    private static final String TAG = FileServlet.class.getSimpleName();


    public static void register(Context context) {
        HttpDaemon.registerServlet(String.format("^%s/((?!/).)+$", HttpConstants.Apple.URL_DOWNLOAD), new FileServlet(context));
    }

    private FileServlet(Context context) {
        super(context);

    }

    @Override
    public HttpResponse doGet(HttpRequest request) {
        String hashCode = request.uri().replace(HttpConstants.Apple.URL_DOWNLOAD + "/", "");

        FileItem fileitem = null;

        for (FileItem item : PreferenceUtil.getFileItemsToSend((context))) {

            if ((item.hashCode() + "").equals(hashCode)) {
                fileitem = item;
                break;
            }
        }

        InputStream inputStream = null;
        if (fileitem != null) {
            try {
                inputStream = fileitem.inputStream(context);
            } catch (FileNotFoundException e) {
                Log.w(TAG, e);
            }
        }

        if (inputStream == null) {
            return HttpResponse.newResponse(HttpResponse.Status.NOT_FOUND,
                    HttpResponse.Status.NOT_FOUND.getDescription());
        }

        HttpResponse response = HttpResponse.newResponse(inputStream, fileitem.size);

        response.setProgressNotifier(new ProgressNotifier(fileitem.uri, getProgressListener(), fileitem.size));

        // APK 名称
        String name = fileitem.name;

        response.addHeader("Content-Disposition",
                String.format("attachment; filename=\"%s\";filename*=UTF-8''%s",
                        name,
                        Uri.encode(name)));

        TransferRecord.recordSending(fileitem.uri, fileitem.name, fileitem.size);

        return response;
    }
}

package com.bbbbiu.biu.lib.servlet.apple;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.bbbbiu.biu.lib.servlet.ProgressBaseServlet;
import com.bbbbiu.biu.lib.util.HttpConstants;
import com.bbbbiu.biu.util.PreferenceUtil;
import com.bbbbiu.biu.util.StorageUtil;
import com.yieldnull.httpd.HttpDaemon;
import com.yieldnull.httpd.HttpRequest;
import com.yieldnull.httpd.HttpResponse;
import com.yieldnull.httpd.ProgressNotifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
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
        File file = null;

        for (String path : PreferenceUtil.getFilesToSend(context)) {
            File f = new File(path);

            if ((f.hashCode() + "").equals(hashCode)) {
                file = f;
                break;
            }
        }

        InputStream inputStream = null;
        if (file != null) {
            try {
                inputStream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                Log.w(TAG, e);
            }
        }

        if (inputStream == null) {
            return HttpResponse.newResponse(HttpResponse.Status.NOT_FOUND,
                    HttpResponse.Status.NOT_FOUND.getDescription());
        }

        HttpResponse response = HttpResponse.newResponse(inputStream, file.length());

        response.setProgressNotifier(new ProgressNotifier(getProgressListener(), file.length()));

        // APK 名称
        String name = StorageUtil.getFileNameToSend(context, file);

        response.addHeader("Content-Disposition",
                String.format("attachment; filename=\"%s\";filename*=UTF-8''%s",
                        name,
                        Uri.encode(name)));


        return response;
    }
}

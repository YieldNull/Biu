package com.bbbbiu.biu.http.server.servlets;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.bbbbiu.biu.http.server.HttpDaemon;
import com.bbbbiu.biu.http.server.HttpRequest;
import com.bbbbiu.biu.http.server.HttpResponse;
import com.bbbbiu.biu.http.server.HttpServlet;
import com.bbbbiu.biu.http.server.ContentType;
import com.bbbbiu.biu.util.Storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

public class DownloadServlet extends HttpServlet {
    private static final String TAG = DownloadServlet.class.getSimpleName();
    private HashMap<String, File> hashFileMap = new HashMap<>();

    private DownloadServlet(Context context, List<Uri> fileUris) {
        super(context);
        List<Uri> fileUris1 = fileUris;


        for (Uri uri : fileUris) {
            File file = new File(Storage.getRealFilePath(context, uri));
            hashFileMap.put(String.valueOf(file.hashCode()), file);
        }
    }

    public static void register(Context context, List<Uri> fileUris) {
        HttpServlet servlet = new DownloadServlet(context, fileUris);
        HttpDaemon.regServlet("/download", servlet);
        HttpDaemon.regServlet("/download/.*", servlet);
    }

    @Override
    public HttpResponse doGet(HttpRequest request) {
        String path = request.getUri();
        if (path.equals("/download")) {

            String html = "";
            return HttpResponse.newResponse(html);
        } else {
            String hashCode = path.replace("/download/", "");

            File file = hashFileMap.get(hashCode);


            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                Log.e(TAG, e.toString());
            }

            HttpResponse response = HttpResponse.newChunkedResponse(HttpResponse.Status.OK, ContentType.MIME_STREAM, inputStream);
            response.addHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", file.getName()));

            return response;
        }
    }


}

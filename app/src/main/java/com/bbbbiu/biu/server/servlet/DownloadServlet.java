package com.bbbbiu.biu.server.servlet;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.bbbbiu.biu.server.HttpDaemon;
import com.bbbbiu.biu.server.HttpRequest;
import com.bbbbiu.biu.server.HttpResponse;
import com.bbbbiu.biu.server.HttpServlet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

public class DownloadServlet extends HttpServlet {
    private static final String TAG = DownloadServlet.class.getSimpleName();
    private List<Uri> fileUris;

    private DownloadServlet(Context context, List<Uri> fileUris) {
        super(context);
        this.fileUris = fileUris;
    }

    public static void register(Context context, List<Uri> fileUris) {
        HttpServlet servlet = new DownloadServlet(context, fileUris);
        HttpDaemon.clearServlet();
        HttpDaemon.regServlet("/", servlet);
        HttpDaemon.regServlet("/download/.*", servlet);
    }

    @Override
    public HttpResponse doGet(HttpRequest request) {
        String path = request.getUri();
        if (path.equals("/")) {
            StringBuilder sb = new StringBuilder();
            for (Uri uri : fileUris) {
                sb.append("<h1><a href=\"/download/\">Download ").append(uri.getPath()).append("</a></h1>");
            }
            String html = "<html><body>" + sb.toString() + "</body></html>";
            return HttpResponse.newFixedLengthResponse(html);

        } else {
            Uri fileUri = fileUris.get(0);


            InputStream inputStream = null;
            try {
                inputStream = context.getContentResolver().openInputStream(fileUri);
            } catch (FileNotFoundException e) {
                Log.e(TAG, e.toString());
            }

            return HttpResponse.newChunkedResponse(HttpResponse.Status.OK, "application/octet-stream", inputStream);
        }
    }


}

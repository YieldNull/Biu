package com.bbbbiu.biu.lib.android.servlets;

import android.content.Context;
import android.os.ResultReceiver;
import android.util.Log;

import com.bbbbiu.biu.gui.ConnectSenderActivity;
import com.bbbbiu.biu.gui.transfer.FileItem;
import com.bbbbiu.biu.lib.httpd.HttpRequest;
import com.bbbbiu.biu.lib.httpd.HttpResponse;
import com.bbbbiu.biu.lib.httpd.HttpServlet;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by YieldNull at 4/22/16
 */
public class ManifestServlet extends HttpServlet {
    private static final String TAG = ManifestServlet.class.getSimpleName();


    public ManifestServlet(Context context) {
        super(context);
    }

    @Override
    public HttpResponse doGet(HttpRequest request) {
        return null;
    }

    @Override
    public HttpResponse doPost(HttpRequest request) {
        Gson gson = new Gson();

        Log.i(TAG, request.getClientIp());
        Log.i(TAG, request.getCharacterEncoding());
        Log.i(TAG, request.getContentType());
        Log.i(TAG, request.getUri());

        ArrayList<FileItem> manifest = gson.fromJson(request.getText(), new TypeToken<ArrayList<FileItem>>() {
        }.getType());

        ConnectSenderActivity.finishConnection(context, manifest);
        return HttpResponse.newResponse("");
    }

}

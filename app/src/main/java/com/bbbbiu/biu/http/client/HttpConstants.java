package com.bbbbiu.biu.http.client;

import android.util.Log;

import com.bbbbiu.biu.http.util.ProgressNotifier;
import com.bbbbiu.biu.http.util.Streams;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by YieldNull at 3/23/16
 */
public class HttpConstants {
    private static final String TAG = HttpConstants.class.getSimpleName();

    public static final String HOST = "http://192.168.1.102:5000";
    public static final String URL_BIND = HOST + "/bind";
    public static final String URL_UPLOAD = HOST + "/client/upload";
    public static final String URL_FILE_LIST = HOST + "/client/list";

    public static final int BIND_ACTION_DOWNLOAD = 0;
    public static final int BIND_ACTION_UPLOAD = 1;

    private static final String BIND_WHAT_UPLOAD = "upload";
    private static final String BIND_WHAT_DOWNLOAD = "download";

    private static final OkHttpClient mHttpClient = new OkHttpClient.Builder()
            .connectTimeout(0, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .writeTimeout(0, TimeUnit.SECONDS)
            .build();


    public static OkHttpClient getHttpClient() {
        return mHttpClient;
    }

    public static Request newFileListRequest(String uid) {
        String url = URL_FILE_LIST + "?uid=" + uid;
        return new Request.Builder()
                .url(url)
                .build();
    }


    public static Request newFileDownloadRequest(String relativeURL, String uid) {
        String url = HOST + relativeURL + "?uid=" + uid;
        return new Request.Builder()
                .url(url)
                .build();
    }

    public static Request newBindRequest(String uid, int action) {
        String what = action == BIND_ACTION_UPLOAD ? BIND_WHAT_UPLOAD : BIND_WHAT_DOWNLOAD;
        String url = URL_BIND + "?uid=" + uid + "&what=" + what;
        return new Request.Builder()
                .url(url)
                .build();
    }

}

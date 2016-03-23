package com.bbbbiu.biu.client;

import android.util.Log;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by YieldNull at 3/23/16
 */
public class HttpManager {
    private static final String TAG = HttpManager.class.getSimpleName();

    public static final String HOST = "http://192.168.1.102";
    public static final String URL_BIND = HOST + "/bind";
    public static final String URL_UPLOAD = HOST + "/client/upload";
    public static final String URL_FILE_LIST = HOST + "/client/list";
    public static final String ACTION_UPLOAD = "upload";
    public static final String ACTION_DOWNLOAD = "download";

    public static final int BIND_ACTION_DOWNLOAD = 0;
    public static final int BIND_ACTION_UPLOAD = 1;

    private static final String BIND_WHAT_UPLOAD = "upload";
    private static final String BIND_WHAT_DOWNLOAD = "download";

    // TODO 下载，上传 大文件时的超时设置
    private static final OkHttpClient mHttpClient = new OkHttpClient.Builder()
            .connectTimeout(14, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(50, TimeUnit.SECONDS)
            .build();

    public static class FileItem {
        public String name;
        public String url;
        public long size;

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }

        public long getSize() {
            return size;
        }
    }

    public static boolean bind(String uid, int action) {
        String what = action == BIND_ACTION_UPLOAD ? BIND_WHAT_UPLOAD : BIND_WHAT_DOWNLOAD;
        String url = URL_BIND + "?uid=" + uid + "&what=" + what;
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response;
        try {
            response = mHttpClient.newCall(request).execute();
        } catch (IOException e) {
            Log.i(TAG, "Bind server failed. HTTP error " + e.toString());
            return false;
        }


        if (response.code() == 200) {
            return true;
        } else {
            Log.i(TAG, "Bind server failed. Response status code " + response.code());
            return false;
        }
    }

    public static List<FileItem> getFileList(String uid) {
        Request request = new Request.Builder()
                .url(HttpManager.URL_FILE_LIST + "?uid=" + uid)
                .build();

        Response response;
        try {
            response = mHttpClient.newCall(request).execute();
        } catch (SocketTimeoutException e) {
            Log.i(TAG, "Get file list timeout");

            return null;
        } catch (IOException e) {
            Log.i(TAG, "Get file list failed. HTTP error" + e.toString());

            return null;
        }

        if (response.code() != 200) {
            Log.i(TAG, "Get file list failed. Response status code " + response.code());
            return null;
        }

        final List<FileItem> files;
        try {

            Gson gson = new Gson();
            files = gson.fromJson(response.body().charStream(), new TypeToken<List<FileItem>>() {
            }.getType());

        } catch (JsonSyntaxException e) {
            Log.i(TAG, "Get file list failed. Response is not a valid json");
            Log.i(TAG, e.toString());
            return null;
        }

        Log.i(TAG, String.valueOf(files.size()));

        if (files.size() == 0) {
            return null;
        }
        return files;
    }

    public static boolean downloadFile(String relativeURL, String uid, File file) {
        String url = HOST + relativeURL + "?uid=" + uid;
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response;

        try {
            response = mHttpClient.newCall(request).execute();
        } catch (IOException e) {
            Log.i(TAG, "Download file failed. " + file.getName() + "  HTTP error" + e.toString());
            return false;
        }

        if (response.code() != 200) {
            Log.i(TAG, "Get file list failed. Response status code " + response.code());
            return false;
        }

        FileOutputStream fileInputStream = null;
        try {
            fileInputStream = new FileOutputStream(file);
            ByteStreams.copy(response.body().byteStream(), fileInputStream);

        } catch (FileNotFoundException e) {
            Log.i(TAG, e.toString());
        } catch (IOException e) {
            Log.w(TAG, "Store file failed", e);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    Log.i(TAG, e.toString());
                }
            }

            response.body().close();
        }

        return true;
    }
}

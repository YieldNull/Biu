package com.bbbbiu.biu.http.client;

import com.bbbbiu.biu.http.util.ProgressNotifier;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * Created by YieldNull at 3/23/16
 */
public class HttpConstants {
    private static final String TAG = HttpConstants.class.getSimpleName();

    public static final String HOST = "http://www.bbbbiu.com";
    public static final String URL_BIND = HOST + "/bind";
    public static final String URL_UPLOAD = HOST + "/api/upload";
    public static final String URL_FILE_LIST = HOST + "/api/filelist";

    public static final int BIND_ACTION_DOWNLOAD = 0;
    public static final int BIND_ACTION_UPLOAD = 1;

    private static final String BIND_WHAT_UPLOAD = "upload";
    private static final String BIND_WHAT_DOWNLOAD = "download";


    public static OkHttpClient newHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(0, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(0, TimeUnit.SECONDS)
                .build();
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

    public static Request newFileUploadRequest(String uid, File file, ProgressNotifier notifier) {
        String url = URL_UPLOAD;

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("uid", uid)
                .addFormDataPart("files", file.getName(), new FileRequestBody(null, file, notifier))
                .build();

        return new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
    }

    public static class FileRequestBody extends RequestBody {
        MediaType contentType;
        File file;
        ProgressNotifier notifier;

        final long DEFAULT_SEGMENT_SIZE = 8192;

        FileRequestBody(MediaType contentType, File file, ProgressNotifier notifier) {
            this.contentType = contentType;
            this.file = file;
            this.notifier = notifier;
        }

        @Override
        public MediaType contentType() {
            return contentType;
        }

        @Override
        public long contentLength() {
            return file.length();
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            System.setProperty("http.keepAlive", "false");
            Source source = null;
            try {
                source = Okio.source(file);
                //sink.writeAll(source);

                int read;
                while ((read = (int) source.read(sink.buffer(), DEFAULT_SEGMENT_SIZE)) != -1) {
                    sink.flush();
                    notifier.noteBytesRead(read);
                }
            } finally {
                Util.closeQuietly(source);
            }
        }
    }
}

package com.bbbbiu.biu.lib.util;

import android.content.Context;

import com.bbbbiu.biu.util.StorageUtil;
import com.yieldnull.httpd.ProgressNotifier;

import java.io.File;
import java.io.IOException;
import java.util.Map;
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
 * Created by YieldNull at 4/23/16
 */
public class HttpManager {
    private static final String TAG = HttpManager.class.getSimpleName();

    public static OkHttpClient newHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(0, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(0, TimeUnit.SECONDS)
                .build();
    }

    public static Request newFileUploadRequest(Context context, String uploadUrl, File file,
                                               Map<String, String> formData, ProgressNotifier notifier) {

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        if (formData != null) {
            for (Map.Entry<String, String> entry : formData.entrySet()) {
                builder.addFormDataPart(entry.getKey(), entry.getValue());
            }
        }


        builder.addFormDataPart(HttpConstants.FILE_FORM_NAME, StorageUtil.getFileNameToSend(context, file),
                new UploadRequestBody(null, file, notifier));

        return new Request.Builder()
                .url(uploadUrl)
                .post(builder.build())
                .build();
    }

    public static Request newRequest(String url) {
        return new Request.Builder()
                .url(url)
                .build();
    }

    public static Request newJsonRequest(String url, String jsonPayload) {
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonPayload);
        return new Request.Builder()
                .url(url)
                .post(body)
                .build();
    }

    /**
     * Created by YieldNull at 4/22/16
     */
    public static class UploadRequestBody extends RequestBody {
        MediaType contentType;
        File file;
        ProgressNotifier notifier;

        final long DEFAULT_SEGMENT_SIZE = 8192;

        public UploadRequestBody(MediaType contentType, File file, ProgressNotifier notifier) {
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

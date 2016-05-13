package com.bbbbiu.biu.lib.util;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

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

    public static Request newFileUploadRequest(String uploadUrl, File file,
                                               Map<String, String> formData, ProgressNotifier notifier) {

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        if (formData != null) {
            for (Map.Entry<String, String> entry : formData.entrySet()) {
                builder.addFormDataPart(entry.getKey(), entry.getValue());
            }
        }


        builder.addFormDataPart("files", file.getName(),
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

}

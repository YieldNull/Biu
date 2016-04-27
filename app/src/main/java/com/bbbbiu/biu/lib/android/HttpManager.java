package com.bbbbiu.biu.lib.android;

import android.webkit.MimeTypeMap;

import com.bbbbiu.biu.lib.util.ProgressNotifier;
import com.bbbbiu.biu.lib.util.UploadRequestBody;
import com.google.common.net.MediaType;

import java.io.File;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Created by YieldNull at 4/22/16
 */
public class HttpManager {

    private static final String TAG = HttpManager.class.getSimpleName();

    public static final String URI_MANIFEST = "/manifest";
    public static final String URI_UPLOAD = "/upload";

    public static OkHttpClient newHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(0, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(0, TimeUnit.SECONDS)
                .build();
    }

    public static Request newManifestSendRequest(InetAddress inetAddress, String manifest) {
        return new Request.Builder()
                .url(genUrl(inetAddress, URI_MANIFEST))
                .post(RequestBody.create(okhttp3.MediaType.parse(MediaType.JSON_UTF_8.toString()), manifest))
                .build();

    }



    private static String genUrl(InetAddress inetAddress, String uri) {
        return String.format("http://%s%s", inetAddress.getHostAddress(), uri);
    }
}

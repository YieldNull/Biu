package com.bbbbiu.biu.http.util;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class HtmlReader {
    private static final String TAG = HtmlReader.class.getSimpleName();

    /**
     * @param context context
     * @param path    在 “assets/html/” 中的路径
     * @return 读取到的源码
     */
    public static String readAll(Context context, String path) {
        StringBuilder sb;
        try {

            InputStream stream = context.getAssets().open("html/" + path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

            sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            Log.w(TAG, e.toString());
            return null;
        }
        return sb.toString();
    }
}


package com.bbbbiu.biu.lib.util;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 读取HTML模板文件
 */
public class HtmlReader {
    private static final String TAG = HtmlReader.class.getSimpleName();
    private static final String CRLF = "\r\n";

    /**
     * @param context context
     * @param path    在 “assets/html” 中的路径,如 "index.html"
     * @return 读取到的源码。找不到该路径下的文件或者读取文件时出现IOException 则返回null
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
                sb.append(CRLF); // 别忘了换行符
            }
        } catch (FileNotFoundException e) {
            Log.w(TAG, e.toString());
            return null;
        } catch (IOException e) {
            Log.w(TAG, e.toString());
            return null;
        }
        return sb.toString();
    }
}


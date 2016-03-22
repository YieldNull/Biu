package com.bbbbiu.biu.gui;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.client.Constants;
import com.bbbbiu.biu.util.StorageUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ReceiveActivity extends AppCompatActivity {
    public static final String INTENT_UID = "com.bbbbiu.biu.gui.ReceiveActivity.INTENT_UID";
    private static final String TAG = ReceiveActivity.class.getSimpleName();
    private String uid;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(14, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(50, TimeUnit.SECONDS)
            .build();

    private Handler handler = new HandlerClass(this);

    private static final int MSG_SHOW_FILE_LIST = 0;
    private static final int MSG_SHOW_FILE_DOWNLOADED = 1;
    private static final String BUNDLE_FILE_LIST = "com.bbbbiu.biu.gui.ReceiveActivity.BUNDLE_FILE_LIST";

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);

        uid = getIntent().getExtras().getString(INTENT_UID);

        textView = (TextView) findViewById(R.id.textView_receive);

        new Thread(new Runnable() {
            @Override
            public void run() {
                downFileList();
            }
        }).start();

    }

    private void downFileList() {
        Request request = new Request.Builder()
                .url(Constants.URL_FILE_LIST + "?uid=" + uid)
                .build();

        Response response = null;
        String content = null;
        try {
            response = client.newCall(request).execute();
            content = response.body().string();
        } catch (SocketTimeoutException e) {
            Log.i(TAG, "Timeout, retry");
            downFileList();
            return;
        } catch (IOException e) {
            Log.w(TAG, "Http error", e);
            return;
        }

        if (!response.isSuccessful()) {
            Log.w(TAG, "Unexpected code " + response);
        }

        if (response.code() == 200) {
            Log.d(TAG, content);
            if (content.equals("[]")) {
                downFileList();
            } else {
                Message message = new Message();
                message.what = MSG_SHOW_FILE_LIST;
                message.setTarget(handler);

                Bundle bundle = new Bundle();
                bundle.putString(BUNDLE_FILE_LIST, content);

                message.setData(bundle);
                message.sendToTarget();
            }
        } else {
            Log.w(TAG, String.valueOf(response.code()));
        }
    }

    private void downloadFile(JSONArray jsonArray) {
    }


    private void showFileList(String fileJson) {
        Log.d(TAG, fileJson);
        try {
            final JSONArray json = new JSONArray(fileJson);

            for (int i = 0; i < json.length(); i++) {
                JSONObject obj = json.getJSONObject(i);
                String name = obj.getString("name");
                String url = obj.getString("url");
                Long size = obj.getLong("size");
                String readableSize = StorageUtil.getReadableSize(size);

                String text = String.format("%s: %s\n%s\n\n", name, readableSize, url);
                textView.setText(textView.getText() + text);
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    downloadFile(json);
                }
            }).start();

        } catch (JSONException e) {
            Log.w(TAG, e.toString());
        }
    }


    private static class HandlerClass extends Handler {
        private final WeakReference<ReceiveActivity> mTarget;

        public HandlerClass(ReceiveActivity context) {
            mTarget = new WeakReference<>(context);

        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SHOW_FILE_LIST:
                    mTarget.get().showFileList(msg.getData().getString(BUNDLE_FILE_LIST));
                    break;
            }

        }
    }
}

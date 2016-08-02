package com.bbbbiu.biu.gui;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.transfer.android.SendingActivity;
import com.bbbbiu.biu.gui.transfer.computer.ConnectingActivity;
import com.bbbbiu.biu.util.PreferenceUtil;
import com.bbbbiu.biu.util.StorageUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ShareActivity extends Activity {

    private static final String TAG = ShareActivity.class.getSimpleName();


    private Set<String> filesToSend = new HashSet<>();


    @Bind(R.id.listView)
    protected ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        ButterKnife.bind(this);

        if (!handleIntent()) {
            Toast.makeText(this, R.string.hint_share_no_file_sendable, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        List<TargetItem> dataSet = new ArrayList<>();

        dataSet.add(new TargetItem(R.drawable.ic_share_android, R.string.list_share_android));
        dataSet.add(new TargetItem(R.drawable.ic_share_apple, R.string.list_share_apple));
        dataSet.add(new TargetItem(R.drawable.ic_share_computer, R.string.list_share_computer));
        mListView.setItemsCanFocus(false);
        mListView.setAdapter(new TargetArrayAdapter(this, dataSet));

    }


    /**
     * 处理要发送的文件
     *
     * @return 是否有能够发送的
     */
    private boolean handleIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (type == null) {
            return false;
        }

        if (action.equals(Intent.ACTION_SEND)) {
            if (type.equals("text/plain")) {
                String text = intent.getStringExtra(Intent.EXTRA_TEXT);

                String uri = StorageUtil.storeTextToSend(this, text);
                if (uri != null) {
                    filesToSend.add(uri);
                }
            } else {
                Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (isValid(uri)) {
                    filesToSend.add(uri.toString());
                }
            }
        } else if (action.equals(Intent.ACTION_SEND_MULTIPLE)) {
            ArrayList<Uri> fileUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            for (Uri uri : fileUris) {
                if (isValid(uri)) {
                    filesToSend.add(uri.toString());
                }
            }
        }

        if (filesToSend.size() == 0) {
            return false;
        } else {
            Log.i(TAG, String.format("Got %d shared file to send.", filesToSend.size()));

            for (String uri : filesToSend) {
                Log.i(TAG, "File Uri:" + uri);
            }
            return true;
        }
    }


    /**
     * 检测URI是否合法
     *
     * @param uri Uri
     * @return 是否合法
     */
    private boolean isValid(Uri uri) {
        if (uri.getScheme().equals(ContentResolver.SCHEME_FILE)) {
            return new File(uri.getPath()).canRead();
        } else if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            return true;
        }

        return false;
    }


    /**
     * 发送文件
     */
    private void sendFile(int resId) {

        PreferenceUtil.storeFilesToSend(this, filesToSend);

        switch (resId) {
            case R.drawable.ic_share_android:
                SendingActivity.startConnection(this);
                break;
            case R.drawable.ic_share_computer:
                ConnectingActivity.connectForSending(this);
                break;
            case R.drawable.ic_share_apple:
                com.bbbbiu.biu.gui.transfer.apple.ConnectingActivity.connectForSending(this);
                break;
            default:
                break;
        }
    }


    /**
     * 列表项
     */
    private static class TargetItem {
        int drawableId;
        int stringId;

        public TargetItem(int drawableId, int stringId) {
            this.drawableId = drawableId;
            this.stringId = stringId;
        }
    }


    /**
     * 列表Adapter
     */
    private class TargetArrayAdapter extends ArrayAdapter<TargetItem> {

        public TargetArrayAdapter(Context context, List<TargetItem> objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            final TargetItem item = getItem(position);

            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.list_share_item, parent, false);
            }


            TextView info = (TextView) view.findViewById(R.id.textView);
            ImageView icon = (ImageView) view.findViewById(R.id.imageView);


            info.setText(item.stringId);
            icon.setImageResource(item.drawableId);


            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendFile(item.drawableId);
                }
            });

            return view;
        }
    }

}

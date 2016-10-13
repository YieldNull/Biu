package com.bbbbiu.biu.gui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
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
import com.bbbbiu.biu.util.NetworkUtil;
import com.bbbbiu.biu.util.PreferenceUtil;
import com.bbbbiu.biu.util.StorageUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

/**
 * 接收其它应用分享的文件
 */
@RuntimePermissions
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        ShareActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    /**
     * 检查VPN连接，并发送
     */
    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void prepareSending(final int resId) {
        if (NetworkUtil.isVpnEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.dialog_vpn_title))
                    .setMessage(getString(R.string.dialog_vpn_message))
                    .setPositiveButton(getString(R.string.dialog_vpn_button_close), null)
                    .setNegativeButton(getString(R.string.dialog_vpn_button_ignore), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sendFile(resId);
                        }
                    })
                    .show();
        } else {
            sendFile(resId);
        }
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void onPermissionDenied() {
        finish();
    }

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void onShowRationale(final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setPositiveButton(R.string.permission_go_request, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        request.proceed();
                    }
                })
                .setCancelable(false)
                .setTitle(R.string.permission_dialog_title)
                .setMessage(R.string.permission_request_storage)
                .show();

    }

    @OnNeverAskAgain(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void onNeverAskAgain() {
        new AlertDialog.Builder(this)
                .setPositiveButton(R.string.permission_go_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.addCategory(Intent.CATEGORY_DEFAULT);
                        intent.setData(Uri.parse("package:" + ShareActivity.this.getPackageName()));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

                        startActivity(intent);
                    }
                })
                .setCancelable(false)
                .setTitle(R.string.permission_dialog_title)
                .setMessage(R.string.permission_request_storage_denied)
                .show();
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
                Uri streamUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

                if (streamUri != null) {
                    filesToSend.add(streamUri.toString());

                } else if (text != null) { // streamUri is null
                    String uri = StorageUtil.storeTextToSend(this, text);
                    Log.i(TAG, "Write plain text to file. " + uri);

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

        TargetItem(int drawableId, int stringId) {
            this.drawableId = drawableId;
            this.stringId = stringId;
        }
    }


    /**
     * 列表Adapter
     */
    private class TargetArrayAdapter extends ArrayAdapter<TargetItem> {

        TargetArrayAdapter(Context context, List<TargetItem> objects) {
            super(context, 0, objects);
        }

        @NonNull
        @Override
        public View getView(int position, View view, @NonNull ViewGroup parent) {
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
                    ShareActivityPermissionsDispatcher.prepareSendingWithCheck(ShareActivity.this, item.drawableId);
                }
            });

            return view;
        }
    }

}

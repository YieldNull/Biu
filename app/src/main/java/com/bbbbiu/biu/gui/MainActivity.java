package com.bbbbiu.biu.gui;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapter.MainAdapter;
import com.bbbbiu.biu.gui.transfer.android.ReceivingActivity;
import com.bbbbiu.biu.gui.transfer.computer.ConnectingActivity;
import com.bbbbiu.biu.util.NetworkUtil;
import com.github.clans.fab.FloatingActionMenu;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String ACTION_RECEIVE_ANDROID = "Android";
    private static final String ACTION_RECEIVE_COMPUTER = "Computer";
    private static final String ACTION_RECEIVE_APPLE = "Apple";


    @Bind(R.id.float_action_menu_main)
    FloatingActionMenu mActionMenu;

    @Bind(R.id.recyclerView)
    RecyclerView mRecyclerView;

    @OnClick(R.id.fbtn_receive_computer)
    void receiveComputer() {
        prepareReceiving(ACTION_RECEIVE_COMPUTER);
    }

    @OnClick(R.id.fbtn_receive_ios)
    void receiveApple() {
        prepareReceiving(ACTION_RECEIVE_APPLE);
    }

    @OnClick(R.id.fbtn_receive_android)
    void receiveAndroid() {
        prepareReceiving(ACTION_RECEIVE_ANDROID);
    }

    private MainAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // android 4.4 状态栏透明
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(getResources().getColor(R.color.colorPrimary));
        }


        mAdapter = new MainAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        GridLayoutManager manager = new GridLayoutManager(this, MainAdapter.SPAN_COUNT);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return mAdapter.getSpanSize(position);
            }
        });
        mRecyclerView.setLayoutManager(manager);


        mActionMenu.setIconAnimated(false);
        mActionMenu.setClosedOnTouchOutside(true);
    }


    @Override
    protected void onStart() {
        super.onStart();

        MainActivityPermissionsDispatcher.requestPermissionWithCheck(MainActivity.this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mAdapter.refreshRecentDownloads();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_history:
                startActivity(new Intent(this, HistoryActivity.class));
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @NeedsPermission({Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void requestPermission() {
    }


    @OnPermissionDenied({Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void onPermissionDenied() {
        MainActivityPermissionsDispatcher.requestPermissionWithCheck(MainActivity.this);
    }

    @OnShowRationale({Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
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
                .setMessage(R.string.permission_request_necessary)
                .show();

    }

    @OnNeverAskAgain({Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void onNeverAskAgain() {
        new AlertDialog.Builder(this)
                .setPositiveButton(R.string.permission_go_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.addCategory(Intent.CATEGORY_DEFAULT);
                        intent.setData(Uri.parse("package:" + MainActivity.this.getPackageName()));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

                        startActivity(intent);
                    }
                })
                .setCancelable(false)
                .setTitle(R.string.permission_dialog_title)
                .setMessage(R.string.permission_request_necessary_denied)
                .show();
    }

    /**
     * 检查是否开了VPN
     *
     * @param action 从谁接受
     */
    private void prepareReceiving(final String action) {
        mActionMenu.toggle(false);

        if (NetworkUtil.isVpnEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.dialog_vpn_title))
                    .setMessage(getString(R.string.dialog_vpn_message))
                    .setPositiveButton(getString(R.string.dialog_vpn_button_close), null)
                    .setNegativeButton(getString(R.string.dialog_vpn_button_ignore), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            receiveFile(action);
                        }
                    })
                    .show();
        } else {
            receiveFile(action);
        }
    }

    /**
     * 接受文件
     *
     * @param action 从谁接受
     */
    private void receiveFile(String action) {
        switch (action) {
            case ACTION_RECEIVE_ANDROID:
                ReceivingActivity.startConnection(this);
                break;
            case ACTION_RECEIVE_APPLE:
                com.bbbbiu.biu.gui.transfer.apple.ConnectingActivity.connectForReceiving(this);
                break;
            case ACTION_RECEIVE_COMPUTER:
                ConnectingActivity.connectForReceiving(this);
                break;
            default:
                break;
        }
    }
}

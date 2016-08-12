package com.bbbbiu.biu.gui.transfer.computer;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.WindowManager;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.lib.servlet.ManifestServlet;
import com.bbbbiu.biu.lib.servlet.apple.DownloadServlet;
import com.bbbbiu.biu.lib.servlet.apple.FileIconServlet;
import com.bbbbiu.biu.lib.servlet.apple.FileServlet;
import com.bbbbiu.biu.lib.servlet.apple.UploadServlet;
import com.bbbbiu.biu.service.HttpdService;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import butterknife.ButterKnife;
import butterknife.OnClick;


/**
 * 连接PC方式提示页面。若选择经由公网传输文件，则要检查设备有没有联网
 */
public class ConnectingActivity extends AppCompatActivity {
    private static final String TAG = ConnectingActivity.class.getSimpleName();

    private static final String ACTION_SEND = "com.bbbbiu.biu.gui.transfer.computer.ConnectComputerActivity.action.SEND";
    private static final String ACTION_RECEIVE = "com.bbbbiu.biu.gui.transfer.computer.ConnectComputerActivity.action.RECEIVE";

    public static void connectForSending(Context context) {
        Intent intent = new Intent(context, ConnectingActivity.class);
        intent.setAction(ACTION_SEND);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
    }

    public static void connectForReceiving(Context context) {
        Intent intent = new Intent(context, ConnectingActivity.class);
        intent.setAction(ACTION_RECEIVE);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
    }

    private String mAction;


    @OnClick(R.id.cardView_web)
    void scanQRCode() {

    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_computer);

        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // android 4.4 状态栏透明
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(getResources().getColor(R.color.colorPrimary));
        }

        mAction = getIntent().getAction();

        // 开HttpServer,注册servlet
        HttpdService.startService(this);

        if (mAction.equals(ACTION_RECEIVE)) {
            ManifestServlet.register(this, false);
            UploadServlet.register(this);
        } else {
            DownloadServlet.register(this);
            FileServlet.register(this);
        }

        FileIconServlet.register(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

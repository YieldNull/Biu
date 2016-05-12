package com.bbbbiu.biu.gui.transfer.apple;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.lib.servlet.apple.DownloadServlet;
import com.bbbbiu.biu.lib.servlet.apple.FileServlet;
import com.bbbbiu.biu.lib.servlet.apple.ManifestServlet;
import com.bbbbiu.biu.lib.servlet.apple.ReceivingServlet;
import com.bbbbiu.biu.lib.util.WifiApManager;
import com.bbbbiu.biu.service.HttpdService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import butterknife.Bind;
import butterknife.ButterKnife;


public class ConnectingActivity extends AppCompatActivity {
    private static final String TAG = ConnectingActivity.class.getSimpleName();

    private static final String ACTION_SENDING = "com.bbbbiu.biu.gui.transfer.apple.ConnectAppleActivity.action.UPLOAD";
    private static final String ACTION_RECEIVING = "com.bbbbiu.biu.gui.transfer.apple.ConnectAppleActivity.action.DOWNLOAD";


    public static void connectForSending(Context context) {
        Intent intent = new Intent(context, ConnectingActivity.class);
        intent.setAction(ACTION_SENDING);
        context.startActivity(intent);
    }

    public static void connectForReceiving(Context context) {
        Intent intent = new Intent(context, ConnectingActivity.class);
        intent.setAction(ACTION_RECEIVING);
        context.startActivity(intent);
    }

    @Bind(R.id.imageView)
    ImageView mQRCodeImage;


    private WifiApManager mApManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_apple);
        ButterKnife.bind(this);

        // Toolbar
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

        mApManager = new WifiApManager(this);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mApManager.createAp();
            }
        }, 1000);


        String url = "http://192.168.43.1:5050";

        mQRCodeImage.setImageBitmap(genQRCode(url));

        // 开HttpServer,注册servlet
        HttpdService.startService(this);

        String action = getIntent().getAction();

        if (action.equals(ACTION_RECEIVING)) {
            ManifestServlet.register(this);
            ReceivingServlet.register(this);
        } else {
            DownloadServlet.register(this);
            FileServlet.register(this);
        }
    }


    @Override
    protected void onDestroy() {
        mApManager.closeAp();
        HttpdService.stopService(this);

        super.onDestroy();
    }

    private Bitmap genQRCode(String info) {

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = (int) (size.x / 1.5);
        int height = size.y;

        Bitmap bitmap = null;
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bm = writer.encode(info, BarcodeFormat.QR_CODE, width, width);
            bitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < width; j++) {
                    bitmap.setPixel(i, j, bm.get(i, j) ? Color.BLACK : Color.WHITE);
                }
            }
        } catch (WriterException e) {
            Log.w(TAG, e.toString());
        }
        return bitmap;
    }
}

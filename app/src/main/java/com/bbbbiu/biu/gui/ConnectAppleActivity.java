package com.bbbbiu.biu.gui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;

import com.bbbbiu.biu.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import butterknife.Bind;
import butterknife.ButterKnife;


public class ConnectAppleActivity extends AppCompatActivity {
    private static final String TAG = ConnectAppleActivity.class.getSimpleName();

    private static final String ACTION_UPLOAD = "com.bbbbiu.biu.gui.ConnectAppleActivity.action.UPLOAD";
    private static final String ACTION_DOWNLOAD = "com.bbbbiu.biu.gui.ConnectAppleActivity.action.DOWNLOAD";


    public static void connectForUpload(Context context) {
        Intent intent = new Intent(context, ConnectAppleActivity.class);
        intent.setAction(ACTION_UPLOAD);
        context.startActivity(intent);
    }

    public static void connectForDownload(Context context) {
        Intent intent = new Intent(context, ConnectAppleActivity.class);
        intent.setAction(ACTION_DOWNLOAD);
        context.startActivity(intent);
    }

    @Bind(R.id.imageView)
    ImageView mQRCodeImage;

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


        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        String url = "http://" + ip + ":8080";

        mQRCodeImage.setImageBitmap(genQRCode(url));
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

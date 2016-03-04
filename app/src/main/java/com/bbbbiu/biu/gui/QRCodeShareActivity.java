package com.bbbbiu.biu.gui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.ImageView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.httpd.servlet.UploadServlet;
import com.bbbbiu.biu.service.HttpdService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;


public class QRCodeShareActivity extends AppCompatActivity {
    private static final String TAG = QRCodeShareActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code_share);

        ImageView mQRCodeImage = (ImageView) findViewById(R.id.imageView_qrcode_share);

        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        String url = "http://" + ip + ":8080";

        mQRCodeImage.setImageBitmap(genQRCode(url));

        HttpdService.startUpload(this);
        UploadServlet.register(this);
    }


    private Bitmap genQRCode(String info) {
        Bitmap bitmap = null;
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bm = writer.encode(info, BarcodeFormat.QR_CODE, 350, 350);
            bitmap = Bitmap.createBitmap(350, 350, Bitmap.Config.ARGB_8888);
            for (int i = 0; i < 350; i++) {
                for (int j = 0; j < 350; j++) {
                    bitmap.setPixel(i, j, bm.get(i, j) ? Color.BLACK : Color.WHITE);
                }
            }
        } catch (WriterException e) {
            Log.w(TAG, e.toString());
        }
        return bitmap;
    }
}

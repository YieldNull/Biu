package com.bbbbiu.biu.gui;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.httpd.servlet.DownloadServlet;
import com.bbbbiu.biu.service.HttpdService;

import java.util.ArrayList;

public class FileSendingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_sending);

        Uri uri = getIntent().getData();
        HttpdService.startDownload(FileSendingActivity.this);

        final ArrayList<Uri> list = new ArrayList<>();
        list.add(uri);

        TextView textView = (TextView) findViewById(R.id.textView_sending_send);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DownloadServlet.register(FileSendingActivity.this, list);
            }
        });
    }
}

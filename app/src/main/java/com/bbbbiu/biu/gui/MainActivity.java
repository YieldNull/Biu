package com.bbbbiu.biu.gui;

import android.os.Build;
import android.os.Bundle;
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
import com.bbbbiu.biu.gui.transfer.apple.ConnectAppleActivity;
import com.github.clans.fab.FloatingActionMenu;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();


    @Bind(R.id.float_action_menu_main)
    FloatingActionMenu actionMenu;

    @Bind(R.id.recyclerView)
    RecyclerView recyclerView;

    @OnClick(R.id.fbtn_receive_computer)
    void receiveComputer() {
        actionMenu.toggle(false);

        ConnectComputerActivity.connectForDownload(this);
    }

    @OnClick(R.id.fbtn_receive_ios)
    void receiveApple() {
        actionMenu.toggle(false);
        ConnectAppleActivity.connectForDownload(this);
    }

    @OnClick(R.id.fbtn_receive_android)
    void receiveAndroid() {
        actionMenu.toggle(false);
        ReceivingActivity.startConnection(this);
    }

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


        final MainAdapter adapter = new MainAdapter(this);
        recyclerView.setAdapter(adapter);

        GridLayoutManager manager = new GridLayoutManager(this, MainAdapter.SPAN_COUNT);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.getSpanSize(position);
            }
        });
        recyclerView.setLayoutManager(manager);


        actionMenu.setIconAnimated(false);
        actionMenu.setClosedOnTouchOutside(true);

//        FileScanService.scheduleAlarm(this);
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
            default:
                break;
        }
        return true;
    }


    // TODO check 在任何需要使用wifi的情况下，wifi是否可用？是不是要自动打开Wifi？
}

package com.bbbbiu.biu.gui;

import android.content.Intent;
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
import com.bbbbiu.biu.gui.transfer.computer.ConnectingActivity;
import com.github.clans.fab.FloatingActionMenu;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();


    @Bind(R.id.float_action_menu_main)
    FloatingActionMenu mActionMenu;

    @Bind(R.id.recyclerView)
    RecyclerView mRecyclerView;

    @OnClick(R.id.fbtn_receive_computer)
    void receiveComputer() {
        mActionMenu.toggle(false);

        ConnectingActivity.connectForReceiving(this);
    }

    @OnClick(R.id.fbtn_receive_ios)
    void receiveApple() {
        mActionMenu.toggle(false);
        com.bbbbiu.biu.gui.transfer.apple.ConnectingActivity.connectForReceiving(this);
    }

    @OnClick(R.id.fbtn_receive_android)
    void receiveAndroid() {
        mActionMenu.toggle(false);
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
        mRecyclerView.setAdapter(adapter);

        GridLayoutManager manager = new GridLayoutManager(this, MainAdapter.SPAN_COUNT);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.getSpanSize(position);
            }
        });
        mRecyclerView.setLayoutManager(manager);


        mActionMenu.setIconAnimated(false);
        mActionMenu.setClosedOnTouchOutside(true);
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
}

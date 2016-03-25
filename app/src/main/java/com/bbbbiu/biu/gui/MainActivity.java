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
import android.view.View;
import android.view.WindowManager;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapters.MainAdapter;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.readystatesoftware.systembartint.SystemBarTintManager;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private FloatingActionMenu actionMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // android 4.4 状态栏透明
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(getResources().getColor(R.color.colorPrimary));
        }

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);

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

        actionMenu = (FloatingActionMenu) findViewById(R.id.float_action_menu_main);
        actionMenu.setIconAnimated(false);
        actionMenu.setClosedOnTouchOutside(true);

        FloatingActionButton btn = (FloatingActionButton) findViewById(R.id.fbtn_receive_computer);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionMenu.toggle(false);

                Intent intent = new Intent(MainActivity.this, QRCodeScanActivity.class);

                intent.putExtra(QRCodeScanActivity.EXTRA_BIND_ACTION, QRCodeScanActivity.ACTION_DOWNLOAD);
                startActivity(intent);
            }
        });
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

}

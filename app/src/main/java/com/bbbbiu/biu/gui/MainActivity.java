package com.bbbbiu.biu.gui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapters.MainPageAdapter;
import com.bbbbiu.biu.gui.adapters.PopupArrayAdapter;
import com.bbbbiu.biu.httpd.servlet.DownloadServlet;
import com.bbbbiu.biu.service.HttpdService;
import com.github.clans.fab.FloatingActionMenu;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, PopupMenu.OnMenuItemClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private final int FILE_REQUEST_CODE = 1;

    private HttpdService mHttpdService;

    private ImageButton mReceiveMenuButton;
    private ImageButton mHistoryMenuButton;
    private ImageButton mSearchMenuButton;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            HttpdService.HttpdServiceBinder binder = (HttpdService.HttpdServiceBinder) service;
            mHttpdService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });


        // 右下侧浮动按钮
        FloatingActionMenu floatingActionMenu = (FloatingActionMenu) findViewById(R.id.float_menu);
        floatingActionMenu.setIconAnimated(false);
        floatingActionMenu.setClosedOnTouchOutside(true);

        // 抽屉
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        //左侧抽屉导航
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setItemIconTintList(null);

        // android 4.4 状态栏透明
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(getResources().getColor(R.color.colorPrimary));

            // 获取status bar的高度 然后让tool bar margin 一下
            FrameLayout.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) findViewById(R.id.main_container).getLayoutParams();
            SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
            p.setMargins(0, config.getStatusBarHeight(), 0, 0);
        }

//        Button btn = (Button) findViewById(R.id.button_main_send);
//
//        btn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
////                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
//                intent.setType("*/*");
//
//
//                HttpdService.startDownload(MainActivity.this);
//                startActivityForResult(intent, FILE_REQUEST_CODE);
//
//            }
//        });
//
//
//        Button btn2 = (Button) findViewById(R.id.button_main_receive);
//        btn2.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                Intent intent = new Intent(MainActivity.this, QRCodeShareActivity.class);
//                startActivity(intent);
//            }
//        });

//        bindService(new Intent(this, HttpdService.class), mServiceConnection, Service.BIND_ABOVE_CLIENT | Service.BIND_AUTO_CREATE);

        // 使用viewPager切换tab
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager_main);
        viewPager.setAdapter(new MainPageAdapter(getSupportFragmentManager(), MainActivity.this));
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout_main);
        tabLayout.setupWithViewPager(viewPager);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri fileUri;

        if (requestCode == FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                fileUri = data.getData();

                ArrayList<Uri> list = new ArrayList<>();
                list.add(fileUri);

                DownloadServlet.register(this, list);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mHttpdService != null) {
            mHttpdService.stopSelf();
            unbindService(mServiceConnection);
        }


        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_search:
                break;
            case R.id.action_history:
                break;
            case R.id.action_receive:
                View menuAction = findViewById(R.id.action_receive);
                createPopupWindow(menuAction);
                break;
        }

        return true;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void createPopupWindow(View v) {
        PopupWindow popup = new PopupWindow(this);

        ArrayList<PopupArrayAdapter.PopupItem> list = new ArrayList<>();
        list.add(new PopupArrayAdapter.PopupItem(R.drawable.ic_nav_computer, R.string.popup_window_android));
        list.add(new PopupArrayAdapter.PopupItem(R.drawable.ic_nav_files, R.string.popup_window_ios));
        list.add(new PopupArrayAdapter.PopupItem(R.drawable.ic_nav_history, R.string.popup_window_computer));


        ListView listView = (ListView) getLayoutInflater().inflate(R.layout.popup_window, null);
        listView.setAdapter(new PopupArrayAdapter(this, list));

        popup.setContentView(listView);
        popup.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        popup.setWidth(200);
        popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_popup_window));
        popup.setFocusable(true);
        popup.showAsDropDown(v, -120, 0);
    }


}

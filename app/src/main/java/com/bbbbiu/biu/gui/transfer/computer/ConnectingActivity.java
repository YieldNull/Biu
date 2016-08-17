package com.bbbbiu.biu.gui.transfer.computer;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapter.util.OnViewTouchListener;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import butterknife.Bind;
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


    @Bind(R.id.recyclerView)
    RecyclerView mRecyclerView;

    @OnClick(R.id.cardView_scan)
    void scanQRCode() {
        if (mAction.equals(ACTION_RECEIVE)) {
            QRCodeScanActivity.scanForDownload(this);
        } else {
            QRCodeScanActivity.scanForUpload(this);
        }
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

        final HintAdapter adapter = new HintAdapter();
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        boolean wifiConnected = wifiInfo.getState() == NetworkInfo.State.CONNECTED;

        // 已连上路由器，则显示用路由器传
        // 没有连接wifi则显示用热点传
        adapter.router = wifiConnected;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                adapter.removed = false;
                adapter.notifyItemInserted(0);
            }
        }, 500);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    class HintAdapter extends RecyclerView.Adapter<HintViewHolder> {

        private boolean removed = true;
        private boolean router = true;

        @Override
        public HintViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(ConnectingActivity.this);

            int layout = router ? R.layout.hint_connpc_local : R.layout.hint_connpc_ap;
            return new HintViewHolder(inflater.inflate(layout, parent, false));
        }

        @Override
        public void onBindViewHolder(final HintViewHolder holder, int position) {

            holder.cancel.setOnTouchListener(new OnViewTouchListener(ConnectingActivity.this, R.color.connpc_hint_divider));
            holder.ok.setOnTouchListener(new OnViewTouchListener(ConnectingActivity.this, R.color.connpc_hint_divider));

            holder.ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.ok.setBackgroundColor(getResources().getColor(android.R.color.transparent));

                    if (mAction.equals(ACTION_SEND)) {
                        com.bbbbiu.biu.gui.transfer.apple.ConnectingActivity.connectForSending(ConnectingActivity.this, router);
                    } else {
                        com.bbbbiu.biu.gui.transfer.apple.ConnectingActivity.connectForReceiving(ConnectingActivity.this, router);
                    }
                }
            });

            holder.cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.cancel.setBackgroundColor(getResources().getColor(android.R.color.transparent));

                    removed = true;
                    notifyItemRemoved(0);
                }
            });
        }

        @Override
        public int getItemCount() {
            return removed ? 0 : 1;
        }
    }

    class HintViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.textView_cancel)
        TextView cancel;

        @Bind(R.id.textView_ok)
        TextView ok;

        public HintViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }
    }
}

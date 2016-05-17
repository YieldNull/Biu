package com.bbbbiu.biu.gui.choose;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapter.choose.FileMoveAdapter;
import com.bbbbiu.biu.gui.choose.listener.OnChangeDirListener;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.io.File;
import java.util.Stack;

import butterknife.Bind;
import butterknife.ButterKnife;

public class FileMoveActivity extends AppCompatActivity implements OnChangeDirListener {
    public static final String EXTRA_DEST_DIR = "com.bbbbiu.biu.gui.choose.FileMoveActivity.extra_DEST_DIR";
    
    public static final int REQUEST_MOVE = 1;
    public static final int REQUEST_COPY = 2;

    @Bind(R.id.fab)
    protected FloatingActionButton mActionButton;

    @Bind(R.id.recyclerView)
    protected RecyclerView mRecyclerView;

    private FileMoveAdapter mAdapter;
    private Stack<File> mDirStack = new Stack<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_move);

        ButterKnife.bind(this);

        // Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.title_activity_file_move));
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_indicator_cancel);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // android 4.4 状态栏透明
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(getResources().getColor(R.color.colorPrimary));
        }

        mAdapter = new FileMoveAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(this).build());


        mActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mDirStack.size() == 0) {
                    Toast.makeText(FileMoveActivity.this, "请先选择文件夹", Toast.LENGTH_SHORT).show();
                } else {
                    sendResult(mDirStack.peek());
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mDirStack.size() == 0) {
            super.onBackPressed();
        } else {
            onExitDir(null);
        }
    }

    @Override
    public void onEnterDir(File dir) {
        mDirStack.add(dir);
        mAdapter.setCurrentDir(dir);

        mRecyclerView.swapAdapter(mAdapter, true);
    }

    @Override
    public void onExitDir(File dir) {
        mDirStack.pop();

        File file;
        if (mDirStack.empty()) {
            file = null;
        } else {
            file = mDirStack.peek();
        }

        mAdapter.setCurrentDir(file);
        mRecyclerView.swapAdapter(mAdapter, true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            sendResult(null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sendResult(File file) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DEST_DIR, file);
        setResult(RESULT_OK, intent);

        finish();
    }
}

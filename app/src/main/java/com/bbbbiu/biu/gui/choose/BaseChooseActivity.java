package com.bbbbiu.biu.gui.choose;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapter.choose.content.BaseContentAdapter;
import com.bbbbiu.biu.gui.choose.listener.OnChoosingListener;
import com.bbbbiu.biu.gui.choose.listener.OnLoadingDataListener;
import com.bbbbiu.biu.gui.transfer.android.SendingActivity;
import com.bbbbiu.biu.gui.transfer.computer.ConnectingActivity;
import com.bbbbiu.biu.util.NetworkUtil;
import com.bbbbiu.biu.util.PreferenceUtil;
import com.github.clans.fab.FloatingActionMenu;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.wang.avi.AVLoadingIndicatorView;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 选各类文件的基类。
 * <p/>
 * 各类文件选择共用一个布局文件“R.layout.activity_choose_base”
 * 界面中主内容为一个RecyclerView，记为 ContentRecyclerView
 * 底部有一个滑出菜单，也是RecyclerView，记为 OptionRecyclerView
 * <p/>
 * 有五个抽象方法，提供相应实现即可
 */
public abstract class BaseChooseActivity extends AppCompatActivity implements
        OnChoosingListener, OnLoadingDataListener {

    private static final String TAG = BaseChooseActivity.class.getSimpleName();

    private static final String ACTION_SEND_ANDROID = "Android";
    private static final String ACTION_SEND_COMPUTER = "Computer";
    private static final String ACTION_SEND_APPLE = "Apple";


    /**
     * layout资源id,必须包括下面被bind的view
     */
    private int mLayoutId;

    protected BaseChooseActivity() {
        mLayoutId = R.layout.activity_choose_base;
    }

    protected BaseChooseActivity(int layoutId) {
        mLayoutId = layoutId;
    }


    /**
     * 开始选择，不保留Activity
     *
     * @param context  context
     * @param theClass class of activity
     */
    public static void startChoosing(Context context, Class<? extends BaseChooseActivity> theClass) {
        Intent intent = new Intent(context, theClass);
        context.startActivity(intent);
    }


    @Bind(R.id.recyclerView_content)
    protected RecyclerView mContentRecyclerView;

    @Bind(R.id.float_action_menu)
    protected FloatingActionMenu mFloatingActionMenu;

    @Bind(R.id.loadingIndicatorView)
    protected AVLoadingIndicatorView mLoadingIndicatorView;

    @Bind(R.id.textView_empty)
    protected TextView mEmptyTextView;


    @OnClick(R.id.fbtn_send_android)
    protected void clickSendAndroid() {
        prepareSending(ACTION_SEND_ANDROID);
    }

    @OnClick(R.id.fbtn_send_apple)
    protected void clickSendApple() {
        prepareSending(ACTION_SEND_APPLE);
    }

    @OnClick(R.id.fbtn_send_computer)
    protected void clickSendComputer() {
        prepareSending(ACTION_SEND_COMPUTER);
    }


    /**
     * 纪录滑动的位置，为隐藏、显示floating button用
     */
    private int mPreviousVisibleItem;


    protected BaseContentAdapter mContentAdapter;
    protected LinearLayoutManager mContentLayoutManager;


    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(mLayoutId);
        ButterKnife.bind(this);

        // Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.title_activity_choose));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // android 4.4 状态栏透明
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintColor(getResources().getColor(R.color.colorPrimary));
        }

        if (getNormalTitle() != null) {
            getSupportActionBar().setTitle(getNormalTitle());
        }

        // 主内容 RecyclerView
        mContentAdapter = onCreateContentAdapter();
        mContentRecyclerView.setAdapter(mContentAdapter);

        mContentLayoutManager = onCreateContentLayoutManager();
        mContentRecyclerView.setLayoutManager(mContentLayoutManager);

        RecyclerView.ItemDecoration itemDecoration = onCreateContentItemDecoration();
        if (itemDecoration != null) {
            mContentRecyclerView.addItemDecoration(itemDecoration);
        }

        // floating action menu
        mFloatingActionMenu.setIconAnimated(false);
        mFloatingActionMenu.setClosedOnTouchOutside(true);
        mFloatingActionMenu.setOnMenuButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mContentAdapter.getChosenFiles().size() == 0) {
                    Toast.makeText(BaseChooseActivity.this, R.string.hint_choose_file_required,
                            Toast.LENGTH_SHORT).show();
                } else {
                    mFloatingActionMenu.toggle(true);
                }
            }
        });

        // floating action menu show && hide动画
        mFloatingActionMenu.setMenuButtonShowAnimation(AnimationUtils.loadAnimation(this, R.anim.show_from_bottom));
        mFloatingActionMenu.setMenuButtonHideAnimation(AnimationUtils.loadAnimation(this, R.anim.hide_to_bottom));

        // 滑动隐藏 floating button
        mContentRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int firstPosition = mContentLayoutManager.findFirstVisibleItemPosition();

                if (firstPosition > mPreviousVisibleItem) {
                    mFloatingActionMenu.hideMenu(true);
                } else if (firstPosition < mPreviousVisibleItem) {
                    mFloatingActionMenu.showMenu(true);
                }
                mPreviousVisibleItem = firstPosition;
            }
        });
    }


    /**
     * 未选择文件时的 menu resource id
     *
     * @return id。 0表示没有menu
     */
    protected abstract int getNormalMenuId();

    /**
     * 已选择文件时的 menu resource id
     *
     * @return id。 0表示没有menu
     */
    protected abstract int getChosenMenuId();

    /**
     * 未选择文件时的title
     *
     * @return title
     */
    protected abstract String getNormalTitle();

    /**
     * Create Adapter for ContentRecyclerView
     *
     * @return ContentAdapter
     */
    protected abstract BaseContentAdapter onCreateContentAdapter();

    /**
     * ContentRecyclerView 中各项内容的分割线
     * <p/>
     * 当LayoutManager使用LinearLayoutManager时，
     * 返回 new HorizontalDividerItemDecoration.Builder(this).build();
     * <p/>
     * 当使用GridLayoutManager使，返回相应ItemDecoration
     * <p/>
     * 参见开源库 com.yqritc:recyclerview-flexibledivider
     *
     * @return 可以为空
     */
    protected abstract RecyclerView.ItemDecoration onCreateContentItemDecoration();


    /**
     * ContentRecyclerView对应的布局管理
     *
     * @return 布局管理
     */
    protected abstract LinearLayoutManager onCreateContentLayoutManager();

    /********************************************************************************************/


    @Override
    public void onBackPressed() {
        mContentAdapter.cancelPicassoTask(); //取消加载图片任务
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mContentAdapter.getChosenCount() == 0) {
            int id = getNormalMenuId();
            if (id > 0) {
                getMenuInflater().inflate(id, menu);
            }
        } else {
            int id = getChosenMenuId();
            if (id > 0) {
                getMenuInflater().inflate(getChosenMenuId(), menu);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finish();
                return true;

            case R.id.action_choose_all:
                setFileAllChosen();
                return true;

            case R.id.action_choosing_dismiss:
                setFileAllDismissed();
                return true;

            case R.id.action_delete:
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.hint_file_delete_confirm))
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteChosenFile();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                showToastInUI(R.string.hint_file_delete_dismissed);
                            }
                        }).show();
                return true;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /********************************************************************************************/

    /**********************************************************************************************
     * **********************************{@link OnChoosingListener}*****************************
     *********************************************************************************************/

    @Override
    public void onFileChosen(String filePath) {
        if (mContentAdapter.getChosenCount() == 1) {
            invalidateOptionsMenu();
        }
        refreshTitle();
    }

    @Override
    public void onFileDismissed(String filePath) {
        if (mContentAdapter.getChosenCount() == 0) {
            invalidateOptionsMenu();
        }
        refreshTitle();
    }

    /********************************************************************************************/


    /*********************************{@link OnLoadingDataListener}******************************/

    /**
     * 加载数据时显示loading动画
     */
    @Override
    public void onStartLoadingData() {
        mLoadingIndicatorView.setVisibility(View.VISIBLE);
    }

    /**
     * 数据加载完毕，关闭loading动画
     */
    @Override
    public void onFinishLoadingData() {
        mLoadingIndicatorView.setVisibility(View.GONE);
    }

    @Override
    public void onEmptyDataSet() {
        mEmptyTextView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onNonEmptyDataSet() {
        mEmptyTextView.setVisibility(View.GONE);
    }

    /********************************************************************************************/

    /**
     * 更新title，显示选中数量
     */
    @SuppressWarnings("ConstantConditions")
    protected void refreshTitle() {
        if (mContentAdapter.getChosenCount() == 0) {
            getSupportActionBar().setTitle(getNormalTitle());
        } else {
            getSupportActionBar().setTitle(getString(R.string.title_chosen_file_count,
                    mContentAdapter.getChosenCount()));
        }
    }

    /**
     * 从Toolbar上面的菜单项选中当前所有显示的文件
     */
    protected void setFileAllChosen() {
        mContentAdapter.setFileAllChosen();
        invalidateOptionsMenu();
        refreshTitle();
    }

    /**
     * 从Toolbar上面的菜单项取消所有已选择的文件
     */
    protected void setFileAllDismissed() {
        mContentAdapter.setFileAllDismissed();
        invalidateOptionsMenu();
        refreshTitle();
    }

    /**
     * 从Toolbar上面的菜单项删除选中的文件
     */
    protected void deleteChosenFile() {
        final Set<String> chosenFiles = mContentAdapter.getChosenFiles();
        setFileAllDismissed();

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean allSucceeded = true;
                for (String path : chosenFiles) {
                    if (!FileUtils.deleteQuietly(new File(path))) {
                        allSucceeded = false;
                    }
                }

                if (!allSucceeded) {
                    showToastInUI(R.string.hint_file_delete_failed);
                } else {
                    showToastInUI(R.string.hint_file_delete_succeeded);
                }

                BaseChooseActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mContentAdapter.updateDataSet();
                    }
                });
            }
        }).start();
    }


    /**
     * 在UI线程显示Toast
     *
     * @param stringId string resource id
     */
    protected void showToastInUI(final int stringId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(BaseChooseActivity.this, stringId, Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * 点击发送按钮
     *
     * @param action 发送给谁
     */
    private void prepareSending(final String action) {
        mFloatingActionMenu.toggle(false);

        if (NetworkUtil.isVpnEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.dialog_vpn_title))
                    .setMessage(getString(R.string.dialog_vpn_message))
                    .setPositiveButton(getString(R.string.dialog_vpn_button_close), null)
                    .setNegativeButton(getString(R.string.dialog_vpn_button_ignore),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    sendFile(action);
                                }
                            })
                    .show();
        } else {
            sendFile(action);
        }
    }


    /**
     * 发送文件
     *
     * @param action 发送给谁
     */
    private void sendFile(String action) {
        Set<String> files = mContentAdapter.getChosenFiles();
        PreferenceUtil.storeFilesToSend(this, files);

        switch (action) {
            case ACTION_SEND_ANDROID:
                SendingActivity.startConnection(this);
                break;
            case ACTION_SEND_COMPUTER:
                ConnectingActivity.connectForSending(this);
                break;
            default:
                com.bbbbiu.biu.gui.transfer.apple.ConnectingActivity.connectForSending(this);
                break;
        }

        Log.i(TAG, String.format("Sending files to %s. File Amount: %d", action, files.size()));
    }
}

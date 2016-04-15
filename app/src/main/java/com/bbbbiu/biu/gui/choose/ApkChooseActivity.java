package com.bbbbiu.biu.gui.choose;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.widget.Toast;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.gui.adapters.choose.ApkContentAdapter;
import com.bbbbiu.biu.gui.adapters.choose.PanelBaseAdapter;
import com.bbbbiu.biu.gui.adapters.choose.ContentBaseAdapter;
import com.bbbbiu.biu.util.StorageUtil;

import java.io.File;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * 传应用。实现卸载已装应用与删除未装独立安装包的功能
 * <p/>
 * 点击卸载按钮时，将已经选择的所有文件的路径分别加入已安装和未安装的队列中
 * 然后先从未安装的队列依次取出文件进行删除，再从已安装的队列中取。
 * 每次删除成功之后要更新界面，及从Adapter中取出删除对应的APK，刷新View
 */
public class ApkChooseActivity extends ChooseBaseActivity {
    private static String TAG = ApkChooseActivity.class.getSimpleName();

    private static final int REQUEST_CODE_UNINSTALL = 1;

    private static final int MSG_PERFORM_DELETE = 2;

    private ApkContentAdapter mApkAdapter;

    private Queue<String> mApkToUninstallQueue = new PriorityQueue<>();
    private Queue<String> mApkToDeleteQueue = new PriorityQueue<>();
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setTitle(getNormalTitle());
        mApkAdapter = (ApkContentAdapter) mContentAdapter;

        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                // 队列不为空则卸卸卸
                // 卸完之后还会再发一次消息
                if (msg.what == MSG_PERFORM_DELETE) {
                    if (mApkToDeleteQueue.size() > 0) {
                        deleteApk(mApkToDeleteQueue.peek());
                    } else if (mApkToUninstallQueue.size() > 0) {
                        uninstallApk(mApkToUninstallQueue.peek());
                    }
                }
                return false;
            }
        });
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                for (String path : mContentAdapter.getChosenFiles()) {
                    if (StorageUtil.isAppInstalled(this, path)) {
                        mApkToUninstallQueue.add(path);
                    } else {
                        mApkToDeleteQueue.add(path);
                    }
                }
                mHandler.sendEmptyMessage(MSG_PERFORM_DELETE);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 删除APK安装包
     *
     * @param path 路径
     */
    private void deleteApk(final String path) {
        Dialog dialog = new AlertDialog.Builder(this)
                .setTitle(StorageUtil.getApkName(this, path))
                .setIcon(StorageUtil.getApkIcon(this, path))
                .setMessage(getString(R.string.apk_confirm_delete))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        File file = new File(path);

                        if (!file.delete()) {
                            Toast.makeText(ApkChooseActivity.this,
                                    getString(R.string.apk_delete_failed), Toast.LENGTH_LONG)
                                    .show();
                        } else {
                            Toast.makeText(ApkChooseActivity.this,
                                    getString(R.string.apk_delete_succeeded),
                                    Toast.LENGTH_SHORT).show();

                            onFinishDelete(path);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(ApkChooseActivity.this, R.string.apk_delete_dismissed,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .create();

        dialog.show();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mApkToDeleteQueue.remove();
                mHandler.sendEmptyMessage(MSG_PERFORM_DELETE);
            }
        });

    }


    /**
     * 卸载应用
     *
     * @param path 路径
     */
    private void uninstallApk(String path) {
        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
        intent.setData(Uri.parse("package:" + StorageUtil.getApkPackageName(this, path)));
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        startActivityForResult(intent, REQUEST_CODE_UNINSTALL);
    }


    /**
     * 删除或卸载之后更新页面
     *
     * @param path 路径
     */
    private void onFinishDelete(String path) {
        mApkAdapter.deleteApk(path);
        refreshTitle();
    }

    /**
     * 接收卸载应用的result
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_UNINSTALL) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(ApkChooseActivity.this, R.string.apk_uninstall_succeeded,
                        Toast.LENGTH_SHORT).show();

                onFinishDelete(mApkToUninstallQueue.remove());
                mHandler.sendEmptyMessage(MSG_PERFORM_DELETE);

            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(ApkChooseActivity.this, R.string.apk_uninstall_dismissed,
                        Toast.LENGTH_SHORT).show();

                mApkToUninstallQueue.remove();
                mHandler.sendEmptyMessage(MSG_PERFORM_DELETE);
            } else {
                Toast.makeText(ApkChooseActivity.this, R.string.apk_uninstall_failed,
                        Toast.LENGTH_SHORT).show();
                mApkToUninstallQueue.remove();
                mHandler.sendEmptyMessage(MSG_PERFORM_DELETE);
            }

        }
    }


    @Override
    protected int getNormalMenuId() {
        return R.menu.apk_choose;
    }

    @Override
    protected int getChosenMenuId() {
        return R.menu.apk_chosen;
    }

    @Override
    protected String getNormalTitle() {
        return getString(R.string.title_activity_choose_apk);
    }

    @Override
    protected RecyclerView.ItemDecoration onCreateContentItemDecoration() {
        return null;
    }

    @Override
    protected RecyclerView.LayoutManager onCreateContentLayoutManager() {
        final GridContentLayoutManager manager = new GridContentLayoutManager(this, 4);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return mApkAdapter.getItemViewType(position) == ContentBaseAdapter.VIEW_TYPE_HEADER
                        ? manager.getSpanCount() : 1;
            }
        });
        return manager;
    }

    @Override
    protected ContentBaseAdapter onCreateContentAdapter() {
        return new ApkContentAdapter(this);
    }

    @Override
    protected void onPanelRecyclerViewUpdate(File file) {
    }

    @Override
    protected PanelBaseAdapter onCreatePanelAdapter() {
        return null;
    }

}

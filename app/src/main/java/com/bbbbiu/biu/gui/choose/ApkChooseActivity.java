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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.widget.Toast;

import com.bbbbiu.biu.R;
import com.bbbbiu.biu.db.search.ApkItem;
import com.bbbbiu.biu.gui.adapter.choose.content.ApkContentAdapter;
import com.bbbbiu.biu.gui.adapter.choose.content.BaseContentAdapter;
import com.bbbbiu.biu.gui.adapter.choose.content.CommonContentAdapter;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 选择应用。实现卸载已装应用与删除未装独立安装包的功能
 * <p/>
 * 点击卸载按钮时，将已经选择的所有文件的路径分别加入已安装和未安装的队列中
 * 然后先从未安装的队列依次取出文件进行删除，再从已安装的队列中取。
 * 每次删除成功之后要更新界面，及从Adapter中取出删除对应的APK，刷新View
 */
public class ApkChooseActivity extends BaseChooseActivity {
    private static String TAG = ApkChooseActivity.class.getSimpleName();

    private static final int REQUEST_CODE_UNINSTALL = 1;

    private static final int MSG_PERFORM_DELETE = 2;

    private ApkContentAdapter mApkAdapter;

    private Queue<ApkItem> mApkToUninstallQueue = new LinkedList<>();
    private Queue<ApkItem> mApkToDeleteQueue = new LinkedList<>();
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

    /********************************************************************************************/

    @Override
    protected int getNormalMenuId() {
        return R.menu.common_normal;
    }

    @Override
    protected int getChosenMenuId() {
        return R.menu.common_chosen;
    }

    @Override
    protected String getNormalTitle() {
        return getString(R.string.title_activity_choose_apk);
    }


    @Override
    protected BaseContentAdapter onCreateContentAdapter() {
        return new ApkContentAdapter(this);
    }


    @Override
    protected LinearLayoutManager onCreateContentLayoutManager() {
        final GridLayoutManager manager = new GridLayoutManager(this, 4);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return mApkAdapter.getItemViewType(position) == CommonContentAdapter.VIEW_TYPE_HEADER
                        ? manager.getSpanCount() : 1;
            }
        });
        return manager;
    }

    @Override
    protected RecyclerView.ItemDecoration onCreateContentItemDecoration() {
        return null;
    }


    /********************************************************************************************/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                for (ApkItem apkItem : mApkAdapter.getChosenApk()) {
                    if (apkItem.isInstalled(this)) {
                        mApkToUninstallQueue.add(apkItem);
                    } else {
                        mApkToDeleteQueue.add(apkItem);
                    }
                }
                mHandler.sendEmptyMessage(MSG_PERFORM_DELETE);
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * 接收卸载应用的result
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_UNINSTALL) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(ApkChooseActivity.this, R.string.hint_apk_delete_succeeded,
                        Toast.LENGTH_SHORT).show();

                onFinishDelete(mApkToUninstallQueue.remove());
                mHandler.sendEmptyMessage(MSG_PERFORM_DELETE);

            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(ApkChooseActivity.this, R.string.hint_apk_delete_dismissed,
                        Toast.LENGTH_SHORT).show();

                mApkToUninstallQueue.remove();
                mHandler.sendEmptyMessage(MSG_PERFORM_DELETE);
            } else {
                Toast.makeText(ApkChooseActivity.this, R.string.hint_apk_delete_failed,
                        Toast.LENGTH_SHORT).show();
                mApkToUninstallQueue.remove();
                mHandler.sendEmptyMessage(MSG_PERFORM_DELETE);
            }

        }
    }


    /**
     * 删除APK安装包
     *
     * @param apkItem 路径
     */
    private void deleteApk(final ApkItem apkItem) {
        Dialog dialog = new AlertDialog.Builder(this)
                .setTitle(apkItem.name)
                .setIcon(apkItem.getIcon(this))
                .setMessage(getString(R.string.hint_apk_delete_confirm))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        File file = apkItem.getFile();

                        if (!file.delete()) {
                            Toast.makeText(ApkChooseActivity.this,
                                    getString(R.string.hint_file_delete_failed), Toast.LENGTH_LONG)
                                    .show();
                        } else {
                            Toast.makeText(ApkChooseActivity.this,
                                    getString(R.string.hint_file_delete_succeeded),
                                    Toast.LENGTH_SHORT).show();

                            onFinishDelete(apkItem);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(ApkChooseActivity.this, R.string.hint_file_delete_dismissed,
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
     * @param apkItem ApkItem
     */
    private void uninstallApk(ApkItem apkItem) {
        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
        intent.setData(Uri.parse("package:" + apkItem.packageName));
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        startActivityForResult(intent, REQUEST_CODE_UNINSTALL);
    }


    /**
     * 删除或卸载之后更新页面
     *
     * @param apkItem ApkItem
     */
    private void onFinishDelete(ApkItem apkItem) {
        mApkAdapter.deleteApk(apkItem);
        refreshTitle();

        if (mApkAdapter.getChosenCount() == 0) {
            invalidateOptionsMenu();
        }
    }
}

package com.bbbbiu.biu.lib.servlet;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.bbbbiu.biu.gui.transfer.TransferBaseActivity;
import com.bbbbiu.biu.service.ProgressListenerImpl;
import com.yieldnull.httpd.HttpRequest;
import com.yieldnull.httpd.HttpResponse;
import com.yieldnull.httpd.HttpServlet;
import com.yieldnull.httpd.ProgressListener;

import java.io.File;

/**
 * 显示文件上传或下载进度
 * <p/>
 * 以LocalBroadcast形式将进度信息发送到界面。使用时，继承之，
 * 并重载{@link #doPost(HttpRequest)}方法。
 * <p/>
 * 一定要使用{@link #getProgressListener()}获取进度监听器，并使用之
 *
 * @see HttpRequest#parseMultipartBody(File, ProgressListener)
 * <p/>
 * Created by YieldNull at 5/13/16
 */
public class ProgressBaseServlet extends HttpServlet {
    protected Context context;

    private ProgressListener mProgressListener = new ProgressListener() {
        private int mCurrentProgress;

        @Override
        public void update(String fileUri, long pBytesRead, long pContentLength) {

            int progress = (int) (pBytesRead * 100.0 / pContentLength);

            // 更新进度(0-100)
            if (progress > mCurrentProgress) {
                mCurrentProgress = progress;

                sendProgressBroadcast(fileUri, progress);
            }
        }
    };

    /**
     * 获取ProgressListener，用来监听进度
     *
     * @return ProgressListener
     * @see ProgressListener
     */
    public ProgressListener getProgressListener() {
        return mProgressListener;
    }


    public ProgressBaseServlet(Context context) {
        this.context = context;
    }


    @Override
    public HttpResponse doGet(HttpRequest request) {
        return null;
    }


    @Override
    public HttpResponse doPost(HttpRequest request) {
        return null;
    }


    /**
     * 发送进度广播
     *
     * @param fileUri  文件URI，用于唯一标识文件
     * @param progress 进度，1-100
     */
    protected void sendProgressBroadcast(String fileUri, int progress) {
        Bundle bundle = new Bundle();
        bundle.putInt(ProgressListenerImpl.RESULT_EXTRA_PROGRESS, progress);
        bundle.putString(ProgressListenerImpl.RESULT_EXTRA_FILE_URI, fileUri);


        Intent intent = new Intent(TransferBaseActivity.ACTION_UPDATE_PROGRESS);
        intent.putExtra(TransferBaseActivity.EXTRA_RESULT_CODE, ProgressListenerImpl.RESULT_PROGRESS);
        intent.putExtra(TransferBaseActivity.EXTRA_RESULT_BUNDLE, bundle);

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }


    /**
     * 发送失败广播
     *
     * @param fileUri 文件URI，用于唯一标识文件
     */
    protected void sendFailureBroadcast(String fileUri) {
        Bundle bundle = new Bundle();
        bundle.putString(ProgressListenerImpl.RESULT_EXTRA_FILE_URI, fileUri);

        Intent intent = new Intent(TransferBaseActivity.ACTION_UPDATE_PROGRESS);
        intent.putExtra(TransferBaseActivity.EXTRA_RESULT_CODE, ProgressListenerImpl.RESULT_FAILED);
        intent.putExtra(TransferBaseActivity.EXTRA_RESULT_BUNDLE, bundle);

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }


    /**
     * 发送成功广播
     *
     * @param fileUri 文件URI，用于唯一标识文件
     */
    protected void sendSuccessBroadcast(String fileUri) {
        Bundle bundle = new Bundle();
        bundle.putString(ProgressListenerImpl.RESULT_EXTRA_FILE_URI, fileUri);

        Intent intent = new Intent(TransferBaseActivity.ACTION_UPDATE_PROGRESS);
        intent.putExtra(TransferBaseActivity.EXTRA_RESULT_CODE, ProgressListenerImpl.RESULT_SUCCEEDED);
        intent.putExtra(TransferBaseActivity.EXTRA_RESULT_BUNDLE, bundle);

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}

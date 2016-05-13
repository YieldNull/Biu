package com.bbbbiu.biu.lib.servlet;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.bbbbiu.biu.gui.transfer.TransferBaseActivity;
import com.bbbbiu.biu.lib.httpd.HttpRequest;
import com.bbbbiu.biu.lib.httpd.HttpResponse;
import com.bbbbiu.biu.lib.httpd.HttpServlet;
import com.bbbbiu.biu.lib.util.ProgressListener;

/**
 * Created by YieldNull at 5/13/16
 */
public class ProgressBaseServlet extends HttpServlet {
    private static final String TAG = ProgressBaseServlet.class.getSimpleName();

    private ProgressListener mProgressListener = new ProgressListener() {
        private int mCurrentProgress;

        @Override
        public void update(long pBytesRead, long pContentLength, int pItems) {

            int progress = (int) (pBytesRead * 100.0 / pContentLength);

            // 更新进度(0-100)
            if (progress > mCurrentProgress) {
                mCurrentProgress = progress;

                sendProgressBroadcast(progress);
            }
        }
    };

    public ProgressListener getProgressListener() {
        return mProgressListener;
    }

    public ProgressBaseServlet(Context context) {
        super(context);
    }

    @Override
    public HttpResponse doGet(HttpRequest request) {
        return null;
    }

    @Override
    public HttpResponse doPost(HttpRequest request) {
        return null;
    }

    protected void sendProgressBroadcast(int progress) {
        Bundle bundle = new Bundle();
        bundle.putInt(ProgressListener.RESULT_EXTRA_PROGRESS, progress);
        bundle.putString(ProgressListener.RESULT_EXTRA_FILE_URI, null);


        Intent intent = new Intent(TransferBaseActivity.ACTION_UPDATE_PROGRESS);
        intent.putExtra(TransferBaseActivity.EXTRA_RESULT_CODE, ProgressListener.RESULT_PROGRESS);
        intent.putExtra(TransferBaseActivity.EXTRA_RESULT_BUNDLE, bundle);

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    protected void sendFailureBroadcast() {
        Bundle bundle = new Bundle();
        bundle.putString(ProgressListener.RESULT_EXTRA_FILE_URI, null);

        Intent intent = new Intent(TransferBaseActivity.ACTION_UPDATE_PROGRESS);
        intent.putExtra(TransferBaseActivity.EXTRA_RESULT_CODE, ProgressListener.RESULT_FAILED);
        intent.putExtra(TransferBaseActivity.EXTRA_RESULT_BUNDLE, bundle);

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }


    protected void sendSuccessBroadcast() {
        Bundle bundle = new Bundle();
        bundle.putString(ProgressListener.RESULT_EXTRA_FILE_URI, null);

        Intent intent = new Intent(TransferBaseActivity.ACTION_UPDATE_PROGRESS);
        intent.putExtra(TransferBaseActivity.EXTRA_RESULT_CODE, ProgressListener.RESULT_SUCCEEDED);
        intent.putExtra(TransferBaseActivity.EXTRA_RESULT_BUNDLE, bundle);

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}

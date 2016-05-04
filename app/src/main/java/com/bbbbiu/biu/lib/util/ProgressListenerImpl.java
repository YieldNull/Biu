package com.bbbbiu.biu.lib.util;

import android.os.Bundle;
import android.os.ResultReceiver;

import com.bbbbiu.biu.lib.httpd.util.ProgressListener;

/**
 * 将工作进度通过resultReceiver传到相应的Activity 从而更新界面
 * <p/>
 * 工作指的是 上传、下载、接收、发送等所有文件传递操作
 * <p/>
 * Created by YieldNull at 4/23/16
 */
public class ProgressListenerImpl implements ProgressListener {

    /**
     * Intent extra. 文件uri
     */
    public static final String RESULT_EXTRA_FILE_URI = "com.bbbbiu.biu.lib.util.ProgressListenerImpl.result.FILE_URI";

    /**
     * Intent extra. {@link #RESULT_EXTRA_FILE_URI} 对应文件的工作进度，0-100
     */
    public static final String RESULT_EXTRA_PROGRESS = "com.bbbbiu.biu.lib.util.ProgressListenerImpl.result.PROGRESS";


    /**
     * ResultCode. 工作失败
     */
    public static final int RESULT_FAILED = 1;

    /**
     * ResultCode. 工作成功
     */
    public static final int RESULT_SUCCEEDED = 2;

    /**
     * ResultCode. 工作进度
     */
    public static final int RESULT_PROGRESS = 3;


    /**
     * 纪录当前文件
     */
    private String mFileUri;

    /**
     * 纪录当前工作进度
     */
    private int mCurrentProgress;

    /**
     * 工作结果的ResultReceiver
     */
    private ResultReceiver mResultReceiver;


    public ProgressListenerImpl(String fileUri, ResultReceiver resultReceiver) {
        mResultReceiver = resultReceiver;
        mFileUri = fileUri;
    }


    @Override
    public void update(long pBytesRead, long pContentLength, int pItems) {
        int progress = (int) (pBytesRead * 100.0 / pContentLength);

        // 更新进度(0-100)
        if (progress > mCurrentProgress) {
            mCurrentProgress = progress;

            Bundle bundle = new Bundle();
            bundle.putInt(RESULT_EXTRA_PROGRESS, progress);
            bundle.putString(RESULT_EXTRA_FILE_URI, mFileUri);

            mResultReceiver.send(RESULT_PROGRESS, bundle);
        }
    }
}

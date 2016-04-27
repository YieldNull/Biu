package com.bbbbiu.biu.lib.util;

import android.os.Bundle;
import android.os.ResultReceiver;

/**
 * Created by YieldNull at 4/23/16
 */
public class ProgressListenerImpl implements ProgressListener {
    public static final String RESULT_EXTRA_PROGRESS = "com.bbbbiu.biu.lib.util.ProgressListenerImpl.result.PROGRESS";
    public static final String RESULT_EXTRA_FILE_URI = "com.bbbbiu.biu.lib.util.ProgressListenerImpl.result.FILE_URI";

    public static final int RESULT_FAILED = 1;
    public static final int RESULT_SUCCEEDED = 2;
    public static final int RESULT_PROGRESS = 3;

    private String mFilePath;
    private int mCurrentProgress;
    private ResultReceiver mResultReceiver;

    public ProgressListenerImpl(String fileUri, ResultReceiver resultReceiver) {
        mResultReceiver = resultReceiver;
        mFilePath = fileUri;
    }


    @Override
    public void update(long pBytesRead, long pContentLength, int pItems) {
        int progress = (int) (pBytesRead * 100.0 / pContentLength);

        if (progress > mCurrentProgress) {
            mCurrentProgress = progress;

            Bundle bundle = new Bundle();
            bundle.putInt(RESULT_EXTRA_PROGRESS, progress);
            bundle.putString(RESULT_EXTRA_FILE_URI, mFilePath);

            mResultReceiver.send(RESULT_PROGRESS, bundle);
        }
    }
}

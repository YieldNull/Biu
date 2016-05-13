package com.bbbbiu.biu.lib.util;

public interface ProgressListener {

    /**
     * Intent extra. 文件uri
     */
    String RESULT_EXTRA_FILE_URI = "com.bbbbiu.biu.lib.utilProgressListener.result.FILE_URI";

    /**
     * Intent extra. {@link #RESULT_EXTRA_FILE_URI} 对应文件的工作进度，0-100
     */
    String RESULT_EXTRA_PROGRESS = "com.bbbbiu.biu.lib.utilProgressListener.result.PROGRESS";

    /**
     * ResultCode. 工作失败
     */
    int RESULT_FAILED = 1;
    /**
     * ResultCode. 工作成功
     */
    int RESULT_SUCCEEDED = 2;
    /**
     * ResultCode. 工作进度
     */
    int RESULT_PROGRESS = 3;

    /**
     * Updates the listeners status information.
     *
     * @param pBytesRead     The total number of bytes, which have been read
     *                       so far.
     * @param pContentLength The total number of bytes, which are being
     *                       read. May be -1, if this number is unknown.
     * @param pItems         The number of the field, which is currently being
     *                       read. (0 = no item so far, 1 = first item is being read, ...)
     */
    void update(long pBytesRead, long pContentLength, int pItems);

}

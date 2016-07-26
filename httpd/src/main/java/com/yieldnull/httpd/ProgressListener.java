package com.yieldnull.httpd;

public interface ProgressListener {

    /**
     * Updates the listeners status information.
     *
     * @param fileUri        The file uri
     * @param pBytesRead     The total number of bytes, which have been read
     *                       so far.
     * @param pContentLength The total number of bytes, which are being
     *                       read. May be -1, if this number is unknown.
     */
    void update(String fileUri, long pBytesRead, long pContentLength);

}

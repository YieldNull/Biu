package com.yieldnull.httpd;

public interface ProgressListener {

    /**
     * Updates the listeners status information.
     *
     * @param pBytesRead     The total number of bytes, which have been read
     *                       so far.
     * @param pContentLength The total number of bytes, which are being
     *                       read. May be -1, if this number is unknown.
     */
    void update(long pBytesRead, long pContentLength);

}

package com.yieldnull.httpd;

public interface ProgressListener {

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

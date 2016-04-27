package com.bbbbiu.biu.lib.util;

/**
 * The {@link ProgressListener} may be used to display a progress bar
 * or do stuff like that.
 *
 * @version $Id: ProgressListener.java 1454691 2013-03-09 12:15:54Z simonetripodi $
 */
public interface ProgressListener {

    /**
     * Updates the listeners status information.
     *
     * @param pBytesRead The total number of bytes, which have been read
     *   so far.
     * @param pContentLength The total number of bytes, which are being
     *   read. May be -1, if this number is unknown.
     * @param pItems The number of the field, which is currently being
     *   read. (0 = no item so far, 1 = first item is being read, ...)
     */
    void update(long pBytesRead, long pContentLength, int pItems);

}

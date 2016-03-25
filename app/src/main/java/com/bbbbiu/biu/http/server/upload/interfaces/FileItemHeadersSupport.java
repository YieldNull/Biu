package com.bbbbiu.biu.http.server.upload.interfaces;

import com.bbbbiu.biu.http.server.upload.FileItem;
import com.bbbbiu.biu.http.util.FileItemHeaders;

/**
 * Interface that will indicate that {@link FileItem} or {@link FileItemStream}
 * implementations will accept the headers read for the item.
 *
 * @since 1.2.1
 *
 * @see FileItem
 * @see FileItemStream
 *
 * @version $Id$
 */
public interface FileItemHeadersSupport {

    /**
     * Returns the collection of headers defined locally within this item.
     *
     * @return the {@link FileItemHeaders} present for this item.
     */
    FileItemHeaders getHeaders();

    /**
     * Sets the headers read from within an item.  Implementations of
     * {@link FileItem} or {@link FileItemStream} should implement this
     * interface to be able to get the raw headers found within the item
     * header block.
     *
     * @param headers the instance that holds onto the headers
     *         for this instance.
     */
    void setHeaders(FileItemHeaders headers);

}

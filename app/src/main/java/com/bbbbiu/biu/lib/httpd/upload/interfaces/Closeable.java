package com.bbbbiu.biu.lib.httpd.upload.interfaces;

import java.io.IOException;

/**
 * Interface of an object, which may be closed.
 *
 * @version $Id: Closeable.java 1454691 2013-03-09 12:15:54Z simonetripodi $
 */
public interface Closeable {

    /**
     * Closes the object.
     *
     * @throws IOException An I/O error occurred.
     */
    void close() throws IOException;

    /**
     * Returns, whether the object is already closed.
     *
     * @return True, if the object is closed, otherwise false.
     * @throws IOException An I/O error occurred.
     */
    boolean isClosed() throws IOException;

}

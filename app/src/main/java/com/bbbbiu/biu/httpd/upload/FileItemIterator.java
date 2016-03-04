package com.bbbbiu.biu.httpd.upload;


import java.io.IOException;

/**
 * An iterator, as returned by
 * @version $Id: FileItemIterator.java 1454691 2013-03-09 12:15:54Z simonetripodi $
 */
public interface FileItemIterator {

    /**
     * Returns, whether another instance of {@link FileItemStream}
     * is available.
     *
     * @throws FileUpload.FileUploadException Parsing or processing the
     *   file item failed.
     * @throws IOException Reading the file item failed.
     * @return True, if one or more additional file items
     *   are available, otherwise false.
     */
    boolean hasNext() throws FileUpload.FileUploadException, IOException;

    /**
     * Returns the next available {@link FileItemStream}.
     *
     * @throws java.util.NoSuchElementException No more items are available. Use
     * {@link #hasNext()} to prevent this exception.
     * @throws FileUpload.FileUploadException Parsing or processing the
     *   file item failed.
     * @throws IOException Reading the file item failed.
     * @return FileItemStream instance, which provides
     *   access to the next file item.
     */
    FileItemStream next() throws FileUpload.FileUploadException, IOException;

}

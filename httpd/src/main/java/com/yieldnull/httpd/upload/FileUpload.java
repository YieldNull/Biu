package com.yieldnull.httpd.upload;

import com.yieldnull.httpd.HttpRequest;
import com.yieldnull.httpd.ProgressListener;
import com.yieldnull.httpd.util.FileItemHeaders;
import com.yieldnull.httpd.util.Streams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileUpload {

    public static final String CONTENT_TYPE = "Content-type";
    public static final String CONTENT_DISPOSITION = "Content-disposition";
    public static final String CONTENT_LENGTH = "Content-length";
    public static final String FORM_DATA = "form-data";
    public static final String ATTACHMENT = "attachment";
    public static final String MULTIPART = "multipart/";
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    public static final String MULTIPART_MIXED = "multipart/mixed";

    private ProgressListener listener;
    private FileItemFactory fileItemFactory;


    public FileItemFactory getFileItemFactory() {
        return fileItemFactory;
    }


    public ProgressListener getProgressListener() {
        return listener;
    }


    public void setProgressListener(ProgressListener pListener) {
        listener = pListener;
    }


    public FileUpload(FileItemFactory fileItemFactory) {
        this.fileItemFactory = fileItemFactory;
    }


    /**
     * 解析Multipart Stream
     * <p/>
     * 详见<a href="http://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a>
     *
     * @param request HttpRequest
     * @return 解析所得
     * @throws IOException
     */
    public List<FileItem> parseRequest(HttpRequest request)
            throws IOException {

        List<FileItem> items = new ArrayList<>();
        boolean successful = false;

        try {
            FileItemIterator iterator = getItemIterator(request);
            FileItemFactory factory = getFileItemFactory();

            while (iterator.hasNext()) {
                final FileItemStream item = iterator.next();

                // Don't use getName() here to prevent an InvalidFileNameException.
                final String fileName = item.getName();
                FileItem fileItem = factory.createItem(item.getFieldName(), item.getContentType(),
                        item.isFormField(), fileName);
                items.add(fileItem);


                Streams.copy(item.openStream(), fileItem.getOutputStream(), true, null);

                final FileItemHeaders fih = item.getHeaders();
                fileItem.setHeaders(fih);
            }
            successful = true;
            return items;

        } finally {
            if (!successful) {
                for (FileItem fileItem : items) {
                    try {
                        fileItem.delete();
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
    }

    /**
     * Processes an <a href="http://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a>
     * compliant <code>multipart/form-data</code> stream.
     *
     * @param request The context for the request to be parsed.
     * @return An iterator to instances of <code>FileItemStream</code>
     * parsed from the request, in the order that they were
     * transmitted.
     * @throws IOException         An I/O error occurred. This may be a network
     *                             error while communicating with the client or a problem while
     *                             storing the uploaded content.
     */
    public FileItemIterator getItemIterator(HttpRequest request)
            throws IOException {
        return new FileItemIterator(request, listener);
    }
}

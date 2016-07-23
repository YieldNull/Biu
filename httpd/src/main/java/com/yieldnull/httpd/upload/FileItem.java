package com.yieldnull.httpd.upload;


import com.yieldnull.httpd.util.FileItemHeaders;
import com.yieldnull.httpd.util.Streams;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;


public class FileItem {

    /**
     * The name of the form field as provided by the browser.
     */
    private String fieldName;

    /**
     * The content type passed by the browser, or <code>null</code> if
     * not defined.
     */
    private final String contentType;

    /**
     * Whether or not this item is a simple form field.
     */
    private boolean isFormField;

    /**
     * The original filename in the user's filesystem.
     */
    private final String fileName;

    /**
     * The size of the item, in bytes. This is used to cache the size when a
     * file item is moved from its original location.
     */
    private long size = -1;


    /**
     * The directory in which uploaded files will be stored, if stored on disk.
     */
    private final File repository;


    /**
     * The file items headers.
     */
    private FileItemHeaders headers;

    // ----------------------------------------------------------- Constructors

    /**
     * Constructs a new <code>FileItem</code> instance.
     *
     * @param fieldName   The name of the form field.
     * @param contentType The content type passed by the browser or
     *                    <code>null</code> if not specified.
     * @param isFormField Whether or not this item is a plain form field, as
     *                    opposed to a file upload.
     * @param fileName    The original filename in the user's filesystem, or
     *                    <code>null</code> if not specified.
     * @param repository  The data repository, which is the directory in
     *                    which files will be created, should the item size
     *                    exceed the threshold.
     */
    public FileItem(String fieldName, String contentType, boolean isFormField,
                    String fileName,
                    File repository) {
        this.fieldName = fieldName;
        this.contentType = contentType;
        this.isFormField = isFormField;
        this.fileName = fileName;
        this.repository = repository;
    }


    public String getName() {
        return Streams.checkFileName(fileName);
    }

    public long getSize() {
        return getFile().length();
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void delete() {
        getFile().delete();
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public boolean isFormField() {
        return isFormField;
    }

    public void setFormField(boolean state) {
        isFormField = state;
    }

    public OutputStream getOutputStream() throws IOException {
        return Files.newOutputStream(getFile().toPath(), StandardOpenOption.READ);
    }


    protected File getFile() {
        return new File(repository, fileName);
    }

    /**
     * Returns the file item headers.
     *
     * @return The file items headers.
     */
    public FileItemHeaders getHeaders() {
        return headers;
    }

    /**
     * Sets the file item headers.
     *
     * @param pHeaders The file items headers.
     */
    public void setHeaders(FileItemHeaders pHeaders) {
        headers = pHeaders;
    }

}

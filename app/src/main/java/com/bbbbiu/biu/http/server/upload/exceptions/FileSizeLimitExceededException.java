package com.bbbbiu.biu.http.server.upload.exceptions;


/**
 * Thrown to indicate that A files size exceeds the configured maximum.
 */
public class FileSizeLimitExceededException
        extends SizeException {

    /**
     * The exceptions UID, for serializing an instance.
     */
    private static final long serialVersionUID = 8150776562029630058L;

    /**
     * File name of the item, which caused the exception.
     */
    private String fileName;

    /**
     * Field name of the item, which caused the exception.
     */
    private String fieldName;

    /**
     * Constructs a <code>SizeExceededException</code> with
     * the specified detail message, and actual and permitted sizes.
     *
     * @param message   The detail message.
     * @param actual    The actual request size.
     * @param permitted The maximum permitted request size.
     */
    public FileSizeLimitExceededException(String message, long actual,
                                          long permitted) {
        super(message, actual, permitted);
    }

    /**
     * Returns the file name of the item, which caused the
     * exception.
     *
     * @return File name, if known, or null.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Sets the file name of the item, which caused the
     * exception.
     *
     * @param pFileName the file name of the item, which caused the exception.
     */
    public void setFileName(String pFileName) {
        fileName = pFileName;
    }

    /**
     * Returns the field name of the item, which caused the
     * exception.
     *
     * @return Field name, if known, or null.
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Sets the field name of the item, which caused the
     * exception.
     *
     * @param pFieldName the field name of the item,
     *                   which caused the exception.
     */
    public void setFieldName(String pFieldName) {
        fieldName = pFieldName;
    }

}

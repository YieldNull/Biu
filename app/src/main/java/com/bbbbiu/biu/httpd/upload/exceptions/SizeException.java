package com.bbbbiu.biu.httpd.upload.exceptions;

/**
 * This exception is thrown, if a requests permitted size
 * is exceeded.
 */
public abstract class SizeException extends FileUploadException {

    /**
     * Serial version UID, being used, if serialized.
     */
    private static final long serialVersionUID = -8776225574705254126L;

    /**
     * The actual size of the request.
     */
    private final long actual;

    /**
     * The maximum permitted size of the request.
     */
    private final long permitted;

    /**
     * Creates a new instance.
     *
     * @param message   The detail message.
     * @param actual    The actual number of bytes in the request.
     * @param permitted The requests size limit, in bytes.
     */
    protected SizeException(String message, long actual, long permitted) {
        super(message);
        this.actual = actual;
        this.permitted = permitted;
    }

    /**
     * Retrieves the actual size of the request.
     *
     * @return The actual size of the request.
     * @since 1.3
     */
    public long getActualSize() {
        return actual;
    }

    /**
     * Retrieves the permitted size of the request.
     *
     * @return The permitted size of the request.
     * @since 1.3
     */
    public long getPermittedSize() {
        return permitted;
    }

}
package com.bbbbiu.biu.lib.httpd.upload.exceptions;

import java.io.IOException;

/**
 * Thrown to indicate an IOException.
 */
public class IOFileUploadException extends FileUploadException {

    /**
     * The exceptions UID, for serializing an instance.
     */
    private static final long serialVersionUID = 1749796615868477269L;

    /**
     * The exceptions cause; we overwrite the parent
     * classes field, which is available since Java
     * 1.4 only.
     */
    private final IOException cause;

    /**
     * Creates a new instance with the given cause.
     *
     * @param pMsg       The detail message.
     * @param pException The exceptions cause.
     */
    public IOFileUploadException(String pMsg, IOException pException) {
        super(pMsg);
        cause = pException;
    }

    /**
     * Returns the exceptions cause.
     *
     * @return The exceptions cause, if any, or null.
     */
    @Override
    public Throwable getCause() {
        return cause;
    }

}
package com.bbbbiu.biu.httpd.upload.exceptions;

import java.io.IOException;

/**
 * This exception is thrown for hiding an inner
 * {@link FileUploadException} in an {@link IOException}.
 */
public class FileUploadIOException extends IOException {

    /**
     * The exceptions UID, for serializing an instance.
     */
    private static final long serialVersionUID = -7047616958165584154L;

    /**
     * The exceptions cause; we overwrite the parent
     * classes field, which is available since Java
     * 1.4 only.
     */
    private final FileUploadException cause;

    /**
     * Creates a <code>FileUploadIOException</code> with the
     * given cause.
     *
     * @param pCause The exceptions cause, if any, or null.
     */
    public FileUploadIOException(FileUploadException pCause) {
        // We're not doing super(pCause) cause of 1.3 compatibility.
        cause = pCause;
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

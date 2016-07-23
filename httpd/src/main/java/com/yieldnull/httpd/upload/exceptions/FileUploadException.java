package com.yieldnull.httpd.upload.exceptions;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Exception for errors encountered while processing the request.
 *
 * @version $Id: FileUploadException.java 1454690 2013-03-09 12:08:48Z simonetripodi $
 */
public class FileUploadException extends IOException {

    /**
     * Serial version UID, being used, if the exception
     * is serialized.
     */
    private static final long serialVersionUID = 8881893724388807504L;

    /**
     * The exceptions cause. We overwrite the cause of
     * the super class, which isn't available in Java 1.3.
     */
    private final Throwable cause;

    /**
     * Constructs a new <code>FileUploadException</code> without message.
     */
    public FileUploadException() {
        this(null, null);
    }

    /**
     * Constructs a new <code>FileUploadException</code> with specified detail
     * message.
     *
     * @param msg the error message.
     */
    public FileUploadException(final String msg) {
        this(msg, null);
    }

    /**
     * Creates a new <code>FileUploadException</code> with the given
     * detail message and cause.
     *
     * @param msg   The exceptions detail message.
     * @param cause The exceptions cause.
     */
    public FileUploadException(String msg, Throwable cause) {
        super(msg);
        this.cause = cause;
    }

    /**
     * Prints this throwable and its backtrace to the specified print stream.
     *
     * @param stream <code>PrintStream</code> to use for output
     */
    @Override
    public void printStackTrace(PrintStream stream) {
        super.printStackTrace(stream);
        if (cause != null) {
            stream.println("Caused by:");
            cause.printStackTrace(stream);
        }
    }

    /**
     * Prints this throwable and its backtrace to the specified
     * print writer.
     *
     * @param writer <code>PrintWriter</code> to use for output
     */
    @Override
    public void printStackTrace(PrintWriter writer) {
        super.printStackTrace(writer);
        if (cause != null) {
            writer.println("Caused by:");
            cause.printStackTrace(writer);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Throwable getCause() {
        return cause;
    }
}
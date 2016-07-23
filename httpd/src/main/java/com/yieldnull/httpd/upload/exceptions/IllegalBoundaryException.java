package com.yieldnull.httpd.upload.exceptions;

import java.io.IOException;

/**
 * Thrown upon attempt of setting an invalid boundary token.
 */
public class IllegalBoundaryException extends IOException {

    /**
     * The UID to use when serializing this instance.
     */
    private static final long serialVersionUID = -161533165102632918L;

    /**
     * Constructs an <code>IllegalBoundaryException</code> with no
     * detail message.
     */
    public IllegalBoundaryException() {
        super();
    }

    /**
     * Constructs an <code>IllegalBoundaryException</code> with
     * the specified detail message.
     *
     * @param message The detail message.
     */
    public IllegalBoundaryException(String message) {
        super(message);
    }

}

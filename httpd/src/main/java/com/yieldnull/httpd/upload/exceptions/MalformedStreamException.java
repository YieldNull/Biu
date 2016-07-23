package com.yieldnull.httpd.upload.exceptions;

import java.io.IOException;

/**
 * Thrown to indicate that the input stream fails to follow the
 * required syntax.
 */
public class MalformedStreamException extends IOException {

    /**
     * The UID to use when serializing this instance.
     */
    private static final long serialVersionUID = 6466926458059796677L;

    /**
     * Constructs a <code>MalformedStreamException</code> with no
     * detail message.
     */
    public MalformedStreamException() {
        super();
    }

    /**
     * Constructs an <code>MalformedStreamException</code> with
     * the specified detail message.
     *
     * @param message The detail message.
     */
    public MalformedStreamException(String message) {
        super(message);
    }

}

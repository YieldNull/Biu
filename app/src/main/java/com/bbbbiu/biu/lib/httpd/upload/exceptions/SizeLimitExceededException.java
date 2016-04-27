package com.bbbbiu.biu.lib.httpd.upload.exceptions;

public class SizeLimitExceededException
        extends SizeException {

    /**
     * The exceptions UID, for serializing an instance.
     */
    private static final long serialVersionUID = -2474893167098052828L;


    /**
     * Constructs a <code>SizeExceededException</code> with
     * the specified detail message, and actual and permitted sizes.
     *
     * @param message   The detail message.
     * @param actual    The actual request size.
     * @param permitted The maximum permitted request size.
     */
    public SizeLimitExceededException(String message, long actual,
                                      long permitted) {
        super(message, actual, permitted);
    }

}

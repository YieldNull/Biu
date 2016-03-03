package com.bbbbiu.biu.server.upload;

/**
 * This exception is thrown in case of an invalid file name.
 * A file name is invalid, if it contains a NUL character.
 * Attackers might use this to circumvent security checks:
 * For example, a malicious user might upload a file with the name
 * "foo.exe\0.png". This file name might pass security checks (i.e.
 * checks for the extension ".png"), while, depending on the underlying
 * C library, it might create a file named "foo.exe", as the NUL
 * character is the string terminator in C.
 *
 * @version $Id: InvalidFileNameException.java 1454691 2013-03-09 12:15:54Z simonetripodi $
 */
public class InvalidFileNameException extends RuntimeException {

    /**
     * Serial version UID, being used, if the exception
     * is serialized.
     */
    private static final long serialVersionUID = 7922042602454350470L;

    /**
     * The file name causing the exception.
     */
    private final String name;

    /**
     * Creates a new instance.
     *
     * @param pName The file name causing the exception.
     * @param pMessage A human readable error message.
     */
    public InvalidFileNameException(String pName, String pMessage) {
        super(pMessage);
        name = pName;
    }

    /**
     * Returns the invalid file name.
     *
     * @return the invalid file name.
     */
    public String getName() {
        return name;
    }

}

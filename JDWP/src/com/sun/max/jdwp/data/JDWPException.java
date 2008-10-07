/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
/*VCSID=67957e95-ee28-423f-aa9e-75a0cf9d1e87*/
package com.sun.max.jdwp.data;

import com.sun.max.jdwp.constants.Error;

/**
 * Class representing an exception during JDWP processing. There is a string message as well as a JDWP error code associated with this exception.
 * For possible values of the error code see {@link com.sun.max.jdwp.constants.Error}.
 *
 * @author Thomas Wuerthinger
 */
public class JDWPException extends Exception {

    private final short _errorCode;
    private final String _message;

    public JDWPException() {
        this((short) Error.INTERNAL);
    }

    public JDWPException(short errorCode) {
        this(errorCode, "");
    }

    public JDWPException(String message) {
        this((short) Error.INTERNAL, message);
    }

    public JDWPException(short errorCode, String message) {
        _errorCode = errorCode;
        _message = message;
    }

    /**
     * The JDWP error code that identifies the exception type.
     * @return the JDWP error code
     */
    public int errorCode() {
        return _errorCode;
    }

    /**
     * String message that describes the exception.
     * @return the exception description
     */
    public String message() {
        return _message;
    }

    @Override
    public String toString() {
        return "JDWPException(" + _errorCode + ", " + _message + ")";
    }
}

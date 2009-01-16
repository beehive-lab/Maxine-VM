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
package com.sun.max.util;

import java.io.*;

import com.sun.max.program.*;

/**
 * Utilities methods for exceptions.
 *
 * @author Doug Simon
 */
public final class Exceptions {

    private Exceptions() {
    }

    /**
     * Tests a given exception to see if it is an instance of a given exception type, casting and throwing it if true.
     */
    public static <Exception_Type extends Exception> void throwIfInstanceOf(Class<Exception_Type> exceptionType,  Exception exception) throws Exception_Type {
        if (exceptionType.isInstance(exception)) {
            throw exceptionType.cast(exception);
        }
    }

    /**
     * Tests a given exception to see if it is an instance of a given exception type, casting and throwing it if so.
     * Otherwise if the exception is an unchecked exception (i.e. an instance of a {@link RuntimeException} or
     * {@link Error}) then it is cast to the appropriate unchecked exception type and thrown. Otherwise, it is wrapped
     * in a {@link ProgramError} and thrown.
     *
     * This method declares a return type simply so that a call to this method can be the expression to a throw
     * instruction.
     */
    public static <Exception_Type extends Exception> Exception_Type cast(Class<Exception_Type> exceptionType,  Throwable exception) throws Exception_Type {
        if (exceptionType.isInstance(exception)) {
            throw exceptionType.cast(exception);
        }
        if (exception instanceof Error) {
            throw (Error) exception;
        }
        if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        }
        throw ProgramError.unexpected(exception);
    }

    /**
     * Tests a given exception to see if it is an instance of either two given exception types, casting and throwing it
     * if so. Otherwise if the exception is an unchecked exception (i.e. an instance of a {@link RuntimeException} or
     * {@link Error}) then it is cast to the appropriate unchecked exception type and thrown. Otherwise, it is wrapped
     * in a {@link ProgramError} and thrown.
     *
     * This method declares a return type simply so that a call to this method can be the expression to a throw
     * instruction.
     */
    public static <Exception_Type1 extends Exception, Exception_Type2 extends Exception>
    Exception_Type1 cast(Class<Exception_Type1> exceptionType1, Class<Exception_Type2> exceptionType2,  Throwable exception) throws Exception_Type1, Exception_Type2 {
        if (exceptionType1.isInstance(exception)) {
            throw exceptionType1.cast(exception);
        }
        if (exceptionType2.isInstance(exception)) {
            throw exceptionType2.cast(exception);
        }
        if (exception instanceof Error) {
            throw (Error) exception;
        }
        if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        }
        throw ProgramError.unexpected(exception);
    }

    /**
     * Gets the stack trace for a given exception as a string.
     */
    public static String stackTraceAsString(Throwable throwable) {
        final StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.getBuffer().toString();
    }
}

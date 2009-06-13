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
package com.sun.max.program;

/**
 * @author Bernd Mathiske
 */
public final class ProgramError extends Error {


    /**
     *  Allows for the handle method to be messaged before throwing ProgramError.
     * @author Paul Caprioli
     */
    public static interface Handler {
        void handle(String message, Throwable throwable);
    }

    /**
     * Sets a Handler to be used before throwing ProgramError.
     * @param handler If non-null, its handle method is messaged before throwing ProgramError.
     */
    public static void setHandler(Handler handler) {
        _handler = handler;
    }

    private ProgramError(String message, Throwable cause) {
        super(message, cause);
    }

    public static void check(boolean condition) {
        if (!condition) {
            unexpected("Program Error");
        }
    }

    public static void check(boolean condition, String message) {
        if (!condition) {
            unexpected(message);
        }
    }

    public static ProgramError unexpected(String message, Throwable throwable) {
        if (_handler != null) {
            _handler.handle(message, throwable);
        }
        throw new ProgramError("Unexpected Program Error: " + message, throwable);
    }

    public static ProgramError unexpected(String message) {
        throw unexpected(message, null);
    }

    public static ProgramError unexpected(Throwable throwable) {
        throw unexpected("", throwable);
    }

    public static ProgramError unexpected() {
        throw unexpected("");
    }

    public static ProgramError unknownCase() {
        throw unexpected("unknown switch case");
    }

    public static ProgramError unknownCase(String caseValue) {
        throw unexpected("unknown switch case: " + caseValue);
    }

    // Checkstyle: stop
    private static Handler _handler = null;

}

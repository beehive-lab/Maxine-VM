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
 * A collection of static methods for reporting a warning when an unexpected, non-fatal condition is encountered.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class ProgramWarning {

    /**
     * Implemented by a client that can {@linkplain ProgramWarning#setHandler(Handler) register}
     * itself to handle program warnings instead of having them printed to {@link System#err}.
     */
    public static interface Handler {

        /**
         * Handles display a given warning message.
         *
         * @param message a warning message
         */
        void handle(String message);
    }

    /**
     * Registers a handler to which warnings are redirected. Any previously registered handler
     * is overwritten and discarded.
     *
     * @param h if non-null, this object's {@link Handler#handle(String)} method is messaged instead of
     *            printing the warning to {@link System#err} a ProgramError.
     */
    public static void setHandler(Handler h) {
        handler = h;
    }

    private static Handler handler;

    private ProgramWarning() {
    }

    /**
     * Prints a given warning message.
     *
     * @param warning the warning message to print
     */
    public static void message(String warning) {
        if (handler != null) {
            handler.handle(warning);
        } else {
            System.err.println("WARNING: " + warning);
        }
    }

    /**
     * Checks a given condition and if it's {@code false}, the appropriate warning message is printed.
     *
     * @param condition a condition to test
     * @param message the warning message to be printed if {@code condition == false}
     */
    public static void check(boolean condition, String warning) {
        if (!condition) {
            message(warning);
        }
    }
}

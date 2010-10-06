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
package com.sun.max.tele.util;

/**
 * Static methods for responding with unchecked exceptions
 * in situations that should not occur.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class TeleError extends Error {

    private TeleError(String message, Throwable cause) {
        super(message, cause);
    }

    private static TeleError error(String message, Throwable cause) {
        throw new TeleError((message == null) ? "Internal Error:" : "Internal Error: " + message, cause);
    }

    /**
     * Reports the occurrence of some error condition by throwing an exception.
     *
     * @param message a message describing the error condition. This value may be {@code null}.
     * @param throwable an exception given more detail on the cause of the error condition. This value may be {@code null}.
     * @throws TeleError unconditionally
     */
    public static TeleError unexpected(String message, Throwable throwable) {
        throw error(message, throwable);
    }

    /**
     * Reports the occurrence of some error condition by throwing an exception.
     * This method never returns normally.
     *
     * @param message a message describing the error condition. This value may be {@code null}.
     * @throws TeleError unconditionally
     */
    public static TeleError unexpected(String message) {
        throw error(message, null);
    }

    /**
     * Reports the occurrence of some error condition by throwing an exception.
     * This method never returns normally.
     *
     * @param throwable an exception given more detail on the cause of the error condition. This value may be {@code null}.
     * @throws TeleError unconditionally
     */
    public static TeleError unexpected(Throwable throwable) {
        throw error(null, throwable);
    }

    /**
     * Reports the occurrence of some error condition by throwing an exception.
     * This method never returns normally.
     *
     * @throws TeleError unconditionally
     */
    public static TeleError unexpected() {
        throw error(null, null);
    }

    /**
     * Checks a condition that should be {@code true}.
     *
     * @param condition a condition to test
     * @throws TeleError if condition is {@code false}
     */
    public static void check(boolean condition) {
        if (!condition) {
            error(null, null);
        }
    }

    /**
     * Checks a condition that should be {@code true}.
     *
     * @param condition a condition to test
     * @param message a message describing the error condition being tested
     * @throws TeleError if condition is {@code false}
     */
    public static void check(boolean condition, String message) {
        if (!condition) {
            error(message, null);
        }
    }

    /**
     * Checks a condition that should be {@code true}.
     *
     * @param condition a condition to test
     * @param message a message describing the error condition being tested
     * @param object an object whose string description is to be appended to the message
     * @throws TeleError  if condition is {@code false}
     */
    public static void check(boolean condition, String message, Object object) {
        if (!condition) {
            error(message + object.toString(), null);
        }
    }

    /**
     * Reports that a {@code switch} statement encountered a {@code case} value it was not expecting.
     * This method never returns normally.
     *
     * @throws TeleError unconditionally
     */
    public static TeleError unknownCase() {
        throw error("unknown switch case", null);
    }

    /**
     * Reports that a {@code switch} statement encountered a {@code case} value it was not expecting.
     * This method never returns normally.
     *
     * @param caseValue the unexpected {@code case} value as a string
     * @throws TeleError unconditionally
     */
    public static TeleError unknownCase(String caseValue) {
        throw error("unknown switch case \"" + caseValue + "\"", null);
    }

    /**
     * Reports that an unimplemented piece of functionality was encountered
     * This method never returns normally.
     *
     * @throws TeleError unconditionally
     */
    public static TeleError unimplemented() {
        throw error("unimplemented functionality", null);
    }

    /**
     * Reports that an unimplemented piece of functionality was encountered
     * This method never returns normally.
     *
     * @param message description of the unimplemented functionality
     * @throws TeleError unconditionally
     */
    public static TeleError unimplemented(String message) {
        throw error("unimplemented " + message, null);
    }

}

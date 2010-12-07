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
package com.sun.max.ins.util;

/**
 * Static methods for announcing non-fatal situations
 * that should not occur.
 *
 * @author Michael Van De Vanter
 */
public final class InspectorWarning {

    private InspectorWarning() {
    }

    private static void warn(String message, Throwable cause) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Warning: ");
        if (message != null) {
            sb.append(message + "  ");
        }
        if (cause != null) {
            sb.append(cause.getMessage());
        }
        System.err.println(sb.toString());
    }

    /**
     * Reports the occurrence of a non-fatal error condition.
     *
     * @param message a message describing the condition. This value may be {@code null}.
     * @param throwable an exception given more detail on the cause of the condition. This value may be {@code null}.
     */
    public static void message(String message, Throwable throwable) {
        warn(message, throwable);
    }

    /**
     * Reports the occurrence of a non-fatal error condition.
     *
     * @param message a message describing the condition. This value may be {@code null}.
     */
    public static void message(String message) {
        warn(message, null);
    }

    /**
     * Reports the occurrence of a non-fatal error condition.
     *
     * @param throwable an exception given more detail on the cause of the condition. This value may be {@code null}.
     * @throws InspectorWarning unconditionally
     */
    public static void message(Throwable throwable) {
        warn(null, throwable);
    }

    /**
     * Reports the occurrence of a non-fatal error condition.
     */
    public static void message() {
        warn(null, null);
    }

    /**
     * Reports the failure of a condition that should be {@code true}.
     *
     * @param condition a condition to test
     */
    public static void check(boolean condition) {
        if (!condition) {
            warn(null, null);
        }
    }

    /**
     * Reports the failure of a condition that should be {@code true}.
     *
     * @param condition a condition to test
     * @param message a message describing the condition being tested
     */
    public static void check(boolean condition, String message) {
        if (!condition) {
            warn(message, null);
        }
    }

    /**
     * Reports the failure of a condition that should be {@code true}.
     *
     * @param condition a condition to test
     * @param message a message describing the error condition being tested
     * @param object an object whose string description is to be appended to the message
     */
    public static void check(boolean condition, String message, Object object) {
        if (!condition) {
            warn(message + object.toString(), null);
        }
    }

    /**
     * Reports that a {@code switch} statement encountered a {@code case} value it was not expecting.
     */
    public static void unknownCase() {
        warn("unknown switch case", null);
    }

    /**
     * Reports that a {@code switch} statement encountered a {@code case} value it was not expecting.
     *
     * @param caseValue the unexpected {@code case} value as a string
     */
    public static void unknownCase(String caseValue) {
        warn("unknown switch case \"" + caseValue + "\"", null);
    }

    /**
     * Reports that an unimplemented piece of functionality was encountered.
     */
    public static void unimplemented() {
        warn("unimplemented functionality", null);
    }

    /**
     * Reports that an unimplemented piece of functionality was encountered.
     *
     * @param message description of the unimplemented functionality
     */
    public static void unimplemented(String message) {
        warn("unimplemented " + message, null);
    }

}

/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.tele.util;

/**
 * Static methods for announcing non-fatal situations
 * that should not occur.
 */
public final class TeleWarning {

    private TeleWarning() {
    }

    private static void warn(String message, Throwable cause) {
        final StringBuilder sb = new StringBuilder();
        sb.append("TeleWarning: ");
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
     * @throws TeleWarning unconditionally
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

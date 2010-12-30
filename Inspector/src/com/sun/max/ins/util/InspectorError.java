/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.util;

import com.sun.max.gui.*;
import com.sun.max.ins.*;

/**
 * Static methods for responding with unchecked exceptions
 * in situations that should not occur.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class InspectorError extends Error {
    private static final String ERROR_DISPLAY_PROPERTY = "maxine.ins.errordisplay";
    private static final String dialogDisplayProperty = System.getProperty(ERROR_DISPLAY_PROPERTY);
    private static final boolean dialogDisplay = dialogDisplayProperty == null || dialogDisplayProperty.equals("dialog");

    private InspectorError(String message, Throwable cause) {
        super(message, cause);
    }

    private static InspectorError error(String message, Throwable cause) {
        throw new InspectorError((message == null) ? "Internal Error:" : "Internal Error: " + message, cause);
    }

    public void display(Inspection inspection) {
        printStackTrace();
        if (dialogDisplay) {
            ThrowableDialog.showLater(this, inspection.gui().frame(), "Inspector Error");
        }
    }

    /**
     * Reports the occurrence of some error condition by throwing an exception.
     *
     * @param message a message describing the error condition. This value may be {@code null}.
     * @param throwable an exception given more detail on the cause of the error condition. This value may be {@code null}.
     * @throws InspectorError unconditionally
     */
    public static InspectorError unexpected(String message, Throwable throwable) {
        throw error(message, throwable);
    }

    /**
     * Reports the occurrence of some error condition by throwing an exception.
     * This method never returns normally.
     *
     * @param message a message describing the error condition. This value may be {@code null}.
     * @throws InspectorError unconditionally
     */
    public static InspectorError unexpected(String message) {
        throw error(message, null);
    }

    /**
     * Reports the occurrence of some error condition by throwing an exception.
     * This method never returns normally.
     *
     * @param throwable an exception given more detail on the cause of the error condition. This value may be {@code null}.
     * @throws InspectorError unconditionally
     */
    public static InspectorError unexpected(Throwable throwable) {
        throw error(null, throwable);
    }

    /**
     * Reports the occurrence of some error condition by throwing an exception.
     * This method never returns normally.
     *
     * @throws InspectorError unconditionally
     */
    public static InspectorError unexpected() {
        throw error(null, null);
    }

    /**
     * Checks a condition that should be {@code true}.
     *
     * @param condition a condition to test
     * @throws InspectorError if condition is {@code false}
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
     * @throws InspectorError if condition is {@code false}
     */
    public static void check(boolean condition, String message) {
        if (!condition) {
            throw error(message, null);
        }
    }

    /**
     * Reports that a {@code switch} statement encountered a {@code case} value it was not expecting.
     * This method never returns normally.
     *
     * @throws InspectorError unconditionally
     */
    public static InspectorError unknownCase() {
        throw error("unknown switch case", null);
    }

    /**
     * Reports that a {@code switch} statement encountered a {@code case} value it was not expecting.
     * This method never returns normally.
     *
     * @param caseValue the unexpected {@code case} value as a string
     * @throws InspectorError unconditionally
     */
    public static InspectorError unknownCase(String caseValue) {
        throw error("unknown switch case \"" + caseValue + "\"", null);
    }

    /**
     * Reports that an unimplemented piece of functionality was encountered
     * This method never returns normally.
     *
     * @throws InspectorError unconditionally
     */
    public static InspectorError unimplemented() {
        throw error("unimplemented functionality", null);
    }

    /**
     * Reports that an unimplemented piece of functionality was encountered
     * This method never returns normally.
     *
     * @param message description of the unimplemented functionality
     * @throws InspectorError unconditionally
     */
    public static InspectorError unimplemented(String message) {
        throw error("unimplemented " + message, null);
    }

}

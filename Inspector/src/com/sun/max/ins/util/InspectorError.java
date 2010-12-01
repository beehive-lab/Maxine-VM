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

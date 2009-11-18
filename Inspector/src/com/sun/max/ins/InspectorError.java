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
package com.sun.max.ins;

import com.sun.max.gui.*;

/**
 * We use this when adding InspectorException into play would violate pre-existing interfaces.
 *
 * @author Bernd Mathiske
 */
public class InspectorError extends Error {
    private static final String ERROR_DISPLAY_PROPERTY = "maxine.ins.errordisplay";
    private static final String dialogDisplayProperty = System.getProperty(ERROR_DISPLAY_PROPERTY);
    private static final boolean dialogDisplay = dialogDisplayProperty == null || dialogDisplayProperty.equals("dialog");

    public InspectorError() {
        super();
    }

    public InspectorError(String message) {
        super(message);
    }

    public InspectorError(Throwable cause) {
        super(cause.toString(), cause);
    }

    public InspectorError(String message, Throwable cause) {
        super(message, cause);
    }

    public void display(Inspection inspection) {
        printStackTrace();
        if (dialogDisplay) {
            ThrowableDialog.showLater(this, inspection.gui().frame(), "Inspector Error");
        }
    }

    /**
     * Throw error if condition not true.
     */
    public static void check(boolean condition, String message) {
        if (!condition) {
            throw new InspectorError(message);
        }
    }
}

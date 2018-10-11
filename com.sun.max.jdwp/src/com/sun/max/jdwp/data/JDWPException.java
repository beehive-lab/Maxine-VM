/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.jdwp.data;

import com.sun.max.jdwp.constants.Error;

/**
 * Class representing an exception during JDWP processing. There is a string message as well as a JDWP error code associated with this exception.
 * For possible values of the error code see {@link com.sun.max.jdwp.constants.Error}.
 */
public class JDWPException extends Exception {

    private final short errorCode;
    private final String message;

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
        this.errorCode = errorCode;
        this.message = message;
    }

    /**
     * The JDWP error code that identifies the exception type.
     * @return the JDWP error code
     */
    public int errorCode() {
        return errorCode;
    }

    /**
     * String message that describes the exception.
     * @return the exception description
     */
    public String message() {
        return message;
    }

    @Override
    public String toString() {
        return "JDWPException(" + errorCode + ", " + message + ")";
    }
}

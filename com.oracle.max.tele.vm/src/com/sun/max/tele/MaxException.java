/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele;

/**
 * Abstract parent class for all checked exceptions that might be
 * thrown by client interfaces in the Tele layer of code.
 *
 * @author Michael Van De Vanter
 */
public abstract class MaxException extends Exception {

    /**
     * {@inheritDoc}
     * <br>
     * Creates a checked exception that can be
     * thrown when access to the VM fails for
     * some non-standard reason.
     *
     * @see Exception#Exception()
     */    protected MaxException() {
        super("");
    }

    /**
     * {@inheritDoc}
     * <br>
     * Creates a checked exception that can be
     * thrown when access to the VM fails for
     * some non-standard reason.
     *
     * @param message a human readable explanation for the exception
     * @see Exception#Exception(String)
     */
    protected MaxException(String message) {
        super(message);
    }

    /**
     * {@inheritDoc}
     * <br>
     * Creates a checked exception that can be
     * thrown when access to the VM fails for
     * some non-standard reason.
     *
     * @param message a human readable explanation for the exception
     * @param cause the cause of the exception
     * @see Exception#Exception(String, Throwable)
     */
    protected MaxException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * {@inheritDoc}
     * <br>
     * Creates a checked exception that can be
     * thrown when access to the VM fails for
     * some non-standard reason.
     *
     * @param cause the cause of the exception
     * @see Exception#Exception(Throwable)
     */
    protected MaxException(Throwable cause) {
        super(cause);
    }
}


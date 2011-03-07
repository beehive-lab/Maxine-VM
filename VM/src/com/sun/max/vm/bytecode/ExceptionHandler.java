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
package com.sun.max.vm.bytecode;

import java.util.*;

import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;

/**
 * A node in a linked list of objects describing the exception handlers active for a given BCI.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class ExceptionHandler {

    private final ExceptionHandler next;
    private final int catchTypeIndex;
    private final int bci;

    /**
     * Creates an object representing an exception handler.
     *
     * @param next one or more other exception handlers that cover the same BCI as this handler
     * @param catchTypeIndex the constant pool index of the {@link ClassConstant} representing the type of exceptions
     *            caught by this handler
     * @param bci the BCI denoting the start of this exception handler
     */
    private ExceptionHandler(ExceptionHandler next, int catchTypeIndex, int bci) {
        this.catchTypeIndex = catchTypeIndex;
        this.bci = bci;

        ExceptionHandler n = next;
        while (n != null && n.catchTypeIndex == catchTypeIndex) {
            n = n.next;
        }
        this.next = n;
    }

    public ExceptionHandler next() {
        return next;
    }

    /**
     * Gets the constant pool index of the {@link ClassConstant} representing the type of exceptions caught by this
     * handler.
     */
    public int catchTypeIndex() {
        return catchTypeIndex;
    }

    /**
     * Gets the BCI denoting the start of this exception handler.
     */
    public int bci() {
        return bci;
    }

    @Override
    public int hashCode() {
        final int n = bci ^ catchTypeIndex;
        if (next == null) {
            return n;
        }
        return n * next.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ExceptionHandler)) {
            return false;
        }
        final ExceptionHandler handler = (ExceptionHandler) other;
        if (catchTypeIndex != handler.catchTypeIndex || bci != handler.bci) {
            return false;
        }
        if (next == null) {
            return handler.next == null;
        }
        return next.equals(handler.next);
    }

    /**
     * Creates a mapping from each BCI within the range covered by at least one exception handler to the
     * list of exception handlers that cover the BCI. Note that the returned mapping includes non-null entries for
     * all BCIs covered by at least one exception handler, including BCIs that may be in the middle of an
     * instruction.
     *
     * @return an array mapping each BCI to an exception handler list (or null)
     */
    public static ExceptionHandler[] createHandlerMap(int codeLength, ExceptionHandlerEntry[] exceptionHandlerEntries) {
        final ExceptionHandler[] handlerMap = new ExceptionHandler[codeLength];
        final HashMap<ExceptionHandler, ExceptionHandler> handlers = new HashMap<ExceptionHandler, ExceptionHandler>();
        for (int i = exceptionHandlerEntries.length - 1; i >= 0; i--) {
            ExceptionHandlerEntry info = exceptionHandlerEntries[i];
            final int catchTypeIndex = info.catchTypeIndex();
            for (int bci = info.startBCI(); bci < info.endBCI(); bci++) {
                final ExceptionHandler candidate = new ExceptionHandler(handlerMap[bci], catchTypeIndex, info.handlerBCI());
                ExceptionHandler handler = handlers.get(candidate);
                if (handler == null) {
                    handlers.put(candidate, candidate);
                    handler = candidate;
                }
                handlerMap[bci] = handler;
            }
        }
        return handlerMap;
    }

    public static ExceptionHandler[] createHandlerMap(CodeAttribute codeAttribute) {
        final ExceptionHandlerEntry[] exceptionHandlerTable = codeAttribute.exceptionHandlerTable();
        if (exceptionHandlerTable.length == 0) {
            return null;
        }
        return createHandlerMap(codeAttribute.code().length, exceptionHandlerTable);
    }
}

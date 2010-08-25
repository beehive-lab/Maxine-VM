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
package com.sun.max.vm.bytecode;

import java.util.*;

import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;

/**
 * A node in a linked list of objects describing the exception handlers active for a given bytecode position.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class ExceptionHandler {

    private final ExceptionHandler next;
    private final int catchTypeIndex;
    private final int position;

    /**
     * Creates an object representing an exception handler.
     *
     * @param next one or more other exception handlers that cover the same bytecode position as this handler
     * @param catchTypeIndex the constant pool index of the {@link ClassConstant} representing the type of exceptions
     *            caught by this handler
     * @param position the bytecode position denoting the start of this exception handler
     */
    private ExceptionHandler(ExceptionHandler next, int catchTypeIndex, int position) {
        this.catchTypeIndex = catchTypeIndex;
        this.position = position;

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
     * Gets the bytecode position denoting the start of this exception handler.
     */
    public int position() {
        return position;
    }

    @Override
    public int hashCode() {
        final int n = position ^ catchTypeIndex;
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
        if (catchTypeIndex != handler.catchTypeIndex || position != handler.position) {
            return false;
        }
        if (next == null) {
            return handler.next == null;
        }
        return next.equals(handler.next);
    }

    /**
     * Creates a mapping from each bytecode position within the range covered by at least one exception handler to the
     * list of exception handlers that cover the position. Note that the returned mapping includes non-null entries for
     * all positions covered by at least one exception handler, including positions that may be in the middle of an
     * instruction.
     *
     * @return an array mapping each byte code position to an exception handler list (or null)
     */
    public static ExceptionHandler[] createHandlerMap(int codeLength, ExceptionHandlerEntry[] exceptionHandlerEntries) {
        final ExceptionHandler[] handlerMap = new ExceptionHandler[codeLength];
        final HashMap<ExceptionHandler, ExceptionHandler> handlers = new HashMap<ExceptionHandler, ExceptionHandler>();
        for (int i = exceptionHandlerEntries.length - 1; i >= 0; i--) {
            ExceptionHandlerEntry info = exceptionHandlerEntries[i];
            final int catchTypeIndex = info.catchTypeIndex();
            for (int position = info.startPosition(); position < info.endPosition(); position++) {
                final ExceptionHandler candidate = new ExceptionHandler(handlerMap[position], catchTypeIndex, info.handlerPosition());
                ExceptionHandler handler = handlers.get(candidate);
                if (handler == null) {
                    handlers.put(candidate, candidate);
                    handler = candidate;
                }
                handlerMap[position] = handler;
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

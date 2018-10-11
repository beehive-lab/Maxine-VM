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
package com.sun.max.tele.interpreter;

import java.lang.reflect.*;

import com.sun.max.vm.value.*;

/**
 * An exception thrown while executing the {@linkplain TeleInterpreter interpreter}.
 *
 * Instances of this exception type are constructed in the context of the interpreter's {@linkplain Machine machine}
 * state which is used to construct a stack trace of the interpreter's call stack at the time of the exception.
 */
public class TeleInterpreterException extends InvocationTargetException {

    private final ReferenceValue throwableReference;

    public TeleInterpreterException(Throwable throwable, Machine machine) {
        super(throwable, throwable.getMessage());
        throwableReference = ReferenceValue.from(throwable);
        initStackTrace(machine);
    }

    public ReferenceValue throwableReference() {
        return throwableReference;
    }

    public Class throwableType() {
        return throwableReference.getClassActor().toJava();
    }

    private void initStackTrace(Machine machine) {
        ExecutionFrame frame = machine.currentThread().frame();
        if (frame == null) {
            setStackTrace(new StackTraceElement[0]);
        } else {
            int i = 0;
            final int depth = frame.depth();
            final StackTraceElement[] stackTrace = new StackTraceElement[depth];
            while (frame != null) {
                stackTrace[i++] = frame.method().toStackTraceElement(frame.currentOpcodePosition());
                frame = frame.callersFrame();
            }
            setStackTrace(stackTrace);
        }
    }

    /**
     * Returns the value of calling {@link #toString()} on the {@link #getCause() wrapped} exception.
     */
    @Override
    public String toString() {
        return getCause().toString();
    }
}

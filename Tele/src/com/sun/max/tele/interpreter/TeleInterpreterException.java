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
package com.sun.max.tele.interpreter;

import java.lang.reflect.*;

import com.sun.max.vm.value.*;

/**
 * An exception thrown while executing the {@linkplain TeleInterpreter interpreter}.
 *
 * Instances of this exception type are constructed in the context of the interpreter's {@linkplain Machine machine}
 * state which is used to construct a stack trace of the interpreter's call stack at the time of the exception.
 *
 * @author Doug Simon
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

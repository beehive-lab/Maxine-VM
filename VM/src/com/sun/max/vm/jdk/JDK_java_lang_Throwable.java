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
package com.sun.max.vm.jdk;

import static com.sun.cri.bytecode.Bytecodes.*;
import static com.sun.cri.bytecode.Bytecodes.Infopoints.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * Substitutions for {@link Throwable} that collect the stack trace.
 *
 * @author Ben L. Titzer
 * @author Laurent Daynes
 * @author Doug Simon
 */
@METHOD_SUBSTITUTIONS(Throwable.class)
public final class JDK_java_lang_Throwable {

    private JDK_java_lang_Throwable() {
    }

    /**
     * Casts this object to its corresponding {@code java.lang.Throwable} instance.
     * @return this object cast to the {@code java.lang.Throwable} type
     */
    @INTRINSIC(UNSAFE_CAST)
    private native Throwable thisThrowable();

    @INTRINSIC(UNSAFE_CAST)
    static native JDK_java_lang_Throwable asThis(Throwable t);

    @ALIAS(declaringClass = Throwable.class)
    StackTraceElement[] stackTrace;

    /**
     * Fills in the stack trace for this exception. This implementation eagerly creates a
     * stack trace and fills in all the {@link java.lang.StackTraceElement stack trace elements}.
     * @see java.lang.Throwable#fillInStackTrace()
     * @return the throwable with a filled-in stack trace (typically this object)
     */
    @SUBSTITUTE
    public synchronized Throwable fillInStackTrace() {
        return fillInStackTraceNoSync();
    }

    @INLINE
    private Throwable fillInStackTraceNoSync() {
        // TODO: one possible optimization is to only record the sequence of frames for an exception
        // and build the exception stack trace elements later. There is a field in the Throwable object
        // called "backtrace" to allow for some natively cached stuff for this purpose.
        final Throwable throwable = thisThrowable();
        if (throwable instanceof OutOfMemoryError) {
            // Don't record stack traces in situations where memory may be exhausted
            return throwable;
        }
        final ClassActor throwableActor = ClassActor.fromJava(throwable.getClass());
        // use the stack walker to collect the frames
        final StackFrameWalker sfw = new VmStackFrameWalker(VmThread.current().tla());
        final Pointer ip = Pointer.fromLong(here());
        final Pointer sp = VMRegister.getCpuStackPointer();
        final Pointer fp = VMRegister.getCpuFramePointer();
        StackTraceElement[] stackTrace = getStackTrace(sfw, ip, sp, fp, throwableActor, Integer.MAX_VALUE);
        JDK_java_lang_Throwable thisThrowable = asThis(throwable);
        thisThrowable.stackTrace = stackTrace;
        return throwable;
    }

    /**
     * Gets a stack trace for a given stack.
     *
     * @param exceptionClass the class of the exception for which the stack trace is being constructed.
     *            This is used to elide the chain of constructors calls for this class and its super classes.
     *            If this value is {@code null}, then the stack trace is not for an exception and no eliding is performed.
     * @param maxDepth the maximum number of elements in the returned array or {@link Integer#MAX_VALUE} if the complete
     *            sequence of stack trace elements for {@code stackFrames} is required
     * @return an array of stack trace elements derived from {@code stackFrames} of length
     */
    public static StackTraceElement[] getStackTrace(StackFrameWalker walker, Pointer ip, Pointer sp, Pointer fp, final ClassActor exceptionClass, final int maxDepth) {
        if (maxDepth <= 0) {
            return new StackTraceElement[0];
        }
        StackTraceVisitor stv = new StackTraceVisitor.Default(exceptionClass, maxDepth);
        stv.walk(walker, ip, sp, fp);
        return stv.trace();
    }


    /**
     * Gets the depth of the stack trace, in the number of stack trace elements.
     *
     * @see java.lang.Throwable#getStackTraceDepth()
     * @return the number of stack trace elements
     */
    @SUBSTITUTE
    private int getStackTraceDepth() {
        final StackTraceElement[] elements = getStackTraceElements();
        if (elements != null) {
            return elements.length;
        }
        return 0;
    }

    /**
     * Gets a single stack trace element at the specified index.
     * @see java.lang.Throwable#getStackTraceElement(int)
     * @param index the index into the stack trace at which to get the element
     * @return the element at the specified index
     */
    @SUBSTITUTE
    private StackTraceElement getStackTraceElement(int index) {
        final StackTraceElement[] elements = getStackTraceElements();
        if (elements != null) {
            return elements[index];
        }
        return null;
    }

    /**
     * Gets the complete stack trace for this throwable.
     * @see java.lang.Throwable#getStackTraceElements()
     * @return an array of stack trace elements representing the complete stack trace
     */
    @INLINE
    private StackTraceElement[] getStackTraceElements() {
        JDK_java_lang_Throwable thisThrowable = asThis(thisThrowable());
        return thisThrowable.stackTrace;
    }

}

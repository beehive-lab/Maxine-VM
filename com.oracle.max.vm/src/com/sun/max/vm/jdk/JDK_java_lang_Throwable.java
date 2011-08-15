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
package com.sun.max.vm.jdk;

import static com.sun.cri.bytecode.Bytecodes.*;
import static com.sun.cri.bytecode.Bytecodes.Infopoints.*;

import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * Substitutions for {@link Throwable} that collect the stack trace.
 *
 */
@METHOD_SUBSTITUTIONS(Throwable.class)
public final class JDK_java_lang_Throwable {

    public static boolean StackTraceInThrowable = true;
    static {
        VMOptions.addFieldOption("-XX:", "StackTraceInThrowable", "Collect backtrace in throwable when exception happens.");
    }

    private static final ObjectThreadLocal<Throwable> TRACE_UNDER_CONSTRUCTION = new ObjectThreadLocal<Throwable>("TRACE_UNDER_CONSTRUCTION",
                    "Exception whose back or stack trace is currently being constructed");


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

    @ALIAS(declaringClass = Throwable.class)
    Object backtrace;

    // Introduced in JDK 7 as the default value of stackTrace (instead of null in JDK 6
    @ALIAS(declaringClass = Throwable.class, optional = true)
    private static StackTraceElement[] UNASSIGNED_STACK;

    /**
     * Fills in the stack trace for this exception. This implementation eagerly creates a
     * stack trace and fills in all the {@link java.lang.StackTraceElement stack trace elements}.
     *
     * @see java.lang.Throwable#fillInStackTrace()
     * @return the throwable with a filled-in stack trace (typically this object)
     */
    @SUBSTITUTE
    public synchronized Throwable fillInStackTrace() {
        final Throwable throwable = thisThrowable();
        if (!StackTraceInThrowable || throwable instanceof OutOfMemoryError) {
            // Don't record stack traces in situations where memory may be exhausted
            return throwable;
        }

        if (TRACE_UNDER_CONSTRUCTION.get() != null) {
            FatalError.unexpected("Recursive exception while constructing back trace for " + this + " (outer exception: " + TRACE_UNDER_CONSTRUCTION.get() + ")");
        }
        TRACE_UNDER_CONSTRUCTION.set(throwable);

        final ClassActor throwableActor = ClassActor.fromJava(throwable.getClass());
        // use the stack walker to collect the frames
        final StackFrameWalker sfw = new VmStackFrameWalker(VmThread.current().tla());
        final Pointer ip = Pointer.fromLong(here());
        final Pointer sp = VMRegister.getCpuStackPointer();
        final Pointer fp = VMRegister.getCpuFramePointer();
        try {
            Backtrace backtrace = getBacktrace(sfw, ip, sp, fp, throwableActor, Integer.MAX_VALUE);
            this.backtrace = backtrace;
        } catch (OutOfMemoryError e) {
            // Could not build backtrace due to memory shortage
            stackTrace = new StackTraceElement[0];
        }
        TRACE_UNDER_CONSTRUCTION.set(null);
        return throwable;
    }

    /**
     * A back trace is a lighter weight representation of a stack trace than
     * an array of {@link StackTraceElement}s.
     */
    static class Backtrace extends StackTraceVisitor {

        static final int INITIAL_LENGTH = 200;

        int count;
        int[] lineNos;
        ClassMethodActor[] methods;

        public Backtrace(ClassActor exceptionClass, int maxDepth) {
            super(exceptionClass, maxDepth);

            int len = Math.min(maxDepth, INITIAL_LENGTH);
            lineNos = new int[len];
            methods = new ClassMethodActor[len];
        }

        @Override
        public void clear() {
            for (int i = count - 1; i >= 0; i--) {
                methods[i] = null;
            }
            count = 0;
        }

        @Override
        public boolean add(ClassMethodActor methodActor, int sourceLineNumber) {
            if (count == lineNos.length) {
                expand();
            }
            lineNos[count] = sourceLineNumber;
            methods[count] = methodActor;
            count++;
            return true;
        }

        private void expand() {
            int newLength = lineNos.length * 2;
            lineNos = Arrays.copyOf(lineNos, newLength);
            methods = Arrays.copyOf(methods, newLength);
        }

        StackTraceElement stackTraceElement(int index) {
            ClassMethodActor method = methods[index];
            ClassActor holder = method.holder();
            int sourceLineNumber = lineNos[index];
            return new StackTraceElement(holder.name.toString(), method.name.toString(), holder.sourceFileName, sourceLineNumber);
        }

        @Override
        public StackTraceElement[] getTrace() {
            StackTraceElement[] trace = new StackTraceElement[count];
            for (int i = 0; i != count; i++) {
                trace[i] = stackTraceElement(i);
            }
            return trace;
        }
    }

    /**
     * Gets a back trace for a given stack.
     *
     * @param exceptionClass the class of the exception for which the back trace is being constructed.
     *            This is used to elide the chain of constructors calls for this class and its super classes.
     *            If this value is {@code null}, then the back trace is not for an exception and no eliding is performed.
     * @param maxDepth the maximum length of the returned back trace
     * @return the back trace
     */
    static Backtrace getBacktrace(StackFrameWalker walker, Pointer ip, Pointer sp, Pointer fp, final ClassActor exceptionClass, final int maxDepth) {
        if (maxDepth <= 0) {
            return null;
        }
        Backtrace backtrace = new Backtrace(exceptionClass, maxDepth);
        backtrace.walk(walker, ip, sp, fp);
        return backtrace;
    }

    /**
     * Gets a stack trace for a given stack.
     *
     * @param exceptionClass the class of the exception for which the stack trace is being constructed.
     *            This is used to elide the chain of constructors calls for this class and its super classes.
     *            If this value is {@code null}, then the stack trace is not for an exception and no eliding is performed.
     * @param maxDepth the maximum number of elements in the returned array
     * @return the stack trace elements for the trace
     */
    public static StackTraceElement[] getStackTrace(StackFrameWalker walker, Pointer ip, Pointer sp, Pointer fp, final ClassActor exceptionClass, final int maxDepth) {
        Backtrace backtrace = getBacktrace(walker, ip, sp, fp, exceptionClass, maxDepth);
        if (backtrace == null) {
            return new StackTraceElement[0];
        }
        return backtrace.getTrace();
    }

    @SUBSTITUTE
    private synchronized StackTraceElement[] getOurStackTrace() {
        // Initialize stack trace if this is the first call to this method
        if (stackTrace == null || stackTrace == UNASSIGNED_STACK) {
            if (backtrace == null) {
                stackTrace = new StackTraceElement[0];
            } else {
                if (TRACE_UNDER_CONSTRUCTION.get() != null) {
                    FatalError.unexpected("Recursive exception while constructing stack trace for " + this + " (outer exception: " + TRACE_UNDER_CONSTRUCTION.get() + ")");

                }
                final Throwable throwable = thisThrowable();
                TRACE_UNDER_CONSTRUCTION.set(throwable);

                try {
                    stackTrace = ((Backtrace) backtrace).getTrace();
                } catch (OutOfMemoryError e) {
                    // Could not build backtrace due to memory shortage
                    stackTrace = new StackTraceElement[0];
                }
                TRACE_UNDER_CONSTRUCTION.set(null);

                // Let the GC clean up the back trace
                backtrace = null;
            }
        }
        return stackTrace;
    }

    /**
     * Gets the depth of the stack trace, in the number of stack trace elements.
     *
     * NOTE: This method is called by the JVM_GetStackTraceDepth function.
     *
     * @see java.lang.Throwable#getStackTraceDepth()
     * @return the number of stack trace elements
     */
    @SUBSTITUTE
    private int getStackTraceDepth() {
        return getOurStackTrace().length;
    }

    /**
     * Gets a single stack trace element at the specified index.
     *
     * NOTE: This method is called by the JVM_GetStackTraceElement function.
     *
     * @see java.lang.Throwable#getStackTraceElement(int)
     * @param index the index into the stack trace at which to get the element
     * @return the element at the specified index
     */
    @SUBSTITUTE
    private StackTraceElement getStackTraceElement(int index) {
        final StackTraceElement[] elements = getOurStackTrace();
        if (elements != null && index >= 0 && index < elements.length) {
            return elements[index];
        }
        return null;
    }

}

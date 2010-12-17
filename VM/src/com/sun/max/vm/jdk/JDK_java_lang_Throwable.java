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
package com.sun.max.vm.jdk;

import static com.sun.cri.bytecode.Bytecodes.*;
import static com.sun.cri.bytecode.Bytecodes.Infopoints.*;

import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
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

    /**
     * Source frame visitor for building a stack trace.
     */
    static final class StackTraceVisitor extends SourceFrameVisitor {
        /**
         * The class of the exception for which the stack trace is being constructed.
         * This is used to elide the chain of constructors calls for this class and its super classes.
         * If this value is {@code null}, then the stack trace is not for an exception and no eliding is performed.
         */
        ClassActor exceptionClass;

        /**
         * The maximum number of elements in the returned array or {@link Integer#MAX_VALUE} if the complete
         * sequence of stack trace elements for {@code stackFrames} is required.
         */
        final int maxDepth;

        final ArrayList<StackTraceElement> trace = new ArrayList<StackTraceElement>(20);

        StackTraceVisitor(ClassActor exceptionClass, int maxDepth) {
            this.exceptionClass = exceptionClass;
            this.maxDepth = maxDepth;
        }

        @Override
        public boolean visitSourceFrame(ClassMethodActor method, int bci, boolean trapped, long frameId) {
            if (trapped) {
                trace.clear();
                exceptionClass = null;
            }
            if (exceptionClass != null) {
                if (method.holder() == exceptionClass && method.isInstanceInitializer()) {
                    // This will initiate filling the stack trace.
                    exceptionClass = null;
                }
                return true;
            }

            // Undo effect of method substitution
            method = method.original();

            final ClassActor holder = method.holder();
            int sourceLineNumber;
            if (method.isNative()) {
                sourceLineNumber = -2;
            } else {
                if (holder.isReflectionStub()) {
                    // ignore reflective invocation stubs
                    return true;
                }
                sourceLineNumber = bci >= 0 ? method.sourceLineNumber(bci) : -1;
            }
            final String sourceFileName = holder.sourceFileName;
            StackTraceElement ste = new StackTraceElement(holder.name.toString(), method.name.toString(), sourceFileName, sourceLineNumber);
            trace.add(ste);
            return trace.size() < maxDepth;
        }

        StackTraceElement[] trace() {
            return trace.toArray(new StackTraceElement[trace.size()]);
        }
    }

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
        StackTraceVisitor stv = new StackTraceVisitor(exceptionClass, maxDepth);
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

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
package com.sun.max.vm.stack;

import java.util.*;

import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;


/**
 * Source frame visitor for building a stack trace.
 *
 * @author Doug Simon
 * @author Mick Jordan
 *
 */
public abstract class StackTraceVisitor extends SourceFrameVisitor {
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

    TraceStorage traceStorage;

    /**
     * Abstracts the mechanism for storing the trace.
     */
    public interface TraceStorage {
        /**
         * Reset the trace.
         */
        void clear();
        /**
         * Add a trace element.
         * @param methodActor
         * @param sourceLineNumber
         * @return true if the trace gathering should continue.
         */
        boolean add(ClassMethodActor methodActor, int sourceLineNumber);
        /**
         * Access the trace.
         * @return
         */
        StackTraceElement[] getTrace();
    }

    /**
     * Variant where caller provides a custom {@link TraceStorage}.
     */
    public static class Custom extends StackTraceVisitor {
        public Custom(ClassActor exceptionClass, int maxDepth, TraceStorage traceHandler) {
            super(exceptionClass, maxDepth, traceHandler);
        }
    }

    /**
     * Default implementation, with storage allocated internally for the trace elements.
     *
    */
    public static class Default extends StackTraceVisitor implements TraceStorage {
        final ArrayList<StackTraceElement> trace = new ArrayList<StackTraceElement>(20);

        public Default(ClassActor exceptionClass, int maxDepth) {
            super(exceptionClass, maxDepth, null);
            this.traceStorage = this;
        }

        public void clear() {
            trace.clear();
        }

        public boolean add(ClassMethodActor methodActor, int sourceLineNumber) {
            final ClassActor holder = methodActor.holder();
            StackTraceElement ste = new StackTraceElement(holder.name.toString(), methodActor.name.toString(), holder.sourceFileName, sourceLineNumber);
            trace.add(ste);
            return trace.size() < maxDepth;
        }

        public StackTraceElement[] getTrace() {
            return trace.toArray(new StackTraceElement[trace.size()]);
        }

    }

    protected StackTraceVisitor(ClassActor exceptionClass, int maxDepth, TraceStorage traceHandler) {
        this.exceptionClass = exceptionClass;
        this.maxDepth = maxDepth;
        this.traceStorage = traceHandler;
    }

    @Override
    public boolean visitSourceFrame(ClassMethodActor method, int bci, boolean trapped, long frameId) {
        if (trapped) {
            traceStorage.clear();
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
        return traceStorage.add(method, sourceLineNumber);
    }

    public StackTraceElement[] trace() {
        return traceStorage.getTrace();
    }
}


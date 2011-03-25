/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.stack;

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

    protected StackTraceVisitor(ClassActor exceptionClass, int maxDepth) {
        this.exceptionClass = exceptionClass;
        this.maxDepth = maxDepth;
    }

    @Override
    public boolean visitSourceFrame(ClassMethodActor method, int bci, boolean trapped, long frameId) {
        if (trapped) {
            clear();
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
        return add(method, sourceLineNumber);
    }

    /**
     * Adds an element to the trace.
     *
     * @return {@code true} if the stack walk should continue to the next element
     */
    public abstract boolean add(ClassMethodActor method, int sourceLineNumber);

    /**
     * Clears all trace elements.
     */
    public abstract void clear();

    /**
     * Gets the trace elements.
     */
    public abstract StackTraceElement[] getTrace();
}


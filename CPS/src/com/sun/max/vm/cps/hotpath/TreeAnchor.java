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

package com.sun.max.vm.cps.hotpath;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.cps.*;
import com.sun.max.vm.cps.hotpath.compiler.*;
import com.sun.max.vm.cps.tir.*;

/**
 * Implements a counter that is associated with loop headers. When the counter reaches a threshold value a Hotpath is
 * detected and recorded.
 *
 * @author Michael Bebenita
 */
public class TreeAnchor extends Anchor {

    /**
     * {@link ClassMethodActor} associated with this {@link TreeAnchor}.
     */
    public ClassMethodActor method() {
        return location.classMethodActor;
    }

    /**
     * Bytecodes position at which this counter is inserted.
     */
    public int position() {
        return location.bci;
    }

    private int frequency;

    /**
     * The tracing threshold, if this value is exceeded a new trace is recorded.
     */
    private final int tracingThreshold;

    /**
     * The number of times the tracer tried to record a trace.
     */
    private int numberOfTries;

    public int incrementNumberOfTries() {
        return numberOfTries++;
    }

    public int numberOfTries() {
        return numberOfTries;
    }

    /**
     * Trace tree associated with this {@link TreeAnchor}.
     */
    private TirTree tree;

    public TirTree tree() {
        return tree;
    }

    public void setTree(TirTree tree) {
        this.tree = tree;
    }

    private int stackHeight;

    public int stackHeight() {
        return stackHeight;
    }

    public void setStackHeight(int stackHeight) {
        this.stackHeight = stackHeight;
    }

    public TreeAnchor(BytecodeLocation location, int tracingThreshold) {
        super(location);
        this.tracingThreshold = tracingThreshold;
    }

    /**
     * This is the main entry point into the Hotpath compiler. JIT instrumentation invokes this method
     * every time the {@link TreeAnchor} is reached. Here we have the opportunity to re-JIT with
     * tracing instrumentation in order to start tracing or simply jump into a trace tree. The JIT
     * instrumentation jumps to the {@link Address} returned by this method if it is not zero.
     *
     * This method is not thread safe. The counter may fail to increment in certain cases and should
     * be treated as a lower bound.
     *
     * @return the address at which execution should resume. A zero address indicates that the execution
     * should resume.
     */
    public final Address visit() {
        incrementFrequency();

        // Execute the associated trace tree if one exists.
        if (hasTree()) {
            return tree.targetTree().targetMethod().start();
        }

        // Start recording a new trace tree if we've exceeded thresholds.
        if (exceedsTracingThreshold()) {
            return Hotpath.start(this);
        }

        // Continue executing code.
        return Address.zero();
    }

    public boolean exceedsTracingThreshold() {
        return frequency > tracingThreshold;
    }

    public boolean hasTree() {
        return tree != null;
    }

    @Override
    public String toString() {
        final int line = location.classMethodActor.codeAttribute().lineNumberTable().findLineNumber(location.bci);
        return "loc: " + location.toString() + ", line: " + line + ", count: " + frequency;
    }

    public final void incrementFrequency() {
        frequency++;
    }

    public int frequency() {
        return frequency;
    }

    public void resetFrequency() {
        frequency = 0;
    }
}

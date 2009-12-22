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

package com.sun.max.vm.profile;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.cps.tir.*;
import com.sun.max.vm.hotpath.*;
import com.sun.max.vm.hotpath.compiler.*;

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
     * Bytecode position at which this counter is inserted.
     */
    public int position() {
        return location.bytecodePosition;
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
        final int line = location.classMethodActor.codeAttribute().lineNumberTable().findLineNumber(location.bytecodePosition);
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

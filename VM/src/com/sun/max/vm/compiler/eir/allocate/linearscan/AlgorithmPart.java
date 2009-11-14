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
package com.sun.max.vm.compiler.eir.allocate.linearscan;

import com.sun.max.profile.*;
import com.sun.max.profile.Metrics.*;
import com.sun.max.util.timer.*;
import com.sun.max.vm.compiler.eir.*;

/**
 * Part of the linear scan register allocator algorithm. Used for timing and assertion of pre and post conditions.
 * Provides access to an algorithm data object holding all necessary data for performing the register allocation.
 *
 * @author Thomas Wuerthinger
 */
public abstract class AlgorithmPart {

    private Timer timer;
    private String name;
    private AlgorithmData data;

    public AlgorithmPart(int phase) {

        if (LinearScanRegisterAllocator.PHASE_TIMING ||
                        LinearScanRegisterAllocator.DETAILED_COUNTING ||
                        LinearScanRegisterAllocator.DETAILED_TIMING) {
            String number = "" + phase;
            if (phase < 10) {
                number = "0" + number;
            }
            name = "Phase" + number + "." + getClass().getSimpleName();
        }

        if (LinearScanRegisterAllocator.PHASE_TIMING) {
            timer = GlobalMetrics.newTimer(LinearScanRegisterAllocator.METRICS_PREFIX + ".TotalTiming." + this.name, Clock.SYSTEM_MILLISECONDS);
        }
    }

    protected final Timer createTimer(String name) {
        if (LinearScanRegisterAllocator.DETAILED_TIMING) {
            return GlobalMetrics.newTimer(LinearScanRegisterAllocator.METRICS_PREFIX + ".DetailedTiming." + this.name + "." + name, Clock.SYSTEM_MILLISECONDS);
        }
        return null;
    }

    protected final Counter createCounter(String name) {
        if (LinearScanRegisterAllocator.DETAILED_COUNTING) {
            return GlobalMetrics.newCounter(LinearScanRegisterAllocator.METRICS_PREFIX + ".Counting." + this.name + "." + name);
        }
        return null;
    }

    /**
     * Runs the algorithm part on the given data object.
     * @param data the data object that provides access to the data required for register allocation
     */
    public final void run(AlgorithmData data) {
        this.data = data;

        if (LinearScanRegisterAllocator.PHASE_TIMING) {
            timer.start();
        }

        assert assertPreconditions();
        doit();
        assert assertPostconditions();

        if (LinearScanRegisterAllocator.PHASE_TIMING) {
            timer.stop();
        }
    }

    /**
     * The data object is guaranteed to be a valid value while running the method {@link doit()}.
     * @return the current data object the algorithm is operating on
     */
    public final AlgorithmData data() {
        return data;
    }

    /**
     * @return the method generation object of the current method
     */
    public final EirMethodGeneration generation() {
        return data.generation();
    }

    /**
     * Override this method in subclasses to check preconditions of an algorithm part.
     * @return true
     */
    protected boolean assertPreconditions() {
        return true;
    }

    /**
     * This method is called for every data object. The algorithm part is supposed to do its work on the current data object.
     */
    protected abstract void doit();

    /**
     * Override this method in subclasses to check postconditions of an algorithm part.
     * @return
     */
    protected boolean assertPostconditions() {
        return true;
    }
}

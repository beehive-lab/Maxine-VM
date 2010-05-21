/*
 * Copyright (c) 2008 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.util.timer;

import com.sun.max.annotate.*;
import com.sun.max.profile.*;

/**
 * This class implements a timer that requires no synchronization but maintains an internal stack
 * for handling nested tasks.
 *
 * @author Ben L. Titzer
 */
public class SingleThreadTimer implements Timer {
    private static final int MAXIMUM_NESTING_DEPTH = 20;

    private final Clock clock;
    private final long[] start = new long[MAXIMUM_NESTING_DEPTH];
    private final long[] nested = new long[MAXIMUM_NESTING_DEPTH];
    @RESET
    private int depth;
    @RESET
    private long last;

    public SingleThreadTimer(Clock clock) {
        this.clock = clock;
    }

    public void start() {
        final int d = this.depth;
        nested[d] = 0;
        this.depth = d + 1;
        start[d] = clock.getTicks();
    }

    public void stop() {
        final long time = clock.getTicks();
        final int d = this.depth - 1;
        last = time - start[d];
        if (d > 0) {
            nested[d - 1] += last;
        }
        this.depth = d;
    }

    public Clock getClock() {
        return clock;
    }

    public long getLastElapsedTime() {
        return last;
    }

    public long getLastNestedTime() {
        return nested[depth];
    }
}

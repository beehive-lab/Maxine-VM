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
 * This class implements a simple timer that requires no synchronization or thread-local data structures.
 * It supports a single-use, one-at-a-time approach to timing tasks, and does not support nested tasks.
 *
 * @author Ben L. Titzer
 */
public class SingleUseTimer implements Timer {
    @RESET
    private long start;
    @RESET
    private long last;
    private final Clock clock;

    public SingleUseTimer(Clock clock) {
        this.clock = clock;
    }

    public void start() {
        start = clock.getTicks();
    }

    public void stop() {
        last = clock.getTicks() - start;
    }

    public Clock getClock() {
        return clock;
    }

    public long getLastElapsedTime() {
        return last;
    }

    public long getLastNestedTime() {
        return 0;
    }
}

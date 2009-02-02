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

import com.sun.max.profile.*;

/**
 * This class implements a timer that requires no synchronization but maintains an internal stack
 * for handling nested tasks.
 *
 * @author Ben L. Titzer
 */
public class SingleThreadTimer implements Timer {
    private static final int MAXIMUM_NESTING_DEPTH = 20;

    private final Clock _clock;
    private final long[] _start = new long[MAXIMUM_NESTING_DEPTH];
    private final long[] _nested = new long[MAXIMUM_NESTING_DEPTH];
    private int _depth;
    private long _last;

    public SingleThreadTimer(Clock clock) {
        this._clock = clock;
    }

    public void start() {
        final int depth = _depth;
        _nested[depth] = 0;
        _depth = depth + 1;
        _start[depth] = _clock.getTicks();
    }

    public void stop() {
        final long time = _clock.getTicks();
        final int depth = _depth - 1;
        _last = time - _start[depth];
        if (depth > 0) {
            _nested[depth - 1] += _last;
        }
        _depth = depth;
    }

    public Clock getClock() {
        return _clock;
    }

    public long getLastElapsedTime() {
        return _last;
    }

    public long getLastNestedTime() {
        return _nested[_depth];
    }
}

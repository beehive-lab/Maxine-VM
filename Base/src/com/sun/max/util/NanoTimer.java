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
package com.sun.max.util;

import java.util.*;

/**
 * This is a class that provides support for timing computations (at nanosecond granularity),
 * including nested computations. The timing data gathered for each computation does not include
 * time spent inside timed inner computations.
 *
 * The {@link Timer} class provides similar functionality but at millisecond
 * granularity with support for flat and total times.
 *
 * @author Bernd Mathiske
 * @author Yi Guo
 * @author Aziz
 *
 */
public final class NanoTimer {
    private final class SingleTimer {
        private long _discount;
        private long _startTime;
        private boolean _isSubComputation;
        private Object _key;
        private int _level;
        private SingleTimer(boolean isSubComputation, Object key, int level) {
            _isSubComputation = isSubComputation;
            _startTime = System.nanoTime();
            _key = key;
            _level = level;
        }
    }
    private ThreadLocal<Stack<SingleTimer>> _timers = new ThreadLocal<Stack<SingleTimer>>() {
        @Override
        protected synchronized Stack<SingleTimer> initialValue() {
            return new Stack<SingleTimer>();
        }
    };


    public NanoTimer() {
    }

    private void start(boolean isSubComputation, Object key, int level) {
        final SingleTimer newTimer = new SingleTimer(isSubComputation, key, level);
        _timers.get().push(newTimer);
    }

    /**
     * Mark the start of a new computation whose total time does not count to the charge of the current computation.
     *
     * @param key used to identify the computation. The same key should be used to stop to ensure the starts/stops are properly called
     * @see NanoTimer#stop(Object key)
     */
    public void startNewComputation(Object key) {
        start(false, key, 0);
    }

    public void startNewComputation() {
        startNewComputation(null);
    }

    public void startSubComputation() {
        startSubComputation(null);
    }

    /**
     * Mark the start of a sub computation which is part of the current computation.
     *
     * @param key used to identify the computation. The same key should be used to stop to ensure the starts/stops are properly called
     * @see NanoTimer#stop(Object key)
     */

    public void startSubComputation(Object key) {
        if (_timers.get().isEmpty()) {
            System.out.println("_timer is empty");
            assert false;
        }

        start(true, key, _timers.get().peek()._level + 1);
    }

    /**
     * @return the nesting level of the current subcomputation. Each new computation has level 0.
     */
    public int level() {
        return _timers.get().peek()._level;
    }

    /**
     * Mark the stop of current computation.
     * @param key should be the same key used to start the computation.
     * @return the charged time of the current computation (including sub-computations) in nanoseconds.
     */
    public long stop(Object key) {
        final long endTime = System.nanoTime();
        final SingleTimer timer = _timers.get().pop();
        if (timer._key != key) {
            System.err.println("Start/stop timer mismatch: ");
            System.err.println(timer._key);
            System.err.println(key);
            assert false;
        }

        final long elapsed = endTime - timer._startTime;
        if (!_timers.get().empty()) {
            final SingleTimer outerTimer = _timers.get().peek();
            if (timer._isSubComputation) {
                outerTimer._discount += timer._discount;
            } else {
                outerTimer._discount += elapsed;
            }

        }
        return elapsed - timer._discount;
    }

    /**
     * Mark the stop of current computation.
     * @param key should be the same key used to start the computation.
     * @return the charged time of the current computation (including sub-computations) in nanoseconds.
     */
    public long stop() {
        return stop(null);
    }
}

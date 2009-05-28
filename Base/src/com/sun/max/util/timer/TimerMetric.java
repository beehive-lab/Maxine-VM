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

import java.io.*;

import com.sun.max.annotate.*;
import com.sun.max.profile.*;
import com.sun.max.profile.Metrics.*;

/**
 * This class implements a wrapper around a timer that collects statistics about the time intervals recorded.
 *
 * @author Ben L. Titzer
 */
public class TimerMetric implements Timer, Metric {
    private final Timer _timer;

    @RESET
    private int _count;
    @RESET
    private long _elapsed;
    @RESET
    private long _nested;

    public TimerMetric(Timer timer) {
        this._timer = timer;
    }

    public void start() {
        _timer.start();
    }

    public void stop() {
        _timer.stop();
        synchronized (this) {
            _count++;
            _elapsed += _timer.getLastElapsedTime();
            _nested += _timer.getLastNestedTime();
        }
    }

    public Clock getClock() {
        return _timer.getClock();
    }

    public long getLastElapsedTime() {
        return _timer.getLastElapsedTime();
    }

    public long getLastNestedTime() {
        return _timer.getLastNestedTime();
    }

    public synchronized void reset() {
        _count = 0;
        _elapsed = 0;
        _nested = 0;
    }

    public long getElapsedTime() {
        return _elapsed;
    }

    public long getNestedTime() {
        return _nested;
    }

    public synchronized void report(String name, PrintStream stream) {
        if (_count > 0) {
            final long hz = _timer.getClock().getHZ();
            final long total = _elapsed - _nested;
            if (hz > 0) {
                // report in seconds
                final double secs = total / (double) hz;
                Metrics.report(stream, name, "total", "--", String.valueOf(secs), "seconds");
                Metrics.report(stream, name, "average", "--", String.valueOf(secs / _count), "seconds (" + _count + " intervals)");
            } else {
                // report in ticks
                Metrics.report(stream, name, "total", "--", String.valueOf(total), "ticks");
                Metrics.report(stream, name, "average", "--", String.valueOf(total / (double) _count), "ticks (" + _count + " intervals)");
            }
        }
    }
}

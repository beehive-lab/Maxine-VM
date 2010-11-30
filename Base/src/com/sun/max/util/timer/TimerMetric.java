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

import com.sun.max.profile.*;
import com.sun.max.profile.Metrics.*;

/**
 * This class implements a wrapper around a timer that collects statistics about the time intervals recorded.
 *
 * @author Ben L. Titzer
 */
public class TimerMetric implements Timer, Metric {
    private final Timer timer;

    private int count;
    private long elapsed;
    private long nested;

    public TimerMetric(Timer timer) {
        this.timer = timer;
    }

    public void start() {
        timer.start();
    }

    public void stop() {
        timer.stop();
        synchronized (this) {
            count++;
            elapsed += timer.getLastElapsedTime();
            nested += timer.getLastNestedTime();
        }
    }

    public Clock getClock() {
        return timer.getClock();
    }

    public long getLastElapsedTime() {
        return timer.getLastElapsedTime();
    }

    public long getLastNestedTime() {
        return timer.getLastNestedTime();
    }

    public synchronized void reset() {
        count = 0;
        elapsed = 0;
        nested = 0;
    }

    public long getElapsedTime() {
        return elapsed;
    }

    public long getNestedTime() {
        return nested;
    }

    public int getCount() {
        return count;
    }

    public synchronized void report(String name, PrintStream stream) {
        if (count > 0) {
            final long hz = timer.getClock().getHZ();
            final long total = elapsed - nested;
            if (hz > 0) {
                // report in seconds
                final double secs = total / (double) hz;
                Metrics.report(stream, name, "total", "--", String.valueOf(secs), "seconds");
                Metrics.report(stream, name, "average", "--", String.valueOf(secs / count), "seconds (" + count + " intervals)");
            } else {
                // report in ticks
                Metrics.report(stream, name, "total", "--", String.valueOf(total), "ticks");
                Metrics.report(stream, name, "average", "--", String.valueOf(total / (double) count), "ticks (" + count + " intervals)");
            }
        }
    }
}

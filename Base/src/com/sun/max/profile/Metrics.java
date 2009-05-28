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
package com.sun.max.profile;

import java.io.*;
import java.util.*;
import java.util.Arrays;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * The {@code Metrics} class implements metrics gathering for quick and dirty
 * reporting of various events in a program.
 *
 * @author Ben L. Titzer
 */
public final class Metrics {

    private Metrics() {
        // do nothing.
    }

    private static boolean _enabled;

    public interface Metric {
        void reset();
        void report(String name, PrintStream stream);
    }

    /**
     * This class implements a simple counter that can be both incremented and accumulated.
     *
     * @author Ben L. Titzer
     */
    public static class Counter implements Metric {
        @RESET
        protected int _count;
        @RESET
        protected long _accumulation;

        public synchronized void increment() {
            _count++;
        }

        public synchronized void accumulate(long value) {
            _count++;
            _accumulation += value;
        }

        public synchronized int getCount() {
            return _count;
        }

        public synchronized long getAccumulation() {
            return _accumulation;
        }

        public synchronized void reset() {
            _count = 0;
            _accumulation = 0;
        }

        public synchronized void report(String name, PrintStream stream) {
            if (_accumulation > 0) {
                final double average = _accumulation / (double) _count;
                Metrics.report(stream, name, "total", "--", String.valueOf(_accumulation), "accumulated");
                Metrics.report(stream, name, "average", "--", String.valueOf(average), "accumulated (" + _count + " counted)");
            } else {
                Metrics.report(stream, name, "total", "--", String.valueOf(_count), "counted");
            }
        }
    }

    public static class Rate implements Metric {

        protected final Counter _counter;
        protected final Clock _clock;
        @RESET
        protected long _firstTicks;
        @RESET
        protected long _lastAccumulation;
        @RESET
        protected long _lastTicks;

        public Rate(Counter counter, Clock clock) {
            _counter = counter;
            _clock = clock;
            _firstTicks = clock.getTicks();
            _lastTicks = _firstTicks;
        }

        public double getRate() {
            return (_counter._accumulation - _lastAccumulation) / (double) (_clock.getTicks() - _lastTicks);
        }

        public double getAverageRate() {
            return (_counter._accumulation) / (double) (_clock.getTicks() - _firstTicks);
        }

        public void reset() {
            _lastAccumulation = _counter._accumulation;
            _lastTicks = _clock.getTicks();
        }

        public void report(String name, PrintStream stream) {
            double inst = getRate();
            double avg = getAverageRate();
            if (_clock.getHZ() == 0) {
                Metrics.report(stream, name, "inst.", "--", String.valueOf(inst), "/ tick");
                Metrics.report(stream, name, "average", "--", String.valueOf(avg), "/ tick");
            } else {
                inst = inst / _clock.getHZ();
                avg = avg / _clock.getHZ();
                Metrics.report(stream, name, "inst.", "--", String.valueOf(inst), "/ second");
                Metrics.report(stream, name, "average", "--", String.valueOf(avg), "/ second");
            }
        }
    }

    /**
     * This class represents a distribution of values that have been recorded. Various
     * implementations implement different approximations with different time and space
     * tradeoffs.
     *
     * @author Ben L. Titzer
     */
    public static class Distribution<Value_Type> implements Metric {

        static class Counted implements Comparable<Counted> {
            final int _total;
            final Object _value;

            public Counted(Object value, int total) {
                _total = total;
                _value = value;
            }

            @Override
            public int compareTo(Counted o) {
                if (o._total < _total) {
                    return -1;
                } else if (o._total > _total) {
                    return 1;
                }
                return 0;
            }

        }

        @RESET
        protected int _total;

        public int getTotal() {
            return _total;
        }

        public int getCount(Value_Type value) {
            return -1;
        }

        public void reset() {
            _total = 0;
        }

        public Map<Value_Type, Integer> asMap() {
            return Collections.emptyMap();
        }

        public void report(String name, PrintStream stream) {
            stream.println(name + ": ");
            final Map<Value_Type, Integer> asMap = asMap();
            final Counted[] counts = new Counted[asMap.size()];
            int i = 0;
            for (Map.Entry<Value_Type, Integer> entry : asMap.entrySet()) {
                counts[i++] = new Counted(entry.getKey(), entry.getValue());
            }
            Arrays.sort(counts);
            for (Counted counted : counts) {
                stream.printf("  %-10d : %s\n", counted._total, counted._value);
            }
        }
    }

    /**
     * This method increments the count of a particular named metric, creating it if necessary.
     * @param name the name of the metric to increment
     */
    public static void increment(String name) {
        GlobalMetrics.getCounter(name).increment();
    }

    /**
     * This method increases the accumulation of a particular named metric by the given value.
     * Each {@code accumulate()} call counts as a single {@code increment()} call as well.
     * @param name the name of the metric to accumulate
     * @param value the value to add to the current accumulation
     */
    public static void accumulate(String name, int value) {
        GlobalMetrics.getCounter(name).accumulate(value);
    }

    public static void trace(int level) {
        if (Trace.hasLevel(level)) {
            GlobalMetrics.report(Trace.stream());
        }
    }

    public static boolean enabled() {
        return _enabled;
    }

    static int _longestMetricName = 16;

    public static void report(PrintStream out, String metricName, String runName, String benchName, String value, String units) {
        out.print("@metric ");
        out.print(Strings.padLengthWithSpaces(metricName, _longestMetricName + 1));
        out.print(" ");
        out.print(Strings.padLengthWithSpaces(runName, 9));
        out.print(" ");
        out.print(Strings.padLengthWithSpaces(benchName, 18));
        out.print(" ");
        out.print(Strings.padLengthWithSpaces(20, value));
        out.print(" ");
        out.print(units);
        out.println("");
    }

    public static void enable(boolean enable) {
        _enabled = enable;
    }
}

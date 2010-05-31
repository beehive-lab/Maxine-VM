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

import com.sun.max.*;
import com.sun.max.profile.Metrics.*;
import com.sun.max.util.timer.*;

public class GlobalMetrics {

    static class EntryComparator implements Comparator<Map.Entry<String, Metric>> {
        public int compare(Map.Entry<String, Metric> a, Map.Entry<String, Metric> b) {
            return String.CASE_INSENSITIVE_ORDER.compare(a.getKey(), b.getKey());
        }
    }

    static class MetricSet<T extends Metric> {
        private final Class<T> clazz;
        private final Map<String, T> metrics = new HashMap<String, T>();

        MetricSet(Class<T> mClass) {
            clazz = mClass;
        }
    }

    protected static final Map<Class<? extends Metric>, MetricSet> metricSets = new HashMap<Class<? extends Metric>, MetricSet>();

    /**
     * This method allocates a new counter with the specified name and adds it to the global
     * metric list. If a previous metric with the same name exists, it will return a reference
     * to the first one created.
     * @param name the name of the metric for which to create a counter
     * @return a reference to a code {@code Counter} object which can be incremented and accumulated
     */
    public static Metrics.Counter newCounter(String name) {
        if (name == null) {
            return new Metrics.Counter();
        }
        return getCounter(name);
    }

    public static TimerMetric newTimer(String name, Clock clock) {
        if (name == null) {
            return new TimerMetric(new MultiThreadTimer(clock));
        }
        return getTimer(name, clock);
    }

    public static Metrics.Rate newRate(String name, Metrics.Counter count, Clock clock) {
        if (name == null) {
            return new Rate(count, clock);
        }
        return getRate(name, count, clock);
    }

    static synchronized Metrics.Counter getCounter(String name) {
        Metrics.Counter counter = getMetric(name, Metrics.Counter.class);
        if (counter == null) {
            counter = setMetric(name, Metrics.Counter.class, new Metrics.Counter());
        }
        return counter;
    }

    static synchronized TimerMetric getTimer(String name, Clock clock) {
        TimerMetric timer = getMetric(name, TimerMetric.class);
        if (timer == null) {
            timer = setMetric(name, TimerMetric.class, new TimerMetric(new MultiThreadTimer(clock)));
        }
        return timer;
    }

    static synchronized Metrics.Rate getRate(String name, Metrics.Counter count, Clock clock) {
        Metrics.Rate rate = getMetric(name, Metrics.Rate.class);
        if (rate == null) {
            rate = setMetric(name, Metrics.Rate.class, new Metrics.Rate(count, clock));
        }
        return rate;
    }

    public static <T extends Metric> T getMetric(String name, Class<T> mClass) {
        final MetricSet<T> metricSet = Utils.cast(metricSets.get(mClass));
        if (metricSet != null) {
            final T metric = metricSet.metrics.get(name);
            if (metric != null) {
                return metric;
            }
        }
        return null;
    }

    public static <T extends Metric> T setMetric(String name, Class<T> mClass, T metric) {
        MetricSet<T> metricSet = Utils.cast(metricSets.get(mClass));
        if (metricSet == null) {
            metricSet = new MetricSet<T>(mClass);
            metricSets.put(mClass, metricSet);
        }
        metricSet.metrics.put(name, metric);
        return metric;
    }

    /**
     * Resets of all the currently registered metrics.
     */
    public static synchronized void reset() {
        for (MetricSet<? extends Metric> metricSet : metricSets.values()) {
            for (Metric metric : metricSet.metrics.values()) {
                metric.reset();
            }
        }
    }

    /**
     * This method prints a report of all the metrics that have been created during this
     * execution run.
     * @param stream the print stream to which to print the report
     */
    public static synchronized void report(PrintStream stream) {
        final Map<String, Metric> allMetrics = new HashMap<String, Metric>();
        for (MetricSet<? extends Metric> metricSet : metricSets.values()) {
            allMetrics.putAll(metricSet.metrics);
        }

        Map.Entry<String, Metric>[] array = Utils.cast(new Map.Entry[allMetrics.size()]);
        array = allMetrics.entrySet().toArray(array);
        Arrays.sort(array, new GlobalMetrics.EntryComparator());
        for (Map.Entry<String, Metric> entry : array) {
            if (entry.getKey().length() > Metrics.longestMetricName) {
                Metrics.longestMetricName = entry.getKey().length();
            }
        }
        for (Map.Entry<String, Metric> entry : array) {
            entry.getValue().report(entry.getKey(), stream);
        }
        stream.flush();
    }

}

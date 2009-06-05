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
package com.sun.max.bench;

import com.sun.max.lang.*;
import com.sun.max.profile.*;
import com.sun.max.program.option.*;

/**
 * This class implements a simple benchmarking harness for Maxine. It can be run as a Java program that accepts a list
 * of fully-qualified Java class names at the command line. Those classes must implement the
 * {@link java.lang.Runnable Runnable} interface/ The framework will then instantiate each class and run it (potentially
 * multiple times), and record the peformance metrics for each run.
 *
 * @author Ben L. Titzer
 */
public class Benchmark {

    private static final OptionSet _optionSet = new OptionSet(false);

    private static final Option<Integer> _runCount = _optionSet.newIntegerOption("runs", 1,
                    "This option specifies the number of times to run the benchmark.");
    private static final Option<Integer> _verbose = _optionSet.newIntegerOption("verbose", 0,
                    "Specifies the verbosity level of the benchmarking harness.");
    private static final Option<Boolean> _discardWarmupRun = _optionSet.newBooleanOption("discard-warmup", false,
                    "This option specifies whether the first run (the warmup run) will be discarded.");
    private static final Option<Boolean> _discardMinAndMax = _optionSet.newBooleanOption("discard-min-max", false,
                    "This option specifies whether the framework will discard the minimum and maximum runs.");
    private static final Option<Boolean> _simpleName = _optionSet.newBooleanOption("simple-name", true,
                    "This option causes the simple name of each benchmark class to be reported, as opposed "
                    + "to the fully qualified Java class name.");
    private static final Option<Boolean> _globalMetrics = _optionSet.newBooleanOption("global-metrics", false,
                    "This option enables reporting of all global metrics gathered by the system during the benchmarks.");

    public static void main(String[] a) {
        String[] args = a;
        long loadTime = System.nanoTime();
        _optionSet.parseArguments(args);
        args = _optionSet.getArguments();
        final Runnable[] benchmarks = getBenchmarks(args);
        loadTime = System.nanoTime() - loadTime;
        runBenchmarks(benchmarks, _runCount.getValue());
        Metrics.report(System.out, "Loadtime", "0", "benchmarks", Strings.fixedDouble(loadTime / 1000000000.0d, 9), "seconds");
    }

    public static void runBenchmarks(final Runnable[] benchmarks, int runCount) {
        for (Runnable benchmark : benchmarks) {
            if (benchmark != null) {
                report(benchmark, runBenchmark(benchmark, runCount));
            }
        }
    }

    private static void report(Runnable benchmark, long[] nanos) {
        int minIndex = -1;
        int maxIndex = -1;
        final int start = _discardWarmupRun.getValue() ? 1 : 0;
        if (_discardMinAndMax.getValue()) {
            for (int i = start; i < nanos.length; i++) {
                if (minIndex < 0 || nanos[i] < nanos[minIndex]) {
                    minIndex = i;
                }
                if (maxIndex < 0 || nanos[i] > nanos[maxIndex]) {
                    maxIndex = i;
                }
            }
        }
        long total = 0;
        int count = 0;
        for (int i = 0; i < nanos.length; i++) {
            if (i < start || i == minIndex || i == maxIndex) {
                // this run will not be averaged
                outputRun(benchmark, String.valueOf(i), nanos[i], false);
            } else {
                // this run will be averaged
                total += nanos[i];
                count++;
                outputRun(benchmark, String.valueOf(i), nanos[i], true);
            }
        }
        outputRun(benchmark, "average", total / (double) count, true);
    }

    private static void printJustified(String s, int width) {
        System.out.print(s);
        final int diff = width - s.length();
        System.out.print(Strings.space(diff));
    }

    private static void outputRun(Runnable benchmark, String run, double nanos, boolean averaged) {
        final String benchName = getBenchmarkName(benchmark);
        final String runTime = averaged ? "Runtime" : "Runtime*";
        Metrics.report(System.out, runTime, run, benchName, Strings.fixedDouble(nanos / 1000000000, 9), "seconds");
    }

    private static String getBenchmarkName(Runnable benchmark) {
        String name = _simpleName.getValue() ? benchmark.getClass().getSimpleName() : benchmark.getClass().getName();
        try {
            if (benchmark.getClass().getMethod("toString").getDeclaringClass() == benchmark.getClass()) {
                // the class defines its own "toString()" method
                name = benchmark.toString();
            }
            // the class defines a "toString()" method
            return benchmark.toString();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return name;
    }

    private static long[] runBenchmark(Runnable runnable, int count) {
        final long[] runNanos = new long[count];
        for (int i = 0; i < count; i++) {
            if (_verbose.getValue() > 0) {
                System.out.println("Running " + getBenchmarkName(runnable) + " (run " + i + ")...");
            }
            final long start = System.nanoTime();
            try {
                runnable.run();
            } catch (Throwable t) {
                // do nothing.
            }
            runNanos[i] = System.nanoTime() - start;
        }
        return runNanos;
    }

    private static Runnable[] getBenchmarks(String[] args) {
        final Runnable[] runnables = new Runnable[args.length];
        for (int i = 0; i < args.length; i++) {
            try {
                final int idx = args[i].lastIndexOf('$');
                if (idx != -1) {
                    /* field access in form foo.bar$baz */
                    final String classname = args[i].substring(0, idx);
                    final String fieldname = args[i].substring(idx + 1);
                    System.out.println("class=" + classname + "  field=" + fieldname);
                    final Class javaClass = Class.forName(classname);
                    runnables[i] = (Runnable) javaClass.getDeclaredField(fieldname).get(javaClass);
                } else {
                    final Class javaClass = Class.forName(args[i]);
                    runnables[i] = (Runnable) javaClass.newInstance();
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ClassCastException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        return runnables;
    }
}

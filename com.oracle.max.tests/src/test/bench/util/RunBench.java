/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package test.bench.util;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.max.program.*;

/**
 * A framework for running (micro) benchmarks that factors out timing and startup issues, leaving the benchmark writer
 * to concentrate on the essentials. The framework is designed to run under the Maxine testing framework, thus allowing
 * several benchmarks to be executed in succession. However, it is equally easy to run a benchmarks as a stand-alone
 * program by providing a {@code main} method and invoking the {@link #runTest} method. This allows controlling
 * properties to be set on the command line and then invokes the test method expected by the test harness.
 * <p>
 * The framework assumes that the benchmark will be compiled by the optimising compiler at VM image build time and
 * included in the test image, along with this class. A simple way to ensure that is for the benchmark to subclass this
 * class and include the actual benchmark as a nested class of that class.
 * <p>
 * The benchmark proper is encapsulated as a subclass of {@link MicroBenchmark}, with the actual benchmark defined by
 * the {@link MicroBenchmark#run} method. For truly micro (nano) benchmarks the overhead of invoking the {@code run}
 * method (and any setup code, e.g. taking a lock), can dominate the timings, so a second instance of
 * {@link MicroBenchmark} can be provided that captures that. This <i>encapsulating<i> benchmark is run first and the
 * timings are subtracted from the full benchmark run timings. For benchmarks that need per-run setup/shutdown steps the
 * {@link MicroBenchmark#prerun} and {@link MicroBenchmark#postrun} methods can be implemented. The
 * {@link MicroBenchmark#run} is required to return a result to prevent a compiler from optimizing the method body away.
 * For convenience the {@link MicroBenchmark#defaultResult} value can be used where appropriate, but it is the
 * benchmark writers responsibility to ensure that the compiler will not optimize away the essential code to be
 * measured. Unexpectedly fast runs should, therefore, be investigated carefully.
 *
 * The benchmark is executed a given number of times, with a default of {@value #DEFAULT_LOOP_COUNT}. This can be
 * changed at run-time by setting the property {@value #LOOP_COUNT_PROPERTY}.  A warm-up phase is also included, by default 10% of the
 * loopcount. This can be changed by setting the property {@value #WARMUP_COUNT_PROPERTY} to the number of warm-up
 * iterations. No timings are collected for the warm-up phase.
 * <p>
 * Some benchmarks, especially multi-threaded ones or those with a lot of setup (i.e., on the fringe of being a micro benchmark)
 * may prefer to perform multiple iterations within the {@link Bench#run} method itself. The only support for this via is the property
 * {@value RUN_ITER_PROPERTY}, which is read and stored in the {@link #runIterCount} variable.
 * <p>
 * The non-encapsulating, non-warm-up runs can be traced by setting the property {@value #TRACE_PROPERTY}.
 *
 * The non-encapsulating, non-warm-up runs can be saved to a file by setting the property {@value #FILE_PROPERTY}
 * to a pathname that will be used as the base name for the results. E.g., setting the property to /tmp/results would
 * produce a series of files, /tmp/results-Tn-Em and /tmp/results-Tn-Rm, where n is the thread id and m is the run id.
 * The E suffix is used to identify the encapsulating benchmark data and the R suffix for the actual run data.
 * <p>
 * Once an instance of the subclass of {@code RunBench} has been created, the benchmark may be run by invoking
 * {@link #runBench}.
 *
 * Results are reported to the standard output by default at the end of the run. This can be disabled by setting the
 * property {@value #NO_REPORT_PROPERTY}. If disabled, the caller can later invoke methods, such as
 * {@link #elapsedTime} to get the relevant information, and report it in a custom manner, or analyze the data
 * offfline having also set {@value #FILE_PROPERTY}.
 * <p>
 * {@link #runBench} returns {@code true} if the benchmark completed successfully and {@code false} if an exception was
 * thrown.
 * <p>
 * Note that the standard reporting mechanism first averages the results of the encapsulating benchmark, then averages
 * the results of the benchmark proper, and then subtracts the two. It is therefore possible for nano-benchmarks to report negative
 * values if some out of band value causes the encapsulating benchmark average to be high.
 * <p>
 * Removing outliers. By default the top and bottom X% of the results are removed before the average, median and stddev are computed.
 * The outlier percentage can be changed by setting the property {@value RunBench#OUTLIER_PROPERTY} to the percentage required.
 * N.B. The data output to the files or returned by {@link #elapsedTimes(int)} is raw, i,e., does not have the outliers removed.
 * <p>
 * Multi-thread support is built into the framework by way of the {@value #DEFAULT_THREAD_COUNT} property. By default the
 * number of threads is set to one but can be changed via this property. Each thread follows the same sequence and shares the
 * same {@link Microbenchmark} instance. Results are reported separately for each thread. There is no explicit support for per thread
 * state; if that is required, use {@link ThreadLocal}.
 *
 */
public class RunBench {

    /**
     * The actual benchmark must be a subclass of this class.
     *
     */
    public abstract static class MicroBenchmark {
        protected long defaultResult;

        /**
         * Any per-run setup needed prior to the run.
         * @throws Exception
         */
        public void prerun() throws Exception {
        }

        /**
         * Run one iteration of the benchmark.
         *
         * @return some value that will prevent the compiler from optimizing away the body of the benchmark
         */
        public abstract long run() throws Exception;

        /**
         * Any per-run shutdown needed.
         * @throws Exception
         */
        public void postrun() throws Exception {
        }
    }

    /**
     * Base class that has no encapsulating code, which is the common case.
     */
    private static class EmptyEncap extends MicroBenchmark {
        @Override
        public long run() {
            return defaultResult;
        }
    }

    public enum RunType {
        ENCAP("E"),
        ACTUAL("R");
        private String fileChar;
        RunType(String fileChar) {
            this.fileChar = fileChar;
        }
    }

    private static int warmupCount = -1;
    private static boolean report = true;
    private static boolean trace;

    public static final int DEFAULT_LOOP_COUNT = 100;
    public static final int DEFAULT_THREAD_COUNT = 1;
    public static final int DEFAULT_RUN_ITER_COUNT = 100000;
    public static final int DEFAULT_OUTLIER_PERCENT = 1;

    public static final String LOOP_COUNT_PROPERTY = "test.bench.loopcount";
    public static final String RUN_ITER_COUNT_PROPERTY = "test.bench.runitercount";
    public static final String WARMUP_COUNT_PROPERTY = "test.bench.warmupcount";
    public static final String THREAD_COUNT_PROPERTY = "test.bench.threadcount";
    public static final String FILE_PROPERTY = "test.bench.file";
    public static final String TRACE_PROPERTY = "test.bench.trace";
    public static final String NO_REPORT_PROPERTY = "test.bench.noreport";
    public static final String OUTLIER_PROPERTY = "test.bench.outlier";
    private static final MicroBenchmark emptyEncap = new EmptyEncap();
    private static String fileNameBase;
    private static int fileNameIndex;

    private static int loopCount = DEFAULT_LOOP_COUNT;
    private static int threadCount = DEFAULT_THREAD_COUNT;
    private static int outlierPercent = DEFAULT_OUTLIER_PERCENT;
    /**
     * This is used in benchmarks that want to iterate in the {@link Bench#run} method.
     */
    private static long runIterCount = DEFAULT_RUN_ITER_COUNT;

    private final MicroBenchmark bench;
    private final MicroBenchmark encapBench;
    private ThreadRunner[] runners;
    private Barrier startGate;
    private Barrier encapGate;

    /**
     * Check if any control properties are set.
     */
    public static void getBenchProperties() {
        final String lps = System.getProperty(LOOP_COUNT_PROPERTY);
        final String wps = System.getProperty(WARMUP_COUNT_PROPERTY);
        final String ips = System.getProperty(RUN_ITER_COUNT_PROPERTY);
        final String tps = System.getProperty(THREAD_COUNT_PROPERTY);
        final String rps = System.getProperty(NO_REPORT_PROPERTY);
        final String ops = System.getProperty(OUTLIER_PROPERTY);
        try {
            if (lps != null) {
                loopCount = Integer.parseInt(lps);
            }
            if (wps != null) {
                warmupCount = Integer.parseInt(wps);
            }
            if (ips != null) {
                runIterCount = Long.parseLong(rps);
            }
            if (tps != null) {
                threadCount = Integer.parseInt(tps);
            }
            if (ops != null) {
                outlierPercent = Integer.parseInt(ops);
            }
            if (rps != null) {
                report = false;
            }
        } catch (NumberFormatException ex) {
            throw ProgramError.unexpected("test.bench.loopcount " + lps + " did not parse");
        }
        trace = System.getProperty(TRACE_PROPERTY) != null;
        fileNameBase = System.getProperty(FILE_PROPERTY);
    }

    /**
     * Create an instance that will run {@code bench}.
     */
    protected RunBench(MicroBenchmark bench) {
        this(bench, null);
    }

    /**
     * Create an instance that will run {@code bench}. {@code encap} should be a variant that just contains any
     * encapsulating code that is necessary for the benchmark. For example, setting up a {@code synchronized} block to
     * test {@link Object#wait}. This is used to factor out code that should not be measured.
     */
    protected RunBench(MicroBenchmark bench, MicroBenchmark encap) {
        getBenchProperties();
        this.bench = bench;
        encapBench = encap == null ? emptyEncap : encap;
        final long now = System.nanoTime();
        bench.defaultResult = now;
        encapBench.defaultResult = now;
    }

    private void zeroArray(long[] values) {
        for (int i = 0; i < values.length; i++) {
            values[i] = 0;
        }
    }

    /**
     * Get the array of result times for the encapsulated variant of the benchmark for given thread.
     * @param threadId value in range {@code 0 .. threadCount - 1}
     * @return array of result times in nanoseconds for given thread
     */
    public long[] encapElapsedTimes(int threadId) {
        return runners[threadId].encapElapsed;
    }

    /**
     * Get the array of result times for the the benchmark for given thread.
     * @param threadId value in range {@code 0 .. threadCount - 1}
     * @return array of result times in nanoseconds
     */
    public long[] elapsedTimes(int threadId) {
        return runners[threadId].elapsed;
    }

    public static long loopCount() {
        return loopCount;
    }

    public static long threadCount() {
        return threadCount;
    }

    public static long runIterCount() {
        return runIterCount;
    }

    /**
     * Run the benchmark for the given number of iterations. in the given number of threads, optionally reporting
     * results.
     *
     * @param loopCount the number of iterations
     * @param report report the results iff true
     * @return {@code false} if benchmark threw an exception, {@code true} otherwise.
     */
    public boolean runBench() {
        if (warmupCount < 0) {
            warmupCount = loopCount < 10 ? 1 : loopCount / 10;
        }
        startGate = new Barrier(threadCount);
        encapGate = new Barrier(threadCount);

        runners = new ThreadRunner[threadCount];
        for (int i = 0; i < threadCount; i++) {
            runners[i] = new ThreadRunner(i);
            runners[i].start();
        }
        for (ThreadRunner runner : runners) {
            try {
                runner.join();
            } catch (InterruptedException ex) {
            }
        }
        for (ThreadRunner runner : runners) {
            runner.doReport();
        }
        if (fileNameBase != null) {
            for (ThreadRunner runner : runners) {
                runner.doFileOutput();
            }
        }
        return true;
    }

    /**
     * Runs the benchmark in one thread.
     *
     */
    private class ThreadRunner extends Thread {

        private long[] elapsed;
        private long[] encapElapsed;
        private boolean reporting;
        private int warmUpIndex;
        private int runIndex;
        private int myId;

        ThreadRunner(int i) {
            setName("BenchmarkRunner-" + i);
            this.reporting = report;
            this.myId = i;
        }

        @Override
        public void run() {
            elapsed = new long[loopCount];
            zeroArray(elapsed);
            encapElapsed = new long[loopCount];
            zeroArray(encapElapsed);
            try {
                startGate.waitForRelease();
                // Do an encapsulating run to factor out overheads
                doRun(loopCount, encapBench, encapElapsed);
                // Now the real thing
                encapGate.waitForRelease();
                doRun(loopCount, bench, elapsed);
            } catch (Throwable t) {
                final String where = runIndex < 0 ? "warmup iteration " + warmUpIndex : "run iteration " + runIndex;
                println("benchmark threw " + t + " on " + where);
                t.printStackTrace();
                reporting = false;
            }
        }

        private void doRun(long loopCount, MicroBenchmark bench, long[] timings) throws Throwable {
            // do warmup and discard results
            runIndex = -1;
            for (warmUpIndex = 0; warmUpIndex < warmupCount; warmUpIndex++) {
                bench.prerun();
                bench.run();
                bench.postrun();
                if (trace && bench != encapBench) {
                    println("warm up run " + warmUpIndex);
                }
            }
            for (runIndex = 0; runIndex < loopCount; runIndex++) {
                bench.prerun();
                final long start = System.nanoTime();
                bench.run();
                timings[runIndex] = System.nanoTime() - start;
                bench.postrun();
                if (trace && bench != encapBench) {
                    println("run " + runIndex + " elapsed " + timings[runIndex]);
                }
            }
        }

        void doReport() {
            if (reporting) {
                final long[] copyEncapElapsed = Arrays.copyOf(encapElapsed, encapElapsed.length);
                final long[] copyElapsed = Arrays.copyOf(elapsed, elapsed.length);
                final SubArray encapSubArray = removeOutliers(copyEncapElapsed);
                final SubArray elapsedSubArray = removeOutliers(copyElapsed);
                report(encapSubArray, elapsedSubArray, this.getName());
            }
        }

        void doFileOutput() {
            fileOutput(RunType.ENCAP, myId, encapElapsed);
            fileOutput(RunType.ACTUAL, myId, elapsed);
            fileNameIndex++;
        }

        void println(String m) {
            System.out.println(this.getName() + ": " + m);
        }

    }

    public static class SubArray {
        long[] values;
        int lwb;
        int upb;

        public SubArray(long[] values, int lwb, int upb) {
            this.values = values;
            this.lwb = lwb;
            this.upb = upb;
        }

        public int length() {
            return upb - lwb;
        }
    }

    public static void report(SubArray encapSubArray, SubArray elapsedSubArray, String threadName) {
        final double avgEncapElapsed = average(encapSubArray);
        final double avgElapsed = average(elapsedSubArray);
        final double benchElapsed = avgElapsed - avgEncapElapsed;
        final double avgElapsedStdDev = stddev(elapsedSubArray, avgElapsed);
        final long[] minMaxArr = maxmin(elapsedSubArray);
        System.out.println("Benchmark results (nanoseconds per iteration) for thread " + threadName);
        System.out.println("  loopcount: " + loopCount + ", warmupcount: " + warmupCount);
        System.out.format("  average overhead: %.3f, median overhead: %.3f\n", avgEncapElapsed, median(encapSubArray, true));
        System.out.format("  average elapsed: %.3f, median elapsed: %.3f, \n", avgElapsed, median(elapsedSubArray, true));
        System.out.format("  average elapsed minus overhead: %.3f\n", benchElapsed);
        System.out.format("  stddev: %.3f, max: %d, min: %d\n", avgElapsedStdDev, minMaxArr[1], minMaxArr[0]);
        System.out.format("  operations/ms: %.3f\n", 1000000 / benchElapsed);
    }

    /**
     * Remove outliers by sorting {@code timings} and removing {@link #outlierPercent} largest values.
     * @param timings
     */
    public static SubArray removeOutliers(long[] timings) {
        Arrays.sort(timings);
        final int length = timings.length;
        final int n = (length * outlierPercent) / 100;
        return new SubArray(timings, n, length - n);
    }

    /**
     * Get the standard deviation of the values in {@code timings}.
     * @param timings array of timing values
     * @return the standard deviation
     */
    public double stddev(long[] timings) {
        final SubArray array = new SubArray(timings, 0, timings.length);
        return stddev(array, average(array));
    }

    private static double stddev(SubArray array, double avg) {
        double res = 0;
        for (int i = array.lwb; i < array.upb; i++) {
            res += Math.pow(array.values[i] - avg, 2);
        }
        return Math.sqrt(res / array.values.length);
    }

    /**
     * Get the max and min vales in the given array of timings.
     * @param timings array of timing values
     * @return array {@code m} of length 2; {@code m[0] == min; m[1] == max}
     */
    public static long[] maxmin(SubArray array) {
        long[] minMaxArr = new long[]{Long.MAX_VALUE, Long.MIN_VALUE};
        for (int i = array.lwb; i < array.upb; i++) {
            final long val = array.values[i];
            if (val < minMaxArr[0]) {
                minMaxArr[0] = val;
            }
            if (val > minMaxArr[1]) {
                minMaxArr[1] = val;
            }
        }
        return minMaxArr;
    }

    private void fileOutput(RunType runType, int threadId, long[] timings) {
        PrintWriter bs = null;
        try {
            bs = new PrintWriter(new BufferedWriter(new FileWriter(fileOutputName(fileNameBase, runType, threadId, fileNameIndex))));
            for (int i = 0; i < timings.length; i++) {
                bs.println(timings[i]);
            }
        } catch (IOException ex) {
            System.out.print(ex);
        } finally {
            if (bs != null) {
                bs.close();
            }
        }
    }

    public static String fileOutputName(String baseName, RunType runType, int threadId, int seqno) {
        return baseName + "-" + "T" + threadId + "-" + runType.fileChar + seqno;
    }

    /**
     * Return the median value of the values in the given array.
     * @param array
     * @return the median value
     */
    public double median(long[] array) {
        return median(new SubArray(array, 0, array.length), false);
    }

    /**
     * Return the median value the values in the given {@link SubArray}.
     * @param array
     * @return the median value
     */
    public static double median(SubArray array, boolean isSorted) {
        long[] values = array.values;
        final int length = array.length();
        if (length == 1) {
            return values[array.lwb];
        } else if (length == 2) {
            return (values[array.lwb] + values[array.lwb + 1]) / 2.0D;
        }
        SubArray sortedArray = array;
        if (!isSorted) {
            final long[] sortedValues = new long[length];
            sortedArray = new SubArray(sortedValues, 0, length);
            System.arraycopy(values, array.lwb, sortedValues, 0, values.length);
            Arrays.sort(sortedValues);
        }
        values = sortedArray.values;

        final int lendiv2 = sortedArray.lwb + values.length / 2;
        if ((length & 1) != 0) {
            return values[lendiv2];
        }
        return (values[lendiv2] + values[lendiv2 - 1]) / 2.0D;
    }

    /**
     * Return the average of the values in the given array.
     * @param values
     * @return the average value
     */
    public double average(long[] array) {
        return average(new SubArray(array, 0, array.length));
    }

    /**
     * Return the average of the values in the given array.
     * @param values
     * @return the average value
     */
    public static double average(SubArray array) {
        long result = 0;
        for (int i = array.lwb; i < array.upb; i++) {
            result += array.values[i];
        }
        return (double) result / (double) loopCount;
    }

    /**
     * Gets a given system property with check for existence.
     * @param name property name
     * @param mustExist if {@code true} then the property must exsit
     * @return property value or {@code null} if not found and {@code mustExist == false}
     * @throws IllegalArgumentException if {@code mustExist == true} and the property is not found.
     */
    public static String getProperty(String name, boolean mustExist) {
        final String result = System.getProperty(name);
        if (result == null && mustExist) {
            System.err.println("property " + name + " must be set");
            throw new IllegalArgumentException();
        }
        return result;
    }

    /**
     * Get a required system property.
     * @param name property name
     * @return value of property
     */
    public static String getRequiredProperty(String name) {
        return getProperty(name, true);
    }

    /**
     * Support for running a benchmark as main program.
     * Accepts command line options in lieu of properties and some additional arguments to control the run:
     * <pre>
     * -runs n                run the entire benchmark (test method) "n" times. This results in "n" reported measurements.
     * -loopcount n        equivalent to -Dtest.bench.loopcount=n
     * -warmupcount n  equivalent to -Dtest.bench.warmupcount=n
     * -threadcount n     equivalent to -Dtest.bench.threadcount=n
     * -runitercount n     equivalent to -Dtest.bench.runitercount=n
     *
     * Other args are passed to the test method (based on its signature). The signature {@code test(int i)}
     * is given a default value of zero if the argument is omitted.
     * </pre>
     *
     * @param testClass
     * @param args
     */
    public static void runTest(Class<? extends RunBench> testClass, String[] args) {
        try {
            final Method[] methods = testClass.getDeclaredMethods();
            Method testMethod = null;
            for (Method method : methods) {
                if (method.getName().equals("test")) {
                    testMethod = method;
                }
            }
            if (testMethod == null) {
                throw new IllegalArgumentException("no test method found in class: " + testClass.getSimpleName());
            }

            int runs = 1;
            // index of first argument that could be an argument to the test method
            int testArgIndex = args.length;
            // Checkstyle: stop modified control variable check
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.startsWith("-")) {
                    arg = arg.substring(1);
                }
                int matchValue;
                if ((matchValue = argMatch(arg, "runs")) >= 0) {
                    runs = Integer.parseInt(matchValue == 0 ? args[++i] : arg.substring(matchValue));
                } else if ((matchValue = argMatch(arg, "loopcount")) >= 0) {
                    System.setProperty(LOOP_COUNT_PROPERTY, matchValue == 0 ? args[++i] : arg.substring(matchValue));
                } else if ((matchValue = argMatch(arg, "warmupcount")) >= 0) {
                    System.setProperty(WARMUP_COUNT_PROPERTY, matchValue == 0 ? args[++i] : arg.substring(matchValue));
                } else if ((matchValue = argMatch(arg, "threadcount")) >= 0) {
                    System.setProperty(THREAD_COUNT_PROPERTY, matchValue == 0 ? args[++i] : arg.substring(matchValue));
                } else if ((matchValue = argMatch(arg, "runitercount")) >= 0) {
                    System.setProperty(RUN_ITER_COUNT_PROPERTY, matchValue == 0 ? args[++i] : arg.substring(matchValue));
                } else if (arg.equals("noreport")) {
                    System.setProperty(NO_REPORT_PROPERTY, "");
                } else {
                    testArgIndex = i;
                    break;
                }
            }
            // Checkstyle: resume modified control variable check
            Class<?>[] params = testMethod.getParameterTypes();
            if (args.length - testArgIndex < params.length) {
                if (!(params.length == 1 && params[0] == int.class)) {
                    throw new IllegalArgumentException("insufficient arguments for test method");
                }
            }
            Object[] testArgs = new Object[params.length];
            for (int i = 0; i < params.length; i++) {
                final Class<?> param = params[i];
                if (param == String.class) {
                    testArgs[i] = args[testArgIndex + i];
                } else if (param == int.class) {
                    testArgs[i] = testArgIndex + i < args.length ? Integer.parseInt(args[testArgIndex + i]) : 0;
                } else if (param == long.class) {
                    testArgs[i] = Long.parseLong(args[testArgIndex + i]);
                } else {
                    throw new IllegalArgumentException("unsupported test argument type: " + param.getSimpleName());
                }
            }
            for (int i = 0; i < runs; i++) {
                testMethod.invoke(null, testArgs);
            }
        } catch (Exception ex) {
            System.err.println(ex);
            ex.printStackTrace();
        }
    }

    /**
     * Argument match, either "key" or "key=value".
     * @param arg argument to match
     * @param key key we are looking for
     * @return < 0 if not matched, 0 if matched exactly, otherwise index of start of value
     */
    private static int argMatch(final String arg, final String key) {
        if (arg.startsWith(key)) {
            final int index = arg.indexOf("=");
            if (index > 0) {
                return index + 1;
            }
            return 0;
        }
        return -1;
    }

}

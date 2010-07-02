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
package test.bench.util;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;

import com.sun.max.program.*;


/**
 * A framework for running (micro) benchmarks that factors out timing and startup issues, leaving the benchmark writer
 * to concentrate on the essentials. The framework is designed to run under the Maxine testing framework, thus allowing
 * several benchmarks to be executed in succession.
 * <p>
 * The framework assumes that the benchmark will be compiled by the optimising compiler at VM image build time and
 * included in the image, along with this class. A simple way to ensure that is for the benchmark to subclass this class
 * and include the actual benchmark as a nested class of that class.
 * <p>
 * The benchmark proper is encapsulated as a {@link MicroBenchmark}, with the actual benchmark defined by the
 * {@link MicroBenchmark#run} method. For truly micro (nano) benchmarks the overhead of invoking the {@code run} method
 * (and any setup code, e.g. taking a lock), can dominate the timings, so a second instance of {@link MicroBenchmark}
 * can be provided that captures that. This <i>encapsulating<i> benchmark is run first and the timings are subtracted
 * from the full benchmark run timings.
 *
 * The benchmark is executed a given number of times, with a default of {@value #DEFAULT_LOOP_COUNT}. This can be
 * changed at run-time by setting the property {@value #LOOP_COUNT_PROPERTY}. It is also possible to explicitly pass in
 * the loop count at benchmark execution time if required. A warm-up phase can also be included by setting the property
 * {@value #WARMUP_COUNT_PROPERTY} to the number of warm-up iterations. No timings are collected for the warm-up phase.
 * <p>
 * The non-encapsulating, non-warm-up runs can be traced by setting the property {@value #TRACE_PROPERTY}.
 *
 * The non-encapsulating, non-warm-up runs can be saved to a file by setting the property {@value #FILE_PROPERTY}
 * <p>
 * Once an instance of the subclass of {@code RunBench} has been created, the benchmark may be run by invoking
 * {@link #runBench}. There are three variants of {@link #runBench}; the first two use the {@value #DEFAULT_LOOP_COUNT
 * default loop count}, and differ in whether result reporting defaults to {@code true} or is specified as an argument.
 * The third variant allows the default loop count to be overridden. If result reporting is enabled the results are
 * reported to the standard output on completion. If it is disabled the caller can later invoke methods, such as
 * {@link #elapsedTime} to get the relevant information, and report it in a custom manner.
 * <p>
 * {@link #runBench} returns {@code true} if the benchmark completed successfully and {@code false} if an exception was
 * thrown.
 *
 * @author Mick Jordan
 */
public class RunBench {

    /*
     * The benchmark must implement this interface.
     */
    protected static interface MicroBenchmark {

        void prerun() throws Exception;

        /*
         * Run one iteration of the benchmark.
         *
         * @param warmup {@code true} if this is a warm up run
         */
        void run(boolean warmup) throws Exception;

        void postrun() throws Exception;
    }

    /**Provides empty implementations of pre and post run for classes that dont need it.
     *
     * @author Puneeet Lakhina
     */
    protected abstract static class AbstractMicroBenchmark implements MicroBenchmark {

        @Override
        public void prerun() throws Exception {
        }

        @Override
        public void postrun() throws Exception {
        }
    }

    /**
     * Base class that has no encapsulating code, which is the common case.
     */
    private static class EmptyEncap extends AbstractMicroBenchmark {

        public void run(boolean warmup) {
        }
    }

    private final MicroBenchmark bench;
    private final MicroBenchmark encapBench;
    private long[] elapsed;
    private long[] encapElapsed;
    private int loopCount;
    private static int warmupCount;
    private static boolean trace;
    private static final int DEFAULT_LOOP_COUNT = 100;
    private static final int DEFAULT_WARMUP_COUNT = 10;
    private static int defaultLoopCount = DEFAULT_LOOP_COUNT;
    private static final String LOOP_COUNT_PROPERTY = "test.bench.loopcount";
    private static final String WARMUP_COUNT_PROPERTY = "test.bench.warmupcount";
    private static final String DISPLAY_INDIVIDUAL_PROPERTY = "test.bench.displayall";
    private static final String FILE_PROPERTY = "test.bench.file";
    private static final String TRACE_PROPERTY = "test.bench.trace";
    private static final MicroBenchmark emptyEncap = new EmptyEncap();
    private static String fileNameBase;
    private static int fileNameIndex;

    /**
     * Check if any control properties are set.
     */
    private static void getBenchProperties() {
        final String lps = System.getProperty(LOOP_COUNT_PROPERTY);
        final String wps = System.getProperty(WARMUP_COUNT_PROPERTY);
        try {
            if (lps != null) {
                defaultLoopCount = Integer.parseInt(lps);
            }
            if (wps != null) {
                warmupCount = Integer.parseInt(wps);
            }
        } catch (NumberFormatException ex) {
            ProgramError.unexpected("test.bench.loopcount " + lps + " did not parse");
        }
        trace = System.getProperty(TRACE_PROPERTY) != null;
        fileNameBase = System.getProperty(FILE_PROPERTY);
    }

    /*
     * Create an instance that will run {@code bench}.
     */
    protected RunBench(MicroBenchmark bench) {
        this(bench, null);
    }

    /*
     * Create an instance that will run {@code bench}. {@code encap} should be a variant that just contains any
     * encapsulating code that is necessary for the benchmark. For example, setting up a {@code synchronized} block to
     * test {@link Object#wait}. This is used to factor out timing of code that should not be measured.
     */
    protected RunBench(MicroBenchmark bench, MicroBenchmark encap) {
        getBenchProperties();
        this.bench = bench;
        encapBench = encap == null ? emptyEncap : encap;
    }

    private void zeroArray(long[] values) {
        for (int i = 0; i < values.length; i++) {
            values[i] = 0;
        }
    }

    /**
     * Gets the number of iterations associated with this benchmark. Note that is this called before {@link #runBench}
     * in invoked, it will return the {@link #defaultLoopCount default loop count}, otherwise it will return either the
     * value passed to {@link #runBench} (if any), or {@link #defaultLoopCount default loop count}.
     *
     * @return the number of iterations
     */
    public int loopCount() {
        return loopCount == 0 ? defaultLoopCount : loopCount;
    }

    public long[] elapsedTime() {
        return elapsed;
    }

    /**
     * Run the benchmark for the default number of iterations and report the results to the standard output.
     *
     * @return {@code false} if benchmark threw an exception, {@code true} otherwise.
     */
    public boolean runBench() {
        return runBench(defaultLoopCount, true);
    }

    /*
     * Run the benchmark for the default number of iterations.
     *
     * @param report report the results iff true
     *
     * @return {@code false} if benchmark threw an exception, {@code true} otherwise.
     */
    public boolean runBench(boolean report) {
        return runBench(defaultLoopCount, report);
    }

    /*
     * Run the benchmark for the given number of iterations.
     *
     * @param loopCount the number of iterations
     *
     * @param report report the results iff true
     *
     * @return {@code false} if benchmark threw an exception, {@code true} otherwise.
     */
    public boolean runBench(int loopCount, boolean report) {
        this.loopCount = loopCount;
        elapsed = new long[loopCount];
        zeroArray(elapsed);
        encapElapsed = new long[loopCount];
        zeroArray(encapElapsed);
        try {
            // Do an encapsulating run to factor out overheads
            doRun(loopCount, encapBench, encapElapsed);
            // Now the real thing
            doRun(loopCount, bench, elapsed);
        } catch (Throwable t) {
            System.err.println("benchmark threw " + t);
            report = false;
        }
        if (report) {
            final long avgEncapElapsed = average(encapElapsed);
            final long avgElapsed = average(elapsed);
            final long benchElapsed = avgElapsed - avgEncapElapsed;

            System.out.println("Benchmark results (nanoseconds)");
            System.out.println("  loopcount: " + loopCount);
            System.out.println("  averge overhead per iteration: " + avgEncapElapsed + ", median overhead per iteration " + median(encapElapsed));
            System.out.println("  average elapsed per iteration: " + avgElapsed + ", median elapsed per iteration " + median(elapsed));
            System.out.println("  average elapsed minus overhead: " + benchElapsed);

            if (getProperty(DISPLAY_INDIVIDUAL_PROPERTY, false) != null) {
                displayElapsed();
            }
        }
        if (fileNameBase != null) {
            fileOutput("E", encapElapsed);
            fileOutput("R", elapsed);
            fileNameIndex++;
        }
        this.loopCount = 0;
        return true;
    }

    private void fileOutput(String type, long[] timings) {
        PrintWriter bs = null;
        try {
            bs = new PrintWriter(new BufferedWriter(new FileWriter(fileNameBase + "-" + type + fileNameIndex)));
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

    private void doRun(long loopCount, MicroBenchmark bench, long[] timings) throws Throwable {
        // do warmup and discard results
        for (int i = 0; i < warmupCount; i++) {
            bench.prerun();
            bench.run(true);
            bench.postrun();
            if (trace && bench != encapBench) {
                System.out.println("warm up run " + i);
            }
        }
        for (int i = 0; i < loopCount; i++) {
            bench.prerun();
            final long start = System.nanoTime();
            bench.run(false);
            timings[i] = System.nanoTime() - start;
            bench.postrun();
            if (trace && bench != encapBench) {
                System.out.println("run " + i + " elapsed " + timings[i]);
            }
        }
    }

    public void displayElapsed() {
        System.out.print("  elapsed values: ");
        for (int i = 0; i < loopCount; i++) {
            if (i % 8 == 0) {
                System.out.println();
            }
            System.out.print("  ");
            System.out.print(elapsed[i]);
        }
        System.out.println();
    }

    public long median(long[] array) {
        final long[] values = new long[array.length];
        System.arraycopy(array, 0, values, 0, array.length);
        Arrays.sort(values);

        final int lendiv2 = values.length / 2;
        return (lendiv2 & 1) != 0 ? values[lendiv2] : (values[lendiv2] + values[lendiv2 - 1]) / 2;
    }

    public long average(long[] values) {
        long result = 0;
        for (int i = 0; i < loopCount; i++) {
            result += values[i];
        }
        return result / loopCount;
    }

    public static String getProperty(String name, boolean mustExist) {
        final String result = System.getProperty(name);
        if (result == null && mustExist) {
            System.err.println("property " + name + " must be set");
            throw new IllegalArgumentException();
        }
        return result;
    }

    public static String getRequiredProperty(String name) {
        return getProperty(name, true);
    }

    public static void runTest(Class<? extends RunBench> testClass, String[] args) {
        try {
            Method testMethod = testClass.getDeclaredMethod("test", new Class<?>[]{});
            int runs = 1;
            // Checkstyle: stop modified control variable check
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("r")) {
                    runs = Integer.parseInt(args[++i]);
                }
            }
            // Checkstyle: resume modified control variable check
            Object[] noArgs = new Object[0];
            for (int i = 0; i < runs; i++) {
                testMethod.invoke(null, noArgs);
            }
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }

}

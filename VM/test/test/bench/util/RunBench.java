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

import com.sun.max.program.*;

/**
 * A micro-benchmark (to be run under the testing framework) must subclass
 * this utility class, which guarantees that this code will be included in the vm image.
 * The micro-benchmark is encapsulated as a LoopRunnable, with the actual benchmark
 * defined by the run method, The loop that encapsulates the actual operation being
 * tested is defined by the runBareLoop method, that is used to approximate the loop overhead
 * which is removed from the reported result. The benchmark is run in a new thread.
 */
public class RunBench implements Runnable {

    /*
     * The benchmark must implement this interface.
     */
    protected static interface LoopRunnable {
        /*
         * Run the benchmark for @see count iterations.
         * @param count number of iterations
         */
        void run(long count) throws Exception;

        /*
         * Run the empty loop that encapsulates the benchmark for @see count iterations.
         * @param count number of iterations
         */
        void runBareLoop(long count);
    }

    private final LoopRunnable bench;
    private long elapsed;
    private long loopCount;
    private static long defaultLoopCount = 1000000;
    private static final String LOOPCOUNT_PROPERTY = "test.bench.loopcount";

    private static void getLoopCount() {
        final String lps = System.getProperty(LOOPCOUNT_PROPERTY);
        if (lps != null) {
            try {
                defaultLoopCount = Long.parseLong(lps);
            }  catch (NumberFormatException ex) {
                ProgramError.unexpected("test.bench.loopcount " + lps + " did not parse");
            }
        }
    }

    protected RunBench(LoopRunnable bench) {
        getLoopCount();
        this.bench = bench;
    }

    protected long loopCount() {
        return loopCount;
    }

    /*
     * Run the benchmark for the default number of iterations.
     * @param report  report the results iff true
     * @return the elapsed time in nanoseconds
     */
    public long runBench(boolean report) throws InterruptedException {
        return runBench(defaultLoopCount, report);
    }

    /*
     * Run the benchmark for the given number of iterations.
     * @param loopCount the number of iterations
     * @param report  report the results iff true
     * @return the elapsed time in nanoseconds
     */
    public long runBench(long loopCount, boolean report) throws InterruptedException {
        this.loopCount = loopCount;
        final long start = System.nanoTime();
        bench.runBareLoop(loopCount);
        final long loopTime = System.nanoTime() - start;

        final Thread thread = new Thread(this);
        thread.start();
        thread.join();
        if (report) {
            final long count = loopCount();
            final long benchElapsed = elapsed - loopTime;
            Trace.line(0, "Benchmark results (nanoseconds)");
            Trace.line(0, "  loopcount: " + count + ", loop overhead " + loopTime);
            Trace.line(0, "  elapsed: " + elapsed +  ", corrected elapsed: " + benchElapsed);
            final long x = benchElapsed / count;
            final long y = benchElapsed % count;
             // loopCount assumed to be a power of 10 for simplicity.
            Trace.line(0, "  nanoseconds per iteration: " + x + "." + y);
        }
        return elapsed;
    }

    public void run() {
        final long startTime = System.nanoTime();
        try {
            bench.run(loopCount);
        } catch (Exception ex) {
            Trace.line(0, "benchmark threw " + ex);
        }
        elapsed = System.nanoTime() - startTime;
    }

}

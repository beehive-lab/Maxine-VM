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
/*
 * @Harness: java
 * @Runs: (1, 1) = true;
 */
package test.bench.threads;

import com.sun.max.annotate.*;
import com.sun.max.program.*;

import test.bench.util.*;

/**
 * This benchmark runs a given number of threads, each of which
 * increments a private counter, for a given number of milliseconds
 * specified by the loopCount value. It reports the sum of the counts.
 * It essentially provides a baseline for Thread_counter2/3.
 *
 * The number of threads defaults the the value passed into the
 * test method (default 1) but can be changed by setting
 * -Dtest.bench.threads.threadcount=N at runtime.
 *
 * The result count should scale linearly with the number of threads.
 *
 * @author Mick Jordan
 */
public class Thread_counter01  extends RunBench {

    private static int threadCount;

    protected Thread_counter01(LoopRunnable bench) {
        super(bench);
    }

    public static boolean test(int i, int t) throws InterruptedException {
        threadCount = t;
        final String threadCountProp = System.getProperty("test.bench.threads.threadcount");
        if (threadCountProp != null) {
            threadCount = Integer.parseInt(threadCountProp);
        }
        final Bench bench = new Bench();
        new Thread_counter01(bench).runBench(false);
        Trace.line(0, "  count: " + bench.totalCount());
        return true;
    }

    static class Bench implements LoopRunnable, Runnable {

        private static /*RunBench.*/Timer benchTimer;
        private long count;
        private static long totalCount;

        public void run(long loopCount) throws InterruptedException {
            totalCount = 0;
            final Thread[] threads = new Thread[threadCount];
            final Bench[] benches = new Bench[threadCount];
            for (int i = 0; i < threadCount; i++) {
                benches[i] = new Bench();
                threads[i] = new Thread(benches[i]);
            }
            benchTimer = new /*RunBench.*/Timer(loopCount);
            new Thread(benchTimer).start();
            for (int i = 0; i < threadCount; i++) {
                threads[i].start();
            }
            for (int i = 0; i < threadCount; i++) {
                threads[i].join();
                totalCount += benches[i].count;
            }
        }

        long totalCount() {
            return totalCount;
        }

        public void run() {
            while (benchTimer.running()) {
                count++;
            }
        }

        public void runBareLoop(long loopCount) {
        }
    }

    public static class Timer implements Runnable {
        private long runtime;
        private boolean done = false;
        public Timer(long runtime) {
            this.runtime = runtime;
        }

        @INLINE
        public boolean running() {
            return !done;
        }

        public void run() {
            try {
                Thread.sleep(runtime);
            } catch (InterruptedException ex) {

            }
            done = true;
        }
    }

}

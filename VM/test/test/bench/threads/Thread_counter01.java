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
 * @Runs: (1, 1000) = true;
 */
package test.bench.threads;

import test.bench.util.*;

/**
 * This benchmark runs a given number of threads, each of which
 * increments a private counter, for a given number of milliseconds
 * specified by the {@code runTime} value. It reports the sum of the counts.
 * It essentially provides a baseline for Thread_counter2.
 *
 * The number of threads defaults the the value passed into the
 * test method (default 1).
 *
 * The result count should scale linearly with the number of threads.
 *
 * @author Mick Jordan
 */
public class Thread_counter01  extends RunBench {

    protected static int threadCount;
    protected static int runTime;
    protected static volatile boolean done;
    protected static long[] totalCounts;

    protected Thread_counter01(MicroBenchmark bench) {
        super(bench, null);
        totalCounts = new long[loopCount()];
    }

    public static boolean test(int t, int r) {
        threadCount = t;
        runTime = r;
        final boolean result = new Thread_counter01(new Bench(new OpenRunnerFactory())).runBench(true);
        displayCounts();
        return result;
    }

    protected static void displayCounts() {
        for (int i = 0; i < totalCounts.length; i++) {
            System.out.println("  total count for run " + i + ": " + totalCounts[i]);
        }
    }

    static class Bench implements MicroBenchmark {

        private static Timer benchTimer;
        private RunnerFactory runnerFactory;
        private int runCount = 0;

        Bench(RunnerFactory runnerFactory) {
            this.runnerFactory = runnerFactory;
        }

        public void run(boolean warmup) {
            done = false;
            final Thread[] threads = new Thread[threadCount];
            final CountingRunner[] benches = new CountingRunner[threadCount];
            for (int i = 0; i < threadCount; i++) {
                benches[i] = runnerFactory.createRunner(threadCount);
                threads[i] = new Thread(benches[i]);
            }
            benchTimer = new Timer(runTime);
            new Thread(benchTimer).start();
            for (int i = 0; i < threadCount; i++) {
                threads[i].start();
            }
            for (int i = 0; i < threadCount; i++) {
                try {
                    threads[i].join();
                } catch (InterruptedException ex) {
                }
                if (!warmup) {
                    totalCounts[runCount] = benches[i].getCount();
                }
            }
            if (!warmup) {
                runCount++;
            }
        }

    }

    abstract static class RunnerFactory {
        abstract CountingRunner createRunner(int threadCount);
    }

    abstract static class CountingRunner implements Runnable {
        protected long count;
        public abstract void run();
        public long getCount() {
            return count;
        }
    }

    static class OpenRunner extends CountingRunner {
        @Override
        public void run() {
            while (!done) {
                count++;
            }
        }
    }

    static class OpenRunnerFactory extends RunnerFactory {
        @Override
        CountingRunner createRunner(int threadCount) {
            return new OpenRunner();
        }
    }

    static class Timer implements Runnable {
        private long runtime;
        public Timer(long runtime) {
            this.runtime = runtime;
        }

        public void run() {
            try {
                Thread.sleep(runtime);
            } catch (InterruptedException ex) {

            }
            done = true;
        }
    }

    // for running stand-alone
    public static void main(String[] args) {
        test(2, 1000);
    }
}

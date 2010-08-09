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
 * @Runs: (1, 100000) = true;
 */
package test.bench.threads;

import test.bench.util.*;

/**
 * This benchmark runs a given number of threads, each of which
 * decrements a private counter until it reaches zero.
 * It provides a baseline for {@link Thread_counter3}.
 *
 * The number of threads defaults to the value passed into the
 * test method (default 1).
 *
 * On an SMP the time should scale (down) linearly with the number of threads.
 *
 *
 * @author Mick Jordan
 */
public class Thread_counter01  extends RunBench {

    protected static final int DEFAULT_COUNT = 100000;
    protected static int threadCount;
    protected static long countDown;
    protected static Barrier startBarrier;

    protected Thread_counter01(MicroBenchmark bench, int t, long c) {
        super(bench, new Bench(new EmptyRunnerFactory()));
        threadCount = t;
        countDown = c;
    }

    public static boolean test(int t, int c) {
        final boolean result = new Thread_counter01(new Bench(new OpenRunnerFactory()), t, c).runBench(true);
        return result;
    }

    static class Bench extends MicroBenchmark {

        private RunnerFactory runnerFactory;
        private int runCount = 0;
        private Thread[] threads;
        private volatile int started;

        Bench(RunnerFactory runnerFactory) {
            this.runnerFactory = runnerFactory;
        }

        @Override
        public void prerun() {
            threads = new Thread[threadCount];
            final CountingRunner[] counter = new CountingRunner[threadCount];
            for (int i = 0; i < threadCount; i++) {
                counter[i] = runnerFactory.createRunner(countDown, threadCount);
                threads[i] = new Thread(counter[i]);
            }
            startBarrier = new Barrier(threadCount + 1);
            // start the threads which will wait at the barrier
            for (int i = 0; i < threadCount; i++) {
                threads[i].start();
            }
        }

        @Override
        public long run() {
            startBarrier.waitForRelease();
            for (int i = 0; i < threadCount; i++) {
                try {
                    threads[i].join();
                } catch (InterruptedException ex) {
                }
            }
            return defaultResult;
        }
    }

    abstract static class RunnerFactory {
        abstract CountingRunner createRunner(long count, int threadCount);
    }

    abstract static class CountingRunner implements Runnable {
        public abstract void run();
    }

    static class OpenRunner extends CountingRunner {
        private long count;
        OpenRunner(long count) {
            this.count = count;
        }
        @Override
        public void run() {
            startBarrier.waitForRelease();
            while (count > 0) {
                count--;
            }
        }
    }

    static class EmptyRunner extends CountingRunner {
        @Override
        public void run() {
            startBarrier.waitForRelease();
        }
    }

    static class OpenRunnerFactory extends RunnerFactory {
        @Override
        CountingRunner createRunner(long count, int threadCount) {
            return new OpenRunner(count / threadCount);
        }
    }

    static class EmptyRunnerFactory extends RunnerFactory {
        @Override
        CountingRunner createRunner(long count, int threadCount) {
            return new EmptyRunner();
        }
    }

    // for running stand-alone
    public static void main(String[] args) {
        if (args.length == 0) {
            test(1, DEFAULT_COUNT);
        } else {
            test(Integer.parseInt(args[0]), DEFAULT_COUNT);
        }
    }
}

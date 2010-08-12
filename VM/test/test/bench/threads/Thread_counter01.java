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
 * It provides a baseline for {@link Thread_counter02}.
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

    protected Thread_counter01(MicroBenchmark bench, int t, long c) {
        super(bench, new Bench(new ThreadCounter.EmptyRunnerFactory(), t, c));
    }

    public static boolean test(int t, int c) {
        final boolean result = new Thread_counter01(new Bench(new OpenRunnerFactory(), t, c), t, c).runBench(true);
        return result;
    }

    static class Bench extends ThreadCounter.Bench {

        Bench(ThreadCounter.RunnerFactory runnerFactory, int threadCount, long countDown) {
            super(runnerFactory, threadCount, countDown);
        }

    }

    static class OpenRunner extends ThreadCounter.BaseRunner implements Runnable {
        OpenRunner(int threadCount, long count) {
            super(threadCount, count);
        }

        @Override
        public void run() {
            ThreadCounter.startBarrier.waitForRelease();
            while (count > 0) {
                count--;
            }
        }
    }

    static class OpenRunnerFactory extends ThreadCounter.RunnerFactory {
        @Override
        public Runnable createRunner(int threadCount, long count) {
            return new OpenRunner(threadCount, count);
        }
    }

    // for running stand-alone
    public static void main(String[] args) {
        RunBench.runTest(Thread_counter01.class, args);
    }
}

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
 * @Runs: (1, 10000) = true;
 */
package test.bench.threads;

import test.bench.util.*;

/**
 *
 * This code is a variant of Thread_counter01 where the counter is shared,
 * therefore we expect contention and not linear scaling.
 *
 * @author Mick Jordan
 */
public class Thread_counter02  extends Thread_counter01 {

    private static Object lock = new Object();
    private static long sharedCount;

    protected Thread_counter02(MicroBenchmark bench, int t, long c)  {
        super(bench, t, c);
    }

    static class SharedLockedRunnerFactory extends ThreadCounter.RunnerFactory {
        @Override
        public Runnable createRunner(int threadCount, long count) {
            sharedCount = count;
            return new SharedLockedRunner();
        }
    }

    static class SharedLockedRunner implements Runnable {

        @Override
        public void run() {
            ThreadCounter.startBarrier.waitForRelease();
            while (true) {
                synchronized (lock) {
                    if (sharedCount == 0) {
                        return;
                    }
                    sharedCount--;
                }
            }
        }
    }


    public static boolean test(int t, int c) {
        final boolean result = new Thread_counter02(new Bench(new SharedLockedRunnerFactory(), t, c), t, c).runBench(true);
        return result;
    }

    // for running stand-alone
    public static void main(String[] args) {
        RunBench.runTest(Thread_counter02.class, args);
    }

}

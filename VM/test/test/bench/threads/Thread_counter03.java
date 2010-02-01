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

import com.sun.max.program.*;

import test.bench.util.*;

/**
 * This benchmark runs a given number of threads, each of which
 * increments a global counter, for a given number of milliseconds
 * specified by the loopCount value. It reports the final value of the count.
 *
 * The counter is, of course, protected by a lock.
 *
 * The number of threads defaults the the value passed into the
 * test method (default 1) but can be changed by setting
 * -Dtest.bench.threads.threadcount=N at runtime.
 *
 * Unlike Thread_counter02, the counter is heavily contended for,
 * so we do not expect linear scaling.
 *
 * @author Mick Jordan
 */
public class Thread_counter03  extends RunBench {

    private static volatile long count;
    private static Object object = new Object();
    private static int threadCount;

    protected Thread_counter03(LoopRunnable bench) {
        super(bench);
    }

    public static boolean test(int i, int t) throws InterruptedException {
        threadCount = t;
        count = 0;
        final String threadCountProp = System.getProperty("test.bench.threads.threadcount");
        if (threadCountProp != null) {
            threadCount = Integer.parseInt(threadCountProp);
        }
        new Thread_counter03(new Bench()).runBench(false);
        Trace.line(0, "  count: " + count);
        return true;
    }

    static class Bench implements LoopRunnable, Runnable {

        private static Thread_counter01.Timer benchTimer;

        public void run(long loopCount) throws InterruptedException {
            final Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(new Bench());
            }
            benchTimer = new Thread_counter01.Timer(loopCount);
            new Thread(benchTimer).start();
            for (int i = 0; i < threadCount; i++) {
                threads[i].start();
            }
            for (int i = 0; i < threadCount; i++) {
                threads[i].join();
            }
        }

        public void run() {
            while (benchTimer.running()) {
                synchronized (object) {
                    count++;
                }
            }
        }

        public void runBareLoop(long loopCount) {
        }
    }
}

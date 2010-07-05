/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
 * @Runs: 0 = true
 */
package test.bench.threads;

import test.bench.util.*;

/**
 * Tests scalability of JNI invocations. This test is designed to show the performance of the
 * MFence vs. CAS synchronization implementation.
 *
 * This is a rather complex micro-benchmark in that each run creates worker and gc threads and
 * those threads then do some work that is controlled by additional values that may be specified
 * by additional system properties:
 * <ul>
 * <li>{@value THREADS_PROPERTY}: the number of worker threads, default {@value DEFAULT_THREADS}
 * <li>{@value JNI_CALLS_PROPERTY}: the number of caalls to make to the worker JNI method, default {@value DEFAULT_JNICALLS}
 * <li>{@value WORKLOAD_PROPERTY}: a value designating the amount of work the JNI call does, default {@value DEFAULT_WORKLOAD}
 * <li>{@value GC_PROPERTY}: if set, create a GC thread
 * <li>{@value GC_INTERVAL_PROPERTY}: interval between garbage collections, default {@value DEFAULT_GC_INTERVAL}
 * </ul>
 *
 * @author Hannes Payer
 * @author Mick Jordan
 */

public class JNI_invocations extends RunBench {

    protected JNI_invocations() {
        super(new Bench());

    }

    public static boolean test() {
        return new JNI_invocations().runBench(true);
    }

    /**
     * A native method that does work proportional to {@code workload}.
     */
    private static native long nativework(long workload);

    static class Bench extends AbstractMicroBenchmark {
        private static Barrier barrier1;
        private static Barrier barrier2;
        private static final int DEFAULT_THREADS = 2;
        private static final int DEFAULT_JNICALLS = 1000;
        private static final int DEFAULT_WORKLOAD = 10;
        private static final int DEFAULT_GC_INTERVAL = 100;
        private static int nrThreads;
        private static int nrJNICalls;
        private static int workload;
        private static int gcInterval;
        private static boolean gc;
        private static boolean trace = System.getProperty("trace") != null;
        private static final String THREADS_PROPERTY = "test.bench.threads.jni.threads";
        private static final String JNICALLS_PROPERTY = "test.bench.threads.jni.calls";
        private static final String WORKLOAD_PROPERTY = "test.bench.threads.jni.work";
        private static final String GC_PROPERTY = "test.bench.threads.jni.gc";
        private static final String GC_INTERVAL_PROPERTY = "test.bench.threads.jni.gc.interval";

        Bench() {
            nrThreads = getIntProperty(THREADS_PROPERTY, DEFAULT_THREADS);
            nrJNICalls = getIntProperty(JNICALLS_PROPERTY, DEFAULT_JNICALLS);
            workload = getIntProperty(WORKLOAD_PROPERTY, DEFAULT_WORKLOAD);
            gc = System.getProperty(GC_PROPERTY) != null;
            if (gc) {
                gcInterval = getIntProperty(GC_INTERVAL_PROPERTY, DEFAULT_GC_INTERVAL);
            }
            if (gc) {
                barrier1 = new Barrier(nrThreads + 2);
                barrier2 = new Barrier(nrThreads + 2);
            } else {
                barrier1 = new Barrier(nrThreads + 1);
                barrier2 = new Barrier(nrThreads + 1);
            }
        }

        private static int getIntProperty(String propName, int defaultValue) {
            int result = defaultValue;
            final String propValue = System.getProperty(propName);
            if (propValue != null) {
                result = Integer.parseInt(propValue);
            }
            return result;
        }

        @Override
        public void prerun() {
            for (int i = 0; i < nrThreads; i++) {
                new Thread(new AllocationThread(nrJNICalls)).start();
            }

            if (gc) {
                new Thread(new GCInvokeThread(nrThreads), "GCInvokeThread").start();
            }
            // All the above threads will now wait at barrier1 until the actual benchmark thread calls the run method

        }

        @Override
        public void run(boolean warmup) {
            // this will release all threads
            barrier1.waitForRelease();
            // wait for everyone to finish
            barrier2.waitForRelease();
        }

        @Override
        public void postrun() {
            barrier1.reset();
            barrier2.reset();
        }

        public static class AllocationThread implements Runnable{

            private int nrJNIcalls;

            public AllocationThread(int nrJNICalls) {
                this.nrJNIcalls = nrJNICalls;
            }

            public void run() {
                barrier1.waitForRelease();
                for (int i = 0; i < nrJNIcalls; i++) {
                    nativework(workload);
                }
                barrier2.waitForRelease();
            }
        }

        public static class GCInvokeThread implements Runnable{
            private int nrJNIThreads;

            public GCInvokeThread(int nrJNIThreads) {
                this.nrJNIThreads = nrJNIThreads;
            }

            public void run() {
                barrier1.waitForRelease();
                while (barrier2.getThreadCount() != nrJNIThreads + 1) {
                    System.gc();
                    try {
                        Thread.sleep(gcInterval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                barrier2.waitForRelease();
            }
        }

    }

    /**
     * All threads wait at barrier until last thread arrives.
     */
    static class Barrier {
        private int threads;
        private int threadCount = 0;

        public Barrier(int threads) {
            this.threads = threads;
        }

        public synchronized void reset() {
            threadCount = 0;
        }

        /**
         * Get number of threads that have reached the barrier.
         * @return number of threads that have reached the barrier
         */
        public int getThreadCount() {
            return threadCount;
        }

        public synchronized void waitForRelease() {
            try {
                threadCount++;
                if (threadCount == threads) {
                    notifyAll();
                } else {
                    while (threadCount < threads) {
                        wait();
                    }
                }
            } catch (InterruptedException ex) {
            }
        }
    }

    public static void main(String[] args) {
        RunBench.runTest(JNI_invocations.class, args);
    }


}

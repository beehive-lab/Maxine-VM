/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
        return new JNI_invocations().runBench();
    }

    /**
     * A native method that does work proportional to {@code workload}.
     */
    private static native long nativework(long workload);

    static class Bench extends MicroBenchmark {
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
            final int bc = nrThreads + 1 + (gc ? 1 : 0);
            barrier1 = new Barrier(bc);
            barrier2 = new Barrier(bc);

            if (gc) {
                new Thread(new GCInvokeThread(nrThreads), "GCInvokeThread").start();
            }
            // All the above threads will now wait at barrier1 until the actual benchmark thread calls the run method

        }

        @Override
        public long run() {
            // this will release all threads
            barrier1.waitForRelease();
            // wait for everyone to finish
            barrier2.waitForRelease();
            return defaultResult;
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

    public static void main(String[] args) {
        RunBench.runTest(JNI_invocations.class, args);
    }


}

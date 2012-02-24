/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package test.output;

/**
 * A heavily multi-threaded GC test stress testing GC Safepoint when many threads starts and go.
 */
public class GCTest8 {
    private static Object lock = new Object();
    static int numCompleted = 0;
    static int batchSize = 0;

    static int MAX_CONCURRENT_THREADS;
    static int MAX_TOTAL_THREADS;
    static {
        MAX_CONCURRENT_THREADS = Integer.parseInt(System.getProperty("gctest.thread.concurrent", "20"));
        MAX_TOTAL_THREADS = Integer.parseInt(System.getProperty("gctest.thread.total", "100"));
    }
    public static void main(String[] args) {
        int numStarted = 0;
        // Starts MAX_CONCURRENT_THREADS  threads.
        // Waits to drop down to 15 to starts new threads again.
        for (numStarted = 0; numStarted < MAX_CONCURRENT_THREADS;) {
            new HeapFiller(numStarted++).start();
        }

        batchSize = MAX_CONCURRENT_THREADS  / 4;
        if (batchSize == 0) {
            batchSize = 1;
        }
        final  int minThreadThreshold = MAX_CONCURRENT_THREADS - batchSize;

        while (numStarted < MAX_TOTAL_THREADS) {
            try {
                synchronized (lock) {
                    while ((numStarted - numCompleted) > minThreadThreshold) {
                        lock.wait();
                    }
                }
            } catch (InterruptedException e) {
            }
            // Get another batch started.
            System.out.println("starting fillers #" + numStarted + " to #" + (numStarted + batchSize));
            for (int i = 0; i < batchSize; i++) {
                new HeapFiller(numStarted++).start();
            }
        }
    }

    private static void notifyFillerDone() {
        synchronized (lock) {
            numCompleted++;
            if (numCompleted % batchSize == 0) {
                lock.notifyAll();
            }
        }
    }

    private static class HeapFiller extends Thread {

        HeapFiller(int id) {
            super("HeapFiller #" + id);
        }

        @Override
        public void run() {
            for (int i = 0; i < 10; i++) {
                createGarbage();
            }
            notifyFillerDone();
        }
    }

    /**
     * Create various kinds of garbage.
     */
    private static void createGarbage() {
        final int max = 2000;
        final Object[] objects = new Object[max];
        for (int i = 0; i < max; i++) {
            final int[] ints = new int[i];
            objects[i / 5] = ints;
        }

        for (int i = 0; i < max / 4; i++) {
            objects[i * 4] = new Object();
            objects[i * 4 + 1] = objects;
        }

        for (int i = 0; i < max / 3; i++) {
            final GCTest8 garbageTest = new GCTest8();
            objects[i * 3] = garbageTest;
        }
    }
}

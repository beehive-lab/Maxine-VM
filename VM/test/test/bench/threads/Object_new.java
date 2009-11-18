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
package test.bench.threads;

/**
 * Test for the scalability of heap allocation. This test is designed to
 * show the performance benefits of thread local allocation buffers (TLABs).
 *
 * @author Hannes Payer
 */
public class Object_new {

    static class Barrier {
        private int threads;
        private int threadCount = 0;

        public Barrier(int threads) {
            this.threads = threads;
        }

        public synchronized void reset() {
            threadCount = 0;
        }

        public synchronized void waitForRelease() throws InterruptedException {
            threadCount++;
            if (threadCount == threads) {
                notifyAll();
            } else {
                while (threadCount < threads) {
                    wait();
                }
            }
        }
    }

    protected static Barrier barrier1;
    protected static Barrier barrier2;
    protected static int nrThreads;
    protected static int allocSize;
    protected static int nrAllocs;
    protected static boolean trace = System.getProperty("trace") != null;

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("ERROR: call java Simple <nr threads> <size> <nr allocs>");
            System.exit(1);
        }

        nrThreads = Integer.parseInt(args[0]);
        allocSize = Integer.parseInt(args[1]);
        nrAllocs = Integer.parseInt(args[2]);

        barrier1 = new Barrier(nrThreads + 1);
        barrier2 = new Barrier(nrThreads + 1);

        for (int i = 0; i < nrThreads; i++) {
            new Thread(new AllocationThread(allocSize, nrAllocs, i)).start();
        }
        long start = 0;
        try {
            barrier1.waitForRelease();
            start = System.currentTimeMillis();
            barrier2.waitForRelease();
        } catch (InterruptedException e) { }

        final long benchtime = System.currentTimeMillis() - start;
        System.out.println(benchtime); // + " ms");

        /*System.out.println("Simple Allocator Benchmark Result (nr threads: " + nrThreads +
                            "; size: " + allocSize + "; nr allocs: " + nrAllocs +
                            "; time: " + benchtime + " ns");*/
    }

    public static class AllocationThread implements Runnable{
        private int size;
        private int nrAllocations;
        private int threadId;

        public AllocationThread(int size, int nrAllocations, int threadId) {
            this.size = size;
            this.nrAllocations = nrAllocations;
            this.threadId = threadId;
        }

        public void run() {
            try {
                barrier1.waitForRelease();
            } catch (InterruptedException e) { }
            // Only have one thread report progress. It should be fairly
            // representative of over all progress.
            if (trace && threadId == 0) {
                for (int i = 0; i < nrAllocations; i++) {
                    final byte[] tmp = new byte[size];
                    tmp[0] = 1;
                    if (i % 10000 == 0) {
                        System.out.println(i);
                    }
                }
            } else {
                for (int i = 0; i < nrAllocations; i++) {
                    final byte[] tmp = new byte[size];
                    tmp[0] = 1;
                }
            }
            //System.out.println("Thread " + threadId + " done");
            try {
                barrier2.waitForRelease();
            } catch (InterruptedException e) { }
        }
    }
}

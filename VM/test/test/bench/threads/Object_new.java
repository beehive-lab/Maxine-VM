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
 * @Runs: (1, 10000, 16) = true;
 */
package test.bench.threads;

import test.bench.util.*;

/**
 * Test for the scalability of heap allocation. This test is designed to
 * show the performance benefits of thread local allocation buffers (TLABs).
 *
 * @author Hannes Payer
 * @author Mick Jordan
 */
public class Object_new  extends RunBench {

    protected static final int DEFAULT_NT = 2;
    protected static final int DEFAULT_NA = 10000;
    protected static final int DEFAULT_AS = 16;

    protected Object_new(int nt, int na, int as) {
        super(new Bench(nt, na, as), new EncapBench(nt, na, as));
    }

    public static boolean test(int nt, int na, int as) {
        return new Object_new(nt, na, as).runBench();
    }

    static class Bench extends MicroBenchmark {
        protected Barrier barrier1;
        protected Barrier barrier2;
        protected int allocSize;
        protected int nrAllocs;
        protected int nrThreads;

        Bench(int nt, int na, int as) {
            nrThreads = nt;
            nrAllocs = na / nt;
            allocSize = as;
        }

        @Override
        public void prerun() {
            barrier1 = new Barrier(nrThreads + 1);
            barrier2 = new Barrier(nrThreads + 1);
            createThreads();
        }

        protected void createThreads() {
            for (int i = 0; i < nrThreads; i++) {
                new Thread(new AllocationThread(), "Alloc-" + i).start();
            }
        }

        @Override
        public long run() {
            barrier1.waitForRelease();
            barrier2.waitForRelease();
            return defaultResult;
        }

        class AllocationThread implements Runnable{
            public void run() {
                barrier1.waitForRelease();
                for (int i = 0; i < nrAllocs; i++) {
                    final byte[] tmp = new byte[allocSize];
                    tmp[0] = 1;
                }
                barrier2.waitForRelease();
            }
        }
    }

    static class EncapBench extends Bench {
        EncapBench(int nt, int na, int as) {
            super(nt, na, as);
        }

        @Override
        public void createThreads() {
            for (int i = 0; i < nrThreads; i++) {
                new Thread(new EncapThread(), "Encap-" + i).start();
            }
        }

        class EncapThread implements Runnable {
            public void run() {
                barrier1.waitForRelease();
                barrier2.waitForRelease();
            }
        }

    }

    public static void main(String[] args) {
        int nt = DEFAULT_NT;
        int na = DEFAULT_NA;
        int as = DEFAULT_AS;
        if (args.length > 0) {
            nt = Integer.parseInt(args[0]);
            if (args.length > 1) {
                na = Integer.parseInt(args[1]);
                if (args.length > 2) {
                    as = Integer.parseInt(args[2]);
                }
            }
        }
        test(nt, na, as);
    }

}

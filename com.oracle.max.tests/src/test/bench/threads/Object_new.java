/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @Runs: (1, 10000, 16) = true;
 */
package test.bench.threads;

import test.bench.util.*;

/**
 * Test for the scalability of heap allocation. This test is designed to
 * show the performance benefits of thread local allocation buffers (TLABs).
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

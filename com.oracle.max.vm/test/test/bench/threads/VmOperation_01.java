/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @Runs: 1 = true
 */
package test.bench.threads;

import java.util.*;

import test.bench.util.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.VmOperation.Mode;
import com.sun.max.vm.thread.*;

/**
 * Benchmarks the time taken to perform a {@linkplain VmOperation VM operation}.
 */
public class VmOperation_01 extends RunBench {

    protected VmOperation_01(int n) {
        super(new Bench(n));
    }

    public static boolean test(int i) {
        return new VmOperation_01(i).runBench();
    }

    static class Bench extends MicroBenchmark {
        int numThreads;
        private Thread[] spinners;
        private volatile boolean done;
        private volatile int started;
        private VmOperation operation;
        private Barrier startGate;
        private Barrier endGate;

        Bench(int n) {
            numThreads = n;
            startGate = new Barrier(n + 1);
            endGate = new Barrier(n + 1);
        }

        @Override
        public void prerun() {
            done = false;
            started = 0;
            spinners = new Thread[numThreads];
            for (int s = 0; s < spinners.length; s++) {
                spinners[s] = new Spinner();
                spinners[s].start();
            }
            final HashSet<Thread> threads = new HashSet<Thread>(Arrays.asList(spinners));
            operation = new VmOperation("Test", null, Mode.Safepoint) {
                @Override
                protected void doThread(VmThread vmThread, Pointer ip, Pointer sp, Pointer fp) {
                    // all threads are stopped at this point, so we can tell then all to quit when they resume
                    done = true;
                }
                @Override
                protected boolean operateOnThread(com.sun.max.vm.thread.VmThread thread) {
                    return threads.contains(thread.javaThread());
                }
            };

            // Wait for all threads to start so that we only benchmark the time taken to freeze threads
            startGate.waitForRelease();
        }

        @Override
        public void postrun() {
            endGate.waitForRelease();
        }

        @Override
        public long run() {
            startGate.waitForRelease();
            operation.submit();
            return defaultResult;
        }

        class Spinner extends Thread {

            @Override
            public void run() {
                startGate.waitForRelease();
                @SuppressWarnings("unused")
                long count = 0;
                while (!done) {
                    count++;
                }
                endGate.waitForRelease();
            }
        }
    }

     // for running stand-alone
    public static void main(String[] args) {
        if (args.length == 0) {
            test(1);
        } else {
            test(Integer.parseInt(args[0]));
        }
    }
}

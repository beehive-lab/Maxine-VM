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
 * @Runs: 1 = true
 */
package test.bench.threads;

import java.util.*;

import test.bench.util.*;

import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.FreezeThreads.*;

/**
 * Benchmarks time taken to perform a {@linkplain FreezeThreads#run() thread freeze}.
 *
 * @author Mick Jordan
 * @author Doug Simon
 */
public class FreezeThreads_01 extends RunBench {

    protected FreezeThreads_01(int n) {
        super(new Bench(n));
    }

    public static boolean test(int i) {
        return new FreezeThreads_01(i).runBench(true);
    }

    static class Bench extends AbstractMicroBenchmark implements Runnable {
        int numThreads;
        private Thread[] spinners;
        private volatile boolean done;
        private volatile int started;
        private FreezeThreads freezeThreads;

        Bench(int n) {
            numThreads = n;
        }

        @Override
        public void prerun() {
            done = false;
            started = 0;
            spinners = new Thread[numThreads];
            for (int s = 0; s < spinners.length; s++) {
                spinners[s] = new Thread(this);
                spinners[s].start();
            }
            ThreadListPredicate predicate = new ThreadListPredicate(Arrays.asList(spinners));
            freezeThreads = new FreezeThreads("Test", predicate);

            // Wait for all threads to start so that we only benchmark the time taken to freeze threads
            while (started != numThreads) {
                Thread.yield();
            }
        }

        @Override
        public void postrun() {
            done = true;

            // Wait for all threads to stop so that they don't interfere with subsequent runs
            for (int s = 0; s < spinners.length; s++) {
                try {
                    spinners[s].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void run(boolean warmup) {
            freezeThreads.run();
        }

        public void run() {
            started++;
            long count = 0;
            while (!done) {
                count++;
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

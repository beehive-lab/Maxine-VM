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

import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;
import test.bench.util.*;

public class StopThreads_01 extends RunBench {

    protected StopThreads_01(int n) {
        super(new Bench(n));
    }

    public static boolean test(int i) {
        return new StopThreads_01(i).runBench(true);
    }

    static class Bench extends AbstractMicroBenchmark implements Runnable {
        int numThreads;
        private Thread[] spinners;
        private volatile boolean done;
        private volatile boolean started;
        private StopThreads.ByPredicate stopThreads;
        private static final StopThreads.ProcessProcedure processProcedure = new StopThreads.ProcessProcedure(new Processor());

        private static class Processor implements StopThreads.ThreadProcessor {
            public void processThread(Pointer threadLocals, Pointer instructionPointer, Pointer stackPointer, Pointer framePointer) {
            }
        }

        Bench(int n) {
            numThreads = n;
        }

        @Override
        public void prerun() {
            done = false;
            spinners = new Thread[numThreads];
            for (int s = 0; s < spinners.length; s++) {
                spinners[s] = new Thread(this);
                spinners[s].start();
            }
            stopThreads = new StopThreads.FromArray(spinners, processProcedure);
        }

        @Override
        public void postrun() {
            done = true;
        }

        public void run(boolean warmup) {
            while (!started) {
                Thread.yield();
            }
            stopThreads.process();
        }

        public void run() {
            started = true;
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

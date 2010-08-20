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
 * @Runs: 0 = true;
 */
package test.bench.threads;

import test.bench.util.*;
import static test.bench.threads.Thread_counter01.*;

/**
 *
 * This code is a variant of Thread_counter02 where the counter is shared.
 * Each thread has its own thread local, but the associated value is the
 * same for all threads.
 *
 * So all threads are contending on the same counter and we do not expect linear scaling.
 *
 * @author Mick Jordan
 */
public class Thread_counter02  extends RunBench {

    protected Thread_counter02() {
        super(new Bench());
    }

    public static boolean test(int i) {
        return new Thread_counter02().runBench();
    }

    static class Bench extends Thread_counter01.Bench {
        private Counter sharedCounter = new Counter(RunBench.runIterCount());

        @Override
        public void prerun() {
            setCounter(sharedCounter);
        }

        @Override
        public long run() {
            Counter counter = threadCounter.get();
            while (true) {
                synchronized (counter) {
                    if (counter.count == 0) {
                        break;
                    }
                    counter.count--;
                }
            }
            return defaultResult;
        }
    }

    // for running stand-alone
    public static void main(String[] args) {
        RunBench.runTest(Thread_counter02.class, args);
    }
}


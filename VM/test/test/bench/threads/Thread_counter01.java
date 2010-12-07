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

/**
 * This benchmark is intended to be run in multi-threaded mode. It measures
 * how long it takes to decrement the {@link RunBench#runIterCount iteration counter}.
 * Each thread is given a share of the count to work on.
 *
 * It provides a baseline for {@link Thread_counter02} which decrements a shared counter.
 *
 * On an SMP the time should scale (down) linearly with the number of threads.
 *
 *
 * @author Mick Jordan
 */
public class Thread_counter01  extends RunBench {

    protected Thread_counter01() {
        super(new Bench());
    }

    public static boolean test(int i) {
        return new Thread_counter01().runBench();
    }

    static class Counter {
        long count;
        Counter(long count) {
            this.count = count;
        }
    }

    static class Bench extends MicroBenchmark {
        protected ThreadLocal<Counter> threadCounter;

        @Override
        public void prerun() {
            setCounter(new Counter(RunBench.runIterCount() / RunBench.threadCount()));
        }

        protected void setCounter(final Counter counter) {
            threadCounter = new ThreadLocal<Counter>() {
                @Override
                public Counter initialValue() {
                    return counter;
                }
            };
        }

        @Override
        public long run() {
            Counter counter = threadCounter.get();
            while (counter.count > 0) {
                counter.count--;
            }
            return defaultResult;
        }
    }

    static class EncapBench extends Bench {
        @Override
        public long run() {
            Counter counter = threadCounter.get();
            return counter.count;
        }

    }

    // for running stand-alone
    public static void main(String[] args) {
        RunBench.runTest(Thread_counter01.class, args);
    }
}

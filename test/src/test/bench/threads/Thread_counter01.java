/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

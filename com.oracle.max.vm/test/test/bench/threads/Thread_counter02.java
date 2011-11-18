/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import test.bench.threads.Thread_counter01.Counter;
import test.bench.util.*;

/**
 *
 * This code is a variant of Thread_counter02 where the counter is shared.
 * Each thread has its own thread local, but the associated value is the
 * same for all threads.
 *
 * So all threads are contending on the same counter and we do not expect linear scaling.
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


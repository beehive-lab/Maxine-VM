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
 * @Runs: 0 = true
 */
package test.bench.threads;

import test.bench.util.*;

public class Object_wait01  extends RunBench {
    static int count = 0;
    static volatile boolean done;
    static final Object object = new Object();

    protected Object_wait01() {
        super(new Bench(), new EncapBench());
    }

    public static boolean test(int i) {
        new Thread(new Notifier()).start();
        final boolean result = new Object_wait01().runBench();
        done = true;
        return result;

    }

    static class Bench extends MicroBenchmark {

        @Override
        public long run() {
            synchronized (object) {
                try {
                    object.wait();
                } catch (InterruptedException ex) {
                }
                count++;
            }
            return defaultResult;
        }
    }

    static class EncapBench extends MicroBenchmark {

        @Override
        public long run() {
            synchronized (object) {
                count++;
            }
            return defaultResult;
        }
    }

    static class Notifier implements Runnable {
        public void run() {
            while (!done) {
                synchronized (object) {
                    object.notify();
                }
            }
        }
    }

    // for running stand-alone
    public static void main(String[] args) {
        test(0);
    }
}

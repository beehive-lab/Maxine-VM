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
 * @Runs: 2 = true;
 */
package test.interactive;

import com.sun.max.program.*;

public final class Thread_runmany01 {

    public static final boolean debug = false;
    public static volatile int runnerCount;

    public static boolean test(int i) throws InterruptedException {
        Thread[] runners = new Thread[i];
        runnerCount = i;
        for (int  j = 0; j < i; j++) {
            runners[j] = new Thread(new Runner(j));
            runners[j].start();
        }
        while (runnerCount > 0) {
            debug("sleeping");
            Thread.sleep(1000);
        }
        return true;
    }

    static class Runner implements Runnable {
        private int id;
        Runner(int id) {
            this.id = id;
        }

        public void run() {
            long startTime = System.currentTimeMillis();
            long now = startTime;
            long count = 0;
            debug("Runner " + id + " starting at " + startTime);
            while (now < startTime + 10000 + id * 2000) {
                count++;
                if (count % 10000000 == 0) {
                    now = System.currentTimeMillis();
                    debug("Runner " + id + " time now " + now);
                }
            }
            debug("Runner " + id + " finished");
            runnerCount--;
        }
    }

    private static void debug(String s) {
        if (debug) {
            Trace.stream().println(s);
        }
    }
}

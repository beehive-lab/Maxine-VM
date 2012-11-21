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
 * @Runs: 1000 = true;
 */
package test.interactive;

import com.sun.max.vm.*;

public final class Thread_sleepIdle01 {
    private static boolean running = true;

    private Thread_sleepIdle01() {
    }

    public static boolean test(int i) throws InterruptedException {
        new Thread(new IdleThread()).start();
        for (int t = 0; t < 10; t++) {
            Log.println("sleeping");
            Thread.sleep(i);
            Log.println("waking");
        }
        running = false;
        return true;
    }

    static class IdleThread implements Runnable {

        public void run() {
            long i = 0;
            while (running) {
                i++;
                if ((i % 10000000) == 0) {
                    Log.println("idling");
                }
            }
        }
    }
}

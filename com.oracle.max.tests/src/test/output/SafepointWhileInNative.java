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
package test.output;

public class SafepointWhileInNative {
    static final class Sleeper implements Runnable {
        boolean done;
        public void run() {
            System.out.println("Sleeper: sleeping...");
            synchronized (this) {
                // Notify that 'sleeper' has started
                notify();
            }
            while (!stopRequested()) {
                synchronized (this) {
                    // Go to sleep
                    try {
                        wait(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            System.out.println("Sleeper: woke up!");
        }
        synchronized boolean stopRequested() {
            return done;
        }
        synchronized void requestStop() {
            done = true;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        final Sleeper sleeper = new Sleeper();
        System.gc();
        final Thread sleeperThread = new Thread(sleeper, "Sleeper");

        synchronized (sleeper) {
            sleeperThread.start();
            // Wait until 'sleeper' has started
            sleeper.wait();
        }

        // Give 'sleeper' a chance to start sleeping
        System.out.println("Main: sleeping...");
        Thread.sleep(200);
        System.out.println("Main: woke up!");

        // GC while 'sleeper' is blocked on the monitor in native code
        System.out.println("GC start");
        System.gc();
        System.out.println("GC stop");

        sleeper.requestStop();
        sleeperThread.join();
    }
}

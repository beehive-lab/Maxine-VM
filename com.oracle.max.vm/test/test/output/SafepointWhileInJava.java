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
package test.output;

public class SafepointWhileInJava {
    private static final class Spinner implements Runnable {
        boolean done;
        int iterations;
        public void run() {
            System.out.println("Spinner: spinning...");
            final Object[] localRefs = new Object[1000];
            while (!stopRequested()) {
                frameWithReferences(localRefs);
                ++iterations;
            }
        }
        private Object frameWithReferences(final Object[] localRefs) {
            Object o = null;
            for (int i = 0; i != localRefs.length; ++i) {
                localRefs[i] = System.out;
                o = localRefs[i];
            }
            return o;
        }

        synchronized boolean stopRequested() {
            return done;
        }
        synchronized void requestStop() {
            done = true;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        final Spinner spinner = new Spinner();
        System.gc();
        final Thread spinnerThread = new Thread(spinner, "Spinner");
        spinnerThread.start();

        // Poll 'spinner' until it starts spinning
        while (spinner.iterations < 1000) {
            Thread.sleep(1);
        }

        // GC while 'spinner' is spinning in Java code
        for (int i = 0; i < 5; ++i) {
            System.out.println("GC start " + i);
            System.gc();
            System.out.println("GC stop " + i);
        }

        // Stop 'spinner'
        spinner.requestStop();
        spinnerThread.join();
    }
}

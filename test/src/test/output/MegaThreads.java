/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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

public class MegaThreads {
    static final int NUMBER_OF_THREADS = 500;

    public static void main(String[] args) throws InterruptedException {
        int threads = NUMBER_OF_THREADS;
        if (args.length > 0) {
            threads = Integer.parseInt(args[0]);
        }
        final ComputeThread[] threadArray = new ComputeThread[threads];
        for (int i = 0; i < threads; i++) {
            final ComputeThread thread = new ComputeThread();
            threadArray[i] = thread;
            thread.start();
        }
        for (int i = 0; i < threads; i++) {
            threadArray[i].join();
        }
        System.out.println("I mean it.");
    }

    private static class ComputeThread extends Thread {
        @Override
        public void run() {
            for (int i = 0; i < 1000; i++) {
                new Object().toString();
            }
            synchronized (ComputeThread.class) {
                System.out.println("done.");
            }
        }
    }
}

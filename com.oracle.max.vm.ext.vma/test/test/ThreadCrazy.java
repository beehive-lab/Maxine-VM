/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package test;

import java.util.*;


public class ThreadCrazy extends Thread {

    final int id;
    long count;
    Random rand;

    ThreadCrazy(int id, long count) {
        this.id = id;
        this.count = count;
        rand = new Random(count);
        setName("Crazy-" + id + "-" + count);
    }

    static boolean verbose;

    public static void main(String[] args) {
        int numThreads = 10;
        int seed = 46347;
        int constant = 0;
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("-t")) {
                numThreads = Integer.parseInt(args[++i]);
            } else if (arg.equals("-v")) {
                verbose = true;
            } else if (arg.equals("-c")) {
                constant = Integer.parseInt(args[++i]);
            }
        }
        // Checkstyle: resume modified control variable check

        Random r = new Random(seed);
        Thread[] threads = new Thread[numThreads];
        for (int t = 0; t < numThreads; t++) {
            threads[t] = new ThreadCrazy(t, constant > 0 ? constant : r.nextInt(1000));
        }
        for (int t = 0; t < numThreads; t++) {
            threads[t].start();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                break;
            }
        }
        for (int t = 0; t < numThreads; t++) {
            try {
                threads[t].join();
            } catch (InterruptedException ex) {

            }
        }
    }

    @Override
    public void run() {
        if (verbose) {
            synchronized (ThreadCrazy.class) {
                System.out.printf("Thread %s starting%n", getName());
            }
        }
        while (count > 0) {
            int r = rand.nextInt(10);
            if (r == 5) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    break;
                }
            }
            count--;
        }
        if (verbose) {
            synchronized (ThreadCrazy.class) {
                System.out.printf("Thread %s done%n", getName());
            }
        }
    }

}

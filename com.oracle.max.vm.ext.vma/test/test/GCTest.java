/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

public class GCTest extends Thread {


    static class A {
        private int a;
        private int lifeTime;
        A(int a, int lifeTime) {
            this.a = a;
            this.lifeTime = lifeTime;
        }

        @Override
        public String toString() {
            return "v=" + a + ", lt=" + lifeTime;
        }
    }

    private static int iterations = 30;
    private static boolean verbose = false;
    private static int nodeCount = 1000;
    private Random random = new Random(467673);
    private List<A> list = new ArrayList<A>();

    public static void main(String[] args) throws Exception  {
        int numThreads = 1;
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("-n")) {
                iterations = Integer.parseInt(args[++i]);
            } else if (arg.equals("-v")) {
                verbose = true;
            } else if (arg.equals("-n")) {
                nodeCount = Integer.parseInt(args[++i]);
            } else if (arg.equals("-t")) {
                numThreads = Integer.parseInt(args[++i]);
            }
        }
        // Checkstyle: resume modified control variable check
        Thread[] threads = new Thread[numThreads];
        for (int t = 0; t < numThreads; t++) {
            threads[t] = new GCTest();
            threads[t].setName("GCTest-" + t);
            threads[t].start();
        }
        for (int t = 0; t < numThreads; t++) {
            threads[t].join();
        }

    }

    private void println(String m) {
        System.out.println(Thread.currentThread().getName() + ": " + m);
    }

    private void buildList() {
        for (int i = 1; i <= nodeCount; i++) {
            A a = new A(i, random.nextInt(iterations + iterations / 20));
            list.add(a);
            if (verbose) {
                println("adding " + a);
            }
        }
    }

    private void deleteExpired(int count) {
        if (verbose) {
            println("deleteExpired " + count);
        }
        Iterator<A> iter = list.iterator();
        while (iter.hasNext()) {
            A a = iter.next();
            if (a.lifeTime < count) {
                if (verbose) {
                    println("deleting " + a);
                }
                iter.remove();
            }
        }
    }

    private void addNew(int count) {
        final int toAdd = nodeCount / 20;
        for (int i = 1; i <= toAdd; i++) {
            A a = new A(nodeCount + count * toAdd + i, random.nextInt(iterations + iterations / 20));
            list.add(a);
            if (verbose) {
                println("adding " + a);
            }
        }
    }

    @Override
    public void run() {
        buildList();
        int count = 1;
        while (count <= iterations) {
            deleteExpired(count);
            addNew(count);
            System.gc();
            count++;
        }
        if (verbose) {
            println("List items still alive: " + list.size());
            for (A a : list) {
                println(a.toString());
            }
        }
    }
}

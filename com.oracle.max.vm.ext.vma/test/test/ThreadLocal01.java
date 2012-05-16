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

import java.util.Random;

/**
 * Test for thread-local data.
 * By default each thread builds a private list of {@link Element} data.
 * If {@link #leak} is set, data will leak to the main thread.
 *
 */
public class ThreadLocal01 extends Thread {

    private static int iterations = 100;
    private static int numThreads;
    private static ThreadCount liveThreads;
    private static boolean leak;
    private static boolean stopLeaker;
    private static List globalList = new List();
    private int id;
    private Random rand;

    static class ThreadCount {
        volatile int count;
        ThreadCount(int count) {
            this.count = count;
        }
    }

    static class ListElement {
        ListElement next;
        Element element;

        ListElement(Element e) {
            element = e;
        }
    }

    static class List {
        ListElement head;
        int size;

        void add(Element e) {
            ListElement le = new ListElement(e);
            if (head == null) {
                head = le;
            } else {
                head.next = le;
                head = le;
            }
            size++;
        }
    }

    ThreadLocal01(int id) {
        this.id = id;
        setName("Generator-" + id);
        if (leak) {
            rand = new Random(46737 + id);
        }
    }

    public static void main(String[] args) throws Exception {
        numThreads = 2;
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("-c")) {
                iterations = Integer.parseInt(args[++i]);
            } else if (arg.equals("-t")) {
                numThreads = Integer.parseInt(args[++i]);
            } else if (arg.equals("-l")) {
                leak = true;
            } else if (arg.equals("-s")) {
                stopLeaker = true;
            }
        }
        // Checkstyle: resume modified control variable check
        liveThreads = new ThreadCount(numThreads);

        System.out.println("running with " + numThreads + " threads");
        LeakThread leakThread = null;
        if (leak) {
            leakThread = new LeakThread();
            leakThread.start();
        }
        Thread[] threads = new Thread[numThreads];
        for (int t = 0; t < numThreads; t++) {
            threads[t] = new ThreadLocal01(t);
            threads[t].start();
        }
        for (int t = 0; t < numThreads; t++) {
            threads[t].join();
        }

        if (leak) {
            System.out.printf("global list size %d, LeakObserver accessCount %d%n", globalList.size, leakThread.accessCount);
        }

        System.out.println("main thread terminating");
    }

    private List list = new List();

    @Override
    public void run() {
        System.out.println("Thread " + getName() + " running");
        final int base = id * iterations;
        for (int i = 0; i < iterations; i++) {
            Element k = new Element(i + base);
            list.add(k);
            if (leak) {
                if (rand.nextInt() % 10 == 0) {
                    synchronized (globalList) {
                        globalList.add(k);
                    }
                }
            }
        }
        System.out.println("Thread " + getName() + " returning");
        synchronized (liveThreads) {
            liveThreads.count--;
        }
    }

    static class Element {
        private int i;

        Element(int i) {
            this.i = i;
        }

    }

    static class LeakThread extends Thread {
        int accessCount;

        LeakThread() {
            setName("LeakObserver");
            setDaemon(true);
        }

        @Override
        public void run() {
            System.out.println("Thread " + getName() + " running");
            while (liveThreads.count > 0 || !stopLeaker) {
                synchronized (globalList) {
                    ListElement element = globalList.head;
                    while (element != null) {
                        @SuppressWarnings("unused")
                        Element k = element.element;
                        element = element.next;
                        accessCount++;
                    }
                }
            }
            System.out.println("Thread " + getName() + " returning");
        }
    }
}

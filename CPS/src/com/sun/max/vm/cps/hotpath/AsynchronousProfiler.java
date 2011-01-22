/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.hotpath;

import java.util.concurrent.*;

import com.sun.max.profile.Metrics.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.*;
import com.sun.max.vm.cps.collect.*;
import com.sun.max.vm.cps.hotpath.compiler.*;
import com.sun.max.vm.cps.tir.*;
import com.sun.max.vm.runtime.*;

public class AsynchronousProfiler implements Runnable {

    public static enum CounterMetric {
        INTERPRETED_BYTECODES, TRUNKS, BRANCHES;
        public static long[] counters = new long[CounterMetric.values().length];
        public static void increment(CounterMetric metric) {
            counters[metric.ordinal()]++;
        }
        private static void print() {
            for (CounterMetric metric : CounterMetric.values()) {
                printKeyValueCount(metric.name(), counters[metric.ordinal()]);
            }
        }
    }

    public static OptionSet optionSet = new OptionSet();
    public static Option<Boolean> profile = optionSet.newBooleanOption("P", false, "(P)rofiles hotpath execution.");

    private static LinkedBlockingQueue<Event> eventQueue = new LinkedBlockingQueue<Event>();

    public static void init() {
        if (MaxineVM.isHosted()) {
            FatalError.unexpected(AsynchronousProfiler.class.getName() + " should only be started at runtime");
        }
        Thread profilerThread = new Thread(new AsynchronousProfiler());
        profilerThread.setPriority(Thread.NORM_PRIORITY);
        profilerThread.setDaemon(true);
        profilerThread.start();
    }

    public void run() {
        try {
            while (true) {
                eventQueue.take().process();
            }
        } catch (InterruptedException e) {
            ProgramError.unexpected();
        }
    }

    public static void print() {
        try {
            // Consume all events before printing final results.
            while (eventQueue.size() > 0) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) { }

        TreeEvent.print();
        ExecuteEvent.print();
        CounterMetric.print();
    }

    private abstract static class Event {
        public abstract void process();
    }

    private abstract static class TreeEvent extends Event {
        private static final LinkedIdentityHashSet<TirTree> trees = new LinkedIdentityHashSet<TirTree>();
        private final TirTree tree;
        public TreeEvent(TirTree tree) {
            this.tree = tree;
            trees.add(tree);
        }

        public static void print() {
            Console.printThinDivider("Trees");
            for (TirTree tree : trees) {
                printKey("Tree", NameMap.nameOf(tree));
                printKey("Anchor", tree.anchor().toString());
                printKey("Iterations", tree.profile().iterations);
                printKey("Executions", tree.profile().executions);
                Console.printThinDivider();
            }
            Console.printThinDivider();
        }
    }

    private static void printKey(String key, Object value) {
        Console.println(key + " : " + value);
    }

    public static void printKeyValueCount(String key, long value) {
        float bytes = 0;
        String unit = "";
        if (value < 1000) {
            bytes = value;
        } else if (value < 1000 * 1000) {
            bytes = value / 1024f;
            unit = "k";
        } else {
            bytes = value / (1000f * 1000f);
            unit = "m";
        }
        printKey(key, String.format("%.2f %s", bytes, unit));
    }

    public static void writeKeyValueBytes(String key, long value) {
        float bytes = 0;
        String unit = "";
        if (value < 1024) {
            bytes = value;
            unit = "B";
        } else if (value < 1024 * 1024) {
            bytes = value / 1024f;
            unit = "KB";
        } else {
            bytes = value / (1024f * 1024f);
            unit = "MB";
        }
        printKey(key, String.format("%.2f %s", bytes, unit));
    }

    private static class ExecuteEvent extends TreeEvent {
        private static final Counter count = new Counter();

        private final Bailout bailout;

        public ExecuteEvent(TirTree tree) {
            super(tree);
            bailout = null;
        }

        public static void print() {
            Console.printThinDivider("Executions");
            printKey("execution", count.getCount());
            Console.printThinDivider();
        }

        public ExecuteEvent(TirTree tree, Bailout bailout) {
            super(tree);
            this.bailout = bailout;
        }

        @Override
        public void process() {
            if (bailout == null) {
                count.increment();
            }
        }
    }

    private static boolean isProfiling() {
        return profile.getValue();
    }

    private static void enqueue(Event event) {
        eventQueue.add(event);
    }

    public static void eventExecute(TirTree tree) {
        if (isProfiling()) {
            enqueue(new ExecuteEvent(tree));
        }
    }

    public static void eventBailout(TirTree tree, Bailout bailout) {
        if (isProfiling()) {
            enqueue(new ExecuteEvent(tree, bailout));
        }
    }

    public static void event(CounterMetric metric) {
        CounterMetric.increment(metric);
    }
}

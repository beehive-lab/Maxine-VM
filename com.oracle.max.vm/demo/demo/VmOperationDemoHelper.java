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
package demo;

import java.util.*;

import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Support for writing {@link VmOperation} demos.
 */
public class VmOperationDemoHelper {
    protected final Map<Thread, DemoRunnable> threads = new HashMap<Thread, DemoRunnable>();

    public static abstract class DemoRunnable implements Runnable {
        protected void runEntered() {
            synchronized (VmOperationDemoHelper.class) {
                started++;
            }
        }

        /**
         * Used by a subclass to customize a thread created by {@link #createThreads},
         * e.g., set the name, set as daemon etc.
         * @param thread
         */
        protected void customize(Thread thread) {
        }
    }

    static volatile int started;

    public static int runTime = 5;
    public static  int numThreads = 10;

    public static void parseArguments(String[] args) {
        // Checkstyle: stop
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-s")) {
                runTime = Integer.parseInt(args[++i]);
                args[i] = null; args[i - 1] = null;
            } else if (arg.equals("-t")) {
                numThreads = Integer.parseInt(args[++i]);
                args[i] = null; args[i - 1] = null;
            }
        }
        // Checkstyle: resume
    }

    public VmOperationDemoHelper(DemoRunnable runnable) {
        createThreads(numThreads, runnable);
    }

    protected boolean operateOnThread(VmThread thread) {
        return threads.get(thread.javaThread()) != null;
    }

    public HashSet<VmThread> vmThreadSet() {
        HashSet<VmThread> set = new HashSet<VmThread>();
        for (Thread thread : threads.keySet()) {
            set.add(VmThread.fromJava(thread));
        }
        return set;
    }

    /**
     * Create a set of threads all executing the same {@link DemoRunnable}.
     * Wait for all of them to enter their run method before returning.
     * @param numThreads
     * @param runnable
     */
    public void createThreads(int numThreads, DemoRunnable runnable) {
        for (int i = 0; i < numThreads; i++) {
            addThread(runnable);
        }
        while (started != numThreads) {
            Thread.yield();
        }
    }

    /**
     * Add a new thread to the set.
     * @param runnable
     */
    public void addThread(DemoRunnable runnable) {
        Thread spinner = new Thread(runnable);
        runnable.customize(spinner);
        threads.put(spinner, runnable);
        spinner.start();
    }

}

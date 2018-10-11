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

import demo.VmOperationDemoHelper.DemoRunnable;

/**
 * A demo (test) for thread suspend and resume {@link VmOperation}s.
 *
 * The test creates a variable number of compute bound spinners
 * and one thread that blocks in native code by calling {@link Thread#sleep}
 * for {@link #blockTime} seconds, default {@link #blockTime}, which can be
 * changed by the {@code -b time} argument.
 *
 * After the threads have all entered their {@link Thread#run} methods,
 * they are all suspended. There is then a delay specified by {@link #resumeDelay},
 * default {@link #resumeDelay}, which can be changed by the {@code -d time} argument.
 *
 * Then the threads are resumed. Finally, the main thread waits for all the spinners
 * and the native blocker to terminate.
 *
 * If {@link #blockTime} is larger than {@link #resumeDelay}, then the resume operation
 * should cancel the suspend request for the native blocker, i.e. the program
 * will not deadlock. Conversely, if {@link #resumeDelay} is larger than {@link #blockTime},
 * the native blocker should suspend but then be released immediately by the resume operation.
 *
 */
public class SuspendResumeVmOperationDemo {

    private static VmOperationDemoHelper demoHelper;
    private static int blockTime = 15;
    private static int resumeDelay = 5;
    private static boolean verbose;

    public static class Spinner extends DemoRunnable {
        static int i = 0;
        volatile boolean done;
        @Override
        public void run() {
            runEntered();
            long x = 0;
            while (!done) {
                x = x + 1;
                if (verbose) {
                    if (x % 100000 == 0) {
                        System.out.printf("%s at %d%n", Thread.currentThread().getName(), x);
                    }
                }
            }
            System.out.println("Spinner terminating");
        }

        @Override
        protected void customize(Thread thread) {
            thread.setName("Spinner-" + i++);
        }
    }

    static class NativeBlocker extends DemoRunnable {
        Thread thread;
        public void run() {
            runEntered();
            try {
                Thread.sleep(blockTime * 1000);
            } catch (InterruptedException ex) {
            }
            System.out.println("Blocker terminating");
        }

        @Override
        protected void customize(Thread thread) {
            thread.setName("NativeBlocker");
            this.thread = thread;
        }
    }

    public static void main(String[] args) throws Exception {
        VmOperationDemoHelper.parseArguments(args);
        // Checkstyle: stop
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                continue;
            }
            if (args[i].equals("-b")) {
                blockTime = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-d")) {
                resumeDelay = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-v")) {
                verbose = true;
            }
        }
        // Checkstyle: resume

        // create a bunch of compute spinners.
        demoHelper = new VmOperationDemoHelper(new Spinner());
        Map<Thread, DemoRunnable> spinners = new HashMap<Thread, DemoRunnable>(demoHelper.threads);

        // now create a thread that will be for sure blocked in native code when
        // suspend request is made.
        NativeBlocker nativeBlocker = new NativeBlocker();
        demoHelper.addThread(nativeBlocker);

        Set<VmThread> vmThreadSet = demoHelper.vmThreadSet();

        VmOperation suspendOperation = new VmOperation.SuspendThreadSet(vmThreadSet);
        try {
            System.out.println("Suspending");
            suspendOperation.submit();
            for (Map.Entry<Thread, DemoRunnable> entry : demoHelper.threads.entrySet()) {
                Thread thread = entry.getKey();
                System.out.printf("Thread %s state is %s%n", thread.getName(), thread.getState());
            }

            System.out.printf("Delaying for %d%n", resumeDelay);
            Thread.sleep(resumeDelay * 1000);

            VmOperation resumeOperation = new VmOperation.ResumeThreadSet(vmThreadSet);

            System.out.println("Resuming");
            resumeOperation.submit();
            for (Map.Entry<Thread, DemoRunnable> entry : demoHelper.threads.entrySet()) {
                Thread thread = entry.getKey();
                System.out.printf("Thread %s state is %s%n", thread.getName(), thread.getState());
            }
            // ask spinners to quit
            for (Map.Entry<Thread, DemoRunnable> entry : spinners.entrySet()) {
                Spinner spinner = (Spinner) entry.getValue();
                spinner.done = true;
            }
            System.out.println("Waiting for Spinners");
            // wait for spinners to finish
            for (Map.Entry<Thread, DemoRunnable> entry : spinners.entrySet()) {
                Thread thread = entry.getKey();
                while (thread.isAlive()) {
                    Thread.yield();
                }
            }
            System.out.println("Waiting for NativeBlocker");
            // wait for NativeBlocker
            while (nativeBlocker.thread.isAlive()) {
                Thread.yield();
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }
}

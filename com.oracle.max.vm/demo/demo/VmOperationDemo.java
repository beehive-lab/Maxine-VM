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
package demo;

import java.util.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * Demonstrates usage of the {@link VmOperation} mechanism.
 * This demo starts a number of threads that spin in a loop
 * that allocates object arrays of length 1024, thus creating
 * garbage quickly. The main thread also spins in a loop for
 * a specified number of seconds, printing a stack trace of
 * all the other spinning threads.
 *
 * This demo also shows what happens if a VM operation
 * triggers a GC.
 *
 * Like all VmOperations, this class must be in the boot image.
 * This is achieved by adding 'demo.VmOperationDemo' to the end
 * of a 'max image' command.
 */
public class VmOperationDemo extends VmOperation {

    private final HashSet<Thread> threads;

    public VmOperationDemo(HashSet<Thread> threads) {
        super("Demo", null, Mode.Safepoint);
        this.threads = threads;
        this.allowsNestedOperations = true;
    }

    @Override
    protected boolean operateOnThread(VmThread thread) {
        return threads.contains(thread.javaThread());
    }

    @Override
    public void doThread(VmThread vmThread, Pointer ip, Pointer sp, Pointer fp) {
        Thread thread = vmThread.javaThread();
        if (ip.isZero()) {
            System.out.println(thread + " [not yet executing Java]");
        } else {
            VmStackFrameWalker sfw = new VmStackFrameWalker(vmThread.tla());
            StackTraceElement[] trace = JDK_java_lang_Throwable.getStackTrace(sfw, ip, sp, fp, null, Integer.MAX_VALUE);
            System.out.println(thread + " [stack depth: " + trace.length + "]");
            for (StackTraceElement e : trace) {
                System.out.println("\tat " + e);
            }
        }
    }

    static volatile boolean done;
    static volatile int started;

    public static void main(String[] args) {

        int seconds = args.length == 0 ? 5 : Integer.parseInt(args[0]);
        Thread[] spinners = new Thread[10];
        for (int i = 0; i < spinners.length; i++) {
            Thread spinner = new Thread() {
                @Override
                public void run() {
                    synchronized (VmOperationDemo.class) {
                        started++;
                    }
                    while (!done) {
                        Object[] o = new Object[1024];
                        o[0] = o;
                    }
                }
            };
            spinners[i] = spinner;
            spinner.start();
        }
        while (started != spinners.length) {
            Thread.yield();
        }

        VmOperationDemo stackTraceDumper = new VmOperationDemo(new HashSet<Thread>(Arrays.asList(spinners)));
        long start = System.currentTimeMillis();
        int time;
        try {
            do {
                time = (int) (System.currentTimeMillis() - start) / 1000;
                try {
                    System.out.println("---- Dumping stacks of spinning threads ----");
                    stackTraceDumper.submit();
                } catch (VmOperationThread.HoldsThreadLockError e) {
                    System.out.println("VM operation triggered while dumping stack traces");
                    break;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (time < seconds);
        } finally {
            done = true;
        }
    }
}

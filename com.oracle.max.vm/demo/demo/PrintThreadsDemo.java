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

import sun.misc.*;

import com.sun.max.vm.runtime.*;

/**
 * Demonstrates use of the {@link PrintThreads} VM operation
 * combined with signal handling. It slightly modifies the
 * default behavior of the VM with respect to the SIGHUP signal.
 * Each successive SIGHUP sent to the Maxine VM running this
 * program toggles the format of the thread dump performed.
 *
 * Like all VmOperations, this class must be in the boot image.
 * This is achieved by adding 'demo.PrintThreadsDemo' to the end
 * of a 'max image' command.
 */
public class PrintThreadsDemo {
    static final int NUMBER_OF_THREADS = 1;

    public static void main(String[] args) throws InterruptedException {
        Signal.handle(new Signal("HUP"), new PrintThreads(false) {
            @Override
            protected void doIt() {
                super.doIt();
                internalFormat = !internalFormat;
            }
        });

        int threads = NUMBER_OF_THREADS;
        if (args.length > 0) {
            threads = Integer.parseInt(args[0]);
        }
        final AllocatingSpinner[] threadArray = new AllocatingSpinner[threads];
        for (int i = 0; i < threads; i++) {
            final AllocatingSpinner thread = new AllocatingSpinner();
            threadArray[i] = thread;
            thread.start();
        }

        // This program has to be stopped with CTRL-C (i.e. SIGINT)
    }

    private static class AllocatingSpinner extends Thread {
        @SuppressWarnings("unused")
        @Override
        public void run() {
            long counter = 0;
            while (true) {
                counter++;
                new Object().toString();
            }
        }
    }
}

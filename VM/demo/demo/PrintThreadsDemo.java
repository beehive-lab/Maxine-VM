/*
 * Copyright (c) 2010 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
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
 *
 * @author Doug Simon
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

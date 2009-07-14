/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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

 /*
 * @Harness: java
 * @Runs: (0, 0) = true; (1, 500) = true
 */

package test.threads;

//Test all, mainly monitors
public class Thread_isInterrupted02 {

    private static final Object start = new Object();
    private static final Object end = new Object();
    private static int waitTime;

    public static boolean test(int i, int time) throws InterruptedException {
        waitTime = time;
        final Thread thread = new Thread();
        synchronized (thread) {
            // start the thread and wait for it
            thread.setDaemon(true); // in case the thread gets stuck
            thread.start();
            thread.wait();
        }
        synchronized (start) {
            thread.interrupt();
        }
        synchronized (end) {
            end.wait(200);
        }
        return thread.interrupted;
    }

    private static class Thread extends java.lang.Thread {
        private boolean interrupted;
        @Override
        public void run() {
            try {
                synchronized (start) {
                    synchronized (this) {
                        // signal test thread that we are running
                        notify();
                    }
                    // wait for the condition, which should be interrupted
                    if (waitTime == 0) {
                        start.wait();
                    } else {
                        start.wait(waitTime);
                    }
                }
            } catch (InterruptedException e) {
                // interrupted successfully.
                interrupted = true;
                synchronized (end) {
                    // notify the other thread we are done
                    end.notify();
                }
            }
        }
    }
}

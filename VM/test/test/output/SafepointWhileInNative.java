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
package test.output;

public class SafepointWhileInNative {
    private static final class Sleeper implements Runnable {
        public void run() {
            System.out.println("Sleeper: sleeping...");
            synchronized (this) {
                // Notify that 'sleeper' has started
                notify();
            }
            synchronized (this) {
                // Go to sleep
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Sleeper: woke up!");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        final Runnable sleeper = new Sleeper();
        System.gc();
        final Thread sleeperThread = new Thread(sleeper, "Sleeper");

        synchronized (sleeper) {
            sleeperThread.start();
            // Wait until 'sleeper' has started
            sleeper.wait();
        }

        // Give 'sleeper' a chance to start sleeping
        System.out.println("Main: sleeping...");
        Thread.sleep(200);
        System.out.println("Main: woke up!");

        // GC while 'sleeper' is blocked on the monitor in native code
        System.out.println("GC start");
        System.gc();
        System.out.println("GC stop");

        synchronized (sleeper) {
            // Wake up 'sleeper'
            sleeper.notify();
        }
        sleeperThread.join();
    }
}

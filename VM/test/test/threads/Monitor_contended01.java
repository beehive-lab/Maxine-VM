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
 * @Runs: 0 = true
 */
package test.threads;


public final class Monitor_contended01 implements Runnable {

    private Monitor_contended01() {
    }

    static final Object cond = new Object();
    static final Object obj = new Object();

    boolean started = false;
    boolean acquired = false;

    public static boolean test(int i) throws InterruptedException {
        // test contention for monitor
        final Monitor_contended01 object = new Monitor_contended01();
        synchronized (obj) {
            new Thread(object).start();
            // wait for other thread to startup and contend
            synchronized (cond) {
                cond.wait(1000);
                if (!object.started) {
                    return false;
                }
            }
        }
        // wait for other thread to acquire monitor and then exit
        synchronized (cond) {
            cond.wait(1000);
        }
        return object.acquired;
    }

    public void run() {
        // signal that we have started up so first thread will release lock
        synchronized (cond) {
            started = true;
            cond.notifyAll();
        }
        synchronized (obj) {

        }
        // signal that we have successfully acquired and released the monitor
        synchronized (cond) {
            acquired = true;
            cond.notifyAll();
        }
    }

}


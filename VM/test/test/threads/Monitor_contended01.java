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

    static final Object _cond = new Object();
    static final Object _object = new Object();

    boolean started = false;
    boolean acquired = false;

    public static boolean test(int i) throws InterruptedException {
        // test contention for monitor
        final Monitor_contended01 object = new Monitor_contended01();
        synchronized (_object) {
            new Thread(object).start();
            // wait for other thread to startup and contend
            synchronized (_cond) {
                _cond.wait(1000);
                if (!object.started) {
                    return false;
                }
            }
        }
        // wait for other thread to acquire monitor and then exit
        synchronized (_cond) {
            _cond.wait(1000);
        }
        return object.acquired;
    }

    public void run() {
        // signal that we have started up so first thread will release lock
        synchronized (_cond) {
            started = true;
            _cond.notifyAll();
        }
        synchronized (_object) {

        }
        // signal that we have successfully acquired and released the monitor
        synchronized (_cond) {
            acquired = true;
            _cond.notifyAll();
        }
    }

}


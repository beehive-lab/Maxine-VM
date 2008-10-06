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
/*VCSID=69bdd734-4dba-4c5c-992e-38bcbafb21d8*/
/*
 * @Harness: java
 * @Runs: 0 = true; 1 = true; 3 = true; 15 = true
 */
package test.threads;



public class Object_wait01 implements Runnable {

    static volatile int _count = 0;
    static volatile boolean _done;
    static final Object _object = new Object();

    public static boolean test(int i) throws InterruptedException {
        _count = 0;
        _done = false;
        new Thread(new Object_wait01()).start();
        synchronized (_object) {
            while (_count < i) {
                _object.wait();
            }
            _done = true;
            return _count >= i;
        }

    }

    public void run() {
        int i = 0;
        while (i++ < 1000000 && !_done) {
            synchronized (_object) {
                _count++;
                _object.notifyAll();
            }
        }
    }
}

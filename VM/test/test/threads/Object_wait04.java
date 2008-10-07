/*VCSID=2f497826-59c1-4a78-acce-5285ed88d226*/
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
  * @Runs: 0 = true; 1=true; 2=true; 3=true; 4=true; 5=true
 */
package test.threads;

public class Object_wait04  implements Runnable {
    static volatile boolean _done;
    static final Object _object = new Object();
    static int _sleep;

    public static boolean test(int i) throws InterruptedException {
        _done = false;
        _sleep = i * 50;
        synchronized (_object) {
            new Thread(new Object_wait04()).start();
            dowait(i);
        }
        return _done;
    }

    private static void dowait(int i) throws InterruptedException {
        if (i == 0) {
            while (!_done) {
                _object.wait(100);
            }
        } else {
            synchronized (_object) {
                dowait(i - 1);
            }
        }
    }

    public void run() {
        try {
            Thread.sleep(_sleep);
        } catch (InterruptedException ex) {

        }
        synchronized (_object) {
            _done = true;
            _object.notifyAll();
        }
    }

}

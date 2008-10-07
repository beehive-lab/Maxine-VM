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
/*VCSID=9b56d6ba-72b7-4e34-9279-5aa273eb9b3f*/
/*
 * @Harness: java
 * @Runs: 0 = false
 * 
 * This test sleeps the thread that is joined to, which should ensure that the joining thread
 * actually does wait for completeion.
 */
package test.threads;


public class Thread_join02 implements Runnable {

    static volatile boolean _continue;

    public static boolean test(int i) throws InterruptedException {
        _continue = true;
        final Thread thread = new Thread(new Thread_join02());
        thread.start();
        thread.join();
        return _continue;
    }

    public void run() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
        }
        _continue = false;
    }
}

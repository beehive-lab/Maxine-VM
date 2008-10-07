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
/*VCSID=0fdea3e1-8c44-4812-a9be-263c9dece9eb*/
/*
 * @Harness: java
 * @Runs: 0 = false
 * 
 * This test sleeps the joining thread, which should enure that the joinee is
 * terminated by the time the join occurs.
 */
package test.threads;


public class Thread_join03 implements Runnable {

    static volatile boolean _continue;

    public static boolean test(int i) throws InterruptedException {
        _continue = true;
        final Thread thread = new Thread(new Thread_join03());
        thread.start();
        Thread.sleep(200);
        thread.join();
        return _continue;
    }

    public void run() {
        _continue = false;
    }
}

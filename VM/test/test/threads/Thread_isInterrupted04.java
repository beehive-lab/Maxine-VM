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
package test.threads;

/*
 * @Harness: java
 * @Runs: 0 = true
 */

// Interrupted while running, do nothing, just set the flag and continue
public class Thread_isInterrupted04 {

    public static boolean test(int i) throws InterruptedException {
        final Thread1 thread = new Thread1();
        thread.start();
        Thread.sleep(10);
        thread.interrupt();
        thread.setStop(true);
        return thread.isInterrupted();
    }

    private static class Thread1 extends java.lang.Thread {

        private volatile boolean _stop = false;
        private long i = 0;

        @Override
        public void run() {
            while (!_stop) {
                i++;
            }
        }

        public void setStop(boolean value) {
            _stop = value;
        }

    }
}

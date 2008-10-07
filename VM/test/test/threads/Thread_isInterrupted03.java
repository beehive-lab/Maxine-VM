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
 * @Runs: 0 = true;
 */

// Interrupted while sleeping, throws an interrupted exception
public class Thread_isInterrupted03 {

    public static boolean test(int i) throws InterruptedException {
        final Thread1 thread = new Thread1();
        thread.start();
        Thread.sleep(1000);
        thread.interrupt();
        Thread.sleep(1000);
        // Did thread get interrupted?
        final boolean result = thread.getInterrupted();
        // This stops the thread even if the interrupt didn't!
        thread.setInterrupted(true);
        return result;
    }

    private static class Thread1 extends java.lang.Thread {

        private boolean _interrupted = false;

        @Override
        public void run() {
            while (!_interrupted) {
                try {
                    sleep(10000);
                } catch (InterruptedException e) {
                     _interrupted = true;
                }
            }
        }

        public void setInterrupted(boolean val) {
            _interrupted = val;
        }

        public boolean getInterrupted() {
            return _interrupted;
        }
    }
}

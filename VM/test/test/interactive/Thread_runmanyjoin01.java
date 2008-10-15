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
 * @Runs: 3 = true;
 */
package test.interactive;

import com.sun.max.program.*;

public final class Thread_runmanyjoin01 {

    public static final boolean debug = true;

    public static boolean test(int i) throws InterruptedException {
       Thread[] runners = new Thread[i];
        for (int  j = 0; j < i; j++) {
            runners[j] = new Thread(new Runner(j));
            runners[j].start();
        }
        for (int  j = 0; j < i; j++) {
            runners[j].join();
        }
       return true;
    }

    static class Runner implements Runnable {
        private int _id;
        Runner(int id) {
            _id = id;
        }

        public void run() {
            long startTime = System.currentTimeMillis();
            long now = startTime;
            long count = 0;
            debug("Runner " + _id + " starting at " + startTime);
            while (now < startTime + 10000 + _id * 2000) {
                count ++;
                if (count % 10000000 == 0) {
                    now = System.currentTimeMillis();
                    debug("Runner " + _id + " time now " + now);
                }
            }
            debug("Runner " + _id + " finished");
        }
    }

    private static void debug(String s) {
        if (debug) {
            Trace.stream().println(s);
        }
    }
}

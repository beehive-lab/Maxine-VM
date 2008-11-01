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
 * @Runs: 1000 = true;
 */
package test.interactive;

import com.sun.max.vm.debug.*;

public final class Thread_sleepIdle01 {
    private static boolean running = true;

    private Thread_sleepIdle01() {
    }

    public static boolean test(int i) throws InterruptedException {
        new Thread(new IdleThread()).start();
        for (int t = 0; t< 10; t++) {
                Debug.println("sleeping");
                Thread.sleep(i);
                Debug.println("waking");
        }
        running = false;
        return true;
    }

    static class IdleThread implements Runnable {

        public void run() {
            long i = 0;
            while (running) {
                i++;
                if ((i % 10000000) == 0) {
                    Debug.println("idling");
                }
            }
        }
    }
}

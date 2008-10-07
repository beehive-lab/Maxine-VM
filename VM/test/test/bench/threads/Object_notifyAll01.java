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
/*VCSID=591a3150-575e-4c00-aa06-c564b0d6eb79*/
/*
 * @Harness: java
 * @Runs: 0 = true
 */
package test.bench.threads;

import test.bench.util.*;

public class Object_notifyAll01 extends RunBench {
    static volatile int _count;

    protected Object_notifyAll01(LoopRunnable bench) {
        super(bench);
    }

    public static boolean test(int i) throws InterruptedException {
        new Object_notifyAll01(new Bench()).runBench(true);
        return true;
    }

    static class Bench implements LoopRunnable {
        public void run(long loopCount) {
            for (long i = 0; i < loopCount; i++) {
                synchronized (this) {
                    _count++;
                    notifyAll();
                }
            }
        }

        public void runBareLoop(long loopCount) {
            for (long i = 0; i < loopCount; i++) {
                synchronized (this) {
                    _count++;
                }
            }
        }
    }

}

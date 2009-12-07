/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package jtt.threads;

/*
 * @Harness: java
 * @Runs: 0 = 0; 1 = 15; 2 = 31; 3 = 48
 */
public class ThreadLocal03 {

    static final ThreadLocal<Integer> local = new ThreadLocal<Integer>();

    public static int test(int i) {
        int sum = 0;
        for (int j = 0; j < i; j++) {
            TThread t = new TThread();
            t.input = 10 + j;
            t.run();
            try {
                t.join();
            } catch (InterruptedException e) {
                return -1;
            }
            sum += t.output;
        }
        return sum;
    }

    private static class TThread extends Thread {
        int input;
        int output;
        public void run() {
            local.set(input + 5);
            output = local.get();
        }
    }
}
/*
 * Copyright (c) 2008 Sun Microsystems, Inc.  All rights reserved.
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
package test.output;


public class MegaThreads {
    static final int NUMBER_OF_THREADS = 500;

    public static void main(String[] args) throws InterruptedException {
        int threads = NUMBER_OF_THREADS;
        if (args.length > 0) {
            threads = Integer.parseInt(args[0]);
        }
        final ComputeThread[] threadArray = new ComputeThread[threads];
        for (int i = 0; i < threads; i++) {
            final ComputeThread thread = new ComputeThread();
            threadArray[i] = thread;
            thread.start();
        }
        for (int i = 0; i < threads; i++) {
            threadArray[i].join();
        }
        System.out.println("I mean it.");
    }

    private static class ComputeThread extends Thread {
        @Override
        public void run() {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            for (int i = 0; i < 1000; i++) {
                new Object().toString();
            }
            synchronized (ComputeThread.class) {
                System.out.println("done.");
            }
        }
    }
}

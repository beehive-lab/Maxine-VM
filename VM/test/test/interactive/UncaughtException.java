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
/*VCSID=28f5817f-1aa7-4bc4-b16d-75762441c556*/
package test.interactive;

/*
 * @Harness: java
 * @Runs: 0 = true
 */
public class UncaughtException {
    static RuntimeException throwMe = new RuntimeException("uncaught");

    /**
     * Testing that an uncaught exception is routed to the thread group.
     */
    public static boolean test(int i) throws InterruptedException {

        final TestThreadGroup testThreadGroup = new TestThreadGroup();
        final Thread thread = new Thread(testThreadGroup, new Runnable() {
            public void run() {
                throw throwMe;
            }
        });
        thread.start();
        thread.join();
        final Throwable throwable =  testThreadGroup.getLastUncaughtException();
        return throwable == throwMe;
    }

    static class TestThreadGroup extends   ThreadGroup {
        Throwable _uncaught;
        public TestThreadGroup() {
            super("test");
        }

        public synchronized Throwable getLastUncaughtException() {
            return _uncaught;
        }

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            synchronized(this) {
                _uncaught = e;
            }
            super.uncaughtException(t, e);
        }
    }
}

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
package test.output;

/**
 * A multi-threaded GC test.
 *
 * @author Ben L. Titzer
 */
public class GCTest5 {
    private static class ComputeThread extends Thread {
        private final Class thisClass = getClass();
        @Override
        public void run() {
            while (true) {
                m1();
            }
        }
        void m1() {
            final Object object = this;
            m2();
            assertReferences(this);
            for (int i = 0; i < 100; i++) {
                this.hashCode();
            }
            assertReferences(object);
            m2();
        }
        void m2() {
            final Object object = this;
            m3();
            assertReferences(this);
            for (int i = 0; i < 100; i++) {
                this.hashCode();
            }
            assertReferences(object);
            m3();
        }
        void m3() {
            final Object object = this;
            m4();
            assertReferences(this);
            for (int i = 0; i < 100; i++) {
                this.hashCode();
            }
            m4();
            assertReferences(object);
        }
        void m4() {
            for (int i = 0; i < 100; i++) {
                final Object object = this;
                assertReferences(object);
                assertReferences(this);
            }
        }
        private void assertReferences(Object param) {
            assert this == param;
            assert this.getClass() == thisClass;
            assert this.getClass().hashCode() == thisClass.hashCode();
        }
    }

    public static void main(String[] args) {
        int threads = 1;
        if (args.length > 0) {
            threads = Integer.parseInt(args[0]);
        }
        System.out.println("Using " + threads + " non-allocating compute threads.");
        for (int i = 0; i < threads; i++) {
            final ComputeThread computeThread = new ComputeThread();
            computeThread.setDaemon(true);
            computeThread.start();
        }
        for (int i = 0; i < 25; i++) {
            System.out.println("Creating garbage: " + i + "...");
            createGarbage();
        }
        System.out.println(GCTest5.class.getSimpleName() + " done.");
    }

    /**
     * Create various kinds of garbage.
     */
    private static void createGarbage() {
        final int max = 10000;
        final Object[] objects = new Object[max];
        for (int i = 0; i < max; i++) {
            final int[] ints = new int[i];
            objects[i / 5] = ints;
        }

        for (int i = 0; i < max / 4; i++) {
            objects[i * 4] = new Object();
            objects[i * 4 + 1] = objects;
        }

        for (int i = 0; i < max / 3; i++) {
            final GCTest5 garbageTest = new GCTest5();
            objects[i * 3] = garbageTest;
        }
    }
}

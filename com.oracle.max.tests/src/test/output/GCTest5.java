/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package test.output;

/**
 * A multi-threaded GC test.
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

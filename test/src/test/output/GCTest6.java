/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * Fills the heap with garbage, runs until GC has been triggered a few times.
 */
public final class GCTest6 {

    private final Object object;

    private GCTest6() {
        object = this;
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
            final GCTest6 garbageTest = new GCTest6();
            objects[i * 3] = garbageTest;
            objects[i * 3 + 1] = garbageTest.object;
        }
    }

    public static void main(String[] args) {
        System.out.println(GCTest6.class.getSimpleName() + " starting...");
        int max = 25;
        if (args.length > 0) {
            max = Integer.parseInt(args[0]);
        }
        for (int i = 0; i < max; i++) {
            System.out.println("Creating garbage: " + i + "...");
            createGarbage();
        }
        System.out.println(GCTest6.class.getSimpleName() + " done.");
    }
}

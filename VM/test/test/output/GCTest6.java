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
 * Fills the heap with garbage, runs until GC has been triggered a few times.
 *
 * @author Bernd Mathiske
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

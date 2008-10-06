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
/*VCSID=859fdecb-50c1-4e23-ab22-72ecd390d137*/
package util;

import com.sun.max.program.*;

/**
 * Fills the heap with garbage, runs until GC has been triggered a few times.
 *
 * @author Bernd Mathiske
 */
public final class GarbageTest {

    private GarbageTest() {
    }

    private static class GarbageClassLoader extends ClassLoader {
    }

    /**
     * Create various kinds of garbage.
     */
    private static void createGarbage() {
        // final ClassLoader classLoader = new GarbageClassLoader();
        try {
            // classLoader.loadClass(HelloWorld.class.getName());
        } catch (Throwable throwable) {
            ProgramError.unexpected("could not load class");
        }

        final Object[] objects = new Object[1000];
        for (int i = 0; i < 1000; i++) {
            final int[] ints = new int[i];
            objects[i / 5] = ints;
        }

        for (int i = 0; i < 200; i++) {
            objects[i * 4] = new Object();
        }

        for (int i = 0; i < 300; i++) {
            objects[i * 3] = new GarbageTest();
        }
    }

    public static void main(String[] args) {
        System.out.println("BEGIN " + GarbageTest.class.getSimpleName());
        // while (VMConfiguration.hostOrTarget().heapScheme().numberOfGarbageCollectionTurnoverCycles() < 5) {
        // createGarbage();
        // }
        System.out.println("END " + GarbageTest.class.getSimpleName());
    }
}

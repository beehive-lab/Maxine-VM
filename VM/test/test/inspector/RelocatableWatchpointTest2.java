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
package test.inspector;

/**
 * Simple Inspector test to proof the implemented watchpoints code.
 *
 * @author Hannes Payer
 */
public class RelocatableWatchpointTest2 {

    /**
     * @param args
     */

    private static final int allocations = 10000000;
    private static final int allocationSize = 20;

    public static SimpleObject getSimpleObject() {
        return new SimpleObject(100, 200);
    }

    public static SimpleObject getGarbageSimpleObject() {
        return new SimpleObject(10, 20);
    }

    public static void printMessage(SimpleObject simpleObject) {
        System.out.println(simpleObject.value1 + " " + simpleObject.value2);
    }

    public static void relocationTest() {
        SimpleObject test = getGarbageSimpleObject();
        test = getSimpleObject();
        System.gc();
        printMessage(test);
        System.gc();
        printMessage(test);
        System.out.println("program end");
    }

    public static void main(String[] args) {
        relocationTest();
    }

    private static class SimpleObject {

        public SimpleObject(int value1, int value2) {
            this.value1 = value1;
            this.value2 = value2;
            this.string = new String("test " + value1);
        }

        public int value1;
        public int value2;
        public String string;
    }
}

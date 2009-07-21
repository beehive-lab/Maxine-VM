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
public class RelocatableWatchpointTest1 {

    /**
     * @param args
     */

    private static final int allocations = 10000000;
    private static final int allocationSize = 20;

    public static String getMessage() {
        return new String("allocationTest");
    }

    public static String getGarbageMessage() {
        return new String("allocationTestGarbage");
    }

    public static void printMessage(String message) {
        System.out.println(message);
    }

    public static void relocationTest() {
        String test = getGarbageMessage();
        printMessage(test);
        test = getMessage();
        printMessage(test);
        for (int i = 0; i < allocations; i++) {
            final byte[] tmp = new byte[allocationSize];
            tmp[0] = 1;
        }
        printMessage(test);
        for (int i = 0; i < allocations; i++) {
            final byte[] tmp = new byte[allocationSize];
            tmp[0] = 1;
        }
        printMessage(test);
    }

    public static void main(String[] args) {
        relocationTest();
    }

}

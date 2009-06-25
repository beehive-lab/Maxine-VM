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
package test.optimize;

/*
 * Tests constant folding of array length operations.
 * @Harness: java
 * @Runs: 0=5; 1=6; 2=7; 3=8; 4=4
 */
public class ArrayLength01 {
    public static final int SIZE = 8;
    public static final byte[] arr = new byte[5];
    public static int test(int arg) {
        if (arg == 0) {
            return arr.length;
        }
        if (arg == 1) {
            return new byte[6].length;
        }
        if (arg == 2) {
            return new Object[7].length;
        }
        if (arg == 3) {
            return new Class[SIZE][].length;
        }
        if (arg == 4) {
            return new int[arg].length;
        }
        return 0;
    }
}
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
 * Tests constant folding of integer operations.
 * @Harness: java
 * @Runs: 0=-2147483648L; 1=-33L; 2=-78L
 */
public class Fold_Convert02 {
    public static long test(long arg) {
        if (arg == 0) {
            return i2l();
        }
        if (arg == 1) {
            return f2l();
        }
        if (arg == 2) {
            return d2l();
        }
        return  0;
    }
    public static long i2l() {
        int x = 0x80000000;
        return x;
    }
    public static long f2l() {
        float x = -33.1f;
        return (long) x;
    }
    public static long d2l() {
        double x = -78.1d;
        return (long) x;
    }
}
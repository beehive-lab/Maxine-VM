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
package jtt.optimize;

/*
 * Tests constant folding of integer operations.
 * @Harness: java
 * @Runs: 0=10L; 1=11L; 2=12L; 3=13L; 4=14L; 5=15L; 6=16L; 7=17L
 */
public class Reduce_Long01 {
    public static long test(long arg) {
        if (arg == 0) {
            return add(10);
        }
        if (arg == 1) {
            return sub(11);
        }
        if (arg == 2) {
            return mul(12);
        }
        if (arg == 3) {
            return div(13);
        }
        if (arg == 4) {
            return mod();
        }
        if (arg == 5) {
            return and(15);
        }
        if (arg == 6) {
            return or(16);
        }
        if (arg == 7) {
            return xor(17);
        }
        return 0;
    }
    public static long add(long x) {
        return x + 0;
    }
    public static long sub(long x) {
        return x - 0;
    }
    public static long mul(long x) {
        return x * 1;
    }
    public static long div(long x) {
        return x / 1;
    }
    public static long mod() {
        return 14;
    }
    public static long and(long x) {
        return x & -1;
    }
    public static long or(long x) {
        return x | 0;
    }
    public static long xor(long x) {
        return x ^ 0;
    }
}

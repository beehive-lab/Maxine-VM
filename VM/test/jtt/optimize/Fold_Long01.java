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
public class Fold_Long01 {
    public static long test(long arg) {
        if (arg == 0) {
            return add();
        }
        if (arg == 1) {
            return sub();
        }
        if (arg == 2) {
            return mul();
        }
        if (arg == 3) {
            return div();
        }
        if (arg == 4) {
            return mod();
        }
        if (arg == 5) {
            return and();
        }
        if (arg == 6) {
            return or();
        }
        if (arg == 7) {
            return xor();
        }
        return 0;
    }
    public static long add() {
        long x = 3;
        return x + 7;
    }
    public static long sub() {
        long x = 15;
        return x - 4;
    }
    public static long mul() {
        long x = 6;
        return x * 2;
    }
    public static long div() {
        long x = 26;
        return x / 2;
    }
    public static long mod() {
        long x = 29;
        return x % 15;
    }
    public static long and() {
        long x = 31;
        return x & 15;
    }
    public static long or() {
        long x = 16;
        return x | 16;
    }
    public static long xor() {
        long x = 0;
        return x ^ 17;
    }
}

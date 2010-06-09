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
 * @Runs: 0=10; 1=11; 2=12; 3=13; 4=14; 5=15; 6=16; 7=17
 */
public class Reduce_Int02 {
    public static int test(int arg) {
        if (arg == 0) {
            return add(10);
        }
        if (arg == 1) {
            return sub();
        }
        if (arg == 2) {
            return mul(12);
        }
        if (arg == 3) {
            return div();
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
    public static int add(int x) {
        return 0 + x;
    }
    public static int sub() {
        return 11;
    }
    public static int mul(int x) {
        return 1 * x;
    }
    public static int div() {
        return 13;
    }
    public static int mod() {
        return 14;
    }
    public static int and(int x) {
        return -1 & x;
    }
    public static int or(int x) {
        return 0 | x;
    }
    public static int xor(int x) {
        return 0 ^ x;
    }
}

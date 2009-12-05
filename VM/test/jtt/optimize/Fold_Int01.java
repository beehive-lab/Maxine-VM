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
public class Fold_Int01 {
    public static int test(int arg) {
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
    public static int add() {
        int x = 3;
        return x + 7;
    }
    public static int sub() {
        int x = 15;
        return x - 4;
    }
    public static int mul() {
        int x = 6;
        return x * 2;
    }
    public static int div() {
        int x = 26;
        return x / 2;
    }
    public static int mod() {
        int x = 29;
        return x % 15;
    }
    public static int and() {
        int x = 31;
        return x & 15;
    }
    public static int or() {
        int x = 16;
        return x | 16;
    }
    public static int xor() {
        int x = 0;
        return x ^ 17;
    }

}

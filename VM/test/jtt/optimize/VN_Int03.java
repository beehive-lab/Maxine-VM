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
 * Tests value numbering of integer operations.
 * @Harness: java
 * @Runs: 0=6; 1=0; 2=36; 3=1; 4=0; 5=5; 6=7; 7=0
 */
public class VN_Int03 {
    private static boolean cond = true;

    public static int test(int arg) {
        if (arg == 0) {
            return add(arg);
        }
        if (arg == 1) {
            return sub(arg);
        }
        if (arg == 2) {
            return mul(arg);
        }
        if (arg == 3) {
            return div(arg);
        }
        if (arg == 4) {
            return mod(arg);
        }
        if (arg == 5) {
            return and(arg);
        }
        if (arg == 6) {
            return or(arg);
        }
        if (arg == 7) {
            return xor(arg);
        }
        return 0;
    }
    public static int add(int x) {
        int c = 3;
        int t = x + c;
        if (cond) {
            int u = x + c;
            return t + u;
        }
        return 0;
    }

    public static int sub(int x) {
        int c = 3;
        int t = x - c;
        if (cond) {
            int u = x - c;
            return t - u;
        }
        return 3;
    }
    public static int mul(int x) {
        int i = 3;
        int t = x * i;
        if (cond) {
            int u = x * i;
            return t * u;
        }
        return 3;
    }
    public static int div(int x) {
        int i = 9;
        int t = i / x;
        if (cond) {
            int u = i / x;
            return t / u;
        }
        return 9;
    }
    public static int mod(int x) {
        int i = 7;
        int t = i % x;
        if (cond) {
            int u = i % x;
            return t % u;
        }
        return 7;
    }
    public static int and(int x) {
        int i = 7;
        int t = i & x;
        if (cond) {
            int u = i & x;
            return t & u;
        }
        return 7;
    }
    public static int or(int x) {
        int i = 7;
        int t = i | x;
        if (cond) {
            int u = i | x;
            return t | u;
        }
        return 7;
    }
    public static int xor(int x) {
        int i = 7;
        int t = i ^ x;
        if (cond) {
            int u = i ^ x;
            return t ^ u;
        }
        return 7;
    }
}

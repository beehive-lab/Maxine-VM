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
 * Tests value numbering of long operations.
 * @Harness: java
 * @Runs: 0=6L; 1=0L; 2=36L; 3=1L; 4=0L; 5=5L; 6=7L; 7=0L
 */
public class VN_Long01 {
    public static long test(int arg) {
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
    public static long add(long x) {
        long t = x + 3;
        long u = x + 3;
        return t + u;
    }
    public static long sub(long x) {
        long t = x - 3;
        long u = x - 3;
        return t - u;
    }
    public static long mul(long x) {
        long t = x * 3;
        long u = x * 3;
        return t * u;
    }
    public static long div(long x) {
        long t = 9 / x;
        long u = 9 / x;
        return t / u;
    }
    public static long mod(long x) {
        long t = 7 % x;
        long u = 7 % x;
        return t % u;
    }
    public static long and(long x) {
        long t = 7 & x;
        long u = 7 & x;
        return t & u;
    }
    public static long or(long x) {
        long t = 7 | x;
        long u = 7 | x;
        return t | u;
    }
    public static long xor(long x) {
        long t = 7 ^ x;
        long u = 7 ^ x;
        return t ^ u;
    }
}

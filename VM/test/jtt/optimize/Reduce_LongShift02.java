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
 * Tests optimization of integer operations.
 * @Harness: java
 * @Runs: 0=80L; 1=11L; 2=12L; 3=13L; 4=64L
 */
public class Reduce_LongShift02 {
    public static long test(long arg) {
        if (arg == 0) {
            return shift0(arg + 80);
        }
        if (arg == 1) {
            return shift1(arg + 0x800000000000000aL);
        }
        if (arg == 2) {
            return shift2(arg + 192);
        }
        if (arg == 3) {
            return shift3(arg + 208);
        }
        if (arg == 4) {
            return shift4(arg);
        }
        return 0;
    }
    public static long shift0(long x) {
        return x >>> 3 << 3;
    }
    public static long shift1(long x) {
        return x << 3 >>> 3;
    }
    public static long shift2(long x) {
        return x >> 3 >> 1;
    }
    public static long shift3(long x) {
        return x >>> 3 >>> 1;
    }
    public static long shift4(long x) {
        return x << 3 << 1;
    }
}

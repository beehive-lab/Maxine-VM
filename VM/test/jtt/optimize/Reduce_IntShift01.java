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
 * @Runs: 0=10; 1=11; 2=12; 3=13; 4=14; 5=15
 */
public class Reduce_IntShift01 {
    public static int test(int arg) {
        if (arg == 0) {
            return shift0(arg + 10);
        }
        if (arg == 1) {
            return shift1(arg + 10);
        }
        if (arg == 2) {
            return shift2(arg + 10);
        }
        if (arg == 3) {
            return shift3(arg + 10);
        }
        if (arg == 4) {
            return shift4(arg + 10);
        }
        if (arg == 5) {
            return shift5(arg + 10);
        }
        return 0;
    }
    public static int shift0(int x) {
        return x >> 0;
    }
    public static int shift1(int x) {
        return x >>> 0;
    }
    public static int shift2(int x) {
        return x << 0;
    }
    public static int shift3(int x) {
        return x >> 64;
    }
    public static int shift4(int x) {
        return x >>> 64;
    }
    public static int shift5(int x) {
        return x << 64;
    }
}

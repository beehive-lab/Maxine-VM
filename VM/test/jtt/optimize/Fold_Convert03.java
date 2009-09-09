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
 * Tests constant folding of float conversions
 * @Harness: java
 * @Runs: 0=1024f; 1=-33f; 2=-78.1f
 */
public class Fold_Convert03 {
    public static float test(float arg) {
        if (arg == 0) {
            return i2f();
        }
        if (arg == 1) {
            return l2f();
        }
        if (arg == 2) {
            return d2f();
        }
        return  0;
    }
    public static float i2f() {
        int x = 1024;
        return x;
    }
    public static float l2f() {
        long x = -33;
        return x;
    }
    public static float d2f() {
        double x = -78.1d;
        return (float) x;
    }
}

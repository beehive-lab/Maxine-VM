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
 * Tests optimization of float operations.
 * @Harness: java
 * @Runs: 0d=22d; 1d=0d; 2d=144d; 3d=1d
 */
public class VN_Double02 {
    private static boolean cond = true;

    public static double test(double arg) {
        if (arg == 0) {
            return add(arg + 10);
        }
        if (arg == 1) {
            return sub(arg + 10);
        }
        if (arg == 2) {
            return mul(arg + 10);
        }
        if (arg == 3) {
            return div(arg + 10);
        }
        return 0;
    }
    public static double add(double x) {
        double c = 1.0d;
        double t = x + c;
        if (cond) {
            double u = x + c;
            return t + u;
        }
        return 1;
    }
    public static double sub(double x) {
        double c = 1.0d;
        double t = x - c;
        if (cond) {
            double u = x - c;
            return t - u;
        }
        return 1;
    }
    public static double mul(double x) {
        double c = 1.0d;
        double t = x * c;
        if (cond) {
            double u = x * c;
            return t * u;
        }
        return 1.0d;
    }
    public static double div(double x) {
        double c = 1.0d;
        double t = x / c;
        if (cond) {
            double u = x / c;
            return t / u;
        }
        return 1.0d;
    }
}
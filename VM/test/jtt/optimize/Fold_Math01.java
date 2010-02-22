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
 * @Harness: java
 * @Runs: 0=10d; 1=0.14943813247359922d; 2=0.9887710779360422d; 3=0.15113521805829508d; 4=0.04834938665190287d;
 * @Runs: 5=12.0d; 6=1.1474024528375417d; 7=-0.8239087409443188d; 8=106.62882057436371d; 9=1.1474024528375417d;
 * @Runs: 10=-1.0d; 11=2.0d; 12=42d
 */
public class Fold_Math01 {

    public static double test(int arg) {
        switch (arg) {
            case 0:
                return abs();
            case 1:
                return sin();
            case 2:
                return cos();
            case 3:
                return tan();
            case 4:
                return atan2();
            case 5:
                return sqrt();
            case 6:
                return log();
            case 7:
                return log10();
            case 8:
                return pow();
            case 9:
                return exp();
            case 10:
                return min();
            case 11:
                return max();
        }
        return 42;
    }

    private static double abs() {
        return Math.abs(-10.0d);
    }

    private static double sin() {
        return Math.sin(0.15d);
    }

    private static double cos() {
        return Math.cos(0.15d);
    }

    private static double tan() {
        return Math.tan(0.15d);
    }

    private static double atan2() {
        return Math.atan2(0.15d, 3.1d);
    }

    private static double sqrt() {
        return Math.sqrt(144d);
    }

    private static double log() {
        return Math.log(3.15d);
    }

    private static double log10() {
        return Math.log10(0.15d);
    }

    private static double pow() {
        return Math.pow(2.15d, 6.1d);
    }

    private static double exp() {
        return Math.log(3.15d);
    }

    private static int min() {
        return Math.min(2, -1);
    }

    private static int max() {
        return Math.max(2, -1);
    }
}

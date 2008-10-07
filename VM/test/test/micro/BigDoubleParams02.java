/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
/*VCSID=4a352e0d-cbe0-4ed9-8432-eba2010a7384*/
package test.micro;

/*
 * @Harness: java
 * @Runs: (0, 1d, 2d, 3d, 4d, 5d, 6d, 7d, 8d, 9d) = 1d;
 * @Runs: (1, 1d, 2d, 3d, 4d, 5d, 6d, 7d, 8d, 9d) = 2d;
 * @Runs: (2, 1d, 2d, 3d, 4d, 5d, 6d, 7d, 8d, 9d) = 3d;
 * @Runs: (3, 1d, 2d, 3d, 4d, 5d, 6d, 7d, 8d, 9d) = 4d;
 * @Runs: (4, 1d, 2d, 3d, 4d, 5d, 6d, 7d, 8d, 9d) = 5d;
 * @Runs: (5, 1d, 2d, 3d, 4d, 5d, 6d, 7d, 8d, 9d) = 6d;
 * @Runs: (6, 1d, 2d, 3d, 4d, 5d, 6d, 7d, 8d, 9d) = 7d;
 * @Runs: (7, 1d, 2d, 3d, 4d, 5d, 6d, 7d, 8d, 9d) = 8d;
 * @Runs: (8, 1d, 2d, 3d, 4d, 5d, 6d, 7d, 8d, 9d) = 9d
 */
public class BigDoubleParams02 {
    public static double test(int choice, double p0, double p1, double p2, double p3, double p4, double p5, double p6, double p7, double p8) {
        switch (choice) {
            case 0: return p0;
            case 1: return p1;
            case 2: return p2;
            case 3: return p3;
            case 4: return p4;
            case 5: return p5;
            case 6: return p6;
            case 7: return p7;
            case 8: return p8;
        }
        return 42;
    }
}

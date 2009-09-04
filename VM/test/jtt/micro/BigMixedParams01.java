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
/*
 * Copyright (c) 2007 Sun Microsystems, Inc. All rights reserved. Use is subject to license terms.
 */
package jtt.micro;

/*
 * @Harness: java
 * @Runs: 0=45D; 1=45D; 2=45D; 3=45D; 4=0D
 */
public class BigMixedParams01 {
    public static double test(int num) {
        double sum = 0;
        if (num == 0) {
            sum += testA(0, -1, -1, -1, -1, 1f, 2f, 3f, 4f, -1, -1, 5f, 6f, 7f, 8f, 9f);
            sum += testA(1, -1, -1, -1, -1, 1f, 2f, 3f, 4f, -1, -1, 5f, 6f, 7f, 8f, 9f);
            sum += testA(2, -1, -1, -1, -1, 1f, 2f, 3f, 4f, -1, -1, 5f, 6f, 7f, 8f, 9f);
            sum += testA(3, -1, -1, -1, -1, 1f, 2f, 3f, 4f, -1, -1, 5f, 6f, 7f, 8f, 9f);
            sum += testA(4, -1, -1, -1, -1, 1f, 2f, 3f, 4f, -1, -1, 5f, 6f, 7f, 8f, 9f);
            sum += testA(5, -1, -1, -1, -1, 1f, 2f, 3f, 4f, -1, -1, 5f, 6f, 7f, 8f, 9f);
            sum += testA(6, -1, -1, -1, -1, 1f, 2f, 3f, 4f, -1, -1, 5f, 6f, 7f, 8f, 9f);
            sum += testA(7, -1, -1, -1, -1, 1f, 2f, 3f, 4f, -1, -1, 5f, 6f, 7f, 8f, 9f);
            sum += testA(8, -1, -1, -1, -1, 1f, 2f, 3f, 4f, -1, -1, 5f, 6f, 7f, 8f, 9f);
        } else if (num == 1) {
            sum += testB(0, -1, -1, -1, 1d, 2d, 3d, 4d, -1, -1, 5d, 6d, 7d, 8d, 9d);
            sum += testB(1, -1, -1, -1, 1d, 2d, 3d, 4d, -1, -1, 5d, 6d, 7d, 8d, 9d);
            sum += testB(2, -1, -1, -1, 1d, 2d, 3d, 4d, -1, -1, 5d, 6d, 7d, 8d, 9d);
            sum += testB(3, -1, -1, -1, 1d, 2d, 3d, 4d, -1, -1, 5d, 6d, 7d, 8d, 9d);
            sum += testB(4, -1, -1, -1, 1d, 2d, 3d, 4d, -1, -1, 5d, 6d, 7d, 8d, 9d);
            sum += testB(5, -1, -1, -1, 1d, 2d, 3d, 4d, -1, -1, 5d, 6d, 7d, 8d, 9d);
            sum += testB(6, -1, -1, -1, 1d, 2d, 3d, 4d, -1, -1, 5d, 6d, 7d, 8d, 9d);
            sum += testB(7, -1, -1, -1, 1d, 2d, 3d, 4d, -1, -1, 5d, 6d, 7d, 8d, 9d);
            sum += testB(8, -1, -1, -1, 1d, 2d, 3d, 4d, -1, -1, 5d, 6d, 7d, 8d, 9d);
        } else if (num == 2) {
            for (int i = 0; i < 9; i++) {
                sum += testA(i, -1, -1, -1, -1, 1f, 2f, 3f, 4f, -1, -1, 5f, 6f, 7f, 8f, 9f);
            }
        } else if (num == 3) {
            for (int i = 0; i < 9; i++) {
                sum += testB(i, -1, -1, -1, 1d, 2d, 3d, 4d, -1, -1, 5d, 6d, 7d, 8d, 9d);
            }
        }
        return sum;
    }

    private static float testA(int choice, int i0, int i1, int i2, int i3, float p0, float p1, float p2, float p3, int i4, int i5, float p4, float p5, float p6, float p7, float p8) {
        switch (choice) {
            case 0:
                return p0;
            case 1:
                return p1;
            case 2:
                return p2;
            case 3:
                return p3;
            case 4:
                return p4;
            case 5:
                return p5;
            case 6:
                return p6;
            case 7:
                return p7;
            case 8:
                return p8;
        }
        return 42;
    }

    private static double testB(int choice, int i0, int i1, int i2, double p0, double p1, double p2, double p3, int i3, int i4, double p4, double p5, double p6, double p7, double p8) {
        switch (choice) {
            case 0:
                return p0;
            case 1:
                return p1;
            case 2:
                return p2;
            case 3:
                return p3;
            case 4:
                return p4;
            case 5:
                return p5;
            case 6:
                return p6;
            case 7:
                return p7;
            case 8:
                return p8;
        }
        return 42;
    }
}

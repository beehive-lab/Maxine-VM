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
/*
 * @Harness: java
 * @Runs: 0=45; 1=45; 2=45; 3=45; 4=0
 */
package jtt.micro;

public class BigIntParams01 {

    public static int test(int num) {
        int sum = 0;
        if (num == 0) {
            sum += testA(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
            sum += testA(1, 1, 2, 3, 4, 5, 6, 7, 8, 9);
            sum += testA(2, 1, 2, 3, 4, 5, 6, 7, 8, 9);
            sum += testA(3, 1, 2, 3, 4, 5, 6, 7, 8, 9);
            sum += testA(4, 1, 2, 3, 4, 5, 6, 7, 8, 9);
            sum += testA(5, 1, 2, 3, 4, 5, 6, 7, 8, 9);
            sum += testA(6, 1, 2, 3, 4, 5, 6, 7, 8, 9);
            sum += testA(7, 1, 2, 3, 4, 5, 6, 7, 8, 9);
            sum += testA(8, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        } else if (num == 1) {
            sum += testB(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
            sum += testB(1, 1, 2, 3, 4, 5, 6, 7, 8, 9);
            sum += testB(2, 1, 2, 3, 4, 5, 6, 7, 8, 9);
            sum += testB(3, 1, 2, 3, 4, 5, 6, 7, 8, 9);
            sum += testB(4, 1, 2, 3, 4, 5, 6, 7, 8, 9);
            sum += testB(5, 1, 2, 3, 4, 5, 6, 7, 8, 9);
            sum += testB(6, 1, 2, 3, 4, 5, 6, 7, 8, 9);
            sum += testB(7, 1, 2, 3, 4, 5, 6, 7, 8, 9);
            sum += testB(8, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        } else if (num == 2) {
            for (int i = 0; i < 9; i++) {
                sum += testA(i, 1, 2, 3, 4, 5, 6, 7, 8, 9);
            }
        } else if (num == 3) {
            for (int i = 0; i < 9; i++) {
                sum += testB(i, 1, 2, 3, 4, 5, 6, 7, 8, 9);
            }
        }
        return sum;
    }

    private static int testA(int choice, int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) {
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

    private static long testB(int choice, long p0, long p1, long p2, long p3, long p4, long p5, long p6, long p7, long p8) {
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

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
/*VCSID=7a8b9f17-2560-4978-9207-f9cbab8e2735*/

package test.micro;

/*
 * @Harness: java
 * @Runs: (0, 1, 2, 3, 4, 5, 6, 7, -8, -9) = 1;
 * @Runs: (1, 1, 2, 3, 4, 5, 6, 7, -8, -9) = 2;
 * @Runs: (2, 1, 2, 3, 4, 5, 6, 7, -8, -9) = 3;
 * @Runs: (3, 1, 2, 3, 4, 5, 6, 7, -8, -9) = 4;
 * @Runs: (4, 1, 2, 3, 4, 5, 6, 7, -8, -9) = 5;
 * @Runs: (5, 1, 2, 3, 4, 5, 6, 7, -8, -9) = 6;
 * @Runs: (6, 1, 2, 3, 4, 5, 6, 7, -8, -9) = 7;
 * @Runs: (7, 1, 2, 3, 4, 5, 6, 7, -8, -9) = -8;
 * @Runs: (8, 1, 2, 3, 4, 5, 6, 7, -8, -9) = -9
 */
public class BigIntParams02 {
    public static int test(int choice, int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) {
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

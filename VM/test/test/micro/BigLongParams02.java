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
/*VCSID=11aa2704-39b1-4651-8bb9-1ae89eec758c*/

package test.micro;

/*
 * @Harness: java
 * @Runs: (0, 1L, 2L, 3L, 4L, 5L, 6L, 7L, -8L, -9L) = 1L;
 * @Runs: (1, 1L, 2L, 3L, 4L, 5L, 6L, 7L, -8L, -9L) = 2L;
 * @Runs: (2, 1L, 2L, 3L, 4L, 5L, 6L, 7L, -8L, -9L) = 3L;
 * @Runs: (3, 1L, 2L, 3L, 4L, 5L, 6L, 7L, -8L, -9L) = 4L;
 * @Runs: (4, 1L, 2L, 3L, 4L, 5L, 6L, 7L, -8L, -9L) = 5L;
 * @Runs: (5, 1L, 2L, 3L, 4L, 5L, 6L, 7L, -8L, -9L) = 6L;
 * @Runs: (6, 1L, 2L, 3L, 4L, 5L, 6L, 7L, -8L, -9L) = 7L;
 * @Runs: (7, 1L, 2L, 3L, 4L, 5L, 6L, 7L, -8L, -9L) = -8L;
 * @Runs: (8, 1L, 2L, 3L, 4L, 5L, 6L, 7L, -8L, -9L) = -9L
 */
public class BigLongParams02 {
    public static long test(int choice, long p0, long p1, long p2, long p3, long p4, long p5, long p6, long p7, long p8) {
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

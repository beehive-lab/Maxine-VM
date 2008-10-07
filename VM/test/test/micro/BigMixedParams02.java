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
/*VCSID=0d8f512e-24f9-42aa-b2ea-1e23aab277dd*/
/*
 * Copyright (c) 2007 Sun Microsystems, Inc. All rights reserved. Use is subject to license terms.
 */
package test.micro;

/*
 * @Harness: java
 * @Runs: (0, -1, -1, -1, -1, 1f, 2f, 3f, 4f, -1, -1, 5f, 6f, 7f, 8f, 9f) = 1f;
 * @Runs: (1, -1, -1, -1, -1, 1f, 2f, 3f, 4f, -1, -1, 5f, 6f, 7f, 8f, 9f) = 2f;
 * @Runs: (2, -1, -1, -1, -1, 1f, 2f, 3f, 4f, -1, -1, 5f, 6f, 7f, 8f, 9f) = 3f;
 * @Runs: (3, -1, -1, -1, -1, 1f, 2f, 3f, 4f, -1, -1, 5f, 6f, 7f, 8f, 9f) = 4f;
 * @Runs: (4, -1, -1, -1, -1, 1f, 2f, 3f, 4f, -1, -1, 5f, 6f, 7f, 8f, 9f) = 5f;
 * @Runs: (5, -1, -1, -1, -1, 1f, 2f, 3f, 4f, -1, -1, 5f, 6f, 7f, 8f, 9f) = 6f;
 * @Runs: (6, -1, -1, -1, -1, 1f, 2f, 3f, 4f, -1, -1, 5f, 6f, 7f, 8f, 9f) = 7f;
 * @Runs: (7, -1, -1, -1, -1, 1f, 2f, 3f, 4f, -1, -1, 5f, 6f, 7f, 8f, 9f) = 8f;
 * @Runs: (8, -1, -1, -1, -1, 1f, 2f, 3f, 4f, -1, -1, 5f, 6f, 7f, 8f, 9f) = 9f;
 */
public class BigMixedParams02 {

    public static float test(int choice, int i0, int i1, int i2, int i3, float p0, float p1, float p2, float p3, int i4, int i5, float p4, float p5, float p6, float p7, float p8) {
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

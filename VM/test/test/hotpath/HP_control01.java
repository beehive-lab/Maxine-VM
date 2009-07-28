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
package test.hotpath;
/*
 * @Harness: java
 * @Runs: 40 = 2432; 80 = 3243;
 */
public class HP_control01 {
    public static int test(int count) {
        int i1 = 1;
        int i2 = 2;
        int i3 = 3;
        int i4 = 4;

        for (int i = 0; i < count; i++) {
            i1 = i2;
            i2 = i3;
            i3 = i4;
            i4 = i1;

            i1 = i2;
            i2 = i3;
            i3 = i4;
            i4 = i1;
            i1 = i2;
            i2 = i3;
            i3 = i4;
            i4 = i1;
            i1 = i2;
            i2 = i3;
            i3 = i4;
            i4 = i1;
        }

        return i1 + i2 * 10 + i3 * 100 + i4 * 1000;
    }
}

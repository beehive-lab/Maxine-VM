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
package jtt.micro;

/*
 * @Harness: java
 * @Runs: 0 = true; 1 = false; 2 = true; 3 = false
 */
public class ArrayCompare02 {
    static final long[] a1 = {1, 1, 1, 1, 1, 1};
    static final long[] a2 = {1, 1, 1, 2, 1, 1};
    static final long[] a3 = {1, 1, 2, 2, 3, 3};

    public static boolean test(int arg) {
        if (arg == 0) {
            return compare(a1);
        }
        if (arg == 1) {
            return compare(a2);
        }
        if (arg == 2) {
            return compare(a3);
        }
        return false;
    }

    static boolean compare(long[] a) {
        return a[0] == a[1] & a[2] == a[3] & a[4] == a[5];
    }
}

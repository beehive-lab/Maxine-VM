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
 * @Harness: java
 * @Runs: 0 = !java.lang.NullPointerException;
 * @Runs: 1 = 3; 2 = 4; 3 = 5; 4 = 42
 */
package test.except;

public class BC_arraylength {

    static int[] _array1 = {1, 2, 3};
    static char[] _array2 = {'a', 'b', 'c', 'd'};
    static Object[] _array3 = new Object[5];

    public static int test(int arg) {
        if (arg == 0) {
            final int[] array = null;
            return array.length;
        }
        if (arg == 1) {
            return _array1.length;
        }
        if (arg == 2) {
            return _array2.length;
        }
        if (arg == 3) {
            return _array3.length;
        }
        return 42;
    }
}

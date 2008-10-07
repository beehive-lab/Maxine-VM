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
package test.bytecode;

/*
 * @Harness: java
 * @Runs: (true, 0) = 0; (true, 1) = 1; (true, 2) = 2; (false, 1) = 1; (false, 2) = 2
 */
public class BC_aastore {

    static Object[] _param = {new Object(), null, "h"};
    static Object[] _array1 = {null, null, null};
    static String[] _array2 = {null, null, null};

    public static int test(boolean a, int indx) {
        Object[] array = a ? _array1 : _array2;
        Object val;
        val = _param[indx];
        array[indx] = val;
        return indx;
    }

}

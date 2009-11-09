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
package jtt.except;

/*
 * @Harness: java
 * @Runs: (true, -2) = 5; (true, -1) = !java.lang.ArrayIndexOutOfBoundsException;
 * @Runs: (true, 0) = 0; (true, 1) = 1; (true, 2) = 2; (true, 3) = !java.lang.ArrayIndexOutOfBoundsException;
 * @Runs: (false, 0) = !java.lang.ArrayStoreException; (false, 1) = 1; (false, 2) = 2; (false, 3) = !java.lang.ArrayIndexOutOfBoundsException
 */
public class BC_aastore1 {
    static Object[] param = {new Object(), null, "h"};
    static Object[] arr = {null, null, null};
    static String[] arr2 = {null, null, null};

    public static int test(boolean a, int indx) {
        try {
            Object[] array = a ? arr : arr2;
            Object val;
            if (indx == -2) {
                array = null;
                val = null;
            } else {
                val = param[indx];
            }
            array[indx] = val;
            return indx;
        } catch (NullPointerException e) {
            return 5;
        }
    }

}

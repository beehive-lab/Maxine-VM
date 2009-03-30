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
 * @Runs: 0 = 3; 1 = 4; 2 = 1; 3 = !java.lang.NullPointerException
 */
package test.reflect;

import java.lang.reflect.*;

public class Array_getLength01 {
    private static final int[] array0 = { 11, 21, 42 };
    private static final boolean[] array1 = { true, true, false, false };
    private static final String[] array2 = { "String" };
    public static int test(int i) {
        Object array = null;
        if (i == 0) {
            array = array0;
        } else if (i == 1) {
            array = array1;
        } else if (i == 2) {
            array = array2;
        }
        return Array.getLength(array);
    }
}


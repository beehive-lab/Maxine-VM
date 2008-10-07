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
/*VCSID=26aa7278-34ac-41e4-9052-a27f11445d7c*/
/*
 * @Harness: java
 * @Runs: (0, true) = true; (1, false) = false; (2, true) = true; (3, false) = !java.lang.ArrayIndexOutOfBoundsException
 */
package test.reflect;

import java.lang.reflect.*;

public class Array_setBoolean01 {
    private static final boolean[] array = { false, false, false };
    public static boolean test(int i, boolean value) {
        Array.setBoolean(array, i, value);
        return array[i];
    }
}


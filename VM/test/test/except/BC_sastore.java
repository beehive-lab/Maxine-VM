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
 * @Runs: (-2, 0s) = !java.lang.NullPointerException; (-1, 3s) = !java.lang.ArrayIndexOutOfBoundsException;
 * @Runs: (0, 0s) = 0s;
 * @Runs: (4, 0s) = !java.lang.ArrayIndexOutOfBoundsException
 */
package test.except;

public class BC_sastore {

    static short[] _array = {0, 0, 0, 0};

    public static short test(int arg, short val) {
        final short[] array = arg == -2 ? null : _array;
        array[arg] = val;
        return array[arg];
    }
}

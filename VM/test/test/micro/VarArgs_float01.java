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
/*VCSID=bc374bae-8cc7-4970-87a7-ac2b7e34b98e*/
package test.micro;


/*
 * @Harness: java
 * @Runs: 0=0.0f; 1=1.0f; 2=2.0f; 3=!java.lang.ArrayIndexOutOfBoundsException;
 * @Runs: 4=!java.lang.ArrayIndexOutOfBoundsException
 */
public class VarArgs_float01 {
    public static float test(int arg) {
        if (arg == 4) {
            return get(0);
        }
        return get(arg, 0.0f, 1.0f, 2.0f);
    }

    private static float get(int index, float... args) {
        return args[index];
    }
}

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
/*VCSID=778e10db-93c7-4a0a-97ae-d2274b619756*/
package test.micro;


/*
 * @Harness: java
 * @Runs: 0="a"; 1=null; 2="test"; 3=!java.lang.ArrayIndexOutOfBoundsException;
 * @Runs: 4=!java.lang.ArrayIndexOutOfBoundsException
 */
public class VarArgs_String01 {
    public static String test(int arg) {
        if (arg == 4) {
            return get(0);
        }
        return get(arg, "a", null, "test");
    }

    private static String get(int index, String... args) {
        return args[index];
    }
}

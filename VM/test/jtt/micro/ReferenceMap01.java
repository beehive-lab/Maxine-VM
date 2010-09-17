/*
 * Copyright (c) 2010 Sun Microsystems, Inc.  All rights reserved.
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
 * @Runs: null=7878D
 */
public class ReferenceMap01 {

    public static Integer val1 = new Integer(3);
    public static Integer val2 = new Integer(4);

    private static String test(String[] args) {
        args = new String[] {"78"};
        Integer i1 = new Integer(1);
        Integer i2 = new Integer(2);
        Integer i3 = val1;
        Integer i4 = val2;
        Integer i5 = new Integer(5);
        Integer i6 = new Integer(6);
        Integer i7 = new Integer(7);
        Integer i8 = new Integer(8);
        Integer i9 = new Integer(9);
        Integer i10 = new Integer(10);
        Integer i11 = new Integer(11);
        Integer i12 = new Integer(12);

        System.gc();
        int sum = i1 + i2 + i3 + i4 + i5 + i6 + i7 + i8 + i9 + i10 + i11 + i12;
        return args[0] + sum;
    }

    public static int test(int num) {
        return Integer.valueOf(test(new String[] {"asdf"}));
    }

}

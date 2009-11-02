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
package test.output;

import com.sun.max.annotate.UNSAFE;

/**
 * GC Test for large number of object parameters.
 *
 * @author Michael Bebenita
 * @author Doug Simon
 * @author Ben L. Titzer
 */
public class GCTest7 {
    public static void main(String[] args) {
        String a0 = "a" + 0;
        String a1 = "a" + 1;
        String a2 = "a" + 2;
        String a3 = "a" + 3;
        String a4 = "a" + 4;
        String a5 = "a" + 5;
        String a6 = "a" + 6;
        String a7 = "a" + 7;
        String a8 = "a" + 8;
        String a9 = "a" + 9;
        String b0 = "b" + 0;
        String b1 = "b" + 1;
        String b2 = "b" + 2;
        String b3 = "b" + 3;
        String b4 = "b" + 4;
        String b5 = "b" + 5;
        String b6 = "b" + 6;
        String b7 = "b" + 7;
        String b8 = "b" + 8;
        String b9 = "b" + 9;


        Object result = objStackParams(16, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9,
                                           b0, b1, b2, b3, b4, b5, b6, b7, b8, b9);
        if (result == b9) {
            System.out.println("OK!");
        }
    }

    @UNSAFE
    private static Object objStackParams(int depth,
                                         String a0, String a1, String a2, String a3, String a4,
                                         String a5, String a6, String a7, String a8, String a9,
                                         String b0, String b1, String b2, String b3, String b4,
                                         String b5, String b6, String b7, String b8, String b9) {
        if (depth > 0) {
            System.gc();
            System.out.println(a0 + a1 + a2 + a3 + a4 + a5 + a6 + a7 + a8 + a9);
            System.out.println(b0 + b1 + b2 + b3 + b4 + b5 + b6 + b7 + b8 + b9);
            return objStackParams(depth - 1, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9,
                                           b0, b1, b2, b3, b4, b5, b6, b7, b8, b9);
        }
        return b9;
    }
}

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

package jtt.bytecode;

/*
 * @Harness: java
 * @Runs: 0 = -9223372036854775808L, 1 = -9223372036854775808L, 2 = 0L, 3 = 9223372036854775807L; 4 = 9223372036854775807L
 */
public class BC_d2l02 {

    private static double[] inputs = {-1.3e44d, Double.NEGATIVE_INFINITY, Double.NaN, Double.POSITIVE_INFINITY, 1.3e44d};

    public static long test(int i) {
        return (long) inputs[i];
    }
}

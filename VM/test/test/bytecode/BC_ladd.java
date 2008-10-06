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
/*VCSID=20286b9f-76d5-41df-bae5-4f8b220f9528*/
package test.bytecode;

/*
 * @Harness: java
 * @Runs: (1L, 2L) = 3L; (0L, -1L) = -1L; (33L, 67L) = 100L; (1L, -1L) = 0L;
 * @Runs: (-2147483648L, 1L) = -2147483647L; (2147483647L, 1L) = 2147483648L
 * @Runs: (-2147483647L,-2L) = -2147483649L
 */
public class BC_ladd {
    public static long test(long a, long b) {
        return a + b;
    }
}

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
package jtt.lang;

/*
 * @Harness: java
 * @Runs: 0x1122334455667708L = 0x877665544332211L
 */
public class Long_reverseBytes02 {
    public static long test(long val) {
        return (((val >> 56) & 0xff) << 0)
                | (((val >> 48) & 0xff) << 8)
                | (((val >> 40) & 0xff) << 16)
                | (((val >> 32) & 0xff) << 24)
                | (((val >> 24) & 0xff) << 32)
                | (((val >> 16) & 0xff) << 40)
                | (((val >> 8) & 0xff) << 48)
                | (((val >> 0) & 0xff) << 56);
    }
}

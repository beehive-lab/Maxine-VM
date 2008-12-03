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
 * Copyright (c) 2007 Sun Microsystems, Inc. All rights reserved. Use is subject to license terms.
 */
package test.except;

/*
 * @Harness: java
 * @Runs: -1 = !java.lang.NegativeArraySizeException; 0 = 0; 1 = 1
 */
public class BC_newarray {

    public static int test(int a) {
        if (new boolean[a] == null) {
            return -1;
        }
        if (new char[a] == null) {
            return -1;
        }
        if (new float[a] == null) {
            return -1;
        }
        if (new double[a] == null) {
            return -1;
        }
        if (new byte[a] == null) {
            return -1;
        }
        if (new short[a] == null) {
            return -1;
        }
        if (new int[a] == null) {
            return -1;
        }
        if (new long[a] == null) {
            return -1;
        }

        return a;
    }
}

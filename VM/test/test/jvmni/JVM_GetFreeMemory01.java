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
/*VCSID=8d4d87ac-0905-44ef-b49b-7fe39a79e694*/

package test.jvmni;

/*
 * @Harness: java
 * @Runs: 0 = true;
 */

public class JVM_GetFreeMemory01 {
    public static boolean test(int arg) {
        if (arg == 0) {
            final long mem = call();

            //TODO: come up with a better test
            // check if return value is between 0 and a terabyte
            return mem > 0 && mem <  1024L * 1024L * 1024L * 1024L;
        }

        return false;
    }

    public static native long call();

}

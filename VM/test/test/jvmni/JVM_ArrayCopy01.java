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
/*VCSID=6718d5c3-f29d-43ca-bab9-af4ac6733066*/

package test.jvmni;

/*
 * @Harness: java
 * @Runs: 0 = true
 */
public class JVM_ArrayCopy01 {
    public static boolean test(int arg) {
        final String[] src = {"1", "2", "3", "4", "5", "6"};
        final String[] dest = {"1", "2", "3", "4", " 5", "6"};

        call(src, 3, dest, 0, 3);
        if (dest[0].equals("4") && dest[1].equals("5") && dest[2].equals("6")) {
            return true;
        }
        return false;
    }

    private static native void call(Object src, int srcPos, Object dest, int destPos, int len);
}

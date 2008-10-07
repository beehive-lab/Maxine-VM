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
/*VCSID=3030015c-f1cb-4e7c-bb45-68740c0779de*/
package test.lang;

/*
 * @Harness: java
 * @Runs: 0 = true; 1 = true; 2 = true; 3 = false
 */
public class System_identityHashCode01 {
    private static final Object object0 = new Object();
    private static final Object object1 = new Object();
    private static final Object object2 = new Object();

    private static final int hash0 = System.identityHashCode(object0);
    private static final int hash1 = System.identityHashCode(object1);
    private static final int hash2 = System.identityHashCode(object2);

    public static boolean test(int i) {
        if (i == 0) {
            return hash0 == System.identityHashCode(object0);
        }
        if (i == 1) {
            return hash1 == System.identityHashCode(object1);
        }
        if (i == 2) {
            return hash2 == System.identityHashCode(object2);
        }
        return false;
    }
}

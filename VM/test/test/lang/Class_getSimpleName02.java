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
/*VCSID=d777f7ad-73a7-4651-900f-5459652d7cc4*/
/*
 * Copyright (c) 2007 Sun Microsystems, Inc. All rights reserved. Use is subject to license terms.
 */
/*
 * @Harness: java
 * @Runs: 0 = "int";
 * @Runs: 1 = "int[]";
 * @Runs: 2 = "Object[][]";
 * @Runs: 3 = null
 */
package test.lang;

public final class Class_getSimpleName02 {
    private Class_getSimpleName02() {
    }

    public static String test(int i) {
        if (i == 0) {
            return int.class.getSimpleName();
        }
        if (i == 1) {
            return int[].class.getSimpleName();
        }
        if (i == 2) {
            return Object[][].class.getSimpleName();
        }
        return null;
    }
}

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
/*VCSID=254623df-0645-4a24-9a02-6a36a447bf13*/
/*
 * Copyright (c) 2007 Sun Microsystems, Inc. All rights reserved. Use is subject to license terms.
 */
/*
 * @Harness: java
 * @Runs: 0 = "Object";
 * @Runs: 1 = "Class";
 * @Runs: 2 = "Class_getSimpleName01";
 * @Runs: 3 = null
 */
package test.lang;

public final class Class_getSimpleName01 {
    private Class_getSimpleName01() {
    }

    public static String test(int i) {
        if (i == 0) {
            return Object.class.getSimpleName();
        }
        if (i == 1) {
            return Class.class.getSimpleName();
        }
        if (i == 2) {
            return Class_getSimpleName01.class.getSimpleName();
        }
        return null;
    }
}

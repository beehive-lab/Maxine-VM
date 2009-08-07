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
 * @Harness: java
 * @Runs: 0 = "class java.lang.Object";
 * @Runs: 1 = "class java.lang.String";
 * @Runs: 2 = "class java.lang.Class";
 * @Runs: 3 = "class test.lang.Class_Literal01";
 * @Runs: 4 = null
 */
package jtt.lang;

public final class Class_Literal01 {
    private Class_Literal01() {
    }

    public static String test(int i) {
        if (i == 0) {
            return Object.class.toString();
        }
        if (i == 1) {
            return String.class.toString();
        }
        if (i == 2) {
            return Class.class.toString();
        }
        if (i == 3) {
            return Class_Literal01.class.toString();
        }
        return null;
    }
}

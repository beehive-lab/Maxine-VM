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
 * @Runs: 0 = null;
 * @Runs: 1 = null;
 * @Runs: 2 = "java.lang.Object";
 * @Runs: 3 = null;
 * @Runs: 4 = "java.lang.Number";
 * @Runs: 5 = "java.lang.Object";
 * @Runs: 6 = "java.lang.Object";
 * @Runs: 7 = null;
 */
package jtt.lang;


public final class Class_getSuperClass01 {
    private Class_getSuperClass01() {
    }

    public static String test(int i) {
        Class cl = Object.class;
        if (i == 0) {
            cl = int.class;
        } else if (i == 1) {
            cl = Object.class;
        } else if (i == 2) {
            cl = int[].class;
        } else if (i == 3) {
            cl = Cloneable.class;
        } else if (i == 4) {
            cl = Integer.class;
        } else if (i == 5) {
            cl = Class.class;
        } else if (i == 6) {
            cl = Class_getSuperClass01.class;
        }
        cl = cl.getSuperclass();
        if (cl == null) {
            return null;
        }
        return cl.getName();
    }
}

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
 * @Runs: 1 = "int";
 * @Runs: 2 = null;
 * @Runs: 3 = "java.lang.Object";
 * @Runs: 4 = null;
 * @Runs: 5 = null;
 * @Runs: 6 = "[Ljava.lang.Object;";
 * @Runs: 7 = null;
 * @Runs: 8 = null;
 */
package jtt.lang;

public final class Class_getComponentType01 {
    private Class_getComponentType01() {
    }

    public static String test(int i) {
        Class cl = Object.class;
        if (i == 0) {
            cl = int.class;
        } else if (i == 1) {
            cl = int[].class;
        } else if (i == 2) {
            cl = Object.class;
        } else if (i == 3) {
            cl = Object[].class;
        } else if (i == 4) {
            cl = Class_getComponentType01.class;
        } else if (i == 5) {
            cl = Cloneable.class;
        } else if (i == 6) {
            cl = Object[][].class;
        } else if (i == 7) {
            cl = void.class;
        }
        cl = cl.getComponentType();
        if (cl == null) {
            return null;
        }
        return cl.getName();
    }
}

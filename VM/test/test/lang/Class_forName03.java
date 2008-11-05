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
package test.lang;

import java.net.*;

/*
 * @Harness: java
 * @Runs: 0 = !java.lang.ClassNotFoundException;
 * @Runs: 1 = "class [Ljava.lang.String;";
 * @Runs: 2 = !java.lang.ClassNotFoundException;
 * @Runs: 3 = "class [I";
 * @Runs: 4 = !java.lang.ClassNotFoundException;
 * @Runs: 5 = null
 */
public final class Class_forName03 {
    private Class_forName03() {
    }

    public static String test(int i) throws ClassNotFoundException {
        String clname = null;
        Class cl = null;
        if (i == 0) {
            clname = "java.lang.Object[]";
            cl = Object.class;
        } else if (i == 1) {
            clname = "[Ljava.lang.String;";
            cl = String.class;
        } else if (i == 2) {
            clname = "[Ljava/lang/String;";
            cl = String.class;
        } else if (i == 3) {
            clname = "[I";
            cl = Class_forName03.class;
        } else if (i == 4) {
            clname = "[java.lang.Object;";
            cl = Class_forName03.class;
        }
        if (clname != null) {
            return Class.forName(clname, false, new URLClassLoader(new URL[0], cl.getClassLoader())).toString();
        }
        return null;
    }
}

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
package jtt.lang;

import java.net.*;

/*
 * @Harness: java
 * @Runs: 0 = "class java.lang.String";
 * @Runs: 1 = !java.lang.ClassNotFoundException;
 * @Runs: 2 = !java.lang.ClassNotFoundException";
 * @Runs: 5 = null
 */
public final class ClassLoader_loadClass01 {
    private ClassLoader_loadClass01() {
    }

    public static String test(int i) throws ClassNotFoundException {
        final URLClassLoader classLoader = new URLClassLoader(new URL[0], String.class.getClassLoader());
        if (i == 0) {
            return classLoader.loadClass("java.lang.String").toString();
        } else if (i == 1) {
            return classLoader.loadClass("[Ljava.lang.String;").toString();
        } else if (i == 2) {
            return classLoader.loadClass("java.lang.String[]").toString();
        }
        return null;
    }
}

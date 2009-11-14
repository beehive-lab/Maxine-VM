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
package test.output;

import java.lang.reflect.*;

/**
 * This is a test case used in the automated testing framework. This program is automatically
 * run on both a standard JVM (e.g. Hotspot), and the Maxine VM, and the resulting output
 * is compared directly.
 */
public class HelloWorldReflect {
    public static void main(String[] args) {
        try {
            final ClassLoader thisClassLoader = HelloWorldReflect.class.getClassLoader();
            final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

            System.out.println("thisClassLoader == systemClassLoader? " + (thisClassLoader == systemClassLoader));

            invokeHellos(args, thisClassLoader);
            invokeHellos(args, systemClassLoader);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void invokeHellos(String[] args, final ClassLoader classLoader) throws NoSuchMethodException, ClassNotFoundException, IllegalAccessException, InvocationTargetException {
        java.lang.reflect.Method m;
        m = classLoader.loadClass("test.output.Hello1").getMethod("main", String[].class);
        m.invoke(null, (Object) args);

        m = classLoader.loadClass("test.output.Hello2").getMethod("main", String[].class);
        m.invoke(null, (Object) args);
    }
}

/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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

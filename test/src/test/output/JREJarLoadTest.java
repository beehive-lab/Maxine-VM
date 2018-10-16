/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.net.*;

public class JREJarLoadTest {
    public static void main(String[] args) {
        testLoad("com.sun.tools.javac.api.JavacTool", "tools.jar");
    }

    private static void testLoad(String name, String jarName) {
        System.out.println(name + " in " + jarName + " = " + loadClass(name, jarName));
    }

    private static Class loadClass(String name, String jarName) {
        File file = new File(System.getProperty("java.home"));
        if (file.getName().equalsIgnoreCase("jre")) {
            file = file.getParentFile();
        }
        final String[] location = {"lib", jarName};
        for (String n : location) {
            file = new File(file, n);
        }
        try {
            final URL[] urls = {file.toURI().toURL()};
            final ClassLoader cl = URLClassLoader.newInstance(urls);
            return Class.forName(name, false, cl);
        } catch (MalformedURLException e) {
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

}

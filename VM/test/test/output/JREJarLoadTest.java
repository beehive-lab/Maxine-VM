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

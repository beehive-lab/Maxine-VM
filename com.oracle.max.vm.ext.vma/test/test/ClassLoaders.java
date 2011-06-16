/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package test;

import java.io.*;

/**
 * A test for classes loaded by different classloaders. Since this class and
 * {@link test.New0} are located in the same package the <code>test.New0</code>
 * will always ultimately be found by {@link ClassLoader# findSystemClass}, but
 * in that case the two instances of <code>test.New0</code> will be loaded by
 * the system class loader. This will cause the assertion check to fail.
 *
 * To get the real effect of the test you must set the <code>-cp</code> argument
 * to a different directory containing the <code>test.New0</code> classes. Then the custom
 * class loaders {@link CLA} and {@link CLB} will load the class from there and
 * not delegate to the system class loader.
 *
 * @author Mick Jordan
 *
 */

public class ClassLoaders {

    private static String classpath;

    static class CLA extends ClassLoader {
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            FileInputStream is = null;
            try {
                final File file = new File(classpath, name.replace('.', '/') + ".class");
                int size = (int) file.length();
                byte[] data = new byte[size];
                is = new FileInputStream(file);
                assert is.read(data) == size;
                return defineClass(name, data, 0, size);
            } catch (Exception ex) {
                throw new ClassNotFoundException(name);
            }
        }

        @Override
        protected Class< ? > loadClass(String name, boolean resolve) throws ClassNotFoundException {
            // no delegation
            Class< ? > c = findLoadedClass(name);
            if (c == null) {
                try {
                    c = findClass(name);
                } catch (ClassNotFoundException e) {
                    c = findSystemClass(name);
                }
                if (c == null) {
                    throw new ClassNotFoundException(name);
                }
                if (resolve) {
                    resolveClass(c);
                }
            }
            return c;
        }
    }

    static class CLB extends CLA {

    }

    public static void main(String[] args) throws Exception {
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("-cp")) {
                classpath = args[++i];
            }
        }
        // Checkstyle: resume modified control variable check
        if (classpath == null) {
            System.out.println("must set -cp to location of test.New0.class");
            System.exit(1);
        }
        CLA cla = new CLA();
        CLB clb = new CLB();
        Class<?> claAclass = cla.loadClass("test.New0");
        assert claAclass.getClassLoader() == cla;
        Class<?> clbAclass = clb.loadClass("test.New0");
        assert clbAclass.getClassLoader() == clb;
        @SuppressWarnings("unused")
        Object claA = claAclass.getDeclaredConstructor(int.class).newInstance(5);
        @SuppressWarnings("unused")
        Object clbA = clbAclass.getDeclaredConstructor(int.class).newInstance(5);
    }

}

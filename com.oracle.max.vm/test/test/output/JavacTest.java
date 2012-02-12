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

import java.io.*;
import java.lang.reflect.*;
import java.net.*;

import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;

public class JavacTest {
    public static void main(String[] args) throws Exception {
        final JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        if (javaCompiler == null) {
            System.out.println("Could not find system Java compiler.");
            return;
        }
        final StandardJavaFileManager fileManager = javaCompiler.getStandardFileManager(null, null, null);
        Iterable<? extends JavaFileObject> fileObjects;
        File tempSourceFile = null;
        if (args.length == 0) {
            // the user didn't specify any files. compile this source file.
            tempSourceFile = createTempSourceFile();
            fileObjects = fileManager.getJavaFileObjects(tempSourceFile);
        } else {
            // the user specified a list of files
            final File[] files = new File[args.length];
            for (int i = 0; i < args.length; i++) {
                files[i] = new File(args[i]);
            }
            fileObjects = fileManager.getJavaFileObjects(files);
        }
        final CompilationTask task = javaCompiler.getTask(new PrintWriter(System.out), fileManager, null, null, null, fileObjects);
        task.call();
        if (tempSourceFile != null) {
            final String tempSourceFilePath = tempSourceFile.getPath();
            final String tempClassName = tempSourceFilePath.substring(0, tempSourceFilePath.length() - ".java".length());
            final String tempClassFilePath = tempClassName + ".class";
            final File tempClassFile = new File(tempClassFilePath);
            final URLClassLoader cl = URLClassLoader.newInstance(new URL[] {new File(".").toURI().toURL()});
            final Method method = cl.loadClass(tempClassName).getMethod("main", String[].class);
            method.invoke(null, (Object) new String[0]);
            tempSourceFile.delete();
            tempClassFile.delete();
        }
    }

    private static File createTempSourceFile()  throws IOException {
        final File qualifiedFile = new File("JavacTestInputSource.java");
        final PrintStream ps = new PrintStream(new FileOutputStream(qualifiedFile));
        ps.println("public class JavacTestInputSource {");
        ps.println("    public static void main(String[] args) {");
        ps.println("        System.out.println(\"Hello world from \" + JavacTestInputSource.class + '!');");
        ps.println("    }");
        ps.println("}");
        ps.close();
        return qualifiedFile;
    }
}

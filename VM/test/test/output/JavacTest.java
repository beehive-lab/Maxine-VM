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
import java.lang.reflect.*;
import java.net.*;

import javax.tools.*;
import javax.tools.JavaCompiler.*;

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

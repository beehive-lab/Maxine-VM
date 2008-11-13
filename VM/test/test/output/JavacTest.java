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

import javax.tools.*;
import javax.tools.JavaCompiler.*;


public class JavacTest {
    public static void main(String[] args) {
        final JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        if (javaCompiler == null) {
            System.out.println("Could not find system Java compiler.");
            return;
        }
        final StandardJavaFileManager fileManager = javaCompiler.getStandardFileManager(null, null, null);
        Iterable<? extends JavaFileObject> fileObjects;
        if (args.length == 0) {
            // the user didn't specify any files. compile this source file.
            final File thisSourceFile = findThisSourceFile();
            fileObjects = fileManager.getJavaFileObjects(thisSourceFile);
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
    }

    private static File findThisSourceFile() {
        final File qualifiedFile = new File(JavacTest.class.getName().replace('.', File.separatorChar) + ".java");
        return qualifiedFile;
    }
}

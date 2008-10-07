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
/*VCSID=f72f8803-e61f-42ba-9c1d-7f8b809c66b5*/
package com.sun.max.ide;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

import javax.tools.*;
import javax.tools.JavaCompiler.*;

import com.sun.max.collect.*;
import com.sun.max.program.*;

/**
 * Provides an interface for invoking a Java source compiler (e.g. javac) and a C header and stub generator (i.e.
 * javah). The Java source compiler used is determined the <a
 * href="http://java.sun.com/javase/6/docs/api/java/util/ServiceLoader.html">service provider mechanism</a>. For
 * example, if you want to use the Eclipse batch compiler, then simply place the stand alone ecj.jar JAR file on the
 * classpath.
 *
 * @author Doug Simon
 */
public final class ToolChain {

    private ToolChain() {
    }

    private static JavaCompiler _javaCompiler;

    private static JavaCompiler javaCompiler() {
        if (_javaCompiler == null) {
            final Iterator<JavaCompiler> iterator = ServiceLoader.load(JavaCompiler.class).iterator();
            if (iterator.hasNext()) {
                _javaCompiler = iterator.next();
            } else {
                _javaCompiler = ToolProvider.getSystemJavaCompiler();
            }
            ProgramError.check(_javaCompiler != null, "Cannot find a Java compiler");
        }
        return _javaCompiler;
    }

    /**
     * Compiles the source for a given class. The location of the source file to be compiled and the directory to which
     * the output class files are to be written are determined by the current {@link JavaProject} context.
     * <p>
     * The supported {@code options} are:
     * <p>
     *
     * <pre>
     *     -noinlinejsr    implement {@code finally} clauses using the {@link Bytecode#JSR} and {@link Bytecode#RET} bytecodes
     * </pre>
     *
     *
     * @param className the name of the class to be compiled
     * @param options options for modifying the compilation
     * @return true if the compilation succeeded without any errors, false otherwise
     */
    public static boolean compile(String className, String... options) {
        return compile(new String[] {className}, options);
    }

    /**
     * Compiles the source for one or more given classes. The location of the source files to be compiled and the
     * directory to which the output class files are to be written are determined by the current {@link JavaProject}
     * context.
     * <p>
     * The supported {@code options} are:
     * <p>
     *
     * <pre>
     *     -noinlinejsr    implement {@code finally} clauses using the {@link Bytecode#JSR} and {@link Bytecode#RET} bytecodes
     * </pre>
     *
     *
     * @param className the name of the class to be compiled
     * @param options options for modifying the compilation
     * @return true if the compilation succeeded without any errors, false otherwise
     */
    public static boolean compile(String[] classNames, String... options) {

        final Classpath classPath = JavaProject.getClassPath(true);
        final Classpath sourcePath = JavaProject.getSourcePath(true);
        final String outputDirectory = classPath.entries().first().toString();

        final AppendableSequence<File> sourceFiles = new ArrayListSequence<File>(classNames.length);
        for (String className : classNames) {
            final String sourceFilePathSuffix = className.replace('.', File.separatorChar) + ".java";
            final File sourceFile = sourcePath.findFile(sourceFilePathSuffix);
            if (sourceFile == null) {
                ProgramWarning.message("Could not find source file for " + className);
                return false;
            }
            sourceFiles.append(sourceFile);
        }

        final JavaCompiler compiler = javaCompiler();
        final String compilerName = compiler.getClass().getName();
        final AppendableSequence<String> opts = new ArrayListSequence<String>(new String[] {"-cp", classPath.toString(), "-d", outputDirectory});
        if (compilerName.equals("com.sun.tools.javac.api.JavacTool")) {
            AppendableSequence.Static.appendAll(opts, "-cp", classPath.toString(), "-d", outputDirectory);
            for (String option : options) {
                if (option.equals("-noinlinejsr")) {
                    opts.append("-source");
                    opts.append("1.4");
                    opts.append("-target");
                    opts.append("1.4");
                    opts.append("-XDjsrlimit=0");
                } else {
                    throw new IllegalArgumentException("Unsupported compiler option " + option);
                }
            }
        } else if (compiler.getClass().getName().equals("org.eclipse.jdt.internal.compiler.tool.EclipseCompiler")) {
            AppendableSequence.Static.appendAll(opts, "-cp", classPath.toString(), "-d", outputDirectory, "-noExit");
            boolean inlineJSR = true;
            for (String option : options) {
                if (option.equals("-noinlinejsr")) {
                    inlineJSR = false;
                } else {
                    throw new IllegalArgumentException("Unsupported compiler option " + option);
                }
            }
            if (inlineJSR) {
                opts.append("-inlineJSR");
            } else {
                opts.append("-source");
                opts.append("1.4");
                opts.append("-target");
                opts.append("1.4");
            }
        } else {
            ProgramWarning.message("Unknown Java compiler may not accept same command line options as javac: " + compilerName);
            AppendableSequence.Static.appendAll(opts, "-cp", classPath.toString(), "-d", outputDirectory);
        }

        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        final StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        final Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(sourceFiles);
        final CompilationTask task = compiler.getTask(null, fileManager, diagnostics, opts, null, compilationUnits);
        final boolean result = task.call();
        final Set<String> reportedDiagnostics = new HashSet<String>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            final String message = diagnostic.getMessage(Locale.getDefault());
            if (!reportedDiagnostics.contains(message)) {
                reportedDiagnostics.add(message);
                if (!message.contains("is Sun proprietary API and may be removed in a future release")) {
                    System.err.println(message);
                }
            }
        }
        try {
            fileManager.close();
        } catch (IOException e) {
            ProgramWarning.message("Error closing file manager: " + e);
        }
        return result;
    }

    private static Method _javahMainMethod;

    private static Method javah() {
        if (_javahMainMethod == null) {
            Class<?> javahMainClass = null;
            try {
                // On the Apple JDKs, there is no tools.jar: all the tools are in classes.jar
                javahMainClass = Class.forName("com.sun.tools.javah.Main");
            } catch (ClassNotFoundException classNotFoundException) {
                // This is expected on non-Apple JDKs
                final ClassLoader systemToolClassLoader = ToolProvider.getSystemToolClassLoader();
                ProgramError.check(systemToolClassLoader != null, "Cannot find the standard system tools class loader");
                try {
                    javahMainClass = Class.forName("com.sun.tools.javah.Main", true, systemToolClassLoader);
                    final URLClassLoader urlClassLoader = (URLClassLoader) javahMainClass.getClassLoader();
                    updateJavaClassPath(urlClassLoader);
                } catch (Exception exception) {
                    ProgramWarning.message("Cannot find or initialize javah: " + exception);
                }
            }
            try {
                if (javahMainClass != null) {
                    _javahMainMethod = javahMainClass.getDeclaredMethod("main", String[].class);
                }
            } catch (Exception exception) {
                ProgramWarning.message("Cannot find or initialize javah: " + exception);
            }
        }
        return _javahMainMethod;
    }

    /**
     * This hack is necessary as javah uses a doclet to do its actual work. The doclet class is found by creating a URL class loader
     * from the system property "java.class.path" which does not include the path to tools.jar for
     * standard JDK installations.
     */
    private static void updateJavaClassPath(final URLClassLoader urlClassLoader) {
        String javaClassPath = System.getProperty("java.class.path", ".");
        for (URL url : urlClassLoader.getURLs()) {
            final String path = url.getPath();
            if (path != null && !path.isEmpty()) {
                final File file = new File(path);
                if (file.exists()) {
                    javaClassPath += File.pathSeparator + file.getAbsolutePath();
                }
            }
        }
        System.setProperty("java.class.path", javaClassPath);
    }

    public static boolean javah(String[] args) {
        try {
            javah().invoke(null, (Object) args);
            return true;
        } catch (InvocationTargetException e) {
            ProgramWarning.message("Error invoking javah: " + e.getTargetException());
        } catch (Exception e) {
            ProgramWarning.message("Error invoking javah: " + e);
        }
        return false;
    }

    private static Method _javapMainMethod;

    private static Method javap() {
        if (_javapMainMethod == null) {
            Class<?> javapMainClass = null;
            try {
                // On the Apple JDKs, there is no tools.jar: all the tools are in classes.jar
                javapMainClass = Class.forName("sun.tools.javap.Main");
            } catch (ClassNotFoundException classNotFoundException) {
                // This is expected on non-Apple JDKs
                final ClassLoader systemToolClassLoader = ToolProvider.getSystemToolClassLoader();
                ProgramError.check(systemToolClassLoader != null, "Cannot find the standard system tools class loader");
                try {
                    javapMainClass = Class.forName("sun.tools.javap.Main", true, systemToolClassLoader);
                } catch (Exception exception) {
                    ProgramWarning.message("Cannot find or initialize javap: " + exception);
                }
            }
            try {
                if (javapMainClass != null) {
                    _javapMainMethod = javapMainClass.getDeclaredMethod("main", String[].class);
                }
            } catch (Exception exception) {
                ProgramWarning.message("Cannot find or initialize javap: " + exception);
            }
        }
        return _javapMainMethod;
    }

    public static boolean javap(String[] args) {
        try {
            javap().invoke(null, (Object) args);
            return true;
        } catch (InvocationTargetException e) {
            ProgramWarning.message("Error invoking javap: " + e.getTargetException());
        } catch (Exception e) {
            ProgramWarning.message("Error invoking javap: " + e);
        }
        return false;
    }
}

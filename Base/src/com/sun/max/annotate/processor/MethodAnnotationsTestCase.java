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
package com.sun.max.annotate.processor;

import java.io.*;
import java.util.*;

import javax.annotation.processing.*;
import javax.tools.*;
import javax.tools.JavaCompiler.*;

import com.sun.max.collect.*;
import com.sun.max.ide.*;
import com.sun.max.program.*;

/**
 * Uses the <a href="http://jcp.org/en/jsr/detail?id=269">Pluggable Annotation Processing</a> framework
 * to validate the usage of compile-time only Maxine specific annotations.
 * 
 * @author Doug Simon
 */
public abstract class MethodAnnotationsTestCase extends MaxTestCase {

    public MethodAnnotationsTestCase(String name) {
        super(name);
    }

    protected Class<? extends Processor> processerClass() {
        return BaseAnnotationProcessor.class;
    }

    private int runProcessor(Sequence<File> sources) {
        final Iterable<String> options = Arrays.asList("-proc:only", "-processor", processerClass().getName(), "-cp", Classpath.fromSystem().toString());
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        final StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        final Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(sources);
        final CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits);
        final int result = task.call().booleanValue() ? 0 : 1;
        final Set<String> reportedDiagnostics = new HashSet<String>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            final String message = diagnostic.getMessage(null);
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

    public void test_annotations() {
        Trace.on(2);
        final Classpath sourcePath = JavaProject.getSourcePath(false);
        final Set<String> seenSources = new HashSet<String>();
        final AppendableSequence<File> sources = new LinkSequence<File>();

        new ClasspathTraversal() {
            @Override
            protected boolean visitFile(File parent, String resource) {
                if (resource.endsWith(".java")) {
                    final File resourceFile = new File(parent, resource);
                    if (resourceFile.getParentFile().getName().equals("SCCS")) {
                        // Ignore files in a directory named SCCS
                    } else {
                        final boolean alreadyAdded = !seenSources.add(resource);
                        if (!alreadyAdded) {
                            Trace.line(2, "Adding source: " + resource);
                            sources.append(resourceFile);
                        } else {
                            System.out.println("ignoring Java source file: " + resourceFile);
                        }
                    }
                }
                return true;
            }

        }.run(sourcePath);
        assertTrue(runProcessor(sources) == 0);
    }
}

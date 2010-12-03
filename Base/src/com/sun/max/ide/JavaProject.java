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
package com.sun.max.ide;

import java.io.*;
import java.util.*;

import com.sun.max.program.*;
import com.sun.max.program.Classpath.Entry;

/**
 * Software project-dependent configuration derived from the
 * {@linkplain Classpath#fromSystem() system class path}.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class JavaProject {

    /**
     * System property name specifying the Maxine workspace directory.
     */
    public static final String MAX_WORKSPACE_PROPERTY = "max.workspace";

    /**
     * A set of project names that can be used to detect the Maxine workspace.
     */
    private static final String[] WORKSPACE_PROJECTS = {"VM", "C1X", "CRI", "Native"};

    /**
     * Determines if a given directory is Maxine workspace directory.
     *
     * @param dir a directory to test
     * @return {@code true} if {@code dir} is a directory containing sub-directories listed in {@link #WORKSPACE_PROJECTS}
     */
    public static boolean isWorkspace(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            int count = 0;
            for (String proj : WORKSPACE_PROJECTS) {
                for (File f : files) {
                    if (f.getName().equals(proj) && f.isDirectory()) {
                        count++;
                    }
                }
            }
            if (count == WORKSPACE_PROJECTS.length) {
                return true;
            }
        }
        return false;
    }

    private JavaProject() {
    }

    public static final String SOURCE_DIRECTORY_NAME = "src";

    public static final String TEST_SOURCE_DIRECTORY_NAME = "test";

    /**
     * Gets the paths on which all the class files referenced by a Java project can be found.
     *
     * @param projClass a class denoting a project (i.e. any class in the project)
     * @param includeDependencies  if true, the returned path includes the location of the
     *                             class files produced by each of the projects that the current
     *                             project depends upon
     */
    public static Classpath getClassPath(Class projClass, boolean includeDependencies) {
        String classfile = projClass.getName().replace('.', '/') + ".class";
        ArrayList<Entry> classPathEntries = new ArrayList<Entry>();
        Entry projEntry = null;
        for (Entry entry : Classpath.fromSystem().entries()) {
            if (entry.contains(classfile)) {
                projEntry = entry;
                classPathEntries.add(entry);
                break;
            }
        }
        if (classPathEntries.isEmpty()) {
            throw new JavaProjectNotFoundException("Could not find path to Java project classes");
        }
        if (includeDependencies) {
            for (Entry entry : Classpath.fromSystem().entries()) {
                if (entry != projEntry) {
                    classPathEntries.add(entry);
                }
            }
        }
        return new Classpath(classPathEntries);
    }

    static class WorkspaceFinder extends ClasspathTraversal {

        File workspace;
        File project;


        boolean deriveWorkspace(File start) {
            File dir = start;
            File child = null;
            while (dir != null) {
                if (isWorkspace(dir)) {
                    workspace = dir;
                    project = child;
                    return true;
                }
                child = dir;
                dir = dir.getParentFile();
            }
            return false;
        }

        @Override
        protected boolean visitFile(File parent, String resource) {
            String classFile = JavaProject.class.getName().replace('.', File.separatorChar) + ".class";
            if (resource.equals(classFile)) {
                if (deriveWorkspace(parent)) {
                    return false;
                }
            }
            return true;
        }
        @Override
        protected boolean visitArchiveEntry(java.util.zip.ZipFile archive, java.util.zip.ZipEntry resource) {
            String classFile = JavaProject.class.getName().replace('.', File.separatorChar) + ".class";
            if (resource.equals(classFile)) {
                File archiveFile = new File(archive.getName());
                if (deriveWorkspace(archiveFile)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Gets Maxine workspace directory (i.e. the parent of all the {@linkplain #WORKSPACE_PROJECTS representative project directories}).
     * This can be specified explicitly with the {@value JavaProject#MAX_WORKSPACE_PROPERTY}
     * or is derived from the {@linkplain Classpath#fromSystem() system class path}.
     *
     * @return the Maxine workspace directory
     */
    public static File findWorkspaceDirectory() {
        final String prop = System.getProperty(JavaProject.MAX_WORKSPACE_PROPERTY);
        if (prop != null) {
            File dir = new File(prop);
            ProgramError.check(isWorkspace(dir), prop + " is not a Maxine workspace directory");
            return dir;
        }
        WorkspaceFinder finder = new WorkspaceFinder();
        finder.run(Classpath.fromSystem());
        ProgramError.check(finder.workspace != null, "failed to find the Maxine workspace directory");
        return finder.workspace;
    }

    /**
     * Gets the paths on which all the Java source files for a Java project can be found.
     *
     * @param projClass a class denoting a project (i.e. any class in the project)
     * @param includeDependencies  if true, the returned path includes the location of the
     *                             Java source files for each of the projects that the current
     *                             project depends upon
     */
    public static Classpath getSourcePath(Class projClass, boolean includeDependencies) {
        final Classpath classPath = getClassPath(projClass, includeDependencies);
        final List<String> sourcePath = new LinkedList<String>();
        for (Entry entry : classPath.entries()) {
            WorkspaceFinder finder = new WorkspaceFinder();
            finder.deriveWorkspace(entry.file());
            final File projectDirectory = finder.project;
            if (projectDirectory != null) {
                final File srcDirectory = new File(projectDirectory, SOURCE_DIRECTORY_NAME);
                if (srcDirectory.exists() && srcDirectory.isDirectory()) {
                    sourcePath.add(srcDirectory.getPath());
                }

                final File testDirectory = new File(projectDirectory, TEST_SOURCE_DIRECTORY_NAME);
                if (testDirectory.exists() && testDirectory.isDirectory()) {
                    sourcePath.add(testDirectory.getPath());
                }
                if (!includeDependencies) {
                    break;
                }
            }
        }
        if (sourcePath.isEmpty()) {
            throw new JavaProjectNotFoundException("Could not find path to Java project sources");
        }
        return new Classpath(sourcePath.toArray(new String[sourcePath.size()]));
    }

    /**
     * Find the primary source directory for a project.
     *
     * @param projClass a class denoting a project (i.e. any class in the project)
     */
    public static File findSourceDirectory(Class projClass) {
        return getSourcePath(projClass, false).entries().get(0).file();
    }
}

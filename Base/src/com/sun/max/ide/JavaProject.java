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

import com.sun.max.*;
import com.sun.max.program.*;
import com.sun.max.program.Classpath.Entry;

/**
 * Software project-dependent configuration. This is all derived from the
 * {@link Classpath#fromSystem() system class path} and assumption that
 * each project has the following directory structure:
 *
 * Eclipse:
 * <top-level-project-dir>/src            # Source files
 * <top-level-project-dir>/test           # Test source files
 * <top-level-project-dir>/bin            # Non-test and test class files
 *
 * Netbeans:
 * <top-level-project-dir>/src                      # Source files
 * <top-level-project-dir>/test                     # Test source files
 * <top-level-project-dir>/dist/<project-name>.jar  # Packaged non-test class files
 * <top-level-project-dir>/build/classes            # Non-test class files
 * <top-level-project-dir>/build/test/classes       # Test class files
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class JavaProject {

    private JavaProject() {
    }

    public static final String SOURCE_DIRECTORY_NAME = "src";

    public static final String TEST_SOURCE_DIRECTORY_NAME = "test";

    /**
     * Gets the paths on which all the class files referenced by the current Java project can be found.
     *
     * @param includeDependencies  if true, the returned path includes the location of the
     *                             class files produced by each of the projects that the current
     *                             project depends upon
     */
    public static Classpath getClassPath(boolean includeDependencies) {
        ArrayList<Entry> classPathEntries = new ArrayList<Entry>();
        for (Entry entry : Classpath.fromSystem().entries()) {
            if (entry.isDirectory()) {
                final File file = new File(entry.path());
                if (file.exists() && file.isDirectory() && !classPathEntries.contains(entry)) {
                    classPathEntries.add(entry);
                    if (!includeDependencies) {
                        break;
                    }
                }
            } else if (entry.isArchive()) {
                if (IDE.current() == IDE.NETBEANS) {
                    if (entry.file().getParentFile().getName().equals("dist")) {
                        classPathEntries.add(entry);
                    }
                }
            }

        }
        if (classPathEntries.isEmpty()) {
            throw new JavaProjectNotFoundException("Could not find path to Java project classes");
        }
        return new Classpath(classPathEntries);
    }

    /** Get the first entry in the {@link Classpath#fromSystem system classpath} that is a project directory.
     * @return see above
     */
    public static File findClassesOnClasspath() {
        return getClassPath(false).entries().get(0).file();
    }

    /**
     * Gets the root directory of the Maxine repository, i.e. the parent of all the project directories.
     * This can be specified explicitly with the {@value IDE#MAX_PROJECT_DIRECTORY_PROPERTY}
     * or is computed by finding the first project in the {@link Classpath#fromSystem system classpath}  containing
     * a "com/sun/max" package.
     * @return a {@link File} for the Maxine root directory
     */
    public static File findMaxineRootDirectory() {
        File result = null;
        final String maxDirProp = System.getProperty(IDE.MAX_PROJECT_DIRECTORY_PROPERTY);
        if (maxDirProp != null) {
            result = new File(maxDirProp);
            if (!(result.isDirectory() && result.exists())) {
                ProgramError.unexpected(IDE.MAX_PROJECT_DIRECTORY_PROPERTY + " is not a Maxine root directory");
            }
        } else {
            for (Entry entry : Classpath.fromSystem().entries()) {
                if (entry.isDirectory()) {
                    final String packageName = MaxPackage.class.getPackage().getName();
                    final File file = new File(entry.path(), packageName.replace('.', File.separatorChar));
                    if (file.exists() && file.isDirectory()) {
                        result = entry.file().getParentFile();
                        break;
                    }
                } else if (entry.isArchive()) {
                    if (IDE.current() == IDE.NETBEANS) {
                        if (entry.file().getParentFile().getName().equals("dist")) {
                            result = entry.file().getParentFile();
                            break;
                        }
                    }
                }
            }
            if (result == null) {
                ProgramError.unexpected("failed to find the Maxine root directory");
            }
        }
        return result.getParentFile().getAbsoluteFile();
    }

    /**
     * Gets the paths on which all the Java source files for the current Java project can be found.
     *
     * @param includeDependencies  if true, the returned path includes the location of the
     *                             Java source files for each of the projects that the current
     *                             project depends upon
     */
    public static Classpath getSourcePath(boolean includeDependencies) {
        final Classpath classPath = getClassPath(includeDependencies);
        final List<String> sourcePath = new LinkedList<String>();
        for (Entry entry : classPath.entries()) {
            final File projectDirectory = IDE.current().findVcsProjectDirectoryFromClasspathEntry(entry.file());
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

    public static File findSourceDirectory() {
        return getSourcePath(false).entries().get(0).file();
    }

    /**
     * Gets the directory used by the current {@linkplain IDE} to store the metadata for the current project.
     * The current project is the one containing the main class of this Java process.
     */
    public static File findIdeProjectDirectory() {
        return IDE.current().findIdeProjectDirectoryFromClasspathEntry(findClassesOnClasspath());
    }

    /**
     * Gets the top-level directory of the current project. That is, the directory that is under VCS control.
     * This may be different from the {@linkplain #findIdeProjectDirectory() IDE project directory}. For example,
     * Netbeans puts the project metadata in a subdirectory (named "nbproject") of the top level project
     * directory where as Eclipse puts the project metadata in <i>dot</i> files (e.g. ".classpath", ".project")
     * in the top level directory.
     * The current project is the one containing the main class of this Java process.
     */
    public static File findVcsProjectDirectory() {
        final IDE ide = IDE.current();
        if (ide == null) {
            throw ProgramError.unexpected("Cannot determine IDE in order to find project directory");
        }
        final File classpath = findClassesOnClasspath().getAbsoluteFile();
        final File projDir = ide.findVcsProjectDirectoryFromClasspathEntry(classpath);
        if (projDir == null) {
            throw ProgramError.unexpected("Cannot find project directory for IDE: " + ide + ", classpath = " + classpath);
        }
        return projDir;
    }
}

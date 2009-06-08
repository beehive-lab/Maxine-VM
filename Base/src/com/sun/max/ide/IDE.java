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

import com.sun.max.*;
import com.sun.max.program.*;

/**
 * @author Bernd Mathiske
 * @author Greg Wright
 * @author Doug Simon
 */
public enum IDE {

    NETBEANS {
        @Override
        public File findIdeProjectDirectoryFromClasspathEntry(File classpathEntry) {
            if (classpathEntry.isDirectory() && classpathEntry.getName().equals("classes")) {
                final File buildDirectory = classpathEntry.getParentFile();
                if (buildDirectory.getName().equals("build")) {
                    return new File(buildDirectory.getParentFile(), "nbproject");
                }
            } else if (classpathEntry.getName().endsWith(".jar")) {
                final File distDirectory = classpathEntry.getParentFile();
                if (distDirectory.getName().equals("dist")) {
                    return new File(distDirectory.getParentFile(), "nbproject");
                }
            }
            return null;
        }

        @Override
        public File findVcsProjectDirectoryFromClasspathEntry(File classesDirectory) {
            final File ideProjectDirectory = findIdeProjectDirectoryFromClasspathEntry(classesDirectory);
            if (ideProjectDirectory != null) {
                return ideProjectDirectory.getParentFile();
            }
            return null;
        }
    },
    ECLIPSE {
        @Override
        public File findIdeProjectDirectoryFromClasspathEntry(File classesDirectory) {
            // shell puts classes in $workspace/$proj/bin/$package/$class.class
            if (classesDirectory.getName().equals("bin")) {
                return classesDirectory.getAbsoluteFile().getParentFile();
            }
            return null;
        }
    },
    INTELLIJ {
        @Override
        public File findIdeProjectDirectoryFromClasspathEntry(File classesDirectory) {
            // IntelliJ puts classes in $workspace/out/production/$proj/$package/$class.class
            File prodDir = classesDirectory;
            while (prodDir != null) {
                if (prodDir.getName().equals("production")) {
                    break;
                }
                prodDir = prodDir.getParentFile();
            }
            if (prodDir != null) {
                File outDir = prodDir.getParentFile();
                if (outDir != null && outDir.getName().equals("out")) {
                    return new File(outDir.getParentFile(), classesDirectory.getName());
                }
            }
            return null;
        }
    },
    SHELL {
        @Override
        public File findIdeProjectDirectoryFromClasspathEntry(File classesDirectory) {
            // shell puts classes in $workspace/$proj/bin/$package/$class.class
            if (classesDirectory.getName().equals("bin")) {
                return classesDirectory.getAbsoluteFile().getParentFile();
            }
            return null;
        }
    },
    AJTRACE {
        @Override
        public File findIdeProjectDirectoryFromClasspathEntry(File classesDirectory) {
            // In this case we have the AspectJ modified classes directory passed in, and we need
            // another property to tell us where the real project directory is located (for native code etc).
            final String projectDirectory = System.getProperty("max.project.directory");
            if (projectDirectory == null) {
                ProgramError.unexpected("the property max.project.directory must be set");
            }
            return new File(projectDirectory).getAbsoluteFile();
        }

    };

    private IDE() {
    }

    public String idePackageName() {
        final MaxPackage thisPackage = MaxPackage.fromClass(IDE.class);
        return thisPackage.name() + "." + name().toLowerCase();
    }

    public abstract File findIdeProjectDirectoryFromClasspathEntry(File classpathEntry);

    public File findVcsProjectDirectoryFromClasspathEntry(File classpathEntry) {
        return findIdeProjectDirectoryFromClasspathEntry(classpathEntry);
    }

    private boolean packageClassExists() {
        return MaxPackage.fromName(idePackageName()) != null;
    }

    public static IDE current() {
        final String ideProperty = System.getProperty("max.ide");
        if (ideProperty != null) {
            try {
                return IDE.valueOf(ideProperty);
            } catch (IllegalArgumentException illegalArgumentException) {
                ProgramWarning.message("Value of max.ide (" + ideProperty + ") does not correspond with an IDE enum value");
            }
        }
        for (IDE ide : IDE.values()) {
            if (ide.packageClassExists()) {
                // if the package class exists, then the user has chosen an IDE
                return ide;
            }
        }
        return null;
    }
}

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
package com.sun.max.program;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import com.sun.max.lang.*;

/**
 * Provides a facility for finding classes reachable on a given {@linkplain Classpath classpath}.
 *
 * @author Doug Simon
 */
public class ClassSearch extends ClasspathTraversal {

    private final HashSet<String> classes;

    public ClassSearch() {
        this(false);
    }

    /**
     * Creates a class search object.
     *
     * @param omitDuplicates if true, then each argument passed to {@link #visitClass(String)} is guaranteed to be unique.
     */
    public ClassSearch(boolean omitDuplicates) {
        if (omitDuplicates) {
            classes = new HashSet<String>();
        } else {
            classes = null;
        }
    }

    /**
     * Handles a class file encountered during the traversal.
     *
     * @param className
     *                the name of the class denoted by the class file
     * @return true if the traversal should continue, false if it should terminate
     */
    protected boolean visitClass(String className) {
        return true;
    }

    /**
     * Handles a class file encountered during the traversal. Unless this object was initialized to omit duplicates,
     * this method may be called more than once for the same class as class files are not guaranteed to be unique in a
     * classpath.
     *
     * @param isArchiveEntry true if the class is in a .zip or .jar file, false if it is a file in a directory
     * @param className the name of the class denoted by the class file
     * @return true if the traversal should continue, false if it should terminate
     */
    protected boolean visitClass(boolean isArchiveEntry, String className) {
        if (classes != null) {
            if (classes.contains(className)) {
                return true;
            }
            classes.add(className);
        }
        return visitClass(className);
    }

    protected boolean visit(boolean isArchiveEntry, String dottifiedResource) {
        if (dottifiedResource.endsWith(".class")) {
            final String className = Strings.chopSuffix(dottifiedResource, ".class");
            return visitClass(isArchiveEntry, className);
        }
        return true;
    }

    @Override
    protected boolean visitArchiveEntry(ZipFile archive, ZipEntry resource) {
        return visit(true, resource.getName().replace('/', '.'));
    }

    @Override
    protected boolean visitFile(File parent, String resource) {
        return visit(false, resource.replace(File.separatorChar, '.'));
    }
}

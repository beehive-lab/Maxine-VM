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
package com.sun.max;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * Loads all the classes in or under a given package.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class PackageLoader {

    private final Classpath _classpath;
    private final ClassLoader _classLoader;
    private int _traceLevel = 1;

    public PackageLoader(ClassLoader classLoader, Classpath classpath) {
        _classpath = classpath;
        _classLoader = classLoader;
    }

    public void setTraceLevel(int level) {
        _traceLevel = level;
    }

    /**
     * Loads classes under a given package.
     * 
     * @param packageName the name of the package from which classes are loaded
     * @param recursive if true, then classes in sub-packages are loaded as well, otherwise only classes in the package
     *            denoted by {@code packageName} are loaded
     * @return the loaded classes
     */
    public Sequence<Class> load(final String packageName, final boolean recursive) {
        Trace.line(_traceLevel, "PackageLoader.load: " + packageName);
        final AppendableSequence<Class> classes = new ArrayListSequence<Class>();
        final Set<String> classNames = new HashSet<String>();
        final ClassSearch classSearch = new ClassSearch() {

            @Override
            protected boolean visitClass(boolean isArchiveEntry, String className) {
                if (!className.endsWith("package-info")) {
                    if (!classNames.contains(className)) {
                        if (recursive || Classes.getPackageName(className).equals(packageName)) {
                            final Class javaClass = Classes.load(_classLoader, className);
                            if (javaClass != null) {
                                Classes.link(javaClass);
                                classNames.add(className);
                                classes.append(javaClass);
                            }
                        }
                    }
                }
                return true;
            }
        };
        classSearch.run(_classpath, packageName.replace('.', '/'));
        ProgramError.check(!classNames.isEmpty(), "no classes found in package: " + packageName);
        return classes;
    }

    /**
     * Loads classes under a given package.
     * 
     * @param maxPackage the package from which classes are loaded
     * @param recursive if true, then classes in sub-packages are loaded as well, otherwise only classes in
     *            {@code maxPackage} are loaded
     * @return the loaded classes
     */
    public Sequence<Class> load(MaxPackage maxPackage, boolean recursive) {
        return load(maxPackage.name(), recursive);
    }

    /**
     * Initializes the given class and all its inner classes, recursively.
     */
    private void initializeAll(Class outerClass) {
        Classes.initialize(outerClass);
        for (Class innerClass : outerClass.getDeclaredClasses()) {
            initializeAll(innerClass);
        }
    }

    public void loadAndInitializeAll(Class representative) {
        try {
            for (Class outerClass : load(MaxPackage.fromClass(representative), false)) {
                initializeAll(outerClass);
            }
        } catch (Throwable throwable) {
            ProgramError.unexpected(throwable);
        }
    }

    private void initializeAllAndInstantiateLeaves(Class<?> c, Class<?> root) {
        Classes.initialize(c);
        if ((c.getModifiers() & Modifier.FINAL) != 0 && root.isAssignableFrom(c)) {
            try {
                c.newInstance();
            } catch (Throwable throwable) {
                ProgramError.unexpected("could not instantiate leaf class of " + root.getSimpleName() + ": " + c, throwable);
            }
        }
        for (Class innerClass : c.getDeclaredClasses()) {
            initializeAllAndInstantiateLeaves(innerClass, root);
        }
    }

    public void loadAndInitializeAllAndInstantiateLeaves(Class root) {
        try {
            for (Class outerClass : load(MaxPackage.fromClass(root), false)) {
                initializeAllAndInstantiateLeaves(outerClass, root);
            }
        } catch (Throwable throwable) {
            ProgramError.unexpected(throwable);
        }
    }
}

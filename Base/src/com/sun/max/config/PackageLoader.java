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
package com.sun.max.config;

import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * Loads all the classes in or under a given package.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class PackageLoader {

    public final Classpath classpath;
    public final ClassLoader classLoader;
    private int traceLevel = 1;

    public PackageLoader(ClassLoader classLoader, Classpath classpath) {
        this.classpath = classpath;
        this.classLoader = classLoader;
    }

    public void setTraceLevel(int level) {
        traceLevel = level;
    }

    /**
     * Loads a given class.
     * A subclass can override this method to omit loading of certain classes in a package
     * which can result in this method returning {@code null}.
     *
     * @param className
     * @return the {@code Class} instance for {@code className}
     */
    protected Class loadClass(String className) {
        return Classes.load(classLoader, className);
    }

    /**
    /**
     * Lists the classes under a given package.
     *
     * @param packageName the name of the package to search
     * @return the class names
     */
    public String[] listClassesInPackage(final MaxPackage maxPackage) {
        final HashSet<String> classNames = new HashSet<String>();
        final String packageName = maxPackage.name();
        final ClassSearch classSearch = new ClassSearch() {
            @Override
            protected boolean visitClass(boolean isArchiveEntry, String className) {
                if (!className.endsWith("package-info")) {
                    if (!classNames.contains(className)) {
                        if (maxPackage.isIncluded(className)) {
                            classNames.add(className);
                        }
                    }
                }
                return true;
            }
        };
        classSearch.run(classpath, packageName.replace('.', '/'));
        return classNames.toArray(new String[classNames.size()]);
    }

    /**
     * Loads classes under a given package, subject to inclusions/exclusions.
     *
     * @param maxPackage the package from which classes are loaded
     * @param initialize specifies whether the loaded classes should be {@linkplain Classes#initialize(Class) initialized}
     * @return the loaded classes
     */
    public List<Class> load(MaxPackage maxPackage, boolean initialize) {
        final String packageName = maxPackage.name();
        Trace.line(traceLevel, "loading: " + packageName);
        maxPackage.loading();
        final List<Class> classes = new ArrayList<Class>();
        String[] classNames = listClassesInPackage(maxPackage);
        for (String className : classNames) {
            final Class javaClass = loadClass(className);
            if (javaClass != null) {
                if (initialize) {
                    Classes.initialize(javaClass);
                }
                classes.add(javaClass);
            }
        }
        ProgramWarning.check(classNames.length != 0, "no classes found in package: " + packageName);
        return classes;
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
            for (Class outerClass : load(MaxPackage.fromClass(representative), true)) {
                initializeAll(outerClass);
            }
        } catch (Throwable throwable) {
            ProgramError.unexpected(throwable);
        }
    }
}

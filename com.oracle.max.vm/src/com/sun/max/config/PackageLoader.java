/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.config;

import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * Loads all the classes in or under a given package.
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
     * Loads classes under a given package, subject to inclusions/exclusions.
     *
     * @param pkg the package from which classes are loaded
     * @param initialize specifies whether the loaded classes should be {@linkplain Classes#initialize(Class) initialized}
     * @return the loaded classes
     */
    public List<Class> load(BootImagePackage pkg, boolean initialize) {
        final String packageName = pkg.name();
        Trace.line(traceLevel, "loading: " + packageName);
        pkg.loading();
        final List<Class> classes = new ArrayList<Class>();
        String[] classNames = pkg.listClasses(classpath);
        for (String className : classNames) {
            final Class javaClass = loadClass(className);
            if (javaClass != null) {
                if (initialize) {
                    Classes.initialize(javaClass);
                }
                classes.add(javaClass);
            }
        }
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
            for (Class outerClass : load(BootImagePackage.fromClass(representative), true)) {
                initializeAll(outerClass);
            }
        } catch (Throwable throwable) {
            throw ProgramError.unexpected(throwable);
        }
    }
}

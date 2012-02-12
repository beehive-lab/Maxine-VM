/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.graal.callanalysis;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.util.zip.*;

import com.sun.max.program.*;

/**
 * Discovers subtype relations of classes in the classpath.
 */
public class SubtypeDiscovery extends ClasspathTraversal {

    private static Logger LOGGER = Logger.getLogger(SubtypeDiscovery.class.getName());

    private final HashMap<Class<?>, Set<Class<?>>> subtypeRelations = new HashMap<Class<?>, Set<Class<?>>>();
    private final ClassLoader loader;

    public SubtypeDiscovery(ClassLoader loader) {
        this.loader = loader;
    }

    public HashMap<Class<?>, Set<Class<?>>> getSubtypeRelations() {
        return subtypeRelations;
    }

    @Override
    protected boolean visitFile(File parent, String resource) {
        if (!resource.endsWith(".class")) {
            LOGGER.log(Level.INFO, "Ignoring non-class file: " + resource);
            return true;
        }

        // TODO: This code examines only the public class with the same name as
        //       the file and its nested classes. There may be other, non-public
        //       classes at the package level that are not found by this code.
        //       While such classes are often considered bad practice, this code
        //       should be able to find and process them.
        String className = resource.substring(0, resource.length() - ".class".length()).replace(File.separatorChar, '.');
        return visitClass(className);
    }

    @Override
    protected boolean visitArchiveEntry(ZipFile archive, ZipEntry resource) {
        if (!resource.getName().endsWith(".class")) {
            LOGGER.log(Level.INFO, "Ignoring non-class archive entry: " + resource);
            return true;
        }

        String path = resource.getName();
        String className = path.substring(0, path.length() - ".class".length()).replace('/', '.');
        return visitClass(className);
    }

    private boolean visitClass(String name) {
        try {
            Class<?> clazz = Class.forName(name, false, loader);
            visitClass(clazz);
            return true;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void visitClass(Class<?> clazz) {
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            recordSubtype(superclass, clazz);
        }

        for (Class<?> iface : clazz.getInterfaces()) {
            recordSubtype(iface, clazz);
        }

        for (Class<?> nested : clazz.getDeclaredClasses()) {
            visitClass(nested);
        }
    }

    private void recordSubtype(Class<?> clazz, Class<?> subtype) {
        Set<Class<?>> entry = subtypeRelations.get(clazz);
        if (entry == null) {
            entry = new HashSet<Class<?>>();
            subtypeRelations.put(clazz, entry);
        }
        entry.add(subtype);
    }

}

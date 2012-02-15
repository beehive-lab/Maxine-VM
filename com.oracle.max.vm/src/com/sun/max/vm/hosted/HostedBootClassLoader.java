/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.hosted;

import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.type.*;

/**
 * The Boot class loader used when running in hosted mode.
 * The singleton {@link #HOSTED_BOOT_CLASS_LOADER} instance is identical to the
 * singleton {@link BootClassLoader#BOOT_CLASS_LOADER} instance of the {@link BootClassLoader}
 * at runtime thanks to {@link JavaPrototype#hostToTarget(Object)}.
 *
 */
public final class HostedBootClassLoader extends HostedClassLoader {
    private static final Set<String> omittedClasses = new HashSet<String>();
    private static final Set<String> omittedPackages = new HashSet<String>();

    private HostedBootClassLoader() {
    }

    /**
     * This value is identical to {@link BootClassLoader#BOOT_CLASS_LOADER} at runtime.
     *
     * @see JavaPrototype#hostToTarget(Object)
     */
    public static final HostedBootClassLoader HOSTED_BOOT_CLASS_LOADER = new HostedBootClassLoader();

    @Override
    protected Classpath getDefaultClassPath() {
        // TODO change to system boot
        return Classpath.fromSystem();
    }

    /**
     * Adds a class that must not be loaded into the VM class registry. Calling {@link #loadClass(String, boolean)} for
     * this class will return null.
     *
     * @param javaClass the class to be omitted
     */
    public static void omitClass(Class javaClass) {
        omitClass(javaClass.getName());
    }

    /**
     * Adds a class that must not be loaded into the VM class registry. Calling {@link #loadClass(String, boolean)} for
     * this class will return null.
     *
     * @param className the name of the class to be omitted
     */
    public static void omitClass(String className) {
        if (ClassRegistry.BOOT_CLASS_REGISTRY.get(JavaTypeDescriptor.getDescriptorForJavaString(className)) != null) {
            throw ProgramError.unexpected("Cannot omit a class already in VM class registry: " + className);
        }
        omittedClasses.add(className);
    }

    /**
     * Adds the name of package whose constituent classes must not be loaded into the VM class registry. Calling
     * {@link #loadClass(String, boolean)} for a class in the named package will return null.
     *
     * @param packageName
     * @param retrospective if true, then this method verifies that the VM class registry does not currently contain any
     *            classes in the specified package
     */
    public static void omitPackage(String packageName, boolean retrospective) {
        if (retrospective) {
            synchronized (HOSTED_BOOT_CLASS_LOADER) {
                ProgramError.check(!HOSTED_BOOT_CLASS_LOADER.loadedPackages.contains(packageName), "Cannot omit a package already in VM class registry: " + packageName);
            }
        }
        omittedPackages.add(packageName);
    }

    /**
     * Determines if a given type descriptor denotes a class that must not be loaded in VM class registry.
     * The set of omitted classes is determined by any preceding calls to {@link #omitClass(Class)} and {@link #omitPackage(String, boolean)}.
     * All inner classes of omitted classes are also omitted.
     *
     * @param className the name of a type to test
     * @return {@code true} if {@code typeDescriptor} denotes a class that must not be loaded in VM class registry
     */
    @Override
    public boolean isOmittedType(String className) {
        if (omittedClasses.contains(className)) {
            return true;
        }
        if (omittedPackages.contains(Classes.getPackageName(className))) {
            return true;
        }

        if (Classes.getSimpleName(className).lastIndexOf('$') >= 0) {
            return isOmittedType(className.substring(0, className.lastIndexOf('$')));
        }
        return false;
    }

}

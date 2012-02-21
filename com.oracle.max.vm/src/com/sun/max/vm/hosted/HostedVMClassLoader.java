/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.type.*;

/**
 * The VM class loader used when running in hosted mode. The singleton {@link #HOSTED_VM_CLASS_LOADER} instance is
 * identical to the singleton {@link VMClassLoader#VM_CLASS_LOADER} instance of the {@link VMClassLoader} at runtime
 * thanks to {@link JavaPrototype#hostToTarget(Object)}.
 *
 * Most of the logic is inherited from {@link HostedClassLoader}.
 *
 * The customizations are:
 * <ul>
 * <li>VM classes are loaded by delegation to the system class loader</li>
 * <li>{@link HOSTED_ONLY} classes are prevented from being loaded into the image.
 * </ul>
 */

public class HostedVMClassLoader extends HostedClassLoader {

    private ClassLoader systemClassLoader;

    private HostedVMClassLoader() {
        super(HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER);
        systemClassLoader = ClassLoader.getSystemClassLoader();
    }

    /**
     * This value is identical to {@link VMClassLoader#VM_CLASS_LOADER} at runtime.
     *
     * @see JavaPrototype#hostToTarget(Object)
     */
    public static final HostedVMClassLoader HOSTED_VM_CLASS_LOADER = new HostedVMClassLoader();

    @Override
    protected Classpath getDefaultClasspath() {
        return Classpath.fromSystem();
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        Class<?> result = null;
        try {
            // Arrays/stubs are handled specially in common code in superclass
            result = super.findClass(name);
        } catch (ClassNotFoundException ex) {
            // regular class
        }
        if (result != null) {
            return result;
        }
        // Regular class, which we load by delegation from the system class loader
        return systemClassLoader.loadClass(name);
    }

    @Override
    protected boolean extraLoadClassChecks(Class< ? > javaType) throws ClassNotFoundException {
        if (MaxineVM.isHostedOnly(javaType)) {
            throw new HostOnlyClassError(javaType.getName());
        } else if (javaType.getClassLoader() == null) {
            // The following prevents hosted only classes and platform (boot classpath) classes from being
            // placed in the VM class registry. Platform classes will already have been placed in the
            // boot class registry by HostedBootClassLoader.
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "VM";
    }

}

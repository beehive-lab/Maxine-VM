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
 * Currently, it is not possible to extend the VM by loading classes at runtime, and if it were, we would want to
 * control the location from which such classes could originate. Therefore, the {@link VMClassLoader} is somewhat
 * similar to the <i>extensions</i> class loader and only has the boot class loader as parent. In consequence, in the
 * hosted context, we have to handle class loading of VM classes explicitly instead of just inheriting from the system
 * (application) class loader.
 *
 * Since this classloader only has the boot classloader as parent, the {{@link #findClass(String)} method will be
 * called to locate all VM classes that are either loaded explicitly via {@link #loadClass(String)} or implicitly
 * via references within a VM class loaded by this loader.
 *
 * In an simple world, the classes that construct the boot image would be completely separate from the classes from the
 * VM classes and the two sets could inhabit separate classloaders. However the existence of {@link HOSTED_ONLY} methods and
 * {@link HOSTED_ONLY} classes in the VM packages means that life is not so simple. It is possible for a hosted only
 * class to be loaded (in the system classloader) and then be referenced from a VM class that has been loaded by this
 * classloader via a {@link HOSTED_ONLY} method . Since this class does not have the system class loader as parent that
 * could lead to a duplicate instantiation of the class. That might be ok, but there is the real risk of duplication of
 * static state that should be shared. So we delegate to the system class loader in {@link #findClass}.
 *
 *
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
//        Trace.line(1, this.getClass().getName() + " findClass: " + name);
        Class<?> result = null;
        try {
            // Arrays must be handled specially
            result = super.findClass(name);
        } catch (ClassNotFoundException ex) {
            // regular class
        }
        if (result != null) {
            return result;
        }
        // Regular class, which we load by delegation from the system class loader
        try {
            try {
                result = systemClassLoader.loadClass(name);
            } catch (ClassNotFoundException ex) {
                // must be an internally generated class, i.e., a stub, or something's missing
            }
            return result;
        } catch (Exception e) {
            throw ProgramError.unexpected(e);
        }
    }

    @Override
    protected boolean extraLoadClassChecks(Class<?> javaType, String name) {
        // The following prevents hosted only classes and platform (boot classpath) classes from being
        // placed in the VM class registry. Platform classes will already have been placed in the
        // boot class registry by HostedBootClassLoader.
        if (javaType != null) {
            if (MaxineVM.isHostedOnly(javaType)) {
                throw new HostOnlyClassError(name);
            } else if (javaType.getClassLoader() == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "VM";
    }

}

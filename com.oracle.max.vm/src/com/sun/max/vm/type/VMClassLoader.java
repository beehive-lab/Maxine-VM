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
package com.sun.max.vm.type;

import java.net.*;

/**
 * A classloader that exists to isolate the VM classes from the JDK classes
 * and application classes. Runtime extensions to the VM are supported by
 * delegating to a {@link URLClassLoader} that is handed URLs to search via the
 * {@link addURL} method from {@link JavaRunScheme#loadVMExtensions}.
 */
public class VMClassLoader extends ClassLoader {
    /**
     * The singleton instance of this class.
     */
    public static final VMClassLoader VM_CLASS_LOADER = new VMClassLoader();

    private static class VMURLClassLoader extends URLClassLoader {
        VMURLClassLoader() {
            super(new URL[0], VM_CLASS_LOADER);
        }

        @Override
        protected void addURL(URL url) {
            super.addURL(url);
        }

        @Override
        protected Class<?> findClass(final String name) throws ClassNotFoundException {
            return super.findClass(name);
        }

    }

    private static final VMURLClassLoader urlClassLoader = new VMURLClassLoader();

    private VMClassLoader() {
        super(BootClassLoader.BOOT_CLASS_LOADER);
    }

    public static void addURL(URL url) {
        urlClassLoader.addURL(url);
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        // VM classes in the extension are found using the URL class loader from their JAR file.
        return urlClassLoader.findClass(name);
    }


}

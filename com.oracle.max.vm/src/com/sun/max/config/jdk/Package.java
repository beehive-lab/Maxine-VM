/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.config.jdk;

import java.io.File;

import com.sun.max.config.*;
import com.sun.max.vm.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;

/**
 * Redirection for the standard set of JDK packages to include in the image.
 */
public class Package extends BootImagePackage {
    private static final String[] packages = {
        "java.lang.*",
        "java.lang.reflect.*",
        "java.lang.ref.*",
        "java.io.*",
        "java.nio.*",
        "java.nio.charset.*",
        "java.security.ProtectionDomain",
        "java.security.DomainCombiner",
        "java.security.PrivilegedAction",
        "java.util.*",
        "java.util.zip.*",
        "java.util.jar.*",
        "java.util.regex.*",
        "java.util.concurrent.atomic.*",
        "java.util.concurrent.locks.*",
        "java.text.*",
        "sun.misc.Version",
        "sun.misc.SharedSecrets",
        "sun.misc.VM",
        "sun.misc.Cleaner",
        "sun.reflect.Reflection",
        "sun.nio.cs.*",
        "sun.security.action.GetPropertyAction"
    };

    private static boolean customised;
    private static boolean reinits;

    public Package() {
        super(packages);
        // order is important
        // we shouldn't be called more than once but are (Word class search)
        if (!reinits) {
            MaxineVM.registerKeepClassInit("java.lang.ProcessEnvironment");
            MaxineVM.registerKeepClassInit("java.lang.ApplicationShutdownHooks");
            MaxineVM.registerKeepClassInit("java.io.File");
            MaxineVM.registerKeepClassInit("sun.misc.Perf");
            reinits = true;
        }
    }

    /**
     * Called just before any classes are loaded from this package.
     * Owing to the cloning in sub-packages we get called multiple times.
     */
    @Override
    public void loading() {
        if (!customised) {
            HostedBootClassLoader.omitClass(JavaTypeDescriptor.getDescriptorForJavaString(File.class.getName() + "$LazyInitialization"));
            HostedBootClassLoader.omitClass(JavaTypeDescriptor.getDescriptorForJavaString(java.util.Calendar.class.getName() + "$CalendarAccessControlContext"));
            final boolean restrictCorePackages = System.getProperty("max.allow.all.core.packages") == null;
            if (restrictCorePackages) {
                // Don't want the static Map fields initialised
                HostedBootClassLoader.omitClass(java.lang.reflect.Proxy.class);

                // LogManager and FileSystemPreferences have many side effects
                // that we do not wish to account for before running the target VM.
                // TODO is this really necessary since we are not loading these packages anyway!
                HostedBootClassLoader.omitPackage("java.util.logging", true);
                HostedBootClassLoader.omitPackage("java.util.prefs", true);

                // TODO check. we load some classes explicitly so what is this actually doing?
                HostedBootClassLoader.omitPackage("java.security", false);
            }
            customised = true;
        }
    }
}

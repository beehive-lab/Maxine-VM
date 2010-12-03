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
package com.sun.max.config.jdk;

import java.io.File;

import com.sun.max.config.*;
import com.sun.max.vm.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;

/**
 * Redirection for the standard set of JDK packages to include in the image.
 *
 * @author Mick Jordan
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

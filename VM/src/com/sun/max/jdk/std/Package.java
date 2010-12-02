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
package com.sun.max.jdk.std;

import java.io.File;
import com.sun.max.jdk.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;

/**
 * Redirection for the standard set of JDK packages to include in the image.
 *
 * @author Mick Jordan
 */
public class Package extends JDKPackage {
    private static final String[] packages = {
        "java.lang",
        "java.lang.reflect",
        "java.lang.ref",
        "java.io",
        "java.nio",
        "java.nio.charset",
        "java.security",
        "java.util",
        "java.util.zip",
        "java.util.jar",
        "java.util.regex",
        "java.util.concurrent.atomic",
        "sun.misc",
        "sun.nio.cs",
        "sun.security.action"};

    private boolean customised;

    public Package() {
        super(false, packages);
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

    /**
     * Called after the cloned instance for all the sub-packages in {@link #packages} is created.
     * It allows us to set the inclusions for those packages that we want to customise.
     */
    @Override
    protected void recursiveOverride() {
        if (name().equals("sun.misc")) {
            setInclusions("Version", "SharedSecrets", "VM", "Cleaner");
        } else if (name().equals("sun.reflect")) {
            setInclusions("Reflection");
        } else if (name().equals("sun.reflect.annotation")) {
            setInclusions("AnnotationParser");
        } else if (name().equals("java.security")) {
            setInclusions("ProtectionDomain", "DomainCombiner", "PrivilegedAction");
        } else if (name().equals("sun.security.action")) {
            setInclusions("GetPropertyAction");
        }
    }
}

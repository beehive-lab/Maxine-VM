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

import com.sun.max.config.*;
import com.sun.max.lang.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.jdk.*;

/**
 * Redirection for the standard set of JDK packages to include in the image.
 */
public class Package extends BootImagePackage {
    private static final String[] packages = {
        "java.lang.*",
        "java.lang.reflect.*",
        "java.lang.ref.*",
    };

    private static boolean loadingDone;
    private static boolean initDone;

    public Package() {
        super(packages);

        // we shouldn't be called more than once but are (Word class search)
        if (initDone) {
            return;
        }
        initDone = true;

        // Static initializers that are invoked again in JDK_java_lang_System.initProperties()
        MaxineVM.registerKeepClassInit("java.lang.ProcessEnvironment");
        MaxineVM.registerKeepClassInit("java.lang.ApplicationShutdownHooks");
        MaxineVM.registerKeepClassInit("java.io.File");
        MaxineVM.registerKeepClassInit("sun.misc.Perf");
        MaxineVM.registerKeepClassInit("sun.misc.Launcher");
    }

    /**
     * Called just before any classes are loaded from this package.
     * Owing to the cloning in sub-packages we get called multiple times.
     */
    @Override
    public void loading() {
        if (loadingDone) {
            return;
        }
        loadingDone = true;

        // Classes that must not be in the boot image for various reasons
        HostedBootClassLoader.omitClass(java.io.File.class.getName() + "$LazyInitialization");
        HostedBootClassLoader.omitClass(java.io.File.class.getName() + "$TempDirectory");
        HostedBootClassLoader.omitClass(java.util.Calendar.class.getName() + "$CalendarAccessControlContext");
        HostedBootClassLoader.omitClass("sun.reflect.UnsafeFieldAccessorFactory");
        // This class uses Unsafe.objectFieldOffset() and stores the offsets in arrays.  We currently have no way in JDKInterceptor
        // to rewrite these offsets to the correct Maxine layout specific values, so make sure this class is not part of the boot image.
        HostedBootClassLoader.omitClass(java.io.ObjectStreamClass.class.getName() + "$FieldReflector");
        // The class sun.security.provider.NativePRNG uses file descriptors to access native random number generators.
        // Make sure the security provider list (which would contain a NativePRNG instance) is not loaded.
        HostedBootClassLoader.omitClass(sun.security.jca.Providers.class);
        // Some other classes would also directly reference NativePRNG, so exclude them too.
        HostedBootClassLoader.omitClass("java.nio.file.TempFileHelper");
        // The static initializer loads the native network library
        // However, InetSocketAddress needs unsafe fields fixing up, which requires this class
        // to be loaded. Loading the network library appears to be harmless.
        //HostedBootClassLoader.omitClass("java.net.InetAddress");
        // The static initializer loads and initializes native libraries
        HostedBootClassLoader.omitClass("sun.nio.ch.FileDispatcherImpl");
        HostedBootClassLoader.omitClass("sun.nio.ch.FileChannelImpl");
        HostedBootClassLoader.omitClass("sun.nio.ch.Util");
        HostedBootClassLoader.omitClass("sun.nio.fs.UnixNativeDispatcher");
        HostedBootClassLoader.omitClass("sun.jkernel.Bundle");
        // Java 7 only class that indirectly caches references to JarFiles
        HostedBootClassLoader.omitClass(sun.misc.Launcher.class.getName() + "$BootClassPathHolder");

        // Methods that are called using JNI during startup; we want the invocation stub in the boot image to avoid compilation at run time
        CompiledPrototype.registerImageInvocationStub(MethodActor.fromJava(Classes.getDeclaredMethod(java.lang.System.class, "getProperty", String.class)));
        CompiledPrototype.registerImageInvocationStub(MethodActor.fromJava(Classes.getDeclaredMethod(java.nio.charset.Charset.class, "isSupported", String.class)));
        CompiledPrototype.registerImageInvocationStub(MethodActor.fromJava(Classes.getDeclaredMethod(java.lang.String.class, "getBytes", String.class)));
        CompiledPrototype.registerImageInvocationStub(MethodActor.fromJavaConstructor(Classes.getDeclaredConstructor(java.lang.String.class, byte[].class, String.class)));
        if (JDK.java_io_UnixFileSystem.javaClass() != null) {
            CompiledPrototype.registerImageInvocationStub(MethodActor.fromJavaConstructor(Classes.getDeclaredConstructor(JDK.java_io_UnixFileSystem.javaClass())));
        }
        // Constructors that are invoked via reflection during startup; we want the invocation stub in the boot image to avoid compilation at run time
        CompiledPrototype.registerImageConstructorStub(MethodActor.fromJavaConstructor(Classes.getDeclaredConstructor(sun.net.www.protocol.jar.Handler.class)));

        // Packages and classes whose methods should not be compiled
        CompiledPrototype.addCompilationBlacklist("sun.security");
        CompiledPrototype.addCompilationBlacklist("sun.util.locale");
        CompiledPrototype.addCompilationBlacklist("java.util.logging");
        CompiledPrototype.addCompilationBlacklist("sun.util.logging");
        CompiledPrototype.addCompilationBlacklist("sun.util.calendar");
        CompiledPrototype.addCompilationBlacklist("sun.text.normalizer");
        CompiledPrototype.addCompilationBlacklist("sun.reflect.annotation");
        CompiledPrototype.addCompilationBlacklist("sun.reflect.generics");
        CompiledPrototype.addCompilationBlacklist("java.util.jar.JarVerifier");
        CompiledPrototype.addCompilationBlacklist("java.net");
        CompiledPrototype.addCompilationBlacklist("sun.nio.ch");

        // Exceptions from the above blacklisted packages
        CompiledPrototype.addCompilationWhitelist("sun.security.util.Debug");
        CompiledPrototype.addCompilationWhitelist("sun.security.provider.PolicyFile");
        CompiledPrototype.addCompilationWhitelist("sun.reflect.annotation.AnnotationParser");
        CompiledPrototype.addCompilationWhitelist("sun.reflect.Reflection");
        CompiledPrototype.addCompilationWhitelist("sun.reflect.ReflectionFactory");
        CompiledPrototype.addCompilationWhitelist("sun.security.action.GetPropertyAction");
        CompiledPrototype.addCompilationWhitelist("java.net.URL");
        CompiledPrototype.addCompilationWhitelist("java.net.Parts");
    }
}

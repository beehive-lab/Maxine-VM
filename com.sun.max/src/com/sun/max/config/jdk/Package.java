/*
 * Copyright (c) 2017, 2019, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2015, Andrey Rodchenko. All rights reserved.
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

        // The following are needed to make -Dsun.misc.URLClassPath.*=value make an effect
        Extensions.resetField("sun.misc.URLClassPath", "DEBUG");
        Extensions.resetField("sun.misc.URLClassPath", "DEBUG_LOOKUP_CACHE");
        Extensions.resetField("sun.misc.URLClassPath", "DISABLE_JAR_CHECKING");
        Extensions.registerClassForReInit("sun.misc.URLClassPath");
        // The following are needed to make -Djava.lang.invoke.MethodHandle.*=value make an effect
        Extensions.resetField("java.lang.invoke.MethodHandleStatics", "DEBUG_METHOD_HANDLE_NAMES");
        Extensions.resetField("java.lang.invoke.MethodHandleStatics", "DUMP_CLASS_FILES");
        Extensions.resetField("java.lang.invoke.MethodHandleStatics", "TRACE_INTERPRETER");
        Extensions.resetField("java.lang.invoke.MethodHandleStatics", "TRACE_METHOD_LINKAGE");
        Extensions.resetField("java.lang.invoke.MethodHandleStatics", "COMPILE_THRESHOLD");
        Extensions.registerClassForReInit("java.lang.invoke.MethodHandleStatics");
        // The following fields are affected by java.lang.invoke.MethodHandleStatics.DUMP_CLASS_FILES so they need to be reset too
        Extensions.resetField("java.lang.invoke.InvokerBytecodeGenerator", "DUMP_CLASS_FILES_DIR");
        Extensions.resetField("java.lang.invoke.InvokerBytecodeGenerator", "DUMP_CLASS_FILES_COUNTERS");
        Extensions.registerClassForReInit("java.lang.invoke.InvokerBytecodeGenerator");

        /* The following are from the java.lang.invoke and sun.invoke.util packages
         * and are to exclude from the boot image static stale MemberName objects.
         */
        if (JDK.JDK_VERSION == JDK.JDK_7) {
            // MethodHandle
            Extensions.resetField("java.lang.invoke.MethodHandle", "NF_reinvokerTarget");
            Extensions.registerClassForReInit("java.lang.invoke.MethodHandle");
        }

        // LambdaForm$NamedFunction
        Extensions.resetField("java.lang.invoke.LambdaForm$NamedFunction", "INVOKER_METHOD_TYPE");
        Extensions.registerClassForReInit("java.lang.invoke.LambdaForm$NamedFunction");

        if (JDK.JDK_VERSION == JDK.JDK_8) {
            Extensions.resetField("java.lang.invoke.MethodHandles", "ACCESS_PERMISSION");
            Extensions.resetField("java.lang.invoke.MethodHandles", "IDENTITY_MHS");
            Extensions.resetField("java.lang.invoke.MethodHandles", "ZERO_MHS");
        }
        Extensions.resetField("java.lang.invoke.MethodHandles", "IMPL_NAMES");
        Extensions.registerClassForReInit("java.lang.invoke.MethodHandles");

        // LambdaForm
        if (JDK.JDK_VERSION == JDK.JDK_7) {
            Extensions.resetField("java.lang.invoke.LambdaForm", "PREPARED_FORMS");
            Extensions.resetField("java.lang.invoke.LambdaForm", "CONSTANT_ZERO");
        }
        Extensions.resetField("java.lang.invoke.LambdaForm", "INTERNED_ARGUMENTS");
        Extensions.registerClassForReInit("java.lang.invoke.LambdaForm");

        // Invokers
        if (JDK.JDK_VERSION == JDK.JDK_7) {
            Extensions.resetField("java.lang.invoke.Invokers", "NF_asType");
        } else {
            Extensions.resetField("java.lang.invoke.Invokers", "NF_checkCustomized");
        }
        Extensions.resetField("java.lang.invoke.Invokers", "NF_checkExactType");
        Extensions.resetField("java.lang.invoke.Invokers", "NF_checkGenericType");
        Extensions.resetField("java.lang.invoke.Invokers", "NF_getCallSiteTarget");
        Extensions.registerClassForReInit("java.lang.invoke.Invokers");

        // ValueConversions
        if (JDK.JDK_VERSION == JDK.JDK_7) {
            Extensions.resetField("sun.invoke.util.ValueConversions", "IDENTITY");
            Extensions.resetField("sun.invoke.util.ValueConversions", "ZERO_OBJECT");
            Extensions.resetField("sun.invoke.util.ValueConversions", "ARRAY_IDENTITY");
            Extensions.resetField("sun.invoke.util.ValueConversions", "FILL_NEW_TYPED_ARRAY");
            Extensions.resetField("sun.invoke.util.ValueConversions", "FILL_NEW_ARRAY");
            Extensions.resetField("sun.invoke.util.ValueConversions", "COLLECT_ARGUMENTS");
            Extensions.resetField("sun.invoke.util.ValueConversions", "WRAPPER_CASTS");
            Extensions.resetField("sun.invoke.util.ValueConversions", "NO_ARGS_ARRAY");
            Extensions.resetField("sun.invoke.util.ValueConversions", "NO_ARGS_LIST");
            Extensions.resetField("sun.invoke.util.ValueConversions", "TYPED_COLLECTORS");
            Extensions.resetField("sun.invoke.util.ValueConversions", "ARRAYS");
            Extensions.resetField("sun.invoke.util.ValueConversions", "FILL_ARRAYS");
            Extensions.resetField("sun.invoke.util.ValueConversions", "FILL_ARRAY_TO_RIGHT");
            Extensions.resetField("sun.invoke.util.ValueConversions", "LISTS");
        }
        Extensions.resetField("sun.invoke.util.ValueConversions", "UNBOX_CONVERSIONS");
        Extensions.resetField("sun.invoke.util.ValueConversions", "BOX_CONVERSIONS");
        Extensions.resetField("sun.invoke.util.ValueConversions", "CONSTANT_FUNCTIONS");
        Extensions.resetField("sun.invoke.util.ValueConversions", "CAST_REFERENCE");
        Extensions.resetField("sun.invoke.util.ValueConversions", "IGNORE");
        Extensions.resetField("sun.invoke.util.ValueConversions", "EMPTY");
        Extensions.resetField("sun.invoke.util.ValueConversions", "CONVERT_PRIMITIVE_FUNCTIONS");
        Extensions.registerClassForReInit("sun.invoke.util.ValueConversions");

        Extensions.resetField("java.lang.invoke.MethodHandleImpl$BindCaller", "MH_checkCallerClass");
        Extensions.registerClassForReInit("java.lang.invoke.MethodHandleImpl$BindCaller");

        if (JDK.JDK_VERSION == JDK.JDK_8) {
            Extensions.resetField("java.lang.invoke.MethodHandles$Lookup", "LOOKASIDE_TABLE");
            Extensions.registerClassForReInit("java.lang.invoke.MethodHandles$Lookup");
            Extensions.resetField("java.lang.UNIXProcess", "processReaperExecutor");
            Extensions.registerClassForReInit("java.lang.UNIXProcess");
            Extensions.resetField("java.io.File", "fs");
            Extensions.registerClassForReInit("java.io.File");

            Extensions.resetField("java.util.concurrent.atomic.Striped64", "NCPU");
            Extensions.registerClassForReInit("java.util.concurrent.atomic.Striped64");

            Extensions.resetField("sun.misc.InnocuousThread", "ACC");
            Extensions.resetField("sun.misc.InnocuousThread", "INNOCUOUSTHREADGROUP");
            Extensions.registerClassForReInit("sun.misc.InnocuousThread");
        }
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
        HostedBootClassLoader.omitClass("java.net.InetAddress");
        HostedBootClassLoader.omitClass("java.net.InetSocketAddress");
        // The static initializer loads and initializes native libraries
        HostedBootClassLoader.omitClass("sun.nio.ch.FileDispatcherImpl");
        HostedBootClassLoader.omitClass("sun.nio.ch.FileChannelImpl");
        HostedBootClassLoader.omitClass("sun.nio.ch.Util");
        HostedBootClassLoader.omitClass("sun.nio.fs.UnixNativeDispatcher");
        HostedBootClassLoader.omitClass("sun.jkernel.Bundle");
        // Java 7 only class that indirectly caches references to JarFiles
        HostedBootClassLoader.omitClass(sun.misc.Launcher.class.getName() + "$BootClassPathHolder");
        HostedBootClassLoader.omitClass("java.lang.invoke.BoundMethodHandle");

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
        CompiledPrototype.addCompilationBlacklist("java.util.logging");
        CompiledPrototype.addCompilationBlacklist("sun.util.logging");
        CompiledPrototype.addCompilationBlacklist("sun.util.calendar");
        CompiledPrototype.addCompilationBlacklist("sun.text.normalizer");
        CompiledPrototype.addCompilationBlacklist("sun.reflect.annotation");
        CompiledPrototype.addCompilationBlacklist("sun.reflect.generics");
        CompiledPrototype.addCompilationBlacklist("java.util.jar.JarVerifier");
        CompiledPrototype.addCompilationBlacklist("java.net");
        CompiledPrototype.addCompilationBlacklist("sun.nio.ch");
        /*
         * Avoid caching problems with MemberNames/LambdaForms in MethodHandles.
         */
        CompiledPrototype.addCompilationBlacklist("java.lang.invoke.MethodHandleImpl");
        CompiledPrototype.addCompilationBlacklist("java.lang.invoke.DirectMethodHandle");
        CompiledPrototype.addCompilationBlacklist("java.lang.invoke.CallSite");

        if (JDK.JDK_VERSION == JDK.JDK_8) {
            // Avoid Lambdas
            CompiledPrototype.addCompilationBlacklist("java.lang.UNIXProcess");
            // Depends on java.lang.invoke.BoundMethodHandle which is omitted
            CompiledPrototype.addCompilationBlacklist("java.lang.invoke.LambdaForm");
            // Classes in these packages break compilation
            CompiledPrototype.addCompilationBlacklist("java.time");
            CompiledPrototype.addCompilationBlacklist("java.io.ObjectStreamClass");
            CompiledPrototype.addCompilationBlacklist("sun.nio.fs.MacOSXFileSystem");
        }

        // Exceptions from the above blacklisted packages
        CompiledPrototype.addCompilationWhitelist("sun.security.util.Debug");
        CompiledPrototype.addCompilationWhitelist("sun.security.provider.PolicyFile");
        CompiledPrototype.addCompilationWhitelist("sun.reflect.annotation.AnnotationParser");
        CompiledPrototype.addCompilationWhitelist("sun.reflect.Reflection");
        CompiledPrototype.addCompilationWhitelist("sun.reflect.ReflectionFactory");
        CompiledPrototype.addCompilationWhitelist("sun.security.action.GetPropertyAction");
        CompiledPrototype.addCompilationWhitelist("sun.reflect.annotation.AnnotationType");
        CompiledPrototype.addCompilationWhitelist("sun.reflect.annotation.AnnotationInvocationHandler");
        CompiledPrototype.addCompilationWhitelist("sun.reflect.generics.factory.CoreReflectionFactory");
        CompiledPrototype.addCompilationWhitelist("sun.reflect.generics.parser.SignatureParser");
        CompiledPrototype.addCompilationWhitelist("sun.reflect.generics.tree.SimpleClassTypeSignature");
        CompiledPrototype.addCompilationWhitelist("sun.reflect.generics.tree.ClassTypeSignature");
        CompiledPrototype.addCompilationWhitelist("sun.reflect.generics.visitor.Reifier");
        CompiledPrototype.addCompilationWhitelist("sun.util.locale.BaseLocale");
        CompiledPrototype.addCompilationWhitelist("java.net.URL");
        CompiledPrototype.addCompilationWhitelist("java.net.Parts");

        CompiledPrototype.registerVMEntryPoint("com.sun.max.vm.jdk.BitSetProxy.*"); // TODO move
    }
}

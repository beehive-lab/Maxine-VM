/*
 * Copyright (c) 2017-2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
 */
package com.sun.max.vm.hosted;

import java.io.*;
import java.lang.invoke.*;
import java.lang.ref.*;
import java.lang.reflect.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import sun.misc.*;

import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.jdk.JDK.ClassRef;
import com.sun.max.vm.jdk.JDK.LazyClassRef;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * This class encapsulates a number of mechanisms for intercepting certain parts of JDK
 * state for the purpose of modifying how that state is copied into the boot image.
 */
public final class JDKInterceptor {

    private JDKInterceptor() {
    }

    /**
     * These are the properties that are to be remembered (inherited) from the host VM on which the image was built.
     * Strictly speaking java vendor.* should not be inherited. However, from JDK 7 onwards all the platforms on which
     * Maxine is built have Oracle as the vendor so it is ok.
     *
     * Evidently several of these properties are sensitive to the environment the VM is running in.
     * This isn't important for the typical current uses of Maxine, but should be fixed in due course.
     * The problem is that finding all the JDK code that might be baked into the image and depends
     * on such a property. For now, it is better to inherit the host VM value, than simply guess it
     * on startup, because most users run the VM in the same environment where they build it.
     */
    public static final String[] REMEMBERED_PROPERTY_NAMES = {
        "awt.toolkit",
        "java.specification.version",
        "java.specification.name",
        "java.class.version",
        "java.vendor",
        "java.vendor.url",
        "java.vendor.url.bug",
        "java.runtime.name",
        "java.runtime.version",
        "java.awt.graphicsenv",
        "java.awt.printerjob",
        "file.encoding",
        "file.encoding.pkg",
        "file.separator",
        "line.separator",
        "path.separator",
        "sun.jnu.encoding",
        "sun.io.unicode.encoding",
        "sun.os.patch.level",
        "os.version",
        "user.language",
        "user.country",
        "user.variant"
    };

    public static final Properties initialSystemProperties = buildInitialSystemProperties();

    /**
     * Overrides the default VM library path with the specified new path.
     * We use this to avoid having to specify command line arguments
     * over and over again for each and every JUnit test and other misc. program runs.
     *
     * @param paths a string representing the new paths to use in looking up native libraries
     */
    public static void setLibraryPath(String paths) {
        final String[] pathArray = paths.split(File.pathSeparator);
        WithoutAccessCheck.setStaticField(ClassLoader.class, "usr_paths", pathArray);
    }

    private static final Vector<Object> systemNativeLibraries = new Vector<Object>();
    static {
        Object lib = null;
        try {
            Class<?> c = JDK.java_lang_ClassLoader$NativeLibrary.javaClass();
            Constructor cons = (JDK.JDK_VERSION == JDK.JDK_8) ?
                c.getConstructor(Class.class, String.class, boolean.class) :
                c.getConstructor(Class.class, String.class);
            cons.setAccessible(true);
            lib = (JDK.JDK_VERSION == JDK.JDK_8) ?
                cons.newInstance(MaxineVM.class, "maxvm", true) :
                cons.newInstance(MaxineVM.class, "maxvm");
        } catch (Exception e) {
            throw FatalError.unexpected("Could not construct VM native library", e);
        }
        systemNativeLibraries.add(lib);
    }

    private static Object internTable;

    static {
        try {
            Class <?> c;
            if (JDK.JDK_VERSION == JDK.JDK_7) {
                c = JDK.java_lang_invoke_MethodType$WeakInternSet.javaClass();
            } else {
                c = JDK.java_lang_invoke_MethodType$ConcurrentWeakInternSet.javaClass();
            }
            Constructor cons = c.getDeclaredConstructors()[0];
            cons.setAccessible(true);
            internTable = cons.newInstance();
        } catch (Exception e) {
            throw FatalError.unexpected("Unable to create WeakInternSet xtor", e);
        }
    }


    /**
     * This array contains all the intercepted fields that are either ignored (i.e. set to zero) or
     * specially handled when building the prototype.
     */
    // Checkstyle: stop
    private static final Object[] interceptedFieldArray = {
        JDK.java_lang_ApplicationShutdownHooks,
            new ValueField("hooks", ReferenceValue.from(new IdentityHashMap<Thread, Thread>())).makeNonFinal(),
        JDK.java_lang_Class,
            new ZeroField("classLoader").makeOptional(),
            "cachedConstructor",
            "newInstanceCallerCache",
            "name",
            "genericInfo",
            "enumConstants",
            "enumConstantDirectory",
            "classRedefinedCount",
            "reflectionData",
        JDK.java_lang_Class$Atomic,
            new FieldOffsetRecomputation("reflectionDataOffset", JDK.java_lang_Class, "reflectionData"),
            new FieldOffsetRecomputation("annotationTypeOffset", JDK.java_lang_Class, "annotationType"),
        JDK.java_lang_ClassLoader,
            new ZeroField("bootstrapClassPath").makeOptional(),
            "scl",
            "sclSet",
            "usr_paths",
            "sys_paths",
            new ValueField("loadedLibraryNames", ReferenceValue.from(new Vector())).makeNonFinal(),
            new ValueField("systemNativeLibraries", ReferenceValue.from(systemNativeLibraries)).makeNonFinal(),
            new ValueField("parallelLockMap", ReferenceValue.from(new ConcurrentHashMap<String, Object>())).makeNonFinal(),
        JDK.java_util_EnumMap,
            "entrySet",
        JDK.java_lang_invoke_CallSite,
            new FieldOffsetRecomputation("TARGET_OFFSET", "target"),
        JDK.java_lang_invoke_MethodHandle,
            new FieldOffsetRecomputation("FORM_OFFSET", "form"),
        JDK.java_lang_invoke_MethodType,
            new FieldOffsetRecomputation("ptypesOffset", "ptypes"),
            new FieldOffsetRecomputation("rtypeOffset", "rtype"),
            new ValueField("internTable", ReferenceValue.from(internTable)).makeNonFinal(),
            new ValueField("objectOnlyTypes", ReferenceValue.from(Array.newInstance(MethodType.class, 20))).makeNonFinal(),
        JDK.java_lang_invoke_LambdaForm,
            "vmentry",
        JDK.java_lang_reflect_Field,
            "genericInfo",
            "declaredAnnotations",
            "fieldAccessor",
            "overrideFieldAccessor",
        JDK.java_lang_reflect_Constructor,
            "genericInfo",
            "constructorAccessor",
        JDK.java_lang_reflect_Method,
            "genericInfo",
            "methodAccessor",
        JDK.java_lang_Package,
            "loader",
            "packageInfo",
        JDK.java_lang_Shutdown,
            new NewShutdownHookList("hooks").makeNonFinal(),
        JDK.java_lang_System,
            new ZeroField("security").makeOptional(),
            new ValueField("props", ReferenceValue.from(initialSystemProperties)).makeNonFinal(),
        JDK.java_lang_ref_Reference,
            "discovered",
            "pending",
            "referent",
        JDK.java_lang_ref_Finalizer,
            "unfinalized",
            new ValueField("queue", ReferenceValue.from(new ReferenceQueue())),
            new ValueField("lock", ReferenceValue.from(new Object())),
        JDK.java_lang_Throwable,
            new ZeroField("backtrace").makeOptional(),
        JDK.java_lang_Thread,
            "parkBlocker",
            "blocker",
            "threadLocals",
            "inheritableThreadLocals",
        JDK.java_lang_ProcessEnvironment,
            new ZeroField("theEnvironment").makeNonFinal(),
            new ZeroField("theUnmodifiableEnvironment").makeNonFinal(),
        JDK.java_lang_Terminator,
            "handler",
        JDK.sun_misc_InnocuousThread,
            new FieldOffsetRecomputation("THREAD_LOCALS", JDK.java_lang_Thread, "threadLocals"),
            new FieldOffsetRecomputation("INHERITABLE_THREAD_LOCALS", JDK.java_lang_Thread, "inheritableThreadLocals"),
            new FieldOffsetRecomputation("INHERITEDACCESSCONTROLCONTEXT", JDK.java_lang_Thread, "inheritedAccessControlContext"),
            new FieldOffsetRecomputation("CONTEXTCLASSLOADER", JDK.java_lang_Thread, "contextClassLoader"),
        JDK.sun_misc_PerfCounter,
            new ValueField("lb", ReferenceValue.from(LongBuffer.allocate(1))).makeNonFinal(),
        JDK.sun_misc_ProxyGenerator,
            new ValueField("saveGeneratedFiles", BooleanValue.from(false)).makeNonFinal(),
        JDK.sun_misc_VM,
            "booted",
            "finalRefCount",
            "peakFinalRefCount",
        JDK.sun_misc_Unsafe,
            new ArrayBaseOffsetRecomputation("ARRAY_BOOLEAN_BASE_OFFSET", boolean[].class),
            new ArrayBaseOffsetRecomputation("ARRAY_BYTE_BASE_OFFSET", byte[].class),
            new ArrayBaseOffsetRecomputation("ARRAY_SHORT_BASE_OFFSET", short[].class),
            new ArrayBaseOffsetRecomputation("ARRAY_CHAR_BASE_OFFSET", char[].class),
            new ArrayBaseOffsetRecomputation("ARRAY_INT_BASE_OFFSET", int[].class),
            new ArrayBaseOffsetRecomputation("ARRAY_LONG_BASE_OFFSET", long[].class),
            new ArrayBaseOffsetRecomputation("ARRAY_FLOAT_BASE_OFFSET", float[].class),
            new ArrayBaseOffsetRecomputation("ARRAY_DOUBLE_BASE_OFFSET", double[].class),
            new ArrayBaseOffsetRecomputation("ARRAY_OBJECT_BASE_OFFSET", Object[].class),
            new ArrayIndexScaleRecomputation("ARRAY_BOOLEAN_INDEX_SCALE", boolean[].class),
            new ArrayIndexScaleRecomputation("ARRAY_BYTE_INDEX_SCALE", byte[].class),
            new ArrayIndexScaleRecomputation("ARRAY_SHORT_INDEX_SCALE", short[].class),
            new ArrayIndexScaleRecomputation("ARRAY_CHAR_INDEX_SCALE", char[].class),
            new ArrayIndexScaleRecomputation("ARRAY_INT_INDEX_SCALE", int[].class),
            new ArrayIndexScaleRecomputation("ARRAY_LONG_INDEX_SCALE", long[].class),
            new ArrayIndexScaleRecomputation("ARRAY_FLOAT_INDEX_SCALE", float[].class),
            new ArrayIndexScaleRecomputation("ARRAY_DOUBLE_INDEX_SCALE", double[].class),
            new ArrayIndexScaleRecomputation("ARRAY_OBJECT_INDEX_SCALE", Object[].class),
        JDK.sun_misc_URLClassPath,
            new ValueField("loaders", ReferenceValue.from(new ArrayList())).makeNonFinal(),
            new ValueField("lmap", ReferenceValue.from(new HashMap())).makeNonFinal(),
        JDK.sun_net_www_protocol_jar_JarFileFactory,
            new ValueField("fileCache", ReferenceValue.from(new HashMap())).makeNonFinal(),
            new ValueField("urlCache", ReferenceValue.from(new HashMap())).makeNonFinal(),
        JDK.sun_reflect_ConstantPool,
            new ZeroField("constantPoolOop").makeOptional(),
        JDK.sun_reflect_Reflection,
            new ValueField("fieldFilterMap", ReferenceValue.from(new HashMap<Class, String[]>())).makeOptional(),
            new ValueField("methodFilterMap", ReferenceValue.from(new HashMap<Class, String[]>())).makeOptional(),
        JDK.sun_security_jca_ProviderConfig,
            new ValueField("LOCK", ReferenceValue.from(new Object())).makeOptional(),
        JDK.sun_security_krb5_PrincipalName,
            new FieldOffsetRecomputation("NAME_STRINGS_OFFSET", "nameStrings"),
        JDK.sun_security_provider_ByteArrayAccess,
            new ArrayBaseOffsetRecomputation("byteArrayOfs", byte[].class),
        JDK.java_util_Random,
            new FieldOffsetRecomputation("seedOffset", "seed"),
        JDK.java_util_concurrent_ConcurrentSkipListSet,
            new FieldOffsetRecomputation("mapOffset", "m"),
        JDK.java_util_concurrent_ConcurrentLinkedQueue,
            new FieldOffsetRecomputation("headOffset", "head"),
            new FieldOffsetRecomputation("tailOffset", "tail"),
        JDK.java_util_concurrent_CopyOnWriteArrayList,
            new FieldOffsetRecomputation("lockOffset", "lock"),
        JDK.java_net_Inet6Address,
            new FieldOffsetRecomputation("FIELDS_OFFSET", "holder").makeOptional(),
        JDK.java_net_InetAddress,
            new FieldOffsetRecomputation("FIELDS_OFFSET", "holder").makeOptional(),
        JDK.java_net_InetSocketAddress,
            new FieldOffsetRecomputation("FIELDS_OFFSET", "holder").makeOptional(),
        JDK.java_nio_Bits,
            "unaligned", // We just intercept it to avoid warning about unsafe accesses
        JDK.java_nio_DirectByteBuffer,
            new ArrayBaseOffsetRecomputation("arrayBaseOffset", byte[].class),
        JDK.java_nio_DirectCharBufferS,
            new ArrayBaseOffsetRecomputation("arrayBaseOffset", char[].class),
        JDK.java_nio_DirectCharBufferU,
            new ArrayBaseOffsetRecomputation("arrayBaseOffset", char[].class),
        JDK.java_nio_DirectDoubleBufferS,
            new ArrayBaseOffsetRecomputation("arrayBaseOffset", double[].class),
        JDK.java_nio_DirectDoubleBufferU,
            new ArrayBaseOffsetRecomputation("arrayBaseOffset", double[].class),
        JDK.java_nio_DirectFloatBufferS,
            new ArrayBaseOffsetRecomputation("arrayBaseOffset", float[].class),
        JDK.java_nio_DirectFloatBufferU,
            new ArrayBaseOffsetRecomputation("arrayBaseOffset", float[].class),
        JDK.java_nio_DirectIntBufferS,
            new ArrayBaseOffsetRecomputation("arrayBaseOffset", int[].class),
        JDK.java_nio_DirectIntBufferU,
            new ArrayBaseOffsetRecomputation("arrayBaseOffset", int[].class),
        JDK.java_nio_DirectLongBufferS,
            new ArrayBaseOffsetRecomputation("arrayBaseOffset", long[].class),
        JDK.java_nio_DirectLongBufferU,
            new ArrayBaseOffsetRecomputation("arrayBaseOffset", long[].class),
        JDK.java_nio_DirectShortBufferS,
            new ArrayBaseOffsetRecomputation("arrayBaseOffset", short[].class),
        JDK.java_nio_DirectShortBufferU,
            new ArrayBaseOffsetRecomputation("arrayBaseOffset", short[].class),
        JDK.java_nio_charset_CharsetEncoder,
            "cachedDecoder",
        JDK.java_util_concurrent_atomic_AtomicBoolean,
            new FieldOffsetRecomputation("valueOffset", "value"),
        JDK.java_util_concurrent_atomic_AtomicInteger,
            new FieldOffsetRecomputation("valueOffset", "value"),
        JDK.java_util_concurrent_atomic_AtomicLong,
            new FieldOffsetRecomputation("valueOffset", "value"),
        JDK.java_util_concurrent_atomic_AtomicMarkableReference,
            new FieldOffsetRecomputation("pairOffset", "pair"),
        JDK.java_util_concurrent_atomic_AtomicReference,
            new FieldOffsetRecomputation("valueOffset", "value"),
        JDK.java_util_concurrent_atomic_AtomicIntegerArray,
            new ArrayBaseOffsetRecomputation("base", int[].class),
            new ArrayIndexScaleShiftRecomputation("shift", int[].class),
        JDK.java_util_concurrent_atomic_AtomicLongArray,
            new ArrayBaseOffsetRecomputation("base", long[].class),
            new ArrayIndexScaleShiftRecomputation("shift", long[].class),
        JDK.java_util_concurrent_atomic_AtomicReferenceArray,
            new FieldOffsetRecomputation("arrayFieldOffset", "array"),
            new ArrayBaseOffsetRecomputation("base", Object[].class),
            new ArrayIndexScaleShiftRecomputation("shift", Object[].class),
        JDK.java_util_concurrent_atomic_AtomicReferenceFieldUpdater$AtomicReferenceFieldUpdaterImpl,
            new AtomicFieldUpdaterOffsetRecomputation("offset"),
        JDK.java_util_concurrent_atomic_AtomicIntegerFieldUpdater$AtomicIntegerFieldUpdaterImpl,
            new AtomicFieldUpdaterOffsetRecomputation("offset"),
        JDK.java_util_concurrent_atomic_AtomicLongFieldUpdater$CASUpdater,
            new AtomicFieldUpdaterOffsetRecomputation("offset"),
        JDK.java_util_concurrent_atomic_AtomicLongFieldUpdater$LockedUpdater,
            new AtomicFieldUpdaterOffsetRecomputation("offset"),
        JDK.java_util_concurrent_atomic_AtomicStampedReference,
            new FieldOffsetRecomputation("pairOffset", "pair"),
        JDK.java_util_concurrent_atomic_Striped64,
            new FieldOffsetRecomputation("BASE", "base"),
            new FieldOffsetRecomputation("CELLSBUSY", "cellsBusy"),
            new FieldOffsetRecomputation("PROBE", JDK.java_lang_Thread, "threadLocalRandomProbe"),
        JDK.java_io_UnixFileSystem,
            new ExpiringCacheField("cache"),
            new ExpiringCacheField("javaHomePrefixCache"),
        JDK.java_util_concurrent_ConcurrentLinkedQueue$Node,
            new FieldOffsetRecomputation("itemOffset", "item"),
            new FieldOffsetRecomputation("nextOffset", "next"),
        JDK.java_util_concurrent_locks_AbstractQueuedSynchronizer,
            new FieldOffsetRecomputation("stateOffset", "state"),
            new FieldOffsetRecomputation("headOffset", "head"),
            new FieldOffsetRecomputation("tailOffset", "tail"),
            new FieldOffsetRecomputation("waitStatusOffset", JDK.java_util_concurrent_locks_AbstractQueuedSynchronizer$Node, "waitStatus"),
            new FieldOffsetRecomputation("nextOffset", JDK.java_util_concurrent_locks_AbstractQueuedSynchronizer$Node, "next"),
        JDK.java_util_concurrent_locks_AbstractQueuedLongSynchronizer,
            new FieldOffsetRecomputation("stateOffset", "state"),
            new FieldOffsetRecomputation("headOffset", "head"),
            new FieldOffsetRecomputation("tailOffset", "tail"),
            new FieldOffsetRecomputation("waitStatusOffset", JDK.java_util_concurrent_locks_AbstractQueuedLongSynchronizer$Node, "waitStatus"),
            new FieldOffsetRecomputation("nextOffset", JDK.java_util_concurrent_locks_AbstractQueuedLongSynchronizer$Node, "next"),
        JDK.java_util_concurrent_locks_LockSupport,
            new FieldOffsetRecomputation("parkBlockerOffset", JDK.java_lang_Thread, "parkBlocker"),
        JDK.java_util_concurrent_SynchronousQueue$TransferStack,
            new FieldOffsetRecomputation("headOffset", "head"),
        JDK.java_util_concurrent_SynchronousQueue$TransferStack$SNode,
            new FieldOffsetRecomputation("matchOffset", "match"),
            new FieldOffsetRecomputation("nextOffset", "next"),
        JDK.java_util_concurrent_SynchronousQueue$TransferQueue,
            new FieldOffsetRecomputation("headOffset", "head"),
            new FieldOffsetRecomputation("tailOffset", "tail"),
            new FieldOffsetRecomputation("cleanMeOffset", "cleanMe"),
        JDK.java_util_concurrent_SynchronousQueue$TransferQueue$QNode,
            new FieldOffsetRecomputation("itemOffset", "item"),
            new FieldOffsetRecomputation("nextOffset", "next"),
    };

    private static final Object[] interceptedFieldArrayJDK7 = {
        JDK.java_lang_Class,
            "annotations",
            "declaredAnnotations",
        JDK.java_lang_Class$ReflectionData,
            "declaredFields",
            "publicFields",
            "declaredMethods",
            "publicMethods",
            "declaredConstructors",
            "declaredPublicFields",
            "declaredPublicMethods",
        JDK.java_lang_invoke_MethodHandle,
            "NF_reinvokerTarget",
        JDK.java_lang_reflect_Constructor,
            "declaredAnnotations",
        JDK.java_lang_reflect_Method,
            "declaredAnnotations",
        JDK.sun_util_calendar_ZoneInfo,
            new ZeroField("aliasTable").makeNonFinal(),
        JDK.java_util_concurrent_ConcurrentHashMap,
            new ArrayBaseOffsetRecomputation("TBASE", Object[].class),
            new ArrayIndexScaleShiftRecomputation("TSHIFT", Object[].class),
            new ArrayBaseOffsetRecomputation("SBASE", Object[].class),
            new ArrayIndexScaleShiftRecomputation("SSHIFT", Object[].class),
        JDK.java_util_concurrent_ConcurrentHashMap$HashEntry,
            new FieldOffsetRecomputation("nextOffset", "next"),
        JDK.java_util_concurrent_ForkJoinPool,
            new ArrayBaseOffsetRecomputation("ABASE", Object[].class),
            new ArrayIndexScaleShiftRecomputation("ASHIFT", Object[].class),
            new FieldOffsetRecomputation("ctlOffset", "ctl"),
            new FieldOffsetRecomputation("stealCountOffset", "stealCount"),
            new FieldOffsetRecomputation("blockedCountOffset", "blockedCount"),
            new FieldOffsetRecomputation("quiescerCountOffset", "quiescerCount"),
            new FieldOffsetRecomputation("scanGuardOffset", "scanGuard"),
            new FieldOffsetRecomputation("nextWorkerNumberOffset", "nextWorkerNumber"),
        JDK.java_util_concurrent_ForkJoinWorkerThread,
            new ArrayBaseOffsetRecomputation("ABASE", Object[].class),
            new ArrayIndexScaleShiftRecomputation("ASHIFT", Object[].class),
        JDK.java_util_concurrent_ForkJoinTask,
            new FieldOffsetRecomputation("statusOffset", "status"),
        // Added in JDK 7 update 15
        JDK.java_math_BigInteger,
            new FieldOffsetRecomputation("signumOffset", "signum"),
            new FieldOffsetRecomputation("magOffset", "mag"),
    };

    private static final Object[] interceptedFieldArrayJDK8 = {
        JDK.java_io_File,
            new FieldOffsetRecomputation("PATH_OFFSET", "path"),
            new FieldOffsetRecomputation("PREFIX_LENGTH_OFFSET", "prefixLength"),
        JDK.java_lang_Class,
            "annotationData",
        JDK.java_lang_Class$AnnotationData,
            "annotations",
            "declaredAnnotations",
        JDK.java_lang_Class$ReflectionData,
            "declaredFields",
            "publicFields",
            "declaredMethods",
            "publicMethods",
            "declaredConstructors",
            "publicConstructors",
            "declaredPublicFields",
            "declaredPublicMethods",
        JDK.java_lang_Class$Atomic,
            new FieldOffsetRecomputation("annotationDataOffset", JDK.java_lang_Class, "annotationData"),
        JDK.java_lang_invoke_CallSite,
            "THROW_UCS",
            "GET_TARGET",
        JDK.java_lang_invoke_LambdaForm,
            "LF_identityForm",
            "LF_zeroForm",
            "NF_identity",
            "NF_zero",
        JDK.java_lang_reflect_Executable,
            "declaredAnnotations",
        JDK.java_math_BigInteger$UnsafeHolder,
            new FieldOffsetRecomputation("signumOffset", JDK.java_math_BigInteger, "signum"),
            new FieldOffsetRecomputation("magOffset", JDK.java_math_BigInteger, "mag"),
        JDK.java_util_concurrent_CompletableFuture,
            new FieldOffsetRecomputation("RESULT", "result"),
            new FieldOffsetRecomputation("STACK", "stack"),
            new FieldOffsetRecomputation("NEXT", JDK.java_util_concurrent_CompletableFuture$Completion, "next"),
        JDK.java_util_concurrent_ConcurrentHashMap,
            new FieldOffsetRecomputation("SIZECTL", "sizeCtl"),
            new FieldOffsetRecomputation("TRANSFERINDEX", "transferIndex"),
            new FieldOffsetRecomputation("BASECOUNT", "baseCount"),
            new FieldOffsetRecomputation("CELLSBUSY", "cellsBusy"),
            new FieldOffsetRecomputation("CELLVALUE", JDK.java_util_concurrent_ConcurrentHashMap$CounterCell, "value"),
            new ArrayBaseOffsetRecomputation("ABASE", Object[].class),
            new ArrayIndexScaleShiftRecomputation("ASHIFT", Object[].class),
        JDK.java_util_concurrent_ConcurrentHashMap$TreeBin,
            new FieldOffsetRecomputation("LOCKSTATE", "lockState"),
        JDK.java_util_concurrent_Exchanger,
            new FieldOffsetRecomputation("BOUND", "bound"),
            new FieldOffsetRecomputation("SLOT", "slot"),
            new FieldOffsetRecomputation("MATCH", JDK.java_util_concurrent_Exchanger$Node, "match"),
            new FieldOffsetRecomputation("BLOCKER", JDK.java_lang_Thread, "parkBlocker"),
            // ABASE absorbs padding in front of element 0 (See Exchanger.java)
            new ArrayBaseOffsetRecomputation("ABASE", Object[].class, (1 << 7)),
        JDK.java_util_concurrent_ForkJoinPool,
            new FieldOffsetRecomputation("CTL", "ctl"),
            new FieldOffsetRecomputation("RUNSTATE", "runState"),
            new FieldOffsetRecomputation("STEALCOUNTER", "stealCounter"),
            new FieldOffsetRecomputation("PARKBLOCKER", JDK.java_lang_Thread, "parkBlocker"),
            new FieldOffsetRecomputation("QTOP", JDK.java_util_concurrent_ForkJoinPool$WorkQueue, "top"),
            new FieldOffsetRecomputation("QLOCK", JDK.java_util_concurrent_ForkJoinPool$WorkQueue, "qlock"),
            new FieldOffsetRecomputation("QSCANSTATE", JDK.java_util_concurrent_ForkJoinPool$WorkQueue, "scanState"),
            new FieldOffsetRecomputation("QPARKER", JDK.java_util_concurrent_ForkJoinPool$WorkQueue, "parker"),
            new FieldOffsetRecomputation("QCURRENTSTEAL", JDK.java_util_concurrent_ForkJoinPool$WorkQueue, "currentSteal"),
            new FieldOffsetRecomputation("QCURRENTJOIN", JDK.java_util_concurrent_ForkJoinPool$WorkQueue, "currentJoin"),
            new ArrayBaseOffsetRecomputation("ABASE", Object[].class),
            new ArrayIndexScaleShiftRecomputation("ASHIFT", Object[].class),
        JDK.java_util_concurrent_ForkJoinPool$WorkQueue,
            new ArrayBaseOffsetRecomputation("ABASE", Object[].class),
            new ArrayIndexScaleShiftRecomputation("ASHIFT", Object[].class),
            new FieldOffsetRecomputation("QTOP", "top"),
            new FieldOffsetRecomputation("QLOCK", "qlock"),
            new FieldOffsetRecomputation("QCURRENTSTEAL", "currentSteal"),
        JDK.java_util_concurrent_ForkJoinWorkerThread,
            new FieldOffsetRecomputation("THREADLOCALS", JDK.java_lang_Thread, "threadLocals"),
            new FieldOffsetRecomputation("INHERITABLETHREADLOCALS", JDK.java_lang_Thread, "inheritableThreadLocals"),
            new FieldOffsetRecomputation("INHERITEDACCESSCONTROLCONTEXT", JDK.java_lang_Thread, "inheritedAccessControlContext"),
        JDK.java_util_concurrent_ForkJoinTask,
            new FieldOffsetRecomputation("STATUS", "status"),
        JDK.java_util_concurrent_locks_ReentrantReadWriteLock,
            new FieldOffsetRecomputation("TID_OFFSET", JDK.java_lang_Thread, "tid"),
        JDK.java_util_concurrent_locks_StampedLock,
            new FieldOffsetRecomputation("STATE", "state"),
            new FieldOffsetRecomputation("WHEAD", "whead"),
            new FieldOffsetRecomputation("WTAIL", "wtail"),
            new FieldOffsetRecomputation("WSTATUS", JDK.java_util_concurrent_locks_StampedLock$WNode, "status"),
            new FieldOffsetRecomputation("WNEXT", JDK.java_util_concurrent_locks_StampedLock$WNode, "next"),
            new FieldOffsetRecomputation("WCOWAIT", JDK.java_util_concurrent_locks_StampedLock$WNode, "cowait"),
            new FieldOffsetRecomputation("PARKBLOCKER", JDK.java_lang_Thread, "parkBlocker"),
        JDK.java_util_concurrent_ThreadLocalRandom,
            new FieldOffsetRecomputation("SEED", JDK.java_lang_Thread, "threadLocalRandomSeed"),
            new FieldOffsetRecomputation("PROBE", JDK.java_lang_Thread, "threadLocalRandomProbe"),
            new FieldOffsetRecomputation("SECONDARY", JDK.java_lang_Thread, "threadLocalRandomSecondarySeed"),
        JDK.java_time_chrono_AbstractChronology,
            "DATE_ORDER",
            "DATE_TIME_ORDER",
            "INSTANT_ORDER",
        JDK.java_time_format_DateTimeFormatter,
            "ISO_DATE_TIME",
            "ISO_ZONED_DATE_TIME",
            "PARSED_EXCESS_DAYS",
            "PARSED_LEAP_SECOND",
        JDK.java_time_format_DateTimeFormatterBuilder,
            "QUERY_REGION_ONLY",
        JDK.java_time_temporal_TemporalQueries,
            "CHRONO",
            "LOCAL_DATE",
            "LOCAL_TIME",
            "OFFSET",
            "PRECISION",
            "ZONE",
            "ZONE_ID",
        JDK.sun_misc_Launcher$ExtClassLoader,
            "instance",
        JDK.sun_misc_SharedSecrets,
            "javaObjectInputStreamAccess",
            new ZeroField("javaObjectInputStreamReadString").makeOptional(), // Not present before jdk8u242
        JDK.sun_launcher_LauncherHelper,
            "appClass",
   };
    // Checkstyle: resume

    private static final Map<String, Map<String, InterceptedField>> interceptedFieldMap = buildInterceptedFieldMap();

    /**
     * Checks whether the specified field should be omitted.
     *
     * @param field the field to check
     * @return {@code true} if the field should be omitted
     */
    public static boolean isOmittedField(Field field) {
        return field.getName().startsWith("$SWITCH_TABLE") || getInterceptedField(field) instanceof ZeroField;
    }

    public static InterceptedField getInterceptedField(FieldActor fieldActor) {
        return getInterceptedField(fieldActor.holder().name.toString(), fieldActor.name.toString());
    }

    public static InterceptedField getInterceptedField(Field field) {
        return getInterceptedField(field.getDeclaringClass().getName(), field.getName());
    }

    public static void addInterceptedField(String className, InterceptedField interceptedField) {
        Map<String, InterceptedField> map = interceptedFieldMap.get(className);
        if (map == null) {
            map = new HashMap<String, InterceptedField>();
            interceptedFieldMap.put(className, map);
        }
        FatalError.check(map.put(interceptedField.getName(), interceptedField) == null,
                        "attempted redefinition of intercepted field: " + className + "." + interceptedField.getName());
    }

    private static InterceptedField getInterceptedField(String className, String fieldName) {
        final Map<String, InterceptedField> map = interceptedFieldMap.get(className);
        if (map != null) {
            return map.get(fieldName);
        }

        if (UnsafeUsageChecker.isClassUsingUnsafe(className)) {
            throw ProgramError.unexpected("class is using Unsafe operations to get field or array offsets \"" + fieldName + "\", but no field interceptor present: " + className);
        }

        return null;
    }

    public static boolean hasMutabilityOverride(FieldActor fieldActor) {
        InterceptedField f = getInterceptedField(fieldActor);
        return f != null && f.mutabilityOverride;
    }

    private static Map<String, Map<String, InterceptedField>> buildInterceptedFieldMap() {
        Map<String, Map<String, InterceptedField>> map = new HashMap<String, Map<String, InterceptedField>>();
        fillInterceptedFieldMap(map, interceptedFieldArray);
        if (JDK.JDK_VERSION == JDK.JDK_7) {
            fillInterceptedFieldMap(map, interceptedFieldArrayJDK7);
        } else if (JDK.JDK_VERSION == JDK.JDK_8) {
            fillInterceptedFieldMap(map, interceptedFieldArrayJDK8);
        }
        return map;
    }
    /**
     * Builds a map that stores the intercepted fields for each class.
     * @param specification an array of objects consisting of a ClassRef followed by a non-empty sequence of either
     * String objects or InterceptedField objects.
     */
    private static void fillInterceptedFieldMap(Map<String, Map<String, InterceptedField>> map, Object[] specification) {
        int i = 0;
        for (; i < specification.length; i++) {
            final Object object = specification[i];
            if (object instanceof ClassRef) {
                // we found a classref, add it and its intercepted fields to the map
                ClassRef holder = (ClassRef) object;
                if (holder.javaClass() == null) {
                    // Optional/platform dependent class: skips its field specs
                    while (i + 1 < specification.length && !(specification[i + 1] instanceof ClassRef)) {
                        i++;
                    }
                    continue;
                }
                Map<String, InterceptedField> fieldMap = map.get(holder.className());
                if (fieldMap == null) {
                    fieldMap = new HashMap<String, InterceptedField>();
                    map.put(holder.className(), fieldMap);
                }

                // add all the subsequent field entries to the map
                for (++i; i < specification.length; i++) {
                    Object field = specification[i];
                    InterceptedField interceptedField;
                    if (field instanceof ClassRef) {
                        i--;
                        break;
                    } else if (field instanceof String) {
                        interceptedField = new ZeroField((String) field);
                    } else {
                        interceptedField = (InterceptedField) field;
                    }
                    if (interceptedField.classRef == null) {
                        interceptedField.classRef = holder;
                    }
                    interceptedField.verify(holder);
                    fieldMap.put(interceptedField.getName(), interceptedField);
                }
            } else {
                throw ProgramError.unexpected("format of intercepted field array is wrong");
            }
        }
    }

    /**
     * Register a field of a class to be reset while bootstrapping.
     *
     * @param fieldName the name of the field as a string
     */
    public static void resetField(String className, String fieldName) {
        Map<String, InterceptedField> fieldMap = interceptedFieldMap.get(className);
        if (fieldMap == null) {
            fieldMap = new HashMap<String, InterceptedField>();
            interceptedFieldMap.put(className, fieldMap);
        }
        ZeroField zeroField = new ZeroField(fieldName);
        zeroField.makeNonFinal();
        zeroField.verify(new LazyClassRef(className));
        fieldMap.put(fieldName, zeroField);
    }

    private static Properties buildInitialSystemProperties() {
        final Properties properties = new Properties();
        for (String p : REMEMBERED_PROPERTY_NAMES) {
            final String value = System.getProperty(p);
            if (value != null) {
                properties.setProperty(p, value);
            }
        }
        return properties;
    }

    /**
     * A mechanism for special-casing the value of a field from a JDK class that is written into
     * the boot image. All other fields of JDK classes simply have their current value (read via
     * reflection) written to the boot image.
     */
    public abstract static class InterceptedField {
        private final String name;
        protected ClassRef classRef;
        protected boolean verifyFieldExists = true;

        public FieldActor fieldActor;
        private boolean mutabilityOverride = false;

        InterceptedField(String name) {
            this.name = name;
        }

        public InterceptedField makeNonFinal() {
            mutabilityOverride = true;
            return this;
        }

        public InterceptedField makeOptional() {
            verifyFieldExists = false;
            return this;
        }

        public String getName() {
            return name;
        }

        /**
         * Gets the value of the field represented by this object that is to be written to the boot image.
         *
         * @param object an object of the class in which this field is defined
         * @param field the field for which a value is being requested.
         */
        public abstract Value getValue(Object object, FieldActor field);

        /**
         * Determines if this field is mutable.
         *
         * @return whether the value of this field can be modified in the VM
         */
        boolean isMutable() {
            return !fieldActor.isConstant() || mutabilityOverride;
        }

        protected void verify(ClassRef holder) {
            if (verifyFieldExists) {
                ensureFieldExists(holder, name);
            }
        }

        protected static void ensureFieldExists(ClassRef holder, String fieldName) {
            try {
                holder.javaClass().getDeclaredField(fieldName);
            } catch (NoSuchFieldException ex) {
                // Some fields are hidden from reflection. In order to avoid false positives, these fields
                // have to set the flag verifyFieldExists to false.
                throw ProgramError.unexpected("Class " + holder.className() + " does not declare field " + fieldName);
            }
        }
    }

    /**
     * An intercepted field whose boot image value is fixed to a value given in the
     * {@linkplain ValueField#ValueField(String, Value) constructor}.
     */
    private static class ValueField extends InterceptedField {
        private final Value value;
        ValueField(String name, Value value) {
            super(name);
            this.value = value;
        }
        @Override
        public Value getValue(Object object, FieldActor field) {
            return value;
        }
    }

    /**
     * An intercepted field whose boot image value is the {@linkplain Kind#zeroValue() zero value}
     * corresponding to the field's kind.
     */
    public static class ZeroField extends InterceptedField {
        ZeroField(String name) {
            super(name);
        }
        @Override
        public Value getValue(Object object, FieldActor field) {
            return field.kind.zeroValue();
        }
    }

    private static class AtomicFieldUpdaterOffsetRecomputation extends InterceptedField {
        AtomicFieldUpdaterOffsetRecomputation(String name) {
            super(name);
        }
        @Override
        public Value getValue(Object object, FieldActor fieldActor) {
            final Field field = fieldActor.toJava();
            assert !Modifier.isStatic(field.getModifiers());
            try {
                /*
                 * Explanation: java.util.concurrent is full of objects and classes that cache the offset
                 * of particular fields in the JDK. Here, Atomic<X>FieldUpdater implementation objects cache
                 * the offset of some specified field. Which field? Oh...only Doug Lea knows that. We have to
                 * search the declaring class for a field that has the same "unsafe" offset as the cached
                 * offset in this atomic updater object.
                 */
                final Field tclassField = field.getDeclaringClass().getDeclaredField("tclass");
                tclassField.setAccessible(true);
                final Class tclass = (Class) tclassField.get(object);

                final Field offsetField = fieldActor.toJava();
                offsetField.setAccessible(true);
                final long offset = offsetField.getLong(object);
                // search the declared fields for a field with a matching offset
                for (Field f : tclass.getDeclaredFields()) {
                    if ((f.getModifiers() & Modifier.STATIC) == 0) {
                        final long fieldOffset = WithoutAccessCheck.unsafe.objectFieldOffset(f);
                        if (fieldOffset == offset) {
                            return LongValue.from(FieldActor.fromJava(f).offset());
                        }
                    }
                }
                throw ProgramError.unexpected("unknown atomic field updater of class: " + tclass + ", offset = " + offset);
            } catch (Exception e) {
                throw ProgramError.unexpected(e);
            }
        }
    }

    private static class ExpiringCacheField extends InterceptedField {
        private final Map<Object, Object> newValues = new IdentityHashMap<Object, Object>();
        ExpiringCacheField(String name) {
            super(name);
        }
        @Override
        public Value getValue(Object object, FieldActor fieldActor) {
            Object result = newValues.get(object);
            if (result == null) {
                result = WithoutAccessCheck.newInstance(JDK.java_io_ExpiringCache.javaClass());
                newValues.put(object, result);
            }
            return ReferenceValue.from(result);
        }
    }

    /**
     * An intercepted field whose boot image value is the {@linkplain FieldActor#offset() offset} of another field.
     * This facility is required to fix up field values obtained via {@link Unsafe#fieldOffset(Field)}.
     */
    public static class FieldOffsetRecomputation extends InterceptedField {
        private final String fieldName;
        private FieldOffsetRecomputation(String offsetFieldName, String fieldName) {
            this(offsetFieldName, null, fieldName);
        }
        public FieldOffsetRecomputation(String offsetFieldName, ClassRef classRef, String fieldName) {
            super(offsetFieldName);
            this.fieldName = fieldName;
            this.classRef = classRef;
        }
        @Override
        public Value getValue(Object object, FieldActor fieldActor) {
            try {
                final Field field = classRef.javaClass().getDeclaredField(fieldName);
                return LongValue.from(FieldActor.fromJava(field).offset());
            } catch (SecurityException e) {
                throw ProgramError.unexpected(e);
            } catch (NoSuchFieldException e) {
                throw ProgramError.unexpected(e);
            }
        }

        @Override
        protected void verify(ClassRef holder) {
            super.verify(holder);
            if (verifyFieldExists) {
                ensureFieldExists(classRef, fieldName);
            }
        }
    }

    /**
     * An intercepted field whose boot image value is the offset of
     * the first element in an array from the array's origin.
     * This facility is required to fix up field values obtained via {@link Unsafe#arrayBaseOffset(Class)}.
     */
    private static class ArrayBaseOffsetRecomputation extends InterceptedField {
        private final Class arrayClass;
        private final int offset;
        ArrayBaseOffsetRecomputation(String arrayBaseOffsetFieldName, Class arrayClass) {
            super(arrayBaseOffsetFieldName);
            this.arrayClass = arrayClass;
            this.offset = 0;
        }
        ArrayBaseOffsetRecomputation(String arrayBaseOffsetFieldName, Class arrayClass, int offset) {
            super(arrayBaseOffsetFieldName);
            this.arrayClass = arrayClass;
            this.offset = offset;
        }
        @Override
        public Value getValue(Object object, FieldActor fieldActor) {
            ArrayLayout arrayLayout = (ArrayLayout) ClassActor.fromJava(arrayClass).dynamicHub().specificLayout;
            return fieldActor.kind.convert(IntValue.from(arrayLayout.getElementOffsetFromOrigin(0).toInt() + offset));
        }
    }

    /**
     * An intercepted field whose boot image value is the scale factor for addressing elements in an array.
     * This facility is required to fix up field values obtained via {@link Unsafe#arrayIndexScale(Class)}.
     */
    private static class ArrayIndexScaleRecomputation extends InterceptedField {
        private final Class arrayClass;
        ArrayIndexScaleRecomputation(String arrayIndexScaleFieldName, Class arrayClass) {
            super(arrayIndexScaleFieldName);
            this.arrayClass = arrayClass;
        }
        @Override
        public Value getValue(Object object, FieldActor fieldActor) {
            return IntValue.from(ClassActor.fromJava(arrayClass).componentClassActor().kind.width.numberOfBytes);
        }
    }

    /**
     * An intercepted field whose boot image value is the scale factor for addressing elements in an array,
     * converted to be useful in a shift operation using the log2.
     * This facility is required to fix up field values obtained via {@link Unsafe#arrayIndexScale(Class)}.
     */
    private static class ArrayIndexScaleShiftRecomputation extends ArrayIndexScaleRecomputation {
        ArrayIndexScaleShiftRecomputation(String arrayIndexScaleFieldName, Class arrayClass) {
            super(arrayIndexScaleFieldName, arrayClass);
        }
        @Override
        public Value getValue(Object object, FieldActor fieldActor) {
            int scale = super.getValue(object, fieldActor).asInt();

            // The following code is taken from the static initializer of ConcurrentHashMap in JDK 7
            if ((scale & (scale - 1)) != 0) {
                throw new Error("data type scale not a power of two");
            }
            return IntValue.from(31 - Integer.numberOfLeadingZeros(scale));
        }
    }
    /**
     * At some point the JDK changed java.lang.Shutdown:hooks from type ArrayList to a Runnable[].
     * Detect which is the case and reallocate as necessary.
     */
    private static class NewShutdownHookList extends InterceptedField {
        private Object result;
        NewShutdownHookList(String fieldName) {
            super(fieldName);
        }

        @Override
        public Value getValue(Object object, FieldActor fieldActor) {
            try {
                if (result == null) {
                    Field field = classRef.javaClass().getDeclaredField(getName());
                    if (field.getType() == Runnable[].class) {
                        // allocate a new array
                        // the size of the array is controlled by a private static final field
                        Field sizeField = classRef.javaClass().getDeclaredField("MAX_SYSTEM_HOOKS");
                        sizeField.setAccessible(true);
                        int size = sizeField.getInt(null);
                        result = new Runnable[size];
                    } else {
                        // allocate a new array list
                        result = new ArrayList<Runnable>();
                    }
                }
            } catch (SecurityException e) {
                throw ProgramError.unexpected(e);
            } catch (NoSuchFieldException e) {
                throw ProgramError.unexpected(e);
            } catch (IllegalAccessException e) {
                throw ProgramError.unexpected(e);
            }
            return ReferenceValue.from(result);
        }
    }
}

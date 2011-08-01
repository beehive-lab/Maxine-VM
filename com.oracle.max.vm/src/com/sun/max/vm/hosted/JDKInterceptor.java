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

import java.io.*;
import java.lang.ref.*;
import java.lang.reflect.*;
import java.util.*;

import sun.misc.*;

import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.jdk.JDK.ClassRef;
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
     * These are the properties that are to be remembered from the host VM on which the image was built.
     */
    public static final String[] REMEMBERED_PROPERTY_NAMES = {
        "awt.toolkit",
        "java.specification.version",
        "java.specification.name",
        "java.class.version",
        "java.vendor",
        "java.vendor.url",
        "java.vendor.url.bug",
        "java.vm.vendor",
        "java.vm.specification.name",
        "java.vm.specification.vendor",
        "java.vm.specification.version",
        "java.runtime.name",
        "java.runtime.version",
        "java.awt.graphicsenv",
        "java.awt.printerjob",
        "file.encoding.pkg",
        "file.separator",
        "path.separator",
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
            Constructor cons = c.getConstructor(Class.class, String.class);
            cons.setAccessible(true);
            lib = cons.newInstance(MaxineVM.class, "maxvm");
        } catch (Exception e) {
            throw FatalError.unexpected("Could not construct VM native library", e);
        }
        systemNativeLibraries.add(lib);
    }

    /**
     * This array contains all the intercepted fields that are either ignored (i.e. set to zero) or
     * specially handled when building the prototype.
     */
    // Checkstyle: stop
    private static final Object[] interceptedFieldArray = {
        JDK.java_lang_ApplicationShutdownHooks,
            new ValueField("hooks", ReferenceValue.from(new IdentityHashMap<Thread, Thread>())),
        JDK.java_lang_Class,
            "cachedConstructor",
            "newInstanceCallerCache",
            "name",
            "declaredFields",
            "publicFields",
            "declaredMethods",
            "publicMethods",
            "declaredConstructors",
            "publicConstructors",
            "declaredPublicFields",
            "declaredPublicMethods",
            "genericInfo",
            "enumConstants",
            "enumConstantDirectory",
            "annotations",
            "declaredAnnotations",
            "classRedefinedCount",
            "lastRedefinedCount",
        JDK.java_lang_ClassLoader,
            "bootstrapClassPath",
            "scl",
            "sclSet",
            "usr_paths",
            "sys_paths",
            new ValueField("loadedLibraryNames", ReferenceValue.from(new Vector())),
            new ValueField("systemNativeLibraries", ReferenceValue.from(systemNativeLibraries)),
        JDK.java_util_EnumMap,
            "entrySet",
        JDK.java_lang_reflect_Field,
            "genericInfo",
            "declaredAnnotations",
            "fieldAccessor",
            "overrideFieldAccessor",
        JDK.java_lang_reflect_Constructor,
            "genericInfo",
            "declaredAnnotations",
            "constructorAccessor",
        JDK.java_lang_reflect_Method,
            "genericInfo",
            "declaredAnnotations",
            "methodAccessor",
        JDK.java_lang_Package,
            "loader",
            "packageInfo",
        JDK.java_lang_Shutdown,
            new NewShutdownHookList(JDK.java_lang_Shutdown, "hooks"),
        JDK.java_lang_System,
            "security",
            new ValueField("props", ReferenceValue.from(initialSystemProperties)),
        JDK.java_lang_ref_Reference,
            "discovered",
            "pending",
        JDK.java_lang_ref_Finalizer,
            "unfinalized",
            new ValueField("queue", ReferenceValue.from(new ReferenceQueue())),
        JDK.java_lang_Throwable,
            "backtrace",
        JDK.java_lang_Thread,
            "parkBlocker",
            "blocker",
            "threadLocals",
            "inheritableThreadLocals",
        JDK.java_lang_ProcessEnvironment,
            new ZeroField("theEnvironment", true),
            new ZeroField("theUnmodifiableEnvironment", true),
        JDK.java_lang_Terminator,
            "handler",
        JDK.sun_misc_VM,
            "booted",
            "finalRefCount",
            "peakFinalRefCount",
        JDK.sun_reflect_ConstantPool,
            "constantPoolOop",
        JDK.sun_reflect_Reflection,
            new ValueField("fieldFilterMap", ReferenceValue.from(new HashMap<Class, String[]>())),
            new ValueField("methodFilterMap", ReferenceValue.from(new HashMap<Class, String[]>())),
        JDK.sun_util_calendar_ZoneInfo,
            new ZeroField("aliasTable", true),
        JDK.java_util_Random,
            new FieldOffsetRecomputation("seedOffset", JDK.java_util_Random, "seed"),
        JDK.java_util_concurrent_ConcurrentSkipListSet,
            new FieldOffsetRecomputation("mapOffset", JDK.java_util_concurrent_ConcurrentSkipListSet, "m"),
        JDK.java_util_concurrent_CopyOnWriteArrayList,
            new FieldOffsetRecomputation("lockOffset", JDK.java_util_concurrent_CopyOnWriteArrayList, "lock"),
        JDK.java_nio_DirectByteBuffer,
            new ArrayBaseOffsetRecomputation("arrayBaseOffset", JDK.java_nio_DirectByteBuffer, byte[].class),
        JDK.java_nio_DirectCharBufferS,
            new ArrayBaseOffsetRecomputation("arrayBaseOffset", JDK.java_nio_DirectCharBufferS, char[].class),
        JDK.java_nio_DirectCharBufferU,
            new ArrayBaseOffsetRecomputation("arrayBaseOffset", JDK.java_nio_DirectCharBufferU, char[].class),
        JDK.java_nio_DirectDoubleBufferS,
            new ArrayBaseOffsetRecomputation("arrayBaseOffset", JDK.java_nio_DirectDoubleBufferS, double[].class),
        JDK.java_nio_DirectDoubleBufferU,
            new ArrayBaseOffsetRecomputation("arrayBaseOffset", JDK.java_nio_DirectDoubleBufferU, double[].class),
        JDK.java_nio_DirectFloatBufferS,
            new ArrayBaseOffsetRecomputation("arrayBaseOffset", JDK.java_nio_DirectFloatBufferS, float[].class),
        JDK.java_nio_DirectFloatBufferU,
            new ArrayBaseOffsetRecomputation("arrayBaseOffset", JDK.java_nio_DirectFloatBufferU, float[].class),
        JDK.java_nio_DirectIntBufferS,
            new ArrayBaseOffsetRecomputation("arrayBaseOffset", JDK.java_nio_DirectIntBufferS, int[].class),
        JDK.java_nio_DirectIntBufferU,
            new ArrayBaseOffsetRecomputation("arrayBaseOffset", JDK.java_nio_DirectIntBufferU, int[].class),
        JDK.java_nio_DirectLongBufferS,
            new ArrayBaseOffsetRecomputation("arrayBaseOffset", JDK.java_nio_DirectLongBufferS, long[].class),
        JDK.java_nio_DirectLongBufferU,
            new ArrayBaseOffsetRecomputation("arrayBaseOffset", JDK.java_nio_DirectLongBufferU, long[].class),
        JDK.java_nio_DirectShortBufferS,
            new ArrayBaseOffsetRecomputation("arrayBaseOffset", JDK.java_nio_DirectShortBufferS, short[].class),
        JDK.java_nio_DirectShortBufferU,
            new ArrayBaseOffsetRecomputation("arrayBaseOffset", JDK.java_nio_DirectShortBufferU, short[].class),
        JDK.java_nio_charset_CharsetEncoder,
            "cachedDecoder",
        JDK.java_util_concurrent_atomic_AtomicBoolean,
            new FieldOffsetRecomputation("valueOffset", JDK.java_util_concurrent_atomic_AtomicBoolean, "value"),
        JDK.java_util_concurrent_atomic_AtomicInteger,
            new FieldOffsetRecomputation("valueOffset", JDK.java_util_concurrent_atomic_AtomicInteger, "value"),
        JDK.java_util_concurrent_atomic_AtomicLong,
            new FieldOffsetRecomputation("valueOffset", JDK.java_util_concurrent_atomic_AtomicLong, "value"),
        JDK.java_util_concurrent_atomic_AtomicReference,
            new FieldOffsetRecomputation("valueOffset", JDK.java_util_concurrent_atomic_AtomicReference, "value"),
        JDK.java_util_concurrent_atomic_AtomicIntegerArray,
            new ArrayBaseOffsetRecomputation("base", JDK.java_util_concurrent_atomic_AtomicIntegerArray, int[].class),
            new ArrayIndexScaleRecomputation("scale", JDK.java_util_concurrent_atomic_AtomicIntegerArray, int[].class),
        JDK.java_util_concurrent_atomic_AtomicLongArray,
            new ArrayBaseOffsetRecomputation("base", JDK.java_util_concurrent_atomic_AtomicLongArray, long[].class),
            new ArrayIndexScaleRecomputation("scale", JDK.java_util_concurrent_atomic_AtomicLongArray, long[].class),
        JDK.java_util_concurrent_atomic_AtomicReferenceArray,
            new ArrayBaseOffsetRecomputation("base", JDK.java_util_concurrent_atomic_AtomicReferenceArray, Object[].class),
            new ArrayIndexScaleRecomputation("scale", JDK.java_util_concurrent_atomic_AtomicReferenceArray, Object[].class),
        JDK.java_util_concurrent_atomic_AtomicReferenceFieldUpdater$AtomicReferenceFieldUpdaterImpl,
            new AtomicFieldUpdaterOffsetRecomputation("offset"),
        JDK.java_util_concurrent_atomic_AtomicIntegerFieldUpdater$AtomicIntegerFieldUpdaterImpl,
            new AtomicFieldUpdaterOffsetRecomputation("offset"),
        JDK.java_util_concurrent_atomic_AtomicLongFieldUpdater$CASUpdater,
            new AtomicFieldUpdaterOffsetRecomputation("offset"),
        JDK.java_util_concurrent_atomic_AtomicLongFieldUpdater$LockedUpdater,
            new AtomicFieldUpdaterOffsetRecomputation("offset"),
        JDK.java_io_UnixFileSystem,
            new ExpiringCacheField("cache"),
            new ExpiringCacheField("javaHomePrefixCache"),
        JDK.java_util_concurrent_locks_AbstractQueuedSynchronizer,
            new FieldOffsetRecomputation("stateOffset", JDK.java_util_concurrent_locks_AbstractQueuedSynchronizer, "state"),
            new FieldOffsetRecomputation("headOffset", JDK.java_util_concurrent_locks_AbstractQueuedSynchronizer, "head"),
            new FieldOffsetRecomputation("tailOffset", JDK.java_util_concurrent_locks_AbstractQueuedSynchronizer, "tail"),
            new FieldOffsetRecomputation("waitStatusOffset", JDK.java_util_concurrent_locks_AbstractQueuedSynchronizer$Node, "waitStatus"),
            new FieldOffsetRecomputation("nextOffset", JDK.java_util_concurrent_locks_AbstractQueuedSynchronizer$Node, "next"),
        JDK.java_util_concurrent_locks_AbstractQueuedLongSynchronizer,
            new FieldOffsetRecomputation("stateOffset", JDK.java_util_concurrent_locks_AbstractQueuedLongSynchronizer, "state"),
            new FieldOffsetRecomputation("headOffset", JDK.java_util_concurrent_locks_AbstractQueuedLongSynchronizer, "head"),
            new FieldOffsetRecomputation("tailOffset", JDK.java_util_concurrent_locks_AbstractQueuedLongSynchronizer, "tail"),
            new FieldOffsetRecomputation("waitStatusOffset", JDK.java_util_concurrent_locks_AbstractQueuedLongSynchronizer$Node, "waitStatus"),
            new FieldOffsetRecomputation("nextOffset", JDK.java_util_concurrent_locks_AbstractQueuedLongSynchronizer$Node, "next"),
        JDK.java_util_concurrent_locks_LockSupport,
            new FieldOffsetRecomputation("parkBlockerOffset", JDK.java_lang_Thread, "parkBlocker"),

    // Definitions that were added in JDK 7
        JDK.sun_misc_Unsafe,
            new ArrayBaseOffsetRecomputation("ARRAY_BOOLEAN_BASE_OFFSET", JDK.sun_misc_Unsafe, boolean[].class),
            new ArrayBaseOffsetRecomputation("ARRAY_BYTE_BASE_OFFSET", JDK.sun_misc_Unsafe, byte[].class),
            new ArrayBaseOffsetRecomputation("ARRAY_SHORT_BASE_OFFSET", JDK.sun_misc_Unsafe, short[].class),
            new ArrayBaseOffsetRecomputation("ARRAY_CHAR_BASE_OFFSET", JDK.sun_misc_Unsafe, char[].class),
            new ArrayBaseOffsetRecomputation("ARRAY_INT_BASE_OFFSET", JDK.sun_misc_Unsafe, int[].class),
            new ArrayBaseOffsetRecomputation("ARRAY_LONG_BASE_OFFSET", JDK.sun_misc_Unsafe, long[].class),
            new ArrayBaseOffsetRecomputation("ARRAY_FLOAT_BASE_OFFSET", JDK.sun_misc_Unsafe, float[].class),
            new ArrayBaseOffsetRecomputation("ARRAY_DOUBLE_BASE_OFFSET", JDK.sun_misc_Unsafe, double[].class),
            new ArrayBaseOffsetRecomputation("ARRAY_OBJECT_BASE_OFFSET", JDK.sun_misc_Unsafe, Object[].class),
            new ArrayIndexScaleRecomputation("ARRAY_BOOLEAN_INDEX_SCALE", JDK.sun_misc_Unsafe, boolean[].class),
            new ArrayIndexScaleRecomputation("ARRAY_BYTE_INDEX_SCALE", JDK.sun_misc_Unsafe, byte[].class),
            new ArrayIndexScaleRecomputation("ARRAY_SHORT_INDEX_SCALE", JDK.sun_misc_Unsafe, short[].class),
            new ArrayIndexScaleRecomputation("ARRAY_CHAR_INDEX_SCALE", JDK.sun_misc_Unsafe, char[].class),
            new ArrayIndexScaleRecomputation("ARRAY_INT_INDEX_SCALE", JDK.sun_misc_Unsafe, int[].class),
            new ArrayIndexScaleRecomputation("ARRAY_LONG_INDEX_SCALE", JDK.sun_misc_Unsafe, long[].class),
            new ArrayIndexScaleRecomputation("ARRAY_FLOAT_INDEX_SCALE", JDK.sun_misc_Unsafe, float[].class),
            new ArrayIndexScaleRecomputation("ARRAY_DOUBLE_INDEX_SCALE", JDK.sun_misc_Unsafe, double[].class),
            new ArrayIndexScaleRecomputation("ARRAY_OBJECT_INDEX_SCALE", JDK.sun_misc_Unsafe, Object[].class),
        JDK.java_math_BigInteger,
            new FieldOffsetRecomputation("signumOffset", JDK.java_math_BigInteger, "signum"),
            new FieldOffsetRecomputation("pairOffset", JDK.java_math_BigInteger, "pair"),
        JDK.java_util_concurrent_ConcurrentHashMap,
            new ArrayBaseOffsetRecomputation("TBASE", JDK.java_util_concurrent_ConcurrentHashMap, Object[].class),
            new ArrayIndexScaleShiftRecomputation("TSHIFT", JDK.java_util_concurrent_ConcurrentHashMap, Object[].class),
            new ArrayBaseOffsetRecomputation("SBASE", JDK.java_util_concurrent_ConcurrentHashMap, Object[].class),
            new ArrayIndexScaleShiftRecomputation("SSHIFT", JDK.java_util_concurrent_ConcurrentHashMap, Object[].class),
        JDK.java_util_concurrent_ConcurrentHashMap$HashEntry,
            new FieldOffsetRecomputation("nextOffset", JDK.java_util_concurrent_ConcurrentHashMap$HashEntry, "next"),
        JDK.java_util_concurrent_ForkJoinPool,
            new ArrayBaseOffsetRecomputation("ABASE", JDK.java_util_concurrent_ForkJoinPool, Object[].class),
            new ArrayIndexScaleShiftRecomputation("ASHIFT", JDK.java_util_concurrent_ForkJoinPool, Object[].class),
            new FieldOffsetRecomputation("ctlOffset", JDK.java_util_concurrent_ForkJoinPool, "ctl"),
            new FieldOffsetRecomputation("stealCountOffset", JDK.java_util_concurrent_ForkJoinPool, "stealCount"),
            new FieldOffsetRecomputation("blockedCountOffset", JDK.java_util_concurrent_ForkJoinPool, "blockedCount"),
            new FieldOffsetRecomputation("quiescerCountOffset", JDK.java_util_concurrent_ForkJoinPool, "quiescerCount"),
            new FieldOffsetRecomputation("scanGuardOffset", JDK.java_util_concurrent_ForkJoinPool, "scanGuard"),
            new FieldOffsetRecomputation("nextWorkerNumberOffset", JDK.java_util_concurrent_ForkJoinPool, "nextWorkerNumber"),
        JDK.java_util_concurrent_ForkJoinWorkerThread,
            new ArrayBaseOffsetRecomputation("ABASE", JDK.java_util_concurrent_ForkJoinWorkerThread, Object[].class),
            new ArrayIndexScaleShiftRecomputation("ASHIFT", JDK.java_util_concurrent_ForkJoinWorkerThread, Object[].class),
        JDK.java_util_concurrent_ForkJoinTask,
            new FieldOffsetRecomputation("statusOffset", JDK.java_util_concurrent_ForkJoinTask, "status"),
        JDK.java_util_concurrent_SynchronousQueue$TransferStack,
            new FieldOffsetRecomputation("headOffset", JDK.java_util_concurrent_SynchronousQueue$TransferStack, "head"),
        JDK.java_util_concurrent_SynchronousQueue$TransferStack$SNode,
            new FieldOffsetRecomputation("matchOffset", JDK.java_util_concurrent_SynchronousQueue$TransferStack$SNode, "match"),
            new FieldOffsetRecomputation("nextOffset", JDK.java_util_concurrent_SynchronousQueue$TransferStack$SNode, "next"),
        JDK.java_util_concurrent_SynchronousQueue$TransferQueue,
            new FieldOffsetRecomputation("headOffset", JDK.java_util_concurrent_SynchronousQueue$TransferQueue, "head"),
            new FieldOffsetRecomputation("tailOffset", JDK.java_util_concurrent_SynchronousQueue$TransferQueue, "tail"),
            new FieldOffsetRecomputation("cleanMeOffset", JDK.java_util_concurrent_SynchronousQueue$TransferQueue, "cleanMe"),
        JDK.java_util_concurrent_SynchronousQueue$TransferQueue$QNode,
            new FieldOffsetRecomputation("itemOffset", JDK.java_util_concurrent_SynchronousQueue$TransferQueue$QNode, "item"),
            new FieldOffsetRecomputation("nextOffset", JDK.java_util_concurrent_SynchronousQueue$TransferQueue$QNode, "next"),
        JDK.java_util_concurrent_atomic_AtomicStampedReference,
            new FieldOffsetRecomputation("pairOffset", JDK.java_util_concurrent_atomic_AtomicStampedReference, "pair"),
        JDK.java_util_concurrent_atomic_AtomicMarkableReference,
            new FieldOffsetRecomputation("pairOffset", JDK.java_util_concurrent_atomic_AtomicMarkableReference, "pair"),
    };
    // Checkstyle: resume

    private static final Map<String, Map<String, InterceptedField>> interceptedFieldMap = buildInterceptedFieldMap(interceptedFieldArray);

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

    private static InterceptedField getInterceptedField(String className, String fieldName) {
        final Map<String, InterceptedField> map = interceptedFieldMap.get(className);
        if (map != null) {
            return map.get(fieldName);
        }

        if (UnsafeUsageChecker.isClassUsingUnsafe(className)) {
            ProgramError.unexpected("class is using Unsafe operations to get field or array offsets, but no field interceptor present: " + className);
        }

        return null;
    }

    public static boolean hasMutabilityOverride(FieldActor fieldActor) {
        InterceptedField f = getInterceptedField(fieldActor);
        return f != null && f.mutabilityOverride;
    }


    /**
     * Builds a map that stores the intercepted fields for each class.
     * @param specification an array of objects consisting of a ClassRef followed by a non-empty sequence of either
     * String objects or InterceptedField objects.
     * @return a map from java classes to their intercepted fields
     */
    private static Map<String, Map<String, InterceptedField>> buildInterceptedFieldMap(Object[] specification) {
        final Map<String, Map<String, InterceptedField>> map = new HashMap<String, Map<String, InterceptedField>>();
        int i = 0;
        for (; i < specification.length; i++) {
            final Object object = specification[i];
            if (object instanceof ClassRef) {
                // we found a classref, add it and its intercepted fields to the map
                final String className = ((ClassRef) object).className();
                final Map<String, InterceptedField> fieldMap = new HashMap<String, InterceptedField>();
                map.put(className, fieldMap);

                // add all the subsequent field entries to the map
                for (++i; i < specification.length; i++) {
                    final Object field = specification[i];
                    if (field instanceof InterceptedField) {
                        final InterceptedField interceptedField = (InterceptedField) field;
                        fieldMap.put(interceptedField.getName(), interceptedField);
                    } else if (field instanceof String) {
                        final String fieldSpec = (String) field;
                        final ZeroField zeroField = new ZeroField(fieldSpec);
                        fieldMap.put(zeroField.getName(), zeroField);
                    } else {
                        i--;
                        break;
                    }
                }
            } else {
                ProgramError.unexpected("format of intercepted field array is wrong");
            }
        }
        return map;
    }

    /**
     * Register a field of a class to be reset while bootstrapping.
     * @param javaClass the java class that declared the field
     * @param fieldName the name of the field as a string
     */
    public static void resetField(String className, String fieldName) {
        Map<String, InterceptedField> fieldMap = interceptedFieldMap.get(className);
        if (fieldMap == null) {
            fieldMap = new HashMap<String, InterceptedField>();
            interceptedFieldMap.put(className, fieldMap);
        }
        Trace.line(2, "registering "  +  className + "." + fieldName + " for reset to default value");
        fieldMap.put(fieldName, new ZeroField(fieldName, true));
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
        public FieldActor fieldActor;
        private final boolean mutabilityOverride;

        InterceptedField(String name, boolean makeNonFinal) {
            this.mutabilityOverride = makeNonFinal;
            this.name = name;
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
    }

    /**
     * An intercepted field whose boot image value is a fixed to a value given in the
     * {@linkplain ValueField#ValueField(String, Value) constructor}.
     */
    private static class ValueField extends InterceptedField {
        private final Value value;
        ValueField(String name, Value value) {
            super(name, false);
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
            super(name, false);
        }
        ZeroField(String name, boolean mutabilityOverride) {
            super(name, mutabilityOverride);
        }
        @Override
        public Value getValue(Object object, FieldActor field) {
            return field.kind.zeroValue();
        }
    }

    private static class AtomicFieldUpdaterOffsetRecomputation extends InterceptedField {
        AtomicFieldUpdaterOffsetRecomputation(String name) {
            super(name, false);
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
            super(name, false);
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
    private static class FieldOffsetRecomputation extends InterceptedField {
        private final ClassRef classRef;
        private final String fieldName;
        FieldOffsetRecomputation(String offsetFieldName, ClassRef classRef, String fieldName) {
            super(offsetFieldName, false);
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
    }

    /**
     * An intercepted field whose boot image value is the offset of
     * the first element in an array from the array's origin.
     * This facility is required to fix up field values obtained via {@link Unsafe#arrayBaseOffset(Class)}.
     */
    private static class ArrayBaseOffsetRecomputation extends InterceptedField {
        private final ClassRef classRef;
        private final Class arrayClass;
        ArrayBaseOffsetRecomputation(String arrayBaseOffsetFieldName, ClassRef classRef, Class arrayClass) {
            super(arrayBaseOffsetFieldName, false);
            this.arrayClass = arrayClass;
            this.classRef = classRef;
        }
        @Override
        public Value getValue(Object object, FieldActor fieldActor) {
            ArrayLayout arrayLayout = (ArrayLayout) ClassActor.fromJava(arrayClass).dynamicHub().specificLayout;
            return fieldActor.kind.convert(IntValue.from(arrayLayout.getElementOffsetFromOrigin(0).toInt()));
        }
    }

    /**
     * An intercepted field whose boot image value is the scale factor for addressing elements in an array.
     * This facility is required to fix up field values obtained via {@link Unsafe#arrayIndexScale(Class)}.
     */
    private static class ArrayIndexScaleRecomputation extends InterceptedField {
        private final ClassRef classRef;
        private final Class arrayClass;
        ArrayIndexScaleRecomputation(String arrayIndexScaleFieldName, ClassRef classRef, Class arrayClass) {
            super(arrayIndexScaleFieldName, false);
            this.arrayClass = arrayClass;
            this.classRef = classRef;
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
        ArrayIndexScaleShiftRecomputation(String arrayIndexScaleFieldName, ClassRef classRef, Class arrayClass) {
            super(arrayIndexScaleFieldName, classRef, arrayClass);
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
        private final ClassRef classRef;
        private Object result;
        NewShutdownHookList(ClassRef classRef, String fieldName) {
            super(fieldName, true);
            this.classRef = classRef;
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

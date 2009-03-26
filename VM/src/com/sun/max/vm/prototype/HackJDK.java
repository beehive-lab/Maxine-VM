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
package com.sun.max.vm.prototype;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import sun.misc.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.jdk.JDK.*;
import com.sun.max.vm.value.*;

/**
 * This class encapsulates a number of hacks to work around inexplicable problems
 * in the JDK and alter some quantities (e.g. the library path) of the underlying host
 * virtual machine.
 *
 * @author Ben L. Titzer
 * @author Bernd Mathiske
 */
@PROTOTYPE_ONLY
public final class HackJDK {

    private HackJDK() {
    }

    /**
     * These are the properties that are to be remembered from the host VM on which the image was built.
     */
    public static final String[] REMEMBERED_PROPERTY_NAMES = {
        "java.specification.version",
        "java.specification.name",
        "java.class.version",
        "java.vendor",
        "java.vendor.url",
        "java.vendor.url.bug",
        "file.encoding.pkg",
        "file.separator",
        "path.separator",
    };

    private static final Unsafe _unsafe = (Unsafe) WithoutAccessCheck.getStaticField(Unsafe.class, "theUnsafe");
    public static final Properties _initialSystemProperties = buildInitialSystemProperties();

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

    /**
     * This array contains all the special fields that are either ignored (i.e. set to zero) or
     * specially handled when building the prototype.
     */
    // Checkstyle: stop
    private static final Object[] _specialFieldArray = {
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
            new ValueField("systemNativeLibraries", ReferenceValue.from(new Vector())),
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
            new ValueField("hooks", ReferenceValue.from(new ArrayList<Runnable>())),
        JDK.java_lang_System,
            "security",
            new ValueField("props", ReferenceValue.from(_initialSystemProperties)),
        JDK.java_lang_ref_Reference,
            "discovered",
            "pending",
        JDK.java_lang_ref_Finalizer,
            "unfinalized",
            "queue",
        JDK.java_lang_Throwable,
            "backtrace",
        JDK.java_lang_Thread,
            "parkBlocker",
            "blocker",
            "threadLocals",
            "inheritableThreadLocals",
        JDK.sun_misc_VM,
            "booted",
            "finalRefCount",
            "peakFinalRefCount",
        JDK.sun_reflect_ConstantPool,
            "constantPoolOop",
        JDK.sun_reflect_Reflection,
            new ValueField("fieldFilterMap", ReferenceValue.from(new HashMap<Class, String[]>())),
            new ValueField("methodFilterMap", ReferenceValue.from(new HashMap<Class, String[]>())),
        JDK.java_util_Random,
            new FieldOffsetRecomputation("seedOffset", JDK.java_util_Random, "seed"),
        JDK.java_util_concurrent_ConcurrentSkipListSet,
            new FieldOffsetRecomputation("mapOffset", JDK.java_util_concurrent_ConcurrentSkipListSet, "m"),
        JDK.java_util_concurrent_CopyOnWriteArrayList,
            new FieldOffsetRecomputation("lockOffset", JDK.java_util_concurrent_CopyOnWriteArrayList, "lock"),
        JDK.java_util_concurrent_atomic_AtomicInteger,
            new FieldOffsetRecomputation("valueOffset", JDK.java_util_concurrent_atomic_AtomicInteger, "value"),
        JDK.java_util_concurrent_atomic_AtomicReference,
            new FieldOffsetRecomputation("valueOffset", JDK.java_util_concurrent_atomic_AtomicReference, "value"),
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
            new FieldOffsetRecomputation("parkBlockerOffset", JDK.java_util_concurrent_atomic_AtomicReference, "parkBlocker"),
    };
    // Checkstyle: start

    private static final Map<String, Map<String, SpecialField>> _specialFieldMap = buildSpecialFieldMap(_specialFieldArray);

    /**
     * Checks whether the specified field should be omitted.
     *
     * @param field the field to check
     */
    public static boolean isOmittedField(Field field) {
        if (field.getName().startsWith("$SWITCH_TABLE")) {
            return true;
        }
        final SpecialField specialField = getSpecialField(field);
        if (specialField instanceof ZeroField) {
            return true;
        }
        return false;
    }

    public static SpecialField getSpecialField(FieldActor fieldActor) {
        final String className = fieldActor.holder().name().toString();
        final Map<String, SpecialField> map = _specialFieldMap.get(className);
        if (map != null) {
            return map.get(fieldActor.name().toString());
        }
        return null;
    }

    public static SpecialField getSpecialField(Field field) {
        final String className = field.getDeclaringClass().getName();
        final Map<String, SpecialField> map = _specialFieldMap.get(className);
        if (map != null) {
            return map.get(field.getName());
        }
        return null;
    }

    /**
     * Builds a map that stores the special fields for each class.
     * @param specification an array of objects consisting of a ClassRef followed by a non-empty sequence of either
     * String objects or SpecialField objects.
     * @return a map from java classes to their special fields
     */
    private static Map<String, Map<String, SpecialField>> buildSpecialFieldMap(Object[] specification) {
        final Map<String, Map<String, SpecialField>> map = new HashMap<String, Map<String, SpecialField>>();
        int i = 0;
        for (; i < specification.length; i++) {
            final Object object = specification[i];
            if (object instanceof ClassRef) {
                // we found a classref, add it and its special fields to the map
                final Class javaClass = ((ClassRef) object).javaClass();
                final Map<String, SpecialField> fieldMap = new HashMap<String, SpecialField>();
                map.put(javaClass.getName(), fieldMap);

                // add all the subsequent field entries to the map
                for (++i; i < specification.length; i++) {
                    final Object field = specification[i];
                    if (field instanceof SpecialField) {
                        final SpecialField specialField = (SpecialField) field;
                        fieldMap.put(specialField.getName(), specialField);
                    } else if (field instanceof String) {
                        final String fieldName = (String) field;
                        fieldMap.put(fieldName, new ZeroField(fieldName));
                    } else {
                        i--;
                        break;
                    }
                }
            } else {
                ProgramError.unexpected("format of special field array is wrong");
            }
        }
        return map;
    }

    /**
     * Register a field of a class to be reset at prototyping time.
     * @param javaClass the java class that declared the field
     * @param fieldName the name of the field as a string
     */
    public static void resetField(Class javaClass, String fieldName) {
        Map<String, SpecialField> fieldMap = _specialFieldMap.get(javaClass.getName());
        if (fieldMap == null ) {
            fieldMap = new HashMap<String, SpecialField>();
            _specialFieldMap.put(javaClass.getName(), fieldMap);
        }
        fieldMap.put(fieldName, new ZeroField(fieldName));
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

    public abstract static class SpecialField {
        private final String _name;
        public FieldActor _fieldActor;
        SpecialField(String name) {
            _name = name;
        }
        public String getName() {
            return _name;
        }
        public abstract Value getValue(Object object, FieldActor field);
    }

    private static class ValueField extends SpecialField {
        private final Value _value;
        ValueField(String name, Value val) {
            super(name);
            _value = val;
        }
        @Override
        public Value getValue(Object object, FieldActor field) {
            return _value;
        }
    }

    public static class ZeroField extends SpecialField {
        ZeroField(String name) {
            super(name);
        }
        @Override
        public Value getValue(Object object, FieldActor field) {
            return field.kind().zeroValue();
        }
    }

    private static class AtomicFieldUpdaterOffsetRecomputation extends SpecialField {
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
                        final long fieldOffset = _unsafe.objectFieldOffset(f);
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

    private static class ExpiringCacheField extends SpecialField {
        private final Map<Object, Object> _newValues = new IdentityHashMap<Object, Object>();
        ExpiringCacheField(String name) {
            super(name);
        }
        @Override
        public Value getValue(Object object, FieldActor fieldActor) {
            Object result = _newValues.get(object);
            if (result == null) {
                result = WithoutAccessCheck.newInstance(JDK.java_io_ExpiringCache.javaClass());
                _newValues.put(object, result);
            }
            return ReferenceValue.from(result);
        }
    }

    private static class FieldOffsetRecomputation extends SpecialField {
        private final ClassRef _classRef;
        private final String _fieldName;
        FieldOffsetRecomputation(String offsetFieldName, ClassRef classRef, String fieldName) {
            super(offsetFieldName);
            _fieldName = fieldName;
            _classRef = classRef;
        }
        @Override
        public Value getValue(Object object, FieldActor fieldActor) {
            return LongValue.from(fieldActor.offset());
        }
    }
}

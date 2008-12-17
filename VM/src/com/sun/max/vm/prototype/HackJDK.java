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
import java.lang.reflect.Proxy;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.regex.*;

import sun.misc.*;
import sun.util.calendar.*;

import com.sun.max.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;

/**
 * This class encapsulates a number of hacks to work around inexplicable problems
 * in the JDK and alter some quantities (e.g. the library path) of the underlying host
 * virtual machine.
 *
 * @author Bernd Mathiske
 */
public final class HackJDK {

    private HackJDK() {
    }

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
     * Check whether the specified field is filtered by the standard java reflection
     * mechanism. The JDK internally hides some fields in order to prevent certain
     * security vulnerabilities.
     * @see sun.reflect.Reflection.registerFieldsToFilter()
     *
     * @param fieldActor the field to check
     * @return {@code true} if the field is normally filtered by the underlying JDK reflection mechanism;
     * {@code false} otherwise
     */
    public static boolean isFilteredFieldActor(FieldActor fieldActor) {
        final Class<Map<Class, String[]>> type = null;
        final Map<Class, String[]> fieldFilterMap = StaticLoophole.cast(type, WithoutAccessCheck.getStaticField(sun.reflect.Reflection.class, "fieldFilterMap"));
        final String[] filteredFieldNames = fieldFilterMap.get(fieldActor.holder().toJava());
        if (filteredFieldNames != null) {
            for (String fieldName : filteredFieldNames) {
                if (fieldName.equals(fieldActor.name())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * This method must be called before creating the Java prototype, if a graph prototype is going to be created as
     * well. It performs various checks the value of various system properties. These are properties that must be set
     * via "-Dname=value" VM flags as they are read before the 'main' method of an application (such as
     * {@link BinaryImageGenerator} is run.
     *
     * Note: Historically, this method actually did something. It has been left here should this kind of functionality
     * be required again in the future.
     */
    public static void checkVMFlags() {
        final AppendableSequence<String> missingVMOptions = new ArrayListSequence<String>();
        ProgramError.check(missingVMOptions.isEmpty(), "The following VM command line option(s) are missing:\n    " + Sequence.Static.toString(missingVMOptions, null, "\n    "));
    }

    /**
     * Lookup a non-public inner class.
     *
     * @param outerClass the class containing the inner class
     * @param innerClassSimpleName the name of the inner class as a string
     */
    private static Class c(Class outerClass, String innerClassSimpleName) {
        final Class innerClass = Classes.getInnerClass(outerClass, innerClassSimpleName);
        if (innerClass == null) {
            ProgramWarning.message("inner class not found: " + innerClassSimpleName);
            return Void.class;
        }
        return innerClass;
    }

    private static Class c(String className) {
        return Classes.forName(className);
    }

    /**
     * Utility class for gathering fields.
     */
    static class FieldSet extends HashSet<Field> {
        public FieldSet(Object... classesOrFieldNames) {
            Class c = null;
            for (Object o : classesOrFieldNames) {
                if (o instanceof Class) {
                    c = (Class) o;
                } else {
                    assert c != null;
                    final String name = (String) o;
                    try {
                        add(c.getDeclaredField(name));
                    } catch (NoSuchFieldException noSuchFieldException) {
                        ProgramWarning.message("field not found: " + c.getName() + "." + name);
                    }
                }
            }
        }

        public void add(Class holder, String... fieldNames) {
            for (String name : fieldNames) {
                try {
                    add(holder.getDeclaredField(name));
                } catch (NoSuchFieldException noSuchFieldException) {
                    ProgramWarning.message("field not found: " + holder.getName() + "." + name);
                }
            }
        }
    }

    /**
     * A set of all the transient fields in the initialized JDK that should be preserved.
     */
    private static final FieldSet _preservedTransientFields = new FieldSet(
        AbstractList.class, "modCount",
        AbstractMap.class, "keySet", "values",
        ArrayList.class, "elementData",
        BitSet.class, "wordsInUse", "sizeIsSticky",
        BasicPermission.class, "wildcard", "path", "exitVM",
        c(Collections.class, "CheckedMap"), "entrySet",
        c(Collections.class, "SingletonMap"), "keySet", "entrySet", "values",
        c(Collections.class, "SynchronizedMap"), "keySet", "entrySet", "values",
        c(Collections.class, "UnmodifiableMap"), "keySet", "entrySet", "values",
        Constructor.class, "signature",
        EnumMap.class, "keyType", "keyUniverse", "size", "vals",
        Field.class, "signature",
        IdentityHashMap.class, "table", "modCount", "threshold", "entrySet",
        HashMap.class, "table", "size", "modCount", "entrySet",
        Hashtable.class, "table", "count", "modCount", "keySet", "entrySet", "values",
        HashSet.class, "map",
        LinkedList.class, "header", "size",
        Method.class, "signature",
        Pattern.class, "compiled", "normalizedPattern", "root", "matchRoot", "buffer", "groupNodes", "temp", "capturingGroupCount", "localCount", "cursor", "patternLength",
        TreeMap.class, "root", "size", "modCount", "entrySet", "navigableKeySet", "descendingMap",
        WeakHashMap.class, "entrySet",
        ConcurrentHashMap.class, "keySet", "entrySet", "values",
        c(ConcurrentHashMap.class, "Segment"), "count", "modCount", "threshold", "table",
        LinkedHashMap.class, "header",
        Locale.class, "hashCodeValue",
        ZoneInfo.class, "dirty", "lastRule",
        LinkedBlockingQueue.class, "head", "last",
        Permissions.class, "permsMap", "hasUnresolved",
        File.class, "prefixLength",
        URL.class, "query", "path", "userInfo", "hostAddress", "handler",
        c(AbstractQueuedSynchronizer.class, "ConditionObject"), "firstWaiter", "lastWaiter",
        AbstractOwnableSynchronizer.class, "exclusiveOwnerThread",
        AbstractQueuedSynchronizer.class, "head", "tail"
    );

    /**
     * A set of all the transient fields in the initialized JDK that should be omitted.
     */
    private static final Set<Field> _omittedTransientFields = new FieldSet(
        Class.class, "cachedConstructor", "newInstanceCallerCache", "name", "declaredFields", "publicFields",
                     "declaredMethods", "publicMethods", "declaredConstructors", "publicConstructors",
                     "declaredPublicFields", "declaredPublicMethods", "genericInfo",
                     "enumConstants", "enumConstantDirectory", "annotations", "declaredAnnotations",
                     "classRedefinedCount", "lastRedefinedCount",
        Field.class, "genericInfo", "declaredAnnotations",
        EnumMap.class, "entrySet",
        Constructor.class, "genericInfo", "declaredAnnotations",
        Method.class, "genericInfo", "declaredAnnotations",
        java.lang.Package.class, "loader", "packageInfo",
        java.lang.ref.Reference.class, "discovered"
    );

    /**
     * We keep track of all those transient fields in the JDK that we expect to encounter,
     * so we don't experience unawareness "surprises".
     * For each transient field in the boot image,
     * we have to carefully decide on a case-by-case basis
     * whether we want to capture its value or not.
     */
    private static final Set<Field> _knownTransientFields = Sets.union(_preservedTransientFields, _omittedTransientFields);

    /**
     * A set to prevent repeating identical warning messages.
     */
    private static final Set<Field> _unknownTransientFields = new HashSet<Field>();

    /**
     * Checks whether the specified field is an unknown transient field.
     *
     * @param field the field
     */
    public static void checkForUnknownTransientField(Field field) {
        if ((field.getModifiers() & Modifier.TRANSIENT) != 0 && !_knownTransientFields.contains(field)) {
            if (MaxPackage.isMaxClass(field.getDeclaringClass())) {
                // The 'transient' keyword is used in Maxine class to mean don't copy the field's value into the boot image
                return;
            }
            if (!_unknownTransientFields.contains(field)) {
                _unknownTransientFields.add(field);
                ProgramWarning.message("unknown transient field: " + field);
            }
        }
    }

    /**
     * Some classes should not have their fields explored because they reference
     * VM-specific implementation details in the host VM. For example,
     * some parts of the reflection machinery in the host VM will not work on the target VM.
     *
     * @see com.sun.max.vm.jdk.JDK_java_lang_reflect_Field
     */
    private static final FieldSet _omittedNonTransientFields = new FieldSet(
        Constructor.class, "constructorAccessor",
        Field.class, "fieldAccessor", "overrideFieldAccessor",
        Method.class, "methodAccessor",

        // We need to copy the main thread but not most of it's state
        Thread.class, "parkBlocker", "blocker",

        c("java.lang.ref.Finalizer"), "unfinalized", "queue", "lock",

        c("java.lang.ref.Reference"), "pending", "lock",

        Thread.class, "threadLocals", "inheritableThreadLocals",
        VM.class, "booted", "finalRefCount", "peakFinalRefCount",

        // If Proxy is allowed in the image, we don't want these fields.
        Proxy.class, "constructorParams", "loaderToCache", "proxyClasses"
    );

    /**
     * The union of both the omitted transient and non-transient fields.
     */
    private static final Set<Field> _omittedFields = Sets.union(_omittedTransientFields, _omittedNonTransientFields);

    /**
     * Checks whether the specified field should be omitted.
     *
     * @param field the field to check
     */
    public static boolean isOmittedField(Field field) {
        if (_omittedFields.contains(field)) {
            return true;
        }
        if ((field.getModifiers() & Modifier.TRANSIENT) != 0) {
            if (_knownTransientFields.contains(field)) {
                assert _preservedTransientFields.contains(field);
                return false;
            }
            return true;
        }
        return field.getName().startsWith("$SWITCH_TABLE");
    }

    /**
     * Checks whether the specified field actor should be omitted.
     *
     * @param fieldActor the field actor to check
     */
    public static boolean isOmittedFieldActor(FieldActor fieldActor) {
        if (fieldActor.name().toString().equals("backtrace")) {
            return fieldActor.holder().toJava() == Throwable.class;
        }
        return false;
    }

    /**
     * Registers an object in a static final field for replacement with new one for the boot image.
     * The original has a stale value from the host VM in its field "offset",
     * referring to a field offset in BufferedInputStream objects.
     * We replace the value with the prospective field offset according to the target layout.
     *
     * @param objectMap the object map used by HostObjectAccess to substitute objects that go into the boot image.
     */
    public static void fixBufferedInputStream(Map<Object, Object> objectMap) {
        try {
            final Field field = BufferedInputStream.class.getDeclaredField("bufUpdater");
            field.setAccessible(true);
            final Object brokenUpdater = field.get(BufferedInputStream.class);

            final Object fixedUpdater = Objects.clone(brokenUpdater);
            final FieldActor fieldActor = ClassActor.fromJava(BufferedInputStream.class).findLocalInstanceFieldActor("buf");
            WithoutAccessCheck.setInstanceField(fixedUpdater, "offset", (long) fieldActor.offset());

            objectMap.put(brokenUpdater, fixedUpdater);
        } catch (NoSuchFieldException noSuchFieldException) {
            // this version of JDK might be ok without this fix
        } catch (Throwable throwable) {
            ProgramError.unexpected();
        }
    }
}

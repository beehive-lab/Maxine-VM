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
/*VCSID=30d29f2f-695c-4e69-8132-1704d81282d9*/
package com.sun.max.vm.prototype;

import java.io.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import sun.misc.*;

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

    /**
     * Utility function to lookup a field given the name of the class and the name of the field.
     *
     * @param holderName the name of the class that contains the field
     * @param name the name of the field
     */
    private static Field f(String holderName, String name) {
        try {
            final Class holder = Class.forName(holderName);
            return holder.getDeclaredField(name);
        } catch (ClassNotFoundException classNotFoundException) {
            ProgramWarning.message("class not found: " + holderName);
            return null;
        } catch (NoSuchFieldException noSuchFieldException) {
            ProgramWarning.message("field not found: " + holderName + "." + name);
            return null;
        }
    }

    /**
     * Utility function to lookup a field given the class and the name of the field.
     *
     * @param holder the class that contains the field
     * @param name the name of the field as a string
     */
    private static Field f(Class holder, String name) {
        try {
            return holder.getDeclaredField(name);
        } catch (NoSuchFieldException noSuchFieldException) {
            ProgramWarning.message("field not found: " + holder.getName() + "." + name);
            return null;
        }
    }

    /**
     * A set of all the transient fields in the initialized JDK that should be preserved.
     */
    private static final Set<Field> _preservedTransientFields = Sets.from(
        f(AbstractList.class, "modCount"),
        f(AbstractMap.class, "keySet"), f(AbstractMap.class, "values"),
        f(ArrayList.class, "elementData"),
        f(BitSet.class, "wordsInUse"), f(BitSet.class, "sizeIsSticky"),
        f(BasicPermission.class, "wildcard"), f(BasicPermission.class, "path"), f(BasicPermission.class, "exitVM"),
        f(c(Collections.class, "CheckedMap"), "entrySet"),
        f(c(Collections.class, "SingletonMap"), "keySet"), f(c(Collections.class, "SingletonMap"), "entrySet"), f(c(Collections.class, "SingletonMap"), "values"),
        f(c(Collections.class, "SynchronizedMap"), "keySet"), f(c(Collections.class, "SynchronizedMap"), "entrySet"), f(c(Collections.class, "SynchronizedMap"), "values"),
        f(c(Collections.class, "UnmodifiableMap"), "keySet"), f(c(Collections.class, "UnmodifiableMap"), "entrySet"), f(c(Collections.class, "UnmodifiableMap"), "values"),
        f(Constructor.class, "signature"),
        f(EnumMap.class, "keyType"),
        f(EnumMap.class, "keyUniverse"),
        f(EnumMap.class, "size"),
        f(EnumMap.class, "vals"),
        f(Field.class, "signature"),
        f(IdentityHashMap.class, "table"), f(IdentityHashMap.class, "modCount"), f(IdentityHashMap.class, "threshold"), f(IdentityHashMap.class, "entrySet"),
        f(HashMap.class, "table"), f(HashMap.class, "size"), f(HashMap.class, "modCount"), f(HashMap.class, "entrySet"),
        f(Hashtable.class, "table"), f(Hashtable.class, "count"), f(Hashtable.class, "modCount"),
        f(Hashtable.class, "keySet"), f(Hashtable.class, "entrySet"), f(Hashtable.class, "values"),
        f(HashSet.class, "map"),
        f(LinkedList.class, "header"), f(LinkedList.class, "size"),
        f(Method.class, "signature"),
        f(Pattern.class, "compiled"), f(Pattern.class, "normalizedPattern"), f(Pattern.class, "root"), f(Pattern.class, "matchRoot"),
        f(Pattern.class, "buffer"), f(Pattern.class, "groupNodes"), f(Pattern.class, "temp"), f(Pattern.class, "capturingGroupCount"),
        f(Pattern.class, "localCount"), f(Pattern.class, "cursor"), f(Pattern.class, "patternLength"),
        f(TreeMap.class, "root"), f(TreeMap.class, "size"), f(TreeMap.class, "modCount"),
        f(TreeMap.class, "entrySet"),
        f(TreeMap.class, "navigableKeySet"), f(TreeMap.class, "descendingMap"),
        f(WeakHashMap.class, "entrySet"),
        f(ConcurrentHashMap.class, "keySet"), f(ConcurrentHashMap.class, "entrySet"), f(ConcurrentHashMap.class, "values"),
        f(c(ConcurrentHashMap.class, "Segment"), "count"), f(c(ConcurrentHashMap.class, "Segment"), "modCount"),
        f(c(ConcurrentHashMap.class, "Segment"), "threshold"), f(c(ConcurrentHashMap.class, "Segment"), "table"),
        f(LinkedHashMap.class, "header")
    );

    /**
     * A set of all the transient fields in the initialized JDK that should be omitted.
     */
    private static final Set<Field> _omittedTransientFields = Sets.from(
        f(Class.class, "cachedConstructor"), f(Class.class, "newInstanceCallerCache"), f(Class.class, "name"),
        f(Class.class, "declaredFields"), f(Class.class, "publicFields"),
        f(Class.class, "declaredMethods"), f(Class.class, "publicMethods"),
        f(Class.class, "declaredConstructors"), f(Class.class, "publicConstructors"),
        f(Class.class, "declaredPublicFields"), f(Class.class, "declaredPublicMethods"),
        f(Class.class, "genericInfo"),
        f(Class.class, "enumConstants"), f(Class.class, "enumConstantDirectory"),
        f(Class.class, "annotations"), f(Class.class, "declaredAnnotations"),
        f(Class.class, "classRedefinedCount"), f(Class.class, "lastRedefinedCount"),
        f(Field.class, "genericInfo"), f(Field.class, "declaredAnnotations"),
        f(EnumMap.class, "entrySet"),
        f(Constructor.class, "genericInfo"), f(Constructor.class, "declaredAnnotations"),
        f(Method.class, "genericInfo"), f(Method.class, "declaredAnnotations"),
        f(java.lang.Package.class, "loader"), f(java.lang.Package.class, "packageInfo"),
        f(java.lang.ref.Reference.class, "discovered")
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
    private static final Set<Field> _omittedNonTransientFields = Sets.from(
        f(Constructor.class, "constructorAccessor"),
        f(Field.class, "fieldAccessor"), f(Field.class, "overrideFieldAccessor"),
        f(Method.class, "methodAccessor"),

        // We need to copy the main thread but not most of it's state
        f(Thread.class, "parkBlocker"),
        f(Thread.class, "blocker"),

        f("java.lang.ref.Finalizer", "unfinalized"),
        f("java.lang.ref.Finalizer", "queue"),
        f("java.lang.ref.Finalizer", "lock"),

        f("java.lang.ref.Reference", "pending"),
        f("java.lang.ref.Reference", "lock"),

        f(VM.class, "booted"),
        f(VM.class, "finalRefCount"),
        f(VM.class, "peakFinalRefCount")
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

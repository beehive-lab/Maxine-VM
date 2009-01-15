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
     * The union of both the omitted transient and non-transient fields.
     */
    private static final FieldSet _omittedFields = new FieldSet(
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
                    java.lang.ref.Reference.class, "discovered",
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
                    Proxy.class, "constructorParams", "loaderToCache", "proxyClasses");

    /**
     * Checks whether the specified field should be omitted.
     *
     * @param field the field to check
     */
    public static boolean isOmittedField(Field field) {
        if (_omittedFields.contains(field)) {
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

/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.cri.ri;

import java.lang.reflect.*;

import com.sun.cri.ci.*;

/**
 * Represents a resolved or unresolved type in the compiler-runtime interface. Types include primitives, objects, {@code void},
 * and arrays thereof. Types, like fields and methods, are resolved through {@link RiConstantPool constant pools}, and
 * their actual implementation is provided by the {@link RiRuntime runtime} to the compiler. Note that most operations
 * are only available on resolved types.
 */
public interface RiType {

    /**
     * Represents each of the several different parts of the runtime representation of
     * a type which compiled code may need to reference individually. These may or may not be
     * different objects or data structures, depending on the runtime system.
     */
    public enum Representation {
        /**
         * The runtime representation of the data structure containing the static fields of this type.
         */
        StaticFields,

        /**
         * The runtime representation of the Java class object of this type.
         */
        JavaClass,

        /**
         * The runtime representation of the "hub" of this type--that is, the closest part of the type
         * representation which is typically stored in the object header.
         */
        ObjectHub,

        /**
         * The runtime representation of the type information for an object, which is typically used
         * for subtype tests.
         */
        TypeInfo
    }

    /**
     * Gets the name of this type in internal form. The following are examples of strings returned by this method:
     * <pre>
     *     "Ljava/lang/Object;"
     *     "I"
     *     "[[B"
     * </pre>
     *
     * @return the name of this type in internal form
     */
    String name();

    /**
     * Checks whether this type is resolved.
     * @return {@code true} if this type is resolved
     */
    boolean isResolved();

    /**
     * For array types, gets the type of the components.
     * @return the component type of this array type
     */
    RiType componentType();

    /**
     * Gets the type representing an array with elements of this type.
     * @return a new compiler interface type representing an array of this type
     */
    RiType arrayOf();

    /**
     * Gets the super type of this type or {@code null} if no such type exists.
     */
    RiType superType();

    /**
     * Gets the kind of this compiler interface type.
     * @param architecture When true, the architecture-specific kind used for emitting machine code is returned.
     *        When false, the kind according to the Java specification is returned.
     * @return the kind
     */
    CiKind kind(boolean architecture);

    /**
     * Gets the encoding of (that is, a constant representing the value of) the specified part of this type.
     * @param r the part of the this type
     * @return a constant representing a reference to the specified part of this type
     */
    CiConstant getEncoding(Representation r);

    /**
     * Gets the kind used to represent the specified part of this type.
     * @param r the part of the this type
     * @return the kind of constants for the specified part of the type
     */
    CiKind getRepresentationKind(Representation r);

    // NOTE: All operations beyond this point are only available on resolved types.

    /**
     * Checks whether this type has any subclasses so far. Any decisions
     * based on this information require the registration of a dependency, since
     * this information may change.
     * NOTE: ONLY AVAILABLE ON RESOLVED TYPES.
     * @return {@code true} if this class has subclasses
     */
    boolean hasSubclass();

    /**
     * Checks whether this type has a finalizer method.
     * NOTE: ONLY AVAILABLE ON RESOLVED TYPES.
     * @return {@code true} if this class has a finalizer
     */
    boolean hasFinalizer();

    /**
     * Checks whether this type has any finalizable subclasses so far. Any decisions
     * based on this information require the registration of a dependency, since
     * this information may change.
     * NOTE: ONLY AVAILABLE ON RESOLVED TYPES.
     * @return {@code true} if this class has any subclasses with finalizers
     */
    boolean hasFinalizableSubclass();

    /**
     * Checks whether this type is an interface.
     * NOTE: ONLY AVAILABLE ON RESOLVED TYPES.
     * @return {@code true} if this type is an interface
     */
    boolean isInterface();

    /**
     * Checks whether this type is an instance class.
     * NOTE: ONLY AVAILABLE ON RESOLVED TYPES.
     * @return {@code true} if this type is an instance class
     */
    boolean isInstanceClass();

    /**
     * Checks whether this type is an array class.
     * NOTE: ONLY AVAILABLE ON RESOLVED TYPES.
     * @return {@code true} if this type is an array class
     */
    boolean isArrayClass();

    /**
     * Gets the access flags for this type. Only the flags specified in the JVM specification
     * will be included in the returned mask. The utility methods in the {@link Modifier} class
     * should be used to query the returned mask for the presence/absence of individual flags.
     * NOTE: ONLY AVAILABLE ON RESOLVED METHODS.
     * @return the mask of JVM defined class access flags defined for this type
     */
    int accessFlags();

    /**
     * Checks whether this type is initialized.
     * NOTE: ONLY AVAILABLE ON RESOLVED TYPES.
     * @return {@code true} if this type is initialized
     */
    boolean isInitialized();

    /**
     * Checks whether this type is a subtype of another type.
     * NOTE: ONLY AVAILABLE ON RESOLVED TYPES.
     * @param other the type to test
     * @return {@code true} if this type a subtype of the specified type
     */
    boolean isSubtypeOf(RiType other);

    /**
     * Checks whether the specified object is an instance of this type.
     * NOTE: ONLY AVAILABLE ON RESOLVED TYPES.
     * @param obj the object to test
     * @return {@code true} if the object is an instance of this type
     */
    boolean isInstance(CiConstant obj);

    /**
     * Attempts to get an exact type for this type. Final classes,
     * arrays of final classes, and primitive types all have exact types.
     * NOTE: ONLY AVAILABLE ON RESOLVED TYPES.
     * @return the exact type of this type, if it exists; {@code null} otherwise
     */
    RiType exactType();

    /**
     * Attempts to get the unique concrete subtype of this type.
     * NOTE: ONLY AVAILABLE ON RESOLVED TYPES.
     * @return the exact type of this type, if it exists; {@code null} otherwise
     */
    RiType uniqueConcreteSubtype();

    /**
     * Resolves the method implementation for virtual dispatches on objects
     * of this dynamic type.
     * NOTE: ONLY AVAILABLE ON RESOLVED TYPES.
     * @param method the method to select the implementation of
     * @return the method implementation that would be selected at runtime
     */
    RiResolvedMethod resolveMethodImpl(RiResolvedMethod method);

    /**
     * Given an RiMethod a, returns a concrete RiMethod b that is the only possible
     * unique target for a virtual call on a(). Returns {@code null} if either no
     * such concrete method or more than one such method exists. Returns the method a
     * if a is a concrete method that is not overridden. If the compiler uses the
     * result of this method for its compilation, it must register an assumption
     * (see {@link CiAssumptions}), because dynamic class loading can invalidate
     * the result of this method.
     * NOTE: ONLY AVAILABLE ON RESOLVED TYPES.
     * @param method the method a for which a unique concrete target is searched
     * @return the unique concrete target or {@code null} if no such target exists
     *         or assumptions are not supported by this runtime
     */
    RiResolvedMethod uniqueConcreteMethod(RiResolvedMethod method);

    /**
     * Returns the instance fields declared in this class sorted by field offset.
     * NOTE: ONLY AVAILABLE ON RESOLVED TYPES.
     * @return an array of instance fields
     */
    RiField[] declaredFields();

    /**
     * Returns the java.lang.Class object representing this RiType instance or {@code null} if none exists.
     * NOTE: ONLY AVAILABLE ON RESOLVED TYPES.
     * @return the java.lang.Class object
     */
    Class<?> toJava();
}

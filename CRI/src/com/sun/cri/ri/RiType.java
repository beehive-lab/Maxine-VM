/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.cri.ri;

import java.lang.reflect.*;

import com.sun.cri.ci.*;

/**
 * Represents a resolved or unresolved type in the compiler-runtime interface. Types include primitives, objects, {@code void},
 * and arrays thereof. Types, like fields and methods, are resolved through {@link RiConstantPool constant pools}, and
 * their actual implementation is provided by the {@link RiRuntime runtime} to the compiler. Note that most operations
 * are only available on resolved types.
 *
 * @author Ben L. Titzer
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
     * Gets the kind of this compiler interface type.
     * @return the kind
     */
    CiKind kind();

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
     * Gets the Java class object associated with this type.
     * NOTE: ONLY AVAILABLE ON RESOLVED TYPES.
     * @return the Java class object for this type.
     */
    Class<?> javaClass();

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
    boolean isInstance(Object obj);

    /**
     * Attempts to get an exact type for this type. Final classes,
     * arrays of final classes, and primitive types all have exact types.
     * NOTE: ONLY AVAILABLE ON RESOLVED TYPES.
     * @return the exact type of this type, if it exists; {@code null} otherwise
     */
    RiType exactType();

    /**
     * Resolves the method implementation for virtual dispatches on objects
     * of this dynamic type.
     * NOTE: ONLY AVAILABLE ON RESOLVED TYPES.
     * @param method the method to select the implementation of
     * @return the method implementation that would be selected at runtime
     */
    RiMethod resolveMethodImpl(RiMethod method);

}

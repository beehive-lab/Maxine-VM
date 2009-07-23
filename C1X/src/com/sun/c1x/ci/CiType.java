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
package com.sun.c1x.ci;

import com.sun.c1x.util.*;
import com.sun.c1x.value.*;

/**
 * The <code>CiType</code> interface represents a resolved or unresolved type in the compiler
 * interface. Types include primitives, objects, {@code void}, and arrays thereof.
 * Types, like fields and methods, are resolved through {@link CiConstantPool constant
 * pools}, and their actual implementation is provided by the {@link CiRuntime runtime}
 * to the compiler. Note that some operations are only available on resolved types.
 *
 * @author Ben L. Titzer
 */
public interface CiType {
    /**
     * Gets the name of this type in internal form. The following are examples of strings returned by this method:
     * <pre>
     *     "Ljava/lang/Object;"
     *     "I"
     *     "[[B"
     * </pre>
     * To convert this name to it Java programming language form, use {@link Util#toJavaName(CiType)}.
     *
     * @return the name of this type in internal form
     */
    String name();

    /**
     * Gets the Java class object associated with this type.
     * NOTE THIS OPERATION IS ONLY AVAILABLE ON RESOLVED TYPES.
     * @return the Java class object for this type.
     */
    Class<?> javaClass();

    /**
     * Checks whether this type has any subclasses so far. Any decisions
     * based on this information require the registration of a dependency, since
     * this information may change.
     * NOTE THIS OPERATION IS ONLY AVAILABLE ON RESOLVED TYPES.
     * @return {@code true} if this class has subclasses
     */
    boolean hasSubclass();

    /**
     * Checks whether this type has a finalizer method.
     * NOTE THIS OPERATION IS ONLY AVAILABLE ON RESOLVED TYPES.
     * @return {@code true} if this class has a finalizer
     */
    boolean hasFinalizer();

    /**
     * Checks whether this type has any finalizable subclasses so far. Any decisions
     * based on this information require the registration of a dependency, since
     * this information may change.
     * NOTE THIS OPERATION IS ONLY AVAILABLE ON RESOLVED TYPES.
     * @return {@code true} if this class has any subclasses with finalizers
     */
    boolean hasFinalizableSubclass();

    /**
     * Checks whether this type is an interface.
     * NOTE THIS OPERATION IS ONLY AVAILABLE ON RESOLVED TYPES.
     * @return {@code true} if this type is an interface
     */
    boolean isInterface();

    /**
     * Checks whether this type is an instance class.
     * NOTE THIS OPERATION IS ONLY AVAILABLE ON RESOLVED TYPES.
     * @return {@code true} if this type is an instance class
     */
    boolean isInstanceClass();

    /**
     * Checks whether this type is an array class.
     * NOTE THIS OPERATION IS ONLY AVAILABLE ON RESOLVED TYPES.
     * @return {@code true} if this type is an array class
     */
    boolean isArrayKlass();

    /**
     * Checks whether this type is an array of non-primitive elements.
     * @return {@code true} if this type is an array of non-primitive elements
     */
    boolean isTypeArrayClass();

    /**
     * Checks whether this type is final.
     * NOTE THIS OPERATION IS ONLY AVAILABLE ON RESOLVED TYPES.
     * @return {@code true} if this type is final
     */
    boolean isFinal();

    /**
     * Checks whether this type is loaded (i.e. resolved).
     * @return {@code true} if this type is resolved
     */
    boolean isLoaded();

    /**
     * Checks whether this type is initialized.
     * NOTE THIS OPERATION IS ONLY AVAILABLE ON RESOLVED TYPES.
     * @return {@code true} if this type is initialized
     */
    boolean isInitialized();

    /**
     * Checks whether this type is a subtype of another type.
     * NOTE THIS OPERATION IS ONLY AVAILABLE ON RESOLVED TYPES.
     * @param other the type to test
     * @return {@code true} if this type a subtype of the specified type
     */
    boolean isSubtypeOf(CiType other);

    /**
     * Checks whether the specified object is an instance of this type.
     * NOTE THIS OPERATION IS ONLY AVAILABLE ON RESOLVED TYPES.
     * @param obj the object to test
     * @return {@code true} if the object is an instance of this type
     */
    boolean isInstance(Object obj);

    /**
     * For array types, gets the type of the elements.
     * @return the element type of this array type
     */
    CiType elementType();

    /**
     * Attempts to get an exact type for this type. Final classes,
     * arrays of final classes, and primitive types all have exact types.
     * NOTE THIS OPERATION IS ONLY AVAILABLE ON RESOLVED TYPES.
     * @return the exact type of this type, if it exists; {@code null} otherwise
     */
    CiType exactType();

    /**
     * Gets the type representing an array with elements of this type.
     * @return a new compiler interface type representing an array of this type
     */
    CiType arrayOf();

    /**
     * Resolves the method implementation for virtual dispatches on objects
     * of this dynamic type.
     * NOTE THIS OPERATION IS ONLY AVAILABLE ON RESOLVED TYPES.
     * @param method the method to select the implementation of
     * @return the method implementation that would be selected at runtime
     */
    CiMethod resolveMethodImpl(CiMethod method);

    /**
     * Gets the basic type of this compiler interface type.
     * @return the basic type
     */
    BasicType basicType();

    /**
     * The instance size of an object of this type.
     * XXX: this is a dumb name
     * @return the instance size in bytes
     */
    int sizeHelper();

    /**
     * Determines whether the slow path should always be taken when creating new instances of this type.
     * XXX: this is a dumb name
     * @return true if the slow path should always be taken, false otherwise
     */
    boolean layoutHelperNeedsSlowPath();

    int superCheckOffset();

    Object encoding();
}

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

import java.util.*;

import com.sun.c1x.util.*;

/**
 * The <code>RiMethod</code> interface represents resolved and unresolved methods.
 * Methods, like fields and types, are resolved through {@link RiConstantPool constant
 * pools}, and their actual implementation is provided by the {@link RiRuntime runtime}
 * to the compiler. Note that some operations are only available on resolved methods.
 *
 * @author Ben L. Titzer
 */
public interface RiMethod {

    /**
     * Gets the name of the method as a string.
     * @return the name of the method
     */
    String name();

    /**
     * Gets the holder of the method as a compiler interface type.
     * @return the holder
     */
    RiType holder();

    /**
     * Gets the signature of the method.
     * @return the signature of the method
     */
    RiSignature signatureType();

    /**
     * Gets the bytecode of the method, if the method has bytecode.
     * NOTE THAT THIS OPERATION IS ONLY AVAILABLE ON RESOLVED METHODS.
     * @return the bytecode of the method
     */
    byte[] code();

    /**
     * Gets the size of the bytecode of the method, if the method has bytecode.
     * NOTE THAT THIS OPERATION IS ONLY AVAILABLE ON RESOLVED METHODS.
     * @return the size of the bytecode of the method
     */
    int codeSize();

    /**
     * Gets the maximum number of locals used in this method's bytecode.
     * NOTE THAT THIS OPERATION IS ONLY AVAILABLE ON RESOLVED METHODS.
     * @return the maximum number of locals
     */
    int maxLocals();

    /**
     * Gets the maximum number of stack slots used in this method's bytecode.
     * NOTE THAT THIS OPERATION IS ONLY AVAILABLE ON RESOLVED METHODS.
     * @return the maximum number of stack slots
     */
    int maxStackSize();

    /**
     * Checks whether this method has balanced monitor operations.
     * NOTE THAT THIS OPERATION IS ONLY AVAILABLE ON RESOLVED METHODS.
     * @return {@code true} if the method has balanced monitor operations
     */
    boolean hasBalancedMonitors();

    /**
     * Checks whether the method has any exception handlers.
     * NOTE THAT THIS OPERATION IS ONLY AVAILABLE ON RESOLVED METHODS.
     * @return {@code true} if the method has an exception handlers
     */
    boolean hasExceptionHandlers();

    /**
     * Checks whether this method is loaded (i.e. is resolved).
     * @return {@code true} if the method is resolved
     */
    boolean isLoaded();

    /**
     * Checks whether this method is abstract.
     * NOTE THAT THIS OPERATION IS ONLY AVAILABLE ON RESOLVED METHODS.
     * @return {@code true} if the method is abstract
     */
    boolean isAbstract();

    /**
     * Checks whether this method is native.
     * NOTE THAT THIS OPERATION IS ONLY AVAILABLE ON RESOLVED METHODS.
     * @return {@code true} if the method is native
     */
    boolean isNative();

    /**
     * Checks whether this method is final.
     * NOTE THAT THIS OPERATION IS ONLY AVAILABLE ON RESOLVED METHODS.
     * @return {@code true} if the method is final
     */
    boolean isFinalMethod();

    /**
     * Checks whether this method is synchronized.
     * NOTE THAT THIS OPERATION IS ONLY AVAILABLE ON RESOLVED METHODS.
     * @return {@code true} if the method is synchronized
     */
    boolean isSynchronized();

    /**
     * Checks whether this method is strict w.r.t. floating point.
     * NOTE THAT THIS OPERATION IS ONLY AVAILABLE ON RESOLVED METHODS.
     * @return {@code true} if the method is strict
     */
    boolean isStrictFP();

    /**
     * Checks whether this method is static.
     * NOTE THAT THIS OPERATION IS ONLY AVAILABLE ON RESOLVED METHODS.
     * @return {@code true} if the method is static
     */
    boolean isStatic();

    /**
     * Checks whether this method has been overriden. Decisions made based
     * on a method being overriden must be registered as dependencies.
     * NOTE THAT THIS OPERATION IS ONLY AVAILABLE ON RESOLVED METHODS.
     * @return {@code true} if the method has been overriden
     */
    boolean isOverridden();

    /**
     * For virtual methods, this method returns the index into the virtual table
     * of the method.
     * NOTE THAT THIS OPERATION IS ONLY AVAILABLE ON RESOLVED METHODS.
     * @return the virtual table index
     */
    int vtableIndex();

    /**
     * Gets the {@link RiMethodProfile method data} for this method, which stores instrumentation,
     * including invocation counts, branch counts, etc.
     * @return the method data object, if it exists; {@code null} otherwise
     */
    RiMethodProfile methodData();

    /**
     * Gets the liveness map for local variables at the specified bytecode index, if it exists.
     * NOTE THAT THIS OPERATION IS ONLY AVAILABLE ON RESOLVED METHODS.
     * @param bci the bytecode index
     * @return the liveness map at the specified index, if it is available; {@code null} otherwise
     */
    BitMap liveness(int bci);

    /**
     * Checks whether this method can be statically bound (i.e. it is final or private or static).
     * NOTE THAT THIS OPERATION IS ONLY AVAILABLE ON RESOLVED METHODS.
     * @return {@code true} if this method can be statically bound
     */
    boolean canBeStaticallyBound();

    /**
     * Gets the list of exception handlers for this method.
     * NOTE THAT THIS OPERATION IS ONLY AVAILABLE ON RESOLVED METHODS.
     * @return the list of exception handlers
     */
    List<RiExceptionHandler> exceptionHandlers();

    /**
     * Retrieves the Java bytecode at the specified bytecode index.
     * @param bci the bytecode index
     * @return the java bytecode at the specified index
     */
    int javaCodeAtBci(int bci); // TODO: remove

    /**
     * Gets the interface method ID for this method, if this method is an interface method.
     * NOTE THAT THIS OPERATION IS ONLY AVAILABLE ON RESOLVED METHODS.
     * @return the interface method id
     */
    int interfaceID();
}

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


/**
 * Represents resolved and unresolved methods. Methods, like fields and types, are resolved through
 * {@link RiConstantPool constant pools}, and their actual implementation is provided by the {@link RiRuntime runtime}
 * to the compiler. Note that most operations are only available on resolved methods.
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
     * Gets the type in which this method is declared.
     * @return the type in which this method is declared
     */
    RiType holder();

    /**
     * Gets the signature of the method.
     * @return the signature of the method
     */
    RiSignature signature();

    /**
     * Checks whether this method is resolved.
     * @return {@code true} if the method is resolved
     */
    boolean isResolved();

    /**
     * Gets the bytecode of the method, if the method {@linkplain #isResolved()} and has code.
     * @return the bytecode of the method or {@code null} if none is available
     */
    byte[] code();

    /**
     * Gets the {@link RiMethodProfile method data} for this method, which stores instrumentation,
     * including invocation counts, branch counts, etc.
     * @return the method data object, if it exists; {@code null} otherwise
     */
    RiMethodProfile methodData();

    // N.B. All operations beyond this point are only available on resolved methods.

    /**
     * Gets the symbol used to link this method if it is native, otherwise {@code null}.
     * NOTE: ONLY AVAILABLE ON RESOLVED METHODS.
     */
    String jniSymbol();

    /**
     * Gets the maximum number of locals used in this method's bytecode.
     * NOTE: ONLY AVAILABLE ON RESOLVED METHODS.
     * @return the maximum number of locals
     */
    int maxLocals();

    /**
     * Gets the maximum number of stack slots used in this method's bytecode.
     * NOTE: ONLY AVAILABLE ON RESOLVED METHODS.
     * @return the maximum number of stack slots
     */
    int maxStackSize();

    /**
     * Checks whether this method has balanced monitor operations.
     * NOTE: ONLY AVAILABLE ON RESOLVED METHODS.
     * @return {@code true} if the method has balanced monitor operations
     */
    boolean hasBalancedMonitors();

    /**
     * Gets the access flags for this method. Only the flags specified in the JVM specification
     * will be included in the returned mask. The utility methods in the {@link Modifier} class
     * should be used to query the returned mask for the presence/absence of individual flags.
     * NOTE: ONLY AVAILABLE ON RESOLVED METHODS.
     * @return the mask of JVM defined method access flags defined for this method
     */
    int accessFlags();
    
    /**
     * Checks whether this method is a leaf method.
     * NOTE: ONLY AVAILABLE ON RESOLVED METHODS.
     * @return {@code true} if the method is a leaf method (that is, is final or private)
     */
    boolean isLeafMethod();

    /**
     * Checks whether this method is a class initializer (that is, <code> &lt;clinit&gt;</code>).
     * NOTE: ONLY AVAILABLE ON RESOLVED METHODS.
     * @return {@code true} if the method is a class initializer
     */
    boolean isClassInitializer();

    /**
     * Checks whether this method is a constructor.
     * NOTE: ONLY AVAILABLE ON RESOLVED METHODS.
     * @return {@code true} if the method is a constructor
     */
    boolean isConstructor();

    /**
     * Checks whether this method has been overridden. Decisions made based
     * on a method being overridden must be registered as dependencies.
     * NOTE: ONLY AVAILABLE ON RESOLVED METHODS.
     * @return {@code true} if the method has been overridden
     */
    boolean isOverridden();

    /**
     * TODO: Currently this must always return null! Fix me!
     * Gets the liveness map for local variables at the specified bytecode index, if it exists.
     * NOTE: ONLY AVAILABLE ON RESOLVED METHODS.
     * @param bci the bytecode index
     * @return the liveness map at the specified index, if it is available; {@code null} otherwise
     */
    Object liveness(int bci);

    /**
     * Checks whether this method can be statically bound (that is, it is final or private or static).
     * NOTE: ONLY AVAILABLE ON RESOLVED METHODS.
     * @return {@code true} if this method can be statically bound
     */
    boolean canBeStaticallyBound();

    /**
     * Gets the list of exception handlers for this method.
     * NOTE: ONLY AVAILABLE ON RESOLVED METHODS.
     * @return the list of exception handlers
     */
    RiExceptionHandler[] exceptionHandlers();
    
    /**
     * Temporary work-around to support the @ACCESSOR Maxine annotation.
     * Non-Maxine VMs should just return {@code null}.
     */
    Class<?> accessor();
}

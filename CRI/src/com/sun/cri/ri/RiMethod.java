/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
     * Gets a map from bytecode indexes to bit maps denoting the live locals at that position.
     * If a non-null array is return, its length is guaranteed to be equal to {@code code().length}. 
     * 
     * NOTE: ONLY AVAILABLE ON RESOLVED METHODS.
     * @return the liveness map if it is available; {@code null} otherwise
     */
    CiBitMap[] livenessMap();

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
     * Gets a stack trace element for this method and a given bytecode index.
     */
    StackTraceElement toStackTraceElement(int bci);
    
    /**
     * Indicates whether this method has compiled code.
     * @return {@code true} if this method has compiled code 
     */
    boolean hasCompiledCode();
    
    /**
     * Temporary work-around to support the @ACCESSOR Maxine annotation.
     * Non-Maxine VMs should just return {@code null}.
     */
    Class<?> accessor();
    
    /**
     * Temporary work-around to support the @ACCESSOR Maxine annotation.
     * Non-Maxine VMs should just return 0.
     */
    int intrinsic();
}

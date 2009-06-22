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

/**
 * The <code>CiRuntime</code> class provides the major interface between the compiler and the
 * runtime system, including access to constant pools, OSR frames, inlining requirements,
 * and runtime calls such as checkcast. C1X may insert calls to the
 * implementation of these methods into compiled code, typically as the slow path.
 *
 * @author Ben L. Titzer
 */
public interface CiRuntime {
    /**
     * Gets the constant pool for a method.
     * @param method the method
     * @return the constant pool for the method
     */
    CiConstantPool getConstantPool(CiMethod method);

    /**
     * Gets an {@link com.sun.c1x.ci.CiOsrFrame OSR frame} instance for the specified method
     * at the specified OSR bytecode index.
     * @param method the method
     * @param bci the bytecode index
     * @return an OSR frame that describes the layout of the frame
     */
    CiOsrFrame getOsrFrame(CiMethod method, int bci);

    /**
     * Checks whether the specified method is required to be inlined (for semantic reasons).
     * @param method the method being called
     * @return {@code true} if the method must be inlined; {@code false} to let the compiler
     * use its own heuristics
     */
    boolean mustInline(CiMethod method);

    /**
     * Checks whether the specified method must not be inlined (for semantic reasons).
     * @param method the method being called
     * @return {@code true} if the method must not be inlined; {@code false} to let the compiler
     * use its own heuristics
     */
    boolean mustNotInline(CiMethod method);

    /**
     * Checks whether the specified method cannot be compiled.
     * @param method the method being called
     * @return {@code true} if the method cannot be compiled
     */
    boolean mustNotCompile(CiMethod method);

    // Hypothetical runtime calls:

    boolean instanceOf(Object object, CiType type);
    Object checkCast(Object object, CiType type);
    Object allocateObject(CiType type);
    Object allocateArray(CiType type, int length);
    CiType resolveType(String name);
    CiType getType(Class<?> javaClass);

}

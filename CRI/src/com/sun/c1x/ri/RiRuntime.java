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
package com.sun.c1x.ri;

import com.sun.c1x.ci.CiKind;
import com.sun.c1x.ci.CiLocation;
import com.sun.c1x.ci.CiRegister;
import com.sun.c1x.ci.CiTargetMethod;

/**
 * This interface encapsulates the main functionality of the runtime for the compiler, including access
 * to constant pools, OSR frames, inlining requirements, and runtime calls such as checkcast.
 *
 * @author Ben L. Titzer
 * @author Thomas Wuerthinger
 */
public interface RiRuntime {
    /**
     * Gets the constant pool for a method.
     * @param method the method
     * @return the constant pool for the method
     */
    RiConstantPool getConstantPool(RiMethod method);

    /**
     * Gets an {@link RiOsrFrame OSR frame} instance for the specified method
     * at the specified OSR bytecode index.
     * @param method the method
     * @param bci the bytecode index
     * @return an OSR frame that describes the layout of the frame
     */
    RiOsrFrame getOsrFrame(RiMethod method, int bci);

    /**
     * Checks whether the specified method is required to be inlined (for semantic reasons).
     * @param method the method being called
     * @return {@code true} if the method must be inlined; {@code false} to let the compiler
     * use its own heuristics
     */
    boolean mustInline(RiMethod method);

    /**
     * Checks whether the specified method must not be inlined (for semantic reasons).
     * @param method the method being called
     * @return {@code true} if the method must not be inlined; {@code false} to let the compiler
     * use its own heuristics
     */
    boolean mustNotInline(RiMethod method);

    /**
     * Checks whether the specified method cannot be compiled.
     * @param method the method being called
     * @return {@code true} if the method cannot be compiled
     */
    boolean mustNotCompile(RiMethod method);

    /**
     * Byte offset of the array length of an array object.
     * @return the byte offset of the array length
     */
    int arrayLengthOffsetInBytes();

    /**
     * Byte offset of the field of an object that contains the pointer to the internal class representation of the type of the object.
     * @return the byte offset of the class field
     */
    int hubOffset();

    /**
     * Byte offset of the field of the internal thread representation that contains the pointer to the thread exception object.
     * @return the byte offset of the exception object field
     */
    int threadExceptionOffset();

    /**
     * Checks whether an explicit null check is needed with the given offset of accessing an object.
     * If this the offset is low, then an implicit null check will work.
     * @param offset the offset at which the object is accessed
     * @return true if an explicit null check is needed, false otherwise
     */
    boolean needsExplicitNullCheck(int offset);

    /**
     * Checks whether we are on a multiprocessor system.
     * @return true if we are on a multiprocessor system, false otherwise
     */
    boolean isMP();

    /**
     * Checks whether jvmti can post exceptions.
     * @return true if jvmti can post exceptions, false otherwise.
     */
    boolean jvmtiCanPostExceptions();

    /**
     * Byte offset of the virtual method table of an internal class object.
     * @return the virtual method table offset in bytes
     */
    int vtableStartOffset();

    /**
     * Byte size of a single virtual method table entry of an internal class object.
     * @return the virtual method table entry
     */
    int vtableEntrySize();

    /**
     * Byte offset of the method field of a virtual method table entry.
     * @return the method offset in bytes
     */
    int vtableEntryMethodOffsetInBytes();

    /**
     * Resolves a given identifier to a type.
     * @param string the name of the type
     * @return the resolved type
     */
    RiType resolveType(String string);

    /**
     * The offset of the first array element of an array of the given type.
     *
     * @param type the type of the array
     * @return the offset in bytes
     */
    int firstArrayElementOffset(CiKind type);

    /**
     * Offset used for the implementation of an intrinsic.
     *
     * @return the offset in bytes
     */
    int sunMiscAtomicLongCSImplValueOffset();

    /**
     * The size of an array header of an array of the given type.
     *
     * @param type the type of the array
     * @return the size in bytes
     */
    int arrayHeaderSize(CiKind type);

    /**
     * Offset of the lock within the lock object.
     *
     * @return the offset in bytes
     */
    int basicObjectLockOffsetInBytes();

    /**
     * Size of a lock object.
     *
     * @return the size in bytes
     */
    int sizeofBasicObjectLock();

    /**
     * Offset of the element hub of an array hub object.
     *
     * @return the offset in bytes
     */
    int elementHubOffset();

    /**
     * @return the maximum length of an array
     */
    int maximumArrayLength();

    /**
     * Calling convention for Java calls.
     *
     * @param signature the basic types of the parameters
     * @param outgoing if the convention is for incoming or outgoing parameters
     * @return an array of exactly the same size as the signature parameter that specifies at which location the
     *         parameters must be stored
     */
    CiLocation[] javaCallingConvention(CiKind[] types, boolean outgoing);

    /**
     * Calling convention for outgoing runtime calls.
     *
     * @param signature the basic types of the parameters
     * @return an array of exactly the same size as the signature parameter that specifies at which location the
     *         parameters must be stored
     */
    CiLocation[] runtimeCallingConvention(CiKind[] signature);

    /**
     * The return register that is used for the given return type.
     *
     * @param kind the basic type of the return parameter
     * @return the register
     */
    CiRegister returnRegister(CiKind kind);

    /**
     * The size of a JIT stack slot. Used for implementing the adapter frames.
     *
     * @return the JIT stack slot size in bytes
     */
    int getJITStackSlotSize();

    /**
     * The offset of the normal entry to the code. The compiler inserts NOP instructions to satisfy this constraint.
     *
     * @return the code offset in bytes
     */
    int codeOffset();

    /**
     * Returns the disassembly of the given code bytes. Used for debugging purposes only.
     *
     * @param code the code bytes that should be disassembled
     * @return the disassembly as a String object
     */
    String disassemble(byte[] code);

    /**
     * Registers the given global stub and returns an object that can be used to identify it in the relocation
     * information.
     *
     * @param targetMethod the target method representing the code of the global stub
     * @param name the name of the stub, used for debugging purposes only
     * @return the identification object
     */
    Object registerTargetMethod(CiTargetMethod targetMethod, String name);

    /**
     * Returns the runtime interface type representing the array type of the given kind.
     *
     * @param elementType the primitive type of the array
     * @return the array type
     */
    RiType primitiveArrayType(CiKind elementType);

    /**
     * Returns the register that stores the thread object.
     *
     * @return the thread register
     */
    CiRegister threadRegister();

    /**
     * Returns the runtime interface representation of the given Java class object.
     *
     * @param javaClass the Java class object
     * @return the runtime interface representation
     */
    public RiType getRiType(Class<?> javaClass);

    /**
     * Returns the register used for safepoint polling.
     *
     * @return the safepoint register
     */
    CiRegister getSafepointRegister();
}

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

import com.sun.c1x.target.*;
import com.sun.c1x.value.*;

/**
 * The <code>RiRuntime</code> class provides the major interface between the compiler and the
 * runtime system, including access to constant pools, OSR frames, inlining requirements,
 * and runtime calls such as checkcast. C1X may insert calls to the
 * implementation of these methods into compiled code, typically as the slow path.
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
     * The header size of an object in words.
     * @return the header size in words
     */
    int headerSize();

    /**
     * Byte offset of the field of an object that contains the pointer to the internal class representation of the type of the object.
     * @return the byte offset of the class field
     */
    int klassOffsetInBytes();

    /**
     * Byte offset of the field of the internal thread representation that contains the pointer to the thread exception object.
     * @return the byte offset of the exception object field
     */
    int threadExceptionOopOffset();

    /**
     * Byte offset of the field of the internal thread representation that contains the exception pc.
     * @return the byte offset of the exception pc
     */
    int threadExceptionPcOffset();

    /**
     * Byte offset of the field of the internal thread representation that contains the pointer to the Java thread object.
     * @return the byte offset of the thread object field
     */
    int threadObjOffset();

    /**
     * Byte offset of the field of the internal class representation that contains the pointer to the Java class object.
     * @return the byte offset of the class object field
     */
    int klassJavaMirrorOffsetInBytes();

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
     * Byte offset of the limit field of java.nio.Buffer.
     * @return the byte offset of the limit field
     */
    int javaNioBufferLimitOffset();

    /**
     * Returns the address of the throw counter. This is used for counting the number of throws.
     * @return the address of the throw counter
     */
    long throwCountAddress();

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

    int arrayBaseOffsetInBytes(BasicType type);

    int sunMiscAtomicLongCSImplValueOffset();

    Register callerSaveFpuRegAt(int i);

    int arrayOopDescHeaderSize(BasicType type);

    int arrayElementSize(BasicType type);

    void vmExitOutOfMemory1(int i, String string, String name);

    int vmPageSize();

    long getPollingPage();

    Register javaCallingConventionReceiverRegister();

    int interpreterFrameMonitorSize();

    int basicObjectLockSize();

    int basicObjectLockOffsetInBytes();

    int basicObjectObjOffsetInBytes();

    int basicLockDisplacedHeaderOffsetInBytes();

    int initStateOffsetInBytes();

    int instanceKlassFullyInitialized();

    int elementKlassOffsetInBytes();

    int methodDataNullSeenByteConstant();

    int secondarySuperCacheOffsetInBytes();

    long doubleSignmaskPoolAddress();

    Object universeNonOopWord();

    int nativeMovConstRegInstructionSize();

    Register getCRarg(int i); // TODO: appears only used for array copy intrinsic

    Register getJRarg(int i); // TODO: appears only used for array copy intrinsic

    int markOffsetInBytes();

    int argRegSaveAreaBytes();

    int threadPendingExceptionOffset();

    boolean universeSupportsInlineContigAlloc();

    int threadTlabTopOffset();

    int threadTlabEndOffset();

    int threadTlabStartOffset();

    int superCheckOffsetOffsetInBytes();

    int secondarySupersOffsetInBytes();

    int threadTlabSizeOffset();

    int vtableLengthOffset();

    int itableMethodEntryMethodOffset();

    int itableOffsetEntrySize();

    int itableInterfaceOffsetInBytes();

    int itableOffsetOffsetInBytes();

    long getMemorySerializePage();

    int biasedLockMaskInPlace();

    int biasedLockPattern();

    int unlockedValue();

    int maxArrayAllocationLength();

    int prototypeHeaderOffsetInBytes();

    int markOopDescPrototype();

    int klassPartOffsetInBytes();

    int getMinObjAlignmentInBytesMask();

    int instanceOopDescBaseOffsetInBytes();

    int outPreserveStackSlots();

    // TODO: why not pass the RiSignature instead?
    int javaCallingConvention(BasicType[] types, CiLocation[] result, boolean outgoing);

    int sizeofBasicObjectLock();

    int codeOffset();

    String disassemble(byte[] copyOf);

    CiLocation receiverLocation();

    int sizeofKlassOopDesc();

    int initThreadOffsetInBytes();

    Register returnRegister(BasicType object);

    int runtimeCallingConvention(BasicType[] signature, CiLocation[] regs);

    Object registerTargetMethod(CiTargetMethod targetMethod, String name);

    RiType primitiveArrayType(BasicType elemType);

    Register threadRegister();
}

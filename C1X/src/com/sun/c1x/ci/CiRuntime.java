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

import com.sun.c1x.target.Register;
import com.sun.c1x.value.BasicType;

/**
 * The <code>CiRuntime</code> class provides the major interface between the compiler and the
 * runtime system, including access to constant pools, OSR frames, inlining requirements,
 * and runtime calls such as checkcast. C1X may insert calls to the
 * implementation of these methods into compiled code, typically as the slow path.
 *
 * @author Ben L. Titzer
 * @author Thomas Wuerthinger
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
     * Checks wheter the dtrace method runtime stub should be called at method entry.
     * @return true if the runtime stub should be called, false otherwise
     */
    boolean dtraceMethodProbes();

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
    CiType resolveType(String string);

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

    long basicObjectLockSize();

    long basicObjectLockOffsetInBytes();

    long basicObjectObjOffsetInBytes();

    int getSerializePageShiftCount();

    int basicLockDisplacedHeaderOffsetInBytes();

    int initStateOffsetInBytes();

    int instanceKlassFullyInitialized();

    int elementKlassOffsetInBytes();

    int methodDataNullSeenByteConstant();

    int secondarySuperCacheOffsetInBytes();

    long doubleSignmaskPoolAddress();

    Object universeNonOopWord();

    long floatSignflipPoolAddress();

    long doubleSignflipPoolAddress();

    int nativeMovConstRegInstructionSize();

    Register getCRarg(int i);

    Register getJRarg(int i);

    int markOffsetInBytes();

    int argRegSaveAreaBytes();

    int threadPendingExceptionOffset();

    int threadVmResultOffset();

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

    long biasedLockingFastPathEntryCountAddr();

    int maxArrayAllocationLength();

    int prototypeHeaderOffsetInBytes();

    int markOopDescPrototype();

    int klassPartOffsetInBytes();

    int getMinObjAlignmentInBytesMask();

    boolean dtraceAllocProbes();

    int instanceOopDescBaseOffsetInBytes();

    int outPreserveStackSlots();

    int javaCallingConvention(BasicType[] types, CiLocation[] result, boolean outgoing);

    int sizeofBasicObjectLock();

    int codeOffset();

    String disassemble(byte[] copyOf);

    CiLocation receiverLocation();

    int sizeofKlassOopDesc();

    int initThreadOffsetInBytes();

    int convertToPointer32(Object obj);

    long convertToPointer64(Object obj);

}

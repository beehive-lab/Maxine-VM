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
package com.sun.max.vm.compiler.c1x;

import java.util.*;

import com.sun.c1x.ci.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.target.x86.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * The <code>MaxCiRuntime</code> class implements the runtime interface needed by C1X.
 * This includes access to runtime features such as class and method representations,
 * constant pools, as well as some compiler tuning.
 *
 * @author Ben L. Titzer
 */
public class MaxCiRuntime implements CiRuntime {

    public static final MaxCiRuntime globalRuntime = new MaxCiRuntime();

    final MaxCiConstantPool globalConstantPool = new MaxCiConstantPool(this, null);

    final WeakHashMap<MaxCiField, MaxCiField> fields = new WeakHashMap<MaxCiField, MaxCiField>();
    final WeakHashMap<MaxCiMethod, MaxCiMethod> methods = new WeakHashMap<MaxCiMethod, MaxCiMethod>();
    final WeakHashMap<MaxCiType, MaxCiType> types = new WeakHashMap<MaxCiType, MaxCiType>();
    final WeakHashMap<ConstantPool, MaxCiConstantPool> constantPools = new WeakHashMap<ConstantPool, MaxCiConstantPool>();

    /**
     * Gets the constant pool for a specified method.
     * @param method the compiler interface method
     * @return the compiler interface constant pool for the specified method
     */
    public CiConstantPool getConstantPool(CiMethod method) {
        final ClassMethodActor classMethodActor = this.asClassMethodActor(method, "getConstantPool()");
        final ConstantPool cp = classMethodActor.rawCodeAttribute().constantPool();
        synchronized (this) {
            MaxCiConstantPool constantPool = constantPools.get(cp);
            if (constantPool == null) {
                constantPool = new MaxCiConstantPool(this, cp);
                constantPools.put(cp, constantPool);
            }
            return constantPool;
        }
    }

    /**
     * Resolves a compiler interface type by its name. Note that this
     * method should only be called for globally available classes (e.g. java.lang.*),
     * since it does not supply a constant pool.
     * @param name the name of the class
     * @return the compiler interface type for the class
     */
    public CiType resolveType(String name) {
        final ClassActor classActor = ClassRegistry.get((ClassLoader) null, JavaTypeDescriptor.getDescriptorForJavaString(name));
        if (classActor != null) {
            return globalConstantPool.canonicalCiType(classActor);
        }
        return null;
    }

    /**
     * Gets the compiler interface type for the specified Java class.
     * @param javaClass the java class object
     * @return the compiler interface type for the specified class
     */
    public CiType getType(Class<?> javaClass) {
        return globalConstantPool.canonicalCiType(ClassActor.fromJava(javaClass));
    }

    /**
     * Gets the <code>CiMethod</code> for a given method actor.
     * @param methodActor the method actor
     * @return the canonical compiler interface method for the method actor
     */
    public CiMethod getCiMethod(MethodActor methodActor) {
        return globalConstantPool.canonicalCiMethod(methodActor);
    }

    /**
     * Gets the OSR frame for a particular method at a particular bytecode index.
     * @param method the compiler interface method
     * @param bci the bytecode index
     * @return the OSR frame
     */
    public CiOsrFrame getOsrFrame(CiMethod method, int bci) {
        throw FatalError.unimplemented();
    }

    /**
     * Checks whether the runtime requires inlining of the specified method.
     * @param method the method to inline
     * @return <code>true</code> if the method must be inlined; <code>false</code>
     * to allow the compiler to use its own heuristics
     */
    public boolean mustInline(CiMethod method) {
        return asClassMethodActor(method, "mustInline()").isInline();
    }

    /**
     * Checks whether the runtime forbids inlining of the specified method.
     * @param method the method to inline
     * @return <code>true</code> if the runtime forbids inlining of the specified method;
     * <code>false</code> to allow the compiler to use its own heuristics
     */
    public boolean mustNotInline(CiMethod method) {
        final ClassMethodActor classMethodActor = asClassMethodActor(method, "mustNotInline()");
        return classMethodActor.rawCodeAttribute() == null || classMethodActor.isNeverInline();
    }

    /**
     * Checks whether the runtime forbids compilation of the specified method.
     * @param method the method to compile
     * @return <code>true</code> if the runtime forbids compilation of the specified method;
     * <code>false</code> to allow the compiler to compile the method
     */
    public boolean mustNotCompile(CiMethod method) {
        return false;
    }

    @Override
    public CiType makeTypeArrayClass(BasicType elemType) {
        throw Util.unimplemented();
    }

    @Override
    public Register getCRarg(int i) {
        switch(i) {
            case 0:
                return X86Register.rdi;
            case 1:
                return X86Register.rsi;
            case 2:
                return X86Register.rdx;
            case 3:
                return X86Register.rcx;
            case 4:
                return X86Register.r8;
            case 5:
                return X86Register.r9;
        }
        Util.unimplemented();
        throw Util.shouldNotReachHere();
    }

    @Override
    public Register getJRarg(int i) {
        if (i == 5) {
            return getCRarg(0);
        }
        return getCRarg(i + 1);
    }

    ClassMethodActor asClassMethodActor(CiMethod method, String operation) {
        if (method instanceof MaxCiMethod) {
            return ((MaxCiMethod) method).asClassMethodActor(operation);
        }
        throw new MaxCiUnresolved("invalid CiMethod instance: " + method.getClass());
    }

    ClassActor asClassActor(CiType type, String operation) {
        if (type instanceof MaxCiType) {
            return ((MaxCiType) type).asClassActor(operation);
        }
        throw new MaxCiUnresolved("invalid CiType instance: " + type.getClass());
    }

    public int arrayLengthOffsetInBytes() {
        throw Util.unimplemented();
    }

    public boolean dtraceMethodProbes() {
        throw Util.unimplemented();
    }

    @Override
    public long getRuntimeEntry(CiRuntimeCall runtimeCall) {
        throw Util.unimplemented();
    }

    @Override
    public int headerSize() {
        throw Util.unimplemented();
    }

    @Override
    public boolean isMP() {
        return true;
    }

    @Override
    public int javaNioBufferLimitOffset() {
        throw Util.unimplemented();
    }

    @Override
    public boolean jvmtiCanPostExceptions() {
        throw Util.unimplemented();
    }

    @Override
    public int klassJavaMirrorOffsetInBytes() {
        throw Util.unimplemented();
    }

    @Override
    public int klassOffsetInBytes() {
        throw Util.unimplemented();
    }

    @Override
    public boolean needsExplicitNullCheck(int offset) {
        // TODO: Return false if implicit null check is possible for this offset!
        return true;
    }

    @Override
    public int threadExceptionOopOffset() {
        throw Util.unimplemented();
    }

    @Override
    public int threadExceptionPcOffset() {
        throw Util.unimplemented();
    }

    @Override
    public int threadObjOffset() {
        throw Util.unimplemented();
    }

    @Override
    public Address throwCountAddress() {
        throw Util.unimplemented();
    }

    @Override
    public int vtableEntryMethodOffsetInBytes() {
        throw Util.unimplemented();
    }

    @Override
    public int vtableEntrySize() {
        throw Util.unimplemented();
    }

    @Override
    public int vtableStartOffset() {
        throw Util.unimplemented();
    }

    @Override
    public int arrayBaseOffsetInBytes(BasicType type) {
        throw Util.unimplemented();
    }

    @Override
    public int nativeCallInstructionSize() {
        throw Util.unimplemented();
    }

    @Override
    public Register callerSaveFpuRegAt(int i) {
        throw Util.unimplemented();
    }

    @Override
    public Object ciEnvUnloadedCiobjarrayklass() {
        throw Util.unimplemented();
    }

    @Override
    public Object makeObjectArrayClass(CiType elementClass) {
        throw Util.unimplemented();
    }

    @Override
    public int sunMiscAtomicLongCSImplValueOffset() {
        throw Util.unimplemented();
    }

    @Override
    public int arrayElementSize(BasicType type) {
        throw Util.unimplemented();
    }

    @Override
    public int arrayOopDescHeaderSize(BasicType type) {
        throw Util.unimplemented();
    }

    @Override
    public void vmExitOutOfMemory1(int i, String string, String name) {
        throw Util.unimplemented();
    }

    @Override
    public int vmPageSize() {
        throw Util.unimplemented();
    }

    @Override
    public Address argRegSaveAreaBytes() {
        throw Util.unimplemented();
    }

    @Override
    public int basicLockDisplacedHeaderOffsetInBytes() {
        throw Util.unimplemented();
    }

    @Override
    public long basicObjectLockOffsetInBytes() {
        throw Util.unimplemented();
    }

    @Override
    public long basicObjectLockSize() {
        throw Util.unimplemented();
    }

    @Override
    public long basicObjectObjOffsetInBytes() {
        throw Util.unimplemented();
    }

    @Override
    public long doubleSignflipPoolAddress() {
        throw Util.unimplemented();
    }

    @Override
    public long doubleSignmaskPoolAddress() {
        throw Util.unimplemented();
    }

    @Override
    public int elementKlassOffsetInBytes() {
        throw Util.unimplemented();
    }

    @Override
    public long floatSignflipPoolAddress() {
        throw Util.unimplemented();
    }


    @Override
    public long getPollingPage() {
        throw Util.unimplemented();
    }

    @Override
    public int getSerializePageShiftCount() {
        throw Util.unimplemented();
    }

    @Override
    public int initStateOffsetInBytes() {
        throw Util.unimplemented();
    }

    @Override
    public int instanceKlassFullyInitialized() {
        throw Util.unimplemented();
    }

    @Override
    public int interpreterFrameMonitorSize() {
        throw Util.unimplemented();
    }

    @Override
    public Register javaCallingConventionReceiverRegister() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int markOffsetInBytes() {
        throw Util.unimplemented();
    }

    @Override
    public int methodDataNullSeenByteConstant() {
        throw Util.unimplemented();
    }

    @Override
    public int nativeCallDisplacementOffset() {
        throw Util.unimplemented();
    }

    @Override
    public int nativeMovConstRegInstructionSize() {
        throw Util.unimplemented();
    }

    @Override
    public int secondarySuperCacheOffsetInBytes() {
        throw Util.unimplemented();
    }

    @Override
    public int secondarySupersOffsetInBytes() {
        throw Util.unimplemented();
    }

    @Override
    public int superCheckOffsetOffsetInBytes() {
        throw Util.unimplemented();
    }

    @Override
    public int threadPendingExceptionOffset() {
        throw Util.unimplemented();
    }

    @Override
    public int threadTlabEndOffset() {
        throw Util.unimplemented();
    }

    @Override
    public int threadTlabSizeOffset() {
        throw Util.unimplemented();
    }

    @Override
    public int threadTlabStartOffset() {
        throw Util.unimplemented();
    }

    @Override
    public int threadTlabTopOffset() {
        throw Util.unimplemented();
    }

    @Override
    public int threadVmResultOffset() {
        throw Util.unimplemented();
    }

    @Override
    public Object universeNonOopWord() {
        throw Util.unimplemented();
    }

    @Override
    public boolean universeSupportsInlineContigAlloc() {
        throw Util.unimplemented();
    }

    @Override
    public int biasedLockMaskInPlace() {
        throw Util.unimplemented();
    }

    @Override
    public int biasedLockPattern() {
        throw Util.unimplemented();
    }

    @Override
    public long biasedLockingFastPathEntryCountAddr() {
        throw Util.unimplemented();
    }

    @Override
    public boolean dtraceAllocProbes() {
        throw Util.unimplemented();
    }

    @Override
    public long getMemorySerializePage() {
        throw Util.unimplemented();
    }

    @Override
    public int getMinObjAlignmentInBytesMask() {
        throw Util.unimplemented();
    }

    @Override
    public int instanceOopDescBaseOffsetInBytes() {
        throw Util.unimplemented();
    }

    @Override
    public int itableInterfaceOffsetInBytes() {
        throw Util.unimplemented();
    }

    @Override
    public int itableMethodEntryMethodOffset() {
        throw Util.unimplemented();
    }

    @Override
    public int itableOffsetEntrySize() {
        throw Util.unimplemented();
    }

    @Override
    public int itableOffsetOffsetInBytes() {
        throw Util.unimplemented();
    }

    @Override
    public int klassPartOffsetInBytes() {
        throw Util.unimplemented();
    }

    @Override
    public int markOopDescPrototype() {
        throw Util.unimplemented();
    }

    @Override
    public int maxArrayAllocationLength() {
        throw Util.unimplemented();
    }

    @Override
    public int prototypeHeaderOffsetInBytes() {
        throw Util.unimplemented();
    }

    @Override
    public int unlockedValue() {
        throw Util.unimplemented();
    }

    @Override
    public int vtableLengthOffset() {
        throw Util.unimplemented();
    }
}

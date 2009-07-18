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
import com.sun.c1x.target.*;
import com.sun.c1x.target.x86.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.*;
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

    public Register getCRarg(int i) {
        // TODO: move this out of the compiler interface
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

    public Register getJRarg(int i) {
        // TODO: move this out of the compiler interface
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
        // TODO: currently save to return false
        return false;
    }

    public long getRuntimeEntry(CiRuntimeCall runtimeCall) {
        throw Util.unimplemented();
    }

    public int headerSize() {
        throw Util.unimplemented();
    }

    public boolean isMP() {
        return true;
    }

    public int javaNioBufferLimitOffset() {
        throw Util.unimplemented();
    }

    public boolean jvmtiCanPostExceptions() {
        throw Util.unimplemented();
    }

    public int klassJavaMirrorOffsetInBytes() {
        throw Util.unimplemented();
    }

    public int klassOffsetInBytes() {
        return Layout.generalLayout().getOffsetFromOrigin(HeaderField.HUB).toInt();
    }

    public boolean needsExplicitNullCheck(int offset) {
        // TODO: Return false if implicit null check is possible for this offset!
        return offset > 0xbad;
    }

    public int threadExceptionOopOffset() {
        throw Util.unimplemented();
    }

    public int threadExceptionPcOffset() {
        throw Util.unimplemented();
    }

    public int threadObjOffset() {
        throw Util.unimplemented();
    }

    public long throwCountAddress() {
        throw Util.unimplemented();
    }

    public int vtableEntryMethodOffsetInBytes() {
        throw Util.unimplemented();
    }

    public int vtableEntrySize() {
        throw Util.unimplemented();
    }

    public int vtableStartOffset() {
        throw Util.unimplemented();
    }

    public int arrayBaseOffsetInBytes(BasicType type) {
        throw Util.unimplemented();
    }

    public int nativeCallInstructionSize() {
        throw Util.unimplemented();
    }

    public Register callerSaveFpuRegAt(int i) {
        throw Util.unimplemented();
    }

    public int sunMiscAtomicLongCSImplValueOffset() {
        throw Util.unimplemented();
    }

    public int arrayElementSize(BasicType type) {
        throw Util.unimplemented(); // TODO: move usages to BasicType.elementSize
    }

    public int arrayOopDescHeaderSize(BasicType type) {
        throw Util.unimplemented();
    }

    public void vmExitOutOfMemory1(int i, String string, String name) {
        throw Util.unimplemented();
    }

    public int vmPageSize() {
        throw Util.unimplemented();
    }

    public int argRegSaveAreaBytes() {
        throw Util.unimplemented();
    }

    public int basicLockDisplacedHeaderOffsetInBytes() {
        throw Util.unimplemented();
    }

    public long basicObjectLockOffsetInBytes() {
        throw Util.unimplemented();
    }

    public long basicObjectLockSize() {
        throw Util.unimplemented();
    }

    public long basicObjectObjOffsetInBytes() {
        throw Util.unimplemented();
    }

    public long doubleSignflipPoolAddress() {
        throw Util.unimplemented();
    }

    public long doubleSignmaskPoolAddress() {
        throw Util.unimplemented();
    }

    public int elementKlassOffsetInBytes() {
        throw Util.unimplemented();
    }

    public long floatSignflipPoolAddress() {
        throw Util.unimplemented();
    }

    public long getPollingPage() {
        throw Util.unimplemented();
    }

    public int getSerializePageShiftCount() {
        throw Util.unimplemented();
    }

    public int initStateOffsetInBytes() {
        throw Util.unimplemented();
    }

    public int instanceKlassFullyInitialized() {
        throw Util.unimplemented();
    }

    public int interpreterFrameMonitorSize() {
        throw Util.unimplemented();
    }

    public Register javaCallingConventionReceiverRegister() {
        return X86Register.rax;
    }

    public int markOffsetInBytes() {
        throw Util.unimplemented();
    }

    public int methodDataNullSeenByteConstant() {
        throw Util.unimplemented();
    }

    public int nativeCallDisplacementOffset() {
        throw Util.unimplemented();
    }

    public int nativeMovConstRegInstructionSize() {
        throw Util.unimplemented();
    }

    public int secondarySuperCacheOffsetInBytes() {
        throw Util.unimplemented();
    }

    public int secondarySupersOffsetInBytes() {
        throw Util.unimplemented();
    }

    public int superCheckOffsetOffsetInBytes() {
        throw Util.unimplemented();
    }

    public int threadPendingExceptionOffset() {
        throw Util.unimplemented();
    }

    public int threadTlabEndOffset() {
        throw Util.unimplemented();
    }

    public int threadTlabSizeOffset() {
        throw Util.unimplemented();
    }

    public int threadTlabStartOffset() {
        throw Util.unimplemented();
    }

    public int threadTlabTopOffset() {
        throw Util.unimplemented();
    }

    public int threadVmResultOffset() {
        throw Util.unimplemented();
    }

    public Object universeNonOopWord() {
        throw Util.unimplemented();
    }

    public boolean universeSupportsInlineContigAlloc() {
        throw Util.unimplemented();
    }

    public int biasedLockMaskInPlace() {
        throw Util.unimplemented();
    }

    public int biasedLockPattern() {
        throw Util.unimplemented();
    }

    public long biasedLockingFastPathEntryCountAddr() {
        throw Util.unimplemented();
    }

    public boolean dtraceAllocProbes() {
        throw Util.unimplemented();
    }

    public long getMemorySerializePage() {
        throw Util.unimplemented();
    }

    public int getMinObjAlignmentInBytesMask() {
        throw Util.unimplemented();
    }

    public int instanceOopDescBaseOffsetInBytes() {
        throw Util.unimplemented();
    }

    public int itableInterfaceOffsetInBytes() {
        throw Util.unimplemented();
    }

    public int itableMethodEntryMethodOffset() {
        throw Util.unimplemented();
    }

    public int itableOffsetEntrySize() {
        throw Util.unimplemented();
    }

    public int itableOffsetOffsetInBytes() {
        throw Util.unimplemented();
    }

    public int klassPartOffsetInBytes() {
        throw Util.unimplemented();
    }

    public int markOopDescPrototype() {
        throw Util.unimplemented();
    }

    public int maxArrayAllocationLength() {
        throw Util.unimplemented();
    }

    public int prototypeHeaderOffsetInBytes() {
        throw Util.unimplemented();
    }

    public int unlockedValue() {
        throw Util.unimplemented();
    }

    public int vtableLengthOffset() {
        throw Util.unimplemented();
    }

    public int javaCallingConvention(CiMethod method, CiLocation[] result, boolean outgoing) {

        int iGeneral = 0;
        int iXMM = 0;
        final CiSignature signature = method.signatureType();


        final int argumentCount = signature.argumentCount(!method.isStatic());


        for (int i = 0; i < argumentCount; i++) {

            final BasicType kind;
            if (i == 0 && !method.isStatic()) {
                kind = BasicType.Object;
            } else {
                kind = signature.argumentBasicTypeAt(i);
            }

            switch (kind) {
                case Byte:
                case Boolean:
                case Short:
                case Char:
                case Int:
                case Long:
                case Word:
                case Object: {
                    if (iGeneral < X86Register.cpuRegisters.length) {
                        result[i] = new CiLocation(X86Register.cpuRegisters[iGeneral++]);
                    }
                    break;
                }
                case Float:
                case Double: {
                    if (iXMM < X86Register.xmmRegisters.length) {
                        result[i] = new CiLocation(X86Register.xmmRegisters[iXMM++]);
                    }
                    break;
                }
                case Void:
                    result[i] = null;
                    break;

                default:
                    throw Util.shouldNotReachHere();
            }
        }

        for (int i = 0; i < result.length; i++) {
            if (result[i] == null) {
                throw Util.shouldNotReachHere();
                // Currently cannot return stack slot
            }
        }

        // Return preserved stack size
        return 0;
    }

    public int outPreserveStackSlots() {
        // This is probably correct for now.
        return 0;
    }

    @Override
    public int sizeofBasicObjectLock() {
        // TODO Auto-generated method stub
        return 0;
    }

}

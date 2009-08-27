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

import java.io.*;
import java.util.*;

import com.sun.c1x.ci.*;
import com.sun.c1x.target.*;
import com.sun.c1x.target.x86.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;
import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.io.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.layout.Layout.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * The <code>MaxRiRuntime</code> class implements the runtime interface needed by C1X.
 * This includes access to runtime features such as class and method representations,
 * constant pools, as well as some compiler tuning.
 *
 * @author Ben L. Titzer
 */
public class MaxRiRuntime implements RiRuntime {

    private static final Register[] generalParameterRegisters = new Register[]{X86.rdi, X86.rsi, X86.rdx, X86.rcx, X86.r8, X86.r9};
    private static final Register[] xmmParameterRegisters = new Register[]{X86.xmm0, X86.xmm1, X86.xmm2, X86.xmm3, X86.xmm4, X86.xmm5, X86.xmm6, X86.xmm7};

    public static final MaxRiRuntime globalRuntime = new MaxRiRuntime();

    final MaxRiConstantPool globalConstantPool = new MaxRiConstantPool(this, null);

    final HashMap<ConstantPool, MaxRiConstantPool> constantPools = new HashMap<ConstantPool, MaxRiConstantPool>();

    /**
     * Gets the constant pool for a specified method.
     * @param method the compiler interface method
     * @return the compiler interface constant pool for the specified method
     */
    public RiConstantPool getConstantPool(RiMethod method) {
        return getConstantPool(this.asClassMethodActor(method, "getConstantPool()"));
    }

    private MaxRiConstantPool getConstantPool(ClassMethodActor classMethodActor) {
        final ConstantPool cp = classMethodActor.holder().constantPool();
        synchronized (this) {
            MaxRiConstantPool constantPool = constantPools.get(cp);
            if (constantPool == null) {
                constantPool = new MaxRiConstantPool(this, cp);
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
    public RiType resolveType(String name) {
        final ClassActor classActor = ClassRegistry.get((ClassLoader) null, JavaTypeDescriptor.getDescriptorForJavaString(name), false);
        if (classActor != null) {
            return canonicalRiType(classActor, globalConstantPool);
        }
        return null;
    }

    /**
     * Gets the <code>RiMethod</code> for a given method actor.
     * @param methodActor the method actor
     * @return the canonical compiler interface method for the method actor
     */
    public RiMethod getRiMethod(ClassMethodActor methodActor) {
        return canonicalRiMethod(methodActor, getConstantPool(methodActor));
    }

    /**
     * Gets the OSR frame for a particular method at a particular bytecode index.
     * @param method the compiler interface method
     * @param bci the bytecode index
     * @return the OSR frame
     */
    public RiOsrFrame getOsrFrame(RiMethod method, int bci) {
        throw FatalError.unimplemented();
    }

    /**
     * Checks whether the runtime requires inlining of the specified method.
     * @param method the method to inline
     * @return <code>true</code> if the method must be inlined; <code>false</code>
     * to allow the compiler to use its own heuristics
     */
    public boolean mustInline(RiMethod method) {
        return asClassMethodActor(method, "mustInline()").isInline();
    }

    /**
     * Checks whether the runtime forbids inlining of the specified method.
     * @param method the method to inline
     * @return <code>true</code> if the runtime forbids inlining of the specified method;
     * <code>false</code> to allow the compiler to use its own heuristics
     */
    public boolean mustNotInline(RiMethod method) {
        final ClassMethodActor classMethodActor = asClassMethodActor(method, "mustNotInline()");
        return classMethodActor.rawCodeAttribute() == null || classMethodActor.isNeverInline();
    }

    /**
     * Checks whether the runtime forbids compilation of the specified method.
     * @param method the method to compile
     * @return <code>true</code> if the runtime forbids compilation of the specified method;
     * <code>false</code> to allow the compiler to compile the method
     */
    public boolean mustNotCompile(RiMethod method) {
        return false;
    }

    public Register getCRarg(int i) {
        // TODO: move this out of the compiler interface
        switch(i) {
            case 0:
                return X86.rdi;
            case 1:
                return X86.rsi;
            case 2:
                return X86.rdx;
            case 3:
                return X86.rcx;
            case 4:
                return X86.r8;
            case 5:
                return X86.r9;
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

    ClassMethodActor asClassMethodActor(RiMethod method, String operation) {
        if (method instanceof MaxRiMethod) {
            return ((MaxRiMethod) method).asClassMethodActor(operation);
        }
        throw new MaxRiUnresolved("invalid RiMethod instance: " + method.getClass());
    }

    ClassActor asClassActor(RiType type, String operation) {
        if (type instanceof MaxRiType) {
            return ((MaxRiType) type).asClassActor(operation);
        }
        throw new MaxRiUnresolved("invalid RiType instance: " + type.getClass());
    }

    public int arrayLengthOffsetInBytes() {
        return VMConfiguration.target().layoutScheme().arrayHeaderLayout.arrayLengthOffset();
    }

    public boolean dtraceMethodProbes() {
        // TODO: currently save to return false
        return false;
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
        // TODO: Check what to return here
        return false;
    }

    public int klassJavaMirrorOffsetInBytes() {
        throw Util.unimplemented();
    }

    public int hubOffsetInBytes() {
        return VMConfiguration.target().layoutScheme().generalLayout.getOffsetFromOrigin(HeaderField.HUB).toInt();
    }

    public int overflowArgumentsSize(BasicType basicType) {
        // TODO: Return wordSize
        // Currently must be a constant!!
        return 8;
    }

    public boolean needsExplicitNullCheck(int offset) {
        // TODO: Return false if implicit null check is possible for this offset!
        return offset > 0xbad;
    }

    public int threadExceptionOopOffset() {
        return VmThreadLocal.EXCEPTION_OBJECT.offset;
    }

    public int threadObjOffset() {
        throw Util.unimplemented();
    }

    public int vtableEntryMethodOffsetInBytes() {
        // TODO: (tw) check if 0 is correct (probably)
        return 0;
    }

    public int vtableEntrySize() {
        // TODO: (tw) modify, return better value
        return 8;
    }

    public int vtableStartOffset() {
        return VMConfiguration.target().layoutScheme().hybridLayout.headerSize();
    }

    public int firstArrayElementOffsetInBytes(BasicType type) {
        return VMConfiguration.target().layoutScheme().arrayHeaderLayout.headerSize();
    }

    public int sunMiscAtomicLongCSImplValueOffset() {
        throw Util.unimplemented();
    }

    public int arrayHeaderSize(BasicType type) {
        throw Util.unimplemented();
    }

    public int basicObjectLockOffsetInBytes() {
        return Util.nonFatalUnimplemented(0);
    }

    public int basicObjectLockSize() {
        return Util.nonFatalUnimplemented(0);
    }

    public int elementKlassOffsetInBytes() {
        return ClassActor.fromJava(Hub.class).findLocalInstanceFieldActor("componentHub").offset();
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
        return X86.rax;
    }

    public int markOffsetInBytes() {
        throw Util.unimplemented();
    }

    public int methodDataNullSeenByteConstant() {
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

    public int biasedLockMaskInPlace() {
        throw Util.unimplemented();
    }

    public int biasedLockPattern() {
        throw Util.unimplemented();
    }

    public boolean dtraceAllocProbes() {
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

    public int vtableLengthOffset() {
        throw Util.unimplemented();
    }

    public int runtimeCallingConvention(BasicType[] signature, CiLocation[] regs) {
        return javaCallingConvention(signature, regs, true);
    }

    public int javaCallingConvention(BasicType[] types, CiLocation[] result, boolean outgoing) {

        assert result.length == types.length;

        int currentGeneral = 0;
        int currentXMM = 0;
        int currentStackSlot = 1;

        for (int i = 0; i < types.length; i++) {

            final BasicType kind = types[i];

            switch (kind) {
                case Byte:
                case Boolean:
                case Short:
                case Char:
                case Int:
                case Long:
                case Word:
                case Object:
                    if (currentGeneral < generalParameterRegisters.length) {
                        Register register = generalParameterRegisters[currentGeneral++];
                        if (kind == BasicType.Long) {
                            result[i] = new CiLocation(register, register);
                        } else {
                            result[i] = new CiLocation(register);
                        }
                    }
                    break;

                case Float:
                case Double:
                    if (currentXMM < xmmParameterRegisters.length) {
                        Register register = xmmParameterRegisters[currentXMM++];
                        if (kind == BasicType.Float) {
                            result[i] = new CiLocation(register);
                        } else {
                            result[i] = new CiLocation(register, register);
                        }
                    }
                    break;

                default:
                    throw Util.shouldNotReachHere();
            }

            if (result[i] == null) {
                result[i] = new CiLocation(currentStackSlot);
                currentStackSlot++; //+= kind.size;
            }
        }

        return currentStackSlot - 1;
    }

    public CiLocation receiverLocation() {
        return new CiLocation(generalParameterRegisters[0]);
    }

    public int sizeofBasicObjectLock() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int codeOffset() {
        // TODO: get rid of this!
        // Offset because this is optimized code:
        return 8;
    }

    public String disassemble(byte[] code) {
        if (MaxineVM.isPrototyping()) {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            final IndentWriter writer = new IndentWriter(new OutputStreamWriter(byteArrayOutputStream));
            writer.flush();
            final ProcessorKind processorKind = VMConfiguration.target().platform().processorKind;
            final InlineDataDecoder inlineDataDecoder = null; //InlineDataDecoder.createFrom(teleTargetMethod.getEncodedInlineDataDescriptors());
            final Pointer startAddress = Pointer.fromInt(0);
            final DisassemblyPrinter disassemblyPrinter = new DisassemblyPrinter(false) {
                @Override
                protected String disassembledObjectString(Disassembler disassembler, DisassembledObject disassembledObject) {
                    final String string = super.disassembledObjectString(disassembler, disassembledObject);
                    if (string.startsWith("call ")) {
                        final BytecodeLocation bytecodeLocation = null; //_teleTargetMethod.getBytecodeLocationFor(startAddress.plus(disassembledObject.startPosition()));
                        if (bytecodeLocation != null) {
                            final MethodRefConstant methodRef = bytecodeLocation.getCalleeMethodRef();
                            if (methodRef != null) {
                                final ConstantPool pool = bytecodeLocation.classMethodActor().codeAttribute().constantPool();
                                return string + " [" + methodRef.holder(pool).toJavaString(false) + "." + methodRef.name(pool) + methodRef.signature(pool).toJavaString(false, false) + "]";
                            }
                        }
                    }
                    return string;
                }
            };
            Disassemble.disassemble(byteArrayOutputStream, code, processorKind, startAddress, inlineDataDecoder, disassemblyPrinter);
            return byteArrayOutputStream.toString();
        }
        return "";
    }

    public Register returnRegister(BasicType object) {

        if (object == BasicType.Void) {
            return Register.noreg;
        }

        if (object == BasicType.Float || object == BasicType.Double) {
            return X86.xmm0;
        }
        return X86.rax;
    }

    int memberIndex;

    public Object registerTargetMethod(CiTargetMethod ciTargetMethod, String name) {
        return new C1XTargetMethodGenerator(new C1XCompilerScheme(VMConfiguration.target()), null, name, ciTargetMethod).finish();
    }

    public RiType primitiveArrayType(BasicType elemType) {
        return canonicalRiType(ClassActor.fromJava(elemType.primitiveArrayClass()), globalConstantPool);
    }

    public Register threadRegister() {
        return X86.r14;
    }

    public int getJITStackSlotSize() {
        return JitStackFrameLayout.JIT_SLOT_SIZE;
    }

    /**
     * Canonicalizes resolved <code>MaxRiMethod</code> instances (per runtime), so
     * that the same <code>MaxRiMethod</code> instance is always returned for the
     * same <code>MethodActor</code>.
     * @param methodActor the mehtod actor for which to get the canonical type
     * @param maxRiConstantPool
     * @return the canonical compiler interface method for the method actor
     */
    public MaxRiMethod canonicalRiMethod(MethodActor methodActor, MaxRiConstantPool maxRiConstantPool) {
//        synchronized (runtime) {
        // all resolved methods are canonicalized per runtime instance
        final MaxRiMethod previous = (MaxRiMethod) methodActor.ciObject;
        if (previous == null) {
            final MaxRiMethod method = new MaxRiMethod(maxRiConstantPool, methodActor);
            methodActor.ciObject = method;
            return method;
        }
        return previous;
//        }
    }

    /**
     * Canonicalizes resolved <code>MaxRiFielde</code> instances (per runtime), so
     * that the same <code>MaxRiField</code> instance is always returned for the
     * same <code>FieldActor</code>.
     * @param fieldActor the field actor for which to get the canonical type
     * @param maxRiConstantPool
     * @return the canonical compiler interface field for the field actor
     */
    public MaxRiField canonicalRiField(FieldActor fieldActor, MaxRiConstantPool maxRiConstantPool) {
//        synchronized (runtime) {
        // all resolved field are canonicalized per runtime instance
        final MaxRiField previous = (MaxRiField) fieldActor.ciObject;
        if (previous == null) {
            final MaxRiField field = new MaxRiField(maxRiConstantPool, fieldActor);
            fieldActor.ciObject = field;
            return field;
        }
        return previous;
//        }
    }

    /**
     * Canonicalizes resolved <code>MaxRiType</code> instances (per runtime), so
     * that the same <code>MaxRiType</code> instance is always returned for the
     * same <code>ClassActor</code>.
     * @param classActor the class actor for which to get the canonical type
     * @param maxRiConstantPool
     * @return the canonical compiler interface type for the class actor
     */
    public MaxRiType canonicalRiType(ClassActor classActor, MaxRiConstantPool maxRiConstantPool) {
//        synchronized (runtime) {
        // all resolved types are canonicalized per runtime instance
        final MaxRiType previous = (MaxRiType) classActor.ciObject;
        if (previous == null) {
            final MaxRiType type = new MaxRiType(maxRiConstantPool, classActor);
            classActor.ciObject = type;
            return type;
        }
        return previous;
//        }
    }
}

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

import com.sun.c1x.target.amd64.*;
import com.sun.c1x.util.*;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiTargetMethod.*;
import com.sun.cri.ci.CiTargetMethod.Safepoint;
import com.sun.cri.ri.*;
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
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * The {@code MaxRiRuntime} class implements the runtime interface needed by C1X.
 * This includes access to runtime features such as class and method representations,
 * constant pools, as well as some compiler tuning.
 *
 * @author Ben L. Titzer
 */
public class MaxRiRuntime implements RiRuntime {

    private final C1XCompilerScheme compilerScheme;
    private RiSnippets snippets;

    public MaxRiRuntime(C1XCompilerScheme compilerScheme) {
        this.compilerScheme = compilerScheme;
    }

    private static final CiRegister[] generalParameterRegisters = new CiRegister[]{AMD64.rdi, AMD64.rsi, AMD64.rdx, AMD64.rcx, AMD64.r8, AMD64.r9};
    private static final CiRegister[] xmmParameterRegisters = new CiRegister[]{AMD64.xmm0, AMD64.xmm1, AMD64.xmm2, AMD64.xmm3, AMD64.xmm4, AMD64.xmm5, AMD64.xmm6, AMD64.xmm7};

    final MaxRiConstantPool globalConstantPool = new MaxRiConstantPool(this, null);

    final HashMap<ConstantPool, MaxRiConstantPool> constantPools = new HashMap<ConstantPool, MaxRiConstantPool>();

    /**
     * Gets the constant pool for a specified method.
     * @param method the compiler interface method
     * @return the compiler interface constant pool for the specified method
     */
    public RiConstantPool getConstantPool(RiMethod method) {
        return getConstantPool(asClassMethodActor(method, "getConstantPool()"));
    }

    private MaxRiConstantPool getConstantPool(ClassMethodActor classMethodActor) {
        final ConstantPool cp = classMethodActor.compilee().codeAttribute().constantPool;
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
            return canonicalRiType(classActor, globalConstantPool, -1);
        }
        return null;
    }

    /**
     * Gets the {@code RiMethod} for a given method actor.
     * @param methodActor the method actor
     * @return the canonical compiler interface method for the method actor
     */
    public RiMethod getRiMethod(ClassMethodActor methodActor) {
        return canonicalRiMethod(methodActor, getConstantPool(methodActor), -1);
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
     * Remove once C1X can compile native method stubs.
     */
    public static final boolean CAN_COMPILE_NATIVE_METHODS = System.getProperty("c1x.can_compile_native") != null;

    /**
     * Remove once C1X implements the semantics of the ACCESSOR annotation.
     */
    private static final boolean CAN_COMPILE_ACCESSOR_METHODS = false;

    /**
     * Checks whether the runtime requires inlining of the specified method.
     * @param method the method to inline
     * @return {@code true} if the method must be inlined; {@code false}
     * to allow the compiler to use its own heuristics
     */
    public boolean mustInline(RiMethod method) {
        if (!method.isResolved()) {
            return false;
        }
        final ClassMethodActor classMethodActor = asClassMethodActor(method, "mustNotInline()");
        if (classMethodActor.accessor() != null && !CAN_COMPILE_ACCESSOR_METHODS) {
            return false;
        }
        if (classMethodActor.isNative() && !CAN_COMPILE_NATIVE_METHODS) {
            return false;
        }
        return classMethodActor.isInline();
    }

    /**
     * Checks whether the runtime forbids inlining of the specified method.
     * @param method the method to inline
     * @return {@code true} if the runtime forbids inlining of the specified method;
     * {@code false} to allow the compiler to use its own heuristics
     */
    public boolean mustNotInline(RiMethod method) {
        if (!method.isResolved()) {
            return false;
        }
        final ClassMethodActor classMethodActor = asClassMethodActor(method, "mustNotInline()");
        if (classMethodActor.accessor() != null && !CAN_COMPILE_ACCESSOR_METHODS) {
            return true;
        }
        if (classMethodActor.isNative() && !CAN_COMPILE_NATIVE_METHODS) {
            return true;
        }

        return classMethodActor.originalCodeAttribute() == null || classMethodActor.isNeverInline();
    }

    /**
     * Checks whether the runtime forbids compilation of the specified method.
     * @param method the method to compile
     * @return {@code true} if the runtime forbids compilation of the specified method;
     * {@code false} to allow the compiler to compile the method
     */
    public boolean mustNotCompile(RiMethod method) {
        return false;
    }

    ClassMethodActor asClassMethodActor(RiMethod method, String operation) {
        if (method instanceof MaxRiMethod) {
            return ((MaxRiMethod) method).asClassMethodActor(operation);
        }
        throw new MaxRiUnresolved("invalid RiMethod instance: " + method.getClass());
    }

    public boolean isMP() {
        return true;
    }

    public boolean jvmtiCanPostExceptions() {
        // TODO: Check what to return here
        return false;
    }

    public boolean needsExplicitNullCheck(int offset) {
        // TODO: Return false if implicit null check is possible for this offset!
        return offset >= 4096;
    }

    public int threadExceptionOffset() {
        return VmThreadLocal.EXCEPTION_OBJECT.offset;
    }

    public int basicObjectLockOffsetInBytes() {
        return Util.nonFatalUnimplemented(0);
    }

    public int sizeofBasicObjectLock() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int codeOffset() {
        return CallEntryPoint.OPTIMIZED_ENTRY_POINT.offset();
    }

    @Override
    public void codePrologue(RiMethod method, OutputStream out) {
        ClassMethodActor callee = asClassMethodActor(method, "codePrologue()");
        AdapterGenerator generator = AdapterGenerator.forCallee(callee, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        if (generator != null) {
            generator.adapt(callee, out);
        }
    }

    @Override
    public String disassemble(RiMethod method) {
        ClassMethodActor classMethodActor = asClassMethodActor(method, "disassemble()");
        return classMethodActor.format("%f %R %H.%n(%P)") + String.format("%n%s", CodeAttributePrinter.toString(classMethodActor.codeAttribute()));
    }

    public String disassemble(byte[] code) {
        if (MaxineVM.isHosted()) {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            final IndentWriter writer = new IndentWriter(new OutputStreamWriter(byteArrayOutputStream));
            writer.flush();
            final ProcessorKind processorKind = VMConfiguration.target().platform().processorKind;
            final InlineDataDecoder inlineDataDecoder = null;
            final Pointer startAddress = Pointer.fromInt(0);
            final DisassemblyPrinter disassemblyPrinter = new DisassemblyPrinter(false);
            Disassemble.disassemble(byteArrayOutputStream, code, processorKind, startAddress, inlineDataDecoder, disassemblyPrinter);
            return byteArrayOutputStream.toString();
        }
        return "";
    }

    @Override
    public String disassemble(final CiTargetMethod targetMethod) {
        if (MaxineVM.isHosted()) {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            final IndentWriter writer = new IndentWriter(new OutputStreamWriter(byteArrayOutputStream));
            writer.flush();
            final ProcessorKind processorKind = VMConfiguration.target().platform().processorKind;
            final InlineDataDecoder inlineDataDecoder = null;
            final Pointer startAddress = Pointer.fromInt(0);
            final DisassemblyPrinter disassemblyPrinter = new DisassemblyPrinter(false) {
                private String toString(Call call) {
                    if (call.runtimeCall != null) {
                        return "{" + call.runtimeCall.name() + "}";
                    } else if (call.symbol != null) {
                        return "{" + call.symbol + "}";
                    } else if (call.globalStubID != null) {
                        return "{" + call.globalStubID + "}";
                    } else {
                        return "{" + call.method + "}";
                    }
                }
                private String siteInfo(int pcOffset) {
                    for (Call call : targetMethod.directCalls) {
                        if (call.pcOffset == pcOffset) {
                            return toString(call);
                        }
                    }
                    for (Call call : targetMethod.indirectCalls) {
                        if (call.pcOffset == pcOffset) {
                            return toString(call);
                        }
                    }
                    for (Safepoint site : targetMethod.safepoints) {
                        if (site.pcOffset == pcOffset) {
                            return "{safepoint}";
                        }
                    }
                    for (DataPatch site : targetMethod.dataReferences) {
                        if (site.pcOffset == pcOffset) {
                            return "{" + site.data + "}";
                        }
                    }
                    return null;
                }

                @Override
                protected String disassembledObjectString(Disassembler disassembler, DisassembledObject disassembledObject) {
                    final String string = super.disassembledObjectString(disassembler, disassembledObject);

                    String site = siteInfo(disassembledObject.startPosition());
                    if (site != null) {
                        return string + " " + site;
                    }
                    return string;
                }
            };
            byte[] code = Arrays.copyOf(targetMethod.targetCode(), targetMethod.targetCodeSize());
            Disassemble.disassemble(byteArrayOutputStream, code, processorKind, startAddress, inlineDataDecoder, disassemblyPrinter);
            return byteArrayOutputStream.toString();
        }
        return "";
    }

    public Object registerTargetMethod(CiTargetMethod ciTargetMethod, String name) {
        return new C1XTargetMethod(name, ciTargetMethod);
    }

    /**
     * Canonicalizes resolved {@code MaxRiMethod} instances (per runtime), so
     * that the same {@code MaxRiMethod} instance is always returned for the
     * same {@code MethodActor}.
     * @param methodActor the method actor for which to get the canonical type
     * @param maxRiConstantPool the constant pool
     * @param cpi the constant pool index
     * @return the canonical compiler interface method for the method actor
     */
    public MaxRiMethod canonicalRiMethod(MethodActor methodActor, MaxRiConstantPool maxRiConstantPool, int cpi) {
        // TODO: is synchronization necessary here or are duplicates harmless?

        // all resolved methods are canonicalized per runtime instance
        final MaxRiMethod previous = (MaxRiMethod) methodActor.ciObject;
        if (previous == null) {
            final MaxRiMethod method = new MaxRiMethod(maxRiConstantPool, methodActor, cpi);
            methodActor.ciObject = method;
            return method;
        }
        return previous;
    }

    /**
     * Canonicalizes resolved {@code MaxRiFielde} instances (per runtime), so
     * that the same {@code MaxRiField} instance is always returned for the
     * same {@code FieldActor}.
     * @param fieldActor the field actor for which to get the canonical type
     * @param maxRiConstantPool the constant pool
     * @param cpi the constant pool index
     * @return the canonical compiler interface field for the field actor
     */
    public MaxRiField canonicalRiField(FieldActor fieldActor, MaxRiConstantPool maxRiConstantPool, int cpi) {
        // TODO: is synchronization necessary here or are duplicates harmless?

        // all resolved fields are canonicalized per runtime instance
        final MaxRiField previous = (MaxRiField) fieldActor.ciObject;
        if (previous == null) {
            final MaxRiField field = new MaxRiField(maxRiConstantPool, fieldActor, cpi);
            fieldActor.ciObject = field;
            return field;
        }
        return previous;
    }

    /**
     * Canonicalizes resolved {@code MaxRiType} instances (per runtime), so
     * that the same {@code MaxRiType} instance is always returned for the
     * same {@code ClassActor}.
     * @param classActor the class actor for which to get the canonical type
     * @param maxRiConstantPool
     * @return the canonical compiler interface type for the class actor
     */
    public MaxRiType canonicalRiType(ClassActor classActor, MaxRiConstantPool maxRiConstantPool, int cpi) {
        // TODO: is synchronization necessary here or are duplicates harmless?

        // all resolved types are canonicalized per runtime instance
        final MaxRiType previous = (MaxRiType) classActor.ciObject;
        if (previous == null) {
            final MaxRiType type = new MaxRiType(maxRiConstantPool, classActor, cpi);
            classActor.ciObject = type;
            return type;
        }
        return previous;
    }

    public RiType getRiType(Class<?> javaClass) {
        // TODO: using target is probably necessary here
        return canonicalRiType(ClassActor.fromJava(javaClass), globalConstantPool, -1);
    }

    public boolean isObjectArrayType(RiType type) {
        if (type.isResolved()) {
            ClassActor c = ((MaxRiType) type).asClassActor("equals Object[]");
            return c.isArrayClassActor() && c == ClassActor.fromJava(Object[].class);
        }
        return false;
    }

    @Override
    public RiSnippets getSnippets() {
        if (snippets == null) {
            snippets = new MaxRiSnippets(this);
        }
        return snippets;
    }
}

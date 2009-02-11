/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.template.source;

import static com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveStaticMethod.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.instrument.*;
import com.sun.max.vm.hotpath.*;
import com.sun.max.vm.hotpath.compiler.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.template.TemplateChooser.*;
import com.sun.max.vm.type.*;

@TEMPLATE(traced = Traced.YES)
public class TracedBytecodeTemplateSource {

    /**
     * Every trace instrumented bytecode template makes a call to the {@code Hotpath.trace()} method in its prolog. This
     * allows the {@link Tracer} to record traces as well as direct the execution flow of the JITed code. If the {@link Tracer}
     * wants to stop recording, it can return the {@link Address} of a bytecode in the non traced version of the JITed code.
     */
    @INLINE
    public static void trace() {
        final Address resumeAddress = Hotpath.trace(VMRegister.getInstructionPointer(), VMRegister.getCpuStackPointer());
        if (resumeAddress.isZero() == false) {
            SpecialBuiltin.jump(resumeAddress);
        }
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.NOP, instrumented = Instrumented.YES, traced = Traced.YES)
    public static void branchInstrumented(TreeAnchor counter) {
        final Address resumeAddress = counter.visit();
        if (resumeAddress.isZero() == false) {
            SpecialBuiltin.jump(resumeAddress);
        }
    }

    /*
    @BYTECODE_TEMPLATE(bytecode = Bytecode.NOP, instrumented = Instrumented.YES, traced = Traced.YES)
    public static void nopInstrumented(Counter counter) {
        InstrumentedBytecodeSource.nop(counter);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.VOID, resolved = Resolved.YES, traced = Traced.YES)
    public static void invokevirtual(int vTableIndex, int receiverStackIndex) {
        trace();
        ResolvedInvokeTemplateSource.invokevirtual(vTableIndex, receiverStackIndex);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.FLOAT, resolved = Resolved.YES, traced = Traced.YES)
    public static void invokevirtualReturnFloat(int vTableIndex, int receiverStackIndex) {
        trace();
        ResolvedInvokeTemplateSource.invokevirtualReturnFloat(vTableIndex, receiverStackIndex);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.LONG, resolved = Resolved.YES, traced = Traced.YES)
    public static void invokevirtualLong(int vTableIndex, int receiverStackIndex) {
        trace();
        ResolvedInvokeTemplateSource.invokevirtualLong(vTableIndex, receiverStackIndex);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.DOUBLE, resolved = Resolved.YES, traced = Traced.YES)
    public static void invokevirtualDouble(int vTableIndex, int receiverStackIndex) {
        trace();
        ResolvedInvokeTemplateSource.invokevirtualDouble(vTableIndex, receiverStackIndex);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.WORD, resolved = Resolved.YES, traced = Traced.YES)
    public static void invokevirtualWord(int vTableIndex, int receiverStackIndex) {
        trace();
        ResolvedInvokeTemplateSource.invokevirtualWord(vTableIndex, receiverStackIndex);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.VOID, resolved = Resolved.YES, traced = Traced.YES)
    public static void invokeinterface(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        trace();
        ResolvedInvokeTemplateSource.invokeinterface(interfaceMethodActor, receiverStackIndex);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.FLOAT, resolved = Resolved.YES, traced = Traced.YES)
    public static void invokeinterfaceFloat(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        trace();
        ResolvedInvokeTemplateSource.invokeinterfaceFloat(interfaceMethodActor, receiverStackIndex);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.LONG, resolved = Resolved.YES, traced = Traced.YES)
    public static void invokeinterfaceLong(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        trace();
        ResolvedInvokeTemplateSource.invokeinterfaceLong(interfaceMethodActor, receiverStackIndex);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.DOUBLE, resolved = Resolved.YES, traced = Traced.YES)
    public static void invokeinterfaceDouble(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        trace();
        ResolvedInvokeTemplateSource.invokeinterfaceDouble(interfaceMethodActor, receiverStackIndex);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.WORD, resolved = Resolved.YES, traced = Traced.YES)
    public static void invokeinterfaceWord(InterfaceMethodActor interfaceMethodActor, int receiverStackIndex) {
        trace();
        ResolvedInvokeTemplateSource.invokeinterfaceWord(interfaceMethodActor, receiverStackIndex);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESPECIAL, kind = KindEnum.VOID, resolved = Resolved.YES, traced = Traced.YES)
    public static void invokespecial() {
        trace();
        ResolvedInvokeTemplateSource.invokespecial();
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESPECIAL, kind = KindEnum.FLOAT, resolved = Resolved.YES, traced = Traced.YES)
    public static void invokespecialReturnSingleSlot() {
        trace();
        ResolvedInvokeTemplateSource.invokespecialReturnSingleSlot();
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESPECIAL, kind = KindEnum.LONG, resolved = Resolved.YES, traced = Traced.YES)
    public static void invokespecialLong() {
        trace();
        ResolvedInvokeTemplateSource.invokespecialLong();
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESPECIAL, kind = KindEnum.DOUBLE, resolved = Resolved.YES, traced = Traced.YES)
    public static void invokespecialDouble() {
        trace();
        ResolvedInvokeTemplateSource.invokespecialDouble();
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESPECIAL, kind = KindEnum.WORD, resolved = Resolved.YES, traced = Traced.YES)
    public static void invokespecialWord() {
        trace();
        ResolvedInvokeTemplateSource.invokespecialWord();
    }
    */

    //
    // Unoptimized Tracing Bytecode Templates
    //

    public static void aconst_null() {
        trace();
        UnoptimizedBytecodeTemplateSource.aconst_null();
    }

    public static void aaload() {
        trace();
        UnoptimizedBytecodeTemplateSource.aaload();
    }

    public static void aastore() {
        trace();
        UnoptimizedBytecodeTemplateSource.aastore();
    }

    public static void aload(int dispToLocalSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.aload(dispToLocalSlot);
    }

    public static void aload_0(int dispToLocalSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.aload_0(dispToLocalSlot);
    }

    public static void aload_1(int dispToLocalSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.aload_1(dispToLocalSlot);
    }

    public static void aload_2(int dispToLocalSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.aload_2(dispToLocalSlot);
    }

    public static void aload_3(int dispToLocalSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.aload_3(dispToLocalSlot);
    }

    public static void anewarray(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.anewarray(guard);
    }

    public static Object areturn() {
        trace();
        return UnoptimizedBytecodeTemplateSource.areturn();
    }

    public static void arraylength() {
        trace();
        UnoptimizedBytecodeTemplateSource.arraylength();
    }

    public static void astore(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.astore(displacementToSlot);
    }

    public static void astore_0(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.astore_0(displacementToSlot);
    }

    public static void astore_1(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.astore_1(displacementToSlot);
    }

    public static void astore_2(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.astore_2(displacementToSlot);
    }

    public static void astore_3(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.astore_3(displacementToSlot);
    }

    public static void athrow() {
        trace();
        UnoptimizedBytecodeTemplateSource.athrow();
    }

    public static void baload() {
        trace();
        UnoptimizedBytecodeTemplateSource.baload();
    }

    public static void bastore() {
        trace();
        UnoptimizedBytecodeTemplateSource.bastore();
    }

    public static void bipush(byte value) {
        trace();
        UnoptimizedBytecodeTemplateSource.bipush(value);
    }

    public static void caload() {
        trace();
        UnoptimizedBytecodeTemplateSource.caload();
    }

    public static void castore() {
        trace();
        UnoptimizedBytecodeTemplateSource.castore();
    }

    public static void checkcast(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.checkcast(guard);
    }

    public static void d2f() {
        trace();
        UnoptimizedBytecodeTemplateSource.d2f();
    }

    public static void d2i() {
        trace();
        UnoptimizedBytecodeTemplateSource.d2i();
    }

    public static void d2l() {
        trace();
        UnoptimizedBytecodeTemplateSource.d2l();
    }

    public static void dadd() {
        trace();
        UnoptimizedBytecodeTemplateSource.dadd();
    }

    public static void daload() {
        trace();
        UnoptimizedBytecodeTemplateSource.daload();
    }

    public static void dastore() {
        trace();
        UnoptimizedBytecodeTemplateSource.dastore();
    }

    public static void dcmpg() {
        trace();
        UnoptimizedBytecodeTemplateSource.dcmpg();
    }

    public static void dcmpl() {
        trace();
        UnoptimizedBytecodeTemplateSource.dcmpl();
    }

    public static void dconst_0(double zero) {
        trace();
        UnoptimizedBytecodeTemplateSource.dconst_0(zero);
    }

    public static void dconst_1(double one) {
        trace();
        UnoptimizedBytecodeTemplateSource.dconst_1(one);
    }

    public static void ddiv() {
        trace();
        UnoptimizedBytecodeTemplateSource.ddiv();
    }

    public static void dload(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.dload(displacementToSlot);
    }

    public static void dload_0(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.dload_0(displacementToSlot);
    }

    public static void dload_1(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.dload_1(displacementToSlot);
    }

    public static void dload_2(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.dload_2(displacementToSlot);
    }

    public static void dload_3(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.dload_3(displacementToSlot);
    }

    public static void dmul() {
        trace();
        UnoptimizedBytecodeTemplateSource.dmul();
    }

    public static void dneg(double zero) {
        trace();
        UnoptimizedBytecodeTemplateSource.dneg(zero);
    }

    public static void drem() {
        trace();
        UnoptimizedBytecodeTemplateSource.drem();
    }

    public static double dreturn() {
        trace();
        return UnoptimizedBytecodeTemplateSource.dreturn();
    }

    public static void dstore(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.dstore(displacementToSlot);
    }

    public static void dstore_0(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.dstore_0(displacementToSlot);
    }

    public static void dstore_1(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.dstore_1(displacementToSlot);
    }

    public static void dstore_2(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.dstore_2(displacementToSlot);
    }

    public static void dstore_3(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.dstore_3(displacementToSlot);
    }

    public static void dsub() {
        trace();
        UnoptimizedBytecodeTemplateSource.dsub();
    }

    public static void dup() {
        trace();
        UnoptimizedBytecodeTemplateSource.dup();
    }

    public static void dup_x1() {
        trace();
        UnoptimizedBytecodeTemplateSource.dup_x1();
    }

    public static void dup_x2() {
        trace();
        UnoptimizedBytecodeTemplateSource.dup_x2();
    }

    public static void dup2() {
        trace();
        UnoptimizedBytecodeTemplateSource.dup2();
    }

    public static void dup2_x1() {
        trace();
        UnoptimizedBytecodeTemplateSource.dup2_x1();
    }

    public static void dup2_x2() {
        trace();
        UnoptimizedBytecodeTemplateSource.dup2_x2();
    }

    public static void f2d() {
        trace();
        UnoptimizedBytecodeTemplateSource.f2d();
    }

    public static void f2i() {
        trace();
        UnoptimizedBytecodeTemplateSource.f2i();
    }

    public static void f2l() {
        trace();
        UnoptimizedBytecodeTemplateSource.f2l();
    }

    public static void fadd() {
        trace();
        UnoptimizedBytecodeTemplateSource.fadd();
    }

    public static void faload() {
        trace();
        UnoptimizedBytecodeTemplateSource.faload();
    }

    public static void fastore() {
        trace();
        UnoptimizedBytecodeTemplateSource.fastore();
    }

    public static void fcmpg() {
        trace();
        UnoptimizedBytecodeTemplateSource.fcmpg();
    }

    public static void fcmpl() {
        trace();
        UnoptimizedBytecodeTemplateSource.fcmpl();
    }

    public static void fconst_0(float zero) {
        trace();
        UnoptimizedBytecodeTemplateSource.fconst_0(zero);
    }

    public static void fconst_1(float one) {
        trace();
        UnoptimizedBytecodeTemplateSource.fconst_1(one);
    }

    public static void fconst_2(float two) {
        trace();
        UnoptimizedBytecodeTemplateSource.fconst_2(two);
    }

    public static void fdiv() {
        trace();
        UnoptimizedBytecodeTemplateSource.fdiv();
    }

    public static void fload(int dispToLocalSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.fload(dispToLocalSlot);
    }

    public static void fload_0(int dispToLocalSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.fload_0(dispToLocalSlot);
    }

    public static void fload_1(int dispToLocalSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.fload_1(dispToLocalSlot);
    }

    public static void fload_2(int dispToLocalSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.fload_2(dispToLocalSlot);
    }

    public static void fload_3(int dispToLocalSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.fload_3(dispToLocalSlot);
    }

    public static void fmul() {
        trace();
        UnoptimizedBytecodeTemplateSource.fmul();
    }

    public static void fneg(float zero) {
        UnoptimizedBytecodeTemplateSource.fneg(zero);
    }

    public static void frem() {
        trace();
        UnoptimizedBytecodeTemplateSource.frem();
    }

    public static float freturn() {
        trace();
        return UnoptimizedBytecodeTemplateSource.freturn();
    }

    public static void fstore(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.fstore(displacementToSlot);
    }

    public static void fstore_0(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.fstore_0(displacementToSlot);
    }

    public static void fstore_1(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.fstore_1(displacementToSlot);
    }

    public static void fstore_2(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.fstore_2(displacementToSlot);
    }

    public static void fstore_3(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.fstore_3(displacementToSlot);
    }

    public static void fsub() {
        trace();
        UnoptimizedBytecodeTemplateSource.fsub();
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.REFERENCE, traced = Traced.YES)
    public static void rgetfield(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.rgetfield(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.WORD, traced = Traced.YES)
    public static void wgetfield(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.wgetfield(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.BYTE, traced = Traced.YES)
    public static void bgetfield(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.bgetfield(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.CHAR, traced = Traced.YES)
    public static void cgetfield(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.cgetfield(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.DOUBLE, traced = Traced.YES)
    public static void dgetfield(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.dgetfield(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.FLOAT, traced = Traced.YES)
    public static void fgetfield(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.fgetfield(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.INT, traced = Traced.YES)
    public static void igetfield(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.igetfield(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.LONG, traced = Traced.YES)
    public static void jgetfield(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.jgetfield(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.SHORT, traced = Traced.YES)
    public static void sgetfield(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.sgetfield(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETFIELD, kind = KindEnum.BOOLEAN, traced = Traced.YES)
    public static void zgetfield(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.zgetfield(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.REFERENCE, traced = Traced.YES)
    public static void rputfield(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.rputfield(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.WORD, traced = Traced.YES)
    public static void wputfield(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.wputfield(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.BYTE, traced = Traced.YES)
    public static void bputfield(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.bputfield(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.CHAR, traced = Traced.YES)
    public static void cputfield(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.cputfield(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.DOUBLE, traced = Traced.YES)
    public static void dputfield(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.dputfield(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.FLOAT, traced = Traced.YES)
    public static void fputfield(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.fputfield(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.INT, traced = Traced.YES)
    public static void iputfield(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.iputfield(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.LONG, traced = Traced.YES)
    public static void jputfield(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.jputfield(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.SHORT, traced = Traced.YES)
    public static void sputfield(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.sputfield(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTFIELD, kind = KindEnum.BOOLEAN, traced = Traced.YES)
    public static void zputfield(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.zputfield(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.BYTE, traced = Traced.YES)
    public static void bgetstatic(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.bgetstatic(guard);

    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.CHAR, traced = Traced.YES)
    public static void cgetstatic(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.cgetstatic(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.DOUBLE, traced = Traced.YES)
    public static void dgetstatic(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.dgetstatic(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.FLOAT, traced = Traced.YES)
    public static void fgetstatic(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.fgetstatic(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.INT, traced = Traced.YES)
    public static void igetstatic(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.igetstatic(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.LONG, traced = Traced.YES)
    public static void jgetstatic(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.jgetstatic(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.REFERENCE, traced = Traced.YES)
    public static void rgetstatic(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.rgetstatic(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.SHORT, traced = Traced.YES)
    public static void sgetstatic(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.sgetstatic(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.GETSTATIC, kind = KindEnum.BOOLEAN, traced = Traced.YES)
    public static void zgetstatic(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.zgetstatic(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.BYTE, traced = Traced.YES)
    public static void bputstatic(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.bputstatic(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.CHAR, traced = Traced.YES)
    public static void cputstatic(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.cputstatic(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.DOUBLE, traced = Traced.YES)
    public static void dputstatic(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.dputstatic(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.FLOAT, traced = Traced.YES)
    public static void fputstatic(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.fputstatic(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.INT, traced = Traced.YES)
    public static void iputstatic(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.iputstatic(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.LONG, traced = Traced.YES)
    public static void jputstatic(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.jputstatic(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.REFERENCE, traced = Traced.YES)
    public static void rputstatic(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.rputstatic(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.SHORT, traced = Traced.YES)
    public static void sputstatic(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.sputstatic(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.PUTSTATIC, kind = KindEnum.BOOLEAN, traced = Traced.YES)
    public static void zputstatic(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.zputstatic(guard);
    }

    public static void i2b() {
        trace();
        UnoptimizedBytecodeTemplateSource.i2b();
    }

    public static void i2c() {
        trace();
        UnoptimizedBytecodeTemplateSource.i2c();
    }

    public static void i2f() {
        trace();
        UnoptimizedBytecodeTemplateSource.i2f();
    }

    public static void i2s() {
        trace();
        UnoptimizedBytecodeTemplateSource.i2s();
    }

    public static void i2l() {
        trace();
        UnoptimizedBytecodeTemplateSource.i2l();
    }

    public static void i2d() {
        trace();
        UnoptimizedBytecodeTemplateSource.i2d();
    }

    public static void iadd() {
        trace();
        UnoptimizedBytecodeTemplateSource.iadd();
    }

    public static void iaload() {
        trace();
        UnoptimizedBytecodeTemplateSource.iaload();
    }

    public static void iand() {
        trace();
        UnoptimizedBytecodeTemplateSource.iand();
    }

    public static void iastore() {
        trace();
        UnoptimizedBytecodeTemplateSource.iastore();
    }

    public static void iconst_m1() {
        trace();
        UnoptimizedBytecodeTemplateSource.iconst_m1();
    }

    public static void iconst_0() {
        trace();
        UnoptimizedBytecodeTemplateSource.iconst_0();
    }

    public static void iconst_1() {
        trace();
        UnoptimizedBytecodeTemplateSource.iconst_1();
    }

    public static void iconst_2() {
        trace();
        UnoptimizedBytecodeTemplateSource.iconst_2();
    }

    public static void iconst_3() {
        trace();
        UnoptimizedBytecodeTemplateSource.iconst_3();
    }

    public static void iconst_4() {
        trace();
        UnoptimizedBytecodeTemplateSource.iconst_4();
    }

    public static void iconst_5() {
        trace();
        UnoptimizedBytecodeTemplateSource.iconst_5();
    }

    public static void idiv() {
        trace();
        UnoptimizedBytecodeTemplateSource.idiv();
    }

    public static void iinc(int dispToLocalSlot, int increment) {
        trace();
        UnoptimizedBytecodeTemplateSource.iinc(dispToLocalSlot, increment);
    }

    public static void iload(int dispToLocalSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.iload(dispToLocalSlot);
    }

    public static void iload_0(int dispToLocalSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.iload_0(dispToLocalSlot);
    }

    public static void iload_1(int dispToLocalSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.iload_1(dispToLocalSlot);
    }

    public static void iload_2(int dispToLocalSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.iload_2(dispToLocalSlot);
    }

    public static void iload_3(int dispToLocalSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.iload_3(dispToLocalSlot);
    }

    public static void imul() {
        trace();
        UnoptimizedBytecodeTemplateSource.imul();
    }

    public static void ineg() {
        trace();
        UnoptimizedBytecodeTemplateSource.ineg();
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INSTANCEOF, traced = Traced.YES)
    public static void instanceof_(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.instanceof_(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.VOID, traced = Traced.YES)
    public static void invokevirtualVoid(ResolutionGuard guard, int receiverStackIndex) {
        trace();
        UnoptimizedBytecodeTemplateSource.invokevirtualVoid(guard, receiverStackIndex);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.FLOAT, traced = Traced.YES)
    public static void invokevirtualFloat(ResolutionGuard guard, int receiverStackIndex) {
        trace();
        UnoptimizedBytecodeTemplateSource.invokevirtualFloat(guard, receiverStackIndex);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.LONG, traced = Traced.YES)
    public static void invokevirtualLong(ResolutionGuard guard, int receiverStackIndex) {
        trace();
        UnoptimizedBytecodeTemplateSource.invokevirtualLong(guard, receiverStackIndex);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.DOUBLE, traced = Traced.YES)
    public static void invokevirtualDouble(ResolutionGuard guard, int receiverStackIndex) {
        trace();
        UnoptimizedBytecodeTemplateSource.invokevirtualDouble(guard, receiverStackIndex);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEVIRTUAL, kind = KindEnum.WORD, traced = Traced.YES)
    public static void invokevirtualWord(ResolutionGuard guard, int receiverStackIndex) {
        trace();
        UnoptimizedBytecodeTemplateSource.invokevirtualWord(guard, receiverStackIndex);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.VOID, traced = Traced.YES)
    public static void invokeinterfaceVoid(ResolutionGuard guard, int receiverStackIndex) {
        trace();
        UnoptimizedBytecodeTemplateSource.invokeinterfaceVoid(guard, receiverStackIndex);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.FLOAT, traced = Traced.YES)
    public static void invokeinterfaceFloat(ResolutionGuard guard, int receiverStackIndex) {
        trace();
        UnoptimizedBytecodeTemplateSource.invokeinterfaceFloat(guard, receiverStackIndex);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.LONG, traced = Traced.YES)
    public static void invokeinterfaceLong(ResolutionGuard guard, int receiverStackIndex) {
        trace();
        UnoptimizedBytecodeTemplateSource.invokeinterfaceLong(guard, receiverStackIndex);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.DOUBLE, traced = Traced.YES)
    public static void invokeinterfaceDouble(ResolutionGuard guard, int receiverStackIndex) {
        trace();
        UnoptimizedBytecodeTemplateSource.invokeinterfaceDouble(guard, receiverStackIndex);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKEINTERFACE, kind = KindEnum.WORD, traced = Traced.YES)
    public static void invokeinterfaceWord(ResolutionGuard guard, int receiverStackIndex) {
        trace();
        UnoptimizedBytecodeTemplateSource.invokeinterfaceWord(guard, receiverStackIndex);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESPECIAL, kind = KindEnum.VOID, traced = Traced.YES)
    public static void invokespecialVoid(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.invokespecialVoid(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESPECIAL, kind = KindEnum.FLOAT, traced = Traced.YES)
    public static void invokespecialFloat(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.invokespecialFloat(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESPECIAL, kind = KindEnum.LONG, traced = Traced.YES)
    public static void invokespecialLong(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.invokespecialLong(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESPECIAL, kind = KindEnum.DOUBLE, traced = Traced.YES)
    public static void invokespecialDouble(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.invokespecialDouble(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESPECIAL, kind = KindEnum.WORD, traced = Traced.YES)
    public static void invokespecialWord(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.invokespecialWord(guard);
    }

    @INLINE
    private static Address invokestaticEntrypoint(ResolutionGuard guard) {
        final StaticMethodActor staticMethod = resolveStaticMethod(guard);
        MakeHolderInitialized.makeHolderInitialized(staticMethod);
        return MakeTracedEntrypoint.makeTracedEntrypoint(staticMethod);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESTATIC, kind = KindEnum.VOID, traced = Traced.YES)
    public static void invokestaticVoid(ResolutionGuard guard) {
        trace();
        JitStackFrameOperation.indirectCallVoid(invokestaticEntrypoint(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESTATIC, kind = KindEnum.FLOAT, traced = Traced.YES)
    public static void invokestaticFloat(ResolutionGuard guard) {
        trace();
        JitStackFrameOperation.indirectCallFloat(invokestaticEntrypoint(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESTATIC, kind = KindEnum.LONG, traced = Traced.YES)
    public static void invokestaticLong(ResolutionGuard guard) {
        trace();
        JitStackFrameOperation.indirectCallLong(invokestaticEntrypoint(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESTATIC, kind = KindEnum.DOUBLE, traced = Traced.YES)
    public static void invokestaticDouble(ResolutionGuard guard) {
        trace();
        JitStackFrameOperation.indirectCallDouble(invokestaticEntrypoint(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INVOKESTATIC, kind = KindEnum.WORD, traced = Traced.YES)
    public static void invokestaticWord(ResolutionGuard guard) {
        trace();
        JitStackFrameOperation.indirectCallWord(invokestaticEntrypoint(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    public static void ior() {
        trace();
        UnoptimizedBytecodeTemplateSource.ior();
    }

    public static void irem() {
        trace();
        UnoptimizedBytecodeTemplateSource.irem();
    }

    public static int ireturn() {
        trace();
        return UnoptimizedBytecodeTemplateSource.ireturn();
    }

    public static void ishl() {
        trace();
        UnoptimizedBytecodeTemplateSource.ishl();
    }

    public static void ishr() {
        trace();
        UnoptimizedBytecodeTemplateSource.ishr();
    }

    public static void istore(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.istore(displacementToSlot);
    }

    public static void istore_0(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.istore_0(displacementToSlot);
    }

    public static void istore_1(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.istore_1(displacementToSlot);
    }

    public static void istore_2(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.istore_2(displacementToSlot);
    }

    public static void istore_3(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.istore_3(displacementToSlot);
    }

    public static void isub() {
        trace();
        UnoptimizedBytecodeTemplateSource.isub();
    }

    public static void iushr() {
        trace();
        UnoptimizedBytecodeTemplateSource.iushr();
    }

    public static void ixor() {
        trace();
        UnoptimizedBytecodeTemplateSource.ixor();
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.LDC, kind = KindEnum.INT, traced = Traced.YES)
    public static void ildc(int constant) {
        trace();
        UnoptimizedBytecodeTemplateSource.ildc(constant);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.LDC, kind = KindEnum.FLOAT, traced = Traced.YES)
    public static void fldc(float constant) {
        trace();
        UnoptimizedBytecodeTemplateSource.fldc(constant);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.LDC, kind = KindEnum.REFERENCE, traced = Traced.YES)
    public static void unresolved_class_ldc(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.unresolved_class_ldc(guard);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.LDC, kind = KindEnum.LONG, traced = Traced.YES)
    public static void jldc(long value) {
        trace();
        UnoptimizedBytecodeTemplateSource.jldc(value);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.LDC, kind = KindEnum.DOUBLE, traced = Traced.YES)
    public static void dldc(double value) {
        trace();
        UnoptimizedBytecodeTemplateSource.dldc(value);
    }

    public static void l2d() {
        trace();
        UnoptimizedBytecodeTemplateSource.l2d();
    }

    public static void l2f() {
        trace();
        UnoptimizedBytecodeTemplateSource.l2f();
    }

    public static void l2i() {
        trace();
        UnoptimizedBytecodeTemplateSource.l2i();
    }

    public static void ladd() {
        trace();
        UnoptimizedBytecodeTemplateSource.ladd();
    }

    public static void laload() {
        trace();
        UnoptimizedBytecodeTemplateSource.laload();
    }

    public static void land() {
        trace();
        UnoptimizedBytecodeTemplateSource.land();
    }

    public static void lastore() {
        trace();
        UnoptimizedBytecodeTemplateSource.lastore();
    }

    public static void lcmp() {
        trace();
        UnoptimizedBytecodeTemplateSource.lcmp();
    }

    public static void lconst_0(long zero) {
        trace();
        UnoptimizedBytecodeTemplateSource.lconst_0(zero);
    }

    public static void lconst_1(long one) {
        trace();
        UnoptimizedBytecodeTemplateSource.lconst_1(one);
    }

    @INLINE
    public static void lload(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.lload(displacementToSlot);
    }

    public static void lload_0(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.lload_0(displacementToSlot);
    }

    public static void lload_1(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.lload_1(displacementToSlot);
    }

    public static void lload_2(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.lload_2(displacementToSlot);
    }

    public static void lload_3(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.lload_3(displacementToSlot);
    }

    public static void ldiv() {
        trace();
        UnoptimizedBytecodeTemplateSource.ldiv();
    }

    public static void lmul() {
        trace();
        UnoptimizedBytecodeTemplateSource.lmul();
    }

    public static void lneg() {
        trace();
        UnoptimizedBytecodeTemplateSource.lneg();
    }

    public static void lor() {
        trace();
        UnoptimizedBytecodeTemplateSource.lor();
    }

    public static void lrem() {
        trace();
        UnoptimizedBytecodeTemplateSource.lrem();
    }

    public static long lreturn() {
        trace();
        return UnoptimizedBytecodeTemplateSource.lreturn();
    }

    public static void lshl() {
        trace();
        UnoptimizedBytecodeTemplateSource.lshl();
    }

    public static void lshr() {
        trace();
        UnoptimizedBytecodeTemplateSource.lshr();
    }

    @INLINE
    public static void lstore(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.lstore(displacementToSlot);
    }

    public static void lstore_0(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.lstore_0(displacementToSlot);
    }

    public static void lstore_1(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.lstore_1(displacementToSlot);
    }

    public static void lstore_2(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.lstore_2(displacementToSlot);
    }

    public static void lstore_3(int displacementToSlot) {
        trace();
        UnoptimizedBytecodeTemplateSource.lstore_3(displacementToSlot);
    }

    public static void lsub() {
        trace();
        UnoptimizedBytecodeTemplateSource.lsub();
    }

    public static void lushr() {
        trace();
        UnoptimizedBytecodeTemplateSource.lushr();
    }

    public static void lxor() {
        trace();
        UnoptimizedBytecodeTemplateSource.lxor();
    }

    public static void monitorenter() {
        trace();
        UnoptimizedBytecodeTemplateSource.monitorenter();
    }

    public static void monitorexit() {
        trace();
        UnoptimizedBytecodeTemplateSource.monitorexit();
    }

    public static void multianewarray(ResolutionGuard guard, int[] lengths) {
        trace();
        UnoptimizedBytecodeTemplateSource.multianewarray(guard, lengths);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.NEW, traced = Traced.YES)
    public static void new_(ResolutionGuard guard) {
        trace();
        UnoptimizedBytecodeTemplateSource.new_(guard);
    }

    public static void newarray(Kind kind) {
        trace();
        UnoptimizedBytecodeTemplateSource.newarray(kind);
    }

    public static void nop() {
        trace();
        UnoptimizedBytecodeTemplateSource.nop();
    }

    public static void pop() {
        trace();
        UnoptimizedBytecodeTemplateSource.pop();
    }

    public static void pop2() {
        trace();
        UnoptimizedBytecodeTemplateSource.pop2();
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.RETURN, traced = Traced.YES)
    public static void vreturn() {
        trace();
        UnoptimizedBytecodeTemplateSource.vreturn();
    }

    public static void saload() {
        trace();
        UnoptimizedBytecodeTemplateSource.saload();
    }

    public static void sastore() {
        trace();
        UnoptimizedBytecodeTemplateSource.sastore();
    }

    public static void sipush(short value) {
        trace();
        UnoptimizedBytecodeTemplateSource.sipush(value);
    }

    public static void swap() {
        trace();
        UnoptimizedBytecodeTemplateSource.swap();
    }

    public static void if_acmpeq() {
        trace();
        BranchBytecodeSource.if_acmpeq();
    }

    public static void if_acmpne() {
        trace();
        BranchBytecodeSource.if_acmpne();
    }

    public static void if_icmpeq() {
        trace();
        BranchBytecodeSource.if_icmpeq();
    }

    public static void if_icmpne() {
        trace();
        BranchBytecodeSource.if_icmpne();
    }

    public static void if_icmplt() {
        trace();
        BranchBytecodeSource.if_icmplt();
    }

    public static void if_icmpge() {
        trace();
        BranchBytecodeSource.if_icmpge();
    }

    public static void if_icmpgt() {
        trace();
        BranchBytecodeSource.if_icmpgt();
    }

    public static void if_icmple() {
        trace();
        BranchBytecodeSource.if_icmple();
    }

    public static void ifeq() {
        trace();
        BranchBytecodeSource.ifeq();
    }

    public static void ifne() {
        trace();
        BranchBytecodeSource.ifne();
    }

    public static void iflt() {
        trace();
        BranchBytecodeSource.iflt();
    }

    public static void ifge() {
        trace();
        BranchBytecodeSource.ifge();
    }

    public static void ifgt() {
        trace();
        BranchBytecodeSource.ifgt();
    }

    public static void ifle() {
        trace();
        BranchBytecodeSource.ifle();
    }

    public static void ifnonnull() {
        trace();
        BranchBytecodeSource.ifnonnull();
    }

    public static void ifnull() {
        trace();
        BranchBytecodeSource.ifnull();
    }

    //
    // Resolved Tracing Bytecode Templates
    //

    @BYTECODE_TEMPLATE(bytecode = Bytecode.LDC, kind = KindEnum.REFERENCE, resolved = Resolved.YES, traced = Traced.YES)
    public static void rldc(Object value) {
        trace();
        ResolvedBytecodeTemplateSource.rldc(value);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.CHECKCAST, resolved = Resolved.YES, traced = Traced.YES)
    public static void checkcast(ClassActor classActor) {
        trace();
        ResolvedBytecodeTemplateSource.checkcast(classActor);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.INSTANCEOF, resolved = Resolved.YES, traced = Traced.YES)
    public static void instanceof_(ClassActor classActor) {
        trace();
        ResolvedBytecodeTemplateSource.instanceof_(classActor);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.ANEWARRAY, resolved = Resolved.YES, traced = Traced.YES)
    public static void anewarray(ArrayClassActor arrayClassActor) {
        trace();
        ResolvedBytecodeTemplateSource.anewarray(arrayClassActor);
    }

    @BYTECODE_TEMPLATE(bytecode = Bytecode.MULTIANEWARRAY, resolved = Resolved.YES, traced = Traced.YES)
    public static void multianewarray(ArrayClassActor arrayClassActor, int[] lengths) {
        trace();
        ResolvedBytecodeTemplateSource.multianewarray(arrayClassActor, lengths);
    }
}

/*
 * Copyright (c) 2017, 2019, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.t1x.armv7;

import static com.oracle.max.asm.target.armv7.ARMV7.*;
import static com.oracle.max.vm.ext.t1x.T1X.*;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.classfile.ErrorContext.*;
import static com.sun.max.vm.compiler.target.Safepoints.*;
import static com.sun.max.vm.stack.JVMSFrameLayout.*;

import java.util.*;

import com.oracle.max.asm.target.armv7.*;
import com.oracle.max.asm.target.armv7.ARMV7Assembler.*;
import com.oracle.max.cri.intrinsics.*;
import com.oracle.max.vm.ext.maxri.*;
import com.oracle.max.vm.ext.t1x.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiAddress.*;
import com.sun.cri.ci.CiTargetMethod.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.armv7.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

public class ARMV7T1XCompilation extends T1XCompilation {

    protected final ARMV7MacroAssembler asm;
    private final PatchInfoARMV7 patchInfo;

    public ARMV7T1XCompilation(T1X compiler) {
        super(compiler);
        asm = new ARMV7MacroAssembler(target(), null);
        buf = asm.codeBuffer;
        patchInfo = new PatchInfoARMV7();
    }

    @Override
    protected void initFrame(ClassMethodActor method, CodeAttribute codeAttribute) {
        int maxLocals = codeAttribute.maxLocals;
        int maxStack = codeAttribute.maxStack;
        int maxParams = method.numberOfParameterSlots();
        if (method.isSynchronized() && !method.isStatic()) {
            synchronizedReceiver = maxLocals++;
        }
        frame = new ARMV7JVMSFrameLayout(maxLocals, maxStack, maxParams, T1XTargetMethod.templateSlots());
    }

    public ARMV7MacroAssembler getMacroAssembler() {
        return asm;
    }

    @Override
    public void decStack(int numberOfSlots) {
        assert numberOfSlots > 0;
        asm.addq(sp, numberOfSlots * JVMS_SLOT_SIZE);
    }

    @Override
    public void incStack(int numberOfSlots) {
        assert numberOfSlots > 0;
        asm.subq(sp, numberOfSlots * JVMS_SLOT_SIZE);
    }

    @Override
    protected void adjustReg(CiRegister reg, int delta) {
        asm.incrementl(reg, delta);
    }

    @Override
    public void peekObject(CiRegister dst, int index) {
        asm.load(dst, spWord(index), CiKind.Int);
    }

    @Override
    public void pokeObject(CiRegister src, int index) {
        asm.store(src, spWord(index), CiKind.Int);
    }

    @Override
    public void peekWord(CiRegister dst, int index) {
        asm.load(dst, spWord(index), CiKind.Int);
    }

    @Override
    public void pokeWord(CiRegister src, int index) {
        asm.store(src, spWord(index), CiKind.Int);
    }

    public void pokeDoubleWord(CiRegister src, int index) {
        asm.store(src, spWord(index), CiKind.Long);
    }

    public void peekDoubleWord(CiRegister dst, int index) {
        asm.load(dst, spWord(index), CiKind.Long);
    }

    @Override
    public void peekInt(CiRegister dst, int index) {
        asm.load(dst, spInt(index), CiKind.Int);
    }

    @Override
    public void pokeInt(CiRegister src, int index) {
        asm.store(src, spInt(index), CiKind.Int);
    }

    @Override
    public void peekLong(CiRegister dst, int index) {
        assert dst.getEncoding() < 10;
        asm.load(dst, spLong(index), CiKind.Long);
    }

    @Override
    public void pokeLong(CiRegister src, int index) {
        assert src.getEncoding() < 10;
        asm.store(src, spLong(index), CiKind.Long);
    }

    @Override
    public void peekDouble(CiRegister dst, int index) {
        assert dst.isFpu();
        asm.load(dst, spLong(index), CiKind.Double);
    }

    @Override
    public void pokeDouble(CiRegister src, int index) {
        assert src.isFpu();
        asm.store(src, spLong(index), CiKind.Double);

    }

    @Override
    public void peekFloat(CiRegister dst, int index) {
        assert dst.isFpu();
        asm.load(dst, spInt(index), CiKind.Float);
    }

    @Override
    public void pokeFloat(CiRegister src, int index) {
        assert src.isFpu();
        asm.store(src, spInt(index), CiKind.Float);
    }

    @Override
    protected void assignObjectReg(CiRegister dst, CiRegister src) {
        asm.mov(ARMV7Assembler.ConditionFlag.Always, false, dst, src);
    }

    @Override
    protected void assignWordReg(CiRegister dst, CiRegister src) {
        asm.mov(ARMV7Assembler.ConditionFlag.Always, false, dst, src);
    }

    @Override
    protected void assignLong(CiRegister dst, long value) {
        assert dst.number < 10;
        asm.movImm64(ConditionFlag.Always, dst, ARMV7.cpuRegisters[dst.getEncoding() + 1], value);
    }

    @Override
    protected void do_invokespecial_resolved(T1XTemplateTag tag, MethodActor methodActor, int receiverStackIndex) {
        peekObject(ARMV7.r8, receiverStackIndex);
        nullCheck(ARMV7.r8);
    }

    @Override
    protected void do_multianewarray(int index, int numberOfDimensions) {
        {
            start(T1XTemplateTag.CREATE_MULTIANEWARRAY_DIMENSIONS);
            assignWordReg(0, "sp", sp);
            assignInt(1, "n", numberOfDimensions);
            finish();
            asm.vmov(ConditionFlag.Always, ARMV7.s31, ARMV7.r0, null, CiKind.Float, CiKind.Int);
            decStack(numberOfDimensions);
        }
        ClassConstant classRef = cp.classAt(index);
        if (classRef.isResolvableWithoutClassLoading(cp)) {
            start(T1XTemplateTag.MULTIANEWARRAY$resolved);
            ClassActor arrayClassActor = classRef.resolve(cp, index);
            assert arrayClassActor.isArrayClass();
            assert arrayClassActor.numberOfDimensions() >= numberOfDimensions : "dimensionality of array class constant smaller that dimension operand";
            assignObject(0, "arrayClassActor", arrayClassActor);
            asm.vmov(ConditionFlag.Always, reg(1, "lengths", Kind.REFERENCE), ARMV7.s31, null, CiKind.Int, CiKind.Float);
            finish();
        } else {
            // Unresolved case
            start(T1XTemplateTag.MULTIANEWARRAY);
            assignObject(0, "guard", cp.makeResolutionGuard(index));
            asm.vmov(ConditionFlag.Always, reg(1, "lengths", Kind.REFERENCE), ARMV7.s31, null, CiKind.Int, CiKind.Float);
            finish();
        }
    }

    @Override
    protected void assignObject(CiRegister dst, Object value) {
        if (value == null) {
            asm.xorq(dst, dst);
            return;
        }
        int index = objectLiterals.size();
        objectLiterals.add(value);
        asm.nop(2);
        asm.addRegisters(ConditionFlag.Always, false, ARMV7.r12, ARMV7.r12, ARMV7.r15, 0, 0);
        int dispPos = buf.position() - 12;
        asm.ldr(ConditionFlag.Always, dst, r12, 0);
        patchInfo.addObjectLiteral(dispPos, index);
    }

    @Override
    protected void loadInt(CiRegister dst, int index) {
        loadGeneric(dst, index, Kind.INT);
    }

    @Override
    protected void loadLong(CiRegister dst, int index) {
        assert dst.number < 10;
        asm.load(dst, localSlot(localSlotOffset(index, Kind.LONG)), CiKind.Long);
    }

    @Override
    protected void loadWord(CiRegister dst, int index) {
        loadGeneric(dst, index, Kind.WORD);
    }

    @Override
    protected void loadObject(CiRegister dst, int index) {
        loadGeneric(dst, index, Kind.REFERENCE);
    }

    private void loadGeneric(CiRegister dst, int index, Kind kind) {
        CiAddress address = localSlot(localSlotOffset(index, kind));
        if (ARMImmediates.isARMV7Immediate(address)) {
            CiRegister register = address.base();
            if (register == CiRegister.Frame) {
                register = asm.frameRegister;
            }
            asm.ldrImmediate(ConditionFlag.Always, 1, address.displacement >= 0 ? 1 : 0, 0, dst, register, address.displacement);
            return;
        }
        asm.setUpScratch(address);
        asm.ldr(ConditionFlag.Always, dst, ARMV7.r12, 0);
    }

    @Override
    protected void storeInt(CiRegister src, int index) {
        storeGeneric(src, index, Kind.INT);
    }

    @Override
    protected void storeLong(CiRegister src, int index) {
        assert src.number < 10;
        asm.store(src, localSlot(localSlotOffset(index, Kind.LONG)), CiKind.Long);
    }

    @Override
    protected void storeWord(CiRegister src, int index) {
        storeGeneric(src, index, Kind.WORD);
    }

    @Override
    protected void storeObject(CiRegister src, int index) {
        storeGeneric(src, index, Kind.REFERENCE);
    }

    private void storeGeneric(CiRegister src, int index, Kind kind) {
        CiAddress address = localSlot(localSlotOffset(index, kind));
        if (ARMImmediates.isARMV7Immediate(address)) {
            CiRegister register = address.base();
            if (register == CiRegister.Frame) {
                register = asm.frameRegister;
            }
            asm.strImmediate(ConditionFlag.Always, 1, address.displacement >= 0 ? 1 : 0, 0, src, register, address.displacement);
            return;
        }
        asm.setUpScratch(address);
        asm.str(ConditionFlag.Always, src, scratch, 0);
    }

    @Override
    public void assignInt(CiRegister dst, int value) {
        asm.movImmediate(ConditionFlag.Always, dst, value);
    }

    @Override
    protected void assignFloat(CiRegister dst, float value) {
        assert dst.number >= ARMV7.s0.number && dst.number <= ARMV7.s31.number;
        asm.movImmediate(ConditionFlag.Always, ARMV7.r12, Float.floatToRawIntBits(value));
        asm.vmov(ConditionFlag.Always, dst, ARMV7.r12, null, CiKind.Float, CiKind.Int);
    }

    @Override
    protected void do_store(int index, Kind kind) {
        switch (kind.asEnum) {
            case INT:
            case FLOAT:
                peekInt(ARMV7.r8, 0);
                decStack(1);
                storeInt(ARMV7.r8, index);
                break;
            case REFERENCE:
                peekWord(ARMV7.r8, 0);
                decStack(1);
                storeWord(ARMV7.r8, index);
                break;
            case LONG:
            case DOUBLE:
                asm.vmov(ConditionFlag.Always, ARMV7.s31, ARMV7.r9, null, CiKind.Float, CiKind.Int);
                peekLong(ARMV7.r8, 0);
                decStack(2);
                storeLong(ARMV7.r8, index);
                asm.vmov(ConditionFlag.Always, ARMV7.r9, ARMV7.s31, null, CiKind.Int, CiKind.Float);
                break;
            default:
                throw new InternalError("Unexpected kind: " + kind);
        }
    }

    @Override
    protected void do_load(int index, Kind kind) {
        switch (kind.asEnum) {
            case INT:
            case FLOAT:
                loadInt(ARMV7.r8, index);
                incStack(1);
                pokeInt(ARMV7.r8, 0);
                break;
            case REFERENCE:
                loadWord(ARMV7.r8, index);
                incStack(1);
                pokeWord(ARMV7.r8, 0);
                break;
            case LONG:
            case DOUBLE:
                asm.vmov(ConditionFlag.Always, ARMV7.s31, ARMV7.r9, null, CiKind.Float, CiKind.Int);
                loadLong(ARMV7.r8, index);
                incStack(2);
                pokeLong(ARMV7.r8, 0);
                asm.vmov(ConditionFlag.Always, ARMV7.r9, ARMV7.s31, null, CiKind.Int, CiKind.Float);
                break;
            default:
                throw new InternalError("Unexpected kind: " + kind);
        }
    }

    @Override
    protected void do_oconst(Object value) {
        assignObject(ARMV7.r8, value);
        incStack(1);
        pokeObject(ARMV7.r8, 0);
    }

    @Override
    protected void do_iconst(int value) {
        assignInt(ARMV7.r8, value);
        incStack(1);
        pokeInt(ARMV7.r8, 0);
    }

    @Override
    protected void do_iinc(int index, int increment) {
        loadInt(ARMV7.r8, index);
        adjustReg(ARMV7.r8, increment);
        storeInt(ARMV7.r8, index);
    }

    @Override
    protected void do_fconst(float value) {
        assignInt(ARMV7.r8, Float.floatToRawIntBits(value));
        incStack(1);
        pokeInt(ARMV7.r8, 0);
    }

    @Override
    protected void do_dconst(double value) {
        asm.vmov(ConditionFlag.Always, ARMV7.s31, ARMV7.r9, null, CiKind.Float, CiKind.Int);
        assignLong(ARMV7.r8, Double.doubleToRawLongBits(value));
        incStack(2);
        pokeLong(ARMV7.r8, 0);
        asm.vmov(ConditionFlag.Always, ARMV7.r9, ARMV7.s31, null, CiKind.Int, CiKind.Float);
    }

    @Override
    protected void do_lconst(long value) {
        asm.vmov(ConditionFlag.Always, ARMV7.s31, ARMV7.r9, null, CiKind.Float, CiKind.Int);
        assignLong(ARMV7.r8, value);
        incStack(2);
        pokeLong(ARMV7.r8, 0);
        asm.vmov(ConditionFlag.Always, ARMV7.r9, ARMV7.s31, null, CiKind.Int, CiKind.Float);
    }

    @Override
    protected void assignDouble(CiRegister dst, double value) {
        assert dst.isFpu();
        asm.saveInFP(9);
        assignLong(ARMV7.r8, Double.doubleToRawLongBits(value));
        asm.vmov(ConditionFlag.Always, dst, ARMV7.r8, ARMV7.r9, CiKind.Double, CiKind.Int);
        asm.restoreFromFP(9);
    }

    @Override
    protected int callDirect() {
        int causePos = buf.position();
        asm.call();
        int safepointPos = buf.position();
        asm.nop();
        return Safepoints.make(safepointPos, causePos, DIRECT_CALL, TEMPLATE_CALL);
    }

    @Override
    protected int callDirect(int receiverStackIndex) {
        if (receiverStackIndex >= 0) {
            peekObject(ARMV7.r0, receiverStackIndex);
        }
        int causePos = buf.position();
        asm.call();
        int safepointPos = buf.position();
        asm.nop();
        return Safepoints.make(safepointPos, causePos, DIRECT_CALL, TEMPLATE_CALL);
    }

    @Override
    protected int callIndirect(CiRegister target, int receiverStackIndex) {
        asm.mov(ConditionFlag.Always, false, ARMV7.r8, target);
        if (receiverStackIndex >= 0) {
            peekObject(ARMV7.r0, receiverStackIndex);
        }
        int causePos = buf.position();
        asm.call(ARMV7.r8);
        int safepointPos = buf.position();
        asm.nop();
        return Safepoints.make(safepointPos, causePos, INDIRECT_CALL, TEMPLATE_CALL);
    }

    @Override
    protected void nullCheck(CiRegister src) {
        asm.nullCheck(src);
    }

    private void alignDirectCall(int callPos) {
        final int alignment = 7;
        final int roundDownMask = ~alignment;
        final int directCallInstructionLength = 4;
        final int endOfCallSite = callPos + (directCallInstructionLength - 1);
        if ((callPos & roundDownMask) != (endOfCallSite & roundDownMask)) {
            asm.nop(8 - (callPos & alignment));
        }
    }

    private int framePointerAdjustment() {
        final int enterSize = frame.frameSize() - Word.size();
        return enterSize - frame.sizeOfNonParameterLocals();
    }

    @Override
    protected Adapter emitPrologue() {
        Adapter adapter = null;
        if (adapterGenerator != null) {
            adapter = adapterGenerator.adapt(method, asm);
        }

        int frameSize = frame.frameSize();
        asm.push(ConditionFlag.Always, asm.getRegisterList(ARMV7.LR, ARMV7.FP), true);
        asm.mov(ConditionFlag.Always, false, ARMV7.FP, ARMV7.rsp);
        asm.subq(ARMV7.rsp, frameSize - Word.size());
        asm.subq(ARMV7.FP, framePointerAdjustment());

        if (Trap.STACK_BANGING) {
            int pageSize = platform().pageSize;
            int framePages = frameSize / pageSize;
            for (int i = 0; i <= framePages; i++) {
                int offset = (i + VmThread.STACK_SHADOW_PAGES) * pageSize;
                offset = offset - frameSize;
                asm.setUpScratch(new CiAddress(WordUtil.archKind(), RSP, -offset));
                asm.str(ConditionFlag.Always, ARMV7.r0, scratch, 0);
            }
        }
        if (T1XOptions.DebugMethods) {
            int a = debugMethodWriter.getNextID();
            asm.movImm32(ConditionFlag.Always, ARMV7.r12, a);
            try {
                debugMethodWriter.append(method.holder() + "." + method.name(), a);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return adapter;
    }

    @Override
    protected void emitUnprotectMethod() {
        protectionLiteralIndex = objectLiterals.size();
        objectLiterals.add(T1XTargetMethod.PROTECTED);
        asm.xorq(ARMV7.r8, ARMV7.r8);
        asm.nop(2);
        int dispPos = buf.position() - 8;
        patchInfo.addObjectLiteral(dispPos, protectionLiteralIndex);
        asm.addRegisters(ConditionFlag.Always, false, ARMV7.r12, ARMV7.r12, ARMV7.r15, 0, 0);
        asm.str(ConditionFlag.Always, ARMV7.r8, ARMV7.r12, 0);
    }

    @Override
    protected void emitEpilogue() {
        asm.addq(ARMV7.FP, framePointerAdjustment());
        asm.mov(ConditionFlag.Always, false, ARMV7.rsp, ARMV7.FP);
        final short stackAmountInBytes = (short) frame.sizeOfParameters();
        asm.pop(ConditionFlag.Always, asm.getRegisterList(ARMV7.FP), true);
        asm.pop(ConditionFlag.Always, asm.getRegisterList(r8), true);
        asm.movImmediate(ConditionFlag.Always, scratch, stackAmountInBytes);
        asm.addRegisters(ConditionFlag.Always, false, ARMV7.rsp, ARMV7.rsp, scratch, 0, 0);
        asm.mov(ConditionFlag.Always, false, ARMV7.PC, ARMV7.r8);
        if (T1XOptions.DebugMethods) {
            try {
                debugMethodWriter.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void do_preVolatileFieldAccess(T1XTemplateTag tag, FieldActor fieldActor) {
        if (fieldActor.isVolatile()) {
            boolean isWrite = tag.opcode == Bytecodes.PUTFIELD || tag.opcode == Bytecodes.PUTSTATIC;
            asm.membar(isWrite ? MemoryBarriers.JMM_PRE_VOLATILE_WRITE : MemoryBarriers.JMM_PRE_VOLATILE_READ);
        }
    }

    @Override
    protected void do_postVolatileFieldAccess(T1XTemplateTag tag, FieldActor fieldActor) {
        if (fieldActor.isVolatile()) {
            boolean isWrite = tag.opcode == Bytecodes.PUTFIELD || tag.opcode == Bytecodes.PUTSTATIC;
            asm.membar(isWrite ? MemoryBarriers.JMM_POST_VOLATILE_WRITE : MemoryBarriers.JMM_POST_VOLATILE_READ);
        }
    }

    @Override
    protected void do_tableswitch() {
        int bci = stream.currentBCI();
        BytecodeTableSwitch ts = new BytecodeTableSwitch(stream, bci);
        int lowMatch = ts.lowKey();
        int highMatch = ts.highKey();
        if (lowMatch > highMatch) {
            throw verifyError("Low must be less than or equal to high in TABLESWITCH");
        }

        // Pop index from stack into scratch
        asm.setUpScratch(new CiAddress(CiKind.Int, RSP));
        asm.ldr(ConditionFlag.Always, ARMV7.r8, asm.scratchRegister, 0);
        asm.addq(r13, JVMSFrameLayout.JVMS_SLOT_SIZE);
        asm.push(ConditionFlag.Always, 1 << 9 | 1 << 7, true);
        asm.mov(ConditionFlag.Always, false, ARMV7.r9, ARMV7.r8); // r9 stores index
        startBlock(ts.defaultTarget());
        asm.cmpl(r9, lowMatch);
        asm.pop(ConditionFlag.SignedLesser, 1 << 9 | 1 << 7, true);
        int pos = buf.position();
        patchInfo.addJCC(ConditionFlag.SignedLesser, pos, ts.defaultTarget());
        asm.jcc(ConditionFlag.SignedLesser, 0, true);
        if (lowMatch != 0) {
            asm.subq(r9, lowMatch);
            asm.cmpl(r9, highMatch - lowMatch);
        } else {
            asm.cmpl(r9, highMatch);
        }
        asm.pop(ConditionFlag.SignedGreater, 1 << 9 | 1 << 7, true);
        pos = buf.position();
        patchInfo.addJCC(ConditionFlag.SignedGreater, pos, ts.defaultTarget());
        asm.jcc(ConditionFlag.SignedGreater, 0, true);

        // Set r9 to address of jump table
        int leaPos = buf.position();
        asm.leaq(r7, CiAddress.Placeholder);
        int afterLea = buf.position();

        // Load jump table entry into r15 and jump to it
        asm.setUpScratch(new CiAddress(CiKind.Int, r7.asValue(), r9.asValue(), Scale.Times4, 0));
        asm.ldr(ConditionFlag.Always, r12, ARMV7.r12, 0);
        asm.addRegisters(ConditionFlag.Always, false, r12, ARMV7.r15, r12, 0, 0);
        asm.add12BitImmediate(ConditionFlag.Always, false, r12, r12, 8);
        asm.pop(ConditionFlag.Always, 1 << 9 | 1 << 7, true); // restore r9/r10
        asm.mov(ConditionFlag.Always, false, ARMV7.r15, ARMV7.r12);

        // Patch LEA instruction above now that we know the position of the jump table
        int jumpTablePos = buf.position();
        buf.setPosition(leaPos); // move the asm buffer position to where the leaq was added
        asm.leaq(r7, new CiAddress(WordUtil.archKind(), rip.asValue(), jumpTablePos - afterLea));
        buf.setPosition(jumpTablePos); // reposition back to the correct place

        // Emit jump table entries
        for (int i = 0; i < ts.numberOfCases(); i++) {
            int targetBCI = ts.targetAt(i);
            startBlock(targetBCI);
            pos = buf.position();
            patchInfo.addJumpTableEntry(pos, jumpTablePos, targetBCI);
            buf.emitInt(0);
        }
        if (codeAnnotations == null) {
            codeAnnotations = new ArrayList<CiTargetMethod.CodeAnnotation>();
        }
        codeAnnotations.add(new JumpTable(jumpTablePos, ts.lowKey(), ts.highKey(), 4));
    }

    @Override
    protected void do_lookupswitch() {
        int bci = stream.currentBCI();
        BytecodeLookupSwitch ls = new BytecodeLookupSwitch(stream, bci);
        if (ls.numberOfCases() == 0) {
            // Pop the key
            decStack(1);
            int targetBCI = ls.defaultTarget();
            startBlock(targetBCI);
            if (stream.nextBCI() == targetBCI) {
                // Skip completely if default target is next instruction
            } else if (targetBCI <= bci) {
                int target = bciToPos[targetBCI];
                assert target != 0;
                asm.jmp(target, false);
            } else {
                patchInfo.addJMP(buf.position(), targetBCI);
                asm.jmp(0, true);
            }
        } else {
            asm.setUpScratch(new CiAddress(CiKind.Int, RSP));
            asm.ldr(ConditionFlag.Always, ARMV7.r8, asm.scratchRegister, 0);
            asm.addq(ARMV7.r13, JVMSFrameLayout.JVMS_SLOT_SIZE);
            asm.push(ConditionFlag.Always, 1 << 7 | 1 << 9 | 1 << 6, true);
            asm.mov(ConditionFlag.Always, false, r9, r8); // r9 stores index

            int leaPos = buf.position();
            asm.leaq(r6, CiAddress.Placeholder);
            int afterLea = buf.position();
            asm.movImmediate(ConditionFlag.Always, r7, (ls.numberOfCases() - 1) * 2);

            int loopPos = buf.position();
            asm.setUpScratch(new CiAddress(CiKind.Int, r6.asValue(), r7.asValue(), Scale.Times4, 0));
            asm.ldr(ConditionFlag.Always, r12, r12, 0);
            asm.cmpl(ARMV7.r9, ARMV7.r12);

            // If equal, exit loop
            int matchTestPos = buf.position();
            final int placeholderForShortJumpDisp = matchTestPos + 4;
            asm.jcc(ConditionFlag.Equal, placeholderForShortJumpDisp, false);
            assert buf.position() - matchTestPos == 4;

            // Decrement loop var and jump to top of loop if it did not go below zero (i.e. carry flag was not set)
            asm.sub(ConditionFlag.Always, true, r7, r7, 2, 0);
            asm.jcc(ConditionFlag.Positive, loopPos, false);

            // Jump to default target
            startBlock(ls.defaultTarget());
            patchInfo.addJMP(buf.position() + 4, ls.defaultTarget()); // The +4 is to jump over the pop
            asm.pop(ConditionFlag.Always, 1 << 9 | 1 << 6 | 1 << 7, true);
            asm.jmp(0, true);

            int matchPos = buf.position();

            buf.setPosition(matchTestPos);
            asm.jcc(ConditionFlag.Equal, matchPos, false);
            buf.setPosition(matchPos);

            // Load jump table entry into r15 and jump to it
            asm.setUpScratch(new CiAddress(CiKind.Int, r6.asValue(), r7.asValue(), Scale.Times4, 4));
            asm.ldr(ConditionFlag.Always, r12, r12, 0);
            asm.addRegisters(ConditionFlag.Always, false, r12, r15, r12, 0, 0);
            asm.add12BitImmediate(ConditionFlag.Always, false, r12, r12, 8);
            asm.pop(ConditionFlag.Always, 1 << 9 | 1 << 6 | 1 << 7, true);
            asm.mov(ConditionFlag.Always, false, r15, r12); // changed to false for flag update

            // Inserting padding so that lookup table address is 4-byte aligned
            while ((buf.position() & 0x3) != 0) {
                asm.nop();
            }

            // Patch the LEA instruction above now that we know the position of the lookup table
            int lookupTablePos = buf.position();
            buf.setPosition(leaPos);
            asm.leaq(r6, new CiAddress(WordUtil.archKind(), rip.asValue(), lookupTablePos - afterLea));
            buf.setPosition(lookupTablePos);

            // Emit lookup table entries
            for (int i = 0; i < ls.numberOfCases(); i++) {
                int key = ls.keyAt(i);
                int targetBCI = ls.targetAt(i);
                startBlock(targetBCI);
                patchInfo.addLookupTableEntry(buf.position(), key, lookupTablePos, targetBCI);
                buf.emitInt(key);
                buf.emitInt(0);
            }
            if (codeAnnotations == null) {
                codeAnnotations = new ArrayList<CiTargetMethod.CodeAnnotation>();
            }
            codeAnnotations.add(new LookupTable(lookupTablePos, ls.numberOfCases(), 4, 4));
        }
    }

    @Override
    public void cleanup() {
        patchInfo.size = 0;
        super.cleanup();
    }

    @Override
    protected void branch(int opcode, int targetBCI, int bci) {
        ConditionFlag cc;
        // Important: the compare instructions must come after the stack
        // adjustment instructions as both affect the condition flags.
        asm.vmov(ConditionFlag.Always, ARMV7.s31, ARMV7.r9, null, CiKind.Float, CiKind.Int);

        switch (opcode) {
            case Bytecodes.IFEQ:
                peekInt(r8, 0);
                assignInt(r9, 0);
                decStack(1);
                asm.cmpl(r8, r9);
                cc = ConditionFlag.Equal;
                break;
            case Bytecodes.IFNE:
                peekInt(r8, 0);
                assignInt(r9, 0);
                decStack(1);
                asm.cmpl(r8, r9);
                cc = ConditionFlag.NotEqual;
                break;
            case Bytecodes.IFLE:
                peekInt(r8, 0);
                assignInt(r9, 0);
                decStack(1);
                asm.cmpl(r8, r9);
                cc = ConditionFlag.SignedLowerOrEqual;
                break;
            case Bytecodes.IFLT:
                peekInt(r8, 0);
                assignInt(r9, 0);
                decStack(1);
                asm.cmpl(r8, r9);
                cc = ConditionFlag.SignedLesser;
                break;
            case Bytecodes.IFGE:
                peekInt(r8, 0);
                assignInt(r9, 0);
                decStack(1);
                asm.cmpl(r8, r9);
                cc = ConditionFlag.SignedGreaterOrEqual;
                break;
            case Bytecodes.IFGT:
                peekInt(r8, 0);
                assignInt(r9, 0);
                decStack(1);
                asm.cmpl(r8, r9);
                cc = ConditionFlag.SignedGreater;
                break;
            case Bytecodes.IF_ICMPEQ:
                peekInt(r8, 1);
                peekInt(r9, 0);
                decStack(2);
                asm.cmpl(r8, r9);
                cc = ConditionFlag.Equal;
                break;
            case Bytecodes.IF_ICMPNE:
                peekInt(r8, 1);
                peekInt(r9, 0);
                decStack(2);
                asm.cmpl(r8, r9);
                cc = ConditionFlag.NotEqual;
                break;
            case Bytecodes.IF_ICMPGE:
                peekInt(r8, 1);
                peekInt(r9, 0);
                decStack(2);
                asm.cmpl(r8, r9);
                cc = ConditionFlag.SignedGreaterOrEqual;
                break;
            case Bytecodes.IF_ICMPGT:
                peekInt(r8, 1);
                peekInt(r9, 0);
                decStack(2);
                asm.cmpl(r8, r9);
                cc = ConditionFlag.SignedGreater;
                break;
            case Bytecodes.IF_ICMPLE:
                peekInt(r8, 1);
                peekInt(r9, 0);
                decStack(2);
                asm.cmpl(r8, r9);
                cc = ConditionFlag.SignedLowerOrEqual;
                break;
            case Bytecodes.IF_ICMPLT:
                peekInt(r8, 1);
                peekInt(r9, 0);
                decStack(2);
                asm.cmpl(r8, r9);
                cc = ConditionFlag.SignedLesser;
                break;
            case Bytecodes.IF_ACMPEQ:
                peekObject(r8, 1);
                peekObject(r9, 0);
                decStack(2);
                asm.cmpl(r8, r9);
                cc = ConditionFlag.Equal;
                break;
            case Bytecodes.IF_ACMPNE:
                peekObject(r8, 1);
                peekObject(r9, 0);
                decStack(2);
                asm.cmpl(r8, r9);
                cc = ConditionFlag.NotEqual;
                break;
            case Bytecodes.IFNULL:
                peekObject(r8, 0);
                assignObject(r9, null);
                decStack(1);
                asm.cmpl(r8, r9);
                cc = ConditionFlag.Equal;
                break;
            case Bytecodes.IFNONNULL:
                peekObject(r8, 0);
                assignObject(r9, null);
                decStack(1);
                asm.cmpl(r8, r9);
                cc = ConditionFlag.NotEqual;
                break;
            case Bytecodes.GOTO:
            case Bytecodes.GOTO_W:
                cc = null;
                break;
            default:
                throw new InternalError("Unknown branch opcode: " + Bytecodes.nameOf(opcode));
        }

        asm.vmov(ConditionFlag.Always, ARMV7.r9, ARMV7.s31, null, CiKind.Int, CiKind.Float);

        int pos = buf.position();
        if (bci < targetBCI) {
            // Forward branch
            if (cc != null) {
                patchInfo.addJCC(cc, pos, targetBCI);
                asm.jcc(cc, 0, true);
            } else {
                // Unconditional jump
                patchInfo.addJMP(pos, targetBCI);
                asm.jmp(0, true);
            }
            assert bciToPos[targetBCI] == 0;
        } else {
            // Backward branch
            // Compute relative offset.
            final int target = bciToPos[targetBCI];
            if (cc == null) {
                asm.jmp(target, false);
            } else {
                asm.jcc(cc, target, false);
            }
        }
    }

    @Override
    protected void addObjectLiteralPatch(int index, int patchPos) {
        final int dispPos = patchPos;
        patchInfo.addObjectLiteral(dispPos, index);
    }

    @Override
    protected void fixup() {
        int i = 0;
        int[] data = patchInfo.data;
        while (i < patchInfo.size) {
            int tag = data[i++];
            if (tag == PatchInfoARMV7.JCC) {
                ConditionFlag cc = ConditionFlag.values[data[i++]];
                int pos = data[i++];
                int targetBCI = data[i++];
                int target = bciToPos[targetBCI];
                assert target != 0;
                buf.setPosition(pos);
                asm.jcc(cc, target, true);
            } else if (tag == PatchInfoARMV7.JMP) {
                int pos = data[i++];
                int targetBCI = data[i++];
                int target = bciToPos[targetBCI];
                assert target != 0;
                buf.setPosition(pos);
                asm.jmp(target, true);
            } else if (tag == PatchInfoARMV7.JUMP_TABLE_ENTRY) {
                int pos = data[i++];
                int jumpTablePos = data[i++];
                int targetBCI = data[i++];
                int target = bciToPos[targetBCI];
                assert target != 0;
                int disp = target - jumpTablePos;
                buf.setPosition(pos);
                buf.emitInt(disp);
            } else if (tag == PatchInfoARMV7.LOOKUP_TABLE_ENTRY) {
                int pos = data[i++];
                int key = data[i++];
                int lookupTablePos = data[i++];
                int targetBCI = data[i++];
                int target = bciToPos[targetBCI];
                assert target != 0;
                int disp = target - lookupTablePos;
                buf.setPosition(pos);
                buf.emitInt(key);
                buf.emitInt(disp);
            } else if (tag == PatchInfoARMV7.OBJECT_LITERAL) {
                int dispPos = data[i++];
                int index = data[i++];
                int tempDispPos = dispPos + 12;
                assert objectLiterals.get(index) != null;
                buf.setPosition(dispPos);
                int dispFromCodeStart = dispFromCodeStart(objectLiterals.size(), 0, index, true);
                int disp = movqDisp(tempDispPos, dispFromCodeStart);
                buf.setPosition(dispPos);
                int val = ARMV7Assembler.movwHelper(ConditionFlag.Always, ARMV7.r12, disp & 0xffff);
                buf.emitInt(val);
                val = ARMV7Assembler.movtHelper(ConditionFlag.Always, ARMV7.r12, (disp >> 16) & 0xffff);
                buf.emitInt(val);
            } else {
                throw FatalError.unexpected(String.valueOf(tag));
            }
        }
    }

    public static int movqDisp(int dispPos, int dispFromCodeStart) {
        assert dispFromCodeStart < 0;
        final int dispSize = 4;
        return dispFromCodeStart - dispPos - dispSize;
    }

    @HOSTED_ONLY
    public static int[] findDataPatchPosns(MaxTargetMethod source, int dispFromCodeStart) {
        int[] result = {};
        for (int pos = 0; pos < source.codeLength(); pos++) {
            for (CiRegister reg : ARMV7.cpuRegisters) {
                final int extraOffset = 12;
                ARMV7Assembler asm = new ARMV7Assembler(target(), null);
                asm.setUpScratch(CiAddress.Placeholder);
                asm.addRegisters(ConditionFlag.Always, false, ARMV7.r12, ARMV7.r12, ARMV7.r15, 0, 0);
                asm.ldr(ConditionFlag.Always, reg, r12, 0);
                int dispPos = pos + asm.codeBuffer.position() - extraOffset - 4;
                int disp = movqDisp(dispPos, dispFromCodeStart - extraOffset);
                asm.codeBuffer.reset();
                asm.movImm32(ConditionFlag.Always, ARMV7.r12, disp);
                asm.addRegisters(ConditionFlag.Always, false, ARMV7.r12, ARMV7.r12, ARMV7.r15, 0, 0);
                asm.ldr(ConditionFlag.Always, reg, r12, 0);
                byte[] pattern = asm.codeBuffer.close(true);
                byte[] instr = Arrays.copyOfRange(source.code(), pos, pos + pattern.length);
                if (Arrays.equals(pattern, instr)) {
                    result = Arrays.copyOf(result, result.length + 1);
                    result[result.length - 1] = dispPos;
                }
            }
        }
        assert result.length != 0;
        return result;
    }

    static class PatchInfoARMV7 extends PatchInfo {

        /**
         * Denotes a conditional jump patch. Encoding: {@code cc, pos, targetBCI}.
         */
        static final int JCC = 0;

        /**
         * Denotes an unconditional jump patch. Encoding: {@code pos, targetBCI}.
         */
        static final int JMP = 1;

        /**
         * Denotes a signed int jump table entry. Encoding: {@code pos, jumpTablePos, targetBCI}.
         */
        static final int JUMP_TABLE_ENTRY = 2;

        /**
         * Denotes a signed int jump table entry. Encoding: {@code pos, key, lookupTablePos, targetBCI}.
         */
        static final int LOOKUP_TABLE_ENTRY = 3;

        /**
         * Denotes a movq instruction that loads an object literal. Encoding: {@code dispPos, index}.
         */
        static final int OBJECT_LITERAL = 4;

        void addJCC(ConditionFlag cc, int pos, int targetBCI) {
            ensureCapacity(size + 4);
            data[size++] = JCC;
            data[size++] = cc.ordinal();
            data[size++] = pos;
            data[size++] = targetBCI;
        }

        void addJMP(int pos, int targetBCI) {
            ensureCapacity(size + 3);
            data[size++] = JMP;
            data[size++] = pos;
            data[size++] = targetBCI;
        }

        void addJumpTableEntry(int pos, int jumpTablePos, int targetBCI) {
            ensureCapacity(size + 4);
            data[size++] = JUMP_TABLE_ENTRY;
            data[size++] = pos;
            data[size++] = jumpTablePos;
            data[size++] = targetBCI;
        }

        void addLookupTableEntry(int pos, int key, int lookupTablePos, int targetBCI) {
            ensureCapacity(size + 5);
            data[size++] = LOOKUP_TABLE_ENTRY;
            data[size++] = pos;
            data[size++] = key;
            data[size++] = lookupTablePos;
            data[size++] = targetBCI;
        }

        void addObjectLiteral(int dispPos, int index) {
            ensureCapacity(size + 3);
            data[size++] = OBJECT_LITERAL;
            data[size++] = dispPos;
            data[size++] = index;
        }
    }

    @Override
    protected Kind invokeKind(SignatureDescriptor signature) {
        Kind returnKind = signature.resultKind();
        if (returnKind.stackKind == Kind.INT) {
            return Kind.WORD;
        }
        return returnKind;
    }

    @Override
    protected void do_dup() {
        incStack(1);
        peekWord(ARMV7.r8, 1);
        pokeWord(ARMV7.r8, 0);
    }

    @Override
    protected void do_dup_x1() {
        incStack(1);
        // value1
        peekWord(ARMV7.r8, 1);
        pokeWord(ARMV7.r8, 0);

        // value2
        peekWord(ARMV7.r8, 2);
        pokeWord(ARMV7.r8, 1);

        // value1
        peekWord(ARMV7.r8, 0);
        pokeWord(ARMV7.r8, 2);
    }

    @Override
    protected void do_dup_x2() {
        incStack(1);
        asm.vmov(ConditionFlag.Always, ARMV7.s31, ARMV7.r9, null, CiKind.Float, CiKind.Int);

        // value1
        peekDoubleWord(ARMV7.r8, 1);
        pokeDoubleWord(ARMV7.r8, 0);

        // value2
        peekDoubleWord(ARMV7.r8, 2);
        pokeDoubleWord(ARMV7.r8, 1);

        // value3
        peekDoubleWord(ARMV7.r8, 3);
        pokeDoubleWord(ARMV7.r8, 2);

        // value1
        peekDoubleWord(ARMV7.r8, 0);
        pokeDoubleWord(ARMV7.r8, 3);

        asm.vmov(ConditionFlag.Always, ARMV7.r9, ARMV7.s31, null, CiKind.Int, CiKind.Float);
    }

    @Override
    protected void do_dup2() {
        incStack(2);
        asm.vmov(ConditionFlag.Always, ARMV7.s31, ARMV7.r9, null, CiKind.Float, CiKind.Int);
        peekDoubleWord(ARMV7.r8, 3);
        pokeDoubleWord(ARMV7.r8, 1);
        peekDoubleWord(ARMV7.r8, 2);
        pokeDoubleWord(ARMV7.r8, 0);
        asm.vmov(ConditionFlag.Always, ARMV7.r9, ARMV7.s31, null, CiKind.Int, CiKind.Float);
    }

    @Override
    protected void do_dup2_x1() {
        incStack(2);
        asm.vmov(ConditionFlag.Always, ARMV7.s31, ARMV7.r9, null, CiKind.Float, CiKind.Int);

        // value1
        peekDoubleWord(ARMV7.r8, 2);
        pokeDoubleWord(ARMV7.r8, 0);

        // value2
        peekDoubleWord(ARMV7.r8, 3);
        pokeDoubleWord(ARMV7.r8, 1);

        // value3
        peekDoubleWord(ARMV7.r8, 4);
        pokeDoubleWord(ARMV7.r8, 2);

        // value1
        peekDoubleWord(ARMV7.r8, 0);
        pokeDoubleWord(ARMV7.r8, 3);

        // value2
        peekDoubleWord(ARMV7.r8, 1);
        pokeDoubleWord(ARMV7.r8, 4);

        asm.vmov(ConditionFlag.Always, ARMV7.r9, ARMV7.s31, null, CiKind.Int, CiKind.Float);
    }

    @Override
    protected void do_dup2_x2() {
        incStack(2);
        asm.vmov(ConditionFlag.Always, ARMV7.s31, ARMV7.r9, null, CiKind.Float, CiKind.Int);

        // value1
        peekDoubleWord(ARMV7.r8, 2);
        pokeDoubleWord(ARMV7.r8, 0);

        // value2
        peekDoubleWord(ARMV7.r8, 3);
        pokeDoubleWord(ARMV7.r8, 1);

        // value3
        peekDoubleWord(ARMV7.r8, 4);
        pokeDoubleWord(ARMV7.r8, 2);

        // value4
        peekDoubleWord(ARMV7.r8, 5);
        pokeDoubleWord(ARMV7.r8, 3);

        // value1
        peekDoubleWord(ARMV7.r8, 0);
        pokeDoubleWord(ARMV7.r8, 4);

        // value2
        peekDoubleWord(ARMV7.r8, 1);
        pokeDoubleWord(ARMV7.r8, 5);

        asm.vmov(ConditionFlag.Always, ARMV7.r9, ARMV7.s31, null, CiKind.Int, CiKind.Float);

    }

    @Override
    protected void do_swap() {
        peekWord(ARMV7.r8, 0);
        peekWord(ARMV7.r9, 1);
        pokeWord(ARMV7.r8, 1);
        pokeWord(ARMV7.r9, 0);
    }

    @Override
    protected void do_ldc(int index) {
        PoolConstant constant = cp.at(index);
        switch (constant.tag()) {
            case CLASS: {
                ClassConstant classConstant = (ClassConstant) constant;
                if (classConstant.isResolvableWithoutClassLoading(cp)) {
                    Object mirror = ((ClassActor) classConstant.value(cp, index).asObject()).javaClass();
                    incStack(1);
                    assignObject(ARMV7.r8, mirror);
                    pokeObject(ARMV7.r8, 0);
                } else {
                    start(T1XTemplateTag.LDC$reference);
                    assignObject(0, "guard", cp.makeResolutionGuard(index));
                    finish();
                }
                break;
            }
            case INTEGER: {
                IntegerConstant integerConstant = (IntegerConstant) constant;
                do_iconst(integerConstant.value());
                break;
            }
            case LONG: {
                LongConstant longConstant = (LongConstant) constant;
                do_lconst(longConstant.value());
                break;
            }
            case FLOAT: {
                FloatConstant floatConstant = (FloatConstant) constant;
                do_fconst(floatConstant.value());
                break;
            }
            case DOUBLE: {
                DoubleConstant doubleConstant = (DoubleConstant) constant;
                do_dconst(doubleConstant.value());
                break;
            }
            case STRING: {
                StringConstant stringConstant = (StringConstant) constant;
                do_oconst(stringConstant.value);
                break;
            }
            default: {
                assert false : "ldc for unexpected constant tag: " + constant.tag();
                break;
            }
        }
    }

    @Override
    protected void emit(T1XTemplateTag tag) {
        start(tag);
        finish();
    }
}

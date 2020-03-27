/*
 * Copyright (c) 2017-2019, APT Group, School of Computer Science,
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
 */
package com.oracle.max.vm.ext.t1x.riscv64;

import static com.oracle.max.vm.ext.t1x.T1X.dispFromCodeStart;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.classfile.ErrorContext.*;
import static com.sun.max.vm.compiler.target.Safepoints.*;
import static com.sun.max.vm.stack.JVMSFrameLayout.*;

import com.oracle.max.asm.target.riscv64.RISCV64MacroAssembler.ConditionFlag;
import com.oracle.max.asm.target.riscv64.*;
import com.oracle.max.vm.ext.maxri.*;
import com.oracle.max.vm.ext.t1x.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.riscv64.RISCV64JVMSFrameLayout;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

import java.util.ArrayList;
import java.util.Arrays;

public class RISCV64T1XCompilation extends T1XCompilation {

    protected final RISCV64MacroAssembler asm;
    final PatchInfoRISCV64 patchInfo;

    public RISCV64T1XCompilation(T1X compiler) {
        super(compiler);
        asm = new RISCV64MacroAssembler(target(), null);
        buf = asm.codeBuffer;
        patchInfo = new PatchInfoRISCV64();
    }

    @Override
    protected void initFrame(ClassMethodActor method, CodeAttribute codeAttribute) {
        int maxLocals = codeAttribute.maxLocals;
        int maxStack = codeAttribute.maxStack;
        int maxParams = method.numberOfParameterSlots();
        if (method.isSynchronized() && !method.isStatic()) {
            synchronizedReceiver = maxLocals++;
        }
        frame = new RISCV64JVMSFrameLayout(maxLocals, maxStack, maxParams, T1XTargetMethod.templateSlots());
    }

    public RISCV64MacroAssembler getMacroAssembler() {
        return asm;
    }

    @Override
    public void decStack(int numberOfSlots) {
        assert numberOfSlots > 0;
        asm.add(64, sp, sp, numberOfSlots * JVMS_SLOT_SIZE);
    }

    @Override
    public void incStack(int numberOfSlots) {
        assert numberOfSlots > 0;
        asm.sub(64, sp, sp, numberOfSlots * JVMS_SLOT_SIZE);
    }

    @Override
    protected void adjustReg(CiRegister reg, int delta) {
        asm.increment32(reg, delta);
    }

    @Override
    public void peekObject(CiRegister dst, int index) {
        CiAddress a = spWord(index);
        asm.load(dst, a, CiKind.Object);
    }

    @Override
    public void pokeObject(CiRegister src, int index) {
        CiAddress a = spWord(index);
        asm.store(src, a, CiKind.Object);
    }

    @Override
    public void peekWord(CiRegister dst, int index) {
        CiAddress a = spWord(index);
        asm.load(dst, a, CiKind.Long);
    }

    @Override
    public void pokeWord(CiRegister src, int index) {
        CiAddress a = spWord(index);
        asm.store(src, a, CiKind.Long);
    }

    @Override
    public void peekInt(CiRegister dst, int index) {
        CiAddress a = spInt(index);
        asm.load(dst, a, CiKind.Int);
    }

    @Override
    public void pokeInt(CiRegister src, int index) {
        CiAddress a = spInt(index);
        asm.store(src, a, CiKind.Int);
    }

    @Override
    public void peekLong(CiRegister dst, int index) {
        CiAddress a = spLong(index);
        asm.load(dst, a, CiKind.Long);
    }

    @Override
    public void pokeLong(CiRegister src, int index) {
        CiAddress a = spLong(index);
        asm.store(src, a, CiKind.Long);
    }

    @Override
    public void peekDouble(CiRegister dst, int index) {
        assert dst.isFpu();
        CiAddress a = spLong(index);
        asm.load(dst, a, CiKind.Double);
    }

    @Override
    public void pokeDouble(CiRegister src, int index) {
        assert src.isFpu();
        CiAddress a = spLong(index);
        asm.store(src, a, CiKind.Double);
    }

    @Override
    public void peekFloat(CiRegister dst, int index) {
        assert dst.isFpu();
        CiAddress a = spInt(index);
        asm.load(dst, a, CiKind.Float);
    }

    @Override
    public void pokeFloat(CiRegister src, int index) {
        assert src.isFpu();
        CiAddress a = spInt(index);
        asm.store(src, a, CiKind.Float);
    }

    @Override
    protected void assignObjectReg(CiRegister dst, CiRegister src) {
        // XXX test me
        asm.mov(dst, src);
    }

    @Override
    protected void assignWordReg(CiRegister dst, CiRegister src) {
        asm.mov(dst, src);
    }

    @Override
    public void assignLong(CiRegister dst, long value) {
        asm.mov64BitConstant(dst, value);
    }

    @Override
    protected void assignObject(CiRegister dst, Object value) {
        if (value == null) {
            asm.mov(dst, 0);
            return;
        }

        int index = objectLiterals.size();
        objectLiterals.add(value);
        patchInfo.addObjectLiteral(buf.position(), index);
        asm.auipc(scratch, 0); // this gets patched by fixup
        asm.nop(RISCV64MacroAssembler.PLACEHOLDER_INSTRUCTIONS_FOR_LONG_OFFSETS);
        asm.ldru(64, dst, RISCV64Address.createBaseRegisterOnlyAddress(scratch));
    }

    @Override
    protected void loadInt(CiRegister dst, int index) {
        CiAddress a = localSlot(localSlotOffset(index, Kind.INT));
        asm.load(dst, a, CiKind.Int);
    }

    @Override
    protected void loadLong(CiRegister dst, int index) {
        CiAddress a = localSlot(localSlotOffset(index, Kind.LONG));
        asm.load(dst, a, CiKind.Long);
    }

    @Override
    protected void loadWord(CiRegister dst, int index) {
        CiAddress a = localSlot(localSlotOffset(index, Kind.WORD));
        asm.load(dst, a, CiKind.Long);
    }

    @Override
    protected void loadObject(CiRegister dst, int index) {
        CiAddress a = localSlot(localSlotOffset(index, Kind.WORD));
        asm.load(dst, a, CiKind.Long);
    }

    @Override
    protected void storeInt(CiRegister src, int index) {
        CiAddress a = localSlot(localSlotOffset(index, Kind.INT));
        asm.store(src, a, CiKind.Int);
    }

    @Override
    protected void storeLong(CiRegister src, int index) {
        CiAddress a = localSlot(localSlotOffset(index, Kind.LONG));
        asm.store(src, a, CiKind.Long);
    }

    @Override
    protected void storeWord(CiRegister src, int index) {
        CiAddress a = localSlot(localSlotOffset(index, Kind.WORD));
        asm.store(src, a, CiKind.Long);
    }

    @Override
    protected void storeObject(CiRegister src, int index) {
        CiAddress a = localSlot(localSlotOffset(index, Kind.WORD));
        asm.store(src, a, CiKind.Long);
    }

    @Override
    public void assignInt(CiRegister dst, int value) {
        asm.mov32BitConstant(dst, value);
    }

    @Override
    protected void assignFloat(CiRegister dst, float value) {
        asm.mov64BitConstant(scratch, Float.floatToRawIntBits(value));
        asm.fmvwx(dst, scratch);
    }

    @Override
    protected void do_store(int index, Kind kind) {
        super.do_store(index, kind);
    }

    @Override
    protected void do_load(int index, Kind kind) {
        super.do_load(index, kind);
    }

    @Override
    protected void do_oconst(Object value) {
        super.do_oconst(value);
    }

    @Override
    protected void do_iconst(int value) {
        super.do_iconst(value);
    }

    @Override
    protected void do_fconst(float value) {
        super.do_fconst(value);
    }

    @Override
    protected void do_dconst(double value) {
        super.do_dconst(value);
    }

    @Override
    protected void do_lconst(long value) {
        super.do_lconst(value);
    }

    @Override
    protected void assignDouble(CiRegister dst, double value) {
        asm.mov64BitConstant(scratch, Double.doubleToRawLongBits(value));
        asm.fmvdx(dst, scratch);
    }

    @Override
    protected int callDirect() {
        alignDirectCall(buf.position());
        int causePos = buf.position();
        asm.call();
        int safepointPos = buf.position();
        asm.nop();
        return Safepoints.make(safepointPos, causePos, DIRECT_CALL, TEMPLATE_CALL);
    }

    @Override
    protected int callDirect(int receiverStackIndex) {
        if (receiverStackIndex >= 0) {
            peekObject(RISCV64.a0, receiverStackIndex);
        }
        alignDirectCall(buf.position());
        int causePos = buf.position();
        asm.call();
        int safepointPos = buf.position();
        asm.nop(); // nop separates any potential safepoint emitted as a successor to the call
        return Safepoints.make(safepointPos, causePos, DIRECT_CALL, TEMPLATE_CALL);
    }

    @Override
    protected int callIndirect(CiRegister target, int receiverStackIndex) {
        if (receiverStackIndex >= 0) {
            if (target == RISCV64.a0) {
                asm.mov(asm.scratchRegister, target);
                target = asm.scratchRegister;
            }
            peekObject(RISCV64.a0, receiverStackIndex);
        }
        int causePos = buf.position();
        asm.call(target);
        int safepointPos = buf.position();
        asm.nop(); // nop separates any potential safepoint emitted as a successor to the call
        return Safepoints.make(safepointPos, causePos, INDIRECT_CALL, TEMPLATE_CALL);
    }

    @Override
    protected void nullCheck(CiRegister src) {
        asm.nullCheck(src);
    }

    private void alignDirectCall(int callPos) {
        asm.alignForPatchableDirectCall(callPos);
    }

    /**
     * Return the displacement which when subtracted from the stack pointer address
     * will position the frame pointer between the non-parameter java locals and the
     * Maxine T1X template slots (see frame layout).
     *
     * @return
     */
    private int framePointerAdjustment() {
        /*
         * Frame size minus the slot used for the callers frame pointer.
         */
        final int enterSize = frame.frameSize() - JVMS_SLOT_SIZE;
        return enterSize - frame.sizeOfNonParameterLocals();
    }

    @Override
    protected Adapter emitPrologue() {
        Adapter adapter = null;
        if (adapterGenerator != null) {
            adapter = adapterGenerator.adapt(method, asm);
        }

        int frameSize = frame.frameSize();
        /*
         * We could <?> use STP here to stack the LR and FP (and LDP later to unstack). That may however affect some
         * of the other machinery e.g. stack walking if there is the assumption that the caller's FP lives
         * in a single JVMS_STACK_SLOT.
         */
        asm.push(64, RISCV64.ra);
        asm.push(64, RISCV64.fp);
        asm.sub(64, RISCV64.fp, RISCV64.sp, framePointerAdjustment()); // fp set relative to sp
        /*
         * Extend the stack pointer past the frame size minus the slot used for the callers
         * frame pointer.
         */
        asm.sub(64, RISCV64.sp, RISCV64.sp, frameSize - JVMS_SLOT_SIZE);


        if (Trap.STACK_BANGING) {
            int pageSize = platform().pageSize;
            int framePages = frameSize / pageSize;
            // emit multiple stack bangs for methods with frames larger than a page
            for (int i = 0; i <= framePages; i++) {
                int offset = (i + VmThread.STACK_SHADOW_PAGES) * pageSize;
                // Deduct 'frameSize' to handle frames larger than (VmThread.STACK_SHADOW_PAGES * pageSize)
                offset = offset - frameSize;
                asm.bangStackWithOffset(offset);
            }
        }
        if (T1XOptions.DebugMethods) {
            int a = debugMethodWriter.getNextID();
            asm.mov64BitConstant(scratch, a);
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
        int dispPos = buf.position();
        asm.auipc(scratch, 0); // this gets patched by fixup
        asm.nop(RISCV64MacroAssembler.PLACEHOLDER_INSTRUCTIONS_FOR_LONG_OFFSETS);
        asm.str(64, RISCV64.zr, RISCV64Address.createBaseRegisterOnlyAddress(scratch));
        patchInfo.addObjectLiteral(dispPos, protectionLiteralIndex);
    }

    @Override
    protected void emitEpilogue() {
        // rewind stack pointer
        asm.add(64, RISCV64.sp, RISCV64.fp, framePointerAdjustment());
        asm.pop(64, RISCV64.fp, true);
        asm.pop(64, RISCV64.ra, true);
        asm.add(64, RISCV64.sp, RISCV64.sp, frame.sizeOfParameters());
        asm.ret(RISCV64.ra);
        if (T1XOptions.DebugMethods) {
            try {
                debugMethodWriter.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * According to JSR133:
     *  for a volatile store we need to issue StoreStore then StoreLoad
     *  for a volatile read we need to issue LoadLoad then LoadStore.
     * (non-Javadoc)
     * @see com.oracle.max.vm.ext.t1x.T1XCompilation#do_preVolatileFieldAccess(com.oracle.max.vm.ext.t1x.T1XTemplateTag, com.sun.max.vm.actor.member.FieldActor)
     */
    @Override
    protected void do_preVolatileFieldAccess(T1XTemplateTag tag, FieldActor fieldActor) {
        // TODO DOUBLE CHECK THIS IS CORRECT
        if (fieldActor.isVolatile()) {
            boolean isWrite = tag.opcode == Bytecodes.PUTFIELD || tag.opcode == Bytecodes.PUTSTATIC;
            asm.fence(0b1111, 0b1111);
        }
    }

    @Override
    protected void do_postVolatileFieldAccess(T1XTemplateTag tag, FieldActor fieldActor) {
        // TODO DOUBLE CHECK THIS IS CORRECT
        if (fieldActor.isVolatile()) {
            boolean isWrite = tag.opcode == Bytecodes.PUTFIELD || tag.opcode == Bytecodes.PUTSTATIC;
            asm.fence(0b1111, 0b1111);
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

        asm.mov64BitConstant(scratch, lowMatch);

        asm.pop(32, scratch1, false); // Pop index from stack

        // Jump to default target if index is not within the jump table
        startBlock(ts.defaultTarget());
        int pos = buf.position();
        // Check if index is lower than lowMatch and branch
        patchInfo.addJCC(RISCV64MacroAssembler.ConditionFlag.LT, pos, ts.defaultTarget(), scratch1, scratch);
        jcc(RISCV64MacroAssembler.ConditionFlag.LT, 0, scratch1, scratch, true);

        // index = index - lowMatch
        asm.sub(scratch1, scratch1, scratch);

        // mov64BitConstant uses scratch1...
        asm.push(64, scratch1);
        asm.mov64BitConstant(scratch, highMatch - lowMatch);
        asm.pop(64, scratch1, true);

        pos = buf.position();
        // Check if index is higher than highMatch and branch
        patchInfo.addJCC(RISCV64MacroAssembler.ConditionFlag.GT, pos, ts.defaultTarget(), scratch1, scratch);
        jcc(RISCV64MacroAssembler.ConditionFlag.GT, 0, scratch1, scratch, true);
        pos = buf.position();

        /*
         * Get the base address of the jump table in scratch2. Use the key (in scratch1)
         * to generate the address of the jump table offset we want and write that back
         * into the scratch1 register. Add the jump table base address to the offset and
         * jump to it.
         */
        asm.auipc(scratch2, 0); // Get the jump table address
        final int adrPos = buf.position();
        asm.add(64, scratch2, scratch2, 0);
        asm.slli(scratch1, scratch1, 2); // Multiply by 4 to get actual label offset
        asm.add(scratch1, scratch2, scratch1); //Add label offset to jump table address
        asm.load(scratch1, RISCV64Address.createBaseRegisterOnlyAddress(scratch1), CiKind.Int);
        asm.add(scratch2, scratch2, scratch1); // Add target offset to jump table address to get the target address
        asm.jalr(RISCV64.x0, scratch2, 0);

        int jumpTablePos = buf.position();
        buf.setPosition(adrPos);
        asm.add(64, scratch2, scratch2, jumpTablePos - adrPos + 4);
        buf.setPosition(jumpTablePos);

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
        codeAnnotations.add(new CiTargetMethod.JumpTable(pos, ts.lowKey(), ts.highKey(), 4));
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
                jmp(target, false);
            } else {
                patchInfo.addJMP(buf.position(), targetBCI);
                jmp(0, true);
            }
        } else {
            asm.pop(32, scratch, false);  // Pop the key we are looking for

            asm.push(64, RISCV64.s3); // Use s3/x19 as the loop counter
            asm.push(64, RISCV64.s4); // Use s4/x20 as the current key

            asm.auipc(scratch2, 0);  // lookup table base
            int adrPos = buf.position();
            asm.add(64, scratch2, scratch2, 0);

            // Initialize loop counter to number of cases x2 to account for pairs of integers (key-offset)
            asm.mov32BitConstant(RISCV64.s3, (ls.numberOfCases() - 1) * 2);

            int loopPos = buf.position();
            asm.slli(scratch1, RISCV64.s3, 2); // Multiply by 4 to get actual label offset
            asm.add(scratch1, scratch2, scratch1);
            asm.load(RISCV64.s4, RISCV64Address.createBaseRegisterOnlyAddress(scratch1), CiKind.Int);
            int branchPos = buf.position();
            asm.beq(scratch, RISCV64.s4, 0);                             // break out of loop
            asm.sub(64, RISCV64.s3, RISCV64.s3, 2);              // decrement loop counter (1 pair at a time)
            jcc(ConditionFlag.GE, loopPos, RISCV64.s3, RISCV64.zr, false);                         // iterate again if >= 0
            startBlock(ls.defaultTarget());                         // No match, jump to default target
            asm.pop(64, RISCV64.s4, true);                                   // after restoring registers r20
            asm.pop(64, RISCV64.s3, true);                                   // and r19.
            patchInfo.addJMP(buf.position(), ls.defaultTarget());
            jmp(0, true);

            // Patch b instruction above
            int branchTargetPos = buf.position();
            buf.setPosition(branchPos);
            asm.beq(scratch, RISCV64.s4, branchTargetPos - branchPos);
            buf.setPosition(branchTargetPos);

            // load offset, add to lookup table base and jump.
            asm.add(64, RISCV64.s3, RISCV64.s3, 1); // increment r19 to get the offset (instead of the key)
            asm.slli(scratch1, RISCV64.s3, 2); // Multiply by 4 to get actual label offset
            asm.add(scratch1, scratch2, scratch1);
            asm.load(scratch, RISCV64Address.createBaseRegisterOnlyAddress(scratch1), CiKind.Int);
            asm.add(scratch, scratch, scratch2);
            asm.pop(64, RISCV64.s4, true);
            asm.pop(64, RISCV64.s3, true);
            asm.jalr(RISCV64.zr, scratch, 0);
            int lookupTablePos = buf.position();

            // Patch adr instruction above now that we know the position of the jump table
            buf.setPosition(adrPos);
            asm.add(64, scratch2, scratch2, lookupTablePos - adrPos + 4);
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
            codeAnnotations.add(new CiTargetMethod.LookupTable(lookupTablePos, ls.numberOfCases(), 4, 4));
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

        if (stream.nextBCI() == targetBCI && methodProfileBuilder == null) {
            // Skip completely if target is next instruction and profiling is turned off
            decStack(1);
            return;
        }

        CiRegister reg1 = null;
        CiRegister reg2 = null;
        switch (opcode) {
            case Bytecodes.IFEQ:
                peekInt(scratch, 0);
                decStack(1);
                reg1 = scratch;
                reg2 = RISCV64.zero;
                cc = ConditionFlag.EQ;
                break;
            case Bytecodes.IFNE:
                peekInt(scratch, 0);
                decStack(1);
                reg1 = scratch;
                reg2 = RISCV64.zero;
                cc = ConditionFlag.NE;
                break;
            case Bytecodes.IFLE:
                peekInt(scratch, 0);
                decStack(1);
                reg1 = scratch;
                reg2 = RISCV64.zero;
                cc = ConditionFlag.LE;
                break;
            case Bytecodes.IFLT:
                peekInt(scratch, 0);
                decStack(1);
                reg1 = scratch;
                reg2 = RISCV64.zero;
                cc = ConditionFlag.LT;
                break;
            case Bytecodes.IFGE:
                peekInt(scratch, 0);
                decStack(1);
                reg1 = scratch;
                reg2 = RISCV64.zero;
                cc = ConditionFlag.GE;
                break;
            case Bytecodes.IFGT:
                peekInt(scratch, 0);
                decStack(1);
                reg1 = scratch;
                reg2 = RISCV64.zero;
                cc = ConditionFlag.GT;
                break;
            case Bytecodes.IF_ICMPEQ:
                peekInt(scratch, 1);
                peekInt(scratch2, 0);
                decStack(2);
                reg1 = scratch;
                reg2 = scratch2;
                cc = ConditionFlag.EQ;
                break;
            case Bytecodes.IF_ICMPNE:
                peekInt(scratch, 1);
                peekInt(scratch2, 0);
                decStack(2);
                reg1 = scratch;
                reg2 = scratch2;
                cc = ConditionFlag.NE;
                break;
            case Bytecodes.IF_ICMPGE:
                peekInt(scratch, 1);
                peekInt(scratch2, 0);
                decStack(2);
                reg1 = scratch;
                reg2 = scratch2;
                cc = ConditionFlag.GE;
                break;
            case Bytecodes.IF_ICMPGT:
                peekInt(scratch, 1);
                peekInt(scratch2, 0);
                decStack(2);
                reg1 = scratch;
                reg2 = scratch2;
                cc = ConditionFlag.GT;
                break;
            case Bytecodes.IF_ICMPLE:
                peekInt(scratch, 1);
                peekInt(scratch2, 0);
                decStack(2);
                reg1 = scratch;
                reg2 = scratch2;
                cc = ConditionFlag.LE;
                break;
            case Bytecodes.IF_ICMPLT:
                peekInt(scratch, 1);
                peekInt(scratch2, 0);
                decStack(2);
                reg1 = scratch;
                reg2 = scratch2;
                cc = ConditionFlag.LT;
                break;
            case Bytecodes.IF_ACMPEQ:
                peekObject(scratch, 1);
                peekObject(scratch2, 0);
                decStack(2);
                reg1 = scratch;
                reg2 = scratch2;
                cc = ConditionFlag.EQ;
                break;
            case Bytecodes.IF_ACMPNE:
                peekObject(scratch, 1);
                peekObject(scratch2, 0);
                decStack(2);
                reg1 = scratch;
                reg2 = scratch2;
                cc = ConditionFlag.NE;
                break;
            case Bytecodes.IFNULL:
                peekObject(scratch, 0);
                decStack(1);
                reg1 = scratch;
                reg2 = RISCV64.zero;
                cc = ConditionFlag.EQ;
                break;
            case Bytecodes.IFNONNULL:
                peekObject(scratch, 0);
                decStack(1);
                reg1 = scratch;
                reg2 = RISCV64.zero;
                cc = ConditionFlag.NE;
                break;
            case Bytecodes.GOTO:
            case Bytecodes.GOTO_W:
                cc = null;
                break;
            default:
                throw new InternalError("Unknown branch opcode: " + Bytecodes.nameOf(opcode));
        }

        int pos = buf.position();

        if (bci < targetBCI) {
            // forward branch
            if (cc == null) {
                patchInfo.addJMP(pos, targetBCI);
                // For now we assume that the target is within a range which
                // can be represented with a signed 21bit offset.
                jmp(buf.position(), true);
            } else {
                patchInfo.addJCC(cc, pos, targetBCI, reg1, reg2);
                jcc(cc, 0, reg1, reg2, true);
            }
        } else {
            // backward branch
            final int target = bciToPos[targetBCI];
            if (cc == null) {
                jmp(target, false);
            } else {
                jcc(cc, target, reg1, reg2, false);
            }
        }
    }

    /**
     * Jump (unconditionally branch) to a target address. The target address must be within a
     * 32 bit range of the program counter.
     * @param target
     */
    protected void jmp(int target, boolean addPatchNops) {
        if (addPatchNops) {
            // Patching this might use 2 instructions if the offset is larger than 20 bits
            asm.nop(PATCH_BRANCH_UNCONDITIONALLY_NOPS);
        } else {
            asm.b(target - buf.position());
        }
    }

    /**
     * Conditional branch to target. Branch immediate takes a signed 12bit address so we can
     * only branch to +-4Kib of the program counter with the branch instruction alone.
     * @param cc
     * @param target
     * @param rs1
     * @param rs2
     */
    protected void jcc(ConditionFlag cc, int target, CiRegister rs1, CiRegister rs2, boolean addPatchNops) {
        if (addPatchNops) {
            // Patching this might use 3 instructions if the offset is larger than 12 bits
            asm.nop(PATCH_BRANCH_CONDITIONALLY_NOPS);
        } else {
            if (RISCV64MacroAssembler.isArithmeticImmediate(target - buf.position())) {
                asm.emitConditionalBranch(cc, rs1, rs2, target - buf.position());
            } else {
                asm.emitConditionalBranch(cc.negate(), rs1, rs2, 3 * RISCV64MacroAssembler.INSTRUCTION_SIZE);
                int offset = target - buf.position();
                asm.insert32BitJump(offset);
            }
        }
    }

    @Override
    protected void addObjectLiteralPatch(int index, int patchPos) {
        patchInfo.addObjectLiteral(patchPos, index);
    }

    @Override
    protected void fixup() {
        int i = 0;
        int[] data = patchInfo.data;
        while (i < patchInfo.size) {
            int tag = data[i++];
            if (tag == PatchInfoRISCV64.JCC) {
                ConditionFlag cc = ConditionFlag.values()[data[i++]];
                int pos = data[i++];
                int targetBCI = data[i++];
                int target = bciToPos[targetBCI];
                assert target != 0;
                asm.assertIfNops(pos, PATCH_BRANCH_CONDITIONALLY_NOPS);
                CiRegister rs1 = RISCV64.cpuRegisters[data[i++]];
                CiRegister rs2 = RISCV64.cpuRegisters[data[i++]];
                buf.setPosition(pos);
                jcc(cc, target, rs1, rs2, false);
            } else if (tag == PatchInfoRISCV64.JMP) {
                int pos = data[i++];
                int targetBCI = data[i++];
                int target = bciToPos[targetBCI];
                asm.assertIfNops(pos, PATCH_BRANCH_UNCONDITIONALLY_NOPS);
                assert target != 0;
                buf.setPosition(pos);
                jmp(target, false);
            } else if (tag == PatchInfoRISCV64.JUMP_TABLE_ENTRY) {
                int pos = data[i++];
                int jumpTablePos = data[i++];
                int targetBCI = data[i++];
                int target = bciToPos[targetBCI];
                assert target != 0;
                int disp = target - jumpTablePos;
                buf.setPosition(pos);
                buf.emitInt(disp);
            } else if (tag == PatchInfoRISCV64.LOOKUP_TABLE_ENTRY) {
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
            } else if (tag == PatchInfoRISCV64.OBJECT_LITERAL) {
                int dispPos = data[i++];
                int index = data[i++];
                assert objectLiterals.get(index) != null;
                int dispFromCodeStart = dispFromCodeStart(objectLiterals.size(), 0, index, true);
                // create a PC relative address in scratch
                final long offset = dispFromCodeStart - dispPos;
                buf.setPosition(dispPos + 4);
                if (RISCV64MacroAssembler.isArithmeticImmediate(offset)) {
                    asm.addi(scratch, scratch, (int) offset);
                    asm.nop(RISCV64MacroAssembler.PLACEHOLDER_INSTRUCTIONS_FOR_LONG_OFFSETS - 1);
                } else {
                    if ((int) offset != offset) {
                        throw FatalError.unexpected("Offset for patching is larger than 32 bits");
                    }
                    int startPos = buf.position();
                    asm.mov32BitConstant(scratch1, (int) offset);
                    asm.add(64, scratch, scratch, scratch1);
                    int endPos = buf.position();
                    assert endPos - startPos <= RISCV64MacroAssembler.PLACEHOLDER_INSTRUCTIONS_FOR_LONG_OFFSETS * RISCV64MacroAssembler.INSTRUCTION_SIZE : endPos - startPos;
                }
            } else {
                throw new InternalError("Unknown PatchInfoRISCV64." + tag);
            }
        }
    }

    @HOSTED_ONLY
    public static int[] findDataPatchPosns(MaxTargetMethod source, int dispFromCodeStart) {
        int[] result = {};
        for (int pos = 0; pos < source.codeLength(); pos++) {
            for (CiRegister reg : RISCV64.cpuRegisters) {
                RISCV64MacroAssembler asm = new RISCV64MacroAssembler(target(), null);
                asm.auipc(scratch, 0);
                asm.addi(scratch, scratch, dispFromCodeStart - pos);
                asm.nop(RISCV64MacroAssembler.PLACEHOLDER_INSTRUCTIONS_FOR_LONG_OFFSETS - 1);
                asm.ldru(64, reg, RISCV64Address.createBaseRegisterOnlyAddress(scratch));
                // pattern must be compatible with RISCV64InstructionDecoder.patchRelativeInstruction
                byte[] patternAddi = asm.codeBuffer.close(true);

                asm = new RISCV64MacroAssembler(target(), null);
                asm.auipc(scratch, 0);
                int[] movInstr = RISCV64MacroAssembler.mov32BitConstantHelper(scratch1, dispFromCodeStart - pos);
                for (int i = 0; i < movInstr.length; i++) {
                    if (movInstr[i] != 0) {
                        asm.codeBuffer.emitInt(movInstr[i]);
                    } else {
                        asm.codeBuffer.emitInt(RISCV64MacroAssembler.addImmediateHelper(RISCV64.x0, RISCV64.x0, 0));
                    }
                }
                asm.add(scratch, scratch, scratch1);
                // movInstr.length + 1 to account for the mov32bitConstant and add above.
                asm.nop(RISCV64MacroAssembler.PLACEHOLDER_INSTRUCTIONS_FOR_LONG_OFFSETS - (movInstr.length + 1));
                asm.ldru(64, reg, RISCV64Address.createBaseRegisterOnlyAddress(scratch));
                byte[] patternMov32BitConstant = asm.codeBuffer.close(true);

                byte[] instr = Arrays.copyOfRange(source.code(), pos, pos + patternAddi.length);
                if (Arrays.equals(patternAddi, instr)) {
                    result = Arrays.copyOf(result, result.length + 1);
                    result[result.length - 1] = pos;
                } else {
                    if (Arrays.equals(patternMov32BitConstant, instr)) {
                        result = Arrays.copyOf(result, result.length + 1);
                        result[result.length - 1] = pos;
                    }
                }
            }
        }
        return result;
    }


    /**
     * Same variables are declared in RISCV64MacroAssembler. However the values here are incremented by one because
     * T1X patching is done using PatchInfoRISCV64 which has it's own data array and does not use the
     * codebuffer to hold patch data. Therefore, we have to include the codebuffer data in our nops.
     */
    private static final int PATCH_BRANCH_CONDITIONALLY_NOPS = 3;
    private static final int PATCH_BRANCH_UNCONDITIONALLY_NOPS = 2;

    static class PatchInfoRISCV64 extends PatchInfo {

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

        void addJCC(RISCV64MacroAssembler.ConditionFlag cc, int pos, int targetBCI, CiRegister rs1, CiRegister rs2) {
            ensureCapacity(size + 6);
            data[size++] = JCC;
            data[size++] = cc.ordinal();
            data[size++] = pos;
            data[size++] = targetBCI;
            data[size++] = rs1.number;
            data[size++] = rs2.number;
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
        super.do_dup();
    }

    @Override
    protected void do_dup_x1() {
        super.do_dup_x1();
    }

    @Override
    protected void do_dup_x2() {
        super.do_dup_x2();
    }

    @Override
    protected void do_dup2() {
        super.do_dup2();
    }

    @Override
    protected void do_dup2_x1() {
        super.do_dup2_x1();
    }

    @Override
    protected void do_dup2_x2() {
        super.do_dup2_x2();
    }

    @Override
    protected void do_swap() {
        super.do_swap();
    }

    @Override
    protected void emit(T1XTemplateTag tag) {
        start(tag);
        finish();
    }

    public void emitPrologueTests() {
        /*
         * mimics the functionality of T1XCompilation::compile2
         */
        emitPrologue();

        emitUnprotectMethod();

        //do_profileMethodEntry();

        do_methodTraceEntry();

         //do_synchronizedMethodAcquire();

        // int bci = 0;
        // int endBCI = stream.endBCI();
        // while (bci < endBCI) {
        // int opcode = stream.currentBC();
        // processBytecode(opcode);
        // stream.next();
        // bci = stream.currentBCI();
        // }

        // int epiloguePos = buf.position();

        // do_synchronizedMethodHandler(method, endBCI);

        // if (epiloguePos != buf.position()) {
        // bciToPos[endBCI] = epiloguePos;
        // }
    }

    public void do_iaddTests() {
        peekInt(RISCV64.x5, 0);
        decStack(1);
        peekInt(RISCV64.x6, 0);
        decStack(1);
        asm.add(RISCV64.x5, RISCV64.x5, RISCV64.x6);
        incStack(1);
        pokeInt(RISCV64.x5, 0);
    }

    public void do_laddTests() {
        peekLong(RISCV64.x0, 0);
        decStack(2);
        peekLong(RISCV64.x1, 0);
        decStack(2);
        asm.add(RISCV64.x0, RISCV64.x0, RISCV64.x1);
        incStack(2);
        pokeLong(RISCV64.x0, 0);
    }

    public void do_daddTests() {
        peekDouble(RISCV64.f0, 0);
        decStack(2);
        peekDouble(RISCV64.f1, 0);
        decStack(2);
        asm.fadd(64, RISCV64.f0, RISCV64.f0, RISCV64.f1);
        incStack(2);
        pokeDouble(RISCV64.f0, 0);
    }

    public void do_imulTests() {
        peekInt(RISCV64.x5, 0);
        decStack(1);
        peekInt(RISCV64.x6, 0);
        decStack(1);
        asm.mul(RISCV64.x5, RISCV64.x5, RISCV64.x6);
        incStack(1);
        pokeInt(RISCV64.x5, 0);
    }

    public void do_initFrameTests(ClassMethodActor method, CodeAttribute codeAttribute) {
        initFrame(method, codeAttribute);
    }

    public void do_storeTests(int index, Kind kind) {
        do_store(index, kind);
    }

    public void do_loadTests(int index, Kind kind) {
        do_load(index, kind);
    }

    public void do_fconstTests(float value) {
        do_fconst(value);
    }

    public void do_dconstTests(double value) {
        do_dconst(value);
    }

    public void do_iconstTests(int value) {
        do_iconst(value);
    }

    public void do_lconstTests(long value) {
        do_lconst(value);
    }

    public void assignmentTests(CiRegister reg, long value) {
        assignLong(reg, value);
    }

    public void assignDoubleTest(CiRegister reg, double value) {
        assignDouble(reg, value);
    }

    public void assignFloatTest(CiRegister reg, float value) {
        assignFloat(reg, value);
    }

    public void emitEpilogueTests() {
        emitEpilogue();
    }
}

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
package com.oracle.max.vm.ext.t1x.aarch64;

import static com.oracle.max.vm.ext.t1x.T1X.*;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.classfile.ErrorContext.*;
import static com.sun.max.vm.compiler.target.Safepoints.*;
import static com.sun.max.vm.stack.JVMSFrameLayout.*;

import java.util.*;

import com.oracle.max.asm.NumUtil;
import com.oracle.max.asm.target.aarch64.*;
import com.oracle.max.asm.target.aarch64.Aarch64Assembler.*;
import com.oracle.max.vm.ext.maxri.*;
import com.oracle.max.vm.ext.t1x.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiTargetMethod.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.DebugMethodWriter;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.aarch64.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

public class Aarch64T1XCompilation extends T1XCompilation {

    protected final Aarch64MacroAssembler asm;
    final PatchInfoAARCH64 patchInfo;

    public Aarch64T1XCompilation(T1X compiler) {
        super(compiler);
        asm = new Aarch64MacroAssembler(target(), null);
        buf = asm.codeBuffer;
        patchInfo = new PatchInfoAARCH64();
    }

    @Override
    protected void initFrame(ClassMethodActor method, CodeAttribute codeAttribute) {
        int maxLocals = codeAttribute.maxLocals;
        int maxStack = codeAttribute.maxStack;
        int maxParams = method.numberOfParameterSlots();
        if (method.isSynchronized() && !method.isStatic()) {
            synchronizedReceiver = maxLocals++;
        }
        frame = new AARCH64JVMSFrameLayout(maxLocals, maxStack, maxParams, T1XTargetMethod.templateSlots());
    }

    public Aarch64MacroAssembler getMacroAssembler() {
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
        asm.mov(64, dst, src);
    }

    @Override
    protected void assignWordReg(CiRegister dst, CiRegister src) {
        asm.mov(64, dst, src);
    }

    @Override
    protected void assignLong(CiRegister dst, long value) {
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
        asm.adr(scratch, 0); // this gets patched by fixup
        asm.nop(Aarch64MacroAssembler.PLACEHOLDER_INSTRUCTIONS_FOR_LONG_OFFSETS);
        asm.ldr(64, dst, Aarch64Address.createBaseRegisterOnlyAddress(scratch));
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
        asm.mov32BitConstant(scratch, Float.floatToRawIntBits(value));
        asm.fmovCpu2Fpu(32, dst, scratch);
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
        asm.fmovCpu2Fpu(64, dst, scratch);
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
            peekObject(Aarch64.r0, receiverStackIndex);
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
            if (target == Aarch64.r0) {
                asm.mov(64, asm.scratchRegister, target);
                target = asm.scratchRegister;
            }
            peekObject(Aarch64.r0, receiverStackIndex);
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
        asm.alignForPatchableDirectCall();
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
        asm.push(Aarch64.linkRegister);
        asm.push(Aarch64.fp);
        asm.sub(64, Aarch64.fp, Aarch64.sp, framePointerAdjustment()); // fp set relative to sp
        /*
         * Extend the stack pointer past the frame size minus the slot used for the callers
         * frame pointer.
         */
        asm.sub(64, Aarch64.sp, Aarch64.sp, frameSize - JVMS_SLOT_SIZE);


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
        asm.adr(scratch, 0); // this gets patched by fixup
        asm.nop(Aarch64MacroAssembler.PLACEHOLDER_INSTRUCTIONS_FOR_LONG_OFFSETS);
        asm.str(64, Aarch64.zr, Aarch64Address.createBaseRegisterOnlyAddress(scratch));
        patchInfo.addObjectLiteral(dispPos, protectionLiteralIndex);
    }

    @Override
    protected void emitEpilogue() {
        // rewind stack pointer
        asm.add(64, Aarch64.sp, Aarch64.fp, framePointerAdjustment());
        asm.pop(Aarch64.fp);
        asm.pop(Aarch64.linkRegister);
        asm.add(64, Aarch64.sp, Aarch64.sp, frame.sizeOfParameters());
        asm.ret(Aarch64.linkRegister);
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
        // XXX Test me
        if (fieldActor.isVolatile()) {
            boolean isWrite = tag.opcode == Bytecodes.PUTFIELD || tag.opcode == Bytecodes.PUTSTATIC;
            asm.dmb(isWrite ? BarrierKind.STORE_STORE : BarrierKind.LOAD_LOAD);
        }
    }

    @Override
    protected void do_postVolatileFieldAccess(T1XTemplateTag tag, FieldActor fieldActor) {
        // XXX Test me
        if (fieldActor.isVolatile()) {
            boolean isWrite = tag.opcode == Bytecodes.PUTFIELD || tag.opcode == Bytecodes.PUTSTATIC;
            asm.dmb(isWrite ? BarrierKind.STORE_LOAD : BarrierKind.LOAD_STORE);
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

        asm.pop(32, scratch); // Pop index from stack

        // Jump to default target if index is not within the jump table
        asm.cmp(32, scratch, lowMatch); // Check if index is lower than lowMatch
        startBlock(ts.defaultTarget());
        int pos = buf.position();
        patchInfo.addJCC(ConditionFlag.LT, pos, ts.defaultTarget());
        jcc(ConditionFlag.LT, 0);

        // Check if index is higher than highMatch
        if (lowMatch == 0) {
            asm.cmp(32, scratch, highMatch);
        } else {
            asm.sub(32, scratch, scratch, (long) lowMatch);
            asm.cmp(32, scratch, highMatch - lowMatch);
        }
        pos = buf.position();
        patchInfo.addJCC(ConditionFlag.GT, pos, ts.defaultTarget());
        jcc(ConditionFlag.GT, 0);
        pos = buf.position();


        /*
         * Get the base address of the jump table in scratch2. Use the key (in scratch)
         * to generate the address of the jump table offset we want and write that back
         * into the scratch register. Add the jump table base address to the offset and
         * jump to it.
         */
        final int adrPos = buf.position();
        asm.adr(scratch2, 0); // Get the jump table adress
        asm.load(scratch, Aarch64Address.createRegisterOffsetAddress(scratch2, scratch, true), CiKind.Int);
        asm.add(64, scratch2, scratch2, scratch); // Add target offset to jump table address to get the target address
        asm.jmp(scratch2);

        int jumpTablePos = buf.position();
        buf.setPosition(adrPos);
        asm.adr(scratch2, jumpTablePos - adrPos);
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
        codeAnnotations.add(new JumpTable(pos, ts.lowKey(), ts.highKey(), 4));
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
                jmp(target);
            } else {
                patchInfo.addJMP(buf.position(), targetBCI);
                asm.jmp(Aarch64.zr);
            }
        } else {
            asm.pop(32, scratch);  // Pop the key we are looking for

            asm.push(Aarch64.r19); // Use r19 as the loop counter
            asm.push(Aarch64.r20); // Use r20 as the current key

            int adrPos = buf.position();
            asm.adr(scratch2, 0);  // lookup table base

            // Initialize loop counter to number of cases x2 to account for pairs of integers (key-offset)
            asm.mov(Aarch64.r19, (ls.numberOfCases() - 1) * 2);

            int loopPos = buf.position();
            asm.load(Aarch64.r20, Aarch64Address.createRegisterOffsetAddress(scratch2, Aarch64.r19, true), CiKind.Int);
            asm.cmp(32, scratch, Aarch64.r20);
            int branchPos = buf.position();
            asm.b(ConditionFlag.EQ, 0);                             // break out of loop
            asm.subs(32, Aarch64.r19, Aarch64.r19, 2);              // decrement loop counter (1 pair at a time)
            jcc(ConditionFlag.PL, loopPos);                         // iterate again if >= 0
            startBlock(ls.defaultTarget());                         // No match, jump to default target
            asm.pop(Aarch64.r20);                                   // after restoring registers r20
            asm.pop(Aarch64.r19);                                   // and r19.
            patchInfo.addJMP(buf.position(), ls.defaultTarget());
            jmp(0);

            // Patch b instruction above
            int branchTargetPos = buf.position();
            buf.setPosition(branchPos);
            asm.b(ConditionFlag.EQ, branchTargetPos - branchPos);
            buf.setPosition(branchTargetPos);

            // load offset, add to lookup table base and jump.
            asm.add(32, Aarch64.r19, Aarch64.r19, 1); // increment r19 to get the offset (instead of the key)
            asm.load(scratch, Aarch64Address.createRegisterOffsetAddress(scratch2, Aarch64.r19, true), CiKind.Int);
            asm.add(64, scratch, scratch, scratch2);
            asm.pop(Aarch64.r20);
            asm.pop(Aarch64.r19);
            asm.jmp(scratch);
            int lookupTablePos = buf.position();

            // Patch adr instruction above now that we know the position of the jump table
            buf.setPosition(adrPos);
            asm.adr(scratch2, lookupTablePos - adrPos);
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

        if (stream.nextBCI() == targetBCI && methodProfileBuilder == null) {
            // Skip completely if target is next instruction and profiling is turned off
            decStack(1);
            return;
        }

        switch (opcode) {
            case Bytecodes.IFEQ:
                peekInt(scratch, 0);
                decStack(1);
                asm.cmp(32, scratch, 0);
                cc = ConditionFlag.EQ;
                break;
            case Bytecodes.IFNE:
                peekInt(scratch, 0);
                decStack(1);
                asm.cmp(32, scratch, 0);
                cc = ConditionFlag.NE;
                break;
            case Bytecodes.IFLE:
                peekInt(scratch, 0);
                decStack(1);
                asm.cmp(32, scratch, 0);
                cc = ConditionFlag.LE;
                break;
            case Bytecodes.IFLT:
                peekInt(scratch, 0);
                decStack(1);
                asm.cmp(32, scratch, 0);
                cc = ConditionFlag.LT;
                break;
            case Bytecodes.IFGE:
                peekInt(scratch, 0);
                decStack(1);
                asm.cmp(32, scratch, 0);
                cc = ConditionFlag.GE;
                break;
            case Bytecodes.IFGT:
                peekInt(scratch, 0);
                decStack(1);
                asm.cmp(32, scratch, 0);
                cc = ConditionFlag.GT;
                break;
            case Bytecodes.IF_ICMPEQ:
                peekInt(scratch, 1);
                peekInt(scratch2, 0);
                decStack(2);
                asm.cmp(32, scratch, scratch2);
                cc = ConditionFlag.EQ;
                break;
            case Bytecodes.IF_ICMPNE:
                peekInt(scratch, 1);
                peekInt(scratch2, 0);
                decStack(2);
                asm.cmp(32, scratch, scratch2);
                cc = ConditionFlag.NE;
                break;
            case Bytecodes.IF_ICMPGE:
                peekInt(scratch, 1);
                peekInt(scratch2, 0);
                decStack(2);
                asm.cmp(32, scratch, scratch2);
                cc = ConditionFlag.GE;
                break;
            case Bytecodes.IF_ICMPGT:
                peekInt(scratch, 1);
                peekInt(scratch2, 0);
                decStack(2);
                asm.cmp(32, scratch, scratch2);
                cc = ConditionFlag.GT;
                break;
            case Bytecodes.IF_ICMPLE:
                peekInt(scratch, 1);
                peekInt(scratch2, 0);
                decStack(2);
                asm.cmp(32, scratch, scratch2);
                cc = ConditionFlag.LE;
                break;
            case Bytecodes.IF_ICMPLT:
                peekInt(scratch, 1);
                peekInt(scratch2, 0);
                decStack(2);
                asm.cmp(32, scratch, scratch2);
                cc = ConditionFlag.LT;
                break;
            case Bytecodes.IF_ACMPEQ:
                peekObject(scratch, 1);
                peekObject(scratch2, 0);
                decStack(2);
                asm.cmp(64, scratch, scratch2);
                cc = ConditionFlag.EQ;
                break;
            case Bytecodes.IF_ACMPNE:
                peekObject(scratch, 1);
                peekObject(scratch2, 0);
                decStack(2);
                asm.cmp(64, scratch, scratch2);
                cc = ConditionFlag.NE;
                break;
            case Bytecodes.IFNULL:
                peekObject(scratch, 0);
                assignObject(scratch2, null);
                decStack(1);
                asm.cmp(64, scratch, scratch2);
                cc = ConditionFlag.EQ;
                break;
            case Bytecodes.IFNONNULL:
                peekObject(scratch, 0);
                assignObject(scratch2, null);
                decStack(1);
                asm.cmp(64, scratch, scratch2);
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
                jmp(buf.position());
            } else {
                patchInfo.addJCC(cc, pos, targetBCI);
                jcc(cc, 0);
            }
        } else {
            // backward branch
            final int target = bciToPos[targetBCI];
            if (cc == null) {
                jmp(target);
            } else {
                jcc(cc, target);
            }
        }
    }

    /**
     * Jump (unconditionally branch) to a target address. This method will emit code to branch
     * within a 4GB offset from the PC.
     * @param target
     */
    protected void longjmp(int target) {
        asm.adrp(scratch, (target >> 12) - (buf.position() >> 12)); // address of target's page
        asm.add(64, scratch, scratch, (int) (target & 0xFFFL)); // low 12 bits of
        asm.br(scratch);
    }

    /**
     * Jump (unconditionally branch) to a target address. The target address must be within a
     * 28bit range of the program counter.
     * @param target
     */
    protected void jmp(int target) {
        asm.b(target - buf.position());
    }
    /**
     * Conditional branch to target. Branch immediate takes a signed 21bit address so we can
     * only branch to +-1MB of the program counter with the branch instruction alone.
     * TODO: enable conditional branching to a 32bit PC offset. We don't need this apparently.
     * @param cc
     * @param target
     */
    protected void jcc(ConditionFlag cc, int target) {
        asm.b(cc, target - buf.position());
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
            if (tag == PatchInfoAARCH64.JCC) {
                ConditionFlag cc = ConditionFlag.values()[data[i++]];
                int pos = data[i++];
                int targetBCI = data[i++];
                int target = bciToPos[targetBCI];
                assert target != 0;
                buf.setPosition(pos);
                jcc(cc, target);
            } else if (tag == PatchInfoAARCH64.JMP) {
                int pos = data[i++];
                int targetBCI = data[i++];
                int target = bciToPos[targetBCI];
                assert target != 0;
                buf.setPosition(pos);
                jmp(target);
            } else if (tag == PatchInfoAARCH64.JUMP_TABLE_ENTRY) {
                int pos = data[i++];
                int jumpTablePos = data[i++];
                int targetBCI = data[i++];
                int target = bciToPos[targetBCI];
                assert target != 0;
                int disp = target - jumpTablePos;
                buf.setPosition(pos);
                buf.emitInt(disp);
            } else if (tag == PatchInfoAARCH64.LOOKUP_TABLE_ENTRY) {
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
            } else if (tag == PatchInfoAARCH64.OBJECT_LITERAL) {
                int dispPos = data[i++];
                int index = data[i++];
                assert objectLiterals.get(index) != null;
                buf.setPosition(dispPos);
                int dispFromCodeStart = dispFromCodeStart(objectLiterals.size(), 0, index, true);
                // create a PC relative address in scratch
                final long offset = dispFromCodeStart - dispPos;
                if (NumUtil.isSignedNbit(21, offset)) {
                    asm.adr(scratch, (int) offset);
                    asm.nop(Aarch64MacroAssembler.PLACEHOLDER_INSTRUCTIONS_FOR_LONG_OFFSETS);
                } else {
                    asm.adr(scratch, 0);
                    int startPos = buf.position();
                    asm.mov64BitConstant(scratch2, offset);
                    asm.add(64, scratch, scratch, scratch2);
                    int endPos = buf.position();
                    assert endPos - startPos <= Aarch64MacroAssembler.PLACEHOLDER_INSTRUCTIONS_FOR_LONG_OFFSETS * Aarch64MacroAssembler.INSTRUCTION_SIZE : endPos - startPos;
                }
            } else {
                throw new InternalError("Unknown PatchInfoAARCH64." + tag);
            }
        }
        /*
         * Hook fixup to create the trampoline array for the current compilation.
         */
        createTrampolines();
    }

    /**
     * Create trampolines for all calls in the compilation.
     */
    private void createTrampolines() {
        int calls = safepointsBuilder.numberOfCalls();
        // Account for the adapter call, if there is one.
        if (adapterGenerator != null) {
            calls++;
        }
        this.trampolines = asm.trampolines(calls);
    }

    @HOSTED_ONLY
    public static int[] findDataPatchPosns(MaxTargetMethod source, int dispFromCodeStart) {
        int[] result = {};
        for (int pos = 0; pos < source.codeLength(); pos++) {
            for (CiRegister reg : Aarch64.cpuRegisters) {
                Aarch64Assembler asm = new Aarch64Assembler(target(), null);
                asm.adr(scratch, dispFromCodeStart - pos);
                asm.nop(Aarch64MacroAssembler.PLACEHOLDER_INSTRUCTIONS_FOR_LONG_OFFSETS);
                asm.ldr(64, reg, Aarch64Address.createBaseRegisterOnlyAddress(scratch));
                // pattern must be compatible with Aarch64InstructionDecoder.patchRelativeInstruction
                byte[] pattern = asm.codeBuffer.close(true);
                byte[] instr = Arrays.copyOfRange(source.code(), pos, pos + pattern.length);
                if (Arrays.equals(pattern, instr)) {
                    result = Arrays.copyOf(result, result.length + 1);
                    result[result.length - 1] = pos;
                }
            }
        }
        return result;
    }

    static class PatchInfoAARCH64 extends PatchInfo {

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
        peekInt(Aarch64.r0, 0);
        decStack(1);
        peekInt(Aarch64.r1, 0);
        decStack(1);
        asm.add(64, Aarch64.r0, Aarch64.r0, Aarch64.r1);
        incStack(1);
        pokeInt(Aarch64.r0, 0);
    }

    public void do_laddTests() {
        peekLong(Aarch64.r0, 0);
        decStack(2);
        peekLong(Aarch64.r1, 0);
        decStack(2);
        asm.add(64, Aarch64.r0, Aarch64.r0, Aarch64.r1);
        incStack(2);
        pokeLong(Aarch64.r0, 0);
    }

    public void do_daddTests() {
        peekDouble(Aarch64.d0, 0);
        decStack(2);
        peekDouble(Aarch64.d1, 0);
        decStack(2);
        asm.fadd(64, Aarch64.d0, Aarch64.d0, Aarch64.d1);
        incStack(2);
        pokeDouble(Aarch64.d0, 0);
    }

    public void do_imulTests() {
        peekInt(Aarch64.r0, 0);
        decStack(1);
        peekInt(Aarch64.r1, 0);
        decStack(1);
        asm.mul(64, Aarch64.r0, Aarch64.r0, Aarch64.r1);
        incStack(1);
        pokeInt(Aarch64.r0, 0);
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

    public void emitEpilogueTests() {
        emitEpilogue();
    }
}

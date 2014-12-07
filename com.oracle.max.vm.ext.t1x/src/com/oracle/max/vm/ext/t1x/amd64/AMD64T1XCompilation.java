/*
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
package com.oracle.max.vm.ext.t1x.amd64;

import static com.oracle.max.asm.target.amd64.AMD64.*;
import static com.oracle.max.vm.ext.t1x.T1X.*;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.classfile.ErrorContext.*;
import static com.sun.max.vm.compiler.target.Safepoints.*;
import static com.sun.max.vm.stack.JVMSFrameLayout.*;

import java.util.*;

import com.oracle.max.asm.target.amd64.*;
import com.oracle.max.asm.target.amd64.AMD64Assembler.ConditionFlag;
import com.oracle.max.cri.intrinsics.*;
import com.oracle.max.vm.ext.maxri.*;
import com.oracle.max.vm.ext.t1x.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiAddress.Scale;
import com.sun.cri.ci.CiTargetMethod.JumpTable;
import com.sun.cri.ci.CiTargetMethod.LookupTable;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.amd64.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;


public class AMD64T1XCompilation extends T1XCompilation {

    protected final AMD64MacroAssembler asm;
    final PatchInfoAMD64 patchInfo;

    public AMD64T1XCompilation(T1X compiler) {
        super(compiler);
        asm = new AMD64MacroAssembler(target(), null);
        buf = asm.codeBuffer;
        patchInfo = new PatchInfoAMD64();
    }

    @Override
    protected void initFrame(ClassMethodActor method, CodeAttribute codeAttribute) {
        int maxLocals = codeAttribute.maxLocals;
        int maxStack = codeAttribute.maxStack;
        int maxParams = method.numberOfParameterSlots();
        if (method.isSynchronized() && !method.isStatic()) {
            synchronizedReceiver = maxLocals++;
        }
        frame = new AMD64JVMSFrameLayout(maxLocals, maxStack, maxParams, T1XTargetMethod.templateSlots());
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
        asm.movq(dst, spWord(index));
    }

    @Override
    public void pokeObject(CiRegister src, int index) {
        asm.movq(spWord(index), src);
    }

    @Override
    public void peekWord(CiRegister dst, int index) {
        asm.movq(dst, spWord(index));
    }

    @Override
    public void pokeWord(CiRegister src, int index) {
        asm.movq(spWord(index), src);
    }

    @Override
    public void peekInt(CiRegister dst, int index) {
        asm.movl(dst, spInt(index));
    }

    @Override
    public void pokeInt(CiRegister src, int index) {
        asm.movl(spInt(index), src);
    }

    @Override
    public void peekLong(CiRegister dst, int index) {
        asm.movq(dst, spLong(index));
    }

    @Override
    public void pokeLong(CiRegister src, int index) {
        asm.movq(spLong(index), src);
    }

    @Override
    public void peekDouble(CiRegister dst, int index) {
        asm.movdbl(dst, spLong(index));
    }

    @Override
    public void pokeDouble(CiRegister src, int index) {
        asm.movdbl(spLong(index), src);
    }

    @Override
    public void peekFloat(CiRegister dst, int index) {
        asm.movflt(dst, spInt(index));
    }

    @Override
    public void pokeFloat(CiRegister src, int index) {
        asm.movflt(spInt(index), src);
    }

    @Override
    protected void assignObjectReg(CiRegister dst, CiRegister src) {
        asm.movq(dst, src);
    }

    @Override
    protected void assignWordReg(CiRegister dst, CiRegister src) {
        asm.movq(dst, src);
    }

    @Override
    protected void assignLong(CiRegister dst, long value) {
        asm.movq(dst, value);
    }

    @Override
    protected void assignObject(CiRegister dst, Object value) {
        if (value == null) {
            asm.xorq(dst, dst);
            return;
        }

        int index = objectLiterals.size();
        objectLiterals.add(value);

        asm.movq(dst, CiAddress.Placeholder);
        int dispPos = buf.position() - 4;
        patchInfo.addObjectLiteral(dispPos, index);
    }

    @Override
    protected void loadInt(CiRegister dst, int index) {
        asm.movl(dst, localSlot(localSlotOffset(index, Kind.INT)));
    }

    @Override
    protected void loadLong(CiRegister dst, int index) {
        asm.movq(dst, localSlot(localSlotOffset(index, Kind.LONG)));
    }

    @Override
    protected void loadWord(CiRegister dst, int index) {
        asm.movq(dst, localSlot(localSlotOffset(index, Kind.WORD)));
    }

    @Override
    protected void loadObject(CiRegister dst, int index) {
        asm.movq(dst, localSlot(localSlotOffset(index, Kind.REFERENCE)));
    }

    @Override
    protected void storeInt(CiRegister src, int index) {
        asm.movl(localSlot(localSlotOffset(index, Kind.INT)), src);
    }

    @Override
    protected void storeLong(CiRegister src, int index) {
        asm.movq(localSlot(localSlotOffset(index, Kind.LONG)), src);
    }

    @Override
    protected void storeWord(CiRegister src, int index) {
        asm.movq(localSlot(localSlotOffset(index, Kind.WORD)), src);
    }

    @Override
    protected void storeObject(CiRegister src, int index) {
        asm.movq(localSlot(localSlotOffset(index, Kind.REFERENCE)), src);
    }

    @Override
    protected void assignInt(CiRegister dst, int value) {
        asm.movl(dst, value);
    }

    @Override
    protected void assignFloat(CiRegister dst, float value) {
        if (value == 0.0f) {
            asm.xorps(dst, dst);
        } else {
            asm.movl(scratch, Float.floatToRawIntBits(value));
            asm.movdl(dst, scratch);
        }
    }

    @Override
    protected void assignDouble(CiRegister dst, double value) {
        if (value == 0.0d) {
            asm.xorpd(dst, dst);
        } else {
            asm.movq(scratch, Double.doubleToRawLongBits(value));
            asm.movdq(dst, scratch);
        }
    }

    @Override
    protected int callDirect() {
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
            peekObject(rdi, receiverStackIndex);
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
        // Align bytecode call site for MT safe patching
        final int alignment = 7;
        final int roundDownMask = ~alignment;
        final int directCallInstructionLength = 5; // [0xE8] disp32
        final int endOfCallSite = callPos + (directCallInstructionLength - 1);
        if ((callPos & roundDownMask) != (endOfCallSite & roundDownMask)) {
            // Emit nops to align up to next 8-byte boundary
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
        asm.enter(frameSize - Word.size(), 0);
        asm.subq(rbp, framePointerAdjustment());
        if (Trap.STACK_BANGING) {
            int pageSize = platform().pageSize;
            int framePages = frameSize / pageSize;
            // emit multiple stack bangs for methods with frames larger than a page
            for (int i = 0; i <= framePages; i++) {
                int offset = (i + VmThread.STACK_SHADOW_PAGES) * pageSize;
                // Deduct 'frameSize' to handle frames larger than (VmThread.STACK_SHADOW_PAGES * pageSize)
                offset = offset - frameSize;
                asm.movq(new CiAddress(WordUtil.archKind(), RSP, -offset), rax);
            }
        }
        return adapter;
    }

    @Override
    protected void emitUnprotectMethod()  {
        protectionLiteralIndex = objectLiterals.size();
        objectLiterals.add(T1XTargetMethod.PROTECTED);

        asm.xorq(scratch, scratch);
        asm.movq(CiAddress.Placeholder, scratch);

        int dispPos = buf.position() - 4;
        patchInfo.addObjectLiteral(dispPos, protectionLiteralIndex);
    }

    @Override
    protected void emitEpilogue() {
        asm.addq(rbp, framePointerAdjustment());
        asm.leave();
        // when returning, retract from the caller stack by the space used for the arguments.
        final short stackAmountInBytes = (short) frame.sizeOfParameters();
        asm.ret(stackAmountInBytes);
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

        // Pop index from stack into rax
        asm.movl(rax, new CiAddress(CiKind.Int, rsp.asValue()));
        asm.addq(rsp, JVMSFrameLayout.JVMS_SLOT_SIZE);

        // Compare index against jump table bounds
        if (lowMatch != 0) {
            // subtract the low value from the switch index
            asm.subl(rax, lowMatch);
            asm.cmpl(rax, highMatch - lowMatch);
        } else {
            asm.cmpl(rax, highMatch);
        }

        startBlock(ts.defaultTarget());
        int pos = buf.position();
        if (methodProfileBuilder == null) {
            // Jump to default target if index is not within the jump table
            patchInfo.addJCC(ConditionFlag.above, pos, ts.defaultTarget());
            asm.jcc(ConditionFlag.above, 0, true);
        } else {
            // If condition is false jump to "not taken" code
            final int placeholderForShortJumpDisp = pos + 2;
            asm.jcc(ConditionFlag.above.negation(), placeholderForShortJumpDisp, false);
            assert buf.position() - pos == 2;

            // Start of "default" code
            int switchProfileIndex = do_ProfileSwitchInit(bci, ts.numberOfCases());
            do_ProfileSwitchDefault(switchProfileIndex, ts.numberOfCases());

            // Jump to default target if index is not within the jump table
            int jmpPos = buf.position();
            patchInfo.addJMP(jmpPos, ts.defaultTarget());
            asm.jmp(0, true);

            // Start of "cases" code
            // Patch the jump to "cases" code now that we know where it is going
            int casesCodePos = buf.position();
            buf.setPosition(pos);
            asm.jcc(ConditionFlag.above.negation(), casesCodePos, false);
            buf.setPosition(casesCodePos);
            do_ProfileSwitchCase(switchProfileIndex, rax);
        }



        // Set r15 to address of jump table
        int leaPos = buf.position();
        asm.leaq(r15, CiAddress.Placeholder);
        int afterLea = buf.position();

        // Load jump table entry into r15 and jump to it
        asm.movslq(rax, new CiAddress(CiKind.Int, r15.asValue(), rax.asValue(), Scale.Times4, 0));
        asm.addq(r15, rax);
        asm.jmp(r15);

        // Inserting padding so that jump table address is 4-byte aligned
        if ((buf.position() & 0x3) != 0) {
            asm.nop(4 - (buf.position() & 0x3));
        }

        // Patch LEA instruction above now that we know the position of the jump table
        int jumpTablePos = buf.position();
        buf.setPosition(leaPos);
        asm.leaq(r15, new CiAddress(WordUtil.archKind(), rip.asValue(), jumpTablePos - afterLea));
        buf.setPosition(jumpTablePos);

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
        int defaultTargetBCI = ls.defaultTarget();
        int numberOfCases = ls.numberOfCases();

        if (numberOfCases == 0) {
            // Pop the key
            decStack(1);
            startBlock(defaultTargetBCI);
            assert defaultTargetBCI > bci;

            if (methodProfileBuilder != null) {
                // Profile switch "default"
                int switchProfileIndex = do_ProfileSwitchInit(bci, numberOfCases);
                do_ProfileSwitchDefault(switchProfileIndex, numberOfCases);
            }
            if (stream.nextBCI() != defaultTargetBCI) {
                // Jump to default target if index is not within the jump table
                patchInfo.addJMP(buf.position(), defaultTargetBCI);
                asm.jmp(0, true);
            }
        } else {
            int switchProfileIndex = methodProfileBuilder.UNDEFINED_INDEX;
            int leaPosProf = methodProfileBuilder.UNDEFINED_POS;
            int afterLeaProf = methodProfileBuilder.UNDEFINED_POS;

            // Pop key from stack into rcx
            asm.movl(rcx, new CiAddress(CiKind.Int, rsp.asValue()));
            asm.addq(rsp, JVMSFrameLayout.JVMS_SLOT_SIZE);

            // Set rbx to address of lookup table
            int leaPos = buf.position();
            asm.leaq(rbx, CiAddress.Placeholder);
            int afterLea = buf.position();

            // Initialize rax to index of the last entry
            asm.movl(rax, numberOfCases - 1);

            int loopPos = buf.position();

            // Compare the value against the key
            asm.cmpl(rcx, new CiAddress(CiKind.Int, rbx.asValue(), rax.asValue(), Scale.Times8, 0));

            // If equal, exit loop
            int matchTestPos = buf.position();
            final int placeholderForShortJumpDisp = matchTestPos + 2;
            asm.jcc(ConditionFlag.equal, placeholderForShortJumpDisp, false);
            assert buf.position() - matchTestPos == 2;

            // Decrement loop var (rax) and jump to top of loop if it did not go below zero (i.e. carry flag was not set)
            asm.subl(rax, 1);
            asm.jcc(ConditionFlag.carryClear, loopPos, false);

            // Jump to default target
            startBlock(defaultTargetBCI);
            if (methodProfileBuilder != null) {
                // Profile switch "default"
                switchProfileIndex = do_ProfileSwitchInit(bci, numberOfCases);
                do_ProfileSwitchDefault(switchProfileIndex, numberOfCases);
            }
            patchInfo.addJMP(buf.position(), defaultTargetBCI);
            asm.jmp(0, true);

            // Patch the first conditional branch instruction above now that we know where's it's going
            int matchPos = buf.position();
            buf.setPosition(matchTestPos);
            asm.jcc(ConditionFlag.equal, matchPos, false);
            buf.setPosition(matchPos);

            // Load jump case table entry into rbx and jump to it
            if (methodProfileBuilder != null) {
                // Profile switch "case"
                do_ProfileSwitchCase(switchProfileIndex, rax);
                // Reset rbx to address of lookup table as it may be killed during call
                leaPosProf = buf.position();
                asm.leaq(rbx, CiAddress.Placeholder);
                afterLeaProf = buf.position();
            }
            asm.movslq(rax, new CiAddress(CiKind.Int, rbx.asValue(), rax.asValue(), Scale.Times8, 4));
            asm.addq(rbx, rax);
            asm.jmp(rbx);

            // Inserting padding so that lookup table address is 4-byte aligned
            while ((buf.position() & 0x3) != 0) {
                asm.nop();
            }

            // Patch the LEA instructions above now that we know the position of the lookup table
            int lookupTablePos = buf.position();
            buf.setPosition(leaPos);
            asm.leaq(rbx, new CiAddress(WordUtil.archKind(), rip.asValue(), lookupTablePos - afterLea));
            if (methodProfileBuilder != null) {
                buf.setPosition(leaPosProf);
                asm.leaq(rbx, new CiAddress(WordUtil.archKind(), rip.asValue(), lookupTablePos - afterLeaProf));
            }
            buf.setPosition(lookupTablePos);

            // Emit lookup table entries
            for (int i = 0; i < numberOfCases; i++) {
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
            codeAnnotations.add(new LookupTable(lookupTablePos, numberOfCases, 4, 4));
        }
    }

    @Override
    protected void cleanup() {
        patchInfo.size = 0;
        super.cleanup();
    }

    /**
     * Constructs code for a branch without profiling instrumentation.
     */
    protected void branchOnConditionWithoutProfile(ConditionFlag cc, int targetBCI, int bci) {
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

            // Compute relative offset
            final int target = bciToPos[targetBCI];
            if (cc == null) {
                do_profileBackwardBranch();
                do_safepointAtBackwardBranch(bci);
                asm.jmp(target, false);
            } else {
                ConditionFlag ccNeg = cc.negation();
                int jumpNotTakenPos = buf.position();
                final int placeholderForShortJumpDisp = jumpNotTakenPos + 2;
                int fallThroughPos;

                // If condition is false jump to "not taken" code
                asm.jcc(ccNeg, placeholderForShortJumpDisp, false);
                assert buf.position() - jumpNotTakenPos == 2;

                // Start of "taken" code
                do_profileBackwardBranch();
                do_safepointAtBackwardBranch(bci);
                asm.jmp(target, false);

                // Start of "not taken" code
                fallThroughPos = buf.position();
                buf.setPosition(jumpNotTakenPos);
                asm.jcc(ccNeg, fallThroughPos, false);
                buf.setPosition(fallThroughPos);
            }
        }
    }

    /**
     * Constructs code for a branch with profiling instrumentation where "not taken" code preceeds "taken" code.
     */
    protected void branchOnConditionWithProfileNotTakenTaken(ConditionFlag cc, int targetBCI, int bci) {
        boolean isForwardBranch = bci < targetBCI;
        boolean isConditionalBranch = cc != null;
        final int relativeOffset = isForwardBranch ? 0 : bciToPos[targetBCI];
        int jumpTakenPos = buf.position();
        int jumpNotTakenPos = buf.position();
        int fallThroughPos;

        if (isConditionalBranch) {
            final int placeholderForShortJumpDisp = jumpTakenPos + 2;
            asm.jcc(cc, placeholderForShortJumpDisp, false);
            assert buf.position() - jumpTakenPos == 2;
        }

        if (isConditionalBranch) {
            // Start of "not taken" code
            final int placeholderForShortJumpDisp;
            do_profileNotTakenBranch(bci);
            jumpNotTakenPos = buf.position();
            placeholderForShortJumpDisp = jumpNotTakenPos + 2;
            asm.jmp(placeholderForShortJumpDisp, false);
            assert buf.position() - jumpNotTakenPos == 2;
        }

        // Start of "taken" code
        if (isConditionalBranch) {
            // Patch the jump to "taken" code now that we know where it is going
            int notTakenCodePos = buf.position();
            buf.setPosition(jumpTakenPos);
            asm.jcc(cc, notTakenCodePos, false);
            buf.setPosition(notTakenCodePos);
        }
        do_profileTakenBranch(bci, targetBCI);
        if (isForwardBranch) {
            int pos = buf.position();
            patchInfo.addJMP(pos, targetBCI);
        } else {
            do_safepointAtBackwardBranch(bci);
        }
        asm.jmp(relativeOffset, isForwardBranch ? true : false);
        assert !isForwardBranch || bciToPos[targetBCI] == 0;

        // Start of "fall through" code
        if (isConditionalBranch) {
            fallThroughPos = buf.position();
            buf.setPosition(jumpNotTakenPos);
            asm.jmp(fallThroughPos, false);
            buf.setPosition(fallThroughPos);
        }

    }

    /**
     * Constructs code for a branch with profiling instrumentation where "taken" code preceeds "not taken" code.
     */
    protected void branchOnConditionWithProfileTakenNotTaken(ConditionFlag cc, int targetBCI, int bci) {
        boolean isForwardBranch = bci < targetBCI;
        boolean isConditionalBranch = cc != null;
        ConditionFlag ccNeg = isConditionalBranch ? cc.negation() : null;
        final int relativeOffset = isForwardBranch ? 0 : bciToPos[targetBCI];
        int jumpNotTakenPos = buf.position();

        if (isConditionalBranch) {
            // If condition is false jump to "not taken" code
            final int placeholderForShortJumpDisp = jumpNotTakenPos + 2;
            asm.jcc(ccNeg, placeholderForShortJumpDisp, false);
            assert buf.position() - jumpNotTakenPos == 2;
        }

        // Start of "taken" code
        do_profileTakenBranch(bci, targetBCI);
        if (isForwardBranch) {
            int pos = buf.position();
            patchInfo.addJMP(pos, targetBCI);
        } else {
            do_safepointAtBackwardBranch(bci);
        }
        asm.jmp(relativeOffset, isForwardBranch ? true : false);
        assert !isForwardBranch || bciToPos[targetBCI] == 0;

        if (isConditionalBranch) {
            // Start of "not taken" code
            // Patch the jump to "not taken" code now that we know where it is going
            int notTakenCodePos = buf.position();
            buf.setPosition(jumpNotTakenPos);
            asm.jcc(ccNeg, notTakenCodePos, false);
            buf.setPosition(notTakenCodePos);
            do_profileNotTakenBranch(bci);
        }
    }

    @Override
    protected void branch(int opcode, int targetBCI, int bci) {
        ConditionFlag cc;

        if (stream.nextBCI() == targetBCI && methodProfileBuilder == null) {
            // Skip completely if target is next instruction and profiling is turned off
            decStack(1);
            return;
        }

        // Important: the compare instructions must come after the stack
        // adjustment instructions as both affect the condition flags.
        switch (opcode) {
            case Bytecodes.IFEQ:
                peekInt(scratch, 0);
                assignInt(scratch2, 0);
                decStack(1);
                asm.cmpl(scratch, scratch2);
                cc = ConditionFlag.equal;
                break;
            case Bytecodes.IFNE:
                peekInt(scratch, 0);
                assignInt(scratch2, 0);
                decStack(1);
                asm.cmpl(scratch, scratch2);
                cc = ConditionFlag.notEqual;
                break;
            case Bytecodes.IFLE:
                peekInt(scratch, 0);
                assignInt(scratch2, 0);
                decStack(1);
                asm.cmpl(scratch, scratch2);
                cc = ConditionFlag.lessEqual;
                break;
            case Bytecodes.IFLT:
                peekInt(scratch, 0);
                assignInt(scratch2, 0);
                decStack(1);
                asm.cmpl(scratch, scratch2);
                cc = ConditionFlag.less;
                break;
            case Bytecodes.IFGE:
                peekInt(scratch, 0);
                assignInt(scratch2, 0);
                decStack(1);
                asm.cmpl(scratch, scratch2);
                cc = ConditionFlag.greaterEqual;
                break;
            case Bytecodes.IFGT:
                peekInt(scratch, 0);
                assignInt(scratch2, 0);
                decStack(1);
                asm.cmpl(scratch, scratch2);
                cc = ConditionFlag.greater;
                break;
            case Bytecodes.IF_ICMPEQ:
                peekInt(scratch, 1);
                peekInt(scratch2, 0);
                decStack(2);
                asm.cmpl(scratch, scratch2);
                cc = ConditionFlag.equal;
                break;
            case Bytecodes.IF_ICMPNE:
                peekInt(scratch, 1);
                peekInt(scratch2, 0);
                decStack(2);
                asm.cmpl(scratch, scratch2);
                cc = ConditionFlag.notEqual;
                break;
            case Bytecodes.IF_ICMPGE:
                peekInt(scratch, 1);
                peekInt(scratch2, 0);
                decStack(2);
                asm.cmpl(scratch, scratch2);
                cc = ConditionFlag.greaterEqual;
                break;
            case Bytecodes.IF_ICMPGT:
                peekInt(scratch, 1);
                peekInt(scratch2, 0);
                decStack(2);
                asm.cmpl(scratch, scratch2);
                cc = ConditionFlag.greater;
                break;
            case Bytecodes.IF_ICMPLE:
                peekInt(scratch, 1);
                peekInt(scratch2, 0);
                decStack(2);
                asm.cmpl(scratch, scratch2);
                cc = ConditionFlag.lessEqual;
                break;
            case Bytecodes.IF_ICMPLT:
                peekInt(scratch, 1);
                peekInt(scratch2, 0);
                decStack(2);
                asm.cmpl(scratch, scratch2);
                cc = ConditionFlag.less;
                break;
            case Bytecodes.IF_ACMPEQ:
                peekObject(scratch, 1);
                peekObject(scratch2, 0);
                decStack(2);
                asm.cmpq(scratch, scratch2);
                cc = ConditionFlag.equal;
                break;
            case Bytecodes.IF_ACMPNE:
                peekObject(scratch, 1);
                peekObject(scratch2, 0);
                decStack(2);
                asm.cmpq(scratch, scratch2);
                cc = ConditionFlag.notEqual;
                break;
            case Bytecodes.IFNULL:
                peekObject(scratch, 0);
                assignObject(scratch2, null);
                decStack(1);
                asm.cmpq(scratch, scratch2);
                cc = ConditionFlag.equal;
                break;
            case Bytecodes.IFNONNULL:
                peekObject(scratch, 0);
                assignObject(scratch2, null);
                decStack(1);
                asm.cmpq(scratch, scratch2);
                cc = ConditionFlag.notEqual;
                break;
            case Bytecodes.GOTO:
            case Bytecodes.GOTO_W:
                cc = null;
                break;
            default:
                throw new InternalError("Unknown branch opcode: " + Bytecodes.nameOf(opcode));

        }
        if (methodProfileBuilder != null) {
            if (cc != null && targetBCI < bci) {
                // For a conditional backward branch a code section for a taken backward branch should be emitted after
                // a code section for a fall through to satisfy the property that a safepoint for non-template code must
                // come after all template code safepoints.
                branchOnConditionWithProfileNotTakenTaken(cc, targetBCI, bci);
            } else {
                branchOnConditionWithProfileTakenNotTaken(cc, targetBCI, bci);
            }
        } else {
            branchOnConditionWithoutProfile(cc, targetBCI, bci);
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
            if (tag == PatchInfoAMD64.JCC) {
                ConditionFlag cc = ConditionFlag.values[data[i++]];
                int pos = data[i++];
                int targetBCI = data[i++];
                int target = bciToPos[targetBCI];
                assert target != 0;
                buf.setPosition(pos);
                asm.jcc(cc, target, true);
            } else if (tag == PatchInfoAMD64.JMP) {
                int pos = data[i++];
                int targetBCI = data[i++];
                int target = bciToPos[targetBCI];
                assert target != 0;
                buf.setPosition(pos);
                asm.jmp(target, true);
            } else if (tag == PatchInfoAMD64.JUMP_TABLE_ENTRY) {
                int pos = data[i++];
                int jumpTablePos = data[i++];
                int targetBCI = data[i++];
                int target = bciToPos[targetBCI];
                assert target != 0;
                int disp = target - jumpTablePos;
                buf.setPosition(pos);
                buf.emitInt(disp);
            } else if (tag == PatchInfoAMD64.LOOKUP_TABLE_ENTRY) {
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
            } else if (tag == PatchInfoAMD64.OBJECT_LITERAL) {
                int dispPos = data[i++];
                int index = data[i++];
                assert objectLiterals.get(index) != null;
                buf.setPosition(dispPos);
                int dispFromCodeStart = dispFromCodeStart(objectLiterals.size(), 0, index, true);
                int disp = movqDisp(dispPos, dispFromCodeStart);
                buf.emitInt(disp);
            } else {
                throw FatalError.unexpected(String.valueOf(tag));
            }
        }
    }

    /**
     * Computes the displacement operand of a {@link AMD64Assembler#movq(CiRegister, CiAddress) movq} instruction that
     * loads data from some memory co-located with the code array in memory.
     *
     * @param dispPos the position of the movq instruction's displacement operand
     * @param dispFromCodeStart the displacement from the start of the code array of the data to load
     * @return the value of the movq displacement operand
     */
    public static int movqDisp(int dispPos, int dispFromCodeStart) {
        assert dispFromCodeStart < 0;
        final int dispSize = 4;
        return dispFromCodeStart - dispPos - dispSize;
    }

    @HOSTED_ONLY
    public static int[] findDataPatchPosns(MaxTargetMethod source, int dispFromCodeStart) {
        int[] result = {};

        for (int pos = 0; pos < source.codeLength(); pos++) {
            for (CiRegister reg : AMD64.cpuxmmRegisters) {
                // Compute displacement operand position for a movq at 'pos'
                AMD64Assembler asm = new AMD64Assembler(target(), null);
                asm.movq(reg, CiAddress.Placeholder);
                int dispPos = pos + asm.codeBuffer.position() - 4;
                int disp = movqDisp(dispPos, dispFromCodeStart);
                asm.codeBuffer.reset();

                // Assemble the movq instruction at 'pos' and compare it to the actual bytes at 'pos'
                CiAddress src = new CiAddress(WordUtil.archKind(), rip.asValue(), disp);
                asm.movq(reg, src);
                byte[] pattern = asm.codeBuffer.close(true);
                byte[] instr = Arrays.copyOfRange(source.code(), pos, pos + pattern.length);
                if (Arrays.equals(pattern, instr)) {
                    result = Arrays.copyOf(result, result.length + 1);
                    result[result.length - 1] = dispPos;
                }
            }

        }
        return result;
    }

    static class PatchInfoAMD64 extends PatchInfo {

        /**
         * Denotes a conditional jump patch.
         * Encoding: {@code cc, pos, targetBCI}.
         */
        static final int JCC = 0;

        /**
         * Denotes an unconditional jump patch.
         * Encoding: {@code pos, targetBCI}.
         */
        static final int JMP = 1;

        /**
         * Denotes a signed int jump table entry.
         * Encoding: {@code pos, jumpTablePos, targetBCI}.
         */
        static final int JUMP_TABLE_ENTRY = 2;

        /**
         * Denotes a signed int jump table entry.
         * Encoding: {@code pos, key, lookupTablePos, targetBCI}.
         */
        static final int LOOKUP_TABLE_ENTRY = 3;

        /**
         * Denotes a movq instruction that loads an object literal.
         * Encoding: {@code dispPos, index}.
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

}

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
package com.oracle.max.vm.ext.t1x.armv7;

import static com.oracle.max.asm.target.armv7.ARMV7.*;
import static com.oracle.max.vm.ext.t1x.T1X.*;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.classfile.ErrorContext.*;
import static com.sun.max.vm.compiler.target.Safepoints.*;
import static com.sun.max.vm.stack.JVMSFrameLayout.*;

import java.util.*;
// Test update APN to check operation of mercurial
import com.oracle.max.asm.target.amd64.AMD64;
import com.oracle.max.asm.target.armv7.*;
import com.oracle.max.asm.target.armv7.ARMV7Assembler.ConditionFlag;
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
import com.sun.max.vm.stack.armv7.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;



/*
APN this is the first attempt to port ARMV7T1X, need to check the behaviour of the ldr instruction as I
believe the offset register can be ignored ... if it cannot then we need to change the instruciton sequence to
something else
the pokes are wrong and need to be changed to stores!!!!


 */
public class ARMV7T1XCompilation extends T1XCompilation {

    protected final ARMV7MacroAssembler asm;
    final PatchInfoARMV7 patchInfo;

    public ARMV7T1XCompilation(T1X compiler) {
        super(compiler);
	System.err.println("Creating ARMV7T1XCompilation");
        asm = new ARMV7MacroAssembler(target(), null);
        buf = asm.codeBuffer;
	System.err.println("code buffer created ");
        patchInfo = new PatchInfoARMV7();
	System.err.println("patchInfo created");
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
        // APN assume that we read a value from the stack into a register
        // spWord returns a CiAddress.
        // asm.movq(dst, spWord(index));
        asm.setUpScratch(spWord(index));
        asm.ldr(ConditionFlag.Always,0,0,0,dst,asm.scratchRegister,ARMV7.r0,0,0);
        // APN need to check these code sequences.
    }

    @Override
    public void pokeObject(CiRegister src, int index) {
        // APN copy value in register to stack
        asm.setUpScratch(spWord(index));
        asm.str(ConditionFlag.Always,0,0,0,src,asm.scratchRegister,ARMV7.r0,0,0);
        //asm.movq(spWord(index), src);
    }

    @Override
    public void peekWord(CiRegister dst, int index) {
        //asm.movq(dst, spWord(index));
        // APN same as peekObject as everything is 32bits.
        System.err.println("ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ");
        asm.setUpScratch(spWord(index));
        asm.ldr(ConditionFlag.Always,0,0,0,dst,asm.scratchRegister,ARMV7.r0,0,0);

    }

    @Override
    public void pokeWord(CiRegister src, int index) {
        // APN same as pokeObject
        //asm.movq(spWord(index), src);
        asm.setUpScratch(spWord(index));
        asm.str(ConditionFlag.Always,0,0,0,src,asm.scratchRegister,ARMV7.r0,0,0);
    }

    @Override
    public void peekInt(CiRegister dst, int index) {

        //asm.movl(dst, spInt(index));
        asm.setUpScratch(spInt(index));
        asm.ldr(ConditionFlag.Always,0,0,0,dst,asm.scratchRegister,ARMV7.r0,0,0);
    }

    @Override
    public void pokeInt(CiRegister src, int index) {
        //asm.movl(spInt(index), src);
        asm.setUpScratch(spInt(index));
        asm.str(ConditionFlag.Always,0,0,0,src,asm.scratchRegister,ARMV7.r0,0,0);
    }

    @Override
    public void peekLong(CiRegister dst, int index) {
        //asm.movq(dst, spLong(index));
        asm.setUpScratch(spLong(index));
        asm.ldr(ConditionFlag.Always,0,0,0,dst,asm.scratchRegister,ARMV7.r0,0,0); //dst needs to be big enough to hold a long!
    }

    @Override
    public void pokeLong(CiRegister src, int index) {
        //asm.movq(spLong(index), src);
        asm.setUpScratch(spLong(index));
        asm.str(ConditionFlag.Always,0,0,0,src,asm.scratchRegister,ARMV7.r0,0,0);
    }

    @Override
    public void peekDouble(CiRegister dst, int index) {
        //asm.movdbl(dst, spLong(index));
        // APN if we use coporocessor REGS then we need to use Coprocesor asm
        asm.setUpScratch(spLong(index));
        asm.ldr(ConditionFlag.Always,0,0,0,dst,asm.scratchRegister,ARMV7.r0,0,0);


    }

    @Override
    public void pokeDouble(CiRegister src, int index) {
        //asm.movdbl(spLong(index), src);
        asm.setUpScratch(spLong(index));
        asm.str(ConditionFlag.Always,0,0,0,src,asm.scratchRegister,ARMV7.r0,0,0);
    }

    @Override
    public void peekFloat(CiRegister dst, int index) {
        asm.setUpScratch(spInt(index));
        asm.ldr(ConditionFlag.Always,0,0,0,dst,asm.scratchRegister,ARMV7.r0,0,0);
        //asm.movflt(dst, spInt(index));
    }

    @Override
    public void pokeFloat(CiRegister src, int index) {
        //asm.movflt(spInt(index), src);
        asm.setUpScratch(spInt(index));
        asm.str(ConditionFlag.Always,0,0,0,src,asm.scratchRegister,ARMV7.r0,0,0);
    }

    @Override
    protected void assignObjectReg(CiRegister dst, CiRegister src) {
        /**
         * Emits code to assign the value in {@code src} to {@code dst}.
         */
        asm.movror(ConditionFlag.Always,false,dst, src,0);
    }

    @Override
    protected void assignWordReg(CiRegister dst, CiRegister src) {
        //asm.movq(dst, src);
        asm.movror(ConditionFlag.Always,false,dst, src,0);
    }

    @Override
    protected void assignLong(CiRegister dst, long value) {
        //asm.movq(dst, value);
        // how are the registers constrianed to correctly use the registers for long?
        //asm.movlong(dst,value);
        asm.movw(ConditionFlag.Always,dst,(int)(value&0xffff));
        asm.movt(ConditionFlag.Always,dst,(int)((value&0xffff0000)>>16));
        asm.movw(ConditionFlag.Always,ARMV7.cpuRegisters[dst.encoding +1],(int)(((value>>32)&0xffff)));
        asm.movt(ConditionFlag.Always,ARMV7.cpuRegisters[dst.encoding +1],(int)(((value>>48)&0xffff)));


    }

    @Override
    protected void assignObject(CiRegister dst, Object value) {
        if (value == null) {
            asm.xorq(dst, dst);
            return;
        }

        int index = objectLiterals.size();
        objectLiterals.add(value);

        //asm.movq(dst, CiAddress.Placeholder);
        asm.mov32BitConstant(ARMV7.r12, 0); // APN so the idea is
        /*  APN
            load the immediate 32bit constant address into r12 (scratch)
            then load the contents of this address into

         */
        asm.ldr(ConditionFlag.Always,0,0,0,dst,ARMV7.r12,ARMV7.r12,0,0);
        int dispPos = buf.position() - 12;// three instructions
        patchInfo.addObjectLiteral(dispPos, index);
    }

    @Override
    protected void loadInt(CiRegister dst, int index) {
        asm.setUpScratch(localSlot(localSlotOffset(index, Kind.INT)));
        asm.ldr(ConditionFlag.Always,0,0,0,dst,ARMV7.r12,ARMV7.r12,0,0);
        //asm.movl(dst, localSlot(localSlotOffset(index, Kind.INT)));
    }

    @Override
    protected void loadLong(CiRegister dst, int index) {
        asm.setUpScratch(localSlot(localSlotOffset(index, Kind.LONG)));
        asm.ldrd(ConditionFlag.Always,dst,asm.scratchRegister,0);
        //asm.movq(dst, localSlot(localSlotOffset(index, Kind.LONG)));
    }

    @Override
    protected void loadWord(CiRegister dst, int index) {
        asm.setUpScratch( localSlot(localSlotOffset(index, Kind.WORD)));
        asm.ldr(ConditionFlag.Always,0,0,0,dst,ARMV7.r12,ARMV7.r12,0,0);
        //asm.movq(dst, localSlot(localSlotOffset(index, Kind.WORD)));
    }

    @Override
    protected void loadObject(CiRegister dst, int index) {
        asm.setUpScratch(localSlot(localSlotOffset(index, Kind.REFERENCE)));
        asm.ldr(ConditionFlag.Always,0,0,0,dst,ARMV7.r12,ARMV7.r12,0,0);
        //asm.movq(dst, localSlot(localSlotOffset(index, Kind.REFERENCE)));
    }

    @Override
    protected void storeInt(CiRegister src, int index) {
        asm.setUpScratch(localSlot(localSlotOffset(index, Kind.INT)));
        asm.str(ConditionFlag.Always,0,0,0,src,asm.scratchRegister,asm.scratchRegister,0,0) ;
        //asm.movl(localSlot(localSlotOffset(index, Kind.INT)), src);
    }

    @Override
    protected void storeLong(CiRegister src, int index) {
        // APN how do we constrain regs to be correct for ARM?
        asm.setUpScratch(localSlot(localSlotOffset(index, Kind.LONG)));
        asm.strd(ConditionFlag.Always,0,0,0,src,asm.scratchRegister,asm.scratchRegister);
        //asm.movq(localSlot(localSlotOffset(index, Kind.LONG)), src);
    }

    @Override
    protected void storeWord(CiRegister src, int index) {
        asm.setUpScratch(localSlot(localSlotOffset(index, Kind.WORD)));
        asm.str(ConditionFlag.Always,0,0,0,src,asm.scratchRegister,asm.scratchRegister,0,0);
        //asm.movq(localSlot(localSlotOffset(index, Kind.WORD)), src);
    }

    @Override
    protected void storeObject(CiRegister src, int index) {
        //
        asm.setUpScratch(localSlot(localSlotOffset(index, Kind.REFERENCE)));
        asm.str(ConditionFlag.Always,0,0,0,src,asm.scratchRegister,asm.scratchRegister,0,0);
        //asm.movq(localSlot(localSlotOffset(index, Kind.REFERENCE)), src);
    }

    @Override
    protected void assignInt(CiRegister dst, int value) {
        asm.mov32BitConstant(dst, value);
    }

    @Override
    protected void assignFloat(CiRegister dst, float value) {
       /* if (value == 0.0f) {
            asm.xorps(dst, dst);
        } else {
            asm.movl(scratch, Float.floatToRawIntBits(value));
            asm.movdl(dst, scratch);
        }
        * APN not implemented
        */
    }

    @Override
    protected void assignDouble(CiRegister dst, double value) {
        /*
        APN not implemented
        if (value == 0.0d) {

            asm.xorpd(dst, dst);
        } else {
            asm.movq(scratch, Double.doubleToRawLongBits(value));
            asm.movdq(dst, scratch);
        }
        */
    }


    /**
            * Emits a direct call instruction whose immediate operand (denoting the absolute or relative offset to the target) will be patched later.
            *
            * @return the {@linkplain Safepoints safepoint} for the call
    */

    @Override
    protected int callDirect() {
        alignDirectCall(buf.position());
        int causePos = buf.position();
        asm.call();
        int safepointPos = buf.position();
        asm.nop(); // nop separates any potential safepoint emitted as a successor to the call
        return Safepoints.make(safepointPos, causePos, DIRECT_CALL, TEMPLATE_CALL);
    }
    /**
     * Emits an indirect call instruction.
     *
     * @param target the register holding the address of the call target
     * @param receiverStackIndex the index of the receiver which must be copied from the stack to the receiver register
     *            used by the optimizing compiler. This is required so that dynamic trampolines can find the receiver in
     *            the expected register. If {@code receiverStackIndex == -1}, then the copy is not emitted as
     *            {@code target} is guaranteed to not be the address of a trampoline.
     * @return the {@linkplain Safepoints safepoint} for the call
     */
    @Override
    protected int callIndirect(CiRegister target, int receiverStackIndex) {
        /* APN
        What is meant by received in this context, do we mean return address?
         */
        if (receiverStackIndex >= 0) {
            peekObject(r0, receiverStackIndex); // was rdi?
        }
        int causePos = buf.position();
        asm.call(target);
        int safepointPos = buf.position();
        asm.nop(); // nop separates any potential safepoint emitted as a successor to the call
        return Safepoints.make(safepointPos, causePos, INDIRECT_CALL, TEMPLATE_CALL);
    }


    @Override
    protected int nullCheck(CiRegister src) {
        // nullCheck on AMD64 testl(AMD64.rax, new CiAddress(Word, r.asValue(Word), 0));
        int safepointPos = buf.position();
        asm.nullCheck(src);
        return Safepoints.make(safepointPos);

    }

    private void alignDirectCall(int callPos) {
        // Align bytecode call site for MT safe patching
        final int alignment = 7;
        final int roundDownMask = ~alignment;
        //final int directCallInstructionLength = 5; // [0xE8] disp32
        final int directCallInstructionLength = 4; // BL on ARM
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
        // APN need to understand the semantics of emitPrologue ...
        // all seems far more complicated than it needs to be for ARM ...
        // discuss with Christos on how we plan to do this to be
        // consistent across Graal T1X ...
        Adapter adapter = null;
        if (adapterGenerator != null) {
            adapter = adapterGenerator.adapt(method, asm);
        }
        // stacksize = imm16
        // push frame pointer
        // framepointer = stackpointer
        // stackptr = framepointer -stacksize
        int frameSize = frame.frameSize();
        //asm.enter(frameSize - Word.size(), 0);
        //asm.enter(frameSize-Word.size());
        asm.push(ConditionFlag.Always, 11<<1 ); // push frame pointer
        asm.movror(ConditionFlag.Always,false,ARMV7.r11,ARMV7.r13,0);  // framepoiter = stack ptr
        asm.mov32BitConstant(ARMV7.r12,frameSize = Word.size());
        asm.sub(ConditionFlag.Always,false,ARMV7.r13,ARMV7.r11,ARMV7.r12,0,0);
        asm.subq(r11, framePointerAdjustment());
        if (Trap.STACK_BANGING) {
            int pageSize = platform().pageSize;
            int framePages = frameSize / pageSize;
            // emit multiple stack bangs for methods with frames larger than a page
            for (int i = 0; i <= framePages; i++) {
                int offset = (i + VmThread.STACK_SHADOW_PAGES) * pageSize;
                // Deduct 'frameSize' to handle frames larger than (VmThread.STACK_SHADOW_PAGES * pageSize)
                offset = offset - frameSize;
                asm.setUpScratch(new CiAddress(WordUtil.archKind(), RSP, -offset));
                asm.str(ConditionFlag.Always,0,0,0,ARMV7.r14,asm.scratchRegister,asm.scratchRegister,0,0);
                // APN guessing rax is return address.
                //asm.movq(new CiAddress(WordUtil.archKind(), RSP, -offset), rax);
            }
        }
        return adapter;
    }

    @Override
    protected void emitUnprotectMethod()  {
        protectionLiteralIndex = objectLiterals.size();
        objectLiterals.add(T1XTargetMethod.PROTECTED);

        asm.xorq(scratch, scratch);
        //asm.movq(CiAddress.Placeholder, scratch);
        buf.emitInt(0);
        buf.emitInt(0);
        int dispPos = buf.position() - 8;
        patchInfo.addObjectLiteral(dispPos, protectionLiteralIndex);
    }

    @Override
    protected void emitEpilogue() {
        asm.addq(ARMV7.r11, framePointerAdjustment());
        asm.leave();
        // stackptr = r11
        // r11 popped off stack
        asm.movror(ConditionFlag.Always,false,ARMV7.r13,ARMV7.r11,0);
        asm.pop(ConditionFlag.Always,1<<ARMV7.r11.encoding ); // pop stack
        // push frame pointer
        // framepointer = stackpointer
        // stackptr = framepointer -stacksize

        // when returning, retract from the caller stack by the space used for the arguments.
        final short stackAmountInBytes = (short) frame.sizeOfParameters();
        //asm.ret(stackAmountInBytes);                  // APN dont know what to do here.
        // ret needs removing or turning into a nop?
        asm.addq(ARMV7.r13,stackAmountInBytes)  ;
        System.err.println("ARM emitEPILOGUE");
        // NOt really sure how much of X86 we need to keep
        // and how much we can get rid off?
    }

    @Override
    protected void do_preVolatileFieldAccess(T1XTemplateTag tag, FieldActor fieldActor) {
        if (fieldActor.isVolatile()) {
            boolean isWrite = tag.opcode == Bytecodes.PUTFIELD || tag.opcode == Bytecodes.PUTSTATIC;
            asm.membar(); // APN we will use the standard DMB instruction thisd might be overkill
                        // and we might need to consider other uses/ways of doing this ....
                        // in order to relax memory access further to account for the R-W W-R W-W semantics
            //asm.membar(isWrite ? MemoryBarriers.JMM_PRE_VOLATILE_WRITE : MemoryBarriers.JMM_PRE_VOLATILE_READ);
        }
    }

    @Override
    protected void do_postVolatileFieldAccess(T1XTemplateTag tag, FieldActor fieldActor) {
        if (fieldActor.isVolatile()) {
            boolean isWrite = tag.opcode == Bytecodes.PUTFIELD || tag.opcode == Bytecodes.PUTSTATIC;
            asm.membar(); // output a DMB instruction
            //asm.membar(isWrite ? MemoryBarriers.JMM_POST_VOLATILE_WRITE : MemoryBarriers.JMM_POST_VOLATILE_READ);
        }
    }

    @Override
    protected void do_tableswitch() {
        // APN this is one way to do an implementation of a switch statement
        // APN TODO lets leave this for now
        int bci = stream.currentBCI();
        BytecodeTableSwitch ts = new BytecodeTableSwitch(stream, bci);
        int lowMatch = ts.lowKey();
        int highMatch = ts.highKey();
        if (lowMatch > highMatch) {
            throw verifyError("Low must be less than or equal to high in TABLESWITCH");
        }

        // pushing and popping should be done using ldm stm?
        // need to look for the usage of such instruction streams in ARM and
        // fix it to be better ....

        // Pop index from stack into rax
        asm.setUpScratch(new CiAddress(CiKind.Int, r13.asValue()));
        asm.ldr(ConditionFlag.Always,0,0,0,r14,asm.scratchRegister,asm.scratchRegister,0,0);
        //asm.movl(r14, new CiAddress(CiKind.Int, r13.asValue()));
        asm.addq(r12, JVMSFrameLayout.JVMS_SLOT_SIZE);

        // Compare index against jump table bounds
        if (lowMatch != 0) {
            // subtract the low value from the switch index
            // APN TODO asm.sub(r14, lowMatch);
            asm.cmpl(r14, highMatch - lowMatch);
        } else {
            asm.cmpl(r14, highMatch);
        }

        // Jump to default target if index is not within the jump table
        startBlock(ts.defaultTarget());
        int pos = buf.position();
        // APN TODO just to let it compile patchInfo.addJCC(ConditionFlag.above, pos, ts.defaultTarget());
        // APN TODO asm.jcc(ConditionFlag.above, 0, true);

        // Set r15 to address of jump table
        int leaPos = buf.position();
        // APN TODO asm.leaq(r15, CiAddress.Placeholder);
        int afterLea = buf.position();

        // Load jump table entry into r15 and jump to it
        // APN TODO asm.movslq(r14, new CiAddress(CiKind.Int, r15.asValue(), r14.asValue(), Scale.Times4, 0));
        // APN TODO asm.add(r15, r14);
        // APN TODO asm.jmp(r15);

        // Inserting padding so that jump table address is 4-byte aligned
        if ((buf.position() & 0x3) != 0) {
            asm.nop(4 - (buf.position() & 0x3));
        }

        // Patch LEA instruction above now that we know the position of the jump table
        int jumpTablePos = buf.position();
        buf.setPosition(leaPos);
        //APN TODO asm.leaq(r15, new CiAddress(WordUtil.archKind(), rip.asValue(), jumpTablePos - afterLea));
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
        //APN TODO do this later, another way of doing a switch
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
         // APN TODO       asm.jmp(target, false);
            } else {
                patchInfo.addJMP(buf.position(), targetBCI);
         // APN TODO       asm.jmp(0, true);
            }
        } else {
            // Pop key from stack into rax
            // APN TODO        asm.movl(rax, new CiAddress(CiKind.Int, rsp.asValue()));
            // APN TODO    asm.addq(rsp, JVMSFrameLayout.JVMS_SLOT_SIZE);

            // Set rbx to address of lookup table
            int leaPos = buf.position();
            // APN TODO    asm.leaq(rbx, CiAddress.Placeholder);
            int afterLea = buf.position();

            // Initialize rcx to index of last entry
            // APN TODO    asm.movl(rcx, (ls.numberOfCases() - 1) * 2);

            int loopPos = buf.position();

            // Compare the value against the key
            // APN TODO    asm.cmpl(rax, new CiAddress(CiKind.Int, rbx.asValue(), rcx.asValue(), Scale.Times4, 0));

            // If equal, exit loop
            int matchTestPos = buf.position();
            final int placeholderForShortJumpDisp = matchTestPos + 2;
            // APN TODO    asm.jcc(ConditionFlag.equal, placeholderForShortJumpDisp, false);
            assert buf.position() - matchTestPos == 2;

            // Decrement loop var (r15) and jump to top of loop if it did not go below zero (i.e. carry flag was not set)
            // APN TODO    asm.subl(rcx, 2);
            // APN TODO    asm.jcc(ConditionFlag.carryClear, loopPos, false);

            // Jump to default target
            startBlock(ls.defaultTarget());
            patchInfo.addJMP(buf.position(), ls.defaultTarget());
            // APN TODO    asm.jmp(0, true);

            // Patch the first conditional branch instruction above now that we know where's it's going
            int matchPos = buf.position();
            buf.setPosition(matchTestPos);
            // APN TODO    asm.jcc(ConditionFlag.equal, matchPos, false);
            buf.setPosition(matchPos);

            // Load jump table entry into r15 and jump to it
            // APN TODO    asm.movslq(rcx, new CiAddress(CiKind.Int, rbx.asValue(), rcx.asValue(), Scale.Times4, 4));
            // APN TODO    asm.addq(rbx, rcx);
            // APN TODO    asm.jmp(rbx);

            // Inserting padding so that lookup table address is 4-byte aligned
            while ((buf.position() & 0x3) != 0) {
                asm.nop();
            }

            // Patch the LEA instruction above now that we know the position of the lookup table
            int lookupTablePos = buf.position();
            buf.setPosition(leaPos);
            // APN TODO    asm.leaq(rbx, new CiAddress(WordUtil.archKind(), rip.asValue(), lookupTablePos - afterLea));
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
    protected void cleanup() {
        patchInfo.size = 0;
        super.cleanup();
    }

    @Override
    protected void branch(int opcode, int targetBCI, int bci) {
        ConditionFlag cc;
        // Important: the compare instructions must come after the stack
        // adjustment instructions as both affect the condition flags.
        switch (opcode) {
            case Bytecodes.IFEQ:
                peekInt(scratch, 0);
                assignInt(scratch2, 0);
                decStack(1);
                asm.cmpl(scratch, scratch2);
                //cc = ConditionFlag.equal;
                cc = ConditionFlag.Equal;
            case Bytecodes.IFNE:
                peekInt(scratch, 0);
                assignInt(scratch2, 0);
                decStack(1);
                asm.cmpl(scratch, scratch2);
                //cc = ConditionFlag.notEqual;
                cc =  ConditionFlag.NotEqual;
                break;
            case Bytecodes.IFLE:
                peekInt(scratch, 0);
                assignInt(scratch2, 0);
                decStack(1);
                asm.cmpl(scratch, scratch2);
                //cc = ConditionFlag.lessEqual;
                cc = ConditionFlag.SignedLowerOrEqual;
            case Bytecodes.IFLT:
                peekInt(scratch, 0);
                assignInt(scratch2, 0);
                decStack(1);
                asm.cmpl(scratch, scratch2);
                //cc = ConditionFlag.less;
                cc = ConditionFlag.SignedLesser;
                break;
            case Bytecodes.IFGE:
                peekInt(scratch, 0);
                assignInt(scratch2, 0);
                decStack(1);
                asm.cmpl(scratch, scratch2);
                //cc = ConditionFlag.greaterEqual;
                cc = ConditionFlag.SignedGreaterOrEqual;
            case Bytecodes.IFGT:
                peekInt(scratch, 0);
                assignInt(scratch2, 0);
                decStack(1);
                asm.cmpl(scratch, scratch2);
                //cc = ConditionFlag.greater;
                cc = ConditionFlag.SignedGreater;
            case Bytecodes.IF_ICMPEQ:
                peekInt(scratch, 1);
                peekInt(scratch2, 0);
                decStack(2);
                asm.cmpl(scratch, scratch2);
                //cc = ConditionFlag.equal;
                cc = ConditionFlag.Equal;
                break;
            case Bytecodes.IF_ICMPNE:
                peekInt(scratch, 1);
                peekInt(scratch2, 0);
                decStack(2);
                asm.cmpl(scratch, scratch2);
                cc = ConditionFlag.NotEqual;
                //cc = ConditionFlag.notEqual;
                break;
            case Bytecodes.IF_ICMPGE:
                peekInt(scratch, 1);
                peekInt(scratch2, 0);
                decStack(2);
                asm.cmpl(scratch, scratch2);
                cc = ConditionFlag.SignedGreaterOrEqual ;
                //cc = ConditionFlag.greaterEqual;
                break;
            case Bytecodes.IF_ICMPGT:
                peekInt(scratch, 1);
                peekInt(scratch2, 0);
                decStack(2);
                asm.cmpl(scratch, scratch2);
                //cc = ConditionFlag.greater;
                cc = ConditionFlag.SignedGreater;
            case Bytecodes.IF_ICMPLE:
                peekInt(scratch, 1);
                peekInt(scratch2, 0);
                decStack(2);
                asm.cmpl(scratch, scratch2);
                cc = ConditionFlag.SignedLowerOrEqual;
                //cc = ConditionFlag.lessEqual;
                break;
            case Bytecodes.IF_ICMPLT:
                peekInt(scratch, 1);
                peekInt(scratch2, 0);
                decStack(2);
                asm.cmpl(scratch, scratch2);
                //cc = ConditionFlag.less;
                cc = ConditionFlag.SignedLesser;
                break;
            case Bytecodes.IF_ACMPEQ:
                peekObject(scratch, 1);
                peekObject(scratch2, 0);
                decStack(2);
                asm.cmpl(scratch, scratch2);
                cc =ConditionFlag.Equal;
                //cc = ConditionFlag.equal;
                break;
            case Bytecodes.IF_ACMPNE:
                peekObject(scratch, 1);
                peekObject(scratch2, 0);
                decStack(2);
                asm.cmpl(scratch, scratch2);
                cc = ConditionFlag.NotEqual;
                //cc= ConditionFlag.notEqual;
                break;
            case Bytecodes.IFNULL:
                peekObject(scratch, 0);
                assignObject(scratch2, null);
                decStack(1);
                asm.cmpl(scratch, scratch2);
                cc = ConditionFlag.Equal;
                //cc = ConditionFlag.equal;
                break;
            case Bytecodes.IFNONNULL:
                peekObject(scratch, 0);
                assignObject(scratch2, null);
                decStack(1);
                asm.cmpl(scratch, scratch2);
                cc = ConditionFlag.NotEqual;
                //cc = ConditionFlag.notEqual;
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
     * Computes the displacement operand of a {@link ARMV7Assembler#movq(CiRegister, CiAddress) movq} instruction that
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
        /* APN
        I don;t know if Im being an idiot but why is it only iterating over the FPregs at each code position then doing the
        move of them?

        We can ignore for now as we are not doing FP?? at least initially
         */

                                                                                                                /*
        for (int pos = 0; pos < source.codeLength(); pos++) {
            for (CiRegister reg : ARMV7.cpuxmmRegisters) {
                // Compute displacement operand position for a movq at 'pos'
                ARMV7Assembler asm = new ARMV7Assembler(target(), null);
                asm.setUpScratch(CiAddress.Placeholder);
                asm.movror(ConditionFlag.Always,false,reg,ARMV7.r12,0);
                int dispPos = pos + asm.codeBuffer.position() - 4;
                int disp = movqDisp(dispPos, dispFromCodeStart);
                asm.codeBuffer.reset(); // APN que? Dont understand why are we continually resetting the buffer?

                // Assemble the movq instruction at 'pos' and compare it to the actual bytes at 'pos'
                CiAddress src = new CiAddress(WordUtil.archKind(), ARMV7.r15.asValue(), disp); // assuming rip in X86 is r15
                asm.setUpScratch(src);
                asm.movror(ConditionFlag.Always,false,reg, ARMV7.r12,0);
                byte[] pattern = asm.codeBuffer.close(true);
                byte[] instr = Arrays.copyOfRange(source.code(), pos, pos + pattern.length);
                if (Arrays.equals(pattern, instr)) {
                    result = Arrays.copyOf(result, result.length + 1);
                    result[result.length - 1] = dispPos;
                }
            }

        }                                                                                                         */

        return result;
    }

    static class PatchInfoARMV7 extends PatchInfo {

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

    public void assignmentTests(CiRegister reg, long value)
    {
         assignLong(reg,value);
        System.err.println("Done");
    }
}

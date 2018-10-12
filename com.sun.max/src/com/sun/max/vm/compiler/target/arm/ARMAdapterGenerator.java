/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.compiler.target.arm;

import static com.sun.cri.ci.CiCallingConvention.Type.*;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.compiler.CallEntryPoint.*;
import static com.sun.max.vm.compiler.deopt.Deoptimization.*;
import static com.sun.max.vm.compiler.target.arm.ARMTargetMethodUtil.*;

import java.io.*;

import com.oracle.max.asm.target.armv7.*;
import com.oracle.max.asm.target.armv7.ARMV7Assembler.*;
import com.oracle.max.cri.intrinsics.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.Adapter.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.type.*;

public abstract class ARMAdapterGenerator extends AdapterGenerator {

    final CiRegister scratch;

    static {
        if (vm().compilationBroker.needsAdapters()) {
            // Create and register the adapter generators
            new Baseline2Opt();
            new Opt2Baseline();
        }
    }

    public ARMAdapterGenerator(Adapter.Type adapterType) {
        super(adapterType);
        scratch = opt.getScratchRegister();
    }

    /**
     * ARMV7 specific generator for {@link Type#BASELINE2OPT} adapters.
     */
    public static class Baseline2Opt extends ARMAdapterGenerator {

        /**
         * ARMV7 specific {@link Type#BASELINE2OPT} adapters.
         */
        public static class Baseline2OptAdapter extends Adapter {

            Baseline2OptAdapter(AdapterGenerator generator, String description, int frameSize, byte[] code, int callPos, int callSize) {
                super(generator, description, frameSize, code, callPos, callSize);
            }

            @Override
            public int callOffsetInPrologue() {
                return 4;
            }

            @Override
            public int callSizeInPrologue() {
                return RIP_CALL_INSTRUCTION_SIZE;
            }

            /**
             * Computes the state of an adapter frame based on an execution point in this adapter.
             * <p/>
             * This complex computation is only necessary for the Inspector as that is the only context in which the
             * current execution point in an adapter can be anywhere except for in the call back to the callee being
             * adapted. Hence the {@link HOSTED_ONLY} annotation.
             *
             * @param cursor a stack frame walker cursor denoting an execution point in this adapter
             * @return the amount by which {@code current.sp()} should be adjusted to obtain the slot holding the
             *         address to which the adapter will return. If the low bit of this value is set, then it means that
             *         the caller's RBP is obtained by reading the word at {@code current.fp()} otherwise it is equal to
             *         {@code current.fp()}.
             */
            @HOSTED_ONLY
            private int computeFrameState(StackFrameCursor cursor) {
                assert false : "Not implemented in ARMv7!";
                int ripAdjustment = frameSize();
                boolean rbpSaved = false;
                StackFrameWalker sfw = cursor.stackFrameWalker();
                int position = cursor.vmIP().minus(codeStart()).toInt();

                byte b = sfw.readByte(cursor.vmIP().toAddress(), 0);
                if (position == 0 || b == ENTER) {
                    ripAdjustment = Word.size();
                } else if (b == RET2) {
                    ripAdjustment = 0;
                } else if (b == REXW) {
                    b = sfw.readByte(cursor.vmIP().toAddress(), 1);
                    if (b == ADDQ_SUBQ_imm8) {
                        ripAdjustment = Word.size();
                    } else if (b == ADDQ_SUBQ_imm32) {
                        ripAdjustment = Word.size();
                    } else {
                        rbpSaved = true;
                    }
                } else {
                    rbpSaved = true;
                }
                assert (ripAdjustment & 1) == 0;
                return ripAdjustment | (rbpSaved ? 1 : 0);
            }

            @Override
            public Pointer returnAddressPointer(StackFrameCursor frame) {
                int ripAdjustment = frameSize();
                if (MaxineVM.isHosted()) {
                    assert false : "Not implemented yet in ARMv7";
                    int state = computeFrameState(frame);
                    ripAdjustment = state & ~1;
                }
                return frame.sp().plus(ripAdjustment);
            }

            @Override
            public void advance(StackFrameCursor current) {
                StackFrameWalker sfw = current.stackFrameWalker();
                int ripAdjustment = frameSize();
                boolean rbpSaved = true;
                if (MaxineVM.isHosted()) {
                    assert false : "Not implemented yet in ARMv7";
                    int state = computeFrameState(current);
                    ripAdjustment = state & ~1;
                    rbpSaved = (state & 1) == 1;
                }

                Pointer ripPointer = current.sp().plus(ripAdjustment);
                Pointer callerIP = sfw.readWord(ripPointer, 0).asPointer();
                Pointer callerSP = ripPointer.plus(Word.size()); // Skip RIP word
                Pointer callerFP = rbpSaved ? sfw.readWord(ripPointer, -Word.size() * 2).asPointer() : current.fp();
                boolean wasDisabled = SafepointPoll.disable();
                sfw.advance(callerIP, callerSP, callerFP);
                if (!wasDisabled) {
                    SafepointPoll.enable();
                }
            }

            @Override
            @HOSTED_ONLY
            public boolean acceptStackFrameVisitor(StackFrameCursor current, StackFrameVisitor visitor) {
                assert false : "Not implemented in ARMv7!";
                int ripAdjustment = computeFrameState(current) & ~1;
                Pointer ripPointer = current.sp().plus(ripAdjustment);
                Pointer fp = ripPointer.minus(frameSize());
                return visitor.visitFrame(new AdapterStackFrame(current.stackFrameWalker().calleeStackFrame(), current.targetMethod(), current.vmIP().toPointer(), fp, current.sp()));
            }

            @Override
            public VMFrameLayout frameLayout() {
                return new Baseline2OptAdapterFrameLayout(frameSize());
            }
        }

        /**
         * A specialization of an ARMV7 specific {@link Type#BASELINE2OPT} adapter that contains a reference map
         * occupying 64 or less bits.
         */
        public static class Baseline2OptAdapterWithRefMap extends Baseline2OptAdapter {

            final long refMap;

            public Baseline2OptAdapterWithRefMap(AdapterGenerator generator, String description, long refMap, int frameSize, byte[] code, int callPos, int callSize) {
                super(generator, description, frameSize, code, callPos, callSize);
                this.refMap = refMap;
            }

            @Override
            public void prepareReferenceMap(StackFrameCursor current, StackFrameCursor callee, FrameReferenceMapVisitor preparer) {
                preparer.logPrepareReferenceMap(this, 0, current.sp(), "frame");
                int frameSlotIndex = preparer.referenceMapBitIndex(current.sp());
                int byteIndex = 0;
                long refMap = this.refMap;
                for (int i = 0; i < 8; i++) {
                    final byte refMapByte = (byte) (refMap & 0xff);
                    preparer.logReferenceMapByteBefore(byteIndex, refMapByte, "Frame");
                    preparer.setBits(frameSlotIndex, refMapByte);
                    preparer.logReferenceMapByteAfter(current.sp(), frameSlotIndex, refMapByte);
                    refMap >>>= Bytes.WIDTH;
                    frameSlotIndex += Bytes.WIDTH;
                    byteIndex++;
                }
            }
        }

        /**
         * A specialization of an ARMV7 specific {@link Type#BASELINE2OPT} adapter that contains a reference map
         * occupying more than 64 bits.
         */
        public static class Baseline2OptAdapterWithBigRefMap extends Baseline2OptAdapter {

            final byte[] refMap;

            public Baseline2OptAdapterWithBigRefMap(AdapterGenerator generator, String description, byte[] refMap, int frameSize, byte[] code, int callPos, int callSize) {
                super(generator, description, frameSize, code, callPos, callSize);
                this.refMap = refMap;
            }

            @Override
            public void prepareReferenceMap(StackFrameCursor current, StackFrameCursor callee, FrameReferenceMapVisitor preparer) {
                preparer.logPrepareReferenceMap(this, 0, current.sp(), "frame");
                int frameSlotIndex = preparer.referenceMapBitIndex(current.sp());
                int byteIndex = 0;
                for (int i = 0; i < refMap.length; i++) {
                    final byte frameReferenceMapByte = refMap[byteIndex];
                    preparer.logReferenceMapByteBefore(byteIndex, frameReferenceMapByte, "Frame");
                    preparer.setBits(frameSlotIndex, frameReferenceMapByte);
                    preparer.logReferenceMapByteAfter(current.sp(), frameSlotIndex, frameReferenceMapByte);
                    frameSlotIndex += Bytes.WIDTH;
                    byteIndex++;
                }
            }
        }

        /**
         * Frame layout for an ARMV7 specific {@link Type#BASELINE2OPT} adapter frame.
         *
         * <pre>
         *
         *          +------------------------+
         *          |  Baseline caller RIP   |
         *          +------------------------+     ---
         *          |     OPT main body      |      ^
         *          +------------------------+      |
         *          |saved X86-RBP ARM-r11   |      |
         *          +------------------------+  frame size
         *          |        OPT arg N       |      |
         *          +------------------------+      |
         *          :                        :      |
         *          +------------------------+      |
         *          |        OPT arg S0      |      |
         *          +------------------------+      |
         *   RSP--> |    deopt rescue slot   |      v
         *          +------------------------+     ---
         *
         *    N == number of args - 1
         *   S0 == first stack arg (S0 == number of register args)
         * </pre>
         */
        public static class Baseline2OptAdapterFrameLayout extends AdapterStackFrameLayout {

            public Baseline2OptAdapterFrameLayout(int frameSize) {
                super(frameSize, true);
            }

            @Override
            public int frameReferenceMapOffset() {
                return 0;
            }

            @Override
            public int frameReferenceMapSize() {
                return ByteArrayBitMap.computeBitMapSize(UnsignedMath.divide(frameSize(), STACK_SLOT_SIZE));
            }

            @Override
            public CiRegister framePointerReg() {
                return ARMV7.rsp;
            }

            @Override
            public Slots slots() {
                return new Slots() {

                    @Override
                    protected String nameOfSlot(int offset) {
                        final int offsetOfReturnAddress = frameSize();
                        if (offset == DEOPT_RETURN_ADDRESS_OFFSET) {
                            return "deopt rescue";
                        }
                        if (offset == offsetOfReturnAddress) {
                            return "return address";
                        }
                        final int offsetOfOptPrologueCallReturnAddress = offsetOfReturnAddress - Word.size();
                        if (offset == offsetOfOptPrologueCallReturnAddress) {
                            return "prologue return";
                        }
                        final int callersRBPOffset = offsetOfOptPrologueCallReturnAddress - Word.size();
                        if (offset == callersRBPOffset) {
                            return "caller's FP";
                        }
                        return "stack arg " + (offset / Word.size() - 1);
                    }
                };
            }
        }

        public static final int PROLOGUE_SIZE = 20;

        public Baseline2Opt() {
            super(Adapter.Type.BASELINE2OPT);
        }

        @Override
        public int prologueSizeForCallee(ClassMethodActor callee) {
            return PROLOGUE_SIZE;
        }

        /**
         * The prologue for a method with a BASELINE2OPT adapter has a call to the adapter at the
         * {@link CallEntryPoint#BASELINE_ENTRY_POINT}. The body of the method starts at the
         * {@link CallEntryPoint#OPTIMIZED_ENTRY_POINT}. The assembler code is as follows:
         *
         * <pre>
         *     +0:  push lr
         *     +4:  movw
         *     +8:  movt
         *    +12:  add (the last three instructions are for the setUpScratch)
         *    +16:  blx
         *    Total size = 20 bytes
         * </pre>
         */

        @Override
        protected int emitPrologue(Object out, Adapter adapter) {
            ARMV7Assembler asm = out instanceof OutputStream ? new ARMV7Assembler(target(), null) : (ARMV7Assembler) out;

            asm.push(ARMV7Assembler.ConditionFlag.Always, asm.getRegisterList(ARMV7.LR));

            if (adapter == null) {
                if (ARMV7Assembler.ASM_DEBUG_MARKERS) {
                    asm.movImm32(ConditionFlag.Always, ARMV7.r12, 0xba5e20af);
                    asm.nop(2);
                } else {
                    asm.nop(4);
                }
            } else {
                asm.call();
                asm.align(PROLOGUE_SIZE);
            }
            int size = asm.codeBuffer.position();
            assert size == PROLOGUE_SIZE;
            copyIfOutputStream(asm.codeBuffer, out);
            return size;
        }

        /**
         * Creates a BASELINE2OPT adapter.
         *
         * @see Baseline2OptAdapterFrameLayout
         */
        @Override
        protected Adapter create(Sig sig) {
            CiValue[] optArgs = opt.getCallingConvention(JavaCall, WordUtil.ciKinds(sig.kinds, true), target(), false).locations;

            ARMV7Assembler asm = new ARMV7Assembler(target(), null);

            // Compute the number of stack args needed for the call (i.e. the args that won't
            // be put into registers)
            int stackArgumentsSize = 0;
            for (int i = optArgs.length - 1; i >= 0; i--) {
                if (optArgs[i].isStackSlot()) {
                    CiStackSlot slot = (CiStackSlot) optArgs[i];
                    int offset = slot.index() * OPT_SLOT_SIZE;
                    int end = offset + OPT_SLOT_SIZE;
                    if (!sig.kinds[i].isCategory1) {
                        end += OPT_SLOT_SIZE;
                    }
                    if (end > stackArgumentsSize) {
                        stackArgumentsSize = end;
                    }
                }
            }

            final int rbpSlotSize = OPT_SLOT_SIZE;
            final int optCallerRIPSlotSize = OPT_SLOT_SIZE;
            final int baselineCallerRIPSlotSize = OPT_SLOT_SIZE;

            // The amount by which RSP is adjusted by CALL and ENTER instructions
            final int implicitlyAllocatedFrameSize = optCallerRIPSlotSize + baselineCallerRIPSlotSize + rbpSlotSize;

            // The adapter frame size does not include the slot holding the baseline caller's RIP.
            // It must also be aligned according platform's stack frame alignment requirements.
            int adapterFrameSize = target().alignFrameSize(stackArgumentsSize + implicitlyAllocatedFrameSize - optCallerRIPSlotSize);

            // The amount by which RSP must be explicitly adjusted to create the adapter frame
            final int explicitlyAllocatedFrameSize = adapterFrameSize - rbpSlotSize - baselineCallerRIPSlotSize;

            // Allocate the frame and save RBP to the stack with an ENTER instruction
            assert explicitlyAllocatedFrameSize >= 0 && explicitlyAllocatedFrameSize <= Short.MAX_VALUE;

            // r14: return address of the caller
            // r11: fp (rbp in x86)
            // r13: sp

            // We save the return address of the caller (i.e. the next address of the blx of the opt method
            // It returns after the adapter in the first instruction of the opt method.
            asm.push(ARMV7Assembler.ConditionFlag.Always, asm.getRegisterList(ARMV7.LR));

            // We save the old framepointer to the stack
            asm.push(ARMV7Assembler.ConditionFlag.Always, asm.getRegisterList(ARMV7.FP));

            // The new FP is the SP
            asm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.FP, ARMV7.rsp);

            asm.subq(ARMV7.rsp, explicitlyAllocatedFrameSize);

            // At this point, the top of the baseline caller's stack (i.e the last arg to the call) is immediately
            // above the adapter's RIP slot. That is, it's at RSP + adapterFrameSize + OPT_SLOT_SIZE.
            int baselineStackOffset = adapterFrameSize + OPT_SLOT_SIZE;
            int baselineArgsSize = 0;
            CiBitMap refMap = null;
            for (int i = optArgs.length - 1; i >= 0; i--) {
                Kind kind = sig.kinds[i];

                if (!kind.isCategory1) {
                    // again --- moved this ifblock position from before the adaptArgumentCall
                    // Skip over the second slot of a long or double
                    baselineStackOffset += BASELINE_SLOT_SIZE;
                    baselineArgsSize += BASELINE_SLOT_SIZE;
                }

                int refMapIndex = adaptArgument(asm, kind, optArgs[i], baselineStackOffset, 0);
                if (refMapIndex != -1) {
                    if (refMap == null) {
                        refMap = new CiBitMap(adapterFrameSize / Word.size());
                    }
                    refMap.set(refMapIndex);
                }
                baselineArgsSize += BASELINE_SLOT_SIZE;
                baselineStackOffset += BASELINE_SLOT_SIZE;
            }

            // Args are now copied to the OPT locations; call the OPT main body
            int callPos = asm.codeBuffer.position();
            asm.nop(3);
            asm.blx(ARMV7.LR);
            int callSize = asm.codeBuffer.position() - callPos;

            // Restore RSP r13 and RBP r11. Given that RBP r11 is never modified by OPT methods and baseline methods
            // always
            // restore it, RBP is guaranteed to be pointing to the slot holding the caller's RBP
            asm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.rsp, ARMV7.FP);
            asm.pop(ARMV7Assembler.ConditionFlag.Always, asm.getRegisterList(ARMV7.FP));

            String description = Type.BASELINE2OPT + "-Adapter" + sig;
            // RSP has been restored to the location holding the address of the OPT main body.
            // The adapter must return to the baseline caller whose RIP is one slot higher up.
            asm.addq(ARMV7.rsp, OPT_SLOT_SIZE);
            asm.pop(ARMV7Assembler.ConditionFlag.Always, asm.getRegisterList(ARMV7.r8)); // POP return address

            assert WordWidth.signedEffective(baselineArgsSize).lessEqual(WordWidth.BITS_16);
            // Retract the stack pointer back to its position before the first argument on the caller's stack.
            asm.addq(ARMV7.rsp, baselineArgsSize);
            asm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.PC, ARMV7.r8);

            final byte[] code = asm.codeBuffer.close(true);
            if (refMap != null) {
                if (refMap.size() <= 64) {
                    long longRefMap = refMap.toLong();
                    return new Baseline2OptAdapterWithRefMap(this, description, longRefMap, adapterFrameSize, code, callPos, callSize);
                }
                return new Baseline2OptAdapterWithBigRefMap(this, description, refMap.toByteArray(), adapterFrameSize, code, callPos, callSize);
            }
            return new Baseline2OptAdapter(this, description, adapterFrameSize, code, callPos, callSize);
        }

        // Checkstyle: stop
        @Override
        protected void adapt(ARMV7Assembler asm, Kind kind, CiRegister reg, int offset32) {
            switch (kind.asEnum) {
                case BYTE:
                    asm.setUpScratch(new CiAddress(CiKind.Byte, ARMV7.RSP, offset32));
                    asm.ldrsb(ARMV7Assembler.ConditionFlag.Always, 1, 0, 0, reg, ARMV7.r12, 0);
                    break;
                case BOOLEAN:
                    asm.setUpScratch(new CiAddress(CiKind.Boolean, ARMV7.RSP, offset32));
                    asm.ldrb(ARMV7Assembler.ConditionFlag.Always, 1, 0, 0, reg, ARMV7.r12, 0);
                    break;
                case SHORT:
                    asm.setUpScratch(new CiAddress(CiKind.Short, ARMV7.RSP, offset32));
                    asm.ldrshw(ARMV7Assembler.ConditionFlag.Always, 1, 0, 0, reg, ARMV7.r12, 0);
                    break;
                case CHAR:
                    asm.setUpScratch(new CiAddress(CiKind.Char, ARMV7.RSP, offset32));
                    asm.ldruhw(ARMV7Assembler.ConditionFlag.Always, 1, 0, 0, reg, ARMV7.r12, 0);
                    break;
                case INT:
                case WORD:
                case REFERENCE:
                    asm.setUpScratch(new CiAddress(CiKind.Int, ARMV7.RSP, offset32));
                    asm.ldr(ARMV7Assembler.ConditionFlag.Always, reg, ARMV7.r12, 0);
                    break;
                case LONG:
                    asm.setUpScratch(new CiAddress(CiKind.Long, ARMV7.RSP, offset32));
                    asm.ldrd(ARMV7Assembler.ConditionFlag.Always, reg, ARMV7.r12, 0);
                    break;
                case FLOAT:
                    assert (reg.number > 15);
                    asm.setUpScratch(new CiAddress(CiKind.Float, ARMV7.RSP, offset32));
                    asm.vldr(ARMV7Assembler.ConditionFlag.Always, reg, ARMV7.r12, 0, CiKind.Float, CiKind.Int);
                    break;
                case DOUBLE:
                    assert (reg.number > 15);
                    asm.setUpScratch(new CiAddress(CiKind.Double, ARMV7.RSP, offset32));
                    asm.vldr(ARMV7Assembler.ConditionFlag.Always, reg, ARMV7.r12, 0, CiKind.Double, CiKind.Int);
                    break;
                default:
                    throw ProgramError.unexpected();
            }
        }

        // Checkstyle: resume

        @Override
        public void adapt(ARMV7Assembler asm, Kind kind, int optStackOffset32, int baselineStackOffset32, int adapterFrameSize) {
            int src = baselineStackOffset32;
            int dst = optStackOffset32;
            stackCopy(asm, kind, src, dst);
        }
    }

    /**
     * ARMV7 specific generator for {@link Type#OPT2BASELINE} adapters.
     */
    static class Opt2Baseline extends ARMAdapterGenerator {

        /**
         * ARMV7 specific {@link Type#OPT2BASELINE} adapter.
         */
        static final class Opt2BaselineAdapter extends Adapter {

            Opt2BaselineAdapter(AdapterGenerator generator, String description, int frameSize, byte[] code, int callPos, int callSize) {
                super(generator, description, frameSize, code, callPos, callSize);
            }

            @Override
            public int callOffsetInPrologue() {
                return 24;
            }

            @Override
            public int callSizeInPrologue() {
                return RIP_CALL_INSTRUCTION_SIZE;
            }

            /**
             * Computes the amount by which the stack pointer in a given {@linkplain StackFrameCursor cursor} should be
             * adjusted to obtain the location on the stack holding the RIP to which the adapter will return.
             * <p/>
             * This complex computation is only necessary for the Inspector as that is the only context in which the
             * current execution point in an adapter can be anywhere except for in the call to the body of the callee
             * being adapted. Hence the {@link HOSTED_ONLY} annotation.
             *
             * @param cursor a stack frame walker cursor denoting an execution point in this adapter
             */
            @HOSTED_ONLY
            private int computeRipAdjustment(StackFrameCursor cursor) {
                assert false : "Not implemented in ARMv7!";
                int ripAdjustment = frameSize();
                StackFrameWalker sfw = cursor.stackFrameWalker();
                int position = cursor.vmIP().minus(codeStart()).toInt();
                if (!cursor.isTopFrame()) {
                    // Inside call to baseline body. The value of RSP in cursor is now the value
                    // that the baseline caller will leave it in after popping the arguments from the stack
                    ripAdjustment = Word.size();
                } else if (position == 0) {
                    ripAdjustment = Word.size();
                } else {
                    switch (sfw.readByte(cursor.vmIP().toAddress(), 0)) {
                        case REXW:
                            byte b = sfw.readByte(cursor.vmIP().toAddress(), 1);
                            if (b == ADDQ_SUBQ_imm8) {
                                ripAdjustment = Word.size();
                            } else if (b == ADDQ_SUBQ_imm32) {
                                ripAdjustment = Word.size();
                            }
                            break;
                        case RET2:
                            ripAdjustment = 0;
                    }
                }

                return ripAdjustment;
            }

            @Override
            public Pointer returnAddressPointer(StackFrameCursor frame) {
                int ripAdjustment = MaxineVM.isHosted() ? computeRipAdjustment(frame) : Word.size();
                return frame.sp().plus(ripAdjustment);
            }

            @Override
            public void advance(StackFrameCursor cursor) {
                int ripAdjustment = MaxineVM.isHosted() ? computeRipAdjustment(cursor) : Word.size();
                StackFrameWalker sfw = cursor.stackFrameWalker();

                Pointer ripPointer = cursor.sp().plus(ripAdjustment);
                Pointer callerIP = sfw.readWord(ripPointer, 0).asPointer();
                Pointer callerSP = ripPointer.plus(Word.size()); // Skip RIP word
                Pointer callerFP = cursor.fp();

                boolean wasDisabled = SafepointPoll.disable();
                sfw.advance(callerIP, callerSP, callerFP);
                if (!wasDisabled) {
                    SafepointPoll.enable();
                }
            }

            @Override
            @HOSTED_ONLY
            public boolean acceptStackFrameVisitor(StackFrameCursor cursor, StackFrameVisitor visitor) {
                int ripAdjustment = MaxineVM.isHosted() ? computeRipAdjustment(cursor) : Word.size();
                Pointer ripPointer = cursor.sp().plus(ripAdjustment);
                Pointer fp = ripPointer.minus(frameSize());
                return visitor.visitFrame(new AdapterStackFrame(cursor.stackFrameWalker().calleeStackFrame(), cursor.targetMethod(), cursor.vmIP().toPointer(), fp, cursor.sp()));
            }

            @Override
            public VMFrameLayout frameLayout() {
                return new Opt2BaselineAdapterFrameLayout(frameSize());
            }
        }

        /**
         * Frame layout for an ARMV7 specific {@link Type#OPT2BASELINE} adapter frame.
         *
         * <pre>
         *
         *          +------------------------+
         *          |     OPT caller RIP     |
         *          +------------------------+     ---
         *          | baseline main body     |      ^
         *          +------------------------+      |
         *          |    baseline arg 0      |      |
         *          +------------------------+  frame size
         *          :                        :      |
         *          +------------------------+      |
         *   RSP--> |    baseline arg N      |      v
         *          +------------------------+     ---
         *
         *   N == number of args - 1
         * </pre>
         */
        public static class Opt2BaselineAdapterFrameLayout extends AdapterStackFrameLayout {

            Opt2BaselineAdapterFrameLayout(int frameSize) {
                super(frameSize, true);
            }

            @Override
            public CiRegister framePointerReg() {
                return ARMV7.rsp;
            }

            @Override
            public Slots slots() {
                return new Slots() {

                    @Override
                    protected String nameOfSlot(int offset) {
                        final int baselinePrologueCallReturnAddress = frameSize() - Word.size();
                        if (offset == baselinePrologueCallReturnAddress) {
                            return "prologue return";
                        }
                        return super.nameOfSlot(offset);
                    }
                };
            }
        }

        static final int PROLOGUE_SIZE = 40;
        static final int PROLOGUE_SIZE_FOR_NO_ARGS_CALLEE = 20;

        Opt2Baseline() {
            super(Adapter.Type.OPT2BASELINE);
            assert BASELINE_ENTRY_POINT.offset() == 0;
            assert OPTIMIZED_ENTRY_POINT.offset() == 20;
        }

        @Override
        public int prologueSizeForCallee(ClassMethodActor callee) {
            if (callee.descriptor().numberOfParameters() == 0 && callee.isStatic()) {
                return PROLOGUE_SIZE_FOR_NO_ARGS_CALLEE;
            }
            return PROLOGUE_SIZE;
        }

        /**
         * The prologue for a method with an OPT2BASELINE adapter has a call to the adapter at the
         * {@link CallEntryPoint#OPTIMIZED_ENTRY_POINT}. The code at the {@link CallEntryPoint#BASELINE_ENTRY_POINT} has
         * a jump over this call to the body of the method. The assembler code is as follows:
         *
         * No Adaptation
         *
         * <pre>

         *     +0:  nop
         *     +4:  nop
         *     +8:  nop
         *     +12: movw debugInfo (optional) or nop
         *     +16: movt debugInfo (optional) or nop
         * L1 +20:  // method body
         *
         * Adaptation
         *  <pre>
         *     +0:  jump L1
         *     +4:  movw debugInfo (optional) or nop
         *     +8:  movt debugInfo (optional) or nop
         *    +12:  nop
         *    +16:  nop
         *    +20:  push LR (push return address of caller, i.e. baseline method)
         *    +24:  (call) movw
         *    +28:  (call) movt
         *    +32:  (call) add
         *    +36:  (call) blx
         * L1 +40:  // method body
         * </pre>
         */
        @Override
        protected int emitPrologue(Object out, Adapter adapter) {
            ARMV7Assembler asm = out instanceof OutputStream ? new ARMV7Assembler(target(), null) : (ARMV7Assembler) out;
            if (adapter == null) {
                if (ARMV7Assembler.ASM_DEBUG_MARKERS) {
                    asm.nop((OPTIMIZED_ENTRY_POINT.offset() / 4) - 2);
                    asm.movImm32(ConditionFlag.Always, ARMV7.r12, 0xba5eba5e); // signifies OPT2BASE
                } else {
                    asm.nop(OPTIMIZED_ENTRY_POINT.offset() / 4);
                }
                assert asm.codeBuffer.position() == PROLOGUE_SIZE_FOR_NO_ARGS_CALLEE;
                copyIfOutputStream(asm.codeBuffer, out);
                return PROLOGUE_SIZE_FOR_NO_ARGS_CALLEE;
            }

            // A baseline caller jumps over the call to the OPT2BASELINE adapter
            ARMV7Label end = new ARMV7Label();
            asm.branch(end);
            if (ARMV7Assembler.ASM_DEBUG_MARKERS) {
                asm.movImm32(ConditionFlag.Always, ARMV7.r12, 0x0af2ba5e);
            }

            // Pad with nops up to the OPT entry point
            asm.nop((OPTIMIZED_ENTRY_POINT.offset() - asm.codeBuffer.position()) / 4);
            asm.push(ARMV7Assembler.ConditionFlag.Always, asm.getRegisterList(ARMV7.LR));
            asm.call();
            asm.bind(end);
            int size = asm.codeBuffer.position();
            assert size == PROLOGUE_SIZE;
            copyIfOutputStream(asm.codeBuffer, out);
            return size;
        }

        /**
         * Creates an OPT2BASELINE adapter.
         *
         * @see Opt2BaselineAdapterFrameLayout
         */
        @Override
        protected Adapter create(Sig sig) {
            CiValue[] optArgs = opt.getCallingConvention(JavaCall, WordUtil.ciKinds(sig.kinds, true), target(), false).locations;
            ARMV7Assembler asm = new ARMV7Assembler(target(), null);

            asm.push(ARMV7Assembler.ConditionFlag.Always, asm.getRegisterList(ARMV7.LR));

            // Initial args are in registers, remaining args are on the stack.
            int baselineArgsSize = frameSizeFor(sig.kinds, BASELINE_SLOT_SIZE);
            assert baselineArgsSize % target().stackAlignment == 0 : "BASELINE_SLOT_SIZE should guarantee parametersSize satifies ABI alignment requirements";

            final int optCallerRIPSlotSize = OPT_SLOT_SIZE;
            int adapterFrameSize = baselineArgsSize + optCallerRIPSlotSize;

            // Adjust RSP to create space for the baseline args
            asm.subq(ARMV7.rsp, baselineArgsSize);

            // Copy OPT args into baseline args
            int baselineStackOffset = 0;
            for (int i = optArgs.length - 1; i >= 0; i--) {
                Kind argKind = sig.kinds[i];
                if (!argKind.isCategory1) {
                    // Skip over the first slot of a long or double
                    baselineStackOffset += BASELINE_SLOT_SIZE;

                }
                adaptArgument(asm, argKind, optArgs[i], baselineStackOffset, adapterFrameSize);
                baselineStackOffset += BASELINE_SLOT_SIZE;
            }

            // Args are now copied to the baseline locations; call the baseline main body
            int callPos = asm.codeBuffer.position();
            asm.nop(3);
            asm.blx(ARMV7.LR);
            int callSize = asm.codeBuffer.position() - callPos;

            // The baseline method will have popped the args off the stack so now
            // RSP is pointing to the slot holding the address of the baseline main body.
            // The adapter must return to the OPT caller whose RIP is one slot higher up.
            asm.addq(ARMV7.rsp, OPT_SLOT_SIZE);

            // Return to the OPT caller
            asm.pop(ARMV7Assembler.ConditionFlag.Always, asm.getRegisterList(ARMV7.r8));
            asm.mov(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.PC, ARMV7.r8);

            final byte[] code = asm.codeBuffer.close(true);

            String description = Type.OPT2BASELINE + "-Adapter" + sig;
            return new Opt2BaselineAdapter(this, description, adapterFrameSize, code, callPos, callSize);
        }

        // Checkstyle: stop
        @Override
        protected void adapt(ARMV7Assembler asm, Kind kind, CiRegister reg, int offset32) {
            switch (kind.asEnum) {
                case BYTE:
                case BOOLEAN:
                case SHORT:
                case CHAR:
                case INT:
                case WORD:
                case REFERENCE:
                    asm.setUpScratch(new CiAddress(CiKind.Int, ARMV7.RSP, offset32));
                    asm.str(ARMV7Assembler.ConditionFlag.Always, reg, ARMV7.r12, 0);
                    break;
                case LONG:
                    asm.setUpScratch(new CiAddress(CiKind.Long, ARMV7.RSP, offset32));
                    asm.strd(ARMV7Assembler.ConditionFlag.Always, reg, ARMV7.r12, 0);
                    break;
                case FLOAT:
                    asm.setUpScratch(new CiAddress(CiKind.Float, ARMV7.RSP, offset32));
                    if (reg.number < 15) {
                        asm.str(ARMV7Assembler.ConditionFlag.Always, reg, ARMV7.r12, 0);
                    } else {
                        asm.vstr(ARMV7Assembler.ConditionFlag.Always, reg, ARMV7.r12, 0, CiKind.Float, CiKind.Int);
                    }
                    break;
                case DOUBLE:
                    asm.setUpScratch(new CiAddress(CiKind.Double, ARMV7.RSP, offset32));
                    if (reg.number < 15) {
                        asm.str(ARMV7Assembler.ConditionFlag.Always, reg, ARMV7.r12, 0);
                    } else {
                        asm.vstr(ARMV7Assembler.ConditionFlag.Always, reg, ARMV7.r12, 0, CiKind.Double, CiKind.Int);
                    }
                    break;
                default:
                    throw ProgramError.unexpected();
            }
        }
        // Checkstyle: resume

        @Override
        public void adapt(ARMV7Assembler asm, Kind kind, int optStackOffset32, int baselineStackOffset32, int adapterFrameSize) {
            // Add word size to take into account the slot used by the RIP of the caller
            int src = adapterFrameSize + optStackOffset32 + Word.size();
            int dst = baselineStackOffset32;
            stackCopy(asm, kind, src, dst);
        }
    }

    /**
     * Emits code to copy an incoming argument from ABI-specified location of the caller to the ABI-specified location
     * of the callee.
     *
     * @param asm assembler used to emit code
     * @param kind the kind of the argument
     * @param optArg the location of the argument according to the OPT convention
     * @param baselineStackOffset32 the 32-bit offset of the argument on the stack according to the baseline convention
     * @param adapterFrameSize the size of the adapter frame
     * @return the reference map index of the reference slot on the adapter frame corresponding to the argument or -1
     */
    protected int adaptArgument(ARMV7Assembler asm, Kind kind, CiValue optArg, int baselineStackOffset32, int adapterFrameSize) {
        if (optArg.isRegister()) {
            adapt(asm, kind, optArg.asRegister(), baselineStackOffset32);
        } else if (optArg.isStackSlot()) {
            int optStackOffset32 = ((CiStackSlot) optArg).index() * OPT_SLOT_SIZE;
            adapt(asm, kind, optStackOffset32, baselineStackOffset32, adapterFrameSize);
            if (kind.isReference) {
                return optStackOffset32 / Word.size();
            }
        } else {
            throw FatalError.unexpected("Unadaptable parameter location type: " + optArg.getClass().getSimpleName());
        }
        return -1;
    }

    protected void stackCopy(ARMV7Assembler asm, Kind kind, int sourceStackOffset, int destStackOffset) {
        if (kind == Kind.LONG || kind == Kind.DOUBLE) {
            asm.vmov(ConditionFlag.Always, ARMV7.s31, ARMV7.r9, null, CiKind.Float, CiKind.Int);
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.RSP, sourceStackOffset));
            asm.ldrd(ConditionFlag.Always, ARMV7.r8, ARMV7.r12, 0);
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.RSP, destStackOffset));
            asm.strd(ConditionFlag.Always, ARMV7.r8, ARMV7.r12, 0);
            asm.vmov(ConditionFlag.Always, ARMV7.r9, ARMV7.s31, null, CiKind.Int, CiKind.Float);
        } else {
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.RSP, sourceStackOffset));
            asm.ldr(ConditionFlag.Always, ARMV7.r8, ARMV7.r12, 0);
            asm.setUpScratch(new CiAddress(WordUtil.archKind(), ARMV7.RSP, destStackOffset));
            asm.str(ConditionFlag.Always, ARMV7.r8, ARMV7.r12, 0);
        }
    }

    protected abstract void adapt(ARMV7Assembler asm, Kind kind, int optStackOffset32, int baselineStackOffset32, int adapterFrameSize);

    protected abstract void adapt(ARMV7Assembler asm, Kind kind, CiRegister reg, int offset32);

    public static final byte REXW = (byte) 0x48;
    public static final byte RET2 = (byte) 0xC2;
    public static final byte ENTER = (byte) 0xC8;
    public static final byte ADDQ_SUBQ_imm8 = (byte) 0x83;
    public static final byte ADDQ_SUBQ_imm32 = (byte) 0x81;
}

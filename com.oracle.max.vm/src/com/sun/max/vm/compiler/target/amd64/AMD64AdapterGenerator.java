/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.compiler.target.amd64;

import static com.oracle.max.asm.target.amd64.AMD64.*;
import static com.sun.cri.ci.CiCallingConvention.Type.*;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.compiler.CallEntryPoint.*;
import static com.sun.max.vm.compiler.deopt.Deoptimization.*;
import static com.sun.max.vm.compiler.target.amd64.AMD64TargetMethodUtil.*;

import java.io.*;

import com.oracle.max.asm.*;
import com.oracle.max.asm.target.amd64.*;
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
import com.sun.max.vm.compiler.target.Adapter.Type;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.Cursor;
import com.sun.max.vm.type.*;

/**
 * Adapter generators for AMD64.
 */
public abstract class AMD64AdapterGenerator extends AdapterGenerator {

    final CiRegister scratch;

    static {
        if (vm().compilationBroker.needsAdapters()) {
            // Create and register the adapter generators
            new Baseline2Opt();
            new Opt2Baseline();
        }
    }

    public AMD64AdapterGenerator(Adapter.Type adapterType) {
        super(adapterType);
        scratch = opt.getScratchRegister();
    }

    @Override
    public boolean advanceIfInPrologue(Cursor current) {
        if (inPrologue(current.ip(), current.targetMethod())) {
            StackFrameWalker sfw = current.stackFrameWalker();
            final Pointer callerIP = sfw.readWord(current.sp(), 0).asPointer();
            Pointer callerSP = current.sp().plus(Word.size()); // skip RIP
            sfw.advance(callerIP, callerSP, current.fp());
            return true;
        }
        return false;
    }

    /**
     * AMD64 specific generator for {@link Type#BASELINE2OPT} adapters.
     */
    public static class Baseline2Opt extends AMD64AdapterGenerator {

        /**
         * AMD64 specific {@link Type#BASELINE2OPT} adapters.
         */
        public static class Baseline2OptAdapter extends Adapter {

            Baseline2OptAdapter(AdapterGenerator generator, String description, int frameSize, byte[] code, int callPos, int callSize) {
                super(generator, description, frameSize, code, callPos, callSize);
            }

            @Override
            public int callOffsetInPrologue() {
                return 0;
            }

            @Override
            public int callSizeInPrologue() {
                return RIP_CALL_INSTRUCTION_SIZE;
            }

            /**
             * Computes the state of an adapter frame based on an execution point in this adapter.
             *
             * This complex computation is only necessary for the Inspector as that is the only context
             * in which the current execution point in an adapter can be anywhere except for in
             * the call back to the callee being adapted. Hence the {@link HOSTED_ONLY} annotation.
             *
             * @param cursor a stack frame walker cursor denoting an execution point in this adapter
             * @return the amount by which {@code current.sp()} should be adjusted to obtain the slot holding the
             *         address to which the adapter will return. If the low bit of this value is set, then it means that
             *         the caller's RBP is obtained by reading the word at {@code current.fp()} otherwise it is equal to
             *         {@code current.fp()}.
             */
            @HOSTED_ONLY
            private int computeFrameState(Cursor cursor) {
                int ripAdjustment = frameSize();
                boolean rbpSaved = false;
                StackFrameWalker sfw = cursor.stackFrameWalker();
                int position = cursor.ip().minus(codeStart).toInt();

                byte b = sfw.readByte(cursor.ip(), 0);
                if (position == 0 || b == ENTER) {
                    ripAdjustment = Word.size();
                } else if (b == RET2) {
                    ripAdjustment = 0;
                } else if (b == REXW) {
                    b = sfw.readByte(cursor.ip(), 1);
                    if (b == ADDQ_SUBQ_imm8) {
                        ripAdjustment = Word.size();
                    } else if (b == ADDQ_SUBQ_imm32) {
                        ripAdjustment = Word.size();
                    }
                } else {
                    rbpSaved = true;
                }

                assert (ripAdjustment & 1) == 0;
                return ripAdjustment | (rbpSaved ? 1 : 0);
            }

            @Override
            public Pointer returnAddressPointer(Cursor frame) {
                int ripAdjustment = frameSize();
                if (MaxineVM.isHosted()) {
                    // Inspector context only
                    int state = computeFrameState(frame);
                    ripAdjustment = state & ~1;
                }

                return frame.sp().plus(ripAdjustment);
            }

            @Override
            public void advance(Cursor current) {
                StackFrameWalker sfw = current.stackFrameWalker();
                int ripAdjustment = frameSize();
                boolean rbpSaved = true;
                if (MaxineVM.isHosted()) {
                    // Inspector context only
                    int state = computeFrameState(current);
                    ripAdjustment = state & ~1;
                    rbpSaved = (state & 1) == 1;
                }

                Pointer ripPointer = current.sp().plus(ripAdjustment);
                Pointer callerIP = sfw.readWord(ripPointer, 0).asPointer();
                Pointer callerSP = ripPointer.plus(Word.size()); // Skip RIP word
                Pointer callerFP = rbpSaved ? sfw.readWord(ripPointer, -Word.size() * 2).asPointer() : current.fp();
                sfw.advance(callerIP, callerSP, callerFP);
            }

            @Override
            public boolean acceptStackFrameVisitor(Cursor current, StackFrameVisitor visitor) {
                int ripAdjustment = MaxineVM.isHosted() ? computeFrameState(current) & ~1 : frameSize();
                Pointer ripPointer = current.sp().plus(ripAdjustment);
                Pointer fp = ripPointer.minus(frameSize());
                return visitor.visitFrame(new AdapterStackFrame(current.stackFrameWalker().calleeStackFrame(), current.targetMethod(), current.ip(), fp, current.sp()));
            }

            @Override
            public VMFrameLayout frameLayout() {
                return new Baseline2OptAdapterFrameLayout(frameSize());
            }
        }

        /**
         * A specialization of an AMD64 specific {@link Type#BASELINE2OPT} adapter that contains a reference map occupying 64 or less bits.
         */
        public static class Baseline2OptAdapterWithRefMap extends Baseline2OptAdapter {

            final long refMap;

            public Baseline2OptAdapterWithRefMap(AdapterGenerator generator, String description, long refMap, int frameSize, byte[] code, int callPos, int callSize) {
                super(generator, description, frameSize, code, callPos, callSize);
                this.refMap = refMap;
            }

            @Override
            public void prepareReferenceMap(Cursor current, Cursor callee, StackReferenceMapPreparer preparer) {
                preparer.tracePrepareReferenceMap(this, 0, current.sp(), "frame");
                int frameSlotIndex = preparer.referenceMapBitIndex(current.sp());
                int byteIndex = 0;
                long refMap = this.refMap;
                for (int i = 0; i < 8; i++) {
                    final byte refMapByte = (byte) (refMap & 0xff);
                    preparer.traceReferenceMapByteBefore(byteIndex, refMapByte, "Frame");
                    preparer.setBits(frameSlotIndex, refMapByte);
                    preparer.traceReferenceMapByteAfter(current.sp(), frameSlotIndex, refMapByte);
                    refMap >>>= Bytes.WIDTH;
                    frameSlotIndex += Bytes.WIDTH;
                    byteIndex++;
                }
            }
        }

        /**
         * A specialization of an AMD64 specific {@link Type#BASELINE2OPT} adapter that contains a reference map occupying more than 64 bits.
         */
        public static class Baseline2OptAdapterWithBigRefMap extends Baseline2OptAdapter {

            final byte[] refMap;

            public Baseline2OptAdapterWithBigRefMap(AdapterGenerator generator, String description, byte[] refMap, int frameSize, byte[] code, int callPos, int callSize) {
                super(generator, description, frameSize, code, callPos, callSize);
                this.refMap = refMap;
            }

            @Override
            public void prepareReferenceMap(Cursor current, Cursor callee, StackReferenceMapPreparer preparer) {
                preparer.tracePrepareReferenceMap(this, 0, current.sp(), "frame");
                int frameSlotIndex = preparer.referenceMapBitIndex(current.sp());
                int byteIndex = 0;
                for (int i = 0; i < refMap.length; i++) {
                    final byte frameReferenceMapByte = refMap[byteIndex];
                    preparer.traceReferenceMapByteBefore(byteIndex, frameReferenceMapByte, "Frame");
                    preparer.setBits(frameSlotIndex, frameReferenceMapByte);
                    preparer.traceReferenceMapByteAfter(current.sp(), frameSlotIndex, frameReferenceMapByte);
                    frameSlotIndex += Bytes.WIDTH;
                    byteIndex++;
                }
            }
        }

        /**
         * Frame layout for an AMD64 specific {@link Type#BASELINE2OPT} adapter frame.
         * <pre>
         *
         *          +------------------------+
         *          |  Baseline caller RIP   |
         *          +------------------------+     ---
         *          |     OPT main body      |      ^
         *          +------------------------+      |
         *          |       saved RBP        |      |
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
                return AMD64.rsp;
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

        private static final int PROLOGUE_SIZE = 8;

        public Baseline2Opt() {
            super(Adapter.Type.BASELINE2OPT);
        }

        @Override
        public int prologueSizeForCallee(ClassMethodActor callee) {
            return PROLOGUE_SIZE;
        }

        /**
         * The prologue for a method with a BASELINE2OPT adapter has a call to the adapter
         * at the {@link CallEntryPoint#BASELINE_ENTRY_POINT}. The body of the method starts at the
         * {@link CallEntryPoint#OPTIMIZED_ENTRY_POINT}. The assembler code is as follows:
         * <pre>
         *     +0:  call <BASELINE2OPT adapter>
         *     +5:  nop
         *     +6:  nop
         *     +7:  nop
         *     +8:  // method body
         * </pre>
         */
        @Override
        protected int emitPrologue(Object out, Adapter adapter) {
            AMD64Assembler asm = out instanceof OutputStream ? new AMD64Assembler(target(), null) : (AMD64Assembler) out;

            if (adapter == null) {
                asm.nop(PROLOGUE_SIZE);
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

            AMD64Assembler asm = new AMD64Assembler(target(), null);

            // On entry to the frame, there are 2 return addresses on the stack at [RSP] and [RSP + 8].
            // The one at [RSP] is the return address of the call in the OPT callee's prologue (which is
            // also the entry to the main body of the OPT callee) and one at [RSP + 8] is the return
            // address in the baseline caller.

            // Save the address of the OPT callee's main body in RAX
            asm.movq(rax, new CiAddress(WordUtil.archKind(), rsp.asValue()));

            // Compute the number of stack args needed for the call (i.e. the args that won't
            // be put into registers)
            int stackArgumentsSize = 0;
            for (int i = optArgs.length - 1; i >= 0; i--) {
                if (optArgs[i].isStackSlot()) {
                    CiStackSlot slot = (CiStackSlot) optArgs[i];
                    int offset = slot.index() * OPT_SLOT_SIZE;
                    int end = offset + OPT_SLOT_SIZE;
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
            asm.enter(explicitlyAllocatedFrameSize, 0);

            // At this point, the top of the baseline caller's stack (i.e the last arg to the call) is immediately
            // above the adapter's RIP slot. That is, it's at RSP + adapterFrameSize + OPT_SLOT_SIZE.
            int baselineStackOffset = adapterFrameSize + OPT_SLOT_SIZE;
            int baselineArgsSize = 0;
            CiBitMap refMap = null;
            for (int i = optArgs.length - 1; i >= 0;  i--) {
                Kind kind = sig.kinds[i];

                if (!kind.isCategory1) {
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
            asm.call(rax);
            int callSize = asm.codeBuffer.position() - callPos;

            // Restore RSP and RBP. Given that RBP is never modified by OPT methods and baseline methods always
            // restore it, RBP is guaranteed to be pointing to the slot holding the caller's RBP
            asm.leave();

            String description = Type.BASELINE2OPT + "-Adapter" + sig;
            // RSP has been restored to the location holding the address of the OPT main body.
            // The adapter must return to the baseline caller whose RIP is one slot higher up.
            asm.addq(rsp, 8);

            assert WordWidth.signedEffective(baselineArgsSize).lessEqual(WordWidth.BITS_16);
            // Retract the stack pointer back to its position before the first argument on the caller's stack.
            asm.ret((short) baselineArgsSize);

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
        protected void adapt(AMD64Assembler asm, Kind kind, CiRegister reg, int offset32) {
            switch (kind.asEnum) {
                case BYTE:      asm.movsxb(reg, new CiAddress(CiKind.Byte, rsp.asValue(), offset32));    break;
                case BOOLEAN:   asm.movzxb(reg, new CiAddress(CiKind.Boolean, rsp.asValue(), offset32)); break;
                case SHORT:     asm.movsxw(reg, new CiAddress(CiKind.Short, rsp.asValue(), offset32));   break;
                case CHAR:      asm.movzxl(reg, new CiAddress(CiKind.Char, rsp.asValue(), offset32));    break;
                case INT:       asm.movslq(reg, new CiAddress(CiKind.Int, rsp.asValue(), offset32));     break;
                case LONG:
                case WORD:
                case REFERENCE: asm.movq(reg, new CiAddress(CiKind.Long, rsp.asValue(), offset32));      break;
                case FLOAT:     asm.movss(reg, new CiAddress(CiKind.Float, rsp.asValue(), offset32));    break;
                case DOUBLE:    asm.movsd(reg, new CiAddress(CiKind.Double, rsp.asValue(), offset32));   break;
                default:        throw ProgramError.unexpected();
            }
        }
        // Checkstyle: resume

        @Override
        public void adapt(AMD64Assembler asm, Kind kind, int optStackOffset32, int baselineStackOffset32, int adapterFrameSize) {
            int src = baselineStackOffset32;
            int dst = optStackOffset32;
            stackCopy(asm, kind, src, dst);
        }
    }

    /**
     * AMD64 specific generator for {@link Type#OPT2BASELINE} adapters.
     */
    static class Opt2Baseline extends AMD64AdapterGenerator {

        /**
         * AMD64 specific {@link Type#OPT2BASELINE} adapter.
         */
        static final class Opt2BaselineAdapter extends Adapter {

            Opt2BaselineAdapter(AdapterGenerator generator, String description, int frameSize, byte[] code, int callPos, int callSize) {
                super(generator, description, frameSize, code, callPos, callSize);
            }

            @Override
            public int callOffsetInPrologue() {
                return 8;
            }

            @Override
            public int callSizeInPrologue() {
                return RIP_CALL_INSTRUCTION_SIZE;
            }

            /**
             * Computes the amount by which the stack pointer in a given {@linkplain Cursor cursor}
             * should be adjusted to obtain the location on the stack holding the RIP to which
             * the adapter will return.
             *
             * This complex computation is only necessary for the Inspector as that is the only context
             * in which the current execution point in an adapter can be anywhere except for in
             * the call to the body of the callee being adapted. Hence the {@link HOSTED_ONLY} annotation.
             *
             * @param cursor a stack frame walker cursor denoting an execution point in this adapter
             */
            @HOSTED_ONLY
            private int computeRipAdjustment(Cursor cursor) {
                int ripAdjustment = frameSize();
                StackFrameWalker sfw = cursor.stackFrameWalker();
                int position = cursor.ip().minus(codeStart).toInt();
                if (!cursor.isTopFrame()) {
                    // Inside call to baseline body. The value of RSP in cursor is now the value
                    // that the baseline caller will leave it in after popping the arguments from the stack
                    ripAdjustment = Word.size();
                } else if (position == 0) {
                    ripAdjustment = Word.size();
                } else {
                    switch (sfw.readByte(cursor.ip(), 0)) {
                        case REXW:
                            byte b = sfw.readByte(cursor.ip(), 1);
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
            public Pointer returnAddressPointer(Cursor frame) {
                int ripAdjustment = MaxineVM.isHosted() ? computeRipAdjustment(frame) : Word.size();
                return frame.sp().plus(ripAdjustment);
            }

            @Override
            public void advance(Cursor cursor) {
                int ripAdjustment = MaxineVM.isHosted() ? computeRipAdjustment(cursor) : Word.size();
                StackFrameWalker sfw = cursor.stackFrameWalker();

                Pointer ripPointer = cursor.sp().plus(ripAdjustment);
                Pointer callerIP = sfw.readWord(ripPointer, 0).asPointer();
                Pointer callerSP = ripPointer.plus(Word.size()); // Skip RIP word
                Pointer callerFP = cursor.fp();

                sfw.advance(callerIP, callerSP, callerFP);
            }

            @Override
            public boolean acceptStackFrameVisitor(Cursor cursor, StackFrameVisitor visitor) {
                int ripAdjustment = MaxineVM.isHosted() ? computeRipAdjustment(cursor) : Word.size();

                Pointer ripPointer = cursor.sp().plus(ripAdjustment);
                Pointer fp = ripPointer.minus(frameSize());
                return visitor.visitFrame(new AdapterStackFrame(cursor.stackFrameWalker().calleeStackFrame(), cursor.targetMethod(), cursor.ip(), fp, cursor.sp()));
            }

            @Override
            public VMFrameLayout frameLayout() {
                return new Opt2BaselineAdapterFrameLayout(frameSize());
            }
        }

        /**
         * Frame layout for an AMD64 specific {@link Type#OPT2BASELINE} adapter frame.
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

            public Opt2BaselineAdapterFrameLayout(int frameSize) {
                super(frameSize, true);
            }

            @Override
            public CiRegister framePointerReg() {
                return AMD64.rsp;
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

        static final int PROLOGUE_SIZE = 13;
        static final int PROLOGUE_SIZE_FOR_NO_ARGS_CALLEE = 8;

        Opt2Baseline() {
            super(Adapter.Type.OPT2BASELINE);
            assert BASELINE_ENTRY_POINT.offset() == 0;
            assert OPTIMIZED_ENTRY_POINT.offset() == 8;
        }

        @Override
        public int prologueSizeForCallee(ClassMethodActor callee) {
            if (callee.descriptor().numberOfParameters() == 0 && callee.isStatic()) {
                return PROLOGUE_SIZE_FOR_NO_ARGS_CALLEE;
            }
            return PROLOGUE_SIZE;
        }

        /**
         * The prologue for a method with an OPT2BASELINE adapter has a call to the adapter
         * at the {@link CallEntryPoint#OPTIMIZED_ENTRY_POINT}. The code at the
         * {@link CallEntryPoint#BASELINE_ENTRY_POINT} has a jump over this call to the
         * body of the method. The assembler code is as follows:
         * <pre>
         *     +0:  jmp L1
         *     +8:  call <adapter>
         * L1 +13:  // method body
         * </pre>
         * In the case where there is no adaptation required (i.e. a parameterless call to a static method),
         * the assembler code in the prologue is a series of {@code nop}s up to the {@link CallEntryPoint#OPTIMIZED_ENTRY_POINT}
         * which is where the method body starts. This means that a baseline caller will fall through the {@code nop}s
         * to the method body.
         */
        @Override
        protected int emitPrologue(Object out, Adapter adapter) {
            AMD64Assembler asm = out instanceof OutputStream ? new AMD64Assembler(target(), null) : (AMD64Assembler) out;
            if (adapter == null) {
                asm.nop(OPTIMIZED_ENTRY_POINT.offset());
                assert asm.codeBuffer.position() == PROLOGUE_SIZE_FOR_NO_ARGS_CALLEE;
                copyIfOutputStream(asm.codeBuffer, out);
                return PROLOGUE_SIZE_FOR_NO_ARGS_CALLEE;
            }

            // A baseline caller jumps over the call to the OPT2BASELINE adapter
            Label end = new Label();
            asm.jmp(end);

            // Pad with nops up to the OPT entry point
            asm.align(OPTIMIZED_ENTRY_POINT.offset());

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
            AMD64Assembler asm = new AMD64Assembler(target(), null);

            // On entry to the frame, there are 2 return addresses at [RSP] and [RSP + 8].
            // The one at [RSP] is the return address of the call in the baseline callee's prologue (which is
            // also the entry to the main body of the baseline callee) and one at [RSP + 8] is the return
            // address in the OPT caller.

            // Save the address of the baseline callee's main body in RAX
            asm.movq(rax, new CiAddress(WordUtil.archKind(), rsp.asValue()));

            // Initial args are in registers, remaining args are on the stack.
            int baselineArgsSize = frameSizeFor(sig.kinds, BASELINE_SLOT_SIZE);
            assert baselineArgsSize % target().stackAlignment == 0 : "BASELINE_SLOT_SIZE should guarantee parametersSize satifies ABI alignment requirements";

            final int optCallerRIPSlotSize = OPT_SLOT_SIZE;
            int adapterFrameSize = baselineArgsSize + optCallerRIPSlotSize;

            // Adjust RSP to create space for the baseline args
            asm.subq(rsp, baselineArgsSize);

            // Copy OPT args into baseline args
            int baselineStackOffset = 0;
            for (int i = optArgs.length - 1; i >= 0; i--) {
                Kind argKind = sig.kinds[i];
                if (!argKind.isCategory1) {
                    // Skip over the second slot of a long or double
                    baselineStackOffset += BASELINE_SLOT_SIZE;
                }
                adaptArgument(asm, argKind, optArgs[i], baselineStackOffset, adapterFrameSize);
                baselineStackOffset += BASELINE_SLOT_SIZE;
            }

            // Args are now copied to the baseline locations; call the baseline main body
            int callPos = asm.codeBuffer.position();
            asm.call(rax);
            int callSize = asm.codeBuffer.position() - callPos;

            // The baseline method will have popped the args off the stack so now
            // RSP is pointing to the slot holding the address of the baseline main body.
            // The adapter must return to the OPT caller whose RIP is one slot higher up.
            asm.addq(rsp, OPT_SLOT_SIZE);

            // Return to the OPT caller
            asm.ret(0);

            final byte[] code = asm.codeBuffer.close(true);

            String description = Type.OPT2BASELINE + "-Adapter" + sig;
            return new Opt2BaselineAdapter(this, description, adapterFrameSize, code, callPos, callSize);
        }

        // Checkstyle: stop
        @Override
        protected void adapt(AMD64Assembler asm, Kind kind, CiRegister reg, int offset32) {
            switch (kind.asEnum) {
                case BYTE:
                case BOOLEAN:
                case SHORT:
                case CHAR:
                case INT:       asm.movl(new CiAddress(CiKind.Int, rsp.asValue(), offset32), reg);  break;
                case LONG:
                case WORD:
                case REFERENCE: asm.movq(new CiAddress(CiKind.Long, rsp.asValue(), offset32), reg);  break;
                case FLOAT:     asm.movss(new CiAddress(CiKind.Float, rsp.asValue(), offset32), reg);  break;
                case DOUBLE:    asm.movsd(new CiAddress(CiKind.Double, rsp.asValue(), offset32), reg);  break;
                default:        throw ProgramError.unexpected();
            }
        }
        // Checkstyle: resume

        @Override
        public void adapt(AMD64Assembler asm, Kind kind, int optStackOffset32, int baselineStackOffset32, int adapterFrameSize) {
            // Add word size to take into account the slot used by the RIP of the caller
            int src = adapterFrameSize + optStackOffset32 + Word.size();
            int dst = baselineStackOffset32;
            stackCopy(asm, kind, src, dst);
        }
    }

    /**
     * Emits code to copy an incoming argument from ABI-specified location of the caller
     * to the ABI-specified location of the callee.
     *
     * @param asm assembler used to emit code
     * @param kind the kind of the argument
     * @param optArg the location of the argument according to the OPT convention
     * @param baselineStackOffset32 the 32-bit offset of the argument on the stack according to the baseline convention
     * @param adapterFrameSize the size of the adapter frame
     * @return the reference map index of the reference slot on the adapter frame corresponding to the argument or -1
     */
    protected int adaptArgument(AMD64Assembler asm, Kind kind, CiValue optArg, int baselineStackOffset32, int adapterFrameSize) {
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

    protected void stackCopy(AMD64Assembler asm, Kind kind, int sourceStackOffset, int destStackOffset) {
        // First, load into a scratch register of appropriate size for the kind, then write to memory location
        if (kind.width == WordWidth.BITS_64) {
            asm.movq(scratch, new CiAddress(WordUtil.archKind(), rsp.asValue(), sourceStackOffset));
            asm.movq(new CiAddress(WordUtil.archKind(), rsp.asValue(), destStackOffset), scratch);
        } else {
            asm.movzxd(scratch, new CiAddress(WordUtil.archKind(), rsp.asValue(), sourceStackOffset));
            asm.movl(new CiAddress(WordUtil.archKind(), rsp.asValue(), destStackOffset), scratch);
        }
    }

    protected abstract void adapt(AMD64Assembler asm, Kind kind, int optStackOffset32, int baselineStackOffset32, int adapterFrameSize);
    protected abstract void adapt(AMD64Assembler asm, Kind kind, CiRegister reg, int offset32);

    public static final byte REXW = (byte) 0x48;
    public static final byte RET2 = (byte) 0xC2;
    public static final byte ENTER = (byte) 0xC8;
    public static final byte ADDQ_SUBQ_imm8 = (byte) 0x83;
    public static final byte ADDQ_SUBQ_imm32 = (byte) 0x81;
}

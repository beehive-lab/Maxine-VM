/*
 * Copyright (c) 2018-2019, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.max.asm.target.aarch64;

import static com.oracle.max.asm.target.aarch64.Aarch64.*;
import static com.oracle.max.asm.target.aarch64.Aarch64Address.AddressingMode.*;
import static com.oracle.max.asm.target.aarch64.Aarch64MacroAssembler.AddressGenerationPlan.WorkPlan.*;

import com.oracle.max.asm.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

public class Aarch64MacroAssembler extends Aarch64Assembler {

    public static final int PLACEHOLDER_INSTRUCTIONS_FOR_LONG_OFFSETS = 5;

    /** Size of a call-site == 1 instruction. */
    public static final int RIP_CALL_INSTRUCTION_SIZE = INSTRUCTION_SIZE;

    /** Offset of the branch instruction in a call-site. */
    public static final int CALL_BRANCH_OFFSET = RIP_CALL_INSTRUCTION_SIZE - INSTRUCTION_SIZE;

    public Aarch64MacroAssembler(CiTarget target, RiRegisterConfig registerConfig) {
        super(target, registerConfig);
    }

    public void iadd(CiRegister dest, CiRegister left, CiRegister right) {
        add(32, dest, left, right);
    }

    public void align(int modulus) {
        if (codeBuffer.position() % modulus != 0) {
            assert modulus % 4 == 0;
            nop((modulus - (codeBuffer.position() % modulus)) / 4);
        }
    }

    /**
     * Specifies what actions have to be taken to turn an arbitrary address of the form
     * {@code base + displacement [+ index [<< scale]]} into a valid Aarch64Address.
     */
    public static class AddressGenerationPlan {

        public final WorkPlan workPlan;
        public final Aarch64Address.AddressingMode addressingMode;
        public final boolean needsScratch;

        public enum WorkPlan {
            /**
             * Can be used as-is without extra work.
             */
            NO_WORK,
            /**
             * Add scaled displacement to index register.
             */
            ADD_TO_INDEX,
            /**
             * Add unscaled displacement to base register.
             */
            ADD_TO_BASE,
        }

        /**
         * @param workPlan Work necessary to generate a valid address.
         * @param addressingMode Addressing mode of generated address.
         * @param needsScratch True if generating address needs a scatch register, false otherwise.
         */
        public AddressGenerationPlan(WorkPlan workPlan, Aarch64Address.AddressingMode addressingMode, boolean needsScratch) {
            this.workPlan = workPlan;
            this.addressingMode = addressingMode;
            this.needsScratch = needsScratch;
        }
    }

    /**
     * Generates an addressplan for an address of the form {@code base + displacement [+ index [<< log2(transferSize)]]}
     * with the index register and scaling being optional.
     *
     * @param displacement an arbitrary displacement.
     * @param hasIndexRegister true if the address uses an index register, false otherwise. non null
     * @param transferSize the memory transfer size in bytes. The log2 of this specifies how much the index register is
     *            scaled. If 0 no scaling is assumed. Can be 0, 1, 2, 4 or 8.
     * @return AddressGenerationPlan that specifies the actions necessary to generate a valid Aarch64Address for the
     *         given parameters.
     */
    public static AddressGenerationPlan generateAddressPlan(long displacement, boolean hasIndexRegister, int transferSize) {
        assert transferSize == 0 || transferSize == 1 || transferSize == 2 || transferSize == 4 || transferSize == 8;
        boolean indexScaled = transferSize != 0;
        int log2Scale = NumUtil.log2Ceil(transferSize);
        long scaledDisplacement = displacement >> log2Scale;
        boolean displacementScalable = indexScaled && (displacement & (transferSize - 1)) == 0;
        if (displacement == 0) {
            // register offset without any work beforehand.
            return new AddressGenerationPlan(NO_WORK, REGISTER_OFFSET, false);
        } else {
            if (hasIndexRegister) {
                if (displacementScalable) {
                    boolean needsScratch = !isArithmeticImmediate(scaledDisplacement);
                    return new AddressGenerationPlan(ADD_TO_INDEX, REGISTER_OFFSET, needsScratch);
                } else {
                    boolean needsScratch = !isArithmeticImmediate(displacement);
                    return new AddressGenerationPlan(ADD_TO_BASE, REGISTER_OFFSET, needsScratch);
                }
            } else {
                if (NumUtil.isSignedNbit(9, displacement)) {
                    return new AddressGenerationPlan(NO_WORK, IMMEDIATE_UNSCALED, false);
                } else if (displacementScalable && NumUtil.isUnsignedNbit(12, scaledDisplacement)) {
                    return new AddressGenerationPlan(NO_WORK, IMMEDIATE_SCALED, false);
                } else {
                    boolean needsScratch = !isArithmeticImmediate(displacement);
                    return new AddressGenerationPlan(ADD_TO_BASE, REGISTER_OFFSET, needsScratch);
                }
            }
        }
    }

    /**
     * Returns an Aarch64Address pointing to {@code base + displacement + index << log2(transferSize)}.
     *
     * @param base general purpose register. May not be null or the zero register.
     * @param displacement arbitrary displacement added to base.
     * @param index general purpose register. May not be null or the stack pointer.
     * @param signExtendIndex if true consider index register a word register that should be sign-extended before being
     *            added.
     * @param transferSize the memory transfer size in bytes. The log2 of this specifies how much the index register is
     *            scaled. If 0 no scaling is assumed. Can be 0, 1, 2, 4 or 8.
     * @param additionalReg additional register used either as a scratch register or as part of the final address,
     *            depending on whether allowOverwrite is true or not. May not be null or stackpointer.
     * @param allowOverwrite if true allows to change value of base or index register to generate address.
     * @return Aarch64Address pointing to memory at {@code base + displacement + index << log2(transferSize)}.
     */
    public Aarch64Address makeAddress(CiKind kind, CiValue base, long displacement, CiValue index, boolean signExtendIndex, int transferSize, CiValue additionalReg, boolean allowOverwrite) {
        assert Aarch64.isGeneralPurposeOrSpReg(base.asRegister()) && Aarch64.isGeneralPurposeOrZeroReg(index.asRegister()) && Aarch64.isGeneralPurposeOrZeroReg(additionalReg.asRegister());
        AddressGenerationPlan plan = generateAddressPlan(displacement, !index.equals(zr), transferSize);
        assert allowOverwrite || !Aarch64.zr.equals(additionalReg) || plan.workPlan == NO_WORK;
        assert !plan.needsScratch || !Aarch64.zr.equals(additionalReg);
        int log2Scale = NumUtil.log2Ceil(transferSize);
        long scaledDisplacement = displacement >> log2Scale;
        int immediate;
        switch (plan.workPlan) {
            case NO_WORK:
                if (plan.addressingMode == IMMEDIATE_SCALED) {
                    immediate = (int) scaledDisplacement;
                } else {
                    immediate = (int) displacement;
                }
                break;
            case ADD_TO_INDEX:
                CiValue oldIndex = index;
                index = allowOverwrite ? index : additionalReg;
                if (plan.needsScratch) {
                    mov(additionalReg.asRegister(), scaledDisplacement);
                    add(signExtendIndex ? 32 : 64, index.asRegister(), oldIndex.asRegister(), additionalReg.asRegister());
                } else {
                    add(signExtendIndex ? 32 : 64, index.asRegister(), oldIndex.asRegister(), (int) scaledDisplacement);
                }
                immediate = 0;
                break;
            case ADD_TO_BASE:
                CiValue oldBase = base;
                base = allowOverwrite ? base : additionalReg;
                if (plan.needsScratch) {
                    mov(additionalReg.asRegister(), displacement);
                    add(64, base.asRegister(), oldBase.asRegister(), additionalReg.asRegister());
                } else {
                    add(64, base.asRegister(), oldBase.asRegister(), (int) displacement);
                }
                immediate = 0;
                break;
            default:
                throw new Error("should not reach here");
        }
        Aarch64Address.AddressingMode addressingMode = plan.addressingMode;
        ExtendType extendType = null;
        if (addressingMode == REGISTER_OFFSET) {
            if (index.equals(zr)) {
                addressingMode = BASE_REGISTER_ONLY;
            } else if (signExtendIndex) {
                addressingMode = EXTENDED_REGISTER_OFFSET;
                extendType = ExtendType.SXTW;
            }
        }
        return Aarch64Address.createAddress(kind, addressingMode, base.asRegister(), index.asRegister(), immediate, transferSize != 0, extendType);
    }

    /**
     * Returns an Aarch64Address pointing to {@code base + displacement}. Specifies the memory transfer size to allow
     * some optimizations when building the address.
     *
     * @param base general purpose register. May not be null or the zero register.
     * @param displacement arbitrary displacement added to base.
     * @param transferSize the memory transfer size in bytes.
     * @param additionalReg additional register used either as a scratch register or as part of the final address,
     *            depending on whether allowOverwrite is true or not. May not be null, zero register or stackpointer.
     * @param allowOverwrite if true allows to change value of base or index register to generate address.
     * @return Aarch64Address pointing to memory at {@code base + displacement}.
     */
    public Aarch64Address makeAddress(CiKind kind, CiValue base, long displacement, CiValue additionalReg, int transferSize, boolean allowOverwrite) {
        assert Aarch64.isGeneralPurposeReg(additionalReg.asRegister());
        return makeAddress(kind, base, displacement, Aarch64.zr.asValue(), /* sign-extend */false, transferSize, additionalReg, allowOverwrite);
    }

    /**
     * Returns an Aarch64Address pointing to {@code base + displacement}. Fails if address cannot be represented without
     * overwriting base register or using a scratch register.
     *
     * @param base general purpose register. May not be null or the zero register.
     * @param displacement arbitrary displacement added to base.
     * @param transferSize the memory transfer size in bytes. The log2 of this specifies how much the index register is
     *            scaled. If 0 no scaling is assumed. Can be 0, 1, 2, 4 or 8.
     * @return Aarch64Address pointing to memory at {@code base + displacement}.
     */
    public Aarch64Address makeAddress(CiKind kind, CiValue base, long displacement, int transferSize) {
        return makeAddress(kind, base, displacement, Aarch64.zr.asValue(), /* signExtend */false, transferSize, Aarch64.zr.asValue(), /* allowOverwrite */false);
    }

    /**
     * Loads memory address into register.
     *
     * @param dst general purpose register. May not be null, zero-register or stackpointer.
     * @param address address whose value is loaded into dst. May not be null,
     *            {@link com.oracle.graal.asm.armv8.Aarch64Address.AddressingMode#IMMEDIATE_POST_INDEXED
     *            IMMEDIATE_POST_INDEXED} or
     *            {@link com.oracle.graal.asm.armv8.Aarch64Address.AddressingMode#IMMEDIATE_PRE_INDEXED
     *            IMMEDIATE_PRE_INDEXED}.
     * @param transferSize the memory transfer size in bytes. The log2 of this specifies how much the index register is
     *            scaled. Can be 1, 2, 4 or 8.
     */
    public void loadAddress(CiRegister dst, Aarch64Address address, int transferSize) {
        assert transferSize == 1 || transferSize == 2 || transferSize == 4 || transferSize == 8;
        assert Aarch64.isGeneralPurposeReg(dst);
        int shiftAmt = NumUtil.log2Ceil(transferSize);
        switch (address.getAddressingMode()) {
            case IMMEDIATE_SCALED:
                int scaledImmediate = address.getImmediateRaw() << shiftAmt;
                int lowerBits = scaledImmediate & NumUtil.getNbitNumberInt(12);
                int higherBits = scaledImmediate & ~NumUtil.getNbitNumberInt(12);
                boolean firstAdd = true;
                if (lowerBits != 0) {
                    add(64, dst, address.getBase(), lowerBits);
                    firstAdd = false;
                }
                if (higherBits != 0) {
                    CiRegister src = firstAdd ? address.getBase() : dst;
                    add(64, dst, src, higherBits);
                }
                break;
            case IMMEDIATE_UNSCALED:
                int immediate = address.getImmediateRaw();
                add(64, dst, address.getBase(), immediate);
                break;
            case REGISTER_OFFSET:
                add(64, dst, address.getBase(), address.getOffset(), ShiftType.LSL, address.isScaled() ? shiftAmt : 0);
                break;
            case EXTENDED_REGISTER_OFFSET:
                add(64, dst, address.getBase(), address.getOffset(), address.getExtendType(), address.isScaled() ? shiftAmt : 0);
                break;
            case PC_LITERAL:
                super.adr(dst, address.getImmediateRaw());
                break;
            case BASE_REGISTER_ONLY:
                movx(dst, address.getBase());
                break;
            default:
                throw new Error("should not reach here");
        }
    }

    public void membar(int barriers) {
        int instruction = 0xd5033f9f; // DSB SY
        emitInt(instruction);
        instruction = 0xd5033fdf; // ISB SY
        emitInt(instruction);
    }

    /**
     * Compare and swap implementation. In the end the scratchregister must hold the value stored in the address.
     *
     * @param size
     * @param newValue
     * @param cmpValue
     * @param address
     */
    public void cas(int size, CiRegister newValue, CiRegister cmpValue, Aarch64Address address) {
        assert scratchRegister != cmpValue;
        assert newValue != cmpValue;

        Label atomicFail = new Label();
        Label notEqualTocmpValue = new Label();

        bind(atomicFail);
        ldxr(size, scratchRegister, address); // scratch has the current Value
        cmp(size, cmpValue, scratchRegister); // compare scratch with cmpValue
        branchConditionally(ConditionFlag.NE, notEqualTocmpValue); // value was not equal to the cmpValue
        stxr(size, scratchRegister, newValue, address); // store newValue to address and result to scratch register

        // If the Condition is Equal then the stxr took place but it MIGHT have failed so we need to test for this.
        // If the scratch register is not 0 then there was an issue with atomicity so do the operation again
        cbnz(64, scratchRegister, atomicFail);
        mov(64, scratchRegister, cmpValue); // stxr succeeded, set scratch register to the cmp value to indicate success
        bind(notEqualTocmpValue);
        dmb(BarrierKind.ANY_ANY);
    }

    /**
     * Calculate the address and emit an appropriate branch instruction.
     * @param cc - the condition code
     * @param target - the target address
     */
//    public void jcc(ConditionFlag cc, int target) {
//        adrp(scratch, codeBuffer.position(), target);
//    }

    /**
     * Calculate the required offset for a given address relative to a position and construct
     * the appropriate adrp instruction
     * @param pos - the position (synthetic program counter)
     * @param address - the address of the 'label'
     */
//    public void adrp(CiRegister reg, int pos, int addr) {
//      super.adrp(reg, (addr >> 12) - (pos >> 12));
//    }



    /**
     * Calculate the address and emit an appropriate branch instruction.
     * @param cc - the condition code
     * @param target - the target address
     */
//    public void jcc(ConditionFlag cc, int target) {
//      adrp(scratch, codeBuffer.position(), target);
//    }

    /**
     * Calculate the required offset for a given address relative to a position and construct
     * the appropriate adrp instruction
     * @param pos - the position (synthetic program counter)
     * @param address - the address of the 'label'
     */
//    public void adrp(CiRegister reg, int pos, int addr) {
//      super.adrp(reg, (addr >> 12) - (pos >> 12));
//    }


    /**
     * Move 32 bit constant into a register as a 32bit register.
     * Unoptimised uses 2 instructions.
     *
     * @param reg
     * @param imm32
     */
    public void mov32BitConstant(CiRegister reg, int imm32) {
        movz(32, reg, imm32 & 0xFFFF, 0);
        movk(32, reg, (imm32 >> 16) & 0xFFFF, 16);
    }

    public void movx(CiRegister dst, CiRegister src) {
        mov(64, dst, src);
    }

    public void mov(int size, CiRegister dst, CiRegister src) {
        if (dst.equals(src)) {
            return;
        }
        if (Aarch64.isSp(dst) || Aarch64.isSp(src)) {
            add(size, dst, src, 0);
        } else {
            or(size, dst, src, Aarch64.zr);
        }
    }

    public void setUpScratch(CiAddress addr) {
        setUpRegister(scratchRegister, addr);
    }

    private int getRegisterList(CiRegister... registers) {
        int regList = 0;
        for (CiRegister reg : registers) {
            regList |= 1 << reg.getEncoding();
        }
        return regList;
    }

    /**
     * Push a register onto the stack using 16byte alignment.
     * @param reg
     */
    public void push(CiRegister reg) {
        if (ASM_DEBUG_MARKERS && (reg == Aarch64.linkRegister)) {
            stp(64, reg, Aarch64.zr, Aarch64Address.createPreIndexedImmediateAddress(Aarch64.sp, -2));
        } else {
            push(64, reg);
        }
    }

    public void push(int size, CiRegister reg) {
        str(size, reg, Aarch64Address.createPreIndexedImmediateAddress(Aarch64.sp, -16));
    }

    public void push(CiRegister... registers) {
        push(getRegisterList(registers));
    }

    /**
     * Push multiple registers onto the stack using 16byte alignment.
     * @param registerList
     * TODO: optimise
     */
    public void push(int registerList) {
        for (int regNumber = 0; regNumber < Integer.SIZE; regNumber++) {
            if (registerList % 2 == 1) {
                str(64, Aarch64.cpuRegisters[regNumber], Aarch64Address.createPreIndexedImmediateAddress(Aarch64.sp, -16));
            }

            registerList = registerList >> 1;
        }
    }

    public void fpush(CiRegister... registers) {
        fpush(getRegisterList(registers));
    }

    public void fpush(int registerList) {
        for (int regNumber = 0; regNumber < Integer.SIZE; regNumber++) {
            if (registerList % 2 == 1) {
                fstr(64, Aarch64.fpuRegisters[regNumber], Aarch64Address.createPreIndexedImmediateAddress(Aarch64.sp, -16));
            }

            registerList = registerList >> 1;
        }
    }

    /**
     * Pop the value from the top of the stack into register. Uses 16byte alignment.
     * @param reg
     */
    public void pop(CiRegister reg) {
        if (ASM_DEBUG_MARKERS && (reg == Aarch64.linkRegister)) {
            ldp(64, reg, zr, Aarch64Address.createPostIndexedImmediateAddress(Aarch64.sp, 2));
        } else {
            pop(64, reg);
        }
    }

    public void pop(int size, CiRegister reg) {
        ldr(size, reg, Aarch64Address.createPostIndexedImmediateAddress(Aarch64.sp, 16));
    }

    public void pop(CiRegister... registers) {
        pop(getRegisterList(registers));
    }

    /**
     * Push multiple registers onto the stack using 16byte alignment.
     * @param registerList
     * TODO: optimise
     */
    public void pop(int registerList) {
        for (int regNumber = Integer.SIZE - 1; regNumber >= 0; regNumber--) {
            if ((registerList >> regNumber) % 2 == 1) {
                ldr(64, Aarch64.cpuRegisters[regNumber], Aarch64Address.createPostIndexedImmediateAddress(Aarch64.sp, 16));
            }
        }
    }

    public void fpop(CiRegister... registers) {
        fpop(getRegisterList(registers));
    }

    public void fpop(int registerList) {
        for (int regNumber = Integer.SIZE - 1; regNumber >= 0; regNumber--) {
            if ((registerList >> regNumber) % 2 == 1) {
                fldr(64, Aarch64.fpuRegisters[regNumber], Aarch64Address.createPostIndexedImmediateAddress(Aarch64.sp, 16));
            }
        }
    }

    /**
     * Generates a move 64-bit immediate code sequence. The immediate may later be updated by HotSpot.
     *
     * @param dst general purpose register. May not be null, stackpointer or zero-register.
     * @param imm
     */
    public void forceMov(CiRegister dst, long imm, boolean optimize) {
        // We have to move all non zero parts of the immediate in 16-bit chunks
        boolean firstMove = true;
        for (int offset = 0; offset < 64; offset += 16) {
            int chunk = (int) (imm >> offset) & NumUtil.getNbitNumberInt(16);
            if (optimize && chunk == 0) {
                continue;
            }
            if (firstMove) {
                movz(64, dst, chunk, offset);
                firstMove = false;
            } else {
                movk(64, dst, chunk, offset);
            }
        }
        assert !firstMove;
    }

    public void forceMov(CiRegister dst, long imm) {
        forceMov(dst, imm, /* optimize */false);
    }

    /**
     * Generates a move 64-bit immediate code sequence. The immediate may later be updated by HotSpot.
     *
     * @param dst general purpose register. May not be null, stackpointer or zero-register.
     */
    public void forceMov(CiRegister dst, int imm) {
        forceMov(dst, NumUtil.asZeroExtendedLong(imm));
    }

    /**
     * Loads immediate into register.
     *
     * @param dst general purpose register. May not be null, zero-register or stackpointer.
     * @param imm immediate loaded into register.
     */
    public void mov(CiRegister dst, long imm) {
        assert Aarch64.isGeneralPurposeReg(dst);
        if (imm == 0L) {
            movz(64, dst, 0, 0);
        } else if (Aarch64LogicalImmediateTable.isRepresentable(true, imm) != Aarch64LogicalImmediateTable.Representable.NO) {
            or(64, dst, Aarch64.zr, imm);
        } else if (imm >> 32 == -1L && (int) imm < 0 && Aarch64LogicalImmediateTable.isRepresentable((int) imm) != Aarch64LogicalImmediateTable.Representable.NO) {
            // If the higher 32-bit are 1s and the sign bit of the lower 32-bits is set *and* we can
            // represent the lower 32 bits as a logical immediate we can create the lower 32-bit and then sign extend
            // them. This allows us to cover immediates like ~1L with 2 instructions.
            mov32BitConstant(dst, (int) imm);
            sxt(64, 32, dst, dst);
        } else {
            forceMov(dst, imm, /* optimize move */true);
        }
    }

    /**
     * Loads immediate into register.
     *
     * @param dst general purpose register. May not be null, zero-register or stackpointer.
     * @param imm immediate loaded into register.
     */
    public void mov(CiRegister dst, int imm) {
        mov(dst, (long) imm);
    }

    public void add(int size, CiRegister dest, CiRegister source, long delta) {
        if (delta == 0) {
            mov(size, dest, source);
        } else if (isArithmeticImmediate(delta)) {
            assert delta == (int) delta;
            if (delta < 0) {
                super.sub(size, dest, source, -(int) delta);
            } else {
                super.add(size, dest, source, (int) delta);
            }
        } else {
            CiRegister deltaRegister;
            if (source == scratchRegister) {
                deltaRegister = Aarch64.r17;
            } else {
                deltaRegister = scratchRegister;
            }

            mov(deltaRegister, delta);
            add(size, dest, source, deltaRegister);
        }
    }

    public void sub(int size, CiRegister dest, CiRegister source, long delta) {
        if (delta == 0) {
            mov(size, dest, source);
        } else if (isArithmeticImmediate(delta)) {
            assert delta == (int) delta;
            if (delta < 0) {
                super.add(size, dest, source, -(int) delta);
            } else {
                super.sub(size, dest, source, (int) delta);
            }
        } else {
            CiRegister deltaRegister;
            if (source == scratchRegister) {
                deltaRegister = Aarch64.r17;
            } else {
                deltaRegister = scratchRegister;
            }

            mov(deltaRegister, delta);
            sub(size, dest, source, deltaRegister);
        }
    }

    /**
     * Applies a delta value to the contents of reg as a 32bit quantity.
     * @param reg
     * @param delta
     */
    public void increment32(CiRegister reg, int delta) {
        add(32, reg, reg, (long) delta);
    }

    public void restore(CiCalleeSaveLayout csl, int frameToCSA) {
        for (CiRegister r : csl.registers) {
            int displacement = csl.offsetOf(r) + frameToCSA;
            final Aarch64Address address = getAddressInFrame(frameRegister, displacement);
            if (r.isCpu()) {
                ldr(64, r, address);
            } else if (r.isFpu()) {
                fldr(64, r, address);
            }
        }
    }

    public Aarch64Address getAddressInFrame(CiRegister frameRegister, int displacement) {
        if (NumUtil.isSignedNbit(9, displacement)) {
            return Aarch64Address.createUnscaledImmediateAddress(frameRegister, displacement);
        } else {
            mov(scratchRegister, displacement);
            return Aarch64Address.createRegisterOffsetAddress(frameRegister, scratchRegister, false);
        }
    }

    /**
     * @return Number of instructions necessary to load immediate into register.
     */
    public static int nrInstructionsToMoveImmediate(long imm) {
        if (imm == 0L || Aarch64LogicalImmediateTable.isRepresentable(true, imm) != Aarch64LogicalImmediateTable.Representable.NO) {
            return 1;
        }
        if (imm >> 32 == -1L && (int) imm < 0 && Aarch64LogicalImmediateTable.isRepresentable((int) imm) != Aarch64LogicalImmediateTable.Representable.NO) {
            // If the higher 32-bit are 1s and the sign bit of the lower 32-bits is set *and* we can
            // represent the lower 32 bits as a logical immediate we can create the lower 32-bit and then sign extend
            // them. This allows us to cover immediates like ~1L with 2 instructions.
            return 2;
        }
        int nrInstructions = 0;
        for (int offset = 0; offset < 64; offset += 16) {
            int part = (int) (imm >> offset) & NumUtil.getNbitNumberInt(16);
            if (part != 0) {
                nrInstructions++;
            }
        }
        return nrInstructions;
    }

    /**
     * Loads a srcSize value from address into rt sign-extending it if necessary.
     *
     * @param targetSize size of target register in bits. Must be 32 or 64.
     * @param srcSize size of memory read in bits. Must be 8, 16 or 32 and smaller or equal to targetSize.
     * @param rt general purpose register. May not be null or stackpointer.
     * @param address all addressing modes allowed. May not be null.
     */
    @Override
    public void ldrs(int targetSize, int srcSize, CiRegister rt, Aarch64Address address) {
        assert targetSize == 32 || targetSize == 64;
        assert srcSize <= targetSize;
        if (targetSize == srcSize) {
            super.ldr(srcSize, rt, address);
        } else {
            super.ldrs(targetSize, srcSize, rt, address);
        }
    }

    /**
     * Conditional move. dst = src1 if condition else src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param result general purpose register. May not be null or the stackpointer.
     * @param trueValue general purpose register. May not be null or the stackpointer.
     * @param falseValue general purpose register. May not be null or the stackpointer.
     * @param cond any condition flag. May not be null.
     */
    public void cmov(int size, CiRegister result, CiRegister trueValue, CiRegister falseValue, ConditionFlag cond) {
        super.csel(size, result, trueValue, falseValue, cond);
    }

    /**
     * Conditional set. dst = 1 if condition else 0.
     *
     * @param dst general purpose register. May not be null or stackpointer.
     * @param condition any condition. May not be null.
     */
    public void cset(CiRegister dst, ConditionFlag condition) {
        super.csinc(32, dst, zr, zr, condition.negate());
    }

    /**
     * Add a long value to a register using scratch register as temporary.
     *
     * @param reg
     * @param value
     */
    public void addq(CiRegister reg, long value) {
        mov64BitConstant(scratchRegister, value);
        add(64, reg, reg, scratchRegister);
    }

    /**
     * dst = src1 + src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     */
    public void add(int size, CiRegister dst, CiRegister src1, CiRegister src2) {
        super.add(size, dst, src1, src2, ShiftType.LSL, 0);
    }

    /**
     * Sub a long value from a register using R15 as temporary.
     * @param reg
     * @param value
     */
    public void subq(CiRegister reg, long value) {
        mov64BitConstant(scratchRegister, value);
        sub(64, reg, reg, scratchRegister);
    }

    /**
     * dst = src1 - src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     */
    public void sub(int size, CiRegister dst, CiRegister src1, CiRegister src2) {
        super.sub(size, dst, src1, src2, ShiftType.LSL, 0);
    }

    /**
     * dst = src1 + shiftType(src2, shiftAmt & (size - 1)).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param shiftType any type but ROR.
     * @param shiftAmt arbitrary shift amount.
     */
    @Override
    public void add(int size, CiRegister dst, CiRegister src1, CiRegister src2, ShiftType shiftType, int shiftAmt) {
        shiftAmt = clampShiftAmt(size, shiftAmt);
        super.add(size, dst, src1, src2, shiftType, shiftAmt);
    }

    /**
     * dst = src1 + shiftType(src2, shiftAmt & (size-1)) and sets condition flags.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     * @param shiftType any type but ROR.
     * @param shiftAmt arbitrary shift amount.
     */
    @Override
    public void sub(int size, CiRegister dst, CiRegister src1, CiRegister src2, ShiftType shiftType, int shiftAmt) {
        shiftAmt = clampShiftAmt(size, shiftAmt);
        super.sub(size, dst, src1, src2, shiftType, shiftAmt);
    }

    /**
     * dst = -src1.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src general purpose register. May not be null or stackpointer.
     */
    public void neg(int size, CiRegister dst, CiRegister src) {
        sub(size, dst, Aarch64.zr, src);
    }

    /**
     * dst = src + immediate.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src general purpose register. May not be null or stackpointer.
     * @param immediate arithmetic immediate
     */
    @Override
    public void add(int size, CiRegister dst, CiRegister src, int immediate) {
        assert immediate != Integer.MIN_VALUE;
        if (immediate < 0) {
            sub(size, dst, src, -immediate);
        } else if (!dst.equals(src) || immediate != 0) {
            super.add(size, dst, src, immediate);
        }
    }

    /**
     * dst = src - immediate.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src general purpose register. May not be null or stackpointer.
     * @param immediate arithmetic immediate
     */
    @Override
    public void sub(int size, CiRegister dst, CiRegister src, int immediate) {
        assert immediate != Integer.MIN_VALUE;
        if (immediate < 0) {
            add(size, dst, src, -immediate);
        } else if (!dst.equals(src) || immediate != 0) {
            super.sub(size, dst, src, immediate);
        }
    }

    public final void decq(CiRegister dst) {
        assert dst.isValid();
        sub(64, dst, dst, 1);
    }

    public final void incq(CiRegister dst) {
        assert dst.isValid();
        add(64, dst, dst, 1);
    }

    /**
     * dst = src1 * src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or the stackpointer.
     * @param src1 general purpose register. May not be null or the stackpointer.
     * @param src2 general purpose register. May not be null or the stackpointer.
     */
    public void mul(int size, CiRegister dst, CiRegister src1, CiRegister src2) {
        super.madd(size, dst, src1, src2, Aarch64.zr);
    }

    /**
     * dst = src1 % src2. Signed.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or the stackpointer.
     * @param n numerator. General purpose register. May not be null or the stackpointer.
     * @param d denominator. General purpose register. Divisor May not be null or the stackpointer.
     */
    public void rem(int size, CiRegister dst, CiRegister n, CiRegister d) {
        // There is no irem or similar instruction. Instead we use the relation:
        // n % d = n - Floor(n / d) * d if nd >= 0
        // n % d = n - Ceil(n / d) * d else
        // Which is equivalent to n - TruncatingDivision(n, d) * d
        super.sdiv(size, dst, n, d);
        super.msub(size, dst, dst, d, n);
    }

    /**
     * dst = src1 % src2. Unsigned.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or the stackpointer.
     * @param n numerator. General purpose register. May not be null or the stackpointer.
     * @param d denominator. General purpose register. Divisor May not be null or the stackpointer.
     */
    public void urem(int size, CiRegister dst, CiRegister n, CiRegister d) {
        // There is no irem or similar instruction. Instead we use the relation:
        // n % d = n - Floor(n / d) * d
        // Which is equivalent to n - TruncatingDivision(n, d) * d
        super.udiv(size, dst, n, d);
        super.msub(size, dst, dst, d, n);
    }

    /**
     * @return True if immediate can be used directly for arithmetic instructions (add/sub), false otherwise.
     */
    public static boolean isArithmeticImmediate(long imm) {
        // If we have a negative immediate we just use the opposite operator. I.e.: x - (-5) == x + 5.
        return NumUtil.isInt(Math.abs(imm)) && isAimm((int) Math.abs(imm));
    }

    /**
     * @return True if immediate can be used directly with comparison instructions, false otherwise.
     */
    public static boolean isComparisonImmediate(long imm) {
        return isArithmeticImmediate(imm);
    }

    /**
     * @return True if immediate can be moved directly into a register, false otherwise.
     */
    public static boolean isMovableImmediate(long imm) {
        // Moves allow a 16bit immediate value that can be shifted by multiples of 16.
        // Positions of first, respectively last set bit.
        int start = Long.numberOfTrailingZeros(imm);
        int end = 64 - Long.numberOfLeadingZeros(imm);
        int length = end - start;
        if (length > 16) {
            return false;
        }
        // We can shift the necessary part of the immediate (i.e. everything between the first and
        // last set bit) by as much as 16 - length around to arrive at a valid shift amount
        int tolerance = 16 - length;
        int prevMultiple = NumUtil.roundDown(start, 16);
        int nextMultiple = NumUtil.roundUp(start, 16);
        return start - prevMultiple <= tolerance || nextMultiple - start <= tolerance;
    }

    /**
     * dst = src << (shiftAmt & (size - 1)).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null, stackpointer or zero-register.
     * @param src general purpose register. May not be null, stackpointer or zero-register.
     * @param shiftAmt amount by which src is shifted.
     */
    public void shl(int size, CiRegister dst, CiRegister src, long shiftAmt) {
        int shift = clampShiftAmt(size, shiftAmt);
        super.ubfm(size, dst, src, (size - shift) & (size - 1), size - 1 - shift);
    }

    /**
     * dst = src1 << (src2 & (size - 1)).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src general purpose register. May not be null or stackpointer.
     * @param shift general purpose register. May not be null or stackpointer.
     */
    public void shl(int size, CiRegister dst, CiRegister src, CiRegister shift) {
        super.lsl(size, dst, src, shift);
    }

    /**
     * dst = src >>> (shiftAmt & (size - 1)).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null, stackpointer or zero-register.
     * @param src general purpose register. May not be null, stackpointer or zero-register.
     * @param shiftAmt amount by which src is shifted.
     */
    public void lshr(int size, CiRegister dst, CiRegister src, long shiftAmt) {
        int shift = clampShiftAmt(size, shiftAmt);
        super.ubfm(size, dst, src, shift, size - 1);
    }

    /**
     * dst = src1 >>> (src2 & (size - 1)).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src general purpose register. May not be null or stackpointer.
     * @param shift general purpose register. May not be null or stackpointer.
     */
    public void lshr(int size, CiRegister dst, CiRegister src, CiRegister shift) {
        super.lsr(size, dst, src, shift);
    }

    /**
     * dst = src >> (shiftAmt & (size-1)).
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null, stackpointer or zero-register.
     * @param src general purpose register. May not be null, stackpointer or zero-register.
     * @param shiftAmt amount by which src is shifted.
     */
    public void ashr(int size, CiRegister dst, CiRegister src, long shiftAmt) {
        int shift = clampShiftAmt(size, shiftAmt);
        super.sbfm(size, dst, src, shift, size - 1);
    }

    /**
     * dst = src1 >> src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src general purpose register. May not be null or stackpointer.
     * @param shift general purpose register. May not be null or stackpointer.
     */
    public void ashr(int size, CiRegister dst, CiRegister src, CiRegister shift) {
        super.asr(size, dst, src, shift);
    }

    /**
     * Clamps shiftAmt into range 0 <= shiftamt < size according to JLS.
     *
     * @param size size of operation.
     * @param shiftAmt arbitrary shift amount.
     * @return value between 0 and size - 1 inclusive that is equivalent to shiftAmt according to JLS.
     */
    private static int clampShiftAmt(int size, long shiftAmt) {
        return (int) (shiftAmt & (size - 1));
    }

    /**
     * dst = src1 & src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src general purpose register. May not be null or stackpointer.
     * @param bimm logical immediate.
     */
    public void and(int size, CiRegister dst, CiRegister src, long bimm) {
        if (isLogicalImmediate(bimm)) {
            super.and(size, dst, src, bimm);
            return;
        }

        mov(scratchRegister, bimm);
        and(size, dst, src, scratchRegister);
    }

    /**
     * dst = src1 & src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     */
    public void and(int size, CiRegister dst, CiRegister src1, CiRegister src2) {
        super.and(size, dst, src1, src2, ShiftType.LSL, 0);
    }

    /**
     * dst = src1 ^ src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     */
    public void eor(int size, CiRegister dst, CiRegister src1, CiRegister src2) {
        super.eor(size, dst, src1, src2, ShiftType.LSL, 0);
    }

    public void xorq(CiRegister dest, CiAddress src) {
        assert dest.isValid();
        setUpScratch(src);
        eor(64, dest, dest, scratchRegister);
    }

    public void xorq(CiRegister dest, CiRegister src) {
        assert dest.isValid();
        assert src.isValid();
        eor(64, dest, dest, src);
    }

    /**
     * dst = src1 | src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src1 general purpose register. May not be null or stackpointer.
     * @param src2 general purpose register. May not be null or stackpointer.
     */
    public void or(int size, CiRegister dst, CiRegister src1, CiRegister src2) {
        super.orr(size, dst, src1, src2, ShiftType.LSL, 0);
    }

    /**
     * dst = src | bimm.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or zero-register.
     * @param src general purpose register. May not be null or stack-pointer.
     * @param bimm logical immediate. See {@link com.oracle.graal.asm.armv8.Aarch64LogicalImmediateTable
     *            Aarch64LogicalImmediateTable} for exact definition.
     */
    public void or(int size, CiRegister dst, CiRegister src, long bimm) {
        super.orr(size, dst, src, bimm);
    }

    /**
     * dst = ~src.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src general purpose register. May not be null or stackpointer.
     */
    public void not(int size, CiRegister dst, CiRegister src) {
        super.orn(size, dst, Aarch64.zr, src, ShiftType.LSL, 0);
    }

    /**
     * Sign-extend value from src into dst.
     *
     * @param destSize destination register size. Has to be 32 or 64.
     * @param srcSize source register size. May be 8, 16 or 32 and smaller than destSize.
     * @param dst general purpose register. May not be null, stackpointer or zero-register.
     * @param src general purpose register. May not be null, stackpointer or zero-register.
     */
    public void sxt(int destSize, int srcSize, CiRegister dst, CiRegister src) {
        assert (destSize == 32 || destSize == 64) && srcSize < destSize;
        assert srcSize == 8 || srcSize == 16 || srcSize == 32;
        int[] srcSizeValues = {7, 15, 31};
        super.sbfm(destSize, dst, src, 0, srcSizeValues[NumUtil.log2Ceil(srcSize / 8)]);
    }

    /**
     * dst = src if condition else -src.
     *
     * @param size register size. Must be 32 or 64.
     * @param dst general purpose register. May not be null or the stackpointer.
     * @param src general purpose register. May not be null or the stackpointer.
     * @param condition any condition except AV or NV. May not be null.
     */
    public void csneg(int size, CiRegister dst, CiRegister src, ConditionFlag condition) {
        super.csneg(size, dst, src, src, condition.negate());
    }

    /**
     * @return True if the immediate can be used directly for logical 64-bit instructions.
     */
    public static boolean isLogicalImmediate(long imm) {
        return Aarch64LogicalImmediateTable.isRepresentable(true, imm) != Aarch64LogicalImmediateTable.Representable.NO;
    }

    /**
     * @return True if the immediate can be used directly for logical 32-bit instructions.
     */
    public static boolean isLogicalImmediate(int imm) {
        return Aarch64LogicalImmediateTable.isRepresentable(imm) == Aarch64LogicalImmediateTable.Representable.YES;
    }

    /* Float instructions */

    /**
     * Moves integer to float, float to integer, or float to float. Does not support integer to integer moves.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst Either floating-point or general-purpose register. If general-purpose register may not be stackpointer
     *            or zero register. Cannot be null in any case.
     * @param src Either floating-point or general-purpose register. If general-purpose register may not be
     *            stackpointer. Cannot be null in any case.
     */
    @Override
    public void fmov(int size, CiRegister dst, CiRegister src) {
        assert !(Aarch64.isIntReg(dst) && Aarch64.isIntReg(src)) : "src and dst cannot both be integer registers.";
        if (dst.equals(src)) {
            return;
        }
        if (Aarch64.isIntReg(dst)) {
            super.fmovFpu2Cpu(size, dst, src);
        } else if (Aarch64.isIntReg(src)) {
            super.fmovCpu2Fpu(size, dst, src);
        } else {
            super.fmov(size, dst, src);
        }
    }

    /**
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst floating point register. May not be null.
     * @param imm immediate that is loaded into dst. If size is 32 only float immediates can be loaded, i.e. (float) imm
     *            == imm must be true. In all cases {@code isFloatImmediate}, respectively {@code #isDoubleImmediate}
     *            must be true depending on size.
     */
    @Override
    public void fmov(int size, CiRegister dst, double imm) {
        if (imm == 0.0) {
            assert Double.doubleToRawLongBits(imm) == 0L : "-0.0 is no valid immediate.";
            super.fmovCpu2Fpu(size, dst, Aarch64.zr);
        } else {
            super.fmov(size, dst, imm);
        }
    }

    /**
     *
     * @return true if immediate can be loaded directly into floating-point register, false otherwise.
     */
    public static boolean isDoubleImmediate(double imm) {
        return Double.doubleToRawLongBits(imm) == 0L || Aarch64Assembler.isDoubleImmediate(imm);
    }

    /**
     *
     * @return true if immediate can be loaded directly into floating-point register, false otherwise.
     */
    public static boolean isFloatImmediate(float imm) {
        return Float.floatToRawIntBits(imm) == 0 || Aarch64Assembler.isFloatImmediate(imm);
    }

    /**
     * Conditional move. dst = src1 if condition else src2.
     *
     * @param size register size.
     * @param result floating point register. May not be null.
     * @param trueValue floating point register. May not be null.
     * @param falseValue floating point register. May not be null.
     * @param condition every condition allowed. May not be null.
     */
    public void fcmov(int size, CiRegister result, CiRegister trueValue, CiRegister falseValue, ConditionFlag condition) {
        super.fcsel(size, result, trueValue, falseValue, condition);
    }

    /**
     * dst = src1 % src2.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst floating-point register. May not be null.
     * @param n numerator. Floating-point register. May not be null.
     * @param d denominator. Floating-point register. May not be null.
     */
    public void frem(int size, CiRegister dst, CiRegister n, CiRegister d) {
        // There is no frem instruction, instead we compute the remainder using the relation:
        // rem = n - Truncating(n / d) * d
        super.fdiv(size, Aarch64.d31, n, d); // Use d31 as scratch to support dst == n
        super.frintz(size, Aarch64.d31, Aarch64.d31);
        super.fmsub(size, dst, Aarch64.d31, d, n);
    }

    /* Branches */

    /**
     * Compares x and y and sets condition flags.
     *
     * @param size register size. Has to be 32 or 64.
     * @param x general purpose register. May not be null or stackpointer.
     * @param y general purpose register. May not be null or stackpointer.
     */
    public void cmp(int size, CiRegister x, CiRegister y) {
        super.subs(size, Aarch64.zr, x, y, ShiftType.LSL, 0);
    }

    /**
     * Compares x to y and sets condition flags.
     *
     * @param size register size. Has to be 32 or 64.
     * @param x general purpose register. May not be null or stackpointer.
     * @param y comparison immediate
     */
    public void cmp(int size, CiRegister x, long y) {
        if (isComparisonImmediate(y)) {
            if (y < 0) {
                super.adds(size, Aarch64.zr, x, (int) -y);
            } else {
                super.subs(size, Aarch64.zr, x, (int) y);
            }
        } else {
            mov(scratchRegister, y);
            cmp(size, x, scratchRegister);
        }
    }

    /**
     * Sets condition flags according to result of x & y.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stack-pointer.
     * @param x general purpose register. May not be null or stackpointer.
     * @param y general purpose register. May not be null or stackpointer.
     */
    public void ands(int size, CiRegister dst, CiRegister x, CiRegister y) {
        super.ands(size, dst, x, y, ShiftType.LSL, 0);
    }

    /**
     * Branch unconditionally to a label.
     * @param label
     */
    public void b(Label label) {
        // TODO Handle case where offset is too large for a single
        // branch immediate instruction.
        if (label.isBound()) {
            int offset = label.position() - codeBuffer.position();
            b(offset);
        } else {
            label.addPatchAt(codeBuffer.position());
            emitByte(PatchLabelKind.BRANCH_UNCONDITIONALLY.encoding);
            emitByte(0);
            emitShort(0);
        }
    }

    /**
     * Compare register and branch if non-zero.
     *
     * @param size Instruction size in bits. Should be either 32 or 64.
     * @param cmp general purpose register. May not be null, zero-register or stackpointer.
     * @param label Can only handle 21-bit word-aligned offsets for now. May be unbound. Non null.
     */
    public void cbnz(int size, CiRegister cmp, Label label) {
        // TODO Handle case where offset is too large for a single jump instruction
        if (label.isBound()) {
            int offset = label.position() - codeBuffer.position();
            super.cbnz(size, cmp, offset);
        } else {
            label.addPatchAt(codeBuffer.position());
            int regEncoding = cmp.getEncoding();
            emitByte(PatchLabelKind.BRANCH_NONZERO.encoding);
            emitByte(size);
            emitShort(regEncoding);
        }
    }

    /**
     * Compare register and branch if zero.
     *
     * @param size Instruction size in bits. Should be either 32 or 64.
     * @param cmp general purpose register. May not be null, zero-register or stackpointer.
     * @param label Can only handle 21-bit word-aligned offsets for now. May be unbound. Non null.
     */
    public void cbz(int size, CiRegister cmp, Label label) {
        // TODO Handle case where offset is too large for a single jump instruction
        if (label.isBound()) {
            int offset = label.position() - codeBuffer.position();
            super.cbz(size, cmp, offset);
        } else {
            label.addPatchAt(codeBuffer.position());
            int regEncoding = cmp.getEncoding();
            emitByte(PatchLabelKind.BRANCH_ZERO.encoding);
            emitByte(size);
            emitShort(regEncoding);
        }
    }

    /**
     * Branches to label if condition is true.
     *
     * @param condition any condition value allowed. Non null.
     * @param label Can only handle 21-bit word-aligned offsets for now. May be unbound. Non null.
     */
    public void branchConditionally(ConditionFlag condition, Label label) {
        // TODO Handle case where offset is too large for a single jump instruction
        if (label.isBound()) {
            int offset = label.position() - codeBuffer.position();
            super.b(condition, offset);
        } else {
            label.addPatchAt(codeBuffer.position());
            emitByte(PatchLabelKind.BRANCH_CONDITIONALLY.encoding);
            emitByte(condition.encoding);
            emitShort(0);
        }
    }

    /**
     * Branches if condition is true. Address of jump is patched up by HotSpot c++ code.
     *
     * @param condition any condition value allowed. Non null.
     */
    public void branchConditionally(ConditionFlag condition) {
        // Correct offset is fixed up by HotSpot later.
        super.b(condition, 0);
    }

    public void save(CiCalleeSaveLayout csl, int frameToCSA) {
        for (CiRegister r : csl.registers) {
            int displacement = frameToCSA + csl.offsetOf(r);
            final Aarch64Address address = getAddressInFrame(frameRegister, displacement);
            if (r.isCpu()) {
                str(64, r, address);
            } else {
                fstr(64, r, address);
            }
        }
    }

// /**
// * Jumps to label.
// *
// * param label Can only handle signed 28-bit offsets. May be unbound. Non null.
// */
// @Override
// public void jmp(Label label) {
// // TODO Handle case where offset is too large for a single jump instruction
// if (label.isBound()) {
// int offset = label.position() - codeBuffer.position();
// super.b(offset);
// } else {
// label.addPatchAt(codeBuffer.position());
// emitInt(PatchLabelKind.BRANCH_UNCONDITIONALLY.encoding);
// }
// }

    /**
     * Jump to address in dest.
     *
     * @param dest General purpose register. May not be null, zero-register or stackpointer.
     */
    public void jmp(CiRegister dest) {
        super.br(dest);
    }

    /* system instructions */

    /**
     * Halting mode software breakpoint: Enters halting mode debug state if enabled, else treated as UNALLOCATED
     * instruction.
     *
     * @param exceptionCode exception code specifying why halt was called. Non null.
     */
    public void hlt(Aarch64ExceptionCode exceptionCode) {
        super.hlt(exceptionCode.encoding);
    }

    /**
     * Monitor mode software breakpoint: exception routed to a debug monitor executing in a higher exception level.
     *
     * @param exceptionCode exception code specifying why break was called. Non null.
     */
    public void brk(Aarch64ExceptionCode exceptionCode) {
        super.brk(exceptionCode.encoding);
    }

    public void ensureUniquePC() {
        nop();
    }

    /**
     * Emit a function call, i.e. branch and link. Trampolines are used to reach targets beyond the
     * Aarch64 +/-128MiB unconditional branch limit, see {@linkplain Aarch64Assembler#trampolines(int) trampolines}. 
     *.
     */
    public final void call() {
        bl(0);
    }

    public final void call(CiRegister src) {
        blr(src);
    }

    public final void ret() {
        pop(Aarch64.linkRegister);
        ret(Aarch64.linkRegister);
    }

    public void leaq(CiRegister dest, CiAddress addr) {
        if (addr == CiAddress.Placeholder) {
            nop(4);
        } else {
            setUpScratch(addr);
            mov(64, dest, scratchRegister);
        }
    }

    public void pause() {
        hint(SystemHint.YIELD);
    }

    public final void crashme() {
        mov(scratchRegister, 0);
        ldr(64, scratchRegister, Aarch64Address.createBaseRegisterOnlyAddress(scratchRegister));
        insertForeverLoop();
    }

    public int insertDivByZeroCheck(int size, CiRegister denominator) {
        cmp(size, denominator, 0); // Check denominator
        b(ConditionFlag.NE, 12);  // If non-zero skip the following two commands
        movz(64, scratchRegister, 0, 0);
        int offset = codeBuffer.position();
        ldr(64, zr, Aarch64Address.createBaseRegisterOnlyAddress(scratchRegister)); // generate SIGSEGV
        return offset;
    }

    public void insertForeverLoop() {
        b(0);
    }

    public void store(CiRegister src, CiAddress addr, CiKind kind) {
        Aarch64Address address = calculateAddress(addr);
        switch (kind) {
            case Boolean:
            case Byte:
                str(8, src, address);
                break;
            case Char:
            case Short:
                str(16, src, address);
                break;
            case Int:
                str(32, src, address);
                break;
            case Object:
            case Long:
                str(64, src, address);
                break;
            case Float:
                fstr(32, src, address);
                break;
            case Double:
                fstr(64, src, address);
                break;
            default:
                assert false : "Unknown kind!";
        }
    }

    public final void ucomisd(int size, CiRegister dst, CiRegister src, CiKind destKind, CiKind srcKind) {
        assert destKind.isFloatOrDouble();
        assert srcKind.isFloatOrDouble();
        fcmp(size, dst, src);
    }

    public void load(CiRegister dest, CiAddress addr, CiKind kind) {
        Aarch64Address address = calculateAddress(addr);

        switch (kind) {
            case Byte:
                ldrs(64, 8, dest, address);
                break;
            case Boolean:
                mov(dest, 0);
                ldr(8, dest, address);
                break;
            case Char:
                mov(dest, 0);
                ldr(16, dest, address);
                break;
            case Short:
                ldrs(64, 16, dest, address);
                break;
            case Int:
                ldrs(64, 32, dest, address);
                break;
            case Object:
            case Long:
                ldr(64, dest, address);
                break;
            case Float:
                fldr(32, dest, address);
                break;
            case Double:
                fldr(64, dest, address);
                break;
            default:
                assert false : "Unknown kind!";
        }
    }

    private Aarch64Address calculateAddress(CiAddress addr) {
        if (addr instanceof Aarch64Address) {
            return (Aarch64Address) addr;
        }

        CiRegister base = addr.base();
        CiRegister index = addr.index();
        CiAddress.Scale scale = addr.scale;
        int disp = addr.displacement;

        assert addr != CiAddress.Placeholder;
        assert base.isValid() || base.compareTo(CiRegister.Frame) == 0;

        if (base == CiRegister.Frame) {
            base = frameRegister;
        }

        if (addr.index.isLegal()) {
            addlsl(scratchRegister, base, index, scale.log2);
            base = scratchRegister;
        }

        if (disp != 0) {
            if (NumUtil.isSignedNbit(9, disp)) {
                return Aarch64Address.createUnscaledImmediateAddress(base, disp);
            } else {
                mov(r17, disp);
                return Aarch64Address.createRegisterOffsetAddress(base, r17, false);
            }
        }

        return Aarch64Address.createBaseRegisterOnlyAddress(base);
    }

    public void setUpRegister(CiRegister dest, CiAddress addr) {
        CiRegister base = addr.base();
        CiRegister index = addr.index();
        CiAddress.Scale scale = addr.scale;
        int disp = addr.displacement;
        if (addr == CiAddress.Placeholder) {
            nop(4);
            return;
        }

        assert !(base.isValid() && disp == 0 && base.compareTo(Aarch64.LATCH_REGISTER) == 0);
        assert base.isValid() || base.compareTo(CiRegister.Frame) == 0;

        if (base == CiRegister.Frame) {
            base = frameRegister;
        }

        assert base.isValid();

        if (disp != 0) {
            if (isAimm(Math.abs(disp))) {
                add(64, dest, base, disp);
            } else {
                mov32BitConstant(dest, disp);
                add(64, dest, dest, base);
            }
            base = dest;
        } else if (!index.isValid()) {
            mov(64, dest, base);
        }
        if (index.isValid()) {
            addlsl(dest, base, index, scale.log2);
        }
    }

    public void nullCheck(CiRegister r) {
        Aarch64Address address = Aarch64Address.createBaseRegisterOnlyAddress(r);
        ldr(64, zr, address);
    }

    /**
     * @param offset the offset RSP at which to bang. Note that this offset is relative to RSP after RSP has been
     *            adjusted to allocated the frame for the method. It denotes an offset "down" the stack. For very large
     *            frames, this means that the offset may actually be negative (i.e. denoting a slot "up" the stack above
     *            RSP).
     */
    public void bangStackWithOffset(int offset) {
        mov32BitConstant(scratchRegister, offset);
        sub(64, scratchRegister, Aarch64.sp, scratchRegister, ExtendType.UXTX, 0);
        str(64, Aarch64.r0, Aarch64Address.createBaseRegisterOnlyAddress(scratchRegister));
    }
}

/*
 * Copyright (c) 2018, APT Group, School of Computer Science,
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

    public Aarch64MacroAssembler(CiTarget target, RiRegisterConfig registerConfig) {
        super(target, registerConfig);
    }

    public void iadd(CiRegister dest, CiRegister left, CiRegister right) {
        add(32, dest, left, right);
    }

    public void iadd(CiRegister dest, CiRegister left, CiAddress right) {
        load(dest, right, CiKind.Int);
        add(32, dest, left, r12);
    }

    public void isub(CiRegister dest, CiRegister left, CiAddress right) {
        load(dest, right, CiKind.Int);
        sub(32, dest, left, r12);
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

    public void cas(int size, CiRegister newValue, CiRegister cmpValue, Aarch64Address address) {
        assert Aarch64.r8 != cmpValue;
        assert Aarch64.r8 != newValue;
        assert newValue != cmpValue;
        assert Aarch64.r0 == cmpValue;

        Aarch64Label atomicFail = new Aarch64Label();
        Aarch64Label notEqualTocmpValue = new Aarch64Label();

        bind(atomicFail);
        membar(-1);
        ldxr(size, scratchRegister, address); // scratch has the current Value
        cmp(size, cmpValue, scratchRegister); // compare scratch with cmpValue
        mov64BitConstant(scratchRegister, 1); // store 1 to r0/cmpValue to indicate failure (in case the branch is taken)
        branchConditionally(ConditionFlag.NE, notEqualTocmpValue); // we were not equal to the cmpValue
        stxr(size, scratchRegister, newValue, address); // store newValue to address and result to scratch register

        // If the Condition isa Equal then the strex took place but it MIGHT have failed so we need to test for this.
        cmp(64, scratchRegister, Aarch64.zr);
        // If the scratch register is not 0 then there was an issue with atomicity so do the operation again
        branchConditionally(ConditionFlag.NE, atomicFail);
        mov(64, cmpValue, Aarch64.zr); // store 0 to r0/cmpValue to indicate success
        bind(notEqualTocmpValue);
        dmb(BarrierKind.SY);
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

    private int numInstructions(CiAddress addr) {
        CiRegister index = addr.index();
        int disp = addr.displacement;
        if (disp != 0) {
            if (index.isValid()) {
                return 4;
            } else {
                return 3;
            }
        } else {
            return 2;
        }
    }

    public void setUpScratch(CiAddress addr) {
        CiRegister base = addr.base();
        CiRegister index = addr.index();
        CiAddress.Scale scale = addr.scale;
        int disp = addr.displacement;

        if (addr == CiAddress.Placeholder) {
            nop(numInstructions(addr));
            return;
        }
        assert !(base.isValid() && disp == 0 && base.compareTo(Aarch64.LATCH_REGISTER) == 0);
        assert base.isValid() || base.compareTo(CiRegister.Frame) == 0;

        if (base.isValid() || base.compareTo(CiRegister.Frame) == 0) {
            if (base == CiRegister.Frame) {
                base = frameRegister;
            }
            if (disp != 0) {
                mov64BitConstant(scratchRegister, disp);
                add(64, scratchRegister, scratchRegister, base, ShiftType.LSL, 0);
                if (index.isValid()) {
                    addlsl(scratchRegister, scratchRegister, index, scale.log2);
                }
            } else {
                if (index.isValid()) {
                    addlsl(scratchRegister, base, index, scale.log2);
                } else {
                    mov(64, scratchRegister, base);
                }
            }
        }
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
     * @param size
     * @param reg
     */
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
            movx(dst, Aarch64.zr);
        } else if (Aarch64LogicalImmediateTable.isRepresentable(true, imm) != Aarch64LogicalImmediateTable.Representable.NO) {
            or(64, dst, Aarch64.zr, imm);
        } else if (imm >> 32 == -1L && (int) imm < 0 && Aarch64LogicalImmediateTable.isRepresentable((int) imm) != Aarch64LogicalImmediateTable.Representable.NO) {
            // If the higher 32-bit are 1s and the sign bit of the lower 32-bits is set *and* we can
            // represent the lower 32 bits as a logical immediate we can create the lower 32-bit and then sign extend
            // them. This allows us to cover immediates like ~1L with 2 instructions.
            mov(dst, (int) imm);
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

    /**
     * Applies a delta value to the contents of reg as a 32bit quantity.
     * @param reg
     * @param delta
     */
    public void increment32(CiRegister reg, int delta) {
        assert reg != scratchRegister;
        mov32BitConstant(scratchRegister, delta);
        add(32, reg, reg, scratchRegister, ShiftType.LSL, 0);
    }

    /**
     * Add value to the current value pointed by dst.
     * @param dst
     * @param value
     */
    public void incrementl(Aarch64Address dst, int value) {
        if (value == 0) {
            return;
        }
        ldr(64, scratchRegister, dst);
        mov(Aarch64.r8, value);
        add(64, Aarch64.r8, scratchRegister, Aarch64.r8);
        str(64, Aarch64.r8, dst);
    }

    /**
     * Subtract value from the current value pointed by address dst.
     * @param dst
     * @param value
     */
    public void decrementl(Aarch64Address dst, int value) {
        if (value == 0) {
            return;
        }
        ldr(64, scratchRegister, dst);
        mov(Aarch64.r8, value);
        sub(64, Aarch64.r8, scratchRegister, Aarch64.r8);
        str(64, Aarch64.r8, dst);
    }

    public void decrementq(CiRegister reg, int value) {
        if (value == Integer.MIN_VALUE) {
            subq(reg, value);
            return;
        }
        if (value < 0) {
            incrementq(reg, -value);
            return;
        }
        if (value == 0) {
            return;
        }
        if (value == 1 && AsmOptions.UseIncDec) {
            decq(reg);
        } else {
            subq(reg, value);
        }
    }

    public void incrementq(CiRegister reg, int value) {
        if (value == Integer.MIN_VALUE) {
            addq(reg, value);
            return;
        }
        if (value < 0) {
            decrementq(reg, -value);
            return;
        }
        if (value == 0) {
            return;
        }
        if (value == 1 && AsmOptions.UseIncDec) {
            incq(reg);
        } else {
            addq(reg, value);
        }
    }

    public void xorptr(CiRegister dst, CiRegister src) {
        xorq(dst, src);
    }

    public void xorptr(CiRegister dst, CiAddress src) {
        xorq(dst, src);
    }

    public void restore(CiCalleeSaveLayout csl, int frameToCSA) {
        CiRegisterValue frame = frameRegister.asValue();
        for (CiRegister r : csl.registers) {
            int offset = csl.offsetOf(r);
            int displacement = offset + frameToCSA;
            int add = (displacement < 0) ? 0 : 1;
            if (displacement < 1020 && displacement > -1020) {
                if (r.isCpu()) {
                    ldr(64, r, Aarch64Address.createUnscaledImmediateAddress(frameRegister, displacement));
                } else if (r.isFpu()) {
                    fldr(64, r, Aarch64Address.createUnscaledImmediateAddress(frameRegister, displacement));
                }
            } else {
                setUpScratch(new CiAddress(target.wordKind, frame, frameToCSA + offset));
                if (r.isCpu()) {
                    ldr(64, r, Aarch64Address.createBaseRegisterOnlyAddress(scratchRegister));
                } else if (r.isFpu()) {
                    fldr(64, r, Aarch64Address.createBaseRegisterOnlyAddress(scratchRegister));
                }
            }
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
        super.fdiv(size, dst, n, d);
        super.frintz(size, dst, dst);
        super.fmsub(size, dst, dst, d, n);
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
     * @param y comparison immediate, {@link #isComparisonImmediate(long)} has to be true for it.
     */
    public void cmp(int size, CiRegister x, int y) {
        if (y < 0) {
            super.adds(size, Aarch64.zr, x, -y);
        } else {
            super.subs(size, Aarch64.zr, x, y);
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
     * When patching up Labels we have to know what kind of code to generate.
     */
    public enum PatchLabelKind {
        BRANCH_CONDITIONALLY(0x0), BRANCH_UNCONDITIONALLY(0x1), BRANCH_NONZERO(0x2), BRANCH_ZERO(0x3), JUMP_ADDRESS(0x4);

        /**
         * Offset by which additional information for branch conditionally, branch zero and branch non zero has to be
         * shifted.
         */
        public static final int INFORMATION_OFFSET = 5;

        public final int encoding;

        PatchLabelKind(int encoding) {
            this.encoding = encoding;
        }

        /**
         * @return PatchLabelKind with given encoding.
         */
        private static PatchLabelKind fromEncoding(int encoding) {
            return values()[encoding & NumUtil.getNbitNumberInt(INFORMATION_OFFSET)];
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
            int regEncoding = cmp.getEncoding() << (PatchLabelKind.INFORMATION_OFFSET + 1);
            int sizeEncoding = (size == 64 ? 1 : 0) << PatchLabelKind.INFORMATION_OFFSET;
            // Encode condition flag so that we know how to patch the instruction later
            emitInt(PatchLabelKind.BRANCH_NONZERO.encoding | regEncoding | sizeEncoding);
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
            int regEncoding = cmp.getEncoding() << (PatchLabelKind.INFORMATION_OFFSET + 1);
            int sizeEncoding = (size == 64 ? 1 : 0) << PatchLabelKind.INFORMATION_OFFSET;
            // Encode condition flag so that we know how to patch the instruction later
            emitInt(PatchLabelKind.BRANCH_ZERO.encoding | regEncoding | sizeEncoding);
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
            // Encode condition flag so that we know how to patch the instruction later
            emitInt(PatchLabelKind.BRANCH_CONDITIONALLY.encoding | condition.encoding << PatchLabelKind.INFORMATION_OFFSET);
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
            int offset = csl.offsetOf(r);
//            System.out.println("@save");
//            System.out.println("@save reg: " + r.name + " num: " + r.number);
//            System.out.println("@save frameToCSA: " + frameToCSA);
//            System.out.println("@save offset:     " + offset);
            str(64, r, Aarch64Address.createUnscaledImmediateAddress(frameRegister, frameToCSA + offset));
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

    /**
     * Immediate jump instruction fixed up by HotSpot c++ code.
     */
    public void jmp() {
        // Offset has to be fixed up by c++ code.
        super.b(0);
    }

    /**
     *
     * @return true if immediate offset can be used in a single branch instruction.
     */
    public static boolean isBranchImmediateOffset(long imm) {
        return NumUtil.isSignedNbit(28, imm);
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

    public final void call() {
        nop();
    }

    public final void ret() {
        pop(Aarch64.linkRegister);
        ret(Aarch64.linkRegister);
    }

    public final void ret(int imm16) {
        if (imm16 == 0) {
            ret();
        } else {
            add(64, Aarch64.sp, Aarch64.sp, imm16);
            ret();
        }
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
        push(1 | 2 | 4 | 8 | 128);
        mov32BitConstant(Aarch64.r7, 158); // sched_yield
        emitInt(0xd4000001); // svc 0
        pop(1 | 2 | 4 | 8 | 128);
    }

    // TODO: emit proper opcode
    public void int3() {
//        emitInt(0xFEDEFFE7);
        throw new Error("unimplemented");
    }

    public final void crashme() {
        eor(64, scratchRegister, scratchRegister, scratchRegister);
        insertForeverLoop();
        ldr(64, scratchRegister, Aarch64Address.createBaseRegisterOnlyAddress(scratchRegister));
    }

    public void insertForeverLoop() {
        Aarch64Label forever = new Aarch64Label();
        bind(forever);
        branchConditionally(ConditionFlag.AL, forever);
    }

    public void asr(CiRegister ciRegister, CiRegister dest, int i) {
        // TODO Port from ARMv7
        throw new Error("unimplemented");
    }

    public void xchgptr(CiRegister src1, CiRegister src2) {
        CiRegister tmp = Aarch64.r8;
        assert src1 != tmp && src2 != tmp;
        mov(64, tmp, src1);
        mov(64, src1, src2);
        mov(64, src2, tmp);
    }

    public void movlong(CiRegister dst, long src, CiKind dstKind) {
        if (dstKind.isGeneral()) {
            mov64BitConstant(dst, src);
        } else {
            assert dstKind.isDouble() : "Dst reg must be double";
            saveInFP(9);
            mov64BitConstant(Aarch64.r8, src);
            fmov(64, dst, Aarch64.r8);
            restoreFromFP(9);
        }
    }

    public void store(CiRegister src, CiAddress addr, CiKind kind) {
        CiAddress address = calculateAddress(addr, kind);
        switch (kind) {
            case Char:
            case Short:
                str(16, src, Aarch64Address.createUnscaledImmediateAddress(address.base(), address.displacement));
                break;
            case Object:
            case Long:
                str(64, src, Aarch64Address.createUnscaledImmediateAddress(address.base(), address.displacement));
                break;
            case Double:
                fstr(64, src, Aarch64Address.createUnscaledImmediateAddress(address.base(), address.displacement));
                break;
            case Float:
                fstr(32, src, Aarch64Address.createUnscaledImmediateAddress(address.base(), address.displacement));
                break;
            case Int:
                str(32, src, Aarch64Address.createUnscaledImmediateAddress(address.base(), address.displacement));
                break;
            case Byte:
            case Boolean:
                str(8, src, Aarch64Address.createUnscaledImmediateAddress(address.base(), address.displacement));
                break;
            default:
                assert false : "Unknown kind!";
        }
    }

    public final void ucomisd(int size, CiRegister dst, CiRegister src, CiKind destKind, CiKind srcKind) {
        assert destKind.isFloatOrDouble();
        assert srcKind.isFloatOrDouble();
        fcmp(size, dst, src);
        mrs(Aarch64.r15, SystemRegister.SPSR_EL1);
    }

    public void restoreFromFP(int reg) {
        fmov(64, Aarch64.cpuRegisters[reg], Aarch64.d28);
    }

    public void saveInFP(int reg) {
        fmov(64, Aarch64.d28, Aarch64.cpuRegisters[reg]);
    }

    // TODO check if str and fstr instructions are equivalent to the ARMv7 ones
    public void load(CiRegister dest, CiAddress addr, CiKind kind) {
        CiAddress address = calculateAddress(addr, kind);

        switch (kind) {
            case Short:
//                ldrshw(dest, Aarch64Address.createUnscaledImmediateAddress(address.base(), address.displacement));
                ldr(64, dest, Aarch64Address.createUnscaledImmediateAddress(address.base(), address.displacement));
//                ldrshw(ConditionFlag.Always, 1, 1, 0, dest, address.base(), address.displacement);
                break;
            case Char:
//                ldruhw(ConditionFlag.Always, 1, 1, 0, dest, address.base(), address.displacement);ldr(64, dest, Aarch64Address.createUnscaledImmediateAddress(address.base(), address.displacement()));
                ldr(64, dest, Aarch64Address.createUnscaledImmediateAddress(address.base(), address.displacement));
                break;
            case Boolean:
            case Byte:
//                ldrsb(ConditionFlag.Always, 1, 1, 0, dest, address.base(), address.displacement);
                ldr(64, dest, Aarch64Address.createUnscaledImmediateAddress(address.base(), address.displacement));
                break;
            case Long:
//                ldrd(ConditionFlag.Always, dest, address.base(), address.displacement);
                ldr(64, dest, Aarch64Address.createUnscaledImmediateAddress(address.base(), address.displacement));
                break;
            case Double:
//                vldr(ConditionFlag.Always, dest, address.base(), address.displacement, CiKind.Double, CiKind.Int);
                fldr(64, dest, Aarch64Address.createUnscaledImmediateAddress(address.base(), address.displacement));
                break;
            case Float:
//                vldr(ConditionFlag.Always, dest, address.base(), address.displacement, CiKind.Float, CiKind.Int);
                fldr(64, dest, Aarch64Address.createUnscaledImmediateAddress(address.base(), address.displacement));
                break;
            case Int:
            case Object:
                ldr(64, dest, Aarch64Address.createUnscaledImmediateAddress(address.base(), address.displacement));
                break;
            default:
                assert false : "Unknown kind!";
        }
    }

    private CiAddress calculateAddress(CiAddress addr, CiKind kind) {
        CiRegister base = addr.base();
        CiRegister index = addr.index();
        CiAddress.Scale scale = addr.scale;
        int disp = addr.displacement;

        assert addr != CiAddress.Placeholder;
        assert !(base.isValid() && disp == 0 && base.compareTo(Aarch64.LATCH_REGISTER) == 0);
        assert base.isValid() || base.compareTo(CiRegister.Frame) == 0;

        if (base == CiRegister.Frame) {
            base = frameRegister;
        }

        switch (addr.format()) {
            case BASE:
                break;
            case BASE_DISP:
                if (Aarch64Immediates.isValidDisp(disp, kind)) {
                    break;
                } else if (Aarch64Immediates.isValidImmediate(Math.abs(disp))) {
                    if (true) {
                        throw new Error("unimplemented");
                    }
//                    if (disp >= 0) {
//                        add12BitImmediate(ConditionFlag.Always, false, scratchRegister, base, Aarch64Immediates.calculateShifter(disp));
//                    } else {
//                        sub12BitImmediate(ConditionFlag.Always, false, scratchRegister, base, Aarch64Immediates.calculateShifter(disp));
//                    }
//                    base = scratchRegister;
//                    disp = 0;
                } else {
                    if (true) {
                        throw new Error("unimplemented");
                    }
//                    movImmediate(ConditionFlag.Always, scratchRegister, disp);
//                    addRegisters(ConditionFlag.Always, false, scratchRegister, base, scratchRegister, 0, 0);
//                    base = scratchRegister;
//                    disp = 0;
                }
                break;
            case BASE_INDEX:
                addlsl(scratchRegister, base, index, scale.log2);
                base = scratchRegister;
                disp = 0;
                break;
            case BASE_INDEX_DISP:
                if (Aarch64Immediates.isValidDisp(disp, kind)) {
                    if (true) {
                        throw new Error("unimplemented");
                    }
//                    addlsl(ConditionFlag.Always, false, scratchRegister, base, index, scale.log2);
//                    base = scratchRegister;
                } else if (Aarch64Immediates.isValidImmediate(Math.abs(disp))) {
                    if (true) {
                        throw new Error("unimplemented");
                    }
//                    if (disp > 0) {
//                        add12BitImmediate(ConditionFlag.Always, false, scratchRegister, base, Aarch64Immediates.calculateShifter(disp));
//                    } else {
//                        sub12BitImmediate(ConditionFlag.Always, false, scratchRegister, base, Aarch64Immediates.calculateShifter(disp));
//                    }
//                    addlsl(ConditionFlag.Always, false, scratchRegister, base, index, scale.log2);
//                    base = scratchRegister;
//                    disp = 0;
                } else {
                    if (true) {
                        throw new Error("unimplemented");
                    }
//                    movImmediate(ConditionFlag.Always, scratchRegister, disp);
//                    addRegisters(ConditionFlag.Always, false, scratchRegister, base, scratchRegister, 0, 0);
//                    base = scratchRegister;
//                    disp = 0;
                }
                break;
            default:
                assert false : "Unknown state!";
        }
        return new CiAddress(addr.kind, new CiRegisterValue(CiKind.Int, base), disp);
    }

    // TODO: check if this works on Aarch64 (migrated from ARMV7)
    public void setUpRegister(CiRegister dest, CiAddress addr) {
        CiRegister base = addr.base();
        CiRegister index = addr.index();
        CiAddress.Scale scale = addr.scale;
        int disp = addr.displacement;
        if (addr == CiAddress.Placeholder) {
            nop(numInstructions(addr));
            return;
        }

        assert base.isValid() || base.compareTo(CiRegister.Frame) == 0;

        if (base.isValid() || base.compareTo(CiRegister.Frame) == 0) {
            if (base == CiRegister.Frame) {
                base = frameRegister;
            }
            if (disp != 0) {
                mov64BitConstant(dest, disp);
                add(64, dest, dest, base);
                if (index.isValid()) {
                    addlsl(dest, dest, index, scale.log2);
                }
            } else {
                if (index.isValid()) {
                    addlsl(dest, base, index, scale.log2);
                } else {
                    mov(64, dest, base);
                }
            }
        }
    }

// /**
// * Aligns PC.
// *
// * @param modulus Has to be positive multiple of 4.
// */
// @Override
// public void align(int modulus) {
// assert modulus > 0 && (modulus & 0x3) == 0 : "Modulus has to be a positive multiple of 4.";
// if (codeBuffer.position() % modulus == 0) {
// return;
// }
// int offset = modulus - codeBuffer.position() % modulus;
// for (int i = 0; i < offset; i += 4) {
// nop();
// }
// }

// /**
// * Patches jump targets when label gets bound.
// */
// @Override
// protected void patchJumpTarget(int branch, int jumpTarget) {
// int instruction = codeBuffer.getInt(branch);
// int branchOffset = jumpTarget - branch;
// PatchLabelKind type = PatchLabelKind.fromEncoding(instruction);
// switch (type) {
// case BRANCH_CONDITIONALLY:
// ConditionFlag cf = ConditionFlag.fromEncoding(instruction >>> PatchLabelKind.INFORMATION_OFFSET);
// super.b(cf, branchOffset, /* pos */branch);
// break;
// case BRANCH_UNCONDITIONALLY:
// super.b(branchOffset, /* pos */branch);
// break;
// case JUMP_ADDRESS:
// codeBuffer.emitInt(jumpTarget, /* pos */branch);
// break;
// case BRANCH_NONZERO: {
// int information = instruction >>> PatchLabelKind.INFORMATION_OFFSET;
// int sizeEncoding = information & 1;
// int regEncoding = information >>> 1;
// CiRegister reg = Aarch64.cpuRegisters[regEncoding];
// // 1 => 64; 0 => 32
// int size = sizeEncoding * 32 + 32;
// super.cbnz(size, reg, branchOffset, /* pos */branch);
// }
// break;
// case BRANCH_ZERO: {
// int information = instruction >>> PatchLabelKind.INFORMATION_OFFSET;
// int sizeEncoding = information & 1;
// int regEncoding = information >>> 1;
// CiRegister reg = Aarch64.cpuRegisters[regEncoding];
// // 1 => 64; 0 => 32
// int size = sizeEncoding * 32 + 32;
// super.cbz(size, reg, branchOffset, /* pos */branch);
// }
// break;
// default:
// throw new Error("should not reach here");
// }
// }

// /**
// * Generates an address of the form {@code base + displacement}.
// *
// * Does not change base register to fulfil this requirement. Will fail if displacement cannot be represented
// * directly as address.
// *
// * @param base general purpose register. May not be null or the zero register.
// * @param displacement arbitrary displacement added to base.
// * @return Aarch64Address referencing memory at {@code base + displacement}.
// */
// @Override
// public Aarch64Address makeAddress(CiRegister base, int displacement) {
// return makeAddress(base, displacement, Aarch64.zr, /*signExtend*/false, /*transferSize*/0,
// Aarch64.zr, /*allowOverwrite*/false);
// }

// @Override
// public AbstractAddress getPlaceholder() {
// return Aarch64Address.PLACEHOLDER;
// }
}

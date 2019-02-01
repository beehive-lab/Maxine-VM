/*
 * Copyright (c) 2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
package com.oracle.max.asm.target.riscv64;

import static com.oracle.max.asm.target.riscv64.RISCV64opCodes.*;

import com.oracle.max.asm.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.RiRegisterConfig;

public class RISCV64MacroAssembler extends RISCV64Assembler {
    public static final int PLACEHOLDER_INSTRUCTIONS_FOR_LONG_OFFSETS = 15;
    public static final int INSTRUCTION_SIZE = 4;

    public RISCV64MacroAssembler(CiTarget target) {
        super(target);
    }

    public RISCV64MacroAssembler(CiTarget target, RiRegisterConfig registerConfig) {
        super(target, registerConfig);
    }

    @Override
    protected void patchJumpTarget(int branch, int target) {
        int branchOffset = target - branch;
        PatchLabelKind type = PatchLabelKind.fromEncoding(codeBuffer.getByte(branch));
        switch (type) {
            case BRANCH_CONDITIONALLY: {
                ConditionFlag cf = ConditionFlag.fromEncoding(codeBuffer.getByte(branch + 1));
                CiRegister rs1 = RISCV64.cpuRegisters[codeBuffer.getByte(branch + 2) & 0b11111];
                CiRegister rs2 = RISCV64.cpuRegisters[codeBuffer.getByte(branch + 3) & 0b11111];
                emitConditionalBranch(cf, rs1, rs2, branchOffset, branch);
                break;
            }
            case TABLE_SWITCH:
            case BRANCH_UNCONDITIONALLY: {
                assert codeBuffer.getByte(branch + 1) == 0;
                assert codeBuffer.getShort(branch + 2) == 0;
                jal(RISCV64.zero, branchOffset, branch);
                break;
            }
            case BRANCH_NONZERO:
            case BRANCH_ZERO:
                switch (type) {
                    case BRANCH_NONZERO: {
                        int oldPos = codeBuffer.position();
                        codeBuffer.setPosition(branch - INSTRUCTION_SIZE);
                        mov32BitConstant(scratchRegister1, 0);
                        CiRegister cmp = RISCV64.cpuRegisters[codeBuffer.getByte(branch + 1) & 0b11111];
                        emitConditionalBranch(ConditionFlag.NE, cmp, scratchRegister1, branchOffset);
                        codeBuffer.setPosition(oldPos);
                        break;
                    }
                    case BRANCH_ZERO: {
                        int oldPos = codeBuffer.position();
                        codeBuffer.setPosition(branch - INSTRUCTION_SIZE);
                        mov32BitConstant(scratchRegister1, 0);
                        CiRegister cmp = RISCV64.cpuRegisters[codeBuffer.getByte(branch + 1) & 0b11111];
                        emitConditionalBranch(ConditionFlag.EQ, cmp, scratchRegister1, branchOffset);
                        codeBuffer.setPosition(oldPos);
                        break;
                    }
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    public void align(int modulus) {
        if (codeBuffer.position() % modulus != 0) {
            assert modulus % 4 == 0;
            nop((modulus - (codeBuffer.position() % modulus)) / 4);
        }
    }

    /**
     * When patching up Labels we have to know what kind of code to generate.
     */
    public enum PatchLabelKind {
        BRANCH_CONDITIONALLY(0x0),
        BRANCH_UNCONDITIONALLY(0x1),
        BRANCH_NONZERO(0x2),
        BRANCH_ZERO(0x3),
        TABLE_SWITCH(0x4);

        public final int encoding;

        PatchLabelKind(int encoding) {
            this.encoding = encoding;
        }

        /**
         * @return PatchLabelKind with given encoding.
         */
        private static PatchLabelKind fromEncoding(int encoding) {
            return values()[encoding];
        }
    }


    public final void alignForPatchableDirectCall(int callPos) {
        assert callPos % INSTRUCTION_SIZE == 0 : "Should be 4 bytes aligned";
    }

    public void mov(CiRegister dst, CiRegister src) {
        and(dst, src, src);
    }

    public void mov64BitConstant(CiRegister dst, long imm64) {
        //TODO improve this to get rid of scratchRegister1

        assert dst != scratchRegister1;
        mov32BitConstant(dst, (int) (imm64 >>> 32));
        slli(dst, dst, 32);
        mov32BitConstant(scratchRegister1, (int) imm64);
        slli(scratchRegister1, scratchRegister1, 32);
        srli(scratchRegister1, scratchRegister1, 32);

        add(dst, dst, scratchRegister1);
    }

    public void mov32BitConstant(CiRegister dst, int imm32) {
        //Any change made to this function must also be applied to mov32BitConstantHelper
        if (imm32 == 0) {
            and(dst, RISCV64.x0, RISCV64.x0);
            return;
        }

        if ((imm32 & 0xFFF) >>> 11 == 0b0) {
            lui(dst, imm32);
        } else {
            lui(dst, (imm32 + (0b1 << 12)) & 0xFFFFF000);
        }
        addi(dst, dst, imm32);

        if (imm32 > 0) {
            slli(dst, dst, 32);
            srli(dst, dst, 32);
        }
    }

    public static int[] mov32BitConstantHelper(CiRegister dst, int imm32) {
        int[] instructions = new int[4];

        if (imm32 == 0) {
            // and(dst, RISCV64.x0, RISCV64.x0);
            instructions[0] = AND.getValue() | dst.number << 7 | 7 << 12 |
                    RISCV64.x0.number << 15 | RISCV64.x0.number << 20;
            return instructions;
        }

        if ((imm32 & 0xFFF) >>> 11 == 0b0) {
            // lui(dst, imm32);
            instructions[0] = LUI.getValue() | dst.number << 7 | (imm32 & 0xFFFFF000);
        } else {
            // lui(dst, (imm32 + (0b1 << 12)) & 0xFFFFF000);
            instructions[0] = LUI.getValue() | dst.number << 7 | ((imm32 + (0b1 << 12)) & 0xFFFFF000);
        }
        // addi(dst, dst, imm32);
        instructions[1] = COMP.getValue() | dst.number << 7 | 0 << 12 | dst.number << 15 | imm32 << 20;

        if (imm32 > 0) {
            // slli(dst, dst, 32);
            instructions[2] = COMP.getValue() | dst.number << 7 | 1 << 12 | dst.number << 15 | 32 << 20;
            // srli(dst, dst, 32);
            instructions[3] = COMP.getValue() | dst.number << 7 | 5 << 12 | dst.number << 15 | 32 << 20;
        }

        return instructions;
    }

    public void mov(CiRegister rd, long imm) {
        if (imm <= Integer.MAX_VALUE && imm >= Integer.MIN_VALUE) {
            mov32BitConstant(rd, (int) imm);
        } else {
            mov64BitConstant(rd, imm);
        }
    }

    public void movByte(CiRegister rd, int imm) {
        int val = imm & 0xFF;
        if (val >>> 7 == 1) {
            val = ~0xFF | val;
        }

        mov64BitConstant(rd, imm);
    }

    public void movShort(CiRegister rd, int imm) {
        int val = imm & 0xFFFF;
        if (val >>> 15 == 1) {
            val = ~0xFFFF | val;
        }

        mov64BitConstant(rd, imm);
    }

    public void add(int size, CiRegister rd, CiRegister rs1, CiRegister rs2) {
        if (size == 32) {
            super.addw(rd, rs1, rs2);
        } else {
            super.add(rd, rs1, rs2);
        }
    }

    public void sub(int size, CiRegister rd, CiRegister rs1, CiRegister rs2) {
        if (size == 32) {
            super.subw(rd, rs1, rs2);
        } else {
            super.sub(rd, rs1, rs2);
        }
    }

    public void mul(int size, CiRegister rd, CiRegister rs1, CiRegister rs2) {
        if (size == 32) {
            super.mulw(rd, rs1, rs2);
        } else {
            super.mul(rd, rs1, rs2);
        }
    }

    public void div(int size, CiRegister rd, CiRegister rs1, CiRegister rs2) {
        if (size == 32) {
            super.divw(rd, rs1, rs2);
        } else {
            super.div(rd, rs1, rs2);
        }
    }

    public void divu(int size, CiRegister rd, CiRegister rs1, CiRegister rs2) {
        if (size == 32) {
            super.divuw(rd, rs1, rs2);
        } else {
            super.divu(rd, rs1, rs2);
        }
    }

    public void rem(int size, CiRegister rd, CiRegister rs1, CiRegister rs2) {
        if (size == 32) {
            super.remw(rd, rs1, rs2);
        } else {
            super.rem(rd, rs1, rs2);
        }
    }


    public void fadd(int size, CiRegister rd, CiRegister rs1, CiRegister rs2) {
        if (size == 32) {
            super.fadds(rd, rs1, rs2);
        } else {
            super.faddd(rd, rs1, rs2);
        }
    }

    public void fsub(int size, CiRegister rd, CiRegister rs1, CiRegister rs2) {
        if (size == 32) {
            super.fsubs(rd, rs1, rs2);
        } else {
            super.fsubd(rd, rs1, rs2);
        }
    }

    public void fmul(int size, CiRegister rd, CiRegister rs1, CiRegister rs2) {
        if (size == 32) {
            super.fmuls(rd, rs1, rs2);
        } else {
            super.fmuld(rd, rs1, rs2);
        }
    }

    public void fdiv(int size, CiRegister rd, CiRegister rs1, CiRegister rs2) {
        if (size == 32) {
            super.fdivs(rd, rs1, rs2);
        } else {
            super.fdivd(rd, rs1, rs2);
        }
    }

    public void fdivRTZ(int size, CiRegister rd, CiRegister rs1, CiRegister rs2) {
        if (size == 32) {
            super.fdivsRTZ(rd, rs1, rs2);
        } else {
            super.fdivdRTZ(rd, rs1, rs2);
        }
    }

    public void frem(int size, CiRegister rd, CiRegister rs1, CiRegister rs2) {
        // There is no frem instruction, instead we compute the remainder using the relation:
        // rem = n - Truncating(n / d) * d
        this.fdivRTZ(size, scratchRegister, rs1, rs2);
        this.fmul(size, scratchRegister, scratchRegister, rs2);
        this.fsub(size, rd, rs1, scratchRegister);
    }

    public void fabs(int size, CiRegister rd, CiRegister rs1) {
        if (size == 32) {
            super.fsgnjxs(rd, rs1, rs1);
        } else {
            super.fsgnjxd(rd, rs1, rs1);
        }
    }

    public void fsqrt(int size, CiRegister rd, CiRegister rs) {
        if (size == 32) {
            super.fsqrts(rd, rs);
        } else {
            super.fsqrtd(rd, rs);
        }
    }

    /**
     * dst = -src1.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src general purpose register. May not be null or stackpointer.
     */
    public void neg(int size, CiRegister dst, CiRegister src) {
        sub(size, dst, RISCV64.zr, src);
    }

    /**
     * dst = -src1.
     *
     * @param size register size. Has to be 32 or 64.
     * @param dst general purpose register. May not be null or stackpointer.
     * @param src general purpose register. May not be null or stackpointer.
     */
    public void fneg(int size, CiRegister dst, CiRegister src) {
        if (size == 32) {
            fsgnjns(dst, src, src);
        } else {
            fsgnjnd(dst, src, src);
        }
    }

    public int insertDivByZeroCheck(CiRegister denominator) {
        Label jumpLabel = new Label();
        mov32BitConstant(scratchRegister1, 0);
        branchConditionally(ConditionFlag.NE, denominator, scratchRegister1, jumpLabel);
        int offset = codeBuffer.position();
        ldr(64, RISCV64.zero, RISCV64Address.createBaseRegisterOnlyAddress(scratchRegister1));
        bind(jumpLabel);
        return offset;
    }

    public void nop() {
        addi(RISCV64.x0, RISCV64.x0, 0);
    }

    public void nop(int times) {
        for (int i = 0; i < times; i++) {
            nop();
        }
    }

    public void subi(CiRegister rd, CiRegister rs, int imm32) {
        addi(rd, rs, -imm32);
    }

    public void push(int size, CiRegister reg) {
        assert size == 32 || size == 64 : "Unimplemented push for size: " + size;
        subi(RISCV64.sp, RISCV64.sp, 16);
        if (size == 64) {
            sd(RISCV64.sp, reg, 0);
        } else {
            sw(RISCV64.sp, reg, 0);
        }
    }

    public void fpush(CiRegister... registers) {
        fpush(getRegisterList(registers));
    }

    public void fpush(int registerList) {
        for (int regNumber = 0; regNumber < Integer.SIZE; regNumber++) {
            if (registerList % 2 == 1) {
                fstr(64, RISCV64.fpuRegisters[regNumber], RISCV64Address.createPreIndexedImmediateAddress(RISCV64.sp, -16));
            }

            registerList = registerList >> 1;
        }
    }

    public void fpop(CiRegister... registers) {
        fpop(getRegisterList(registers));
    }

    public void fpop(int registerList) {
        for (int regNumber = Integer.SIZE - 1; regNumber >= 0; regNumber--) {
            if ((registerList >> regNumber) % 2 == 1) {
                fldr(64, RISCV64.fpuRegisters[regNumber], RISCV64Address.createPostIndexedImmediateAddress(RISCV64.sp, 16));
            }
        }
    }

    public void pop(int size, CiRegister reg) {
        assert size == 32 || size == 64 : "Unimplemented pop for size: " + size;
        if (size == 64) {
            ld(reg, RISCV64.sp, 0);
        } else {
            lw(reg, RISCV64.sp, 0);
        }
        addi(RISCV64.sp, RISCV64.sp, 16);
    }

    public void push(int size, CiRegister... registers) {
        for (CiRegister register : registers) {
            push(size, register);
        }
    }

    public void pop(int size, CiRegister... registers) {
        for (CiRegister register : registers) {
            pop(size, register);
        }
    }

    public void push(int size, int registerList) {
        for (int regNumber = 0; regNumber < Integer.SIZE; regNumber++) {
            if (registerList % 2 == 1) {
                push(size, RISCV64.cpuRegisters[regNumber]);
            }

            registerList = registerList >> 1;
        }
    }

    public void pop(int size, int registerList) {
        for (int regNumber = Integer.SIZE - 1; regNumber >= 0; regNumber--) {
            if ((registerList >> regNumber) % 2 == 1) {
                pop(size, RISCV64.cpuRegisters[regNumber]);
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

    public void membar(int barriers) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     * Compare and swap implementation.
     *
     * @param size
     * @param newValue
     * @param cmpValue
     * @param address
     */
    public void cas(int size, CiRegister newValue, CiRegister cmpValue, RISCV64Address address) {
        //TODO At the moment we don't know how to implement memory barriers in RISC-V.
        // This implementation must be changed to be atmoic and use a memory barrier.
        // See Aarch64MacroAssembler.cas() for example of implementation

        assert scratchRegister != cmpValue;
        assert newValue != cmpValue;

        Label notEqualTocmpValue = new Label();

        ldr(size, scratchRegister, address);
        branchConditionally(ConditionFlag.NE, cmpValue, scratchRegister, notEqualTocmpValue);
        str(size, newValue, address);

        bind(notEqualTocmpValue);
    }

    /**
     * Count Leading Zeros implementation.
     * RISC-V lacks this instruction
     *
     * @param size
     * @param dst
     * @param src
     */
    public void clz(int size, CiRegister dst, CiRegister src) {
        Label end = new Label();
        Label continuel = new Label();

        mov32BitConstant(scratchRegister1, 0b1);
        mov32BitConstant(dst, 0);
        if (size == 32) {
            slli(scratchRegister1, scratchRegister1, 31);
        } else {
            slli(scratchRegister1, scratchRegister1, 63);
        }
        this.bind(continuel);
        push(64, scratchRegister1);
        and(scratchRegister1, src, scratchRegister1);
        branchConditionally(ConditionFlag.NE, scratchRegister1, RISCV64.zero, end);
        add(dst, dst, 1);
        pop(64, scratchRegister1);
        srli(scratchRegister1, scratchRegister1, 1);
        b(continuel);

        this.bind(end);
        pop(64, scratchRegister1);
    }

    /**
     * Compare register and branch if zero.
     *
     * @param cmp general purpose register. May not be null, zero-register or stackpointer.
     * @param label Can only handle 32-bit word-aligned offsets for now. May be unbound. Non null.
     */
    public void cbz(CiRegister cmp, Label label) {
        // TODO Handle case where offset is too large for a single jump instruction
        if (label.isBound()) {
            mov32BitConstant(scratchRegister1, 0);
            int offset = label.position() - codeBuffer.position();
            emitConditionalBranch(RISCV64MacroAssembler.ConditionFlag.EQ, cmp, scratchRegister1, offset);
        } else {
            label.addPatchAt(codeBuffer.position());
            int regEncoding = cmp.getEncoding();
            emitInt(0);
            emitByte(PatchLabelKind.BRANCH_ZERO.encoding);
            emitByte(regEncoding);
            emitShort(0);
        }
    }

    /**
     * Checks whether immediate can be encoded as an arithmetic immediate.
     *
     * @param imm Immediate has to be either an unsigned 12bit value or an unsigned 24bit value with
     *            the lower 12 bits 0.
     * @return true if valid arithmetic immediate, false otherwise.
     */
    public static boolean isAimm(int imm) {
        return NumUtil.isUnsignedNbit(12, imm) ||
                NumUtil.isUnsignedNbit(12, imm >>> 12) && (imm & 0xfff) == 0;
    }

    /**
     * @return True if immediate can be used directly for arithmetic instructions (add/sub), false otherwise.
     */
    public static boolean isArithmeticImmediate(long imm) {
        // If we have a negative immediate we just use the opposite operator. I.e.: x - (-5) == x + 5.
        return NumUtil.isInt(Math.abs(imm)) && isAimm((int) Math.abs(imm));
    }

    public void add(CiRegister dest, CiRegister source, long delta) {
        if (delta == 0) {
            mov(dest, source);
        } else if (isArithmeticImmediate(delta)) {
            assert delta == (int) delta;
            super.addi(dest, source, (int) delta);
        } else {
            assert dest != scratchRegister;
            assert source != scratchRegister;
            mov(scratchRegister, delta);
            add(dest, source, scratchRegister);
        }
    }

    public void sub(CiRegister dest, CiRegister source, long delta) {
        add(dest, source, -delta);
    }

    /**
     * Applies a delta value to the contents of reg as a 32bit quantity.
     *
     * @param reg
     * @param delta
     */
    public void increment32(CiRegister reg, int delta) {
        add(reg, reg, delta);
    }

    public void b(int offset) {
//        The  unconditional  jump  instructions  all  use  PC-relative  addressing  to  help  support  position-
//        independent  code.   The  JALR  instruction  was  defined  to  enable  a  two-instruction  sequence  to
//        jump anywhere in a 32-bit absolute address range.  A LUI instruction can first load rs1 with the
//        upper 20 bits of a target address, then JALR can add in the lower bits.  Similarly, AUIPC then
//        JALR can jump anywhere in a 32-bit pc-relative address range.
        auipc(scratchRegister, offset);
        jalr(RISCV64.zero, scratchRegister, offset);
    }

    public void b(int offset, int pos) {
        auipc(scratchRegister, offset, pos);
        jalr(RISCV64.zero, scratchRegister, offset, pos + INSTRUCTION_SIZE);
    }

    /**
     * Branch unconditionally to a label.
     *
     * @param label
     */
    public void b(Label label) {
        // TODO Handle case where offset is too large for a single
        // branch immediate instruction.
        if (label.isBound()) {
            int offset = label.position() - codeBuffer.position();
            b(offset);
        } else {
            throw new UnsupportedOperationException("Unimplemented");
        }
    }

    /**
     * Condition Flags for branches. See 4.3
     */
    public enum ConditionFlag {
        // Integer | Floating-point meanings
        /**
         * Equal | Equal.
         */
        EQ(0x0),
        /**
         * Not Equal | Not equal or unordered.
         */
        NE(0x1),
        /**
         * signed greater than or equal | greater than or equal.
         */
        GE(0x2),
        /**
         * signed less than | less than or unordered.
         */
        LT(0x3),
        /**
         * signed greater than | greater than.
         */
        GT(0x4),
        /**
         * signed less than or equal | less than, equal or unordered.
         */
        LE(0x5),
        /** unsigned greater than or equal.
         */
        GEU(0x6),
        /** unsigned less than.
         */
        LTU(0x7),
        /** unsigned greater than.
         */
        GTU(0x8),
        /** unsigned less than or equal.
         */
        LEU(0x9),
        /**
         * always | always.
         */
        AL(0xA);

        public final int encoding;

        ConditionFlag(int encoding) {
            this.encoding = encoding;
        }

        /**
         * @return ConditionFlag specified by decoding.
         */
        public static ConditionFlag fromEncoding(int encoding) {
            return values()[encoding];
        }

        public ConditionFlag negate() {
            switch (this) {
                case EQ:
                    return NE;
                case NE:
                    return EQ;
                case GE:
                    return LT;
                case LT:
                    return GE;
                case GT:
                    return LE;
                case LE:
                    return GT;
                case GEU:
                    return LTU;
                case LTU:
                    return GEU;
                case GTU:
                    return LEU;
                case LEU:
                    return GTU;
                case AL:
                default:
                    throw new Error("should not reach here");
            }
        }
    }

    public void bgt(CiRegister rs1, CiRegister rs2, int imm32) {
        blt(rs2, rs1, imm32);
    }

    public void bgt(CiRegister rs1, CiRegister rs2, int imm32, int pos) {
        blt(rs2, rs1, imm32, pos);
    }

    public void ble(CiRegister rs1, CiRegister rs2, int imm32) {
        bge(rs2, rs1, imm32);
    }

    public void ble(CiRegister rs1, CiRegister rs2, int imm32, int pos) {
        bge(rs2, rs1, imm32, pos);
    }

    /**
     * Branches to label if condition is true.
     *
     * @param condition any condition value allowed. Non null.
     * @param label     Can only handle 21-bit word-aligned offsets for now. May be unbound. Non null.
     */
    public void branchConditionally(ConditionFlag condition, CiRegister rs1, CiRegister rs2, Label label) {
        // TODO Handle case where offset is too large for a single jump instruction
        if (label.isBound()) {
            int offset = label.position() - codeBuffer.position();
            emitConditionalBranch(condition, rs1, rs2, offset);
        } else {
            label.addPatchAt(codeBuffer.position());
            emitByte(PatchLabelKind.BRANCH_CONDITIONALLY.encoding);
            emitByte(condition.encoding);
            emitByte(rs1.number);
            emitByte(rs2.number);
        }
    }

    private void emitConditionalBranch(ConditionFlag condition, CiRegister rs1, CiRegister rs2, int offset) {
        emitConditionalBranch(condition, rs1, rs2, offset, -1);
    }

    private void emitConditionalBranch(ConditionFlag condition, CiRegister rs1, CiRegister rs2, int offset, int position) {
        switch (condition) {
            case EQ:
                beq(rs1, rs2, offset, position);
                break;
            case NE:
                bne(rs1, rs2, offset, position);
                break;
            case GE:
                bge(rs1, rs2, offset, position);
                break;
            case LT:
                blt(rs1, rs2, offset, position);
                break;
            case GT:
                bgt(rs1, rs2, offset, position);
                break;
            case LE:
                ble(rs1, rs2, offset, position);
                break;
            case GEU:
                bgeu(rs1, rs2, offset, position);
                break;
            case LTU:
                bltu(rs1, rs2, offset, position);
                break;
            case GTU:
                bltu(rs2, rs1, offset, position);
                break;
            case LEU:
                bgeu(rs2, rs1, offset, position);
                break;
            case AL:
                b(offset, position);
                break;
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
        //TODO implement conditional move. RISC-V does not have conditional moves...
        throw new UnsupportedOperationException("Unimplemented");
    }

    public void save(CiCalleeSaveLayout csl, int frameToCSA) {
        for (CiRegister r : csl.registers) {
            int displacement = frameToCSA + csl.offsetOf(r);

            if (r.isCpu()) {
                if (NumUtil.isSignedNbit(9, displacement)) {
                    sd(frameRegister, r, displacement);
                } else {
                    mov(scratchRegister, displacement);
                    add(scratchRegister, frameRegister, scratchRegister);
                    sd(scratchRegister, r, 0);
                }
            } else {
                if (NumUtil.isSignedNbit(9, displacement)) {
                    fsd(frameRegister, r, displacement);
                } else {
                    mov(scratchRegister, displacement);
                    add(scratchRegister, frameRegister, scratchRegister);
                    fsd(scratchRegister, r, 0);
                }
            }
        }
    }

    public final void call() {
        nop(4);
        jal(RISCV64.ra, 0);
    }

    public void restore(CiCalleeSaveLayout csl, int frameToCSA) {
        for (CiRegister r : csl.registers) {
            int displacement = csl.offsetOf(r) + frameToCSA;
            if (r.isCpu()) {
                if (NumUtil.isSignedNbit(9, displacement)) {
                    ld(r, frameRegister, displacement);
                } else {
                    mov(scratchRegister, displacement);
                    add(scratchRegister, frameRegister, scratchRegister);
                    ld(r, scratchRegister, 0);
                }
            } else if (r.isFpu()) {
                if (NumUtil.isSignedNbit(9, displacement)) {
                    fld(r, frameRegister, displacement);
                } else {
                    mov(scratchRegister, displacement);
                    add(scratchRegister, frameRegister, scratchRegister);
                    fld(r, scratchRegister, 0);
                }
            }
        }
    }

    public final void ret() {
        pop(64, RISCV64.ra);
        ret(RISCV64.ra);
    }

    public final void ret(CiRegister r) {
        jalr(RISCV64.x0, r, 0);
    }

    public void leaq(CiRegister dest, CiAddress addr) {
        if (addr == CiAddress.Placeholder) {
            nop(4);
        } else {
            setUpScratch(addr);
            mov(dest, scratchRegister);
        }
    }

    public void pause() {
        //TODO Implement software pause
    }

    public final void crashme() {
        mov(scratchRegister, 0);
        ldr(64, scratchRegister, RISCV64Address.createBaseRegisterOnlyAddress(scratchRegister));
        insertForeverLoop();
    }

    public void insertForeverLoop() {
        b(0);
    }

    public void ldr(int srcSize, CiRegister rd, CiRegister rs, int offset) {
        if (srcSize == 32) {
            lw(rd, rs, offset);
        } else if (srcSize == 64) {
            ld(rd, rs, offset);
        }
    }

    public void fldr(int srcSize, CiRegister rd, CiRegister rs, int offset) {
        if (srcSize == 32) {
            flw(rd, rs, offset);
        } else if (srcSize == 64) {
            fld(rd, rs, offset);
        }
    }

    public void str(int srcSize, CiRegister rd, CiRegister rs, int offset) {
        if (srcSize == 32) {
            sw(rd, rs, offset);
        } else if (srcSize == 64) {
            sd(rd, rs, offset);
        }
    }

    public void fstr(int srcSize, CiRegister rd, CiRegister rs, int offset) {
        if (srcSize == 32) {
            fsw(rd, rs, offset);
        } else if (srcSize == 64) {
            fsd(rd, rs, offset);
        }
    }

    public void ldr(int srcSize, CiRegister rt, RISCV64Address a) {
        switch(a.getAddressingMode()) {
            case BASE_REGISTER_ONLY:
                ldr(srcSize, rt, a.getBase(), 0);
                break;
            case IMMEDIATE_UNSCALED:
                ldr(srcSize, rt, a.getBase(), a.getImmediateRaw());
                break;
            default:
                throw new UnsupportedOperationException("Unimplemented");
        }
    }

    public void fldr(int srcSize, CiRegister rt, RISCV64Address a) {
        switch(a.getAddressingMode()) {
            case BASE_REGISTER_ONLY:
                fldr(srcSize, rt, a.getBase(), 0);
                break;
            case IMMEDIATE_UNSCALED:
                fldr(srcSize, rt, a.getBase(), a.getImmediateRaw());
                break;
            default:
                throw new UnsupportedOperationException("Unimplemented");
        }
    }

    public void str(int srcSize, CiRegister rt, RISCV64Address a) {
        switch(a.getAddressingMode()) {
            case BASE_REGISTER_ONLY:
                str(srcSize, a.getBase(), rt, 0);
                break;
            case IMMEDIATE_UNSCALED:
                str(srcSize, a.getBase(), rt, a.getImmediateRaw());
                break;
            default:
                throw new UnsupportedOperationException("Unimplemented");
        }
    }

    public void fstr(int srcSize, CiRegister rt, RISCV64Address a) {
        switch(a.getAddressingMode()) {
            case BASE_REGISTER_ONLY:
                fstr(srcSize, a.getBase(), rt, 0);
                break;
            case IMMEDIATE_UNSCALED:
                fstr(srcSize, a.getBase(), rt, a.getImmediateRaw());
                break;
            default:
                throw new UnsupportedOperationException("Unimplemented");
        }
    }

    public void load(CiRegister dest, CiAddress addr, CiKind kind) {
        RISCV64Address address = calculateAddress(addr);
        switch (kind) {
            case Object:
            case Long:
                ldr(64, dest, address);
                break;
            case Int:
                ldr(32, dest, address);
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

    public void store(CiRegister src, CiAddress addr, CiKind kind) {
        RISCV64Address address = calculateAddress(addr);
        switch (kind) {
            case Object:
            case Long:
                str(64, src, address);
                break;
            case Int:
                str(32, src, address);
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

    private RISCV64Address calculateAddress(CiAddress addr) {
        if (addr instanceof RISCV64Address) {
            return (RISCV64Address) addr;
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
            throw new UnsupportedOperationException("Unimplemented");
        }

        if (disp != 0) {
            if (NumUtil.isSignedNbit(11, disp)) {
                return RISCV64Address.createUnscaledImmediateAddress(base, disp);
            } else {
                throw new UnsupportedOperationException("Offset is larger than 12 bit signed "
                        + Integer.toBinaryString(disp));
            }
        }

        return RISCV64Address.createBaseRegisterOnlyAddress(base);
    }

    public void setUpScratch(CiAddress addr) {
        setUpRegister(scratchRegister, addr);
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

        assert !(base.isValid() && disp == 0 && base.compareTo(RISCV64.LATCH_REGISTER) == 0);
        assert base.isValid() || base.compareTo(CiRegister.Frame) == 0;

        if (base == CiRegister.Frame) {
            base = frameRegister;
        }

        assert base.isValid();

        if (disp != 0) {
            if (isAimm(Math.abs(disp))) {
                add(dest, base, disp);
            } else {
                mov32BitConstant(dest, disp);
                add(dest, dest, base);
            }
            base = dest;
        } else if (!index.isValid()) {
            mov(dest, base);
        }
        if (index.isValid()) {
            slli(dest, index, scale.log2);
            add(dest, base, dest);
        }
    }

    public void nullCheck(CiRegister r) {
        RISCV64Address address = RISCV64Address.createBaseRegisterOnlyAddress(r);
        ldr(64, RISCV64.zr, address);
    }

    /**
     * @param offset the offset RSP at which to bang. Note that this offset is relative to RSP after RSP has been
     *            adjusted to allocated the frame for the method. It denotes an offset "down" the stack. For very large
     *            frames, this means that the offset may actually be negative (i.e. denoting a slot "up" the stack above
     *            RSP).
     */
    public void bangStackWithOffset(int offset) {
        mov32BitConstant(scratchRegister, offset);
        sub(scratchRegister, RISCV64.sp, scratchRegister);
        str(64, RISCV64.zr, RISCV64Address.createBaseRegisterOnlyAddress(scratchRegister));
    }

    public final void call(CiRegister src) {
        jalr(RISCV64.ra, src, 0);
    }
}

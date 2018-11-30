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
    public static final int PLACEHOLDER_INSTRUCTIONS_FOR_LONG_OFFSETS = 20;
    public static final int INSTRUCTION_SIZE = 4;

    public RISCV64MacroAssembler(CiTarget target) {
        super(target);
    }

    public RISCV64MacroAssembler(CiTarget target, RiRegisterConfig registerConfig) {
        super(target, registerConfig);
    }


    public final void alignForPatchableDirectCall() {
        //TODO: Can we get alignment issues?
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

    public void mov(CiRegister rd, long imm) {
        if (imm <= Integer.MAX_VALUE && imm >= Integer.MIN_VALUE) {
            mov32BitConstant(rd, (int) imm);
        } else {
            mov64BitConstant(rd, imm);
        }
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
        subi(RISCV64.sp, RISCV64.sp, 16);
        switch (size) {
            case 64: sd(RISCV64.sp, reg, 0); break;
            case 32: sw(RISCV64.sp, reg, 0); break;
            default: throw new UnsupportedOperationException("Unimplemented push for size: " + size);
        }
    }

    public void pop(int size, CiRegister reg) {
        switch (size) {
            case 64: ld(reg, RISCV64.sp, 0); break;
            case 32: lw(reg, RISCV64.sp, 0); break;
            default: throw new UnsupportedOperationException("Unimplemented pop for size: " + size);
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
        auipc(RISCV64.x28, offset);
        jalr(RISCV64.zero, RISCV64.x28, offset);
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
        EQ,
        /**
         * Not Equal | Not equal or unordered.
         */
        NE,
        /**
         * signed greater than or equal | greater than or equal.
         */
        GE,
        /**
         * signed less than | less than or unordered.
         */
        LT,
        /**
         * signed greater than | greater than.
         */
        GT,
        /**
         * signed less than or equal | less than, equal or unordered.
         */
        LE,
        /**
         * always | always.
         */
        AL;

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
                case AL:
                default:
                    throw new Error("should not reach here");
            }
        }
    }

    public void bgt(CiRegister rs1, CiRegister rs2, int imm32) {
        blt(rs2, rs1, imm32);
    }

    public void ble(CiRegister rs1, CiRegister rs2, int imm32) {
        bge(rs2, rs1, imm32);
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

            switch (condition) {
                case EQ:
                    beq(rs1, rs2, offset);
                    break;
                case NE:
                    bne(rs1, rs2, offset);
                    break;
                case GE:
                    bne(rs1, rs2, offset);
                    break;
                case LT:
                    blt(rs1, rs2, offset);
                    break;
                case GT:
                    bgt(rs1, rs2, offset);
                    break;
                case LE:
                    ble(rs1, rs2, offset);
                    break;
                case AL:
                    b(offset);
                    break;
            }
        } else {
            throw new UnsupportedOperationException("Unimplemented");
        }
    }

    public void save(CiCalleeSaveLayout csl, int frameToCSA) {
        for (CiRegister r : csl.registers) {
            int displacement = frameToCSA + csl.offsetOf(r);

            if (r.isCpu()) {
                if (NumUtil.isSignedNbit(9, displacement)) {
                    sd(frameRegister, r, displacement);
                } else {
                    mov(scratchRegister, frameRegister);
                    mov(scratchRegister1, displacement);
                    add(scratchRegister, scratchRegister, scratchRegister1);
                    sd(scratchRegister, r, 0);
                }
            } else {
                if (NumUtil.isSignedNbit(9, displacement)) {
                    fsd(frameRegister, r, displacement);
                } else {
                    mov(scratchRegister, frameRegister);
                    mov(scratchRegister1, displacement);
                    add(scratchRegister, scratchRegister, scratchRegister1);
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
                    ld(frameRegister, scratchRegister, 0);
                }
            } else if (r.isFpu()) {
                if (NumUtil.isSignedNbit(9, displacement)) {
                    fld(r, frameRegister, displacement);
                } else {
                    mov(scratchRegister, displacement);
                    fld(frameRegister, scratchRegister, 0);
                }
            }
        }
    }

    public final void ret(int imm16) {
        if (imm16 == 0) {
            jal(RISCV64.ra, 0);
        } else {
            add(RISCV64.sp, RISCV64.sp, imm16);
            jal(RISCV64.ra, 0);
        }
    }

    public final void ret(CiRegister r) {
        jalr(RISCV64.x0, r, 0);
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
        jalr(RISCV64.ra, src,0);
    }
}

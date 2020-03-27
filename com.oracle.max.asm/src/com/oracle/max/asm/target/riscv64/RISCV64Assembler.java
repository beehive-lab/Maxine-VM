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

import static com.oracle.max.asm.NumUtil.*;
import static com.oracle.max.asm.target.riscv64.RISCV64.*;
import static com.oracle.max.asm.target.riscv64.RISCV64opCodes.*;

import com.oracle.max.asm.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.RiRegisterConfig;

public class RISCV64Assembler extends AbstractAssembler {
    public static final int JTYPE_IMM_BITS = 21;
    public CiRegister frameRegister;
    public CiRegister scratchRegister;
    public CiRegister scratchRegister1;

    public RISCV64Assembler(CiTarget target) {
        super(target);
        this.frameRegister = RISCV64.fp;
        this.scratchRegister = RISCV64.x28;
        this.scratchRegister1 = RISCV64.x29;
    }

    public RISCV64Assembler(CiTarget target, RiRegisterConfig registerConfig) {
        super(target);
        this.frameRegister = registerConfig == null ? RISCV64.fp : registerConfig.getFrameRegister();
        this.scratchRegister = registerConfig == null ? RISCV64.x28 : registerConfig.getScratchRegister();
        this.scratchRegister1 = registerConfig == null ? RISCV64.x29 : registerConfig.getScratchRegister1();
    }

    @Override
    protected void patchJumpTarget(int branch, int target) {
        throw new UnsupportedOperationException("This is implemented in the MacroAssembler");
    }

    /**
     * Emits an instruction of type U-type.
     *
     * <pre>
     *     | imm[31:12] | rd | opcode |
     *     |------------|----|--------|
     *     |     20     |  5 |    7   |
     * </pre>
     * @param opcode
     * @param rd
     * @param imm32
     * @param pos
     */
    private void utype(RISCV64opCodes opcode, CiRegister rd, int imm32, int pos) {
        assert opcode.getValue() >> 7 == 0 : opcode.getValue();
        assert rd.getEncoding() >> 5 == 0 : rd.getEncoding();
        int instruction = opcode.getValue();
        instruction |= rd.getEncoding() << 7;
        instruction |= imm32 & 0xFFFFF000;

        if (pos == -1) {
            emitInt(instruction);
        } else {
            emitInt(instruction, pos);
        }
    }

    private void utype(RISCV64opCodes opcode, CiRegister rd, int imm32) {
        utype(opcode, rd, imm32, -1);
    }

    /**
     * Emits an instruction of type R-type.
     *
     * <pre>
     *     | funct7 | rs2 | rs1 | funct3 | rd | opcode |
     *     |--------|-----|-----|--------|----|--------|
     *     |    7   |  5  |  5  |   3    |  5 |    7   |
     * </pre>
     * @param opcode
     * @param rd
     * @param funct3
     * @param rs1
     * @param rs2
     * @param funct7
     */
    private void rtype(RISCV64opCodes opcode, CiRegister rd, int funct3, CiRegister rs1, CiRegister rs2, int funct7, int pos) {
        assert opcode.getValue() >> 7 == 0;
        assert rd.getEncoding() >> 5 == 0;
        assert rs1.getEncoding() >> 5 == 0;
        assert rs2.getEncoding() >> 5 == 0;
        int instruction = opcode.getValue();
        instruction |= rd.getEncoding() << 7;
        instruction |= funct3 << 12;
        instruction |= rs1.getEncoding() << 15;
        instruction |= rs2.getEncoding() << 20;
        instruction |= funct7 << 25;

        if (pos == -1) {
            emitInt(instruction);
        } else {
            emitInt(instruction, pos);
        }
    }

    private void rtype(RISCV64opCodes opcode, CiRegister rd, int funct3, CiRegister rs1, CiRegister rs2, int funct7) {
        rtype(opcode, rd, funct3, rs1, rs2, funct7, -1);
    }


    /**
     * Emits an instruction of type I-type.
     *
     * <pre>
     *     | imm[11:0] | rs1 | funct3 | rd | opcode |
     *     |-----------|-----|--------|----|--------|
     *     |    12     |  5  |    3   |  5 |    7   |
     * </pre>
     * @param opcode
     * @param rd
     * @param funct3
     * @param rs1
     * @param imm32
     */
    private void itype(RISCV64opCodes opcode, CiRegister rd, int funct3, CiRegister rs1, int imm32, int pos) {
        assert opcode.getValue() >> 7 == 0;
        assert rd.getEncoding() >> 5 == 0;
        assert rs1.getEncoding() >> 5 == 0;
        int instruction = opcode.getValue();
        instruction |= rd.getEncoding() << 7;
        instruction |= funct3 << 12;
        instruction |= rs1.getEncoding() << 15;
        instruction |= imm32 << 20;

        if (pos == -1) {
            emitInt(instruction);
        } else {
            emitInt(instruction, pos);
        }
    }

    private void itype(RISCV64opCodes opcode, CiRegister rd, int funct3, CiRegister rs1, int imm32) {
        itype(opcode, rd, funct3, rs1, imm32, -1);
    }

    /**
     * Helping method that emits an instruction of type I-type for the Shift instrictions.
     *
     * <pre>
     *     | ext | shampt | rs1 | funct3 | rd | opcode |
     *     |-----|--------|-----|--------|----|--------|
     *     |  7  |    5   |  5  |    3   |  5 |    7   |
     * </pre>
     * @param opcode
     * @param rd
     * @param funct3
     * @param rs1
     * @param shamt
     * @param ext
     */
    private void shiftHelper(RISCV64opCodes opcode, CiRegister rd, int funct3, CiRegister rs1, int shamt, int ext) {
        itype(opcode, rd, funct3, rs1, ext << 5 | shamt);
    }

    /**
     * Emits an instruction of type S-type.
     *
     * <pre>
     *     | imm[11:5] | rs2 | rs1 | funct3 | imm[4:0] | opcode |
     *     |-----------|-----|-----|--------|----------|--------|
     *     |     7     |  5  |  5  |    3   |    5     |    7   |
     * </pre>
     * @param opcode
     * @param funct3
     * @param rs1
     * @param rs2
     * @param imm32
     */
    private void stype(RISCV64opCodes opcode, int funct3, CiRegister rs1, CiRegister rs2, int imm32) {
        assert opcode.getValue() >> 7 == 0;
        assert rs1.getEncoding() >> 5 == 0;
        assert rs2.getEncoding() >> 5 == 0;
        int instruction = opcode.getValue();
        instruction |= (imm32 & 0x1F) << 7;
        instruction |= funct3 << 12;
        instruction |= rs1.getEncoding() << 15;
        instruction |= rs2.getEncoding() << 20;
        instruction |= ((imm32 >> 5) & 0x7F) << 25;
        emitInt(instruction);
    }

    /**
     * Emits an instruction of type B-type.
     *
     * <pre>
     *     | imm[12|10:5] | rs2 | rs1 | funct3 | imm[4:1|11] | opcode |
     *     |--------------|-----|-----|--------|-------------|--------|
     *     |       7      |  5  |  5  |    3   |      5      |    7   |
     * </pre>
     * @param opcode
     * @param funct3
     * @param rs1
     * @param rs2
     * @param imm32
     */
    private void btype(RISCV64opCodes opcode, int funct3, CiRegister rs1, CiRegister rs2, int imm32, int pos) {
        assert opcode.getValue() >> 7 == 0;
        assert ((byte) funct3) >> 3 == 0;
        assert rs1.getEncoding() >> 5 == 0;
        assert rs2.getEncoding() >> 5 == 0;
        int instruction = opcode.getValue();
        instruction |= ((imm32 >> 11) & 1) << 7;
        instruction |= ((imm32 >> 1) & 0xF) << 8;
        instruction |= funct3 << 12;
        instruction |= rs1.getEncoding() << 15;
        instruction |= rs2.getEncoding() << 20;
        instruction |= ((imm32 >> 5) & 0x3F) << 25;
        instruction |= ((imm32 >> 12) & 1) << 31;

        if (pos == -1) {
            emitInt(instruction);
        } else {
            emitInt(instruction, pos);
        }
    }

    private void btype(RISCV64opCodes opcode, int funct3, CiRegister rs1, CiRegister rs2, int imm32) {
        btype(opcode, funct3, rs1, rs2, imm32, -1);
    }

    /**
     * Emits an instruction of type J-type.
     *
     * <pre>
     *     | imm[20|10:1|11|19:12] | rd | opcode |
     *     |-----------------------|----|--------|
     *     |          20           |  5 |    7   |
     * </pre>
     * @param opcode
     * @param rd
     * @param imm21
     * @param pos
     */
    private void jtype(RISCV64opCodes opcode, CiRegister rd, int imm21, int pos) {
        assert opcode.getValue() >> 7 == 0;
        assert rd.getEncoding() >> 5 == 0;
        assert isSignedNbit(JTYPE_IMM_BITS, imm21);
        int instruction = opcode.getValue();
        instruction |= rd.getEncoding() << 7;
        instruction |= ((imm21 >> 20) & 1) << 31; // This places bit 20 of imm21 in bit 31 of instruction
        instruction |= ((imm21 >> 1) & 0x3FF) << 21; // This places bits 10:1 of imm21 in bits 30:21 of instruction
        instruction |= ((imm21 >> 11) & 1) << 20; // This places bit 11 of imm21 in bit20 of instruction
        instruction |= ((imm21 >> 12) & 0xFF) << 12; // This places bits 19:12 of imm21 in bits 19:12 of instruction

        if (pos == -1) {
            emitInt(instruction);
        } else {
            emitInt(instruction, pos);
        }
    }

    // RV32I Base instruction set /////////////////////////////////////////////

    /**
     *
     * @param rd
     * @param imm32
     */
    public void lui(CiRegister rd, int imm32) {
        utype(LUI, rd, imm32);
    }

    /**
     * AUIPC (add upper immediate to pc) is used to build pc-relative addresses and uses the U-type
     * format. AUIPC forms a 32-bit offset from the 20-bit U-immediate, filling in the lowest 12 bits with
     * zeros, adds this offset to the pc, then places the result in register rd.
     *
     * @param rd the regiester to place the result to
     * @param imm32 the 32-bit offset (with 12LSBs zero)
     */
    public void auipc(CiRegister rd, int imm32) {
        utype(AUIPC, rd, imm32);
    }

    public void auipc(CiRegister rd, int imm32, int pos) {
        utype(AUIPC, rd, imm32, pos);
    }

    public void jal(CiRegister rd, int imm21, int pos) {
        jtype(JAL, rd, imm21, pos);
    }

    public void jal(CiRegister rd, int imm21) {
        jtype(JAL, rd, imm21, -1);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void jalr(CiRegister rd, CiRegister rs, int imm32) {
        itype(JALR, rd, 0, rs, imm32);
    }

    public void jalr(CiRegister rd, CiRegister rs, int imm32, int pos) {
        itype(JALR, rd, 0, rs, imm32, pos);
    }

    /**
     *
     * @param rs1
     * @param rs2
     * @param imm32
     */
    public void beq(CiRegister rs1, CiRegister rs2, int imm32) {
        btype(BRNC, 0, rs1, rs2, imm32);
    }

    public void beq(CiRegister rs1, CiRegister rs2, int imm32, int pos) {
        btype(BRNC, 0, rs1, rs2, imm32, pos);
    }

    /**
     *
     * @param rs1
     * @param rs2
     * @param imm32
     */
    public void bne(CiRegister rs1, CiRegister rs2, int imm32) {
        btype(BRNC, 1, rs1, rs2, imm32);
    }

    public void bne(CiRegister rs1, CiRegister rs2, int imm32, int pos) {
        btype(BRNC, 1, rs1, rs2, imm32, pos);
    }

    /**
     *
     * @param rs1
     * @param rs2
     * @param imm32
     */
    public void blt(CiRegister rs1, CiRegister rs2, int imm32) {
        btype(BRNC, 4, rs1, rs2, imm32);
    }

    public void blt(CiRegister rs1, CiRegister rs2, int imm32, int pos) {
        btype(BRNC, 4, rs1, rs2, imm32, pos);
    }

    /**
     *
     * @param rs1
     * @param rs2
     * @param imm32
     */
    public void bge(CiRegister rs1, CiRegister rs2, int imm32) {
        btype(BRNC, 5, rs1, rs2, imm32);
    }

    public void bge(CiRegister rs1, CiRegister rs2, int imm32, int pos) {
        btype(BRNC, 5, rs1, rs2, imm32, pos);
    }

    /**
     *
     * @param rs1
     * @param rs2
     * @param imm32
     */
    public void bltu(CiRegister rs1, CiRegister rs2, int imm32) {
        btype(BRNC, 6, rs1, rs2, imm32);
    }

    public void bltu(CiRegister rs1, CiRegister rs2, int imm32, int pos) {
        btype(BRNC, 6, rs1, rs2, imm32, pos);
    }

    /**
     *
     * @param rs1
     * @param rs2
     * @param imm32
     */
    public void bgeu(CiRegister rs1, CiRegister rs2, int imm32) {
        btype(BRNC, 7, rs1, rs2, imm32);
    }

    public void bgeu(CiRegister rs1, CiRegister rs2, int imm32, int pos) {
        btype(BRNC, 7, rs1, rs2, imm32, pos);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void lb(CiRegister rd, CiRegister rs, int imm32) {
        itype(LOAD, rd, 0, rs, imm32);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void lh(CiRegister rd, CiRegister rs, int imm32) {
        itype(LOAD, rd, 1, rs, imm32);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void lw(CiRegister rd, CiRegister rs, int imm32) {
        itype(LOAD, rd, 2, rs, imm32);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void lbu(CiRegister rd, CiRegister rs, int imm32) {
        itype(LOAD, rd, 4, rs, imm32);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void lhu(CiRegister rd, CiRegister rs, int imm32) {
        itype(LOAD, rd, 5, rs, imm32);
    }

    /**
     *
     * @param rs1
     * @param rs2
     * @param imm32
     */
    public void sb(CiRegister rs1, CiRegister rs2, int imm32) {
        stype(STORE, 0, rs1, rs2, imm32);
    }

    /**
     *
     * @param rs1
     * @param rs2
     * @param imm32
     */
    public void sh(CiRegister rs1, CiRegister rs2, int imm32) {
        stype(STORE, 1, rs1, rs2, imm32);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void sw(CiRegister rd, CiRegister rs, int imm32) {
        stype(STORE, 2, rd, rs, imm32);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void addi(CiRegister rd, CiRegister rs, int imm32) {
        itype(COMP, rd, 0, rs, imm32);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void slti(CiRegister rd, CiRegister rs, int imm32) {
        itype(COMP, rd, 3, rs, imm32);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void sltiu(CiRegister rd, CiRegister rs, int imm32) {
        itype(COMP, rd, 3, rs, imm32);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void xori(CiRegister rd, CiRegister rs, int imm32) {
        itype(COMP, rd, 4, rs, imm32);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void ori(CiRegister rd, CiRegister rs, int imm32) {
        itype(COMP, rd, 6, rs, imm32);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void andi(CiRegister rd, CiRegister rs, int imm32) {
        itype(COMP, rd, 7, rs, imm32);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void slli(CiRegister rd, CiRegister rs, int imm32) {
        shiftHelper(COMP, rd, 1, rs, imm32, 0);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void srli(CiRegister rd, CiRegister rs, int imm32) {
        shiftHelper(COMP, rd, 5, rs, imm32, 0);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void srai(CiRegister rd, CiRegister rs, int imm32) {
        shiftHelper(COMP, rd, 5, rs, imm32, 32);
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void add(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(ADD, rd, 0, rs1, rs2, 0);
    }

    public void add(CiRegister rd, CiRegister rs1, CiRegister rs2, int pos) {
        rtype(ADD, rd, 0, rs1, rs2, 0, pos);
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void sub(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(SUB, rd, 0, rs1, rs2, 32);
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void sll(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(SRLL, rd, 1, rs1, rs2, 0);
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void slt(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(SLT, rd, 2, rs1, rs2, 0);
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void sltu(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(SLTU, rd, 3, rs1, rs2, 0);
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void xor(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(XOR, rd, 4, rs1, rs2, 0);
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void srl(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(SRLL, rd, 5, rs1, rs2, 0);
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void sra(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(SLT, rd, 5, rs1, rs2, 32);
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void or(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(OR, rd, 6, rs1, rs2, 0);
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void and(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(AND, rd, 7, rs1, rs2, 0);
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void mul(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(MUL, rd, 0, rs1, rs2, 1);
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void mulw(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(MULW, rd, 0, rs1, rs2, 1);
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void div(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(MUL, rd, 4, rs1, rs2, 1);
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void divw(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(MULW, rd, 4, rs1, rs2, 1);
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void divu(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(MUL, rd, 5, rs1, rs2, 1);
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void divuw(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(MULW, rd, 5, rs1, rs2, 1);
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void rem(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(MUL, rd, 6, rs1, rs2, 1);
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void remw(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(RV32M, rd, 6, rs1, rs2, 1);
    }

    /**
     *
     * @param predecessorMask
     * @param successorMask
     */
    public void fence(int predecessorMask, int successorMask) {
        itype(FENCE, x0, 0, x0, predecessorMask << 4 | successorMask);
    }

    /**
     *
     */
    public void fencei() {
        itype(FENCE, x0, 0, x0, 0);
    }

    /**
     *
     */
    public void ecall() {
        itype(SYS, x0, 0, x0, 0);
    }

    /**
     *
     */
    public void ebreak() {
        itype(SYS, x0, 0, x0, 1);
    }

    /**
     *
     * @param rd
     * @param csr
     * @param rs
     */
    public void csrrw(CiRegister rd, int csr, CiRegister rs) {
        itype(SYS, rd, 1, rs, csr);
    }

    /**
     *
     * @param rd
     * @param csr
     * @param rs
     */
    public void csrrs(CiRegister rd, int csr, CiRegister rs) {
        itype(SYS, rd, 2, rs, csr);
    }

    /**
     *
     * @param rd
     * @param csr
     * @param rs
     */
    public void csrrc(CiRegister rd, int csr, CiRegister rs) {
        itype(SYS, rd, 3, rs, csr);
    }

    /**
     *
     * @param rd
     * @param csr
     * @param imm32
     */
    public void csrrwi(CiRegister rd, int csr, int imm32) {
        csrImmediate(SYS, rd, 5, csr, imm32);
    }

    /**
     *
     * @param rd
     * @param csr
     * @param imm32
     */
    public void csrrsi(CiRegister rd, int csr, int imm32) {
        csrImmediate(SYS, rd, 6, csr, imm32);
    }

    /**
     *
     * @param rd
     * @param csr
     * @param imm32
     */
    public void csrrci(CiRegister rd, int csr, int imm32) {
        csrImmediate(SYS, rd, 7, csr, imm32);
    }

    private void csrImmediate(RISCV64opCodes opcode, CiRegister rd, int funct3, int csr, int imm32) {
        assert opcode.getValue() >> 7 == 0;
        assert rd.getEncoding() >> 5 == 0;
        assert funct3 >> 3 == 0;
        assert imm32 >> 5 == 0;
        assert csr >>> 12 == 0;
        int instruction = opcode.getValue();
        instruction |= rd.getEncoding() << 7;
        instruction |= funct3 << 12;
        instruction |= imm32 << 15;
        instruction |= csr << 20;

        emitInt(instruction);
    }

    // RV64I Base instruction set /////////////////////////////////////////////

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void lwu(CiRegister rd, CiRegister rs, int imm32) {
        itype(LD, rd, 6, rs, imm32);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void ld(CiRegister rd, CiRegister rs, int imm32) {
        itype(LD, rd, 3, rs, imm32);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void sd(CiRegister rd, CiRegister rs, int imm32) {
        stype(SD, 3, rd, rs, imm32);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void addiw(CiRegister rd, CiRegister rs, int imm32) {
        itype(COMP64, rd, 0, rs, imm32);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void slliw(CiRegister rd, CiRegister rs, int imm32) {
        itype(COMP64, rd, 1, rs, imm32);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void srliw(CiRegister rd, CiRegister rs, int imm32) {
        itype(COMP64, rd, 5, rs, imm32);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void sraiw(CiRegister rd, CiRegister rs, int imm32) {
        shiftHelper(COMP64, rd, 5, rs, imm32, 32);
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void addw(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(ADDW, rd, 0, rs1, rs2, 0);
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void subw(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(SUBW, rd, 0, rs1, rs2, 32);
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void sllw(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(SLLW, rd, 1, rs1, rs2, 0);
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void srlw(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(SRLW, rd, 5, rs1, rs2, 0);
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void sraw(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(SRAW, rd, 5, rs1, rs2, 32);
    }

    // Floating point instructions double precision
    public void faddd(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(RV32D, rd, 0, rs1, rs2, 0b0000001);
    }

    public void fsubd(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(RV32D, rd, 0, rs1, rs2, 0b0000101);
    }

    public void fmuld(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(RV32D, rd, 0, rs1, rs2, 0b0001001);
    }

    public void fmuldRTZ(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(RV32D, rd, 1, rs1, rs2, 0b0001001);
    }

    public void fdivd(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(RV32D, rd, 0, rs1, rs2, 0b0001101);
    }

    public void fdivdRTZ(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(RV32D, rd, 1, rs1, rs2, 0b0001101);
    }

    public void fmvxd(CiRegister rd, CiRegister rs) {
        assert rd.isGeneral() || rs.isFpu();
        itype(RV32D, rd, 0, rs, 0b111000100000);
    }

    public void fmvdx(CiRegister rd, CiRegister rs) {
        assert rd.isFpu() || rs.isGeneral();
        itype(RV32D, rd, 0, rs, 0b111100100000);
    }

    public void fld(CiRegister dst, CiRegister base, int offset) {
        itype(LOAD_FP, dst, 3, base, offset);
    }

    public void fsd(CiRegister rd, CiRegister rs, int offset) {
        stype(STORE_FP, 3, rd, rs, offset);
    }

    public void fcvtsd(CiRegister rd, CiRegister rs) {
        itype(RV32D, rd, 0, rs, 0b010000000001);
    }

    public void fcvtds(CiRegister rd, CiRegister rs) {
        itype(RV32D, rd, 0, rs, 0b010000100000);
    }

    public void fcvtdw(CiRegister rd, CiRegister rs) {
        itype(RV32D, rd, 0, rs, 0b110100100000);
    }

    public void fcvtwd(CiRegister rd, CiRegister rs) {
        itype(RV32D, rd, 0, rs, 0b110000100000);
    }

    public void fcvtwdRTZ(CiRegister rd, CiRegister rs) {
        itype(RV32D, rd, 0b001, rs, 0b110000100000);
    }

    public void fcvtdl(CiRegister rd, CiRegister rs) {
        itype(RV32D, rd, 0, rs, 0b110100100010);
    }

    public void fcvtld(CiRegister rd, CiRegister rs) {
        itype(RV32D, rd, 0, rs, 0b110000100010);
    }

    public void fcvtldRTZ(CiRegister rd, CiRegister rs) {
        itype(RV32D, rd, 0b001, rs, 0b110000100010);
    }

    public void fsgnjd(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(RV32D, rd, 0, rs1, rs2, 0b0010001);
    }

    public void fsgnjxd(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(RV32D, rd, 2, rs1, rs2, 0b0010001);
    }

    public void fsgnjnd(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(RV32D, rd, 1, rs1, rs2, 0b0010001);
    }

    public void fsqrtd(CiRegister rd, CiRegister rs) {
        itype(RV32D, rd, 0, rs, 0b010110100000);
    }

    public void fltd(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(RV32D, rd, 1, rs1, rs2, 0b1010001);
    }

    public void fled(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(RV32D, rd, 0, rs1, rs2, 0b1010001);
    }

    public void feqd(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(RV32D, rd, 0b010, rs1, rs2, 0b1010001);
    }

    // Floating point instructions single precision
    public void fadds(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(RV32F, rd, 0, rs1, rs2, 0b0000000);
    }

    public void fsubs(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(RV32F, rd, 0, rs1, rs2, 0b0000100);
    }

    public void fmuls(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(RV32F, rd, 0, rs1, rs2, 0b0001000);
    }

    public void fmulsRTZ(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(RV32F, rd, 1, rs1, rs2, 0b0001000);
    }

    public void fdivs(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(RV32F, rd, 0, rs1, rs2, 0b0001100);
    }

    public void fdivsRTZ(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(RV32F, rd, 1, rs1, rs2, 0b0001100);
    }

    public void flw(CiRegister dst, CiRegister base, int offset) {
        itype(LOAD_FP, dst, 2, base, offset);
    }

    public void fsw(CiRegister dst, CiRegister base, int offset) {
        stype(STORE_FP, 2, dst, base, offset);
    }

    public void fcvtls(CiRegister rd, CiRegister rs) {
        itype(RV32F, rd, 0, rs, 0b110000000010);
    }

    public void fcvtlsRTZ(CiRegister rd, CiRegister rs) {
        itype(RV32F, rd, 0b001, rs, 0b110000000010);
    }

    public void fcvtws(CiRegister rd, CiRegister rs) {
        itype(RV32F, rd, 0, rs, 0b110000000000);
    }

    public void fcvtwsRTZ(CiRegister rd, CiRegister rs) {
        itype(RV32F, rd, 0b001, rs, 0b110000000000);
    }

    public void fcvtwus(CiRegister rd, CiRegister rs) {
        itype(RV32F, rd, 0, rs, 0b110000000001);
    }

    public void fcvtwusRTZ(CiRegister rd, CiRegister rs) {
        itype(RV32F, rd, 0b001, rs, 0b110000000001);
    }

    public void fcvtsw(CiRegister rd, CiRegister rs) {
        itype(RV32F, rd, 0, rs, 0b110100000000);
    }

    public void fcvtswu(CiRegister rd, CiRegister rs) {
        itype(RV32F, rd, 0, rs, 0b110100000001);
    }

    public void fmvxw(CiRegister rd, CiRegister rs) {
        itype(RV32F, rd, 0, rs, 0b111000000000);
    }

    public void fmvwx(CiRegister rd, CiRegister rs) {
        itype(RV32F, rd, 0, rs, 0b111100000000);
    }

    public void fcvtsl(CiRegister rd, CiRegister rs) {
        itype(RV32F, rd, 0, rs, 0b110100000010);
    }

    public void fsgnjs(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(RV32F, rd, 0, rs1, rs2, 0b0010000);
    }

    public void fsgnjxs(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(RV32F, rd, 2, rs1, rs2, 0b0010000);
    }

    public void fsgnjns(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(RV32F, rd, 1, rs1, rs2, 0b0010000);
    }

    public void fsqrts(CiRegister rd, CiRegister rs) {
        itype(RV32F, rd, 0, rs, 0b010110000000);
    }

    public void flts(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(RV32F, rd, 1, rs1, rs2, 0b1010000);
    }

    public void fles(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(RV32F, rd, 0, rs1, rs2, 0b1010000);
    }

    public void feqs(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        rtype(RV32F, rd, 0b010, rs1, rs2, 0b1010000);
    }

    public enum ExtendType {
        UXTB(0), UXTH(1), UXTW(2), UXTX(3), SXTB(4), SXTH(5), SXTW(6), SXTX(7);

        public final int encoding;

        ExtendType(int encoding) {
            this.encoding = encoding;
        }
    }

    // Atomic instructions
    public void lrw(CiRegister dest, CiRegister addr, int aq, int rl) {
        int imm32 = 0b00000 + (rl << 5) + (aq << 6) + (0b00010 << 7);
        itype(LRSC, dest, 0b010, addr, imm32);
    }

    public void lrd(CiRegister dest, CiRegister addr, int aq, int rl) {
        int imm32 = 0b00000 + (rl << 5) + (aq << 6) + (0b00010 << 7);
        itype(LRSC, dest, 0b011, addr, imm32);
    }

    public void scw(CiRegister dest, CiRegister addr, CiRegister src, int aq, int rl) {
        int imm32 = rl + (aq << 1) + (0b00011 << 2);
        rtype(LRSC, dest, 0b010, addr, src, imm32);
    }

    public void scd(CiRegister dest, CiRegister addr, CiRegister src, int aq, int rl) {
        int imm32 = rl + (aq << 1) + (0b00011 << 2);
        rtype(LRSC, dest, 0b011, addr, src, imm32);
    }

}

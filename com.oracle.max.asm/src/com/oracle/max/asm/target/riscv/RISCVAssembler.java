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
package com.oracle.max.asm.target.riscv;

import static com.oracle.max.asm.target.riscv.RISCVopCodes.*;

import com.oracle.max.asm.*;
import com.sun.cri.ci.*;
//import com.oracle.max.asm.target.riscv.*; //kostas
import static com.oracle.max.asm.target.riscv.RISCV64.*; //kostas

public class RISCVAssembler extends AbstractAssembler {
    public RISCVAssembler(CiTarget target) {
        super(target);
    }

    @Override
    protected void patchJumpTarget(int branch, int target) {
        throw new UnsupportedOperationException("Unimplemented");
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
     */
    private void utype(RISCVopCodes opcode, CiRegister rd, int imm32) {
        assert opcode.getValue() >> 7 == 0 : opcode.getValue();
        assert rd.number >> 5 == 0 : rd.number;
        int instruction = opcode.getValue();
        instruction |= rd.number << 7;
        instruction |= imm32 & 0xFFFFF000;
        emitInt(instruction);
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
    private void rtype(RISCVopCodes opcode, CiRegister rd, int funct3, CiRegister rs1, CiRegister rs2, int funct7) {
        assert opcode.getValue() >> 7 == 0;
        assert rd.number >> 5 == 0;
        assert rs1.number >> 5 == 0;
        assert rs2.number >> 5 == 0;
        int instruction = opcode.getValue();
        instruction |= rd.number << 7;
        instruction |= funct3 << 12;
        instruction |= rs1.number << 15;
        instruction |= rs2.number << 20;
        instruction |= funct7 << 25;
        emitInt(instruction);
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
    private void itype(RISCVopCodes opcode, CiRegister rd, int funct3, CiRegister rs1, int imm32, int ext, int shift) {
        assert opcode.getValue() >> 7 == 0;
        assert rd.number >> 5 == 0;
        assert rs1.number >> 5 == 0;
        int instruction = opcode.getValue();
        instruction |= rd.number << 7;
        instruction |= funct3 << 12;
        instruction |= rs1.number << 15;
        instruction |= imm32 << 20;
        if (shift == 1) {
            instruction |= ext << 25;
        }
        emitInt(instruction);
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
     * @param rd
     * @param funct3
     * @param rs1
     * @param rs2
     * @param imm32
     */
    private void stype(RISCVopCodes opcode, int funct3, CiRegister rs1, CiRegister rs2, int imm32) {
        assert opcode.getValue() >> 7 == 0;
        assert rs1.number >> 5 == 0;
        assert rs2.number >> 5 == 0;
        int instruction = opcode.getValue();
        instruction |= (imm32 & 0x1F) << 7;
        instruction |= funct3 << 12;
        instruction |= rs1.number << 15;
        instruction |= rs2.number << 20;
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
     * @param rd
     * @param funct3
     * @param rs1
     * @param rs2
     * @param imm32
     */
    private void btype(RISCVopCodes opcode, int funct3, CiRegister rs1, CiRegister rs2, int imm32) {
        assert opcode.getValue() >> 7 == 0;
        assert ((byte) funct3) >> 3 == 0;
        assert rs1.number >> 5 == 0;
        assert rs2.number >> 5 == 0;
        int instruction = opcode.getValue();
        instruction |= ((imm32 >> 11) & 1) << 7;
        instruction |= ((imm32 >> 1) & 0xF) << 8;
        instruction |= funct3 << 12;
        instruction |= rs1.number << 15;
        instruction |= rs2.number << 20;
        instruction |= ((imm32 >> 5) & 0x3F) << 25;
        instruction |= ((imm32 >> 12) & 1) << 31;
        emitInt(instruction);
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
     * @param imm32
     */
    private void jtype(RISCVopCodes opcode, CiRegister rd, int imm32) {
        assert opcode.getValue() >> 7 == 0;
        assert rd.number >> 5 == 0;
        int instruction = opcode.getValue();
        instruction |= rd.number << 7;
        instruction |= ((imm32 >> 20) & 1) << 31; // This places bit 20 of imm32 in bit 31 of instruction
        instruction |= ((imm32 >> 1) & 0x3FF) << 21; // This places bits 10:1 of imm32 in bits 30:21 of instruction
        instruction |= ((imm32 >> 11) & 1) << 20; // This places bit 11 of imm32 in bit20 of instruction
        instruction |= ((imm32 >> 12) & 0xFF) << 12; // This places bits 19:12 of imm32 in bits 19:12 of instruction
        emitInt(instruction);
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

    /**
     *
     * @param rd
     * @param imm32
     */
    public void jal(CiRegister rd, int imm32) {
        jtype(JAL, rd, imm32);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void jalr(CiRegister rd, CiRegister rs, int imm32) {
        itype(JALR, rd, 0, rs, imm32, 0, 0);
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

    /**
     *
     * @param rs1
     * @param rs2
     * @param imm32
     */
    public void bne(CiRegister rs1, CiRegister rs2, int imm32) {
        btype(BRNC, 1, rs1, rs2, imm32);
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

    /**
     *
     * @param rs1
     * @param rs2
     * @param imm32
     */
    public void bge(CiRegister rs1, CiRegister rs2, int imm32) {
        btype(BRNC, 5, rs1, rs2, imm32);
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

    /**
     *
     * @param rs1
     * @param rs2
     * @param imm32
     */
    public void bgeu(CiRegister rs1, CiRegister rs2, int imm32) {
        btype(BRNC, 7, rs1, rs2, imm32);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void lb(CiRegister rd, CiRegister rs, int imm32) {
        itype(LOAD, rd, 0, rs, imm32, 0, 0);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void lh(CiRegister rd, CiRegister rs, int imm32) {
        itype(LOAD, rd, 1, rs, imm32, 0, 0);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void lw(CiRegister rd, CiRegister rs, int imm32) {
        itype(LOAD, rd, 2, rs, imm32, 0, 0);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void lbu(CiRegister rd, CiRegister rs, int imm32) {
        itype(LOAD, rd, 4, rs, imm32, 0, 0);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void lhu(CiRegister rd, CiRegister rs, int imm32) {
        itype(LOAD, rd, 5, rs, imm32, 0, 0);
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
     * @param rs1
     * @param rs2
     * @param imm32
     */
    public void sw(CiRegister rs1, CiRegister rs2, int imm32) {
        stype(STORE, 2, rs1, rs2, imm32);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void addi(CiRegister rd, CiRegister rs, int imm32) {
        itype(COMP, rd, 0, rs, imm32, 0, 0);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void slti(CiRegister rd, CiRegister rs, int imm32) {
        itype(COMP, rd, 3, rs, imm32, 0, 0);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void sltiu(CiRegister rd, CiRegister rs, int imm32) {
        itype(COMP, rd, 3, rs, imm32, 0, 0);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void xori(CiRegister rd, CiRegister rs, int imm32) {
        itype(COMP, rd, 4, rs, imm32, 0, 0);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void ori(CiRegister rd, CiRegister rs, int imm32) {
        itype(COMP, rd, 6, rs, imm32, 0, 0);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void andi(CiRegister rd, CiRegister rs, int imm32) {
        itype(COMP, rd, 7, rs, imm32, 0, 0);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void slli(CiRegister rd, CiRegister rs, int imm32) {
        itype(COMP, rd, 1, rs, imm32, 0, 1);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void srli(CiRegister rd, CiRegister rs, int imm32) {
        itype(COMP, rd, 5, rs, imm32, 0, 1);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void srai(CiRegister rd, CiRegister rs, int imm32) {
        itype(COMP, rd, 5, rs, imm32, 32, 1);
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
        rtype(COMP, rd, 1, rs1, rs2, 0);
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
        rtype(COMP, rd, 5, rs1, rs2, 0);
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
     * @param predecessorMask
     * @param successorMask
     */
    public void fence(int predecessorMask, int successorMask) {
        itype(FENCE, x0, 0, x0, predecessorMask + successorMask, 0, 0);
    }

    /**
     *
     */
    public void fencei() {
        itype(FENCE, x0, 0, x0, 0, 0, 0);
    }

    /**
     *
     */
    public void ecall() {
        itype(SYS, x0, 0, x0, 0, 0, 0);
    }

    /**
     *
     */
    public void ebreak() {
        itype(SYS, x0, 0, x0, 1, 0, 0);
    }

    /**
     *
     * @param rd
     * @param csr
     * @param rs
     */
    public void csrrw(CiRegister rd, CiRegister csr, CiRegister rs) {
        itype(SYS, rd, 1, rs, csr.number, 0, 0);
    }

    /**
     *
     * @param rd
     * @param csr
     * @param rs
     */
    public void csrrs(CiRegister rd, CiRegister csr, CiRegister rs) {
        itype(SYS, rd, 2, rs, csr.number, 0, 0);
    }

    /**
     *
     * @param rd
     * @param csr
     * @param rs
     */
    public void csrrc(CiRegister rd, CiRegister csr, CiRegister rs) {
        itype(SYS, rd, 3, rs, csr.number, 0, 0);
    }

    /**
     *
     * @param rd
     * @param csr
     * @param imm32
     */
    public void csrrwi(CiRegister rd, CiRegister csr, int imm32) {
        itype(SYS, rd, 5, x0, csr.number, 0, 0);
    }

    /**
     *
     * @param rd
     * @param csr
     * @param imm32
     */
    public void csrrsi(CiRegister rd, CiRegister csr, int imm32) {
        itype(SYS, rd, 6, x0, csr.number, 0, 0);
    }

    /**
     *
     * @param rd
     * @param csr
     * @param imm32
     */
    public void csrrci(CiRegister rd, CiRegister csr, int imm32) {
        itype(SYS, rd, 7, x0, csr.number, 0, 0);
    }

    // RV64I Base instruction set /////////////////////////////////////////////

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void lwu(CiRegister rd, CiRegister rs, int imm32) {
        itype(LD, rd, 6, rs, imm32, 0, 0);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void ld(CiRegister rd, CiRegister rs, int imm32) {
        itype(LD, rd, 3, rs, imm32, 0, 0);
    }

    /**
     *
     * @param rs1
     * @param rs2
     * @param imm32
     */
    public void sd(CiRegister rs1, CiRegister rs2, int imm32) {
        stype(SD, 3, rs1, rs2, imm32);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void addiw(CiRegister rd, CiRegister rs, int imm32) {
        itype(COMP64, rd, 0, rs, imm32, 0, 0);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void slliw(CiRegister rd, CiRegister rs, int imm32) {
        itype(COMP64, rd, 1, rs, imm32, 0, 0);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void srliw(CiRegister rd, CiRegister rs, int imm32) {
        itype(COMP64, rd, 5, rs, imm32, 0, 0);
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void sraiw(CiRegister rd, CiRegister rs, int imm32) {
        itype(COMP64, rd, 5, rs, imm32, 32, 1);
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

}

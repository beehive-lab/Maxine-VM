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

import com.oracle.max.asm.*;
import com.sun.cri.ci.*;

public class RISCVAssembler extends AbstractAssembler {
    public RISCVAssembler(CiTarget target) {
        super(target);
    }

    @Override
    protected void patchJumpTarget(int branch, int target) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    // RV32I Base instruction set /////////////////////////////////////////////

    /**
     *
     * @param rd
     * @param imm32
     */
    public void lui(CiRegister rd, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param imm32
     */
    public void auipc(CiRegister rd, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param imm32
     */
    public void jal(CiRegister rd, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void jalr(CiRegister rd, CiRegister rs, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rs1
     * @param rs2
     * @param imm32
     */
    public void beq(CiRegister rs1, CiRegister rs2, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rs1
     * @param rs2
     * @param imm32
     */
    public void bne(CiRegister rs1, CiRegister rs2, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rs1
     * @param rs2
     * @param imm32
     */
    public void blt(CiRegister rs1, CiRegister rs2, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rs1
     * @param rs2
     * @param imm32
     */
    public void bge(CiRegister rs1, CiRegister rs2, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rs1
     * @param rs2
     * @param imm32
     */
    public void bltu(CiRegister rs1, CiRegister rs2, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rs1
     * @param rs2
     * @param imm32
     */
    public void bgeu(CiRegister rs1, CiRegister rs2, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void lb(CiRegister rd, CiRegister rs, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void lh(CiRegister rd, CiRegister rs, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void lw(CiRegister rd, CiRegister rs, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void lbu(CiRegister rd, CiRegister rs, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void lhu(CiRegister rd, CiRegister rs, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rs1
     * @param rs2
     * @param imm32
     */
    public void sb(CiRegister rs1, CiRegister rs2, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rs1
     * @param rs2
     * @param imm32
     */
    public void sh(CiRegister rs1, CiRegister rs2, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rs1
     * @param rs2
     * @param imm32
     */
    public void sw(CiRegister rs1, CiRegister rs2, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void addi(CiRegister rd, CiRegister rs, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void slti(CiRegister rd, CiRegister rs, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void sltiu(CiRegister rd, CiRegister rs, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void xori(CiRegister rd, CiRegister rs, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void ori(CiRegister rd, CiRegister rs, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void andi(CiRegister rd, CiRegister rs, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void slli(CiRegister rd, CiRegister rs, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void srli(CiRegister rd, CiRegister rs, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param rs
     * @param imm32
     */
    public void srai(CiRegister rd, CiRegister rs, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void add(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void sub(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void sll(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void slt(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void sltu(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void xor(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void srl(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void sra(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void or(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param rs1
     * @param rs2
     */
    public void and(CiRegister rd, CiRegister rs1, CiRegister rs2) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param predecessorMask
     * @param successorMask
     */
    public void fence(int predecessorMask, int successorMask) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     */
    public void fencei() {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     */
    public void ecall() {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     */
    public void ebreak() {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param csr
     * @param rs
     */
    public void csrrw(CiRegister rd, CiRegister csr, CiRegister rs) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param csr
     * @param rs
     */
    public void csrrs(CiRegister rd, CiRegister csr, CiRegister rs) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param csr
     * @param rs
     */
    public void csrrc(CiRegister rd, CiRegister csr, CiRegister rs) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param csr
     * @param imm32
     */
    public void csrrwi(CiRegister rd, CiRegister csr, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param csr
     * @param imm32
     */
    public void csrrsi(CiRegister rd, CiRegister csr, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    /**
     *
     * @param rd
     * @param csr
     * @param imm32
     */
    public void csrrci(CiRegister rd, CiRegister csr, int imm32) {
        throw new UnsupportedOperationException("Unimplemented");
    }

}

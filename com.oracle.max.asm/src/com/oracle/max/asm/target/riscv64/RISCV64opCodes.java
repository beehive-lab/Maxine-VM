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

public enum RISCV64opCodes {

    // RV32I Base instruction set /////////////////////////////////////////////

    LUI((byte) 0b0110111),
    ADD((byte) 0b0110011),
    SUB((byte) 0b0110011),
    SLT((byte) 0b0110011),
    SLTU((byte) 0b0110011),
    COMP((byte) 0b0010011), // Computational instructions
    SRLL((byte) 0b0110011),
    XOR((byte) 0b0110011),
    OR((byte) 0b0110011),
    AND((byte) 0b0110011),
    LOAD((byte) 0b0000011),
    STORE((byte) 0b0100011),
    AUIPC((byte) 0b0010111),
    BRNC((byte) 0b1100011),
    JAL((byte) 0b1101111),
    JALR((byte) 0b1100111),
    FENCE((byte) 0b0001111),
    SYS((byte) 0b1110011),
    // RV64I Base instruction set /////////////////////////////////////////////

    LWU((byte) 0b0000011),
    COMP64((byte) 0b0011011),
    SLLW((byte) 0b0111011),
    SRLW((byte) 0b0111011),
    ADDW((byte) 0b0111011),
    SUBW((byte) 0b0111011),
    SRAW((byte) 0b0111011),
    LD((byte) 0b0000011),
    SD((byte) 0b0100011),

    // Floating-point
    LOAD_FP((byte) 0b0000111),
    STORE_FP((byte) 0b0100111),
    FCVT((byte) 0b1010011),
    RV32F((byte) 0b1010011),
    RV32D((byte) 0b1010011),

    MUL((byte) 0b0110011),
    MULW((byte) 0b0111011),
    RV32M((byte) 0b0111011),

    // Atomic instructions
    LRSC((byte) 0b0101111);

    public byte getValue() {
        return value;
    }

    private byte value;

    RISCV64opCodes(byte opcode) {
        value = opcode;
    }
}

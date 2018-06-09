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

public enum RISCVopCodes {

    // RV32I Base instruction set /////////////////////////////////////////////

    LUI((byte) 0b0110111),
    ADD((byte) 0b0110011),
    SUB((byte) 0b0110011),
    SLT((byte) 0b0110011),
    SLTU((byte) 0b0110011),
    COMP((byte) 0b0010011), // Computational instructions
    XOR((byte) 0b0110011),
    OR((byte) 0b0110011),
    AND((byte) 0b0110011),
    LOAD((byte) 0b0000011),
    STORE((byte) 0b0100011),
    AUIPC((byte) 0b0010111),
    BRNC((byte) 0b1100011),
    // TODO: fill the rest

    // RV64I Base instruction set /////////////////////////////////////////////

    LWU((byte) 0b0000011);
    // TODO: fill the rest

    public byte getValue() {
        return value;
    }

    private byte value;

    RISCVopCodes(byte opcode) {
        value = opcode;
    }
}

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

import static com.oracle.max.asm.target.riscv64.RISCV64.*;

import com.oracle.max.asm.*;
import com.sun.cri.ci.*;

/**
 * Represents an address in target machine memory, specified using one of the different addressing modes of the
 * RISCV64 ISA.
 * - Base register only
 * - Base register + immediate or register with shifted offset
 * - Pre-indexed: base + immediate offset are written back to base register, value used in instruction is base + offset
 * - Post-indexed: base + offset (immediate or register) are written back to base register,
 * value used in instruction is base only
 * <p>
 * Not all addressing modes are supported for all instructions.
 */
public final class RISCV64Address extends CiAddress {
    // Placeholder for addresses that get patched later.
    public static final RISCV64Address Placeholder = new RISCV64Address(CiKind.Illegal, RISCV64.x31.asValue(), 0, AddressingMode.BASE_REGISTER_ONLY);
    private static final long serialVersionUID = 2306820231108443722L;

    public enum AddressingMode {
        /**
         * base + imm12.
         */
        IMMEDIATE,
        /**
         * base.
         */
        BASE_REGISTER_ONLY,
        /**
         * address = base.
         * base is updated to base + imm12
         */
        IMMEDIATE_POST_INDEXED,
        /**
         * address = base + imm9.
         * base is updated to base + imm12
         */
        IMMEDIATE_PRE_INDEXED,
    }

    private final CiRegister base;
    private final int immediate;
    private final AddressingMode addressingMode;

    /**
     * General address generation mechanism. Accepted values for all parameters depend on the addressingMode.
     * Null is never accepted for a register, if an addressMode doesn't use a register the register has to be the zero-register.
     */
    public static RISCV64Address createAddress(CiKind kind, AddressingMode addressingMode, CiRegister base,
                                               int immediate) {
        return new RISCV64Address(kind, base.asValue(), immediate, addressingMode);
    }

    /**
     * @param base may not be null or the zero-register.
     * @param imm9 Signed 12 bit immediate value.
     * @return RISCV64Address specifying a post-indexed immediate address pointing to base.
     *         After ldr/str instruction, base is updated to point to base + imm12
     */
    public static RISCV64Address createPostIndexedImmediateAddress(CiRegister base, int imm9) {
        return new RISCV64Address(CiKind.Int, base.asValue(), imm9, AddressingMode.IMMEDIATE_POST_INDEXED);
    }

    /**
     * @param base may not be null or the zero-register.
     * @param imm12 Signed 12 bit immediate value.
     * @return RISCV64Address specifying a pre-indexed immediate address pointing to base + imm12.
     *         After ldr/str instruction, base is updated to point to base + imm12
     */
    public static RISCV64Address createPreIndexedImmediateAddress(CiRegister base, int imm12) {
        return new RISCV64Address(CiKind.Int, base.asValue(), imm12, AddressingMode.IMMEDIATE_PRE_INDEXED);
    }

    /**
     * @param base may not be null or the zero-register.
     * @param imm12 Signed 12 bit immediate value.
     * @return RISCV64Address specifying an unscaled immediate address of the form base + imm12
     */
    public static RISCV64Address createImmediateAddress(CiRegister base, int imm12) {
        return new RISCV64Address(CiKind.Int, base.asValue(), imm12, AddressingMode.IMMEDIATE);
    }

    /**
     * @param base May not be null or the zero register.
     * @return RISCV64Address specifying the address pointed to by base.
     */
    public static RISCV64Address createBaseRegisterOnlyAddress(CiRegister base) {
        return new RISCV64Address(CiKind.Int, base.asValue(), 0, AddressingMode.BASE_REGISTER_ONLY);
    }

    private RISCV64Address(CiKind kind, CiValue base, int immediate, AddressingMode addressingMode) {
        super(kind, base, immediate);
        this.base = base.asRegister();
        this.addressingMode = addressingMode;
        this.immediate = immediate;
        verify();
    }

    private void verify() {
        assert addressingMode != null;
        assert RISCV64.isIntReg(base);
        switch (addressingMode) {
            case BASE_REGISTER_ONLY:
                assert !base.equals(zr) : base.number;
                assert immediate == 0 : immediate;
                break;
            case IMMEDIATE:
            case IMMEDIATE_POST_INDEXED:
            case IMMEDIATE_PRE_INDEXED:
                assert !base.equals(zr);
                assert NumUtil.isSignedNbit(12, immediate);
                break;
            default:
                throw new Error("should not reach here");
        }
    }

    public CiRegister getBase() {
        return base;
    }

    /**
     * @return immediate in correct representation for the given addressing mode.
     */
    public int getImmediate() {
        switch (addressingMode) {
            case IMMEDIATE:
            case IMMEDIATE_POST_INDEXED:
            case IMMEDIATE_PRE_INDEXED:
                return RISCV64MacroAssembler.isAimm(immediate) ?
                        immediate & NumUtil.getNbitNumberInt(12) :
                        immediate & NumUtil.getNbitNumberInt(11);
            default:
                throw new Error("should not reach here!! Should only be called for addressing modes that use immediate values.");

        }
    }

    public AddressingMode getAddressingMode() {
        return addressingMode;
    }

    public String toString(int log2TransferSize) {
        switch (addressingMode) {
            case IMMEDIATE:
                return String.format("[X%d, %d]", base.getEncoding(), immediate);
            case BASE_REGISTER_ONLY:
                return String.format("[X%d]", base.getEncoding());
            case IMMEDIATE_POST_INDEXED:
                return String.format("[X%d],%d", base.getEncoding(), immediate);
            case IMMEDIATE_PRE_INDEXED:
                return String.format("[X%d,%d]!", base.getEncoding(), immediate);
            default:
                throw new Error("should not reach here");
        }
    }

}

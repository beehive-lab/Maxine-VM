/*
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
//package com.oracle.graal.asm.armv8;
package com.oracle.max.asm.target.aarch64;

import com.oracle.max.asm.AbstractAddress;
import com.oracle.max.asm.target.aarch64.Aarch64;
import com.oracle.max.asm.NumUtil;

import com.oracle.max.asm.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

import static com.oracle.max.asm.target.aarch64.Aarch64.zr;

/**
 * Represents an address in target machine memory, specified using one of the different addressing modes of the
 * Aarch64 ISA.
 * - Base register only
 * - Base register + immediate or register with shifted offset
 * - Pre-indexed: base + immediate offset are written back to base register, value used in instruction is base + offset
 * - Post-indexed: base + offset (immediate or register) are written back to base register,
 * value used in instruction is base only
 * - Literal: PC + 19-bit signed word aligned offset
 * <p>
 * Not all addressing modes are supported for all instructions.
 */
public final class Aarch64Address extends AbstractAddress {
    // Placeholder for addresses that get patched later.
    public static final Aarch64Address PLACEHOLDER = createPcLiteralAddress(0);

    public static enum AddressingMode {
        /**
         * base + uimm12 << log2(memory_transfer_size).
         */
        IMMEDIATE_SCALED,
        /**
         * base + imm9.
         */
        IMMEDIATE_UNSCALED,
        /**
         * base.
         */
        BASE_REGISTER_ONLY,
        /**
         * base + offset [<< log2(memory_transfer_size)].
         */
        REGISTER_OFFSET,
        /**
         * base + extend(offset) [<< log2(memory_transfer_size)].
         */
        EXTENDED_REGISTER_OFFSET,
        /**
         * PC + imm21 (word aligned).
         */
        PC_LITERAL,
        /**
         * address = base.
         * base is updated to base + imm9
         */
        IMMEDIATE_POST_INDEXED,
        /**
         * address = base + imm9.
         * base is updated to base + imm9
         */
        IMMEDIATE_PRE_INDEXED,
    }

    private final CiRegister base;
    private final CiRegister offset;
    private final int immediate;
    /**
     * Should register offset be scaled or not.
     */
    private final boolean scaled;
    private final Aarch64Assembler.ExtendType extendType;
    private final AddressingMode addressingMode;

    /**
     * General address generation mechanism. Accepted values for all parameters depend on the addressingMode.
     * Null is never accepted for a register, if an addressMode doesn't use a register the register has to be the zero-register.
     * extendType has to be null for every addressingMode except EXTENDED_REGISTER_OFFSET.
     */
    public static Aarch64Address createAddress(AddressingMode addressingMode, CiRegister base, CiRegister offset,
                                             int immediate, boolean isScaled, Aarch64Assembler.ExtendType extendType) {
        return new Aarch64Address(base, offset, immediate, isScaled, extendType, addressingMode);
    }

    /**
     * @param base may not be null or the zero-register.
     * @param imm9 Signed 9 bit immediate value.
     * @return Aarch64Address specifying a post-indexed immediate address pointing to base.
     *         After ldr/str instruction, base is updated to point to base + imm9
     */
    public static Aarch64Address createPostIndexedImmediateAddress(CiRegister base, int imm9) {
        return new Aarch64Address(base, zr, imm9, false, null, AddressingMode.IMMEDIATE_POST_INDEXED);
    }

    /**
     * @param base may not be null or the zero-register.
     * @param imm9 Signed 9 bit immediate value.
     * @return Aarch64Address specifying a pre-indexed immediate address pointing to base + imm9.
     *         After ldr/str instruction, base is updated to point to base + imm9
     */
    public static Aarch64Address createPreIndexedImmediateAddress(CiRegister base, int imm9) {
        return new Aarch64Address(base, zr, imm9, false, null, AddressingMode.IMMEDIATE_PRE_INDEXED);
    }

    /**
     * @param base  may not be null or the zero-register.
     * @param imm12 Unsigned 12 bit immediate value. This is scaled by the word access size. This means if this
     *              address is used to load/store a word, the immediate is shifted by 2 (log2Ceil(4)).
     * @return Aarch64Address specifying a signed address of the form base + imm12 << log2(memory_transfer_size).
     */
    public static Aarch64Address createScaledImmediateAddress(CiRegister base, int imm12) {
        return new Aarch64Address(base, zr, imm12, true, null, AddressingMode.IMMEDIATE_SCALED);
    }

    /**
     * @param base may not be null or the zero-register.
     * @param imm9 Signed 9 bit immediate value.
     * @return Aarch64Address specifying an unscaled immediate address of the form base + imm9
     */
    public static Aarch64Address createUnscaledImmediateAddress(CiRegister base, int imm9) {
        return new Aarch64Address(base, zr, imm9, false, null, AddressingMode.IMMEDIATE_UNSCALED);
    }

    /**
     * @param base May not be null or the zero register.
     * @return Aarch64Address specifying the address pointed to by base.
     */
    public static Aarch64Address createBaseRegisterOnlyAddress(CiRegister base) {
        return createRegisterOffsetAddress(base, zr, false);
    }

    /**
     * @param base   may not be null or the zero-register.
     * @param offset Register specifying some offset, optionally scaled by the memory_transfer_size.
     *               May not be null or the stackpointer.
     * @param scaled Specifies whether offset should be scaled by memory_transfer_size or not.
     * @return Aarch64Address specifying a register offset address of the form base + offset [<< log2
     *         (memory_transfer_size)]
     */
    public static Aarch64Address createRegisterOffsetAddress(CiRegister base, CiRegister offset, boolean scaled) {
        return new Aarch64Address(base, offset, 0, scaled, null, AddressingMode.REGISTER_OFFSET);
    }

    /**
     * @param base       may not be null or the zero-register.
     * @param offset     Word register specifying some offset, optionally scaled by the memory_transfer_size.
     *                   May not be null or the stackpointer.
     * @param scaled     Specifies whether offset should be scaled by memory_transfer_size or not.
     * @param extendType Describes whether register is zero- or sign-extended. May not be null.
     * @return Aarch64Address specifying an extended register offset of the form base + extendType(offset)
     *         [<< log2(memory_transfer_size)]
     */
    public static Aarch64Address createExtendedRegisterOffsetAddress(CiRegister base, CiRegister offset, boolean scaled,
                                                                   Aarch64Assembler.ExtendType extendType) {
        return new Aarch64Address(base, offset, 0, scaled, extendType, AddressingMode.EXTENDED_REGISTER_OFFSET);
    }

    /**
     * @param imm21 Signed 21-bit offset, word aligned.
     * @return Aarch64Address specifying a PC-literal address of the form PC + offset
     */
    public static Aarch64Address createPcLiteralAddress(int imm21) {
        return new Aarch64Address(zr, zr, imm21, false, null, AddressingMode.PC_LITERAL);
    }

    private Aarch64Address(CiRegister base, CiRegister offset, int immediate, boolean scaled,
                         Aarch64Assembler.ExtendType extendType, AddressingMode addressingMode) {
        this.base = base;
        this.offset = offset;
        if ((addressingMode == AddressingMode.REGISTER_OFFSET || addressingMode == AddressingMode.EXTENDED_REGISTER_OFFSET)
                && offset.equals(zr)) {
            this.addressingMode = AddressingMode.BASE_REGISTER_ONLY;
        } else {
            this.addressingMode = addressingMode;
        }
        this.immediate = immediate;
        this.scaled = scaled;
        this.extendType = extendType;
        assert verify();
    }

    private boolean verify() {
        assert addressingMode != null;
        assert Aarch64.isIntReg(base) && Aarch64.isIntReg(offset);
        switch (addressingMode) {
        case IMMEDIATE_SCALED:
//            System.out.println("@IMMEDIATE_SCALED");
            return !base.equals(zr) && offset.equals(zr) && extendType == null && NumUtil.isUnsignedNbit(12, immediate);
        case IMMEDIATE_UNSCALED:
//            System.out.println("@IMMEDIATE_UNSCALED");
//            System.out.println("!base.equals(zr): "                             + (!base.equals(zr)));
//            System.out.println("offset.equals(zr): "                            + (offset.equals(zr)));
//            System.out.println("extendType == null: "                           + (extendType == null));
//            System.out.println("NumUtil.isSignedNbit(9, "+ immediate +"): "     + (NumUtil.isSignedNbit(9, immediate)));
            return !base.equals(zr) &&  offset.equals(zr) && extendType == null && NumUtil.isSignedNbit(9, immediate);
        case BASE_REGISTER_ONLY:
//            System.out.println("@BASE_REGISTER_ONLY");
            return !base.equals(zr) && offset.equals(zr) && extendType == null && immediate == 0;
        case REGISTER_OFFSET:
//            System.out.println("@REGISTER_OFFSET");
            return !base.equals(zr) && Aarch64.isGeneralPurposeReg(offset) && extendType == null && immediate == 0;
        case EXTENDED_REGISTER_OFFSET:
//            System.out.println("@EXTENDED_REGISTER_OFFSET");
            return !base.equals(zr) && Aarch64.isGeneralPurposeReg(offset) &&
                    (extendType == Aarch64Assembler.ExtendType.SXTW || extendType == Aarch64Assembler.ExtendType.UXTW) &&
                    immediate == 0;
        case PC_LITERAL:
//            System.out.println("@PC_LITERAL");
//            System.out.println("base.equals(zr):"                              + base.equals(zr));
//            System.out.println("offset.equals(zr): "                            + offset.equals(zr));
//            System.out.println("extendType == null: "                           + (extendType == null));
//            System.out.println("NumUtil.isSignedNbit(21, immediate): "          + NumUtil.isSignedNbit(21, immediate));
//            System.out.println("(immediate & 0x3) == 0: "                       + ((immediate & 0x3) == 0));
            return base.equals(zr) && offset.equals(zr) && extendType == null &&
                    NumUtil.isSignedNbit(21, immediate) && ((immediate & 0x3) == 0);
        case IMMEDIATE_POST_INDEXED:
        case IMMEDIATE_PRE_INDEXED:
//            System.out.println("@IMMEDIATE_POST_INDEXED|IMMEDIATE_PRE_INDEXED");
            return !base.equals(zr) && offset.equals(zr) && extendType == null && NumUtil.isSignedNbit(9, immediate);
        default:
            throw new Error("should not reach here");
        }
    }

    public CiRegister getBase() {
        return base;
    }

    public CiRegister getOffset() {
        return offset;
    }

    /**
     * @return immediate in correct representation for the given addressing mode. For example in case
     *         of <code>addressingMode ==IMMEDIATE_UNSCALED </code> the value will be returned as the 9bit signed
     *         representation.
     */
    public int getImmediate() {
        switch (addressingMode) {
        case IMMEDIATE_UNSCALED:
        case IMMEDIATE_POST_INDEXED:
        case IMMEDIATE_PRE_INDEXED:
            // 9-bit signed value
            return immediate & NumUtil.getNbitNumberInt(9);
        case IMMEDIATE_SCALED:
            // Unsigned value can be returned as-is.
            return immediate;
        case PC_LITERAL:
            // 21-bit signed value, but lower 2 bits are always 0 and are shifted out.
            return (immediate >> 2) & NumUtil.getNbitNumberInt(19);
        default:
            throw new Error("should not reach here!! Should only be called for addressing modes that use immediate values.");

        }
    }

    /**
     * @return Raw immediate as a 32-bit signed value.
     */
    public int getImmediateRaw() {
        switch (addressingMode) {
        case IMMEDIATE_UNSCALED:
        case IMMEDIATE_SCALED:
        case IMMEDIATE_POST_INDEXED:
        case IMMEDIATE_PRE_INDEXED:
        case PC_LITERAL:
            return immediate;
        default:
            throw new Error("should not reach here!! Should only be called for addressing modes that use immediate values.");
        }
    }

    public boolean isScaled() {
        return scaled;
    }

    public Aarch64Assembler.ExtendType getExtendType() {
        return extendType;
    }

    public AddressingMode getAddressingMode() {
        return addressingMode;
    }

    public String toString(int log2TransferSize) {
        int shiftVal = scaled ? log2TransferSize : 0;
        switch (addressingMode) {
        case IMMEDIATE_SCALED:
            return String.format("[X%d, %d]", base.getEncoding(), immediate << log2TransferSize);
        case IMMEDIATE_UNSCALED:
            return String.format("[X%d, %d]", base.getEncoding(), immediate);
        case BASE_REGISTER_ONLY:
            return String.format("[X%d]", base.getEncoding());
        case EXTENDED_REGISTER_OFFSET:
            if (shiftVal != 0) {
                return String.format("[X%d, W%d, %s %d]", base.getEncoding(), offset.getEncoding(), extendType.name(), shiftVal);
            } else {
                return String.format("[X%d, W%d, %s]", base.getEncoding(), offset.getEncoding(), extendType.name());
            }
        case REGISTER_OFFSET:
            if (shiftVal != 0) {
                return String.format("[X%d, X%d, LSL %d]", base.getEncoding(), offset.getEncoding(), shiftVal);
            } else {
                // LSL 0 may be optional, but still encoded differently so we always leave it off
                return String.format("[X%d, X%d]", base.getEncoding(), offset.getEncoding());
            }
        case PC_LITERAL:
            return String.format(".%s%d", immediate >= 0 ? "+" : "", immediate);
        case IMMEDIATE_POST_INDEXED:
            return String.format("[X%d],%d", base.getEncoding(), immediate);
        case IMMEDIATE_PRE_INDEXED:
            return String.format("[X%d,%d]!", base.getEncoding(), immediate);
        default:
            throw new Error("should not reach here");
        }
    }

}

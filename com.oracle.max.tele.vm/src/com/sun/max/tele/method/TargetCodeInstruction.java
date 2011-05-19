/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.method;

import com.sun.max.asm.dis.*;
import com.sun.max.unsafe.*;

/**
 * A single, disassembled native code instruction.
 *
 * @author Michael Van De Vanter
  */
public final class TargetCodeInstruction {

    /**
     * disassembled mnemonic for this instruction.
     */
    public final String mnemonic;

    /**
     * position in bytes in the sequence of instructions for this method.
     */
    public final int position;

    /**
     * address of the first byte of the instruction.
     */
    public final Address address;

    /**
     * The label (if any) at this instruction's address.
     */
    public final String label;

    public final byte[] bytes;

    public final String operands;

    /**
     * The target address of this instruction if it is a direct call or jump instruction otherwise null.
     */
    public final Address targetAddress;

    /**
     * The address of a literal value loaded by a load instruction. How the literal is loaded depends on the instruction set
     * (e.g., load relative to current pc, or relative of a base register, etc...)
     */
    public final Address literalSourceAddress;

    TargetCodeInstruction(String mnemonic, Address address, int position, String label, byte[] bytes, String operands, Address targetAddress, Address literalSourceAddress) {
        this.mnemonic = mnemonic;
        this.address = address;
        this.position = position;
        this.label = label;
        this.bytes = bytes;
        this.operands = operands;
        this.targetAddress = targetAddress;
        this.literalSourceAddress = literalSourceAddress;
    }

    /**
     * @return The target address of this instruction if it is a direct call or jump instruction, else 0.
     */
    public long getTargetAddressAsLong() {
        if (targetAddress == null) {
            return 0;
        }
        return targetAddress.toLong();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(address.toHexString()).
            append("[+").
            append(position).
            append("] ");
        if (label != null) {
            sb.append(label).
                append(": ");
        }
        sb.append(mnemonic).
            append(' ').
            append(operands).
            append("    ").
            append(DisassembledInstruction.toHexString(bytes));
        return sb.toString();
    }
}

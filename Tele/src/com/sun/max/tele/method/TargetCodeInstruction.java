/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
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

    private final String mnemonic;

    /**
     * @return disassembled mnemonic for this instruction.
     */
    public String mnemonic() {
        return mnemonic;
    }

    private final int position;

    /**
     * @return position in bytes in the sequence of instructions for this method
     */
    public int position() {
        return position;
    }

    private final Address address;

    /**
     * @return address of the first byte of the instruction
     */
    public Address address() {
        return address;
    }

    private final String label;

    /**
     * The label (if any) at this instruction's address.
     */
    public String label() {
        return label;
    }

    private final byte[] bytes;

    public byte[] bytes() {
        return bytes;
    }

    private final String operands;

    public String operands() {
        return operands;
    }

    /**
     * The target address of this instruction if it is a direct call or jump instruction otherwise null.
     */
    public final Address targetAddress;

    /**
     * The address of a literal value loaded by a load instruction. How the literal is loaded depends on the instruction set
     * (e.g., load relative to current pc, or relative of a base register, etc...)
     */
    public final Address literalSourceAddress;

    public String getMnemonic() {
        return mnemonic;
    }

    public int getPosition() {
        return position;
    }

    public long getAddress() {
        return address.toLong();
    }

    public String getLabel() {
        return label;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getOperands() {
        return operands;
    }

    public long getTargetAddress() {
        if (targetAddress == null) {
            return 0;
        }
        return targetAddress.toLong();
    }

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
     * @return length in bytes of the instruction.
     */
    public int length() {
        return bytes.length;
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

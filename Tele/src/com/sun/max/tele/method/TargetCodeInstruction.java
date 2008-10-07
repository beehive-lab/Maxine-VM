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
/*VCSID=fc1a4f77-0360-4961-bc92-79f2df39df87*/
package com.sun.max.tele.method;

import com.sun.max.asm.dis.*;
import com.sun.max.unsafe.*;

/**
 * A single, disassembled native code instruction.
 *
 * @author Michael Van De Vanter
  */
public final class TargetCodeInstruction {

    private final String _mnemonic;

    /**
     * @return disassembled mnemonic for this instruction.
     */
    public String mnemonic() {
        return _mnemonic;
    }

    private final int _position;

    /**
     * @return position in bytes in the sequence of instructions for this method
     */
    public int position() {
        return _position;
    }

    private final Address _address;

    /**
     * @return address of the first byte of the instruction
     */
    public Address address() {
        return _address;
    }

    private final String _label;

    /**
     * The label (if any) at this instruction's address.
     */
    public String label() {
        return _label;
    }

    private final byte[] _bytes;

    public byte[] bytes() {
        return _bytes;
    }

    private final String _operands;

    public String operands() {
        return _operands;
    }

    /**
     * The target address of this instruction if it is a direct call or jump instruction otherwise null.
     */
    public final Address _targetAddress;

    /**
     * The address of a literal value loaded by a load instruction. How the literal is loaded depends on the instruction set
     * (e.g., load relative to current pc, or relative of a base register, etc...)
     */
    public final Address _literalSourceAddress;

    public String getMnemonic() {
        return _mnemonic;
    }

    public int getPosition() {
        return _position;
    }

    public long getAddress() {
        return _address.toLong();
    }

    public String getLabel() {
        return _label;
    }

    public byte[] getBytes() {
        return _bytes;
    }

    public String getOperands() {
        return _operands;
    }

    public long getTargetAddress() {
        if (_targetAddress == null) {
            return 0;
        }
        return _targetAddress.toLong();
    }

    TargetCodeInstruction(String mnemonic, Address address, int position, String label, byte[] bytes, String operands, Address targetAddress, Address literalSourceAddress) {
        _mnemonic = mnemonic;
        _address = address;
        _position = position;
        _label = label;
        _bytes = bytes;
        _operands = operands;
        _targetAddress = targetAddress;
        _literalSourceAddress = literalSourceAddress;
    }

    /**
     * @return length in bytes of the instruction.
     */
    public int length() {
        return _bytes.length;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(_address.toHexString()).
            append("[+").
            append(_position).
            append("] ");
        if (_label != null) {
            sb.append(_label).
                append(": ");
        }
        sb.append(_mnemonic).
            append(' ').
            append(_operands).
            append("    ").
            append(DisassembledInstruction.toHexString(_bytes));
        return sb.toString();
    }
}

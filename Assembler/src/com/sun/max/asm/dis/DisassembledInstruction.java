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
package com.sun.max.asm.dis;

import com.sun.max.asm.*;
import com.sun.max.asm.gen.*;
import com.sun.max.collect.*;

/**
 * A assembly instruction in internal format, combined with the bytes that it was disassembled from.
 *
 * @author Dave Ungar
 * @author Adam Spitz
 * @author Bernd Mathiske
 * @author Greg Wright
 */
public class DisassembledInstruction implements DisassembledObject {

    private final Disassembler disassembler;
    private final int startPosition;
    private final byte[] bytes;
    private final Template template;
    private final IndexedSequence<Argument> arguments;

    public DisassembledInstruction(Disassembler disassembler, int position, byte[] bytes, Template template, IndexedSequence<Argument> arguments) {
        assert bytes.length != 0;
        this.disassembler = disassembler;
        this.startPosition = position;
        this.bytes = bytes;
        this.template = template;
        this.arguments = arguments;
    }

    public ImmediateArgument startAddress() {
        return disassembler.startAddress().plus(startPosition);
    }

    /**
     * Gets the address to which an offset argument of this instruction is relative.
     */
    public ImmediateArgument addressForRelativeAddressing() {
        return disassembler.addressForRelativeAddressing(this);
    }

    public int startPosition() {
        return startPosition;
    }

    public int endPosition() {
        return startPosition + bytes.length;
    }

    public byte[] bytes() {
        return bytes.clone();
    }

    public Template template() {
        return template;
    }

    @Override
    public boolean isCode() {
        return true;
    }

    public IndexedSequence<Argument> arguments() {
        return arguments;
    }

    @Override
    public String toString() {
        return toString(disassembler.addressMapper());
    }

    @Override
    public String mnemonic() {
        return disassembler.mnemonic(this);
    }

    public String toString(AddressMapper addressMapper) {
        return disassembler.toString(this, addressMapper);
    }

    public String operandsToString(AddressMapper addressMapper) {
        return disassembler.operandsToString(this, addressMapper);
    }

    public ImmediateArgument targetAddress() {
        final int parameterIndex = template().labelParameterIndex();
        if (parameterIndex >= 0) {
            final ImmediateArgument immediateArgument = (ImmediateArgument) arguments().get(parameterIndex);
            final Parameter parameter = template().parameters().get(parameterIndex);
            if (parameter instanceof OffsetParameter) {
                return addressForRelativeAddressing().plus(immediateArgument);
            }
            return immediateArgument;
        }
        return null;
    }

    public static String toHexString(byte[] bytes) {
        String result = "[";
        String separator = "";
        for (byte b : bytes) {
            result += separator + String.format("%02X", b);
            separator = " ";
        }
        result += "]";
        return result;
    }
}

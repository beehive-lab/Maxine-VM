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
public abstract class DisassembledInstruction<Template_Type extends Template> implements DisassembledObject {

    private final Disassembler _disassembler;
    private final int _startPosition;
    private final byte[] _bytes;
    private final Template_Type _template;
    private final IndexedSequence<Argument> _arguments;

    protected DisassembledInstruction(Disassembler disassembler, int position, byte[] bytes, Template_Type template, IndexedSequence<Argument> arguments) {
        assert bytes.length != 0;
        _disassembler = disassembler;
        _startPosition = position;
        _bytes = bytes;
        _template = template;
        _arguments = arguments;
    }

    public ImmediateArgument startAddress() {
        return _disassembler.startAddress().plus(_startPosition);
    }

    public ImmediateArgument endAddress() {
        return _disassembler.startAddress().plus(endPosition());
    }

    public int startPosition() {
        return _startPosition;
    }

    public int endPosition() {
        return _startPosition + _bytes.length;
    }

    public byte[] bytes() {
        return _bytes.clone();
    }

    public Template_Type template() {
        return _template;
    }

    public Type type() {
        return Type.CODE;
    }

    public IndexedSequence<Argument> arguments() {
        return _arguments;
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

    @Override
    public String toString() {
        return toString(_disassembler.addressMapper());
    }

    public abstract String toString(AddressMapper addressMapper);

    public abstract String operandsToString(AddressMapper addressMapper);

    /**
     * Gets the address to which an offset argument of this instruction is relative.
     */
    public abstract ImmediateArgument addressForRelativeAddressing();

    public ImmediateArgument targetAddress() {
        final Template_Type template = template();
        final int parameterIndex = template.labelParameterIndex();
        if (parameterIndex >= 0) {
            final ImmediateArgument immediateArgument = (ImmediateArgument) arguments().get(parameterIndex);
            final Parameter parameter = template.parameters().get(parameterIndex);
            if (parameter instanceof OffsetParameter) {
                return addressForRelativeAddressing().plus(immediateArgument);
            }
            return immediateArgument;
        }
        return null;
    }

    /**
     * Gets the byte array encoding this instruction.
     */
    protected byte[] rawInstruction() {
        return _bytes;
    }
}

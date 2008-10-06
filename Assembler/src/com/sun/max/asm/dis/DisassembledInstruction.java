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
/*VCSID=fe70dae4-a119-4686-b750-d53d4b2f29e8*/
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
public abstract class DisassembledInstruction<Template_Type extends Template> implements AssemblyObject, AddressInstruction {

    private final int _startPosition;
    private final byte[] _bytes;
    private final Template_Type _template;
    private final IndexedSequence<Argument> _arguments;

    protected DisassembledInstruction(int position, byte[] bytes, Template_Type template, IndexedSequence<Argument> arguments) {
        _startPosition = position;
        _bytes = bytes;
        _template = template;
        _arguments = arguments;
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

    protected static DisassembledLabel instructionPositionToLabel(int instructionPosition, Sequence<DisassembledLabel> labels) {
        for (DisassembledLabel label : labels) {
            if (label.position() == instructionPosition) {
                return label;
            }
        }
        return null;
    }

    protected DisassembledLabel offsetArgumentToLabel(ImmediateArgument argument, Sequence<DisassembledLabel> labels) {
        final int argumentOffset = (int) argument.asLong();
        final int targetPosition = argumentOffset + positionForRelativeAddressing();
        return instructionPositionToLabel(targetPosition, labels);
    }

    protected DisassembledLabel addressArgumentToLabel(ImmediateArgument argument, Sequence<DisassembledLabel> labels) {
        final long targetOffset = addressToPosition(argument);
        if (targetOffset < 0) {
            return null;
        }
        return instructionPositionToLabel((int) targetOffset, labels);
    }

    public abstract String toString(Sequence<DisassembledLabel> labels, GlobalLabelMapper globalLabelMapper);

    public String toString(Sequence<DisassembledLabel> labels) {
        return toString(labels, null);
    }

    public abstract String externalName();

    public abstract String operandsToString(Sequence<DisassembledLabel> labels, GlobalLabelMapper globalLabelMapper);

    public String operandsToString(Sequence<DisassembledLabel> labels) {
        return operandsToString(labels, null);
    }

    public abstract int positionForRelativeAddressing();

    /*
     * Return the byte array encoding this instruction.
     */
    protected byte[] rawInstruction() {
        return _bytes;
    }

}

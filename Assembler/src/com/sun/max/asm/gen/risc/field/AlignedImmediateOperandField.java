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
package com.sun.max.asm.gen.risc.field;

import java.util.*;

import com.sun.max.asm.*;
import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.risc.bitRange.*;

/**
 * An instruction field whose encoded value does not include bits for
 * the low-order 0 bits of the aligned values that the field represents.
 * This class can convert between the field's <i>argument</i> (i.e.
 * the represented value) and it's <i>operand</i> (i.e. the encoded value).
 *
 * @author Dave Ungar
 * @author Bernd Mathiske
 * @author Adam Spitz
 * @author Doug Simon
 */
public class AlignedImmediateOperandField extends ImmediateOperandField {

    protected int zeroes;

    public AlignedImmediateOperandField(BitRange bitRange, int zeroes) {
        super(bitRange);
        this.zeroes = zeroes;
    }

    @Override
    public String asJavaExpression() {
        final String value = valueString();
        return "(" + super.asJavaExpression() + ") && ((" + value + " % " + grain() + ") == 0)";
    }

    @Override
    public boolean check(Template template, List<Argument> arguments) {
        if (!super.check(template, arguments)) {
            return false;
        }
        final long value = template.bindingFor(this, arguments).asLong();
        return (value % grain()) == 0;
    }

    @Override
    public int maxArgumentValue() {
        return super.maxArgumentValue() << zeroes();
    }

    @Override
    public int minArgumentValue() {
        return super.minArgumentValue() << zeroes();
    }

    @Override
    public int zeroes() {
        return zeroes;
    }

    /**
     * Converts an argument value to the operand value that does not include bits for the
     * implied low-order 0 bits that the aligned argument value is guaranteed to contain.
     * For example, if this field represents a 4-byte aligned value, then {@code argumentToOperand(536) == 134}.
     */
    private int argumentToOperand(int value) throws AssemblyException {
        final int p = grain();
        if (value % p != 0) {
            throw new AssemblyException("unaligned immediate operand: " + value);
        }
        return value / p;
    }

    /**
     * Converts an operand value to the argument value that includes
     * low-order 0 bits for the alignment of this field.
     * For example, if this field represents a 4-byte aligned value,
     * then {@code operandToArgument(134) == 536}.
     */
    private int operandToArgument(int operand) {
        return operand << zeroes();
    }

    @Override
    public int assemble(int value) throws IndexOutOfBoundsException, AssemblyException {
        return super.assemble(argumentToOperand(value));
    }

    @Override
    public Immediate32Argument disassemble(int instruction) {
        return new Immediate32Argument(operandToArgument(extract(instruction)));
    }
}

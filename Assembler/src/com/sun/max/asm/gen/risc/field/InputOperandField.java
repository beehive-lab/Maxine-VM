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

import com.sun.max.asm.*;
import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.risc.bitRange.*;
import com.sun.max.program.*;

/**
 * An input operand is a parameter to an assembler method that does not correspond directly
 * to a set of bits in the instruction but is a term in an expression that gives the value
 * for another operand that does represent a set of bits in the instruction.
 *
 * @author Doug Simon
 * @author Sumeet Panchal
 */
public class InputOperandField extends OperandField<ImmediateArgument> {

    private final Iterable< ? extends Argument> testArguments;
    private final ArgumentRange argumentRange;
    private final Iterable< ? extends Argument> illegalTestArguments;

    public InputOperandField(Iterable< ? extends Argument> testArguments, Iterable< ? extends Argument> illegalTestArguments, ArgumentRange argumentRange) {
        super(BitRange.create(new int[]{-1}, BitRangeOrder.DESCENDING));
        this.testArguments = testArguments;
        this.argumentRange = argumentRange;
        this.illegalTestArguments = illegalTestArguments;
    }

    public static InputOperandField create(OperandField valueRangeProvider) {
        return new InputOperandField(valueRangeProvider.getLegalTestArguments(), valueRangeProvider.getIllegalTestArguments(), valueRangeProvider.argumentRange());
    }

    @Override
    public ImmediateArgument disassemble(int instruction) {
        throw ProgramError.unexpected();
    }

    @Override
    public Class type() {
        return int.class;
    }

    public String valueString() {
        return variableName();
    }

    @Override
    public InputOperandField setVariableName(String name) {
        super.setVariableName(name);
        return this;
    }

    public Iterable< ? extends Argument> getLegalTestArguments() {
        return testArguments;
    }

    public Iterable<? extends Argument> getIllegalTestArguments() {
        //return Iterables.empty();
        return illegalTestArguments;
    }

    public ArgumentRange argumentRange() {
        return argumentRange;
    }

}

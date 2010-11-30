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
package com.sun.max.asm.gen.risc;

import java.util.*;

import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.risc.field.*;
import com.sun.max.program.*;

/**
 * @author Bernd Mathiske
 * @author Dave Ungar
 * @author Adam Spitz
 */
public class RiscTemplate extends Template implements RiscInstructionDescriptionVisitor {

    private final List<RiscField> allFields = new LinkedList<RiscField>();
    private final List<OperandField> operandFields = new LinkedList<OperandField>();
    private final List<OptionField> optionFields = new LinkedList<OptionField>();
    private final List<OperandField> parameters = new ArrayList<OperandField>();
    private final List<Option> options = new LinkedList<Option>();

    private int opcode;
    private int opcodeMask;
    private RiscTemplate canonicalRepresentative;

    public RiscTemplate(InstructionDescription instructionDescription) {
        super(instructionDescription);
    }

    @Override
    public RiscInstructionDescription instructionDescription() {
        return (RiscInstructionDescription) super.instructionDescription();
    }

    private RiscTemplate synthesizedFrom;

    public void setSynthesizedFrom(RiscTemplate synthesizedFrom) {
        assert instructionDescription().isSynthetic();
        this.synthesizedFrom = synthesizedFrom;
    }

    public RiscTemplate synthesizedFrom() {
        return synthesizedFrom;
    }

    /**
     * Adds the value of a constant field to the opcode of the instruction and
     * updates the opcode mask to include the bits of the field.
     *
     * @param field a field containing a constant value
     * @param value the constant value
     */
    private void organizeConstant(RiscField field, int value) {
        try {
            opcode |= field.bitRange().assembleUnsignedInt(value);
            opcodeMask |= field.bitRange().instructionMask();
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
            ProgramError.unexpected("operand for constant field " + field.name() + " does not fit: " + value);
        }
    }

    public void visitField(RiscField field) {
        allFields.add(field);
        if (field instanceof OperandField) {
            final OperandField operandField = (OperandField) field;
            if (field instanceof OffsetParameter) {
                setLabelParameterIndex();
            }
            if (operandField.boundTo() == null) {
                parameters.add(operandField);
            }
            operandFields.add(operandField);
        } else if (field instanceof OptionField) {
            optionFields.add((OptionField) field);
        } else if (field instanceof ReservedField) {
            organizeConstant(field, 0);
        } else {
            ProgramError.unexpected("unknown or unallowed type of field: " + field);
        }
    }

    public void visitConstant(RiscConstant constant) {
        organizeConstant(constant.field(), constant.value());
    }

    public void visitConstraint(InstructionConstraint constraint) {
    }

    /**
     * Sets the internal name of this template from a given string it is not already set.
     *
     * @param string  a string specified in the to consider
     */
    public void visitString(String string) {
        if (internalName() == null) {
            setInternalName(string);
        }
    }

    public List<OperandField> operandFields() {
        return operandFields;
    }

    public int opcode() {
        return opcode;
    }

    public int opcodeMask() {
        return opcodeMask;
    }

    public List<OptionField> optionFields() {
        return optionFields;
    }

    public void addOptionField(OptionField f) {
        allFields.add(f);
        optionFields.add(f);
    }

    public int specificity() {
        return Integer.bitCount(opcodeMask);
    }

    public void organizeOption(Option option, RiscTemplate canonicalRepresentative) {
        instructionDescription().setExternalName(externalName() + option.externalName());
        setInternalName(internalName() + option.name());
        try {
            opcode |= option.field().bitRange().assembleUnsignedInt(option.value());
            opcodeMask |= option.field().bitRange().instructionMask();
        } catch (IndexOutOfBoundsException e) {
            ProgramError.unexpected("Option: " + option.name() + " does not fit in field " + option.field().name());
        }

        options.add(option);
        if (option.isRedundant()) {
            this.canonicalRepresentative = canonicalRepresentative;
        }
    }

    @Override
    public Template canonicalRepresentative() {
        return canonicalRepresentative;
    }

    @Override
    public String assemblerMethodName() {
        return internalName();
    }

    @Override
    public List<Operand> operands() {
        throw ProgramError.unexpected("unimplemented");
    }

    @Override
    public List<OperandField> parameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return "<" + getClass().getSimpleName() + " #" + serial() + ": " + internalName() + " " + Integer.toHexString(opcode()) + ", " + parameters() + ">";
    }

}

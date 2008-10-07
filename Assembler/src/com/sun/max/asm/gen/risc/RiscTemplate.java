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
/*VCSID=fe64cdcc-bbcf-4903-afe8-c2313d664d38*/
package com.sun.max.asm.gen.risc;

import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.risc.field.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;

/**
 * @author Bernd Mathiske
 * @author Dave Ungar
 * @author Adam Spitz
 */
public abstract class RiscTemplate extends Template implements RiscInstructionDescriptionVisitor {

    private final AppendableSequence<RiscField> _allFields = new LinkSequence<RiscField>();
    private final AppendableSequence<OperandField> _operandFields = new LinkSequence<OperandField>();
    private final AppendableSequence<OptionField> _optionFields = new LinkSequence<OptionField>();
    private final AppendableIndexedSequence<OperandField> _parameters = new ArrayListSequence<OperandField>();
    private final AppendableSequence<Option> _options = new LinkSequence<Option>();

    private int _opcode;
    private int _opcodeMask;
    private RiscTemplate _canonicalRepresentative;

    protected RiscTemplate(InstructionDescription instructionDescription) {
        super(instructionDescription);
    }

    @Override
    public RiscInstructionDescription instructionDescription() {
        return (RiscInstructionDescription) super.instructionDescription();
    }

    private RiscTemplate _synthesizedFrom;

    public void setSynthesizedFrom(RiscTemplate synthesizedFrom) {
        assert instructionDescription().isSynthetic();
        _synthesizedFrom = synthesizedFrom;
    }

    public RiscTemplate synthesizedFrom() {
        return _synthesizedFrom;
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
            _opcode |= field.bitRange().assembleUnsignedInt(value);
            _opcodeMask |= field.bitRange().instructionMask();
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
            ProgramError.unexpected("operand for constant field " + field.name() + " does not fit: " + value);
        }
    }

    public void visitField(RiscField field) {
        _allFields.append(field);
        if (field instanceof OperandField) {
            final OperandField operandField = (OperandField) field;
            if (field instanceof OffsetParameter) {
                setLabelParameterIndex();
            }
            if (operandField.boundTo() == null) {
                _parameters.append(operandField);
            }
            _operandFields.append(operandField);
        } else if (field instanceof OptionField) {
            _optionFields.append((OptionField) field);
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

    public Sequence<OperandField> operandFields() {
        return _operandFields;
    }

    public int opcode() {
        return _opcode;
    }

    public int opcodeMask() {
        return _opcodeMask;
    }

    public Sequence<OptionField> optionFields() {
        return _optionFields;
    }

    public void addOptionField(OptionField f) {
        _allFields.append(f);
        _optionFields.append(f);
    }

    public int specificity() {
        return Integer.bitCount(_opcodeMask);
    }

    public void organizeOption(Option option, RiscTemplate canonicalRepresentative) {
        instructionDescription().setExternalName(externalName() + option.externalName());
        setInternalName(internalName() + option.name());
        try {
            _opcode |= option.field().bitRange().assembleUnsignedInt(option.value());
            _opcodeMask |= option.field().bitRange().instructionMask();
        } catch (IndexOutOfBoundsException e) {
            ProgramError.unexpected("Option: " + option.name() + " does not fit in field " + option.field().name());
        }

        _options.append(option);
        if (option.isRedundant()) {
            _canonicalRepresentative = canonicalRepresentative;
        }
    }

    @Override
    public boolean isRedundant() {
        return _canonicalRepresentative != null;
    }

    @Override
    public boolean isEquivalentTo(Template other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RiscTemplate)) {
            return false;
        }
        RiscTemplate a = this;
        if (a._canonicalRepresentative != null) {
            a = a._canonicalRepresentative;
        }
        RiscTemplate b = (RiscTemplate) other;
        if (b._canonicalRepresentative != null) {
            b = b._canonicalRepresentative;
        }
        return a == b;
    }

    @Override
    public String assemblerMethodName() {
        return internalName();
    }

    @Override
    public Sequence<Operand> operands() {
        Problem.unimplemented();
        return null;
    }

    @Override
    public IndexedSequence<OperandField> parameters() {
        return _parameters;
    }

    @Override
    public String toString() {
        return "<" + getClass().getSimpleName() + " #" + serial() + ": " + internalName() + " " + Integer.toHexString(opcode()) + ", " + parameters() + ">";
    }

}

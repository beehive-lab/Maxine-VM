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

import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.risc.field.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;

/**
 * Output of RISC instructions in external assembler format
 * We use exactly the same syntax as Sun's SPARC assembler "as"
 * and GNU's assembler "gas", except for branches to detected labels.
 * In the latter case, the label's name followed by ":"
 * is printed instead of ".".
 *
 * Examples of branch instructions without labels:
 *
 *     brz,a  . +20
 *     bne    . -200
 *
 * Examples of branch instructions with detected labels:
 *
 *     ba     L1: +112
 *     be,pt  L2: -50
 *
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Greg Wright
 */
public abstract class RiscExternalInstruction implements RiscInstructionDescriptionVisitor {

    protected final RiscTemplate _template;
    protected final Queue<Argument> _arguments;
    protected final int _position;
    protected final Sequence<DisassembledLabel> _labels;
    protected final GlobalLabelMapper _globalLabelMapper;

    public RiscExternalInstruction(RiscTemplate template, Sequence<Argument> arguments) {
        _template = template;
        _arguments = new MutableQueue<Argument>(arguments);
        _position = -1;
        _labels = Sequence.Static.empty(DisassembledLabel.class);
        _globalLabelMapper = null;
    }

    public RiscExternalInstruction(RiscTemplate template, Sequence<Argument> arguments, int position, Sequence<DisassembledLabel> labels) {
        _template = template;
        _arguments = new MutableQueue<Argument>(arguments);
        _position = position;
        _labels = labels;
        _globalLabelMapper = null;
    }

    public RiscExternalInstruction(RiscTemplate template, Sequence<Argument> arguments, int position, Sequence<DisassembledLabel> labels, GlobalLabelMapper globalLabelMapper) {
        _template = template;
        _arguments = new MutableQueue<Argument>(arguments);
        _position = position;
        _labels = labels;
        _globalLabelMapper = globalLabelMapper;
    }

    private String _nameString;

    public String name() {
        if (_nameString == null) {
            _nameString = _template.externalName();
            for (Argument argument : _arguments) {
                if (argument instanceof ExternalMnemonicSuffixArgument) {
                    final String suffix = argument.externalValue();
                    _nameString += suffix;
                }
            }
        }
        return _nameString;
    }

    private String _operandsString;

    public String operands() {
        if (_operandsString == null) {
            _operandsString = "";
            RiscInstructionDescriptionVisitor.Static.visitInstructionDescription(this, _template.instructionDescription());
        }
        return _operandsString;
    }

    @Override
    public String toString() {
        return Strings.padLengthWithSpaces(name(), 10) + "    " + operands();
    }

    private void print(String s) {
        _operandsString += s;
    }

    protected abstract boolean isAbsoluteBranch();

    /**
     * @return the symbol used to represent the value of the current location counter
     */
    protected String locationCounterSymbol() {
        return ".";
    }

    private void printBranchDisplacement(ImmediateArgument immediateArgument) {
        final int delta = (int) immediateArgument.asLong();
        if (_position > 0) {
            final int targetPosition = _position + delta;
            String globalName = null;
            if (_globalLabelMapper != null) {
                globalName = _globalLabelMapper.map(targetPosition);
            }
            if (globalName != null) {
                print(globalName + ": ");
            } else {
                for (DisassembledLabel label : _labels) {
                    if (label.position() == targetPosition) {
                        print(label.name() + ": ");
                    }
                }
            }
        } else {
            if (!isAbsoluteBranch()) {
                print(locationCounterSymbol() + " ");
            }
        }
        if (delta >= 0) {
            print("+");
        }
        print(Integer.toString(delta));
    }

    private Object _previousSpecification;

    public void visitField(RiscField field) {
        if (field instanceof OperandField) {
            final OperandField operandField = (OperandField) field;
            if (operandField.boundTo() != null) {
                return;
            }
            final Argument argument = _arguments.remove();
            if (argument instanceof ExternalMnemonicSuffixArgument) {
                return;
            }
            if (_previousSpecification != null && !(_previousSpecification instanceof String)) {
                print(", ");
            }
            if (argument instanceof ImmediateArgument) {
                final ImmediateArgument immediateArgument = (ImmediateArgument) argument;
                if (field instanceof BranchDisplacementOperandField) {
                    printBranchDisplacement(immediateArgument);
                } else {
                    if (operandField.isSigned()) {
                        print(immediateArgument.signedExternalValue());
                    } else {
                        print(immediateArgument.externalValue());
                    }
                }
            } else {
                print(argument.externalValue());
            }
            _previousSpecification = field;
        }
    }

    public void visitConstant(RiscConstant constant) {
    }

    private boolean _writingStrings;

    public void visitString(String string) {
        if (_writingStrings) {
            print(string);
            _previousSpecification = string;
        }
        _writingStrings = true;
    }

    public void visitConstraint(InstructionConstraint constraint) {
    }

}

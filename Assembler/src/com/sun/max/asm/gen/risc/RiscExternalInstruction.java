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

    protected final RiscTemplate template;
    protected final Queue<Argument> arguments;
    protected final ImmediateArgument address;
    protected final AddressMapper addressMapper;

    public RiscExternalInstruction(RiscTemplate template, Sequence<Argument> arguments) {
        this.template = template;
        this.arguments = new MutableQueue<Argument>(arguments);
        this.address = null;
        this.addressMapper = null;
    }

    public RiscExternalInstruction(RiscTemplate template, Sequence<Argument> arguments, ImmediateArgument address, AddressMapper addressMapper) {
        this.template = template;
        this.arguments = new MutableQueue<Argument>(arguments);
        this.address = address;
        this.addressMapper = addressMapper;
    }

    private String nameString;

    public String name() {
        if (nameString == null) {
            nameString = template.externalName();
            for (Argument argument : arguments) {
                if (argument instanceof ExternalMnemonicSuffixArgument) {
                    final String suffix = argument.externalValue();
                    nameString += suffix;
                }
            }
        }
        return nameString;
    }

    private String operandsString;

    public String operands() {
        if (operandsString == null) {
            operandsString = "";
            RiscInstructionDescriptionVisitor.Static.visitInstructionDescription(this, template.instructionDescription());
        }
        return operandsString;
    }

    @Override
    public String toString() {
        return Strings.padLengthWithSpaces(name(), 10) + "    " + operands();
    }

    private void print(String s) {
        operandsString += s;
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
        if (address != null) {
            final ImmediateArgument targetAddress = address.plus(delta);
            final DisassembledLabel label = addressMapper.labelAt(targetAddress);
            if (label != null) {
                print(label.name() + ": ");
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

    private Object previousSpecification;

    public void visitField(RiscField field) {
        if (field instanceof OperandField) {
            final OperandField operandField = (OperandField) field;
            if (operandField.boundTo() != null) {
                return;
            }
            final Argument argument = arguments.remove();
            if (argument instanceof ExternalMnemonicSuffixArgument) {
                return;
            }
            if (previousSpecification != null && !(previousSpecification instanceof String)) {
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
            previousSpecification = field;
        }
    }

    public void visitConstant(RiscConstant constant) {
    }

    private boolean writingStrings;

    public void visitString(String string) {
        if (writingStrings) {
            print(string);
            previousSpecification = string;
        }
        writingStrings = true;
    }

    public void visitConstraint(InstructionConstraint constraint) {
    }

}

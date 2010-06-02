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

import static com.sun.max.asm.gen.LabelParameter.*;

import java.util.*;

import com.sun.max.*;
import com.sun.max.asm.*;
import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.risc.bitRange.*;
import com.sun.max.asm.gen.risc.field.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * 
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class RiscAssemblerGenerator<Template_Type extends RiscTemplate> extends AssemblerGenerator<Template_Type> {

    protected RiscAssemblerGenerator(Assembly<Template_Type> assembly) {
        super(assembly, false);
    }

    private String encode(OperandField operandField, String val) {
        String value = val;
        // Convert the argument value to the operand value
        if (operandField.zeroes() != 0) {
            value = "(" + value + " >> " + operandField.zeroes() + ")";
        }
        return operandField.bitRange().encodingString(value, operandField.isSigned(), false);
    }

    @Override
    protected int printMethod(IndentWriter writer, Template_Type template) {
        final int startLineCount = writer.lineCount();
        writer.print("public void ");
        writer.print(template.assemblerMethodName() + "(");
        writer.print(formatParameterList("final ", template.parameters(), false));
        writer.println(") {");
        writer.indent();
        writer.println("int instruction = " + Ints.toHexLiteral(template.opcode()) + ";");

        // Print argument constraint checking statements
        final List<InstructionConstraint> constraints = template.instructionDescription().constraints();
        for (InstructionConstraint constraint : constraints) {
            if (!(constraint instanceof TestOnlyInstructionConstraint)) {
                final String constraintExpression = constraint.asJavaExpression();
                writer.println("checkConstraint(" + constraintExpression + ", \"" + constraintExpression + "\");");
            }
        }

        for (OperandField operandField : template.operandFields()) {
            if (operandField instanceof InputOperandField) {
                continue;
            }
            writer.println("instruction |= " + encode(operandField, operandField.valueString()) + ";");
        }

        writer.println("emitInt(instruction);");
        writer.outdent();
        writer.println("}");
        return writer.lineCount() - startLineCount;
    }

    @Override
    protected void printLabelMethod(final IndentWriter indentWriter, final Template_Type labelTemplate, String assemblerClassName) {
        final List<Parameter> parameters = getParameters(labelTemplate, true);
        final InstructionWithLabelSubclass labelInstructionSubclass = new InstructionWithLabelSubclass(labelTemplate, InstructionWithOffset.class, "");
        printLabelMethodHelper(indentWriter, labelTemplate, parameters, 4, assemblerClassName, labelInstructionSubclass);
    }

    /**
     * Prints the reference to the raw method from which a synthetic method was defined.
     */
    @Override
    protected void printExtraMethodJavadoc(IndentWriter writer, Template_Type template, List<String> extraLinks, boolean forLabelAssemblerMethod) {
        if (template.instructionDescription().isSynthetic()) {
            final RiscTemplate syntheticTemplate = template;
            final RiscTemplate rawTemplate = syntheticTemplate.synthesizedFrom();
            final List<? extends Parameter> parameters = getParameters(rawTemplate, forLabelAssemblerMethod);
            final String ref = rawTemplate.internalName() + "(" + formatParameterList("", parameters, true) + ")";
            writer.println(" * <p>");
            writer.print(" * This is a synthetic instruction equivalent to: {@code " + rawTemplate.internalName() + "(");
            extraLinks.add("#" + ref);

            boolean firstOperand = true;
            for (OperandField rawOperand : rawTemplate.operandFields()) {
                if (!firstOperand) {
                    writer.print(", ");
                }
                writer.print(getRawOperandReplacement(syntheticTemplate, rawTemplate, rawOperand, forLabelAssemblerMethod));
                firstOperand = false;
            }

            writer.println(")}");
        }
    }

    /**
     * Gets the expression in terms of the parameters and opcode of a synthetic instruction that replaces a parameter of
     * the raw instruction from which the synthetic operand was derived.
     * 
     * @param syntheticTemplate
     *                the synthetic instruction
     * @param rawTemplate
     *                the raw instruction from which {@code syntheticTemplate} was derived
     * @param rawOperand
     *                a parameter of {@code rawTemplate}
     */
    private String getRawOperandReplacement(RiscTemplate syntheticTemplate, RiscTemplate rawTemplate, OperandField rawOperand, boolean forLabelAssemblerMethod) {
        if (Utils.indexOfIdentical(syntheticTemplate.operandFields(), rawOperand) != -1) {
            if (rawOperand instanceof OffsetParameter && forLabelAssemblerMethod) {
                return LABEL.variableName();
            }
            return rawOperand.variableName();
        }

        final int rawOperandMask = rawOperand.bitRange().instructionMask();
        String expression = null;
        if ((syntheticTemplate.opcodeMask() & rawOperandMask) != 0) {
            // Some or all bits of the raw operand are encoded as part of the synthetic instruction opcode
            final Argument value = rawOperand.disassemble(syntheticTemplate.opcode());
            assert value != null;
            if (value instanceof SymbolicArgument) {
                expression = ((SymbolicArgument) value).name();
            } else if (value instanceof Enum) {
                expression = ((Enum) value).name();
            } else if (value instanceof ImmediateArgument) {
                expression = Long.toString(((ImmediateArgument) value).asLong());
            } else {
                ProgramError.unexpected("unknown type of disassembled value: " + value.getClass().getName());
            }
        }
        if ((syntheticTemplate.opcodeMask() & rawOperandMask) != rawOperandMask) {
            // Some or all bits of the raw operand are given as a parameter of the synthetic instruction
            for (OperandField syntheticOperand : syntheticTemplate.operandFields()) {
                final int syntheticOperandMask = syntheticOperand.bitRange().instructionMask();
                if ((syntheticOperandMask & rawOperandMask) != 0) {
                    final String term;
                    if (syntheticOperand.boundTo() != null) {
                        term = syntheticOperand.boundTo().valueString();
                    } else {
                        assert (syntheticOperandMask & rawOperandMask) == syntheticOperandMask :
                            "cannot handle synthetic parameter that defines bits that are not a subset of bits defined by a raw parameter";
                        final BitRange subFieldRange = syntheticOperand.bitRange().move(false, syntheticOperand.bitRange().numberOfLessSignificantBits());
                        final int shift = syntheticOperand.bitRange().numberOfLessSignificantBits() - rawOperand.bitRange().numberOfLessSignificantBits();
                        final String value = syntheticOperand.variableName();
                        final String assembledSubField = subFieldRange.encodingString(value, syntheticOperand.isSigned(), true);
                        if (shift != 0) {
                            term = "(" + assembledSubField + " * " + (1 << shift) + ")";
                        } else {
                            term = assembledSubField;
                        }
                    }

                    if (expression != null && !expression.equals("0")) {
                        expression += " | " + term;
                    } else {
                        expression = term;
                    }
                }
            }
        }
        assert expression != null;
        return expression;
    }
}

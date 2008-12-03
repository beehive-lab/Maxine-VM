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
package com.sun.max.asm.gen.cisc.amd64;

import com.sun.max.asm.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.asm.amd64.complete.*;
import com.sun.max.asm.dis.*;
import com.sun.max.asm.dis.amd64.*;
import com.sun.max.asm.gen.cisc.x86.*;
import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * Run this program to generate the AMD64RawAssembler and AMD64LabelAssembler classes.
 *
 * @author Bernd Mathiske
 */
public class AMD64AssemblerGenerator extends X86AssemblerGenerator<AMD64Template> {

    public AMD64AssemblerGenerator() {
        super(AMD64Assembly.ASSEMBLY, WordWidth.BITS_64);
    }

    public static void main(String[] programArguments) {
        final AMD64AssemblerGenerator generator = new AMD64AssemblerGenerator();
        generator._options.parseArguments(programArguments);
        generator.generate();
    }

    private static final String REX_BYTE_NAME = "rex";

    private String basicRexValue(X86Template template) {
        if (template.operandSizeAttribute() == WordWidth.BITS_64 && template.instructionDescription().defaultOperandSize() != WordWidth.BITS_64) {
            return Bytes.toHexLiteral((byte) (X86Opcode.REX_MIN.ordinal() + (1 << X86Field.REX_W_BIT_INDEX)));
        }
        return Bytes.toHexLiteral((byte) X86Opcode.REX_MIN.ordinal());
    }

    private void printUnconditionalRexBit(IndentWriter writer, X86Parameter parameter, int bitIndex) {
        writer.print(REX_BYTE_NAME + " |= (" + parameter.valueString() + " & 8) >> " + (3 - bitIndex) + ";");
        writer.println(" // " + parameter.place().comment());
    }

    private void checkGeneralRegister8Values(IndentWriter writer, X86Template template) {
        for (X86Parameter parameter : template.parameters()) {
            if (parameter.type() == AMD64GeneralRegister8.class) {
                writer.println("if (" + parameter.variableName() + ".isHighByte()) {");
                writer.indent();
                writer.println("throw new IllegalArgumentException(\"Cannot encode \" + " + parameter.variableName() + ".name() + \" in the presence of a REX prefix\");");
                writer.outdent();
                writer.println("}");
            }
        }
    }

    private void printUnconditionalRexPrefix(IndentWriter writer, X86Template template) {
        writer.println("byte " + REX_BYTE_NAME + " = (byte) " + basicRexValue(template) + ";");
        for (X86Parameter parameter : template.parameters()) {
            switch (parameter.place()) {
                case MOD_REG_REXR:
                    printUnconditionalRexBit(writer, parameter, X86Field.REX_R_BIT_INDEX);
                    break;
                case MOD_RM_REXB:
                case SIB_BASE_REXB:
                case OPCODE1_REXB:
                case OPCODE2_REXB:
                    printUnconditionalRexBit(writer, parameter, X86Field.REX_B_BIT_INDEX);
                    break;
                case SIB_INDEX_REXX:
                    printUnconditionalRexBit(writer, parameter, X86Field.REX_X_BIT_INDEX);
                    break;
                default:
                    break;
            }
        }
        checkGeneralRegister8Values(writer, template);
        emitByte(writer, REX_BYTE_NAME);
        writer.println();
    }

    private void printConditionalRexBit(IndentWriter writer, X86Template template, X86Parameter parameter, int bitIndex) {
        if (parameter.type() == AMD64GeneralRegister8.class) {
            writer.println("if (" + parameter.variableName() + ".requiresRexPrefix()) {");
            writer.indent();
            writer.println(REX_BYTE_NAME + " |= " + basicRexValue(template) + ";");
            writer.println("if (" + parameter.valueString() + " >= 8) {");
            writer.indent();
            writer.println(REX_BYTE_NAME + " |= 1 << " + bitIndex + "; // " + parameter.place().comment());
            writer.outdent();
            writer.println("}");
            writer.outdent();
            writer.println("}");
        } else {
            writer.println("if (" + parameter.valueString() + " >= 8) {");
            writer.indent();
            writer.println(REX_BYTE_NAME + " |= (1 << " + bitIndex + ") + " + basicRexValue(template) + "; // " + parameter.place().comment());
            writer.outdent();
            writer.println("}");
        }
    }

    private void printConditionalRexPrefix(IndentWriter writer, X86Template template) {
        writer.println("byte " + REX_BYTE_NAME + " = (byte) 0;");
        for (X86Parameter parameter : template.parameters()) {
            switch (parameter.place()) {
                case MOD_REG_REXR:
                    printConditionalRexBit(writer, template, parameter, X86Field.REX_R_BIT_INDEX);
                    break;
                case MOD_RM_REXB:
                case SIB_BASE_REXB:
                case OPCODE1_REXB:
                case OPCODE2_REXB:
                    printConditionalRexBit(writer, template, parameter, X86Field.REX_B_BIT_INDEX);
                    break;
                case SIB_INDEX_REXX:
                    printConditionalRexBit(writer, template, parameter, X86Field.REX_X_BIT_INDEX);
                    break;
                default:
                    break;
            }
        }
        writer.println("if (" + REX_BYTE_NAME + " != (byte) 0) {");
        writer.indent();
        checkGeneralRegister8Values(writer, template);
        emitByte(writer, REX_BYTE_NAME);
        writer.println();
        writer.outdent();
        writer.println("}");
    }

    @Override
    protected void printPrefixes(IndentWriter writer, AMD64Template template) {
        super.printPrefixes(writer, template);
        if (template.operandSizeAttribute() == WordWidth.BITS_64 && template.instructionDescription().defaultOperandSize() != WordWidth.BITS_64) {
            printUnconditionalRexPrefix(writer, template);
        } else {
            for (X86Parameter parameter : template.parameters()) {
                switch (parameter.place()) {
                    case MOD_REG_REXR:
                    case MOD_RM_REXB:
                    case SIB_BASE_REXB:
                    case SIB_INDEX_REXX:
                    case OPCODE1_REXB:
                    case OPCODE2_REXB:
                        printConditionalRexPrefix(writer, template);
                        return;
                    default:
                        break;
                }
            }
        }
    }

    @Override
    protected void printModVariants(IndentWriter writer, AMD64Template template) {
        if (template.modCase() != X86TemplateContext.ModCase.MOD_0 || template.parameters().length() == 0) {
            return;
        }
        switch (template.rmCase()) {
            case NORMAL: {
                switch (template.addressSizeAttribute()) {
                    case BITS_32:
                        printModVariant(writer, template, AMD64IndirectRegister32.EBP_INDIRECT);
                        break;
                    case BITS_64:
                        printModVariant(writer, template, AMD64IndirectRegister64.RBP_INDIRECT, AMD64IndirectRegister64.R13_INDIRECT);
                        break;
                    default:
                        ProgramError.unexpected();
                }
                break;
            }
            case SIB: {
                switch (template.sibBaseCase()) {
                    case GENERAL_REGISTER:
                        switch (template.addressSizeAttribute()) {
                            case BITS_32:
                                printModVariant(writer, template, AMD64BaseRegister32.EBP_BASE);
                                break;
                            case BITS_64:
                                printModVariant(writer, template, AMD64BaseRegister64.RBP_BASE, AMD64BaseRegister64.R13_BASE);
                                break;
                            default:
                                ProgramError.unexpected();
                        }
                        break;
                    default:
                        break;
                }
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    protected void printSibVariants(IndentWriter writer, AMD64Template template) {
        if (template.parameters().length() == 0 || template.modCase() == null || template.modCase() == X86TemplateContext.ModCase.MOD_3 || template.rmCase() != X86TemplateContext.RMCase.NORMAL) {
            return;
        }
        switch (template.modCase()) {
            case MOD_0:
            case MOD_1:
            case MOD_2: {
                switch (template.addressSizeAttribute()) {
                    case BITS_32:
                        printSibVariant(writer, template, AMD64IndirectRegister32.ESP_INDIRECT);
                        break;
                    case BITS_64:
                        printSibVariant(writer, template, AMD64IndirectRegister64.RSP_INDIRECT, AMD64IndirectRegister64.R12_INDIRECT);
                        break;
                    default:
                        ProgramError.unexpected();
                }
                break;
            }
            default: {
                break;
            }
        }
    }

    @Override
    protected DisassembledInstruction generateExampleInstruction(AMD64Template template, IndexedSequence<Argument> arguments) throws AssemblyException {
        final AMD64Assembler assembler = new AMD64Assembler(0);
        assembly().assemble(assembler, template, arguments);
        final byte[] bytes = assembler.toByteArray();
        return new AMD64DisassembledInstruction(new AMD64Disassembler(0, null), 0, bytes, template, arguments);
    }
}

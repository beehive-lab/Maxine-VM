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
package com.sun.max.asm.gen.cisc.x86;

import java.io.*;

import com.sun.max.asm.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.util.*;

/**
 * An assembler that creates binary instructions from templates and arguments.
 *
 * @author Bernd Mathiske
 */
public class X86TemplateAssembler<Template_Type extends X86Template> {

    private static final int MORE_BYTES_THAN_ANY_INSTRUCTION = 32;
    private final byte[] bytes = new byte[MORE_BYTES_THAN_ANY_INSTRUCTION];
    private int n;

    private int rexByte;

    private void emit(byte b) {
        bytes[n++] = b;
    }

    private void emit(int b) {
        bytes[n++] = (byte) (b & 0xff);
    }

    private void emit(HexByte b) {
        bytes[n++] = b.byteValue();
    }

    private int createRexData(int bitIndex, long argument) {
        final byte b = (byte) (argument & 0xffL);
        if (b == 0) {
            return 0;
        }
        return X86Field.inRexPlace(bitIndex, b);
    }

    private int createFieldData(X86Field field, long argument) {
        return field.inPlace((byte) (argument & 0xffL));
    }

    private final Template_Type template;
    private WordWidth addressWidth;

    public X86TemplateAssembler(Template_Type template, WordWidth addressWidth) {
        this.template = template;
        this.addressWidth = addressWidth;
    }

    private int createModRMByte() {
        if (!template.hasModRMByte()) {
            return 0;
        }
        int result = template.modCase().ordinal() << X86Field.MOD.shift();
        if (template.modRMGroupOpcode() != null) {
            result |= template.modRMGroupOpcode().byteValue() << X86Field.REG.shift();
        }
        result |= template.rmCase().value() << X86Field.RM.shift();
        return result;
    }

    private int createSibByte() {
        if (template.hasSibByte() && template.sibBaseCase() == X86TemplateContext.SibBaseCase.SPECIAL) {
            return 5 << X86Field.BASE.shift();
        }
        return 0;
    }

    private boolean modRMRequiresSib(int modRMByte) {
        final byte m = (byte) modRMByte;
        return X86Field.MOD.extract(m) != 3 && X86Field.RM.extract(m) == 4;
    }

    private boolean modRMRequiresImmediate(int modRMByte) {
        final byte m = (byte) modRMByte;
        return X86Field.MOD.extract(m) == 0 && X86Field.RM.extract(m) == 5;
    }

    private boolean sibRequiresImmediate(int sibRMByte) {
        final byte s = (byte) sibRMByte;
        return X86Field.BASE.extract(s) == 5;
    }

    public byte[] assemble(IndexedSequence<Argument> arguments) {
        int rexByt = 0;
        if (template.operandSizeAttribute() == WordWidth.BITS_64 && template.instructionDescription().defaultOperandSize() != WordWidth.BITS_64) {
            rexByt = X86Opcode.REX_MIN.byteValue() | (1 << X86Field.REX_W_BIT_INDEX);
        }
        int opcode1 = template.opcode1().byteValue() & 0xff;
        int opcode2 = template.opcode2() == null ? 0 : template.opcode2().byteValue() & 0xff;
        int modRMByte = createModRMByte();
        int sibByte = createSibByte();
        final ByteArrayOutputStream appendStream = new ByteArrayOutputStream();
        for (int i = 0; i < arguments.length(); i++) {
            final X86Parameter parameter = template.parameters().get(i);
            final long argument = arguments.get(i).asLong();
            switch (parameter.place()) {
                case MOD_REG_REXR:
                    rexByt |= createRexData(X86Field.REX_R_BIT_INDEX, argument);
                    // fall through...
                case MOD_REG:
                    modRMByte |= createFieldData(X86Field.REG, argument);
                    break;
                case MOD_RM_REXB:
                    rexByt |= createRexData(X86Field.REX_B_BIT_INDEX, argument);
                    // fall through...
                case MOD_RM:
                    modRMByte |= createFieldData(X86Field.RM, argument);
                    break;
                case SIB_BASE_REXB:
                    rexByt |= createRexData(X86Field.REX_B_BIT_INDEX, argument);
                    // fall through...
                case SIB_BASE:
                    sibByte |= createFieldData(X86Field.BASE, argument);
                    break;
                case SIB_INDEX_REXX:
                    rexByt |= createRexData(X86Field.REX_X_BIT_INDEX, argument);
                    // fall through...
                case SIB_INDEX:
                    sibByte |= createFieldData(X86Field.INDEX, argument);
                    break;
                case SIB_SCALE:
                    sibByte |= createFieldData(X86Field.SCALE, argument);
                    break;
                case APPEND:
                    if (parameter instanceof X86EnumerableParameter) {
                        appendStream.write((byte) (argument & 0xffL));
                    } else {
                        try {
                            final X86NumericalParameter numericalParameter = (X86NumericalParameter) parameter;
                            switch (numericalParameter.width()) {
                                case BITS_8:
                                    appendStream.write((byte) (argument & 0xffL));
                                    break;
                                case BITS_16:
                                    Endianness.LITTLE.writeShort(appendStream, (short) (argument & 0xffffL));
                                    break;
                                case BITS_32:
                                    Endianness.LITTLE.writeInt(appendStream, (int) (argument & 0xffffffffL));
                                    break;
                                case BITS_64:
                                    Endianness.LITTLE.writeLong(appendStream, argument);
                                    break;
                            }
                        } catch (IOException ioException) {
                            ProgramError.unexpected();
                        }
                    }
                    break;
                case OPCODE1_REXB:
                    rexByt |= createRexData(X86Field.REX_B_BIT_INDEX, argument);
                    // fall through...
                case OPCODE1:
                    opcode1 |= (int) argument & 7;
                    break;
                case OPCODE2_REXB:
                    rexByt |= createRexData(X86Field.REX_B_BIT_INDEX, argument);
                    // fall through...
                case OPCODE2:
                    opcode2 |= (int) argument & 7;
                    break;
            }
        }
        if (rexByt > 0) {
            emit(rexByt);
        }
        if (template.addressSizeAttribute() != addressWidth) {
            emit(X86Opcode.ADDRESS_SIZE);
        }
        if (template.operandSizeAttribute() == WordWidth.BITS_16) {
            emit(X86Opcode.OPERAND_SIZE);
        }
        if (template.instructionSelectionPrefix() != null) {
            emit(template.instructionSelectionPrefix());
        }
        emit(opcode1);
        if (opcode2 != 0) {
            emit(opcode2);
        }
        if (template.hasModRMByte()) {
            emit(modRMByte);
            if (modRMRequiresImmediate(modRMByte) && appendStream.size() == 0) {
                return null;
            }
        }
        if (template.hasSibByte()) {
            if (sibRequiresImmediate(sibByte) && appendStream.size() == 0) {
                return null;
            }
            emit(sibByte);
        } else if (modRMRequiresSib(modRMByte)) {
            return null;
        }
        for (byte b : appendStream.toByteArray()) {
            emit(b);
        }
        return Bytes.withLength(bytes, n);
    }

}

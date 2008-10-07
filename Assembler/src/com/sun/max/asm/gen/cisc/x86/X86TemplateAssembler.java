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
/*VCSID=312dff7f-9f56-4670-aa81-d1477dc65edd*/
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

    private static final int _MORE_BYTES_THAN_ANY_INSTRUCTION = 32;
    private final byte[] _bytes = new byte[_MORE_BYTES_THAN_ANY_INSTRUCTION];
    private int _n;

    private int _rexByte;

    private void emit(byte b) {
        _bytes[_n++] = b;
    }

    private void emit(int b) {
        _bytes[_n++] = (byte) (b & 0xff);
    }

    private void emit(HexByte b) {
        _bytes[_n++] = b.byteValue();
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

    private final Template_Type _template;
    private WordWidth _addressWidth;

    public X86TemplateAssembler(Template_Type template, WordWidth addressWidth) {
        _template = template;
        _addressWidth = addressWidth;
    }

    private int createModRMByte() {
        if (!_template.hasModRMByte()) {
            return 0;
        }
        int result = _template.modCase().ordinal() << X86Field.MOD.shift();
        if (_template.modRMGroupOpcode() != null) {
            result |= _template.modRMGroupOpcode().byteValue() << X86Field.REG.shift();
        }
        result |= _template.rmCase().value() << X86Field.RM.shift();
        return result;
    }

    private int createSibByte() {
        if (_template.hasSibByte() && _template.sibBaseCase() == X86TemplateContext.SibBaseCase.SPECIAL) {
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
        int rexByte = 0;
        if (_template.operandSizeAttribute() == WordWidth.BITS_64 && _template.instructionDescription().defaultOperandSize() != WordWidth.BITS_64) {
            rexByte = X86Opcode.REX_MIN.byteValue() | (1 << X86Field.REX_W_BIT_INDEX);
        }
        int opcode1 = _template.opcode1().byteValue() & 0xff;
        int opcode2 = _template.opcode2() == null ? 0 : _template.opcode2().byteValue() & 0xff;
        int modRMByte = createModRMByte();
        int sibByte = createSibByte();
        final ByteArrayOutputStream appendStream = new ByteArrayOutputStream();
        for (int i = 0; i < arguments.length(); i++) {
            final X86Parameter parameter = _template.parameters().get(i);
            final long argument = arguments.get(i).asLong();
            switch (parameter.place()) {
                case MOD_REG_REXR:
                    rexByte |= createRexData(X86Field.REX_R_BIT_INDEX, argument);
                    // fall through...
                case MOD_REG:
                    modRMByte |= createFieldData(X86Field.REG, argument);
                    break;
                case MOD_RM_REXB:
                    rexByte |= createRexData(X86Field.REX_B_BIT_INDEX, argument);
                    // fall through...
                case MOD_RM:
                    modRMByte |= createFieldData(X86Field.RM, argument);
                    break;
                case SIB_BASE_REXB:
                    rexByte |= createRexData(X86Field.REX_B_BIT_INDEX, argument);
                    // fall through...
                case SIB_BASE:
                    sibByte |= createFieldData(X86Field.BASE, argument);
                    break;
                case SIB_INDEX_REXX:
                    rexByte |= createRexData(X86Field.REX_X_BIT_INDEX, argument);
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
                    rexByte |= createRexData(X86Field.REX_B_BIT_INDEX, argument);
                    // fall through...
                case OPCODE1:
                    opcode1 |= (int) argument & 7;
                    break;
                case OPCODE2_REXB:
                    rexByte |= createRexData(X86Field.REX_B_BIT_INDEX, argument);
                    // fall through...
                case OPCODE2:
                    opcode2 |= (int) argument & 7;
                    break;
            }
        }
        if (rexByte > 0) {
            emit(rexByte);
        }
        if (_template.addressSizeAttribute() != _addressWidth) {
            emit(X86Opcode.ADDRESS_SIZE);
        }
        if (_template.operandSizeAttribute() == WordWidth.BITS_16) {
            emit(X86Opcode.OPERAND_SIZE);
        }
        if (_template.instructionSelectionPrefix() != null) {
            emit(_template.instructionSelectionPrefix());
        }
        emit(opcode1);
        if (opcode2 != 0) {
            emit(opcode2);
        }
        if (_template.hasModRMByte()) {
            emit(modRMByte);
            if (modRMRequiresImmediate(modRMByte) && appendStream.size() == 0) {
                return null;
            }
        }
        if (_template.hasSibByte()) {
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
        return Bytes.withLength(_bytes, _n);
    }

}

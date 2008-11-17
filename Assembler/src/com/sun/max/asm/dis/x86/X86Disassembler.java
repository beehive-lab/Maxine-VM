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
package com.sun.max.asm.dis.x86;

import java.io.*;
import java.util.*;

import com.sun.max.asm.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.asm.dis.*;
import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.cisc.x86.*;
import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.util.*;

/**
 * A disassembler for x86 instructions.
 *
 * @see Disassembler
 * @see X86DisassembledInstruction
 *
 * @author Bernd Mathiske
 * @author David Liu
 */
public abstract class X86Disassembler<Template_Type extends X86Template, DisassembledInstruction_Type extends X86DisassembledInstruction<Template_Type>>
                          extends Disassembler<Template_Type, DisassembledInstruction_Type> {

    protected X86Disassembler(ImmediateArgument startAddress, Assembly<Template_Type> assembly, InlineDataDecoder inlineDataDecoder) {
        super(startAddress, assembly, Endianness.LITTLE, inlineDataDecoder);
    }

    protected abstract boolean isRexPrefix(HexByte opcode);

    private X86InstructionHeader scanInstructionHeader(BufferedInputStream stream) throws IOException {
        int byteValue = stream.read();
        if (byteValue < 0) {
            return null;
        }
        final X86InstructionHeader header = new X86InstructionHeader();
        do {
            final HexByte hexByte = HexByte.VALUES.get(byteValue);
            if (header._opcode1 == null) {
                if (hexByte == X86Opcode.ADDRESS_SIZE) {
                    header._hasAddressSizePrefix = true;
                } else if (hexByte == X86Opcode.OPERAND_SIZE) {
                    header._instructionSelectionPrefix = hexByte;
                } else if (hexByte == X86Opcode.REPE || hexByte == X86Opcode.REPNE) {
                    header._instructionSelectionPrefix = hexByte;
                    return header;
                } else if (isRexPrefix(hexByte)) {
                    header._rexPrefix = hexByte;
                } else {
                    header._opcode1 = hexByte;
                    if (hexByte != HexByte._0F) {
                        return header;
                    }
                }
            } else {
                header._opcode2 = hexByte;
                return header;
            }
            byteValue = stream.read();
        } while (byteValue >= 0);
        return header;
    }

    private IndexedSequence<Argument> scanArguments(BufferedInputStream stream, Template_Type template, X86InstructionHeader header, byte modRMByte, byte sibByte) throws IOException {
        final AppendableIndexedSequence<Argument> arguments = new ArrayListSequence<Argument>();
        final byte rexByte = (header._rexPrefix != null) ? header._rexPrefix.byteValue() : 0;
        for (X86Parameter parameter : template.parameters()) {
            int value = 0;
            switch (parameter.place()) {
                case MOD_REG_REXR:
                    value = X86Field.extractRexValue(X86Field.REX_R_BIT_INDEX, rexByte);
                    // fall through...
                case MOD_REG:
                    value += X86Field.REG.extract(modRMByte);
                    break;
                case MOD_RM_REXB:
                    value = X86Field.extractRexValue(X86Field.REX_B_BIT_INDEX, rexByte);
                    // fall through...
                case MOD_RM:
                    value += X86Field.RM.extract(modRMByte);
                    break;
                case SIB_BASE_REXB:
                    value = X86Field.extractRexValue(X86Field.REX_B_BIT_INDEX, rexByte);
                    // fall through...
                case SIB_BASE:
                    value += X86Field.BASE.extract(sibByte);
                    break;
                case SIB_INDEX_REXX:
                    value = X86Field.extractRexValue(X86Field.REX_X_BIT_INDEX, rexByte);
                    // fall through...
                case SIB_INDEX:
                    value += X86Field.INDEX.extract(sibByte);
                    break;
                case SIB_SCALE:
                    value = X86Field.SCALE.extract(sibByte);
                    break;
                case APPEND:
                    if (parameter instanceof X86EnumerableParameter) {
                        final X86EnumerableParameter enumerableParameter = (X86EnumerableParameter) parameter;
                        final Enumerator enumerator = enumerableParameter.enumerator();
                        arguments.append((Argument) enumerator.fromValue(endianness().readByte(stream)));
                        continue;
                    }
                    final X86NumericalParameter numericalParameter = (X86NumericalParameter) parameter;
                    switch (numericalParameter.width()) {
                        case BITS_8:
                            arguments.append(new Immediate8Argument(endianness().readByte(stream)));
                            break;
                        case BITS_16:
                            arguments.append(new Immediate16Argument(endianness().readShort(stream)));
                            break;
                        case BITS_32:
                            arguments.append(new Immediate32Argument(endianness().readInt(stream)));
                            break;
                        case BITS_64:
                            arguments.append(new Immediate64Argument(endianness().readLong(stream)));
                            break;
                    }
                    continue;
                case OPCODE1_REXB:
                    value = X86Field.extractRexValue(X86Field.REX_B_BIT_INDEX, rexByte);
                    // fall through...
                case OPCODE1:
                    value += header._opcode1.ordinal() & 7;
                    break;
                case OPCODE2_REXB:
                    value = X86Field.extractRexValue(X86Field.REX_B_BIT_INDEX, rexByte);
                    // fall through...
                case OPCODE2:
                    value += header._opcode2.ordinal() & 7;
                    break;
            }
            final X86EnumerableParameter enumerableParameter = (X86EnumerableParameter) parameter;
            final Enumerator enumerator = enumerableParameter.enumerator();
            if (enumerator == AMD64GeneralRegister8.ENUMERATOR) {
                arguments.append(AMD64GeneralRegister8.fromValue(value, header._rexPrefix != null));
            } else {
                arguments.append((Argument) enumerator.fromValue(value));
            }
        }
        return arguments;
    }

    private int getModVariantParameterIndex(Template_Type template, byte modRMByte, byte sibByte) {
        if (template.modCase() == X86TemplateContext.ModCase.MOD_0 && X86Field.MOD.extract(modRMByte) != X86TemplateContext.ModCase.MOD_0.value()) {
            switch (template.rmCase()) {
                case NORMAL: {
                    if (template.addressSizeAttribute() == WordWidth.BITS_16) {
                        if (X86Field.RM.extract(modRMByte) != X86TemplateContext.RMCase.SWORD.value()) {
                            return -1;
                        }
                    } else if (X86Field.RM.extract(modRMByte) != X86TemplateContext.RMCase.SDWORD.value()) {
                        return -1;
                    }
                    for (int i = 0; i < template.parameters().length(); i++) {
                        switch (template.parameters().get(i).place()) {
                            case MOD_RM_REXB:
                            case MOD_RM:
                                return i;
                            default:
                                break;
                        }
                    }
                    break;
                }
                case SIB: {
                    if (template.sibBaseCase() == X86TemplateContext.SibBaseCase.GENERAL_REGISTER && X86Field.BASE.extract(sibByte) == 5) {
                        for (int i = 0; i < template.parameters().length(); i++) {
                            switch (template.parameters().get(i).place()) {
                                case SIB_BASE_REXB:
                                case SIB_BASE:
                                    return i;
                                default:
                                    break;
                            }
                        }
                    }
                    break;
                }
                default: {
                    break;
                }
            }
        }
        return -1;
    }

    private byte getSibByte(BufferedInputStream stream, Template_Type template, byte modRMByte) throws IOException {
        if (template.addressSizeAttribute() == WordWidth.BITS_16) {
            return 0;
        }
        if (template.hasSibByte()) {
            return endianness().readByte(stream);
        }
        if (template.hasModRMByte() && X86Field.RM.extract(modRMByte) == X86TemplateContext.RMCase.SIB.value() &&
                   X86Field.MOD.extract(modRMByte) != X86TemplateContext.ModCase.MOD_3.value()) {
            return endianness().readByte(stream);
        }
        return 0;
    }

    protected abstract Map<X86InstructionHeader, AppendableSequence<Template_Type>> headerToTemplates();

    private static int _serial;

    public DisassembledObject scanInstruction(BufferedInputStream stream, X86InstructionHeader header) throws IOException, AssemblyException {
        _serial++;
        Trace.line(4, "instruction: " + _serial);
        if (header._opcode1 != null) {
            boolean isFloatingPointEscape = false;
            if (X86Opcode.isFloatingPointEscape(header._opcode1)) {
                final int byte2 = stream.read();
                if (byte2 >= 0xC0) {
                    isFloatingPointEscape = true;
                    header._opcode2 = HexByte.VALUES.get(byte2);
                }
            }
            final Sequence<Template_Type> templates = headerToTemplates().get(header);
            if (templates != null) {
                for (Template_Type template : templates) {
                    Trace.line(3, template.toString());
                    stream.reset();
                    scanInstructionHeader(stream);
                    if (isFloatingPointEscape) {
                        stream.read();
                    }
                    try {
                        byte modRMByte = 0;
                        byte sibByte = 0;
                        int modVariantParameterIndex = -1;
                        IndexedSequence<Argument> arguments = null;
                        if (template.hasModRMByte()) {
                            modRMByte = endianness().readByte(stream);
                            sibByte = getSibByte(stream, template, modRMByte);
                            modVariantParameterIndex = getModVariantParameterIndex(template, modRMByte, sibByte);
                            if (modVariantParameterIndex >= 0) {
                                final Template_Type modVariantTemplate = X86Assembly.getModVariantTemplate(templates, template, template.parameters().get(modVariantParameterIndex).type());
                                arguments = scanArguments(stream, modVariantTemplate, header, modRMByte, sibByte);
                            }
                        }
                        if (arguments == null) {
                            arguments = scanArguments(stream, template, header, modRMByte, sibByte);
                        }
                        if (modVariantParameterIndex >= 0) {
                            final Immediate8Argument immediateArgument = (Immediate8Argument) arguments.get(modVariantParameterIndex);
                            if (immediateArgument.value() != 0) {
                                continue;
                            }

                            // Remove the mod variant argument
                            final Argument modVariantArgument = arguments.get(modVariantParameterIndex);
                            arguments = IndexedSequence.Static.filter(arguments, new Predicate<Argument>() {
                                public boolean evaluate(Argument argument) {
                                    return modVariantArgument != argument;
                                }
                            });
                        }
                        if (!Sequence.Static.containsIdentical(arguments, null)) {
                            byte[] bytes;
                            if (true) {
                                final Assembler assembler = createAssembler(_currentPosition);
                                assembly().assemble(assembler, template, arguments);
                                bytes = assembler.toByteArray();
                            } else { // TODO: does not work yet
                                final X86TemplateAssembler<Template_Type> templateAssembler = new X86TemplateAssembler<Template_Type>(template, addressWidth());
                                bytes = templateAssembler.assemble(arguments);
                            }
                            if (bytes != null) {
                                stream.reset();
                                if (Streams.startsWith(stream, bytes)) {
                                    final DisassembledInstruction_Type disassembledInstruction = createDisassembledInstruction(_currentPosition, bytes, template, arguments);
                                    _currentPosition += bytes.length;
                                    return disassembledInstruction;
                                }
                            }
                        }
                    } catch (IOException ioException) {
                        // this one did not work, so loop back up and try another template
                    }
                }
            }
        }
        if (header._instructionSelectionPrefix == X86Opcode.REPE || header._instructionSelectionPrefix == X86Opcode.REPNE) {
            final X86InstructionHeader prefixHeader = new X86InstructionHeader();
            prefixHeader._opcode1 = header._instructionSelectionPrefix;
            final Sequence<Template_Type> prefixTemplates = headerToTemplates().get(prefixHeader);
            final Template_Type template = prefixTemplates.first();
            final byte[] bytes = new byte[]{header._instructionSelectionPrefix.byteValue()};
            final DisassembledInstruction_Type disassembledInstruction = createDisassembledInstruction(_currentPosition, bytes, template, IndexedSequence.Static.empty(Argument.class));
            _currentPosition++;
            return disassembledInstruction;
        }
        if (INLINE_INVALID_INSTRUCTIONS_AS_BYTES) {
            stream.reset();
            final int size = 1;
            final byte[] data = new byte[size];
            Streams.readFully(stream, data);
            final InlineData inlineData = new InlineData(_currentPosition, data);
            final DisassembledData disassembledData = createDisassembledDataObjects(inlineData).iterator().next();
            _currentPosition += size;
            return disassembledData;
        }
        throw new AssemblyException("unknown instruction");
    }

    private static final int MORE_THAN_ANY_INSTRUCTION_LENGTH = 100;
    private static final boolean INLINE_INVALID_INSTRUCTIONS_AS_BYTES = true;

    @Override
    public IndexedSequence<DisassembledObject> scanOne0(BufferedInputStream stream) throws IOException, AssemblyException {
        final AppendableIndexedSequence<DisassembledObject> disassembledObjects = new ArrayListSequence<DisassembledObject>();
        stream.mark(MORE_THAN_ANY_INSTRUCTION_LENGTH);
        final X86InstructionHeader header = scanInstructionHeader(stream);
        if (header == null) {
            throw new AssemblyException("unknown instruction");
        }
        disassembledObjects.append(scanInstruction(stream, header));
        return disassembledObjects;
    }

    @Override
    public IndexedSequence<DisassembledObject> scan0(BufferedInputStream stream) throws IOException, AssemblyException {
        final SortedSet<Integer> knownGoodCodePositions = new TreeSet<Integer>();
        final AppendableIndexedSequence<DisassembledObject> result = new ArrayListSequence<DisassembledObject>();
        boolean processingKnownValidCode = true;

        while (true) {
            while (knownGoodCodePositions.size() > 0 && knownGoodCodePositions.first().intValue() < _currentPosition) {
                knownGoodCodePositions.remove(knownGoodCodePositions.first());
            }

            scanInlineData(stream, result);

            stream.mark(MORE_THAN_ANY_INSTRUCTION_LENGTH);

            final X86InstructionHeader header = scanInstructionHeader(stream);
            if (header == null) {
                return result;
            }
            final DisassembledObject disassembledObject = scanInstruction(stream, header);

            if (knownGoodCodePositions.size() > 0) {
                final int firstKnownGoodCodePosition = knownGoodCodePositions.first().intValue();
                final int startPosition = disassembledObject.startPosition();
                if (firstKnownGoodCodePosition > startPosition && firstKnownGoodCodePosition < disassembledObject.endPosition()) {
                    // there is a known valid code location in the middle of this instruction - assume that it is an invalid instruction
                    stream.reset();
                    final int size = firstKnownGoodCodePosition - startPosition;
                    final byte[] data = new byte[size];
                    Streams.readFully(stream, data);
                    final InlineData inlineData = new InlineData(startPosition, data);
                    _currentPosition += addDisassembledDataObjects(result, inlineData);
                    processingKnownValidCode = true;
                } else {
                    result.append(disassembledObject);
                    if (firstKnownGoodCodePosition == startPosition) {
                        processingKnownValidCode = true;
                    }
                }
            } else {
                if (processingKnownValidCode && disassembledObject instanceof DisassembledInstruction) {
                    final Class<DisassembledInstruction_Type> type = null;
                    final DisassembledInstruction_Type disassembledInstruction = StaticLoophole.cast(type, disassembledObject);
                    if (isRelativeJumpForward(disassembledInstruction)) {
                        int jumpOffset;
                        if (disassembledInstruction.arguments().first() instanceof Immediate32Argument) {
                            jumpOffset = ((Immediate32Argument) disassembledInstruction.arguments().first()).value();
                        } else {
                            assert disassembledInstruction.arguments().first() instanceof Immediate8Argument;
                            jumpOffset = ((Immediate8Argument) disassembledInstruction.arguments().first()).value();
                        }
                        final int targetPosition = disassembledInstruction.endPosition() + jumpOffset;
                        knownGoodCodePositions.add(targetPosition);
                        processingKnownValidCode = false;
                    }
                }
                result.append(disassembledObject);
            }
        }
    }

    private boolean isRelativeJumpForward(DisassembledInstruction instruction) {
        return instruction.template().internalName().equals("jmp") && // check if this is a jump instruction...
            instruction.arguments().length() == 1 && // that accepts one operand...
            ((instruction.arguments().first() instanceof Immediate32Argument && // which is a relative offset...
            ((Immediate32Argument) instruction.arguments().first()).value() >= 0) || // forward in the code stream
            (instruction.arguments().first() instanceof Immediate8Argument && // which is a relative offset...
            ((Immediate8Argument) instruction.arguments().first()).value() >= 0)); // forward in the code stream
    }

    protected abstract Template_Type createInlineDataTemplate(Object[] specification);
}

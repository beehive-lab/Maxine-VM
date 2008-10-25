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
package com.sun.max.asm.dis.risc;

import java.io.*;
import java.util.Arrays;

import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.risc.*;
import com.sun.max.asm.gen.risc.bitRange.*;
import com.sun.max.asm.gen.risc.field.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 *
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Dave Ungar
 * @author Adam Spitz
 */
public abstract class RiscDisassembler<Template_Type extends RiscTemplate, DisassembledInstruction_Type extends DisassembledInstruction<Template_Type>>
    extends Disassembler<Template_Type, DisassembledInstruction_Type> {

    protected RiscDisassembler(Assembly<Template_Type> assembly, WordWidth addressWidth, Endianness endianness, InlineDataDecoder inlineDataDecoder) {
        super(assembly, addressWidth, endianness, inlineDataDecoder);
    }

    @Override
    public RiscAssembly<Template_Type> assembly() {
        final Class<RiscAssembly<Template_Type>> type = null;
        return StaticLoophole.cast(type, super.assembly());
    }

    private static final boolean INLINE_INVALID_INSTRUCTIONS_AS_BYTES = true;

    /**
     * Extract the value for each operand of a template from an encoded instruction whose opcode
     * matches that of the template.
     *
     * @param instruction  the encoded instruction
     * @return the decoded arguments for each operand or null if at least one operand has
     *         an invalid value in the encoded instruction
     */
    private IndexedSequence<Argument> disassemble(int instruction, Template_Type template) {
        final AppendableIndexedSequence<Argument> arguments = new ArrayListSequence<Argument>();
        for (OperandField operandField : template.parameters()) {
            final Argument argument = operandField.disassemble(instruction);
            if (argument == null) {
                return null;
            }
            arguments.append(argument);
        }
        return arguments;
    }

    private boolean isLegalArgumentList(Template_Type template, IndexedSequence<Argument> arguments) {
        final Sequence<InstructionConstraint> constraints = template.instructionDescription().constraints();
        for (InstructionConstraint constraint : constraints) {
            if (!(constraint.check(template, arguments))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Sequence<DisassembledObject> scanOneInstruction(BufferedInputStream stream) throws IOException, AssemblyException {
        final int instruction = endianness().readInt(stream);
        final AppendableSequence<DisassembledObject> result = new LinkSequence<DisassembledObject>();
        final byte[] instructionBytes = endianness().toBytes(instruction);
        for (SpecificityGroup<Template_Type> specificityGroup : assembly().specificityGroups()) {
            for (OpcodeMaskGroup<Template_Type> opcodeMaskGroup : specificityGroup.opcodeMaskGroups()) {
                final int opcode = instruction & opcodeMaskGroup.mask();
                for (Template_Type template : opcodeMaskGroup.templatesFor(opcode)) {
                    // Skip synthetic instructions when preference is for raw instructions,
                    // and skip instructions with a different number of arguments than requested if so (i.e. when running the AssemblyTester):
                    if (template != null && template.isDisassemblable() && ((abstractionPreference() == AbstractionPreference.SYNTHETIC) || !template.instructionDescription().isSynthetic())) {
                        final IndexedSequence<Argument> arguments = disassemble(instruction, template);
                        if (arguments != null && (expectedNumberOfArguments() < 0 || arguments.length() == expectedNumberOfArguments())) {
                            if (isLegalArgumentList(template, arguments)) {
                                final Assembler assembler = createAssembler(_currentPosition);
                                try {
                                    assembly().assemble(assembler, template, arguments);
                                    final byte[] bytes = assembler.toByteArray();
                                    if (Arrays.equals(bytes, instructionBytes)) {
                                        final DisassembledInstruction_Type disassembledInstruction = createDisassembledInstruction(_currentPosition, bytes, template, arguments);
                                        result.append(disassembledInstruction);
                                    }
                                } catch (AssemblyException assemblyException) {
                                    ProgramWarning.message("could not assemble matching instruction: " + template);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (result.isEmpty()) {
            if (INLINE_INVALID_INSTRUCTIONS_AS_BYTES) {
                stream.reset();
                final InlineData inlineData = new InlineData(_currentPosition, instructionBytes);
                final DisassembledData disassembledData = createDisassembledDataObjects(inlineData).iterator().next();
                result.append(disassembledData);
            } else {
                throw new AssemblyException("instruction could not be disassembled: " + Bytes.toHexLiteral(endianness().toBytes(instruction)));
            }
        }
        _currentPosition += 4;
        return result;
    }

    @Override
    public IndexedSequence<DisassembledObject> scan(BufferedInputStream stream) throws IOException, AssemblyException {
        final AppendableIndexedSequence<DisassembledObject> result = new ArrayListSequence<DisassembledObject>();
        try {
            while (true) {
                if (inlineDataDecoder() != null) {
                    InlineData inlineData;
                    while ((inlineData = inlineDataDecoder().decode(_currentPosition, stream)) != null) {
                        final IterableWithLength<DisassembledData> dataObjects = createDisassembledDataObjects(inlineData);
                        for (DisassembledData dataObject : dataObjects) {
                            result.append(dataObject);
                        }
                        _currentPosition += inlineData.size();
                    }
                }

                final Sequence<DisassembledObject> disassembledObjects = scanOneInstruction(stream);
                boolean foundSyntheticDisassembledInstruction = false;
                if (abstractionPreference() == AbstractionPreference.SYNTHETIC) {
                    for (DisassembledObject disassembledObject : disassembledObjects) {
                        if (disassembledObject instanceof DisassembledInstruction) {
                            final Class<DisassembledInstruction_Type> type = null;
                            final DisassembledInstruction_Type disassembledInstruction = StaticLoophole.cast(type, disassembledObject);
                            if (disassembledInstruction.template().instructionDescription().isSynthetic()) {
                                result.append(disassembledInstruction);
                                foundSyntheticDisassembledInstruction = true;
                                break;
                            }
                        }
                    }
                }
                if (!foundSyntheticDisassembledInstruction) {
                    result.append(disassembledObjects.first());
                }
            }
        } catch (IOException ioException) {
            return result;
        }
    }

    protected abstract Template_Type createInlineDataTemplate(InstructionDescription instructionDescription);

    private final ImmediateOperandField[] _byteFields = {createByteField(0), createByteField(1), createByteField(2), createByteField(3)};

    private ImmediateOperandField createByteField(int index) {
        if (assembly().bitRangeEndianness() == BitRangeOrder.ASCENDING) {
            final int firstBit = index * Bytes.WIDTH;
            final int lastBit = firstBit + 7;
            return ImmediateOperandField.createAscending(firstBit, lastBit);
        }
        final int lastBit = index * Bytes.WIDTH;
        final int firstBit = lastBit + 7;
        return ImmediateOperandField.createDescending(firstBit, lastBit);
    }

    @Override
    protected IterableWithLength<DisassembledData> createDisassembledDataObjects(InlineData inlineData) {
        final InlineDataDescriptor descriptor = inlineData.descriptor();
        switch (descriptor.tag()) {
            case BYTE_DATA: {
                throw Problem.unimplemented();
            }
            case JUMP_TABLE32: {
                throw Problem.unimplemented();
            }
            case LOOKUP_TABLE32: {
                throw Problem.unimplemented();
            }
        }
        throw ProgramError.unknownCase(descriptor.tag().toString());
    }

}

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
package com.sun.max.vm.interpret.dt.amd64;

import java.io.*;

import com.sun.max.asm.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.asm.x86.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.asm.amd64.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.interpret.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.template.*;

/**
 * Runtime generates an AMD64DtInterpreter from the given templates.
 *
 * @author Simon Wilkinson
 */
public class AMD64DtInterpreterGenerator {

    private static final TargetABI<AMD64GeneralRegister64, AMD64XMMRegister> targetABI;

    private static final int RIP_MOV_SIZE = 7;

    private final AMD64DtInterpreterTemplateSet templateSet;

    static {
        final Class<TargetABI<AMD64GeneralRegister64, AMD64XMMRegister>> type = null;
        targetABI = StaticLoophole.cast(type, VMConfiguration.target().targetABIsScheme().interpreterABI());
    }

    AMD64DtInterpreterGenerator(AMD64DtInterpreterTemplateSet templateSet) {
        this.templateSet = templateSet;
    }

    private int baseFrameSize() {
        final int numberOfSlots = 1 + templateSet.maxFrameSlots();
        final int unalignedSize = numberOfSlots * JavaStackFrameLayout.STACK_SLOT_SIZE;
        return unalignedSize;
    }

    private int maxCompiledTemplateSize() {
        int maxCodeSize = 0;
        for (CompiledBytecodeTemplate template : templateSet.templates()) {
            final int codeSize = template.targetMethod.codeLength();
            if (codeSize > maxCodeSize) {
                maxCodeSize = codeSize;
            }
        }
        return maxCodeSize;
    }

    private int templateDispatchSizeEstimate() {
        // Will never be longer than this. (Assuming that the epilogue stub is smaller than the dispatch stub).
        return generateBytecodeIndexIncrementStub(Bytecode.JSR_W).length + generateDispatchStub((byte) 1).length;
    }

    public byte logTemplateSlotSize() {
        final int maxCodeSize = maxCompiledTemplateSize() + templateDispatchSizeEstimate();
        return (byte) (Integer.SIZE - Integer.numberOfLeadingZeros(maxCodeSize));
    }

    private byte[] generateDispatchStub(byte logTemplateSlotSize) {

        final AMD64Assembler asm = new AMD64Assembler();

        final int firstBytecodeArrayElementOffset = MaxineVM.target().configuration().layoutScheme().byteArrayLayout.getElementOffsetFromOrigin(0).toInt();

        // Load up the scratch reg with the next bytecode
        asm.movzxb(targetABI.scratchRegister(), firstBytecodeArrayElementOffset,
                        AMD64DtInterpreterABI.bytecodeArrayPointer().base(),
                        AMD64DtInterpreterABI.bytecodeIndex().index(), Scale.SCALE_1);

        // Work out the offset of the bytecode's template from the start of the template array
        asm.shlq(targetABI.scratchRegister(), logTemplateSlotSize);

        // Add the address of our template array to get the jump target
        asm.add(targetABI.scratchRegister(), AMD64DtInterpreterABI.firstTemplatePointer());

        // Jump
        asm.jmp(targetABI.scratchRegister());

        try {
            return asm.toByteArray();
        } catch (AssemblyException e) {
            ProgramError.unexpected(e);
            return null;
        }
    }

    private byte[] generateBytecodeIndexIncrementStub(Bytecode bytecode) {

        final AMD64Assembler asm = new AMD64Assembler();

        // Simply add the opcode length to our bytecode array index
        asm.addq(AMD64DtInterpreterABI.bytecodeIndex(), BytecodeSizes.lookup(bytecode));

        try {
            return asm.toByteArray();
        } catch (AssemblyException e) {
            ProgramError.unexpected(e);
            return null;
        }
    }

    private byte[] generatePrologue(int prologueToFirstLiteralOffset, byte[] dispatchStub) {

        final AMD64Assembler asm = new AMD64Assembler();

        // Pushes the caller's FP, and moves the SP to the end of the spill slots
        asm.enter((short) (baseFrameSize() - Word.size()), (byte) 0);

        // The caller has told us the size of the non-parameter locals
        asm.mov(targetABI.framePointer(), targetABI.stackPointer());
        asm.sub(targetABI.stackPointer(), AMD64DtInterpreterABI.nonParameterlocalsSize());

        // Prepare for the first dispatch
        asm.rip_mov(AMD64DtInterpreterABI.firstTemplatePointer(), -(RIP_MOV_SIZE + asm.currentPosition()) + prologueToFirstLiteralOffset);
        asm.mov(AMD64DtInterpreterABI.bytecodeIndex(), 0);

        final ByteArrayOutputStream prologueCode = new ByteArrayOutputStream();
        try {
            prologueCode.write(asm.toByteArray());
            prologueCode.write(dispatchStub);
        } catch (AssemblyException e) {
            ProgramError.unexpected(e);
        } catch (IOException e) {
            ProgramError.unexpected(e);
        }
        return prologueCode.toByteArray();
    }

    private byte[] generateEpilogue() {

        final AMD64Assembler asm = new AMD64Assembler();

        asm.addq(targetABI.framePointer(), baseFrameSize() - Word.size());
        asm.leave();
        asm.ret();

        try {
            return asm.toByteArray();
        } catch (AssemblyException e) {
            ProgramError.unexpected(e);
            return null;
        }
    }

    private void fixLiterals(AMD64DtInterpreter interpreter) {
        interpreter.start().asPointer().setWord(interpreter.templatesStart());
    }

    private byte[] generateLiterals() {
        // Just create one literal, which we will fill in with the address of the first template.
        // We pad the literals with a word of NOPs so that the inspector doesn't get too confused as to where
        // the first instruction of the prologue starts.
        final byte[] literals = new byte[Word.size() * 2];
        for (int i = Word.size(); i < literals.length; i++) {
            literals[i] = (byte) 0x90;
        }
        return literals;
    }

    public AMD64DtInterpreter generate() {

        // How big do we want our slots in the array of templates?
        final byte logTemplateSlotSize = logTemplateSlotSize();
        final int templateSlotSize = 1 << logTemplateSlotSize;

        // The dispatch stub is common to all templates and the prologue
        final byte[] dispatchStub = generateDispatchStub(logTemplateSlotSize);

        // The epilogue is common to all 'return' templates
        final byte[] epilogueStub = generateEpilogue();

        // Generate the literals and prologue
        final byte[] literals = generateLiterals();
        final byte[] prologueCode = generatePrologue(-literals.length, dispatchStub);

        // Generate the template array
        final TemplateArrayCodeBuffer templateArrayCodeBuffer = new TemplateArrayCodeBuffer(templateSlotSize);
        for (CompiledBytecodeTemplate template : templateSet.templates()) {
            final Bytecode bytecode = template.bytecode;
            templateArrayCodeBuffer.setPositionToSlotStart(bytecode);
            templateArrayCodeBuffer.emit(template.targetMethod.code());
            if (bytecode.is(Bytecode.Flags.RETURN_)) {
                templateArrayCodeBuffer.emit(epilogueStub);
            } else {
                templateArrayCodeBuffer.emit(generateBytecodeIndexIncrementStub(bytecode));
                templateArrayCodeBuffer.emit(dispatchStub);
            }
        }

        // Concatenate the literals, prologue and templates
        final ByteArrayOutputStream interpreterCode = new ByteArrayOutputStream();
        try {
            interpreterCode.write(literals);
            interpreterCode.write(prologueCode);
            interpreterCode.write(templateArrayCodeBuffer.toByteArray());
        } catch (IOException e) {
            ProgramError.unexpected(e);
        }

        final AMD64DtInterpreter interpreter = new AMD64DtInterpreter(interpreterCode.toByteArray(),
                                                                      literals.length,
                                                                      literals.length + prologueCode.length,
                                                                      baseFrameSize());
        fixLiterals(interpreter);
        return interpreter;
    }

    private static class TemplateArrayCodeBuffer {
        private final byte[] templateArray;
        private final int slotSize;
        private int position = 0;

        public TemplateArrayCodeBuffer(int templateSlotSize) {
            slotSize = templateSlotSize;
            templateArray = new byte[slotSize * Bytecode.BREAKPOINT.ordinal()];
            for (int i = 0; i < templateArray.length; i++) {
                templateArray[i] = (byte) 0x90; // Fill with NOPS so the inspector does not get too confused
            }
        }
        public byte[] toByteArray() {
            return templateArray;
        }
        public void setPositionToSlotStart(Bytecode bytecode) {
            position = slotSize * bytecode.ordinal();
        }
        public void emit(byte[] code) {
            System.arraycopy(code, 0, templateArray, position, code.length);
            position += code.length;
        }
    }

}

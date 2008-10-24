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
package com.sun.max.vm.jit.sparc;

import static com.sun.max.vm.bytecode.BranchCondition.*;
import static com.sun.max.vm.stack.JavaStackFrameLayout.*;
import static com.sun.max.vm.stack.JitStackFrameLayout.*;

import java.io.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.asm.Assembler.*;
import com.sun.max.asm.sparc.*;
import com.sun.max.asm.sparc.complete.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.b.c.d.e.sparc.target.*;
import com.sun.max.vm.compiler.eir.sparc.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.sparc.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.jit.Stop.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.runtime.VMRegister.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.sparc.*;
import com.sun.max.vm.template.*;
import com.sun.max.vm.type.*;

/**
 *
 * SPARC-specific part of the bytecode to target translator.
 * This implementation doesn't use register windows for stack frames, i.e., a sequence of jited-code to jited-code uses the
 * same register window. Therefore, JITed code needs to save the caller context (its return address and frame pointer).
 * This context saving is done in the JITed code prologue.
 * When calling to optimized code or runtime code, the callee pushes a register window, and no context need to be saved.
 *
 * @author Laurent Daynes
 */
public class BytecodeToSPARCTargetTranslator extends BytecodeToTargetTranslator {

    /**
     * Canonicalized Target ABI.
     */
    private static final TargetABI<GPR, FPR> _targetABI;

    private static final GPR _cpuFramePointer;

    /**
     * Bytes for the instruction performing safepoint. Used to fill delay slot of backward branches.
     */
    private static final byte[] _safepointTemplate;

    /**
     * Bytes for the nop instruction. Useful to efficiently fill unused delay slot.
     */
    private static final byte[] _nopTemplate;

    private static final BranchConditionMap<byte[]> _branchTemplates = new BranchConditionMap<byte[]>();
    private static final int[] _branchTemplateInstruction = new int[BranchCondition.values().length];

    /**
     * Templates for table switch bytecode.
     * There are two templates. The second one differs from the first one in that it has an extra instruction to adjust the tag register.
     * The second must be used when the low match isn't zero.
     */
    private static final byte[][]  _tableSwitchTemplates = new byte[2][];
    /**
     * Offset to the instruction within the table switch template that branch to the default target.
     */
    private static final int _offsetToTableSwitchBranchToDefaultTarget;

    /**
     * Template of the instruction that branch to the default target. Used to fix the 19-bits displacement of the branch instruction.
     */
    private static final int _tableSwitchBranchToDefaultTargetTemplate;

    /**
     * Template to the lookup switch bytecode.
     */
    private static final byte[] _lookupSwitchTemplate;
    /**
     * Offset to the instruction within the lookup switch template that branch to the default target.
     */
    private static final int _offsetToLookupSwitchBranchToDefaultTarget;

    /**
     * Template of the instruction that branch to the default target. Used to fix the 19-bits displacement of the branch instruction.
     */
    private static final int _lookupSwitchBranchToDefaultTargetTemplate;

    public static final int RET_TEMPLATE;

    @Override
    public TargetABI targetABI() {
        return _targetABI;
    }

    private final SPARCAssembler _asm;

    private final SPARCAdapterFrameGenerator _adapterFrameGenerator;

    /**
     * Offset in bytes, from the beginning of the emitted code,  to the instruction in the prologue that set the literal base.
     * This must be added to the offset to literal passed in parameters to  {@link loadTemplateArgumentRelativeToInstructionPointer}.
     */
    private int _offsetToSetLiteralBaseInstruction;

    public BytecodeToSPARCTargetTranslator(ClassMethodActor classMethodActor, CodeBuffer codeBuffer, TemplateTable templateTable, SPARCEirABI optimizingCompilerAbi, boolean trace) {
        super(classMethodActor, codeBuffer, templateTable, new SPARCJitStackFrameLayout(classMethodActor, templateTable.maxFrameSlots()), trace);
        _asm = optimizingCompilerAbi.createAssembler();
        SPARCAdapterFrameGenerator adapterFrameGenerator = null;
        if (optimizingCompilerAbi != null) {
            adapterFrameGenerator = SPARCAdapterFrameGenerator.optimizedToJitCompilerAdapterFrameGenerator(classMethodActor, optimizingCompilerAbi);
        }
        _adapterFrameGenerator = adapterFrameGenerator;
    }

    @Override
    public int adapterReturnPosition() {
        try {
            if (_adapterFrameGenerator.adapterReturnPoint().state() == Label.State.BOUND) {
                return _adapterFrameGenerator.adapterReturnPoint().position();
            }
            return -1;
        } catch (AssemblyException assemblyException) {
            throw ProgramError.unexpected();
        }
    }

    @Override
    protected void assignDoubleTemplateArgument(int parameterIndex, double argument) {
        try {
            final GPR scratchRegister = GPR.G1;
            final GPR scratchRegister2 = GPR.O7;
            final DFPR register = (DFPR) _targetABI.floatingPointParameterRegisters().get(parameterIndex);
            _asm.reset();
            if (argument == 0) {
                _asm.clr(scratchRegister);
            } else {
                _asm.setx(UnsafeLoophole.doubleToLong(argument), scratchRegister2, scratchRegister);
            }
            _asm.stx(scratchRegister, _cpuFramePointer, SPARCJitStackFrameLayout.OFFSET_TO_FLOATING_POINT_TEMP_AREA);
            _asm.ldd(_cpuFramePointer, SPARCJitStackFrameLayout.OFFSET_TO_FLOATING_POINT_TEMP_AREA, register);
            _codeBuffer.emitCodeFrom(_asm);
        } catch (AssemblyException e) {
            ProgramError.unexpected();
        }
    }

    @Override
    protected void assignFloatTemplateArgument(int parameterIndex, float argument) {
        try {
            final GPR scratchRegister = _targetABI.scratchRegister();
            final SFPR register = (SFPR) _targetABI.floatingPointParameterRegisters().get(parameterIndex);
            _asm.reset();
            if (argument == 0) {
                _asm.clr(scratchRegister);
            } else {
                _asm.setsw(UnsafeLoophole.floatToInt(argument), scratchRegister);
            }
            _asm.stw(scratchRegister, _cpuFramePointer, SPARCJitStackFrameLayout.OFFSET_TO_FLOATING_POINT_TEMP_AREA);
            _asm.ld(_cpuFramePointer, SPARCJitStackFrameLayout.OFFSET_TO_FLOATING_POINT_TEMP_AREA, register);
            _codeBuffer.emitCodeFrom(_asm);
        } catch (AssemblyException e) {
            ProgramError.unexpected();
        }
    }

    @Override
    protected void assignIntTemplateArgument(int parameterIndex, int argument) {
        try {
            final GPR register = _targetABI.integerIncomingParameterRegisters().get(parameterIndex);
            _asm.reset();
            if (argument == 0) {
                _asm.clr(register);
            } else {
                _asm.setsw(argument, register);
            }
            _codeBuffer.emitCodeFrom(_asm);
        } catch (AssemblyException e) {
            ProgramError.unexpected();
        }
    }

    @Override
    protected void assignLongTemplateArgument(int parameterIndex, long argument) {
        try {
            final GPR register = _targetABI.integerIncomingParameterRegisters().get(parameterIndex);
            _asm.reset();
            _asm.setx(argument, GPR.O7, register);
            _codeBuffer.emitCodeFrom(_asm);
        } catch (AssemblyException e) {
            ProgramError.unexpected();
        }
    }

    /*
     * Constant amount of bytes to add to a call-save-area pointer to obtain the biased stack pointer of the caller.
     * This is used below to re-compute the biased stack pointer of a JIT caller.
     */
    private static final int CALL_SAVE_AREA_OFFSET_TO_STACK = SPARCJitStackFrameLayout.CALL_SAVE_AREA_SIZE - SPARCStackFrameLayout.offsetToFirstFreeSlotFromStackPointer();

    @Override
    protected void emitReturn() {
        final GPR framePointer = _targetABI.framePointer();
        final GPR stackPointer = _targetABI.stackPointer();
        final GPR callSaveAreaPointer = GPR.O5;
        final int stackAmountInBytes = _jitStackFrameLayout.sizeOfParameters() + CALL_SAVE_AREA_OFFSET_TO_STACK;
        assert SPARCAssembler.isSimm13(stackAmountInBytes) : "must be imm13";

        final int offsetToCallSaveArea = VMConfiguration.target().targetABIsScheme().jitABI().alignFrameSize(SPARCJitStackFrameLayout.CALL_SAVE_AREA_SIZE +
                        _jitStackFrameLayout.sizeOfTemplateSlots()) - SPARCJitStackFrameLayout.CALL_SAVE_AREA_SIZE;

        _asm.reset();
        _asm.add(framePointer, offsetToCallSaveArea, callSaveAreaPointer);
        _asm.ldx(callSaveAreaPointer, STACK_SLOT_SIZE, GPR.O7);
        _asm.ldx(callSaveAreaPointer, GPR.G0, framePointer);
        // Reload literal base register (its saving area is just below the frame pointer).
        _asm.ldx(framePointer, -STACK_SLOT_SIZE, _targetABI.literalBaseRegister());

        _asm.jmpl(GPR.O7, 8, GPR.G0);
        _asm.add(callSaveAreaPointer, stackAmountInBytes, stackPointer);
        _codeBuffer.emitCodeFrom(_asm);
    }


    private static final int DISP19_MASK = 0x7ffff;
    /**
     * Efficient patching of the target of branch instruction.
     * @param instructionOffset offset to the branch instruction in the code buffer
     * @param byteOffsetToBranchTarget target of the branch as a byte displacement from the branch instruction.
     * @param branchTemplate the 4 bytes encoding the branch instruction to patch.
     */
    private void fixBranchLabel(int instructionOffset, int byteOffsetToBranchTarget, int branchTemplate) {
        assert SPARCAssembler.isSimm19(byteOffsetToBranchTarget >> 2);
       // Only need to fix the 3 bytes of the instruction that the label may span.
        try {
            final int disp19 = (byteOffsetToBranchTarget >> 2) & DISP19_MASK;
            final int instruction = branchTemplate | disp19;
            _codeBuffer.fix(instructionOffset + 3, (byte) (instruction & 0xff));
            _codeBuffer.fix(instructionOffset + 2, (byte) ((instruction >> 8) & 0xff));
            _codeBuffer.fix(instructionOffset + 1, (byte) ((instruction  >> 16) & 0xff));
        } catch (AssemblyException assemblyException) {
            throw new TranslationException(assemblyException);
        }
    }

    @Override
    protected void emitBranch(BranchCondition branchCondition, int fromBytecodePosition, int toBytecodePosition) {
        final int branchPosition = _codeBuffer.currentPosition();

        _codeBuffer.emit(_branchTemplates.get(branchCondition));
        if (fromBytecodePosition < toBytecodePosition) {
            // Fill delay slot with a nop.
            _codeBuffer.emit(_nopTemplate);
            // Forward branch. Record it.
            addForwardBranch(new ForwardBranch(branchCondition, branchPosition, toBytecodePosition));
        } else {
            final int branchDestination = bytecodeToTargetCodePosition(toBytecodePosition);
            final int byteOffsetToBranchTarget = branchDestination - branchPosition;
            fixBranchLabel(branchPosition, byteOffsetToBranchTarget, _branchTemplateInstruction[branchCondition.ordinal()]);
            // The safepoint instruction is emitted in the delay slot of the branch. It will be executed only if the branch is taken.
            _codeBuffer.emit(_safepointTemplate);
            emitSafepoint(new BackwardBranchBytecodeSafepoint(branchPosition + 4));
        }
    }

    @Override
    protected void fixForwardBranch(ForwardBranch forwardBranch) {
        final int toBranchTarget = bytecodeToTargetCodePosition(forwardBranch._targetBytecodePosition);
        final int offsetToBranchTarget = toBranchTarget - forwardBranch._targetCodePosition;
        assert offsetToBranchTarget >= 0;
        fixBranchLabel(forwardBranch._targetCodePosition, offsetToBranchTarget, _branchTemplateInstruction[forwardBranch._condition.ordinal()]);
    }

    @Override
    protected void emitLookupSwitch(int opcodePosition, int defaultTargetOffset, int numberOfCases) {
        // FIXME: a lot of this can be put in common with AMD64.
        // If we were to parameterized the template for lookup switch, the only difference between the two would be the
        // one liner that assemble the setting of the number of cases register.
        try {
            final int defaultTargetBytecodePosition = opcodePosition + defaultTargetOffset;
            if (numberOfCases == 0) {
                // Skip completely if default target is next instruction.
                // lookup switch are aligned on 4 bytes and have a minimum of 12 bytes.
                final int nextBytecodePosition = (opcodePosition & 3) + 12;
                if (defaultTargetBytecodePosition > nextBytecodePosition) {
                    emitBranch(NONE, opcodePosition, defaultTargetBytecodePosition);
                }
                return;
            }
            final int sizeOfInlinedTable = numberOfCases * 2 * Ints.SIZE;
            final int offsetToLastKey = sizeOfInlinedTable -  (2 * Ints.SIZE);
            _asm.reset();
            _asm.setsw(offsetToLastKey, GPR.O1);
            _codeBuffer.emitCodeFrom(_asm);
            _codeBuffer.emit(_lookupSwitchTemplate);

            _inlineDataRecorder.add(new InlineDataDescriptor.ByteData(_codeBuffer.currentPosition(), sizeOfInlinedTable));
            _codeBuffer.reserve(sizeOfInlinedTable);

            final int[] matches = new int[numberOfCases];
            final int[] targetBytecodePositions = new int[numberOfCases];
            final BytecodeScanner scanner = getBytecodeScanner();
            for (int i = 0; i != numberOfCases; ++i) {
                matches[i] = scanner.readSwitchCase();
                targetBytecodePositions[i] = scanner.readSwitchOffset() + opcodePosition;
            }
            addSwitch(new LookupSwitch(opcodePosition, defaultTargetBytecodePosition, matches, targetBytecodePositions));
        } catch (AssemblyException assemblyException) {
            throw new TranslationException(assemblyException);
        }
    }

    // FIXME: put this in common with AMD64 (pull up one level)
    protected void fixMatchOffsetPairTable(LookupSwitch lookupSwitch, int matchOffsetPairTableAddress) {
        try {
            _asm.reset();
            final Directives directives = _asm.directives();
            // Initialize the match-offset pair table: matching values are at even positions, offset to target at odd positions
            for (int i = 0; i < lookupSwitch._matches.length; i++) {
                directives.inlineInt(lookupSwitch._matches[i]);
                final int targetBytecodePosition = lookupSwitch._targetBytecodePositions[i];
                final int targetTargetCodeOffset = bytecodeToTargetCodePosition(targetBytecodePosition) - matchOffsetPairTableAddress;
                directives.inlineInt(targetTargetCodeOffset);
            }
            _codeBuffer.fix(matchOffsetPairTableAddress, _asm);
        } catch (AssemblyException assemblyException) {
            throw new TranslationException(assemblyException);
        }
    }

    @Override
    protected void fixLookupSwitch(LookupSwitch lookupSwitch) {
        // The lookupSwitchTemplate doesn't include the instruction(s) that set the offset to the last key.
        // Count the additional instructions (one or two, depending on the size of the inline table) to get the correct offset to
        // first instruction of the lookup template.

        final int numInstructionsBeforeTemplate = SPARCAssembler.isSimm13(lookupSwitch._matches.length) ? 1 : 2;
        final int templateTargetCodePosition = bytecodeToTargetCodePosition(lookupSwitch._opcodePosition) + (numInstructionsBeforeTemplate * SPARCStackFrameLayout.SPARC_INSTRUCTION_WIDTH);

        // The default target is expressed as an offset from the lookup switch bytecode.
        // Compute the default target's position relative to the beginning of the method.
        final int defaultTargetTargetCodePosition = bytecodeToTargetCodePosition(lookupSwitch._defaultTargetBytecodePosition);

        // Fix the branch target in the template.
        final int offsetToBranchInstruction = templateTargetCodePosition + _offsetToLookupSwitchBranchToDefaultTarget;

        final int offsetToDefaultTarget = defaultTargetTargetCodePosition - offsetToBranchInstruction;
        fixBranchLabel(offsetToBranchInstruction, offsetToDefaultTarget, _lookupSwitchBranchToDefaultTargetTemplate);

        final int matchOffsetPairTableAddress =  templateTargetCodePosition  + _lookupSwitchTemplate.length;
        fixMatchOffsetPairTable(lookupSwitch, matchOffsetPairTableAddress);
    }

    /**
     * Offset to the top of the operand stack, when the top is an integer value.
     */
    private static final int ITOS_OFFSET = SPARCStackFrameLayout.offsetToFirstFreeSlotFromStackPointer() + JitStackFrameLayout.offsetInStackSlot(Kind.INT);

    @PROTOTYPE_ONLY
    private static byte[] buildLookupSwitchTemplate(SPARCAssembler asm, Label branchToDefaultTarget) {
        final GPR keyRegister = GPR.O0;
        final GPR indexRegister = GPR.O1;  // initialized to index of the last match.
        final GPR matchRegister = GPR.O2;
        final GPR offsetRegister = matchRegister;
        final GPR tableRegister = _targetABI.scratchRegister();
        final Label matchOffsetPairsTable = new Label();
        final Label here = new Label();
        final Label loopBegin = new Label();
        final Label found = new Label();

        asm.reset();

        asm.bindLabel(here);
        asm.rd(StateRegister.PC, tableRegister);
        // Pop the key from the top of the stack
        asm.ldsw(_targetABI.stackPointer(), ITOS_OFFSET, keyRegister);
        asm.addcc(tableRegister, here, matchOffsetPairsTable, tableRegister);

        asm.ldsw(tableRegister, indexRegister, matchRegister);
        asm.bindLabel(loopBegin);
        asm.cmp(keyRegister, matchRegister);
        asm.be(AnnulBit.A, found);
        asm.inc(Ints.SIZE, indexRegister);
        asm.dec(2 * Ints.SIZE, indexRegister);
        asm.brgez(AnnulBit.A, BranchPredictionBit.PT, indexRegister, loopBegin);
        asm.ldsw(tableRegister, indexRegister, matchRegister);

        asm.bindLabel(branchToDefaultTarget);
        asm.ba(AnnulBit.NO_A, BranchPredictionBit.PT, ICCOperand.ICC, 0);
        // pop top of stack in delay slot
        asm.add(_targetABI.stackPointer(),  JIT_SLOT_SIZE, _targetABI.stackPointer());

        asm.bindLabel(found);
        asm.ldsw(tableRegister, indexRegister, offsetRegister);
        asm.jmp(tableRegister, offsetRegister);
        // pop top of stack in delay slot
        asm.add(_targetABI.stackPointer(),  JIT_SLOT_SIZE, _targetABI.stackPointer());

        asm.bindLabel(matchOffsetPairsTable);

        return toByteArrayAndReset(asm);
    }


    @Override
    protected void emitTableSwitch(int lowMatch, int highMatch, int opcodePosition, int defaultTargetOffset, int numberOfCases) {
        try {
            int templateIndex = 0;
            _asm.reset();
            _asm.setsw(numberOfCases, GPR.O1);
            if (lowMatch != 0) {
                templateIndex = 1;
                _asm.setsw(lowMatch, GPR.O2);
            }
            final int templatePrefixSize = _asm.currentPosition();
            _codeBuffer.emitCodeFrom(_asm);
            _codeBuffer.emit(_tableSwitchTemplates[templateIndex]);

            final InlineDataDescriptor.JumpTable32 jumpTable32 = new InlineDataDescriptor.JumpTable32(_codeBuffer.currentPosition(), lowMatch, highMatch);
            _inlineDataRecorder.add(jumpTable32);
            assert jumpTable32.size() == numberOfCases * WordWidth.BITS_32.numberOfBytes();
            _codeBuffer.reserve(jumpTable32.size());

            // Remember the location of the tableSwitch bytecode and the area in the code buffer where the targets will be written.
            final BytecodeScanner scanner = getBytecodeScanner();
            final int[] targetBytecodePositions = new int[numberOfCases];
            for (int i = 0; i != numberOfCases; ++i) {
                targetBytecodePositions[i] = scanner.readSwitchOffset() + opcodePosition;
            }
            final int defaultTargetBytecodePosition = opcodePosition + defaultTargetOffset;
            addSwitch(new TableSwitch(opcodePosition, templateIndex, defaultTargetBytecodePosition, targetBytecodePositions, templatePrefixSize));
        } catch (AssemblyException assemblyException) {
            throw new TranslationException(assemblyException);
        }
    }

    @Override
    protected void fixTableSwitch(TableSwitch tableSwitch) {
        try {
            final int templateTargetCodePosition = bytecodeToTargetCodePosition(tableSwitch._opcodePosition) + tableSwitch.templatePrefixSize();
            // The default target is expressed as an offset from the tableswitch bytecode. Compute the default target's position relative to the beginning of the method.
            final int defaultTargetTargetCodePosition = bytecodeToTargetCodePosition(tableSwitch._defaultTargetBytecodePosition);
            // Fix the branch target in the template.
            final int offsetToBranchInstruction = templateTargetCodePosition + _offsetToTableSwitchBranchToDefaultTarget +
                tableSwitch._templateIndex * SPARCStackFrameLayout.SPARC_INSTRUCTION_WIDTH;

            final int offsetToDefaultTarget = defaultTargetTargetCodePosition - offsetToBranchInstruction;
            fixBranchLabel(offsetToBranchInstruction, offsetToDefaultTarget, _tableSwitchBranchToDefaultTargetTemplate);

            // We generate a jump table using the inlining support of the assembler. The resulting table is then used to fix the
            // reserved space in the code buffer. The jump table comprises only offsets relative to the table itself.
            // This avoids having to deal with relocation.
            _asm.reset();
            final Directives directives = _asm.directives();
            final int jumpTableAddress =  templateTargetCodePosition + _tableSwitchTemplates[tableSwitch._templateIndex].length;
            for (int targetBytecodePosition : tableSwitch._targetBytecodePositions) {
                final int targetTargetCodeOffset = bytecodeToTargetCodePosition(targetBytecodePosition) - jumpTableAddress;
                directives.inlineInt(targetTargetCodeOffset);
            }
            _codeBuffer.fix(jumpTableAddress, _asm);
        } catch (AssemblyException assemblyException) {
            throw new TranslationException(assemblyException);
        }
    }


    @PROTOTYPE_ONLY
    private static byte[] buildTableSwitchTemplate(SPARCAssembler asm, Label branchToDefaultTarget, boolean adjustTag) {
        asm.reset();
        final GPR scratchRegister = _targetABI.scratchRegister();
        final GPR tableRegister = scratchRegister;
        final GPR targetAddressRegister = tableRegister;
        final Label jumpTable = new Label();
        final Label here = new Label();
        final int defaultTargetOffset = 0;

        final GPR tagRegister = GPR.O0;
        final GPR indexRegister = tagRegister;
        final GPR numElementsRegister = GPR.O1;

        asm.ldsw(_targetABI.stackPointer(), ITOS_OFFSET, tagRegister);
        if (adjustTag) {
            final GPR minMatchValue = GPR.O2;
            asm.sub(tagRegister, minMatchValue, tagRegister);
        }
        asm.cmp(tagRegister, numElementsRegister);
        asm.bindLabel(here);
        asm.rd(StateRegister.PC, tableRegister);
        asm.bindLabel(branchToDefaultTarget);
        asm.bcc(AnnulBit.NO_A, defaultTargetOffset);
        // pop top of the operand stack
        asm.add(_targetABI.stackPointer(),  JIT_SLOT_SIZE, _targetABI.stackPointer());
        asm.addcc(tableRegister, here, jumpTable, tableRegister);
        asm.sll(tagRegister, 2, indexRegister);
        asm.ldsw(tableRegister, indexRegister, GPR.O7);
        asm.jmp(targetAddressRegister, GPR.O7);
        asm.nop(); // delay slot
        asm.bindLabel(jumpTable);
        return toByteArrayAndReset(asm);
    }


    @Override
    protected  int computeReferenceLiteralOffset(int numReferenceLiteral) {
        // Remember: in the target bundle, the reference literal cell is directly adjacent to the code cell.
        return numReferenceLiteral * Word.size() + _offsetToSetLiteralBaseInstruction;
    }

    @Override
    protected void loadTemplateArgumentRelativeToInstructionPointer(Kind kind, int parameterIndex, int offsetFromLiteralBase) {
        final GPR literalBaseRegister = _targetABI.literalBaseRegister();
        _asm.reset();
        switch (kind.asEnum()) {
            case LONG:
            case WORD:
            case REFERENCE:
                final GPR destinationRegister = _targetABI.integerIncomingParameterRegisters().get(parameterIndex);
                if (SPARCAssembler.isSimm13(offsetFromLiteralBase)) {
                    _asm.ldx(literalBaseRegister, offsetFromLiteralBase, destinationRegister);
                } else {
                    try {
                        _asm.setsw(offsetFromLiteralBase, GPR.O7);
                        _asm.ldx(literalBaseRegister, GPR.O7, destinationRegister);
                    } catch (AssemblyException e) {
                        ProgramError.unexpected();
                    }
                }
                _codeBuffer.emitCodeFrom(_asm);

                break;
            default: {
                Problem.unimplemented();
            }
        }
    }

    @Override
    protected int registerReferenceMapSize() {
        return SPARCTargetMethod.Static.registerReferenceMapSize();
    }

    /**
     * Return the size (in number of bytes) of the sequence of instructions in the prologue that sets up the new frame .
     * @param targetMethod
     * @return
     */
    public static int frameBuilderSize(SPARCJitTargetMethod targetMethod) {
        final boolean largeFrame = !SPARCAssembler.isSimm13(targetMethod.frameSize());
        final JitStackFrameLayout stackFrameLayout = targetMethod.stackFrameLayout();
        final int offsetToCallSaveArea = SPARCStackFrameLayout.minStackFrameSize() + stackFrameLayout.sizeOfTemplateSlots() + stackFrameLayout.sizeOfNonParameterLocals() + JIT_SLOT_SIZE;
        final boolean largeRipOffset =  !SPARCAssembler.isSimm13(offsetToCallSaveArea + STACK_SLOT_SIZE);

        int numInstructions = 4;
        if (largeFrame) {
            numInstructions += 2;
        }
        if (largeRipOffset) {
            numInstructions += 2;
        }
        return numInstructions * SPARCStackFrameLayout.SPARC_INSTRUCTION_WIDTH;
    }

    @Override
    protected int emitPrologue() {
        if (_adapterFrameGenerator != null) {
            final GPR stackPointerRegister = _targetABI.stackPointer();
            final GPR framePointerRegister = _targetABI.framePointer();
            final GPR ripPointer = GPR.O7;
            final GPR frameSizeRegister = _targetABI.scratchRegister();
            final Label jitEntryPoint = new Label();

            //                                   |<-------------------- JIT frame size ----------------------->|
            //      | register window save area  | non parameter locals | lb | template slots | call save area | parameters |
            //      |<------------- offset to saved literal base ------>|
            //      |<------------- offset to spill slots------------------->|                                 ^
            //                                                               ^                            caller's stack pointer (unbiased)
            //                                                        callee frame pointer

            final int jitedCodeFrameSize = _jitStackFrameLayout.frameSize();
            final int offsetToCallSaveAreaFromFP = ((SPARCJitStackFrameLayout) _jitStackFrameLayout).offsetToTopOfFrameFromFramePointer() - SPARCJitStackFrameLayout.CALL_SAVE_AREA_SIZE;

            // The following offsets are from the callee's stack pointer
            final int offsetToSavedBaseLiteral = SPARCStackFrameLayout.STACK_BIAS + SPARCStackFrameLayout.minStackFrameSize() + _jitStackFrameLayout.sizeOfNonParameterLocals();
            final int offsetToSpillSlots = offsetToSavedBaseLiteral + JIT_SLOT_SIZE;
            final int offsetToCallSaveArea = offsetToSpillSlots + offsetToCallSaveAreaFromFP;
            final boolean largeFrame = !SPARCAssembler.isSimm13(jitedCodeFrameSize);
            final boolean largeOffsets = !SPARCAssembler.isSimm13(offsetToCallSaveArea + 2 * STACK_SLOT_SIZE);
            _adapterFrameGenerator.setJitedCodeFrameSize(jitedCodeFrameSize);
            _adapterFrameGenerator.setJitEntryPoint(jitEntryPoint);
            try {
                _asm.reset();
                // Skip over the frame adapter for calls from jit code
                _asm.ba(AnnulBit.NO_A, BranchPredictionBit.PT, ICCOperand.ICC, jitEntryPoint);
                if (largeFrame) {
                    _asm.sethi(_asm.hi(jitedCodeFrameSize), frameSizeRegister);
                } else {
                    _asm.sub(stackPointerRegister, jitedCodeFrameSize, stackPointerRegister);
                }
                // Entry point to the optimized code
                final Label adapterCodeStart = new Label();
                _asm.bindLabel(adapterCodeStart);
                _adapterFrameGenerator.emitPrologue(_asm);
                _adapterFrameGenerator.emitEpilogue(_asm);

                assert jitEntryPoint.state().equals(Label.State.BOUND);

                if (largeFrame) {
                    _asm.or(frameSizeRegister, _asm.lo(jitedCodeFrameSize), frameSizeRegister);
                    _asm.sub(stackPointerRegister, frameSizeRegister, stackPointerRegister);
                }

                // Frame for the JITed call already allocated. All that  is left to do is
                // save the caller's address, frame pointer, then set our own frame pointer.
                if (largeOffsets) {
                    assert SPARCAssembler.isSimm13(offsetToCallSaveAreaFromFP);
                    // Offsets from stack pointer too large to be used as immediate.
                    // Instead, we compute the new frame pointer into a temporary that we use as a base.
                    final GPR newFramePointerRegister = _targetABI.scratchRegister();
                    _asm.setsw(offsetToSpillSlots, newFramePointerRegister);
                    _asm.add(stackPointerRegister, newFramePointerRegister, newFramePointerRegister);
                    _asm.stx(ripPointer, newFramePointerRegister, offsetToCallSaveAreaFromFP + STACK_SLOT_SIZE);
                    _asm.stx(framePointerRegister, newFramePointerRegister, offsetToCallSaveAreaFromFP);
                    _asm.mov(newFramePointerRegister, framePointerRegister);
                } else {
                    _asm.stx(ripPointer, stackPointerRegister, offsetToCallSaveArea + STACK_SLOT_SIZE);
                    _asm.stx(framePointerRegister, stackPointerRegister, offsetToCallSaveArea);
                    _asm.add(stackPointerRegister, offsetToSpillSlots, framePointerRegister);
                }
                _asm.rd(StateRegister.PC, _targetABI.literalBaseRegister());
                int numInstructions = 1;
                if (classMethodActor().codeAttribute().exceptionHandlerTable() != null) {
                    // Conservatively assume that the method may catch an implicit exception.
                    // In that case, we must initialized the saved area with the literal base pointer to make sure
                    // stack unwind can set it correctly.
                    _asm.stx(_targetABI.literalBaseRegister(), framePointerRegister, -STACK_SLOT_SIZE);
                    numInstructions++;
                }

                // TODO: this can be removed if the method is leaf and if it doesn't have any runtime exception handler
                _codeBuffer.emitCodeFrom(_asm);
                final int offsetToSetLiteralBaseInstruction = _codeBuffer.currentPosition() - numInstructions * SPARCStackFrameLayout.SPARC_INSTRUCTION_WIDTH;
                _offsetToSetLiteralBaseInstruction = Layout.byteArrayLayout().getElementOffsetInCell(offsetToSetLiteralBaseInstruction).toInt();

                return jitEntryPoint.position() - adapterCodeStart.position();
            } catch (AssemblyException assemblyException) {
                ProgramError.unexpected(assemblyException);
            }
        }
        return 0;
    }

    static {
        /*
         * Initialization of the target ABI
         * FIXME: some redundancies with EirABI constructor... Need to figure out how to better factor this out.
         */
        final Class<TargetABI<GPR, FPR>> type = null;
        _targetABI = StaticLoophole.cast(type, VMConfiguration.target().targetABIsScheme().jitABI());
        _cpuFramePointer = _targetABI.registerRoleAssignment().integerRegisterActingAs(Role.CPU_FRAME_POINTER);
        _safepointTemplate = VMConfiguration.target().safepoint().code();
        assert _safepointTemplate.length == SPARCStackFrameLayout.SPARC_INSTRUCTION_WIDTH;
        final Endianness endianness = VMConfiguration.target().platform().processorKind().dataModel().endianness();
        final SPARCAssembler asm =  SPARCAssembler.createAssembler(VMConfiguration.target().platform().processorKind().dataModel().wordWidth());
        asm.nop();
        _nopTemplate = toByteArrayAndReset(asm);

        // Table switch templates creation.
        Label branchToDefaultTarget = new Label();
        _tableSwitchTemplates[0] = buildTableSwitchTemplate(asm, branchToDefaultTarget, false);
        _offsetToTableSwitchBranchToDefaultTarget = toPosition(branchToDefaultTarget);
        branchToDefaultTarget = new Label();
        _tableSwitchTemplates[1] = buildTableSwitchTemplate(asm, branchToDefaultTarget, true);
        assert _offsetToTableSwitchBranchToDefaultTarget == toPosition(branchToDefaultTarget) - SPARCStackFrameLayout.SPARC_INSTRUCTION_WIDTH;

        // Lookup switch template creation
        branchToDefaultTarget = new Label();
        _lookupSwitchTemplate = buildLookupSwitchTemplate(asm, branchToDefaultTarget);
        _offsetToLookupSwitchBranchToDefaultTarget =  toPosition(branchToDefaultTarget);

        // Conditional branch template creation
        asm.be(AnnulBit.A, BranchPredictionBit.PT, ICCOperand.ICC, 0);
        _branchTemplates.put(EQ, toByteArrayAndReset(asm));
        asm.bge(AnnulBit.A, BranchPredictionBit.PT, ICCOperand.ICC, 0);
        _branchTemplates.put(GE, toByteArrayAndReset(asm));
        asm.ble(AnnulBit.A, BranchPredictionBit.PT, ICCOperand.ICC, 0);
        _branchTemplates.put(LE, toByteArrayAndReset(asm));
        asm.bg(AnnulBit.A, BranchPredictionBit.PT, ICCOperand.ICC, 0);
        _branchTemplates.put(GT, toByteArrayAndReset(asm));
        asm.bl(AnnulBit.A, BranchPredictionBit.PT, ICCOperand.ICC, 0);
        _branchTemplates.put(LT, toByteArrayAndReset(asm));
        asm.bne(AnnulBit.A, BranchPredictionBit.PT, ICCOperand.ICC, 0);
        _branchTemplates.put(NE, toByteArrayAndReset(asm));
        asm.ba(AnnulBit.NO_A, BranchPredictionBit.PT, ICCOperand.ICC, 0);
        _branchTemplates.put(NONE, toByteArrayAndReset(asm));

        try {
            final ByteArrayInputStream bin = new ByteArrayInputStream(_tableSwitchTemplates[0]);
            bin.skip(_offsetToTableSwitchBranchToDefaultTarget);
            _tableSwitchBranchToDefaultTargetTemplate = endianness.readInt(bin);

            for (BranchCondition branchCondition : BranchCondition.values()) {
                _branchTemplateInstruction[branchCondition.ordinal()] = endianness.readInt(new ByteArrayInputStream(_branchTemplates.get(branchCondition)));
            }
            _lookupSwitchBranchToDefaultTargetTemplate = _branchTemplateInstruction[NONE.ordinal()];

            asm.jmpl(GPR.O7, 8, GPR.G0);
            RET_TEMPLATE = endianness.readInt(new ByteArrayInputStream(toByteArrayAndReset(asm)));
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}

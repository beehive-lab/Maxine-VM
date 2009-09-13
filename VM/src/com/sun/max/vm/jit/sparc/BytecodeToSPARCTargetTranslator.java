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
import com.sun.max.asm.InlineDataDescriptor.*;
import com.sun.max.asm.sparc.*;
import com.sun.max.asm.sparc.complete.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.b.c.d.e.sparc.target.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.eir.sparc.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.jit.Stop.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.VMRegister.*;
import com.sun.max.vm.runtime.sparc.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.sparc.*;
import com.sun.max.vm.template.*;
import com.sun.max.vm.type.*;

/**
 * SPARC-specific part of the bytecode to target translator. This implementation doesn't use register windows for stack
 * frames, i.e., a sequence of JITed-code to JITed-code uses the same register window. Therefore, JITed code needs to
 * save the caller context (its return address and frame pointer). This context saving is done in the JITed code
 * prologue. When calling to optimized code or runtime code, the callee pushes a register window, and no context need to
 * be saved.
 *
 * @author Laurent Daynes
 * @author Paul Caprioli
 */
public class BytecodeToSPARCTargetTranslator extends BytecodeToTargetTranslator {

    /**
     * Used to disable stack banging in JITed code even when Trap.STACK_BANGING is true.
     */
    private static final boolean ENABLE_JIT_STACK_BANGING = false;

    /**
     * Canonicalized Target ABI.
     */
    static final TargetABI<GPR, FPR> TARGET_ABI;

    private static final GPR CPU_FRAME_POINTER;

    /**
     * Bytes for the instruction performing safepoint. Used to fill delay slot of backward branches.
     */
    private static final byte[] SAFEPOINT_TEMPLATE;

    /**
     * Bytes for the nop instruction. Useful to efficiently fill unused delay slot.
     */
    private static final byte[] NOP_TEMPLATE;

    private static final BranchConditionMap<byte[]> BRANCH_TEMPLATES = new BranchConditionMap<byte[]>();

    private static final int[] BRANCH_TEMPLATE_INSTRUCTION = new int[BranchCondition.values().length];

    /**
     * Templates for table switch bytecode. There are two templates. The second one differs from the first one in that
     * it has an extra instruction to adjust the tag register. The second must be used when the low match isn't zero.
     */
    private static final byte[][] TABLE_SWITCH_TEMPLATES = new byte[2][];

    /**
     * Offset to the instruction within the table switch template that branch to the default target.
     */
    private static final int OFFSET_TO_TABLE_SWITCH_BRANCH_TO_DEFAULT_TARGET;

    /**
     * Template of the instruction that branch to the default target. Used to fix the 19-bits displacement of the branch
     * instruction.
     */
    private static final int TABLE_SWITCH_BRANCH_TO_DEFAULT_TARGET_TEMPLATE;

    /**
     * Template to the lookup switch bytecode.
     */
    private static final byte[] LOOKUP_SWITCH_TEMPLATE;

    /**
     * Offset to the instruction within the lookup switch template that branch to the default target.
     */
    private static final int OFFSET_TO_LOOKUP_SWITCH_BRANCH_TO_DEFAULT_TARGET;

    /**
     * Template of the instruction that branch to the default target. Used to fix the 19-bits displacement of the branch
     * instruction.
     */
    private static final int LOOKUP_SWITCH_BRANCH_TO_DEFAULT_TARGET_TEMPLATE;

    public static final int RET_TEMPLATE;

    /*
     * Constant amount of bytes to add to a call-save-area pointer to obtain the biased stack pointer of the caller.
     * This is used below to re-compute the biased stack pointer of a JIT caller.
     */
    private static final int CALL_SAVE_AREA_OFFSET_TO_STACK = SPARCJitStackFrameLayout.CALL_SAVE_AREA_SIZE - SPARCStackFrameLayout.OFFSET_FROM_SP_TO_FIRST_SLOT;

    private static final int DISP19_MASK = 0x7ffff;

    /**
     * Offset to the top of the operand stack, when the top is an integer value.
     */
    private static final int ITOS_OFFSET = SPARCStackFrameLayout.OFFSET_FROM_SP_TO_FIRST_SLOT + JitStackFrameLayout.offsetInStackSlot(Kind.INT);

    private final SPARCAssembler asm;

    private final SPARCAdapterFrameGenerator adapterFrameGenerator;

    /**
     * Offset in bytes, from the beginning of the emitted code, to the instruction in the prologue that set the literal
     * base. This must be added to the offset to literal passed in parameters to
     * {@link loadTemplateArgumentRelativeToInstructionPointer}.
     */
    private int offsetToSetLiteralBaseInstruction;

    static {
        /*
         * Initialization of the target ABI FIXME: some redundancies with EirABI constructor... Need to figure out how
         * to better factor this out.
         */
        final Class<TargetABI<GPR, FPR>> type = null;
        TARGET_ABI = StaticLoophole.cast(type, VMConfiguration.target().targetABIsScheme().jitABI());
        CPU_FRAME_POINTER = TARGET_ABI.registerRoleAssignment().integerRegisterActingAs(Role.CPU_FRAME_POINTER);
        SAFEPOINT_TEMPLATE = VMConfiguration.target().safepoint.code;
        assert SAFEPOINT_TEMPLATE.length == InstructionSet.SPARC.instructionWidth;
        final Endianness endianness = VMConfiguration.target().platform().processorKind.dataModel.endianness;
        final SPARCAssembler asm = SPARCAssembler.createAssembler(VMConfiguration.target().platform().processorKind.dataModel.wordWidth);
        asm.nop();
        NOP_TEMPLATE = toByteArrayAndReset(asm);

        // Table switch templates creation.
        Label branchToDefaultTarget = new Label();
        TABLE_SWITCH_TEMPLATES[0] = buildTableSwitchTemplate(asm, branchToDefaultTarget, false);
        OFFSET_TO_TABLE_SWITCH_BRANCH_TO_DEFAULT_TARGET = toPosition(branchToDefaultTarget);
        branchToDefaultTarget = new Label();
        TABLE_SWITCH_TEMPLATES[1] = buildTableSwitchTemplate(asm, branchToDefaultTarget, true);
        assert OFFSET_TO_TABLE_SWITCH_BRANCH_TO_DEFAULT_TARGET == toPosition(branchToDefaultTarget) - InstructionSet.SPARC.instructionWidth;

        // Lookup switch template creation
        branchToDefaultTarget = new Label();
        LOOKUP_SWITCH_TEMPLATE = buildLookupSwitchTemplate(asm, branchToDefaultTarget);
        OFFSET_TO_LOOKUP_SWITCH_BRANCH_TO_DEFAULT_TARGET = toPosition(branchToDefaultTarget);

        // Conditional branch template creation
        asm.be(AnnulBit.A, BranchPredictionBit.PT, ICCOperand.ICC, 0);
        BRANCH_TEMPLATES.put(EQ, toByteArrayAndReset(asm));
        asm.bge(AnnulBit.A, BranchPredictionBit.PT, ICCOperand.ICC, 0);
        BRANCH_TEMPLATES.put(GE, toByteArrayAndReset(asm));
        asm.ble(AnnulBit.A, BranchPredictionBit.PT, ICCOperand.ICC, 0);
        BRANCH_TEMPLATES.put(LE, toByteArrayAndReset(asm));
        asm.bg(AnnulBit.A, BranchPredictionBit.PT, ICCOperand.ICC, 0);
        BRANCH_TEMPLATES.put(GT, toByteArrayAndReset(asm));
        asm.bl(AnnulBit.A, BranchPredictionBit.PT, ICCOperand.ICC, 0);
        BRANCH_TEMPLATES.put(LT, toByteArrayAndReset(asm));
        asm.bne(AnnulBit.A, BranchPredictionBit.PT, ICCOperand.ICC, 0);
        BRANCH_TEMPLATES.put(NE, toByteArrayAndReset(asm));
        asm.ba(AnnulBit.NO_A, BranchPredictionBit.PT, ICCOperand.ICC, 0);
        BRANCH_TEMPLATES.put(NONE, toByteArrayAndReset(asm));

        try {
            final ByteArrayInputStream bin = new ByteArrayInputStream(TABLE_SWITCH_TEMPLATES[0]);
            bin.skip(OFFSET_TO_TABLE_SWITCH_BRANCH_TO_DEFAULT_TARGET);
            TABLE_SWITCH_BRANCH_TO_DEFAULT_TARGET_TEMPLATE = endianness.readInt(bin);

            for (BranchCondition branchCondition : BranchCondition.values()) {
                BRANCH_TEMPLATE_INSTRUCTION[branchCondition.ordinal()] = endianness.readInt(new ByteArrayInputStream(BRANCH_TEMPLATES.get(branchCondition)));
            }
            LOOKUP_SWITCH_BRANCH_TO_DEFAULT_TARGET_TEMPLATE = BRANCH_TEMPLATE_INSTRUCTION[NONE.ordinal()];

            asm.jmpl(GPR.O7, 8, GPR.G0);
            RET_TEMPLATE = endianness.readInt(new ByteArrayInputStream(toByteArrayAndReset(asm)));
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @PROTOTYPE_ONLY
    private static byte[] buildLookupSwitchTemplate(SPARCAssembler asm, Label branchToDefaultTarget) {
        final GPR keyRegister = GPR.O0;
        final GPR indexRegister = GPR.O1; // initialized to index of the last match.
        final GPR matchRegister = GPR.O2;
        final GPR offsetRegister = matchRegister;
        final GPR tableRegister = TARGET_ABI.scratchRegister();
        final Label matchOffsetPairsTable = new Label();
        final Label here = new Label();
        final Label loopBegin = new Label();
        final Label found = new Label();

        asm.reset();

        asm.bindLabel(here);
        asm.rd(StateRegister.PC, tableRegister);
        // Pop the key from the top of the stack
        asm.ldsw(TARGET_ABI.stackPointer(), ITOS_OFFSET, keyRegister);
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
        asm.add(TARGET_ABI.stackPointer(), JIT_SLOT_SIZE, TARGET_ABI.stackPointer());

        asm.bindLabel(found);
        asm.ldsw(tableRegister, indexRegister, offsetRegister);
        asm.jmp(tableRegister, offsetRegister);
        // pop top of stack in delay slot
        asm.add(TARGET_ABI.stackPointer(), JIT_SLOT_SIZE, TARGET_ABI.stackPointer());

        asm.bindLabel(matchOffsetPairsTable);

        return toByteArrayAndReset(asm);
    }

    @PROTOTYPE_ONLY
    private static byte[] buildTableSwitchTemplate(SPARCAssembler asm, Label branchToDefaultTarget, boolean adjustTag) {
        asm.reset();
        final GPR scratchRegister = TARGET_ABI.scratchRegister();
        final GPR tableRegister = scratchRegister;
        final GPR targetAddressRegister = tableRegister;
        final Label jumpTable = new Label();
        final Label here = new Label();
        final int defaultTargetOffset = 0;

        final GPR tagRegister = GPR.O0;
        final GPR indexRegister = tagRegister;
        final GPR numElementsRegister = GPR.O1;

        asm.ldsw(TARGET_ABI.stackPointer(), ITOS_OFFSET, tagRegister);
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
        asm.add(TARGET_ABI.stackPointer(), JIT_SLOT_SIZE, TARGET_ABI.stackPointer());
        asm.addcc(tableRegister, here, jumpTable, tableRegister);
        asm.sll(tagRegister, 2, indexRegister);
        asm.ldsw(tableRegister, indexRegister, GPR.O7);
        asm.jmp(targetAddressRegister, GPR.O7);
        asm.nop(); // delay slot
        asm.bindLabel(jumpTable);
        return toByteArrayAndReset(asm);
    }

    public BytecodeToSPARCTargetTranslator(ClassMethodActor classMethodActor, CodeBuffer codeBuffer, TemplateTable templateTable, SPARCEirABI optimizingCompilerAbi, boolean trace) {
        super(classMethodActor, codeBuffer, templateTable, new SPARCJitStackFrameLayout(classMethodActor, templateTable.maxFrameSlots), trace);
        asm = optimizingCompilerAbi.createAssembler();
        SPARCAdapterFrameGenerator adapterFrameGenerator = null;
        if (optimizingCompilerAbi != null) {
            adapterFrameGenerator = SPARCAdapterFrameGenerator.optimizedToJitCompilerAdapterFrameGenerator(classMethodActor, optimizingCompilerAbi);
        }
        this.adapterFrameGenerator = adapterFrameGenerator;
    }

    @Override
    public TargetABI targetABI() {
        return TARGET_ABI;
    }

    @Override
    public int adapterReturnPosition() {
        try {
            if (adapterFrameGenerator.adapterReturnPoint().state() == Label.State.BOUND) {
                return adapterFrameGenerator.adapterReturnPoint().position();
            }
            return -1;
        } catch (AssemblyException assemblyException) {
            throw ProgramError.unexpected();
        }
    }

    private DFPR doublePrecisionParameterRegister(int parameterIndex) {
        return (DFPR) TARGET_ABI.floatingPointParameterRegisters().get(2 * parameterIndex);
    }

    private SFPR singlePrecisionParameterRegister(int parameterIndex) {
        return (SFPR) TARGET_ABI.floatingPointParameterRegisters().get((2 * parameterIndex) + 1);
    }

    @Override
    protected void assignDoubleTemplateArgument(int parameterIndex, double argument) {
        try {
            final GPR scratchRegister = GPR.G1;
            final GPR scratchRegister2 = GPR.O7;
            final DFPR register = doublePrecisionParameterRegister(parameterIndex);
            asm.reset();
            if (argument == 0) {
                asm.clr(scratchRegister);
            } else {
                asm.setx(SpecialBuiltin.doubleToLong(argument), scratchRegister2, scratchRegister);
            }
            asm.stx(scratchRegister, CPU_FRAME_POINTER, SPARCJitStackFrameLayout.OFFSET_TO_FLOATING_POINT_TEMP_AREA);
            asm.ldd(CPU_FRAME_POINTER, SPARCJitStackFrameLayout.OFFSET_TO_FLOATING_POINT_TEMP_AREA, register);
            codeBuffer.emitCodeFrom(asm);
        } catch (AssemblyException e) {
            ProgramError.unexpected();
        }
    }

    @Override
    protected void assignFloatTemplateArgument(int parameterIndex, float argument) {
        try {
            final GPR scratchRegister = TARGET_ABI.scratchRegister();
            final SFPR register = singlePrecisionParameterRegister(parameterIndex);
            asm.reset();
            if (argument == 0) {
                asm.clr(scratchRegister);
            } else {
                asm.setsw(SpecialBuiltin.floatToInt(argument), scratchRegister);
            }
            asm.stw(scratchRegister, CPU_FRAME_POINTER, SPARCJitStackFrameLayout.OFFSET_TO_FLOATING_POINT_TEMP_AREA);
            asm.ld(CPU_FRAME_POINTER, SPARCJitStackFrameLayout.OFFSET_TO_FLOATING_POINT_TEMP_AREA, register);
            codeBuffer.emitCodeFrom(asm);
        } catch (AssemblyException e) {
            ProgramError.unexpected();
        }
    }

    @Override
    protected void assignIntTemplateArgument(int parameterIndex, int argument) {
        try {
            final GPR register = TARGET_ABI.integerIncomingParameterRegisters().get(parameterIndex);
            asm.reset();
            if (argument == 0) {
                asm.clr(register);
            } else {
                asm.setsw(argument, register);
            }
            codeBuffer.emitCodeFrom(asm);
        } catch (AssemblyException e) {
            ProgramError.unexpected();
        }
    }

    @Override
    protected void assignLongTemplateArgument(int parameterIndex, long argument) {
        try {
            final GPR register = TARGET_ABI.integerIncomingParameterRegisters().get(parameterIndex);
            asm.reset();
            asm.setx(argument, GPR.O7, register);
            codeBuffer.emitCodeFrom(asm);
        } catch (AssemblyException e) {
            ProgramError.unexpected();
        }
    }

    @Override
    protected void emitReturn() {
        final GPR framePointer = TARGET_ABI.framePointer();
        final GPR stackPointer = TARGET_ABI.stackPointer();
        final GPR callSaveAreaPointer = GPR.O5;
        final int stackAmountInBytes = jitStackFrameLayout.sizeOfParameters() + CALL_SAVE_AREA_OFFSET_TO_STACK;
        assert SPARCAssembler.isSimm13(stackAmountInBytes) : "must be imm13";

        final int offsetToCallSaveArea = VMConfiguration.target().targetABIsScheme().jitABI().alignFrameSize(SPARCJitStackFrameLayout.CALL_SAVE_AREA_SIZE + jitStackFrameLayout.sizeOfTemplateSlots()) -
                        SPARCJitStackFrameLayout.CALL_SAVE_AREA_SIZE;

        asm.reset();
        asm.add(framePointer, offsetToCallSaveArea, callSaveAreaPointer);
        asm.ldx(callSaveAreaPointer, STACK_SLOT_SIZE, GPR.O7);
        asm.ldx(callSaveAreaPointer, GPR.G0, framePointer);
        // Reload literal base register (its saving area is just below the frame pointer).
        asm.ldx(framePointer, -STACK_SLOT_SIZE, TARGET_ABI.literalBaseRegister());

        asm.jmpl(GPR.O7, 8, GPR.G0);
        asm.add(callSaveAreaPointer, stackAmountInBytes, stackPointer);
        codeBuffer.emitCodeFrom(asm);
    }

    /**
     * Efficient patching of the target of branch instruction.
     *
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
            codeBuffer.fix(instructionOffset + 3, (byte) (instruction & 0xff));
            codeBuffer.fix(instructionOffset + 2, (byte) ((instruction >> 8) & 0xff));
            codeBuffer.fix(instructionOffset + 1, (byte) ((instruction >> 16) & 0xff));
        } catch (AssemblyException assemblyException) {
            throw new TranslationException(assemblyException);
        }
    }

    @Override
    protected void emitBranch(BranchCondition branchCondition, int fromBytecodePosition, int toBytecodePosition) {
        final int branchPosition = codeBuffer.currentPosition();

        codeBuffer.emit(BRANCH_TEMPLATES.get(branchCondition));
        if (fromBytecodePosition < toBytecodePosition) {
            // Fill delay slot with a nop.
            codeBuffer.emit(NOP_TEMPLATE);
            // Forward branch. Record it.
            addForwardBranch(new ForwardBranch(branchCondition, branchPosition, toBytecodePosition));
        } else {
            final int branchDestination = bytecodeToTargetCodePosition(toBytecodePosition);
            final int byteOffsetToBranchTarget = branchDestination - branchPosition;
            fixBranchLabel(branchPosition, byteOffsetToBranchTarget, BRANCH_TEMPLATE_INSTRUCTION[branchCondition.ordinal()]);
            // The safepoint instruction is emitted in the delay slot of the branch. It will be executed only if the
            // branch is taken.
            codeBuffer.emit(SAFEPOINT_TEMPLATE);
            emitSafepoint(new BackwardBranchBytecodeSafepoint(branchPosition + 4, currentOpcodePosition()));
        }
    }

    @Override
    protected void fixForwardBranch(ForwardBranch forwardBranch) {
        final int toBranchTarget = bytecodeToTargetCodePosition(forwardBranch.targetBytecodePosition);
        final int offsetToBranchTarget = toBranchTarget - forwardBranch.targetCodePosition;
        assert offsetToBranchTarget >= 0;
        fixBranchLabel(forwardBranch.targetCodePosition, offsetToBranchTarget, BRANCH_TEMPLATE_INSTRUCTION[forwardBranch.condition.ordinal()]);
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
            final int offsetToLastKey = sizeOfInlinedTable - (2 * Ints.SIZE);
            asm.reset();
            asm.setsw(offsetToLastKey, GPR.O1);
            codeBuffer.emitCodeFrom(asm);
            codeBuffer.emit(LOOKUP_SWITCH_TEMPLATE);

            final LookupTable32 lookupTable32 = new LookupTable32(codeBuffer.currentPosition(), numberOfCases);
            inlineDataRecorder.add(lookupTable32);
            codeBuffer.reserve(lookupTable32.size());

            final int[] matches = new int[numberOfCases];
            final int[] targetBytecodePositions = new int[numberOfCases];
            final BytecodeScanner scanner = bytecodeScanner();
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
            asm.reset();
            final Directives directives = asm.directives();
            // Initialize the match-offset pair table: matching values are at even positions, offset to target at odd
            // positions
            for (int i = 0; i < lookupSwitch.matches.length; i++) {
                directives.inlineInt(lookupSwitch.matches[i]);
                final int targetBytecodePosition = lookupSwitch.targetBytecodePositions[i];
                final int targetTargetCodeOffset = bytecodeToTargetCodePosition(targetBytecodePosition) - matchOffsetPairTableAddress;
                directives.inlineInt(targetTargetCodeOffset);
            }
            codeBuffer.fix(matchOffsetPairTableAddress, asm);
        } catch (AssemblyException assemblyException) {
            throw new TranslationException(assemblyException);
        }
    }

    @Override
    protected void fixLookupSwitch(LookupSwitch lookupSwitch) {
        // The lookupSwitchTemplate doesn't include the instruction(s) that set the offset to the last key.
        // Count the additional instructions (one or two, depending on the size of the inline table) to get the correct
        // offset to
        // first instruction of the lookup template.

        final int numInstructionsBeforeTemplate = SPARCAssembler.isSimm13(lookupSwitch.matches.length) ? 1 : 2;
        final int templateTargetCodePosition = bytecodeToTargetCodePosition(lookupSwitch.opcodePosition) + (numInstructionsBeforeTemplate * InstructionSet.SPARC.instructionWidth);

        // The default target is expressed as an offset from the lookup switch bytecode.
        // Compute the default target's position relative to the beginning of the method.
        final int defaultTargetTargetCodePosition = bytecodeToTargetCodePosition(lookupSwitch.defaultTargetBytecodePosition);

        // Fix the branch target in the template.
        final int offsetToBranchInstruction = templateTargetCodePosition + OFFSET_TO_LOOKUP_SWITCH_BRANCH_TO_DEFAULT_TARGET;

        final int offsetToDefaultTarget = defaultTargetTargetCodePosition - offsetToBranchInstruction;
        fixBranchLabel(offsetToBranchInstruction, offsetToDefaultTarget, LOOKUP_SWITCH_BRANCH_TO_DEFAULT_TARGET_TEMPLATE);

        final int matchOffsetPairTableAddress = templateTargetCodePosition + LOOKUP_SWITCH_TEMPLATE.length;
        fixMatchOffsetPairTable(lookupSwitch, matchOffsetPairTableAddress);
    }

    @Override
    protected void emitTableSwitch(int lowMatch, int highMatch, int opcodePosition, int defaultTargetOffset, int numberOfCases) {
        try {
            int templateIndex = 0;
            asm.reset();
            asm.setsw(numberOfCases, GPR.O1);
            if (lowMatch != 0) {
                templateIndex = 1;
                asm.setsw(lowMatch, GPR.O2);
            }
            final int templatePrefixSize = asm.currentPosition();
            codeBuffer.emitCodeFrom(asm);
            codeBuffer.emit(TABLE_SWITCH_TEMPLATES[templateIndex]);

            final InlineDataDescriptor.JumpTable32 jumpTable32 = new InlineDataDescriptor.JumpTable32(codeBuffer.currentPosition(), lowMatch, highMatch);
            inlineDataRecorder.add(jumpTable32);
            assert jumpTable32.size() == numberOfCases * WordWidth.BITS_32.numberOfBytes;
            codeBuffer.reserve(jumpTable32.size());

            // Remember the location of the tableSwitch bytecode and the area in the code buffer where the targets will
            // be written.
            final BytecodeScanner scanner = bytecodeScanner();
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
            final int templateTargetCodePosition = bytecodeToTargetCodePosition(tableSwitch.opcodePosition) + tableSwitch.templatePrefixSize();
            // The default target is expressed as an offset from the tableswitch bytecode. Compute the default target's
            // position relative to the beginning of the method.
            final int defaultTargetTargetCodePosition = bytecodeToTargetCodePosition(tableSwitch.defaultTargetBytecodePosition);
            // Fix the branch target in the template.
            final int offsetToBranchInstruction = templateTargetCodePosition + OFFSET_TO_TABLE_SWITCH_BRANCH_TO_DEFAULT_TARGET + tableSwitch.templateIndex * InstructionSet.SPARC.instructionWidth;

            final int offsetToDefaultTarget = defaultTargetTargetCodePosition - offsetToBranchInstruction;
            fixBranchLabel(offsetToBranchInstruction, offsetToDefaultTarget, TABLE_SWITCH_BRANCH_TO_DEFAULT_TARGET_TEMPLATE);

            // We generate a jump table using the inlining support of the assembler. The resulting table is then used to
            // fix the
            // reserved space in the code buffer. The jump table comprises only offsets relative to the table itself.
            // This avoids having to deal with relocation.
            asm.reset();
            final Directives directives = asm.directives();
            final int jumpTableAddress = templateTargetCodePosition + TABLE_SWITCH_TEMPLATES[tableSwitch.templateIndex].length;
            for (int targetBytecodePosition : tableSwitch.targetBytecodePositions) {
                final int targetTargetCodeOffset = bytecodeToTargetCodePosition(targetBytecodePosition) - jumpTableAddress;
                directives.inlineInt(targetTargetCodeOffset);
            }
            codeBuffer.fix(jumpTableAddress, asm);
        } catch (AssemblyException assemblyException) {
            throw new TranslationException(assemblyException);
        }
    }

    @Override
    protected int computeReferenceLiteralOffset(int numReferenceLiteral) {
        // Remember: in the target bundle, the reference literal cell is directly adjacent to the code cell.
        return numReferenceLiteral * Word.size() + offsetToSetLiteralBaseInstruction;
    }

    @Override
    protected void loadTemplateArgumentRelativeToInstructionPointer(Kind kind, int parameterIndex, int offsetFromLiteralBase) {
        final GPR literalBaseRegister = TARGET_ABI.literalBaseRegister();
        asm.reset();
        switch (kind.asEnum) {
            case LONG:
            case WORD:
            case REFERENCE:
                final GPR destinationRegister = TARGET_ABI.integerIncomingParameterRegisters().get(parameterIndex);
                if (SPARCAssembler.isSimm13(offsetFromLiteralBase)) {
                    asm.ldx(literalBaseRegister, offsetFromLiteralBase, destinationRegister);
                } else {
                    try {
                        asm.setsw(offsetFromLiteralBase, GPR.O7);
                        asm.ldx(literalBaseRegister, GPR.O7, destinationRegister);
                    } catch (AssemblyException e) {
                        ProgramError.unexpected();
                    }
                }
                codeBuffer.emitCodeFrom(asm);

                break;
            default: {
                FatalError.unimplemented();
            }
        }
    }

    @Override
    protected int registerReferenceMapSize() {
        return SPARCTrapStateAccess.registerReferenceMapSize();
    }

    /**
     * Return the size (in number of bytes) of the sequence of instructions in the prologue that sets up the new frame.
     * This method must not perform allocation.
     *
     * @param targetMethod
     * @return
     */
    public static int frameBuilderSize(SPARCJitTargetMethod targetMethod) {
        final boolean largeFrame = !SPARCAssembler.isSimm13(targetMethod.frameSize());
        final JitStackFrameLayout stackFrameLayout = targetMethod.stackFrameLayout();
        final int offsetToCallSaveArea = SPARCStackFrameLayout.MIN_STACK_FRAME_SIZE + stackFrameLayout.sizeOfTemplateSlots() + stackFrameLayout.sizeOfNonParameterLocals() + JIT_SLOT_SIZE;
        final boolean largeOffset = !SPARCAssembler.isSimm13(offsetToCallSaveArea + SPARCJitStackFrameLayout.CALL_SAVE_AREA_SIZE);

        int numInstructions;
        if (ENABLE_JIT_STACK_BANGING & Trap.STACK_BANGING) {
            numInstructions = 5; // includes the stack-banging ldub
            final int stackBangOffset = -Trap.stackGuardSize + StackBias.SPARC_V9.stackBias();
            if (!SPARCAssembler.isSimm13(stackBangOffset)) {
                numInstructions += SPARCAssembler.setswNumberOfInstructions(stackBangOffset & ~0x3FF);
            }
        } else {
            numInstructions = 4;
        }
        if (largeFrame) {
            numInstructions += 2;
        }
        if (largeOffset) {
            numInstructions += 2;
        }
        return numInstructions * InstructionSet.SPARC.instructionWidth;
    }

    @Override
    protected int emitPrologue() {
        if (adapterFrameGenerator != null) {
            final GPR stackPointerRegister = TARGET_ABI.stackPointer();
            final GPR framePointerRegister = TARGET_ABI.framePointer();
            final GPR linkRegister = GPR.O7;
            final GPR scratchRegister = TARGET_ABI.scratchRegister();
            final Label jitEntryPoint = new Label();

            // |<-------------------- JIT frame size ----------------------->|
            // | register window save area | non parameter locals | lb | template slots | call save area | parameters |
            // |<------------- offset to saved literal base ------>|
            // |<------------- offset to spill slots------------------->| ^
            // ^ caller's stack pointer (unbiased)
            // callee frame pointer

            final int jitedCodeFrameSize = jitStackFrameLayout.frameSize();
            final int offsetToCallSaveAreaFromFP = ((SPARCJitStackFrameLayout) jitStackFrameLayout).offsetToTopOfFrameFromFramePointer() - SPARCJitStackFrameLayout.CALL_SAVE_AREA_SIZE;

            // The following offsets are from the callee's stack pointer
            final int offsetToSavedBaseLiteral = SPARCStackFrameLayout.STACK_BIAS + SPARCStackFrameLayout.MIN_STACK_FRAME_SIZE + jitStackFrameLayout.sizeOfNonParameterLocals();
            final int offsetToSpillSlots = offsetToSavedBaseLiteral + JIT_SLOT_SIZE;
            final int offsetToCallSaveArea = offsetToSpillSlots + offsetToCallSaveAreaFromFP;
            final boolean largeFrame = !SPARCAssembler.isSimm13(jitedCodeFrameSize);
            final boolean largeOffsets = !SPARCAssembler.isSimm13(offsetToCallSaveArea + SPARCJitStackFrameLayout.CALL_SAVE_AREA_SIZE);
            adapterFrameGenerator.setJitedCodeFrameSize(jitedCodeFrameSize);
            adapterFrameGenerator.setJitEntryPoint(jitEntryPoint);
            try {
                asm.reset();
                // Skip over the frame adapter for calls from jit code
                asm.ba(AnnulBit.NO_A, BranchPredictionBit.PT, ICCOperand.ICC, jitEntryPoint);
                if (largeFrame) {
                    asm.sethi(SPARCAssembler.hi(jitedCodeFrameSize), scratchRegister);
                } else {
                    asm.sub(stackPointerRegister, jitedCodeFrameSize, stackPointerRegister);
                }
                // Entry point to the optimized code
                final Label adapterCodeStart = new Label();
                asm.bindLabel(adapterCodeStart);
                adapterFrameGenerator.emitPrologue(asm);
                adapterFrameGenerator.emitEpilogue(asm);

                assert jitEntryPoint.state().equals(Label.State.BOUND);

                if (largeFrame) {
                    asm.or(scratchRegister, SPARCAssembler.lo(jitedCodeFrameSize), scratchRegister);
                    asm.sub(stackPointerRegister, scratchRegister, stackPointerRegister);
                }
                if (ENABLE_JIT_STACK_BANGING & Trap.STACK_BANGING) {
                    final int stackBangOffset = -Trap.stackGuardSize + StackBias.SPARC_V9.stackBias();
                    if (SPARCAssembler.isSimm13(stackBangOffset)) {
                        asm.ldub(stackPointerRegister, stackBangOffset, GPR.G0);
                    } else {
                        asm.setsw(stackBangOffset & ~0x3FF, scratchRegister); // Note: stackBangOffset is rounded off
                        asm.ldub(stackPointerRegister, scratchRegister, GPR.G0);
                    }
                }

                // Frame for the JITed call already allocated. All that is left to do is
                // save the caller's address, frame pointer, then set our own frame pointer.
                if (largeOffsets) {
                    assert SPARCAssembler.isSimm13(offsetToCallSaveAreaFromFP);
                    // Offsets from stack pointer too large to be used as immediate.
                    // Instead, we compute the new frame pointer into a temporary that we use as a base.
                    final GPR newFramePointerRegister = scratchRegister;
                    asm.setsw(offsetToSpillSlots, newFramePointerRegister);
                    asm.add(stackPointerRegister, newFramePointerRegister, newFramePointerRegister);
                    asm.stx(linkRegister, newFramePointerRegister, offsetToCallSaveAreaFromFP + STACK_SLOT_SIZE);
                    asm.stx(framePointerRegister, newFramePointerRegister, offsetToCallSaveAreaFromFP);
                    asm.mov(newFramePointerRegister, framePointerRegister);
                } else {
                    asm.stx(linkRegister, stackPointerRegister, offsetToCallSaveArea + STACK_SLOT_SIZE);
                    asm.stx(framePointerRegister, stackPointerRegister, offsetToCallSaveArea);
                    asm.add(stackPointerRegister, offsetToSpillSlots, framePointerRegister);
                }
                asm.rd(StateRegister.PC, TARGET_ABI.literalBaseRegister());
                int numInstructions = 1;
                if (classMethodActor.codeAttribute().exceptionHandlerTable() != null) {
                    // Conservatively assume that the method may catch an implicit exception.
                    // In that case, we must initialize the save area with the literal base pointer to make sure
                    // stack unwind can set it correctly.
                    asm.stx(TARGET_ABI.literalBaseRegister(), framePointerRegister, -STACK_SLOT_SIZE);
                    numInstructions++;
                }

                codeBuffer.emitCodeFrom(asm);

                // TODO: this can be removed if the method is leaf and if it doesn't have any runtime exception handler
                final int offset = codeBuffer.currentPosition() - numInstructions * InstructionSet.SPARC.instructionWidth;
                this.offsetToSetLiteralBaseInstruction = Layout.byteArrayLayout().getElementOffsetInCell(offset).toInt();

                return jitEntryPoint.position() - adapterCodeStart.position();
            } catch (AssemblyException assemblyException) {
                ProgramError.unexpected(assemblyException);
            }
        }
        return 0;
    }

}

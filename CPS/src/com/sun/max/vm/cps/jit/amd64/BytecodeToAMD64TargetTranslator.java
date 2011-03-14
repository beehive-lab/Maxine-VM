/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.cps.jit.amd64;

import static com.sun.max.asm.x86.Scale.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.bytecode.BranchCondition.*;

import java.io.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.asm.Assembler.Directives;
import com.sun.max.asm.*;
import com.sun.max.asm.InlineDataDescriptor.JumpTable32;
import com.sun.max.asm.InlineDataDescriptor.LookupTable32;
import com.sun.max.asm.amd64.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.asm.amd64.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.amd64.*;
import com.sun.max.vm.cps.eir.amd64.*;
import com.sun.max.vm.cps.jit.*;
import com.sun.max.vm.cps.jit.Stop.BackwardBranchBytecodeSafepoint;
import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.cps.template.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.amd64.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * AMD64-specific part of the Bytecodes to Target Translator.
 *
 * @author Laurent Daynes
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class BytecodeToAMD64TargetTranslator extends BytecodeToTargetTranslator {

    /**
     * Canonicalized Target ABI.
     */
    public static final TargetABI<AMD64GeneralRegister64, AMD64XMMRegister> TARGET_ABI;

    private final AMD64Assembler asm = new AMD64Assembler();

    @Override
    public TargetABI<AMD64GeneralRegister64, AMD64XMMRegister> targetABI() {
        return TARGET_ABI;
    }

    @Override
    protected int registerReferenceMapSize() {
        return AMD64TargetMethodUtil.registerReferenceMapSize();
    }

    @Override
    protected  int computeReferenceLiteralOffset(int numReferenceLiteral) {
        // Remember: in the target bundle, the reference literal cell is directly adjacent to the code cell.
        return numReferenceLiteral * Word.size() + Layout.byteArrayLayout().getElementOffsetInCell(codeBuffer.currentPosition()).toInt();
    }

    @Override
    protected void assignIntTemplateArgument(int parameterIndex, int argument) {
        final AMD64GeneralRegister64 register = TARGET_ABI.integerIncomingParameterRegisters.get(parameterIndex);
        asm.reset();
        asm.mov(register, argument);
        codeBuffer.emitCodeFrom(asm);
    }

    @Override
    protected void assignFloatTemplateArgument(int parameterIndex, float argument) {
        final AMD64GeneralRegister32 scratch = AMD64GeneralRegister32.from(TARGET_ABI.scratchRegister());
        asm.reset();
        asm.movl(scratch, SpecialBuiltin.floatToInt(argument));
        asm.movdl(TARGET_ABI.floatingPointParameterRegisters.get(parameterIndex), scratch);
        codeBuffer.emitCodeFrom(asm);
    }

    @Override
    protected void assignLongTemplateArgument(int parameterIndex, long argument) {
        asm.reset();
        asm.mov(TARGET_ABI.integerIncomingParameterRegisters.get(parameterIndex), argument);
        codeBuffer.emitCodeFrom(asm);
    }

    @Override
    protected void assignDoubleTemplateArgument(int parameterIndex, double argument) {
        asm.reset();
        asm.mov(TARGET_ABI.scratchRegister(), SpecialBuiltin.doubleToLong(argument));
        asm.movdq(TARGET_ABI.floatingPointParameterRegisters.get(parameterIndex), TARGET_ABI.scratchRegister());
        codeBuffer.emitCodeFrom(asm);
    }

    @Override
    protected void alignDirectBytecodeCall(TargetMethod template, ClassMethodActor callee) {
        final int alignment = WordWidth.BITS_64.numberOfBytes - 1;
        if ((codeBuffer.currentPosition() & alignment) == 0) {
            // don't bother. CodeBuffer is already aligned.
            return;
        }
        if (template.getTargetMethod(callee) != null) {
            return;
        }
        final int callSitePosition = codeBuffer.currentPosition() + template.stopPosition(0);
        final int roundDownMask = ~alignment;
        final int endOfCallSite = callSitePosition + (AMD64EirInstruction.CALL.DIRECT_METHOD_CALL_INSTRUCTION_LENGTH - 1);
        if ((callSitePosition & roundDownMask) == (endOfCallSite & roundDownMask)) {
            // No need for any alignment.
            return;
        }
        // Only one call site. Don't need to align the template to a 8-byte boundaries. It is enough to ensure that the single call
        // fits within a cache line (currently conservatively assumed to be 8 bytes).
        final byte nop = (byte) 0x90;
        int numBytesNeeded = WordWidth.BITS_64.numberOfBytes - (callSitePosition & alignment);
        // Emit nop instructions to align up to next Word boundary.
        while (numBytesNeeded > 0) {
            codeBuffer.emit(nop);
            --numBytesNeeded;
        }
    }

    @Override
    protected void loadTemplateArgumentRelativeToInstructionPointer(Kind kind, int parameterIndex, int offsetFromInstructionPointer) {
        switch (kind.asEnum) {
            case LONG:
            case WORD:
            case REFERENCE: {
                codeBuffer.emit((byte) 0x48); // rex prefix (without partial register encoding since we only use a couple of template parameters)
                codeBuffer.emit((byte) 0x8B); // opcode for rpi_mov(AMD64GeneralRegister64, offset32)

                final int register = TARGET_ABI.integerIncomingParameterRegisters.get(parameterIndex).ordinal();
                codeBuffer.emit((byte) (5 | (register << 3))); // mod/rm byte encoding address mode and destination register

                final int instructionSize = 7;
                int offset = offsetFromInstructionPointer - instructionSize;

                // Little-endian output:
                codeBuffer.emit((byte) (offset & 0xff));
                offset >>= 8;
                codeBuffer.emit((byte) (offset & 0xff));
                offset >>= 8;
                codeBuffer.emit((byte) (offset & 0xff));
                offset >>= 8;
                codeBuffer.emit((byte) (offset & 0xff));
                break;
            }
            default: {
                FatalError.unimplemented();
            }
        }
    }

    /**
     * Templates for native branch instructions with a short (8 bits) relative offset.
     */
    private static final BranchConditionMap<byte[]> rel8BranchTemplates = new BranchConditionMap<byte[]>();

    /**
     * Templates for native branch instructions with a long (32 bits) relative offset.
     */
    private static final BranchConditionMap<byte[]> rel32BranchTemplates = new BranchConditionMap<byte[]>();

    /**
     * Table of templates for tableswitch bytecode. There is one template for each byte of an alignment requirement for the
     * jump table (e.g., if the requirement is 4-bytes aligned, there are 4 templates, one for offsets 0, 1, 2 and 3).
     */
    private static final byte[][] tableSwitchTemplates = new byte[WordWidth.BITS_32.numberOfBytes][];

    private static byte[] lookupSwitchTemplate;
    private static ImmediateConstantModifier tableswitchHighMatchModifier;
    private static ImmediateConstantModifier tableswitchIndexAdjustModifier;
    private static BranchTargetModifier tableswitchBranchToDefaultTargetModifier;

    private static ImmediateConstantModifier lookupswitchMaxIndexModifier;
    private static BranchTargetModifier lookupswitchBranchToDefaultTargetModifier;

    /**
     * Editors of the template for conditional branch with short offset (<= 8 bits).
     */
    private final BranchConditionMap<AMD64InstructionEditor> rel8BranchEditors;

    /**
     * Editors of the template for conditional branch with long offset (> 8 bits and <= 32 bits).
     */
    private final BranchConditionMap<AMD64InstructionEditor> rel32BranchEditors;

    public BytecodeToAMD64TargetTranslator(ClassMethodActor classMethodActor, CodeBuffer codeBuffer, TemplateTable templateTable, boolean trace) {
        super(classMethodActor, codeBuffer, templateTable, new AMD64JVMSFrameLayout(classMethodActor, templateTable.maxFrameSlots), trace);
        rel8BranchEditors = new BranchConditionMap<AMD64InstructionEditor>();
        rel32BranchEditors = new BranchConditionMap<AMD64InstructionEditor>();
        // Make copies of the template. That's because at the moment, the translator works by modifying the template, then
        // copying the modified template in the code buffer. Thus, if we want concurrent compilation to occur, each translator needs
        // it's own copy of the templates.
        // FIXME: may want to do this lazily, i.e., allocate editors as needed (causes an extra test).
        for (BranchCondition cond :  BranchCondition.VALUES) {
            rel8BranchEditors.put(cond, new AMD64InstructionEditor(rel8BranchTemplates.get(cond).clone()));
            rel32BranchEditors.put(cond, new AMD64InstructionEditor(rel32BranchTemplates.get(cond).clone()));
        }
    }

    @Override
    protected void fixForwardBranch(ForwardBranch forwardBranch) {
        final int toBranchTarget = bytecodeToTargetCodePosition(forwardBranch.targetBytecodePosition);
        // Take address of target code of following bytecode. This currently works because the JIT emits code linearly, regardless of basic block,
        // i.e., no tiling is taking place. If this assumption does not hold any more, we'll have to recompute the length of the template, or record its offset in the code buffer.
        final int endOfRel32BranchInstruction = forwardBranch.targetCodePosition + rel32BranchTemplates.get(forwardBranch.condition).length;
        final int offsetToBranchTarget = toBranchTarget - endOfRel32BranchInstruction;
        // TODO: what to do for a branch target == 0?
        assert offsetToBranchTarget >= 0;
        final AMD64InstructionEditor branchEditor = rel32BranchEditors.get(forwardBranch.condition);
        try {
            // Fix the branch target in the template.
            branchEditor.fixBranchRelativeDisplacement(WordWidth.BITS_32, offsetToBranchTarget);
            // Replace the placeholder with the conditional branch instruction
            codeBuffer.fix(forwardBranch.targetCodePosition, branchEditor.code, branchEditor.startPosition, branchEditor.size);
        } catch (AssemblyException assemblyException) {
            throw new TranslationException(assemblyException);
        }
    }

    @Override
    protected void emitBranch(BranchCondition branchCondition, int fromBytecodePosition, int toBytecodePosition) {
        int offsetToBranchTarget = 0;
        AMD64InstructionEditor branchEditor = rel32BranchEditors.get(branchCondition);  // Default
        WordWidth offsetWidth = WordWidth.BITS_32;                                       // Default

        if (fromBytecodePosition < toBytecodePosition) {
            // Forward branch. Record it
            addForwardBranch(new ForwardBranch(branchCondition, codeBuffer.currentPosition(), toBytecodePosition));
            assert bytecodeToTargetCodePosition(toBytecodePosition) == 0;
            // we leave the default 32 bit relative offset format.
            // TODO: alternative is to conservatively guess the width of the relative offset based
            // on the max length of templates times the number
            // of bytecodes to the target of the branch.
        } else {
            if (!emitBackwardEdgeSafepointAtTarget) {
                // Need to emit the safepoint at the source of the branch. We emit the safepoint just before the actual branch instruction:
                // so it can benefit from the same condition testing as the branch instruction to be performed conditionally (using a conditional move), i.e., we
                // want the safepoint to occur only if we're branching backward.
                // Note that the safepoint takes place once the stack frame is in the same state as that of the target bytecode.
                // The reference maps of the target should be used when at this safepoint.
                final int stopPosition = codeBuffer.currentPosition();
                codeBuffer.emit(vm().safepoint.code);
                emitSafepoint(new BackwardBranchBytecodeSafepoint(stopPosition, opcodeBci));
            }
            // Compute relative offset.
            final int toBranchTarget = bytecodeToTargetCodePosition(toBytecodePosition);
            final int endOfRel8BranchInstruction = codeBuffer.currentPosition() + rel8BranchTemplates.get(branchCondition).length;
            offsetToBranchTarget = toBranchTarget - endOfRel8BranchInstruction;
            if (WordWidth.signedEffective(offsetToBranchTarget) != WordWidth.BITS_8) {
                final int endOfRel32BranchInstruction = codeBuffer.currentPosition() + rel32BranchTemplates.get(branchCondition).length;
                offsetToBranchTarget = toBranchTarget - endOfRel32BranchInstruction;
                branchEditor = rel32BranchEditors.get(branchCondition);
            } else {
                // Stay with 8 bits branch. Change the default setting:
                branchEditor = rel8BranchEditors.get(branchCondition);
                offsetWidth = WordWidth.BITS_8;
            }
        }
        assert offsetToBranchTarget <= 0;
        // Fix the branch target in the template
        try {
            branchEditor.fixBranchRelativeDisplacement(offsetWidth, offsetToBranchTarget);
            // Emit the unconditional branch.
            branchEditor.writeTo(codeBuffer.outputStream());
        } catch (Exception exception) {
            throw new TranslationException(exception);
        }
    }

    // The template is emitted and customized. A switch statement has typically only forward branches. Thus,
    // the emitting only reserves space for inlined targets and records the target code position of the first
    // target. This position combined with the bytecode position of the tableswitch instruction is then used to
    // eventually compute all the targets and write them in the inlined area.
    //
    @Override
    protected void emitTableSwitch(int lowMatch, int highMatch, int opcodePosition, int defaultTargetOffset, int numberOfCases) {
        try {
            int effectiveHighMatch = highMatch;
            // What's the offset of the code buffer pointer to the last 4-byte aligned word ?
            final int mask = WordWidth.BITS_32.numberOfBytes - 1;
            final int templateIndex = codeBuffer.currentPosition() & mask;
            final byte[] tableSwitchTemplate = tableSwitchTemplates[templateIndex].clone();
            if (lowMatch != 0) {
                tableswitchIndexAdjustModifier.fix(tableSwitchTemplate, 0, lowMatch);
                effectiveHighMatch -= lowMatch;
            }
            if (effectiveHighMatch != 1) {
                tableswitchHighMatchModifier.fix(tableSwitchTemplate, 0, effectiveHighMatch);
            }
            codeBuffer.emit(tableSwitchTemplate);

            final JumpTable32 jumpTable32 = new JumpTable32(codeBuffer.currentPosition(), lowMatch, highMatch);
            inlineDataRecorder.add(jumpTable32);
            codeBuffer.reserve(jumpTable32.size());

            // Remember the location of the tableSwitch bytecode and the area in the code buffer where the targets will be written.
            final int[] targetBytecodePositions = new int[numberOfCases];
            for (int i = 0; i != numberOfCases; ++i) {
                targetBytecodePositions[i] = readS4() + opcodePosition;
            }
            final int defaultTargetBytecodePosition = opcodePosition + defaultTargetOffset;
            addSwitch(new TableSwitch(opcodePosition, templateIndex, defaultTargetBytecodePosition, targetBytecodePositions));
        } catch (AssemblyException assemblyException) {
            throw new TranslationException(assemblyException);
        }
    }

    @Override
    protected void fixTableSwitch(TableSwitch tableSwitch) {
        try {
            final int templateTargetCodePosition = bytecodeToTargetCodePosition(tableSwitch.opcodePosition);
            // The default target is expressed as an offset from the tableswitch bytecode. Compute the default target's position relative to the beginning of the method.
            final int defaultTargetTargetCodePosition = bytecodeToTargetCodePosition(tableSwitch.defaultTargetBytecodePosition);
            // Fix the branch target in the template.
            final int offsetToDefaultTarget = defaultTargetTargetCodePosition - (templateTargetCodePosition + tableswitchBranchToDefaultTargetModifier.endPosition());
            codeBuffer.fix(templateTargetCodePosition, tableswitchBranchToDefaultTargetModifier, offsetToDefaultTarget);
            // We generate a jump table using the inlining support of the assembler. The resulting table is then used to fix the
            // reserved space in the code buffer. The jump table comprises only offsets relative to the table itself.
            // This avoids having to deal with relocation.
            asm.reset();
            final Directives directives = asm.directives();
            final int jumpTablePosition =  templateTargetCodePosition + tableSwitchTemplates[tableSwitch.templateIndex].length;
            for (int targetBytecodePosition : tableSwitch.targetBytecodePositions) {
                final int targetTargetCodeOffset = bytecodeToTargetCodePosition(targetBytecodePosition) - jumpTablePosition;
                directives.inlineInt(targetTargetCodeOffset);
            }
            codeBuffer.fix(jumpTablePosition, asm);
        } catch (AssemblyException assemblyException) {
            throw new TranslationException(assemblyException);
        }
    }

    @Override
    protected void emitLookupSwitch(int opcodePosition, int defaultTargetOffset, int numberOfCases) {
        try {
            final int defaultTargetBytecodePosition = opcodePosition + defaultTargetOffset;
            if (numberOfCases == 0) {
                // Pop the key
                TargetMethod code = getCode(BytecodeTemplate.POP);
                emitAndRecordStops(code);
                // Skip completely if default target is next instruction.
                // Lookup switches are aligned on 4 bytes and have a minimum of 12 bytes.
                final int nextBytecodePosition = (opcodePosition & 3) + 12;
                if (defaultTargetBytecodePosition > nextBytecodePosition) {
                    emitBranch(NONE, opcodePosition, defaultTargetBytecodePosition);
                }
                return;
            }
            final byte[] lookupSwitchTemplate = BytecodeToAMD64TargetTranslator.lookupSwitchTemplate.clone();
            // Fix the index to the last match value in the jump table
            final int lastMatchIndex = (numberOfCases - 1) * 2;
            lookupswitchMaxIndexModifier.fix(lookupSwitchTemplate, lastMatchIndex);
            codeBuffer.emit(lookupSwitchTemplate);

            final LookupTable32 lookupTable32 = new LookupTable32(codeBuffer.currentPosition(), numberOfCases);
            inlineDataRecorder.add(lookupTable32);
            codeBuffer.reserve(lookupTable32.size());

            final int[] matches = new int[numberOfCases];
            final int[] targetBytecodePositions = new int[numberOfCases];
            for (int i = 0; i != numberOfCases; ++i) {
                matches[i] = readS4();
                targetBytecodePositions[i] = readS4() + opcodePosition;
            }
            addSwitch(new LookupSwitch(opcodePosition, defaultTargetBytecodePosition, matches, targetBytecodePositions));
        } catch (AssemblyException assemblyException) {
            throw new TranslationException(assemblyException);
        }
    }

    @Override
    protected void fixLookupSwitch(LookupSwitch lookupSwitch) {
        try {
            final int templateAddress = bytecodeToTargetCodePosition(lookupSwitch.opcodePosition);
            // The default target is expressed as an offset from the lookupswitch bytecode. Compute the default target's position relative to the beginning of the method.
            final int defaultTargetAddress = bytecodeToTargetCodePosition(lookupSwitch.defaultTargetBytecodePosition);
            // Fix the branches to the default  target in the template.
            final int offsetToDefaultTarget = defaultTargetAddress - (templateAddress + lookupswitchBranchToDefaultTargetModifier.endPosition());
            codeBuffer.fix(templateAddress, lookupswitchBranchToDefaultTargetModifier, offsetToDefaultTarget);

            asm.reset();
            final Directives directives = asm.directives();
            final int matchOffsetPairTableAddress =  templateAddress + lookupSwitchTemplate.length;
            // Initialize the match offset pair table: matching values are at even positions, offset to target at odd positions
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
    protected void emitReturn() {
        asm.reset();
        asm.addq(TARGET_ABI.framePointer(), framePointerAdjustment());
        asm.leave();
        // when returning, retract from the caller stack by the space used for the arguments.
        final short stackAmountInBytes = (short) jitStackFrameLayout.sizeOfParameters();
        if (stackAmountInBytes != 0) {
            asm.ret(stackAmountInBytes);
        } else {
            asm.ret();
        }
        codeBuffer.emitCodeFrom(asm);
    }

    @Override
    public Adapter emitPrologue() {
        Adapter adapter = null;
        if (adapterGenerator != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(13);
            adapter = adapterGenerator.adapt(classMethodActor, baos);
            byte[] prologue = baos.toByteArray();
            asm.emitByteArray(prologue, 0, prologue.length);
        }

        // method entry point: setup a regular frame
        asm.enter((short) (jitStackFrameLayout.frameSize() - Word.size()), (byte) 0);
        asm.subq(TARGET_ABI.framePointer(), framePointerAdjustment());
        if (Trap.STACK_BANGING) {
            asm.mov(TARGET_ABI.scratchRegister(), -Trap.stackGuardSize, TARGET_ABI.stackPointer().indirect());
        }
        codeBuffer.emitCodeFrom(asm);
        return adapter;
    }

    private int framePointerAdjustment() {
        final int enterSize = jitStackFrameLayout.frameSize() - Word.size();
        return enterSize - jitStackFrameLayout.sizeOfNonParameterLocals();
    }

    static {
        /*
         * Initialization of the target ABI
         * FIXME: some redundancies with EirABI constructor... Need to figure out how to better factor this out.
         */
        final Class<TargetABI<AMD64GeneralRegister64, AMD64XMMRegister>> type = null;
        TARGET_ABI = Utils.cast(type, TargetABIsScheme.INSTANCE.jitABI);
        // Initialization of the few hand-crafted templates
        final byte rel8 = 0;
        final int rel32 = 0;
        // First, emit all the templates with a single assembler, recording their offset in the code buffer

        final AMD64Assembler asm = new AMD64Assembler();

        asm.jmp(rel8);
        rel8BranchTemplates.put(NONE, toByteArrayAndReset(asm));
        asm.jmp(rel32);
        rel32BranchTemplates.put(NONE, toByteArrayAndReset(asm));

        asm.jz(rel8);
        rel8BranchTemplates.put(EQ, toByteArrayAndReset(asm));
        asm.jz(rel32);
        rel32BranchTemplates.put(EQ, toByteArrayAndReset(asm));

        asm.jnz(rel8);
        rel8BranchTemplates.put(NE, toByteArrayAndReset(asm));
        asm.jnz(rel32);
        rel32BranchTemplates.put(NE, toByteArrayAndReset(asm));

        asm.jl(rel8);
        rel8BranchTemplates.put(LT, toByteArrayAndReset(asm));
        asm.jl(rel32);
        rel32BranchTemplates.put(LT, toByteArrayAndReset(asm));

        asm.jnl(rel8);
        rel8BranchTemplates.put(GE, toByteArrayAndReset(asm));
        asm.jnl(rel32);
        rel32BranchTemplates.put(GE, toByteArrayAndReset(asm));

        asm.jnle(rel8);
        rel8BranchTemplates.put(GT, toByteArrayAndReset(asm));
        asm.jnle(rel32);
        rel32BranchTemplates.put(GT, toByteArrayAndReset(asm));

        asm.jle(rel8);
        rel8BranchTemplates.put(LE, toByteArrayAndReset(asm));
        asm.jle(rel32);
        rel32BranchTemplates.put(LE, toByteArrayAndReset(asm));

        for (BranchCondition branchCondition : BranchCondition.VALUES) {
            assert rel8BranchTemplates.get(branchCondition) != null;
            assert rel32BranchTemplates.get(branchCondition) != null;
        }

        // Templates for tableswitch
        buildTableSwitchTemplate(asm);

        // Template for lookupswitch
        buildLookupSwitchTemplate(asm);
    }

    @HOSTED_ONLY
    private static void buildTableSwitchTemplate(final AMD64Assembler asm) {
        for (int i = 0; i < tableSwitchTemplates.length; i++) {
            final Label indexTest = new Label();
            final Label branchToDefaultTarget = new Label();
            final Label loadJumpTable = new Label();
            final Label indexAdjust = new Label();
            tableSwitchTemplates[i] = createTableSwitchTemplate(asm, i, indexAdjust, indexTest, branchToDefaultTarget, loadJumpTable);
            if (i == 0) {
                tableswitchIndexAdjustModifier = new ImmediateConstantModifier(toPosition(indexAdjust), toPosition(indexTest) - toPosition(indexAdjust), IntValue.from(0));
                tableswitchHighMatchModifier = new ImmediateConstantModifier(toPosition(indexTest), toPosition(branchToDefaultTarget) - toPosition(indexTest), IntValue.from(1));
                tableswitchBranchToDefaultTargetModifier = new BranchTargetModifier(toPosition(branchToDefaultTarget), toPosition(loadJumpTable) - toPosition(branchToDefaultTarget), WordWidth.BITS_32);
            }
        }
    }

    @HOSTED_ONLY
    private static void buildLookupSwitchTemplate(final AMD64Assembler asm) {
        asm.reset();
        final Label branchToDefaultTarget = new Label();
        final Label initLastMatchIndex = new Label();
        final Label loopBegin = new Label();
        final Label found = new Label();

        final AMD64GeneralRegister32 valueRegister = AMD64GeneralRegister32.EAX;
        final AMD64GeneralRegister64 tableRegister = AMD64GeneralRegister64.RBX;
        final AMD64GeneralRegister32 indexRegister = AMD64GeneralRegister32.ECX;
        final Label valueTable = new Label();
        final int offsetToDefaultTarget = 0;
        final byte offsetToTargetDisp = 4;

        // Load top of stack into temporary
        asm.mov(valueRegister, 0, TARGET_ABI.stackPointer().indirect());
        // Pop top of stack
        asm.addq(TARGET_ABI.stackPointer(), JVMSFrameLayout.JVMS_SLOT_SIZE);
        asm.rip_lea(tableRegister, valueTable);
        asm.bindLabel(initLastMatchIndex);   // marker for template customization.
        asm.mov(indexRegister, 1);
        asm.bindLabel(loopBegin);
        // compare the test value against the value register
        final AMD64GeneralRegister64 indexRegister64 = AMD64GeneralRegister64.from(indexRegister);
        asm.cmp(valueRegister, tableRegister.base(), indexRegister64.index(), SCALE_4);
        // if exact match, exit loop
        asm.jz(found);
        asm.subl(indexRegister, 2);
        asm.jnb(loopBegin);
        asm.bindLabel(branchToDefaultTarget);   // marker for template customization.
        // If not exact match, means it's lower, and not in the table. Go to default case.
        asm.jmp(offsetToDefaultTarget);
        asm.bindLabel(found);
        asm.movsxd(indexRegister64, offsetToTargetDisp, tableRegister.base(), indexRegister64.index(), SCALE_4);
        asm.add(tableRegister, indexRegister64);
        asm.jmp(tableRegister);
        asm.bindLabel(valueTable);
        lookupSwitchTemplate = toByteArrayAndReset(asm);
        lookupswitchMaxIndexModifier = new ImmediateConstantModifier(toPosition(initLastMatchIndex), toPosition(loopBegin) - toPosition(initLastMatchIndex), IntValue.from(0));
        lookupswitchBranchToDefaultTargetModifier = new BranchTargetModifier(toPosition(branchToDefaultTarget), toPosition(found) - toPosition(branchToDefaultTarget), WordWidth.BITS_32);
    }

    @HOSTED_ONLY
    private static byte[] createTableSwitchTemplate(AMD64Assembler asm, int numBytesFromAlignment, Label indexAdjust, Label indexTest, Label branchToDefaultTarget, Label loadJumpTable) {
        asm.reset();
        final AMD64GeneralRegister32 indexRegister = AMD64GeneralRegister32.EAX;
        final AMD64GeneralRegister64 tableRegister = AMD64GeneralRegister64.R15;
        final AMD64GeneralRegister64 targetRegister = tableRegister;
        final Directives directives = asm.directives();
        final int offsetToDefaultTarget = 0; // to force an 32-bits target address in branch -- see below.
        final Label jumpTable = new Label();

        // Fill up the assembler's buffer with as much
        final byte zero = 0;
        int numBytes = numBytesFromAlignment;
        while (numBytes-- > 0) {
            directives.inlineByte(zero);
        }
        // Load top of stack into temporary
        asm.mov(indexRegister, 0, TARGET_ABI.stackPointer().indirect());
        // Pop top of stack
        asm.addq(TARGET_ABI.stackPointer(), JVMSFrameLayout.JVMS_SLOT_SIZE);
        // subtract the low value from the switch index
        asm.bindLabel(indexAdjust);
        asm.sub_EAX(0);
        // Test against the high value
        asm.bindLabel(indexTest);   // marker for template customization.
        asm.cmp_EAX(1);
        // index not in the table, use default target.
        asm.bindLabel(branchToDefaultTarget);   // marker for template customization.
        asm.jnbe(offsetToDefaultTarget);
        // Set register to address of inlined  jumpTable
        asm.bindLabel(loadJumpTable);
        asm.rip_lea(tableRegister, jumpTable);
        // Load offset to the target corresponding to the value from the inlined jump table.
        final AMD64GeneralRegister64 indexRegister64 = AMD64GeneralRegister64.from(indexRegister);
        asm.movsxd(indexRegister64, tableRegister.base(), indexRegister64.index(), SCALE_4);
        asm.add(targetRegister, indexRegister64);
        asm.jmp(targetRegister);

        // setup the inlined jmp table.
        directives.align(WordWidth.BITS_32.numberOfBytes);
        asm.bindLabel(jumpTable);
        final byte[] emitted = toByteArrayAndReset(asm);
        final byte[] template = new byte[emitted.length - numBytesFromAlignment];
        Bytes.copy(emitted, numBytesFromAlignment, template, 0, template.length);
        return template;
    }
}

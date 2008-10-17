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
package com.sun.max.vm.jit.amd64;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.asm.Assembler.*;
import com.sun.max.asm.amd64.*;

import static com.sun.max.asm.x86.Scale.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.asm.amd64.*;
import com.sun.max.vm.bytecode.*;

import static com.sun.max.vm.bytecode.BranchCondition.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.b.c.d.e.amd64.target.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.amd64.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.jit.Stop.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.amd64.*;
import com.sun.max.vm.template.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * AMD64-specific part of the Bytecode to Target Translator.
 *
 * @author Laurent Daynes
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class BytecodeToAMD64TargetTranslator extends BytecodeToTargetTranslator {

    public static final boolean STACK_BANGING = true;

    /**
     * Canonicalized Target ABI.
     */
    private static final TargetABI<AMD64GeneralRegister64, AMD64XMMRegister> _targetABI;

    private final AMD64Assembler _asm = new AMD64Assembler();

    @Override
    public TargetABI<AMD64GeneralRegister64, AMD64XMMRegister> targetABI() {
        return _targetABI;
    }

    @Override
    protected int registerReferenceMapSize() {
        return AMD64TargetMethod.Static.registerReferenceMapSize();
    }

    @Override
    protected  int computeReferenceLiteralOffset(int numReferenceLiteral) {
        // Remember: in the target bundle, the reference literal cell is directly adjacent to the code cell.
        return numReferenceLiteral * Word.size() + Layout.byteArrayLayout().getElementOffsetInCell(_codeBuffer.currentPosition()).toInt();
    }

    @Override
    protected void assignIntTemplateArgument(int parameterIndex, int argument) {
        final AMD64GeneralRegister64 register = _targetABI.integerIncomingParameterRegisters().get(parameterIndex);
        _asm.reset();
        _asm.mov(register, argument);
        _codeBuffer.emitCodeFrom(_asm);
    }

    @Override
    protected void assignFloatTemplateArgument(int parameterIndex, float argument) {
        final AMD64GeneralRegister32 scratch = AMD64GeneralRegister32.from(_targetABI.scratchRegister());
        _asm.reset();
        _asm.movl(scratch, UnsafeLoophole.floatToInt(argument));
        _asm.movdl(_targetABI.floatingPointParameterRegisters().get(parameterIndex), scratch);
        _codeBuffer.emitCodeFrom(_asm);
    }

    @Override
    protected void assignLongTemplateArgument(int parameterIndex, long argument) {
        _asm.reset();
        _asm.mov(_targetABI.integerIncomingParameterRegisters().get(parameterIndex), argument);
        _codeBuffer.emitCodeFrom(_asm);
    }

    @Override
    protected void assignDoubleTemplateArgument(int parameterIndex, double argument) {
        _asm.reset();
        _asm.mov(_targetABI.scratchRegister(), UnsafeLoophole.doubleToLong(argument));
        _asm.movdq(_targetABI.floatingPointParameterRegisters().get(parameterIndex), _targetABI.scratchRegister());
        _codeBuffer.emitCodeFrom(_asm);
    }

    @Override
    protected void loadTemplateArgumentRelativeToInstructionPointer(Kind kind, int parameterIndex, int offsetFromInstructionPointer) {
        switch (kind.asEnum()) {
            case LONG:
            case WORD:
            case REFERENCE: {
                _codeBuffer.emit((byte) 0x48); // rex prefix (without partial register encoding since we only use a couple of template parameters)
                _codeBuffer.emit((byte) 0x8B); // opcode for rpi_mov(AMD64GeneralRegister64, offset32)

                final int register = _targetABI.integerIncomingParameterRegisters().get(parameterIndex).ordinal();
                _codeBuffer.emit((byte) (5 | (register << 3))); // mod/rm byte encoding address mode and destination register

                final int instructionSize = 7;
                int offset = offsetFromInstructionPointer - instructionSize;

                // Little-endian output:
                _codeBuffer.emit((byte) (offset & 0xff));
                offset >>= 8;
                _codeBuffer.emit((byte) (offset & 0xff));
                offset >>= 8;
                _codeBuffer.emit((byte) (offset & 0xff));
                offset >>= 8;
                _codeBuffer.emit((byte) (offset & 0xff));
                break;
            }
            default: {
                Problem.unimplemented();
            }
        }
    }

    /**
     * Templates for native branch instructions with a short (8 bits) relative offset.
     */
    private static final BranchConditionMap<byte[]> _rel8BranchTemplates = new BranchConditionMap<byte[]>();

    /**
     * Templates for native branch instructions with a long (32 bits) relative offset.
     */
    private static final BranchConditionMap<byte[]> _rel32BranchTemplates = new BranchConditionMap<byte[]>();

    private static final BranchConditionMap<byte[]> _safepointAtBackwardBranchTemplates = new BranchConditionMap<byte[]>();

    /**
     * Table of templates for tableswitch bytecode. There is one template for each byte of an alignment requirement for the
     * jump table (e.g., if the requirement is 4-bytes aligned, there are 4 templates, one for offsets 0, 1, 2 and 3).
     */
    private static final byte[][] _tableSwitchTemplates = new byte[WordWidth.BITS_32.numberOfBytes()][];

    private static byte[] _lookupSwitchTemplate;
    private static ImmediateConstantModifier _tableswitchHighMatchModifier;
    private static ImmediateConstantModifier _tableswitchIndexAdjustModifier;
    private static BranchTargetModifier _tableswitchBranchToDefaultTargetModifier;

    private static ImmediateConstantModifier _lookupswitchMaxIndexModifier;
    private static BranchTargetModifier _lookupswitchBranchToDefaultTargetModifier;

    /**
     * Editors of the template for conditional branch with short offset (<= 8 bits).
     */
    private final BranchConditionMap<AMD64InstructionEditor> _rel8BranchEditors;

    /**
     * Editors of the template for conditional branch with long offset (> 8 bits and <= 32 bits).
     */
    private final BranchConditionMap<AMD64InstructionEditor> _rel32BranchEditors;

    /**
     * Adapter Frame Generator when using both an optimizing and jit compiler.
     */
    private final AMD64AdapterFrameGenerator _adapterFrameGenerator;

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

    public BytecodeToAMD64TargetTranslator(ClassMethodActor classMethodActor, CodeBuffer codeBuffer, TemplateTable templateTable, EirABI optimizingCompilerAbi, boolean trace) {
        super(classMethodActor, codeBuffer, templateTable, new AMD64JitStackFrameLayout(classMethodActor, templateTable.maxFrameSlots()), trace);
        _rel8BranchEditors = new BranchConditionMap<AMD64InstructionEditor>();
        _rel32BranchEditors = new BranchConditionMap<AMD64InstructionEditor>();
        // Make copies of the template. That's because at the moment, the translator works by modifying the template, then
        // copying the modified template in the code buffer. Thus, if we want concurrent compilation to occur, each translator needs
        // it's own copy of the templates.
        // FIXME: may want to do this lazily, i.e., allocate editors as needed (causes an extra test).
        for (BranchCondition cond :  BranchCondition.VALUES) {
            _rel8BranchEditors.put(cond, new AMD64InstructionEditor(_rel8BranchTemplates.get(cond).clone()));
            _rel32BranchEditors.put(cond, new AMD64InstructionEditor(_rel32BranchTemplates.get(cond).clone()));
        }

        AMD64AdapterFrameGenerator adapterFrameGenerator = null;
        if (optimizingCompilerAbi != null) {
            adapterFrameGenerator = AMD64AdapterFrameGenerator.optimizingToJitCompilerAdapterFrameGenerator(classMethodActor, optimizingCompilerAbi);
        }
        _adapterFrameGenerator = adapterFrameGenerator;

    }

    @Override
    protected void fixForwardBranch(ForwardBranch forwardBranch) {
        final int toBranchTarget = bytecodeToTargetCodePosition(forwardBranch._targetBytecodePosition);
        // Take address of target code of following bytecode. This currently works because the JIT emits code linearly, regardless of basic block,
        // i.e., no tiling is taking place. If this assumption does not hold any more, we'll have to recompute the length of the template, or record its offset in the code buffer.
        final int endOfRel32BranchInstruction = forwardBranch._targetCodePosition + _rel32BranchTemplates.get(forwardBranch._condition).length;
        final int offsetToBranchTarget = toBranchTarget - endOfRel32BranchInstruction;
        // TODO: what to do for a branch target == 0?
        assert offsetToBranchTarget >= 0;
        final AMD64InstructionEditor branchEditor = _rel32BranchEditors.get(forwardBranch._condition);
        try {
            // Fix the branch target in the template.
            branchEditor.fixBranchRelativeDisplacement(WordWidth.BITS_32, offsetToBranchTarget);
            // Replace the placeholder with the conditional branch instruction
            _codeBuffer.fix(forwardBranch._targetCodePosition, branchEditor._code, branchEditor._startPosition, branchEditor._size);
        } catch (AssemblyException assemblyException) {
            throw new TranslationException(assemblyException);
        }
    }

    @Override
    protected void emitBranch(BranchCondition branchCondition, int fromBytecodePosition, int toBytecodePosition) {
        int offsetToBranchTarget = 0;
        AMD64InstructionEditor branchEditor = _rel32BranchEditors.get(branchCondition);  // Default
        WordWidth offsetWidth = WordWidth.BITS_32;                                       // Default

        if (fromBytecodePosition < toBytecodePosition) {
            // Forward branch. Record it
            addForwardBranch(new ForwardBranch(branchCondition, _codeBuffer.currentPosition(), toBytecodePosition));
            assert bytecodeToTargetCodePosition(toBytecodePosition) == 0;
            // we leave the default 32 bit relative offset format.
            // TODO: alternative is to conservatively guess the width of the relative offset based
            // on the max length of templates times the number
            // of bytecodes to the target of the branch.
        } else {
            if (!_emitBackwardEdgeSafepointAtTarget) {
                // Need to emit the safepoint at the source of the branch. We emit the safepoint just before the actual branch instruction:
                // so it can benefit from the same condition testing as the branch instruction to be performed conditionally (using a conditional move), i.e., we
                // want the safepoint to occur only if we're branching backward.
                // Note that the safepoint takes place once the stack frame is in the same state as that of the target bytecode.
                // The reference maps of the target should be used when at this safepoint.
                final int stopPosition = _codeBuffer.currentPosition();
                _codeBuffer.emit(_safepointAtBackwardBranchTemplates.get(branchCondition));
                emitSafepoint(new BackwardBranchBytecodeSafepoint(stopPosition));
            }
            // Compute relative offset.
            final int toBranchTarget = bytecodeToTargetCodePosition(toBytecodePosition);
            final int endOfRel8BranchInstruction = _codeBuffer.currentPosition() + _rel8BranchTemplates.get(branchCondition).length;
            offsetToBranchTarget = toBranchTarget - endOfRel8BranchInstruction;
            if (WordWidth.signedEffective(offsetToBranchTarget) != WordWidth.BITS_8) {
                final int endOfRel32BranchInstruction = _codeBuffer.currentPosition() + _rel32BranchTemplates.get(branchCondition).length;
                offsetToBranchTarget = toBranchTarget - endOfRel32BranchInstruction;
                branchEditor = _rel32BranchEditors.get(branchCondition);
            } else {
                // Stay with 8 bits branch. Change the default setting:
                branchEditor = _rel8BranchEditors.get(branchCondition);
                offsetWidth = WordWidth.BITS_8;
            }
        }
        assert offsetToBranchTarget <= 0;
        // Fix the branch target in the template
        try {
            branchEditor.fixBranchRelativeDisplacement(offsetWidth, offsetToBranchTarget);
            // Emit the unconditional branch.
            branchEditor.writeTo(_codeBuffer.outputStream());
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
            final int mask = WordWidth.BITS_32.numberOfBytes() - 1;
            final int templateIndex = _codeBuffer.currentPosition() & mask;
            final byte[] tableSwitchTemplate = _tableSwitchTemplates[templateIndex].clone();
            if (lowMatch != 0) {
                _tableswitchIndexAdjustModifier.fix(tableSwitchTemplate, 0, lowMatch);
                effectiveHighMatch -= lowMatch;
            }
            if (effectiveHighMatch != 1) {
                _tableswitchHighMatchModifier.fix(tableSwitchTemplate, 0, effectiveHighMatch);
            }
            _codeBuffer.emit(tableSwitchTemplate);
            final int sizeOfInlinedTable = numberOfCases * WordWidth.BITS_32.numberOfBytes();
            _inlineDataRecorder.record(_codeBuffer.currentPosition(), sizeOfInlinedTable);
            _codeBuffer.reserve(sizeOfInlinedTable);

            // Remember the location of the tableSwitch bytecode and the area in the code buffer where the targets will be written.
            final BytecodeScanner scanner = getBytecodeScanner();
            final int[] targetBytecodePositions = new int[numberOfCases];
            for (int i = 0; i != numberOfCases; ++i) {
                targetBytecodePositions[i] = scanner.readSwitchOffset() + opcodePosition;
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
            final int templateTargetCodePosition = bytecodeToTargetCodePosition(tableSwitch._opcodePosition);
            // The default target is expressed as an offset from the tableswitch bytecode. Compute the default target's position relative to the beginning of the method.
            final int defaultTargetTargetCodePosition = bytecodeToTargetCodePosition(tableSwitch._defaultTargetBytecodePosition);
            // Fix the branch target in the template.
            final int offsetToDefaultTarget = defaultTargetTargetCodePosition - (templateTargetCodePosition + _tableswitchBranchToDefaultTargetModifier.endPosition());
            _codeBuffer.fix(templateTargetCodePosition, _tableswitchBranchToDefaultTargetModifier, offsetToDefaultTarget);
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

    @Override
    protected void emitLookupSwitch(int opcodePosition, int defaultTargetOffset, int numberOfCases) {
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
            final byte[] lookupSwitchTemplate = _lookupSwitchTemplate.clone();
            // Fix the index to the last match value in the jump table
            final int lastMatchIndex = (numberOfCases - 1) * 2;
            _lookupswitchMaxIndexModifier.fix(lookupSwitchTemplate, lastMatchIndex);
            _codeBuffer.emit(lookupSwitchTemplate);
            final int sizeOfInlinedTable = numberOfCases * 2 * Ints.SIZE;
            _inlineDataRecorder.record(_codeBuffer.currentPosition(), sizeOfInlinedTable);
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

    @Override
    protected void fixLookupSwitch(LookupSwitch lookupSwitch) {
        try {
            final int templateAddress = bytecodeToTargetCodePosition(lookupSwitch._opcodePosition);
            // The default target is expressed as an offset from the lookupswitch bytecode. Compute the default target's position relative to the beginning of the method.
            final int defaultTargetAddress = bytecodeToTargetCodePosition(lookupSwitch._defaultTargetBytecodePosition);
            // Fix the branches to the default  target in the template.
            final int offsetToDefaultTarget = defaultTargetAddress - (templateAddress + _lookupswitchBranchToDefaultTargetModifier.endPosition());
            _codeBuffer.fix(templateAddress, _lookupswitchBranchToDefaultTargetModifier, offsetToDefaultTarget);

            _asm.reset();
            final Directives directives = _asm.directives();
            final int matchOffsetPairTableAddress =  templateAddress + _lookupSwitchTemplate.length;
            // Initialize the match offset pair table: matching values are at even positions, offset to target at odd positions
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
    protected void emitReturn() {
        _asm.reset();
        _asm.addq(_targetABI.framePointer(), framePointerAdjustment());
        _asm.leave();
        // when returning, retract from the caller stack the space saved for parameters.
        final short stackAmountInBytes = (short) _jitStackFrameLayout.sizeOfParameters();
        if (stackAmountInBytes > 0) {
            _asm.ret(stackAmountInBytes);
        } else {
            _asm.ret();
        }
        _codeBuffer.emitCodeFrom(_asm);
    }

    @Override
    protected int emitPrologue() {
        if (_adapterFrameGenerator != null) {
            _asm.reset();
            final Directives dir = _asm.directives();
            final Label methodEntryPoint = new Label();
            _asm.jmp(methodEntryPoint);
            _asm.nop();
            _asm.nop();
            _asm.nop();
            dir.align(Kind.BYTE.size() * 4);  // forcing alignment to the next 4-bytes will always provide an 8-bytes long prologue.

            // Entry point to the optimized code
            final Label adapterCodeStart = new Label();
            _asm.bindLabel(adapterCodeStart);
            _adapterFrameGenerator.emitPrologue(_asm);
            _adapterFrameGenerator.emitEpilogue(_asm);
            _asm.bindLabel(methodEntryPoint);

            // method entry point: setup a regular frame
            _asm.enter((short) (_jitStackFrameLayout.frameSize() - Word.size()), (byte) 0);
            _asm.subq(_targetABI.framePointer(), framePointerAdjustment());
            if (STACK_BANGING) {
                _asm.mov(_targetABI.scratchRegister(), -2 * 4096, _targetABI.stackPointer().indirect());
            }
            _codeBuffer.emitCodeFrom(_asm);

            try {
                return methodEntryPoint.position() - adapterCodeStart.position();
            } catch (AssemblyException assemblyException) {
                ProgramError.unexpected(assemblyException);
            }
        }
        return 0;
    }

    private int framePointerAdjustment() {
        final int enterSize = _jitStackFrameLayout.frameSize() - Word.size();
        return enterSize - _jitStackFrameLayout.sizeOfNonParameterLocals();
    }

    static {
        if (MaxineVM.isPrototyping()) {
            /*
             * Initialization of the target ABI
             * FIXME: some redundancies with EirABI constructor... Need to figure out how to better factor this out.
             */
            final Class<TargetABI<AMD64GeneralRegister64, AMD64XMMRegister>> type = null;
            _targetABI = StaticLoophole.cast(type, VMConfiguration.target().targetABIsScheme().jitABI());
            // Initialization of the few hand-crafted templates
            final AMD64GeneralRegister64 safepointLatchReg1 = _targetABI.registerRoleAssignment().integerRegisterActingAs(VMRegister.Role.SAFEPOINT_LATCH);
            final byte rel8 = 0;
            final int rel32 = 0;
            // First, emit all the templates with a single assembler, recording their offset in the code buffer

            final AMD64Assembler asm = new AMD64Assembler();

            asm.jmp(rel8);
            _rel8BranchTemplates.put(NONE, toByteArrayAndReset(asm));
            asm.jmp(rel32);
            _rel32BranchTemplates.put(NONE, toByteArrayAndReset(asm));
            asm.mov(safepointLatchReg1, safepointLatchReg1.indirect());
            _safepointAtBackwardBranchTemplates.put(NONE, toByteArrayAndReset(asm));


            asm.jz(rel8);
            _rel8BranchTemplates.put(EQ, toByteArrayAndReset(asm));
            asm.jz(rel32);
            _rel32BranchTemplates.put(EQ, toByteArrayAndReset(asm));
            asm.cmove(safepointLatchReg1, safepointLatchReg1.indirect());
            _safepointAtBackwardBranchTemplates.put(EQ, toByteArrayAndReset(asm));

            asm.jnz(rel8);
            _rel8BranchTemplates.put(NE, toByteArrayAndReset(asm));
            asm.jnz(rel32);
            _rel32BranchTemplates.put(NE, toByteArrayAndReset(asm));
            asm.cmovne(safepointLatchReg1, safepointLatchReg1.indirect());
            _safepointAtBackwardBranchTemplates.put(NE, toByteArrayAndReset(asm));

            asm.jl(rel8);
            _rel8BranchTemplates.put(LT, toByteArrayAndReset(asm));
            asm.jl(rel32);
            _rel32BranchTemplates.put(LT, toByteArrayAndReset(asm));
            asm.cmovl(safepointLatchReg1, safepointLatchReg1.indirect());
            _safepointAtBackwardBranchTemplates.put(LT, toByteArrayAndReset(asm));

            asm.jnl(rel8);
            _rel8BranchTemplates.put(GE, toByteArrayAndReset(asm));
            asm.jnl(rel32);
            _rel32BranchTemplates.put(GE, toByteArrayAndReset(asm));
            asm.cmovge(safepointLatchReg1, safepointLatchReg1.indirect());
            _safepointAtBackwardBranchTemplates.put(GE, toByteArrayAndReset(asm));

            asm.jnle(rel8);
            _rel8BranchTemplates.put(GT, toByteArrayAndReset(asm));
            asm.jnle(rel32);
            _rel32BranchTemplates.put(GT, toByteArrayAndReset(asm));
            asm.cmovg(safepointLatchReg1, safepointLatchReg1.indirect());
            _safepointAtBackwardBranchTemplates.put(GT, toByteArrayAndReset(asm));

            asm.jle(rel8);
            _rel8BranchTemplates.put(LE, toByteArrayAndReset(asm));
            asm.jle(rel32);
            _rel32BranchTemplates.put(LE, toByteArrayAndReset(asm));
            asm.cmovle(safepointLatchReg1, safepointLatchReg1.indirect());
            _safepointAtBackwardBranchTemplates.put(LE, toByteArrayAndReset(asm));

            for (BranchCondition branchCondition : BranchCondition.VALUES) {
                assert _rel8BranchTemplates.get(branchCondition) != null;
                assert _rel32BranchTemplates.get(branchCondition) != null;
                assert _safepointAtBackwardBranchTemplates.get(branchCondition) != null;
            }

            // Templates for tableswitch
            buildTableSwitchTemplate(asm);

            // Template for lookupswitch
            buildLookupSwitchTemplate(asm);
        } else {
            // This class initializer should never be run after boot image construction.
            ProgramError.unexpected();
            _targetABI = null;
            _lookupSwitchTemplate = null;
            _tableswitchHighMatchModifier = null;
            _tableswitchIndexAdjustModifier = null;
            _tableswitchBranchToDefaultTargetModifier = null;
            _lookupswitchMaxIndexModifier = null;
            _lookupswitchBranchToDefaultTargetModifier = null;
        }
    }

    @PROTOTYPE_ONLY
    private static void buildTableSwitchTemplate(final AMD64Assembler asm) {
        ImmediateConstantModifier tableswitchHighMatchModifier = null;
        ImmediateConstantModifier tableswitchIndexAdjustModifier = null;
        BranchTargetModifier tableswitchBranchToDefaultTargetModifier = null;

        for (int i = 0; i < _tableSwitchTemplates.length; i++) {
            final Label indexTest = new Label();
            final Label branchToDefaultTarget = new Label();
            final Label loadJumpTable = new Label();
            final Label indexAdjust = new Label();
            _tableSwitchTemplates[i] = createTableSwitchTemplate(asm, i, indexAdjust, indexTest, branchToDefaultTarget, loadJumpTable);
            if (i == 0) {
                tableswitchIndexAdjustModifier = new ImmediateConstantModifier(toPosition(indexAdjust), toPosition(indexTest) - toPosition(indexAdjust), IntValue.from(0));
                tableswitchHighMatchModifier = new ImmediateConstantModifier(toPosition(indexTest), toPosition(branchToDefaultTarget) - toPosition(indexTest), IntValue.from(1));
                tableswitchBranchToDefaultTargetModifier = new BranchTargetModifier(toPosition(branchToDefaultTarget), toPosition(loadJumpTable) - toPosition(branchToDefaultTarget), WordWidth.BITS_32);
            }
        }
        _tableswitchHighMatchModifier = tableswitchHighMatchModifier;
        _tableswitchIndexAdjustModifier = tableswitchIndexAdjustModifier;
        _tableswitchBranchToDefaultTargetModifier = tableswitchBranchToDefaultTargetModifier;
    }

    @PROTOTYPE_ONLY
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
        asm.mov(valueRegister, 0, _targetABI.stackPointer().indirect());
        // Pop top of stack
        asm.addq(_targetABI.stackPointer(), JitStackFrameLayout.JIT_SLOT_SIZE);
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
        _lookupSwitchTemplate = toByteArrayAndReset(asm);
        _lookupswitchMaxIndexModifier = new ImmediateConstantModifier(toPosition(initLastMatchIndex), toPosition(loopBegin) - toPosition(initLastMatchIndex), IntValue.from(0));
        _lookupswitchBranchToDefaultTargetModifier = new BranchTargetModifier(toPosition(branchToDefaultTarget), toPosition(found) - toPosition(branchToDefaultTarget), WordWidth.BITS_32);
    }

    @PROTOTYPE_ONLY
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
        asm.mov(indexRegister, 0, _targetABI.stackPointer().indirect());
        // Pop top of stack
        asm.addq(_targetABI.stackPointer(), JitStackFrameLayout.JIT_SLOT_SIZE);
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
        directives.align(WordWidth.BITS_32.numberOfBytes());
        asm.bindLabel(jumpTable);
        final byte[] emitted = toByteArrayAndReset(asm);
        final byte[] template = new byte[emitted.length - numBytesFromAlignment];
        Bytes.copy(emitted, numBytesFromAlignment, template, 0, template.length);
        return template;
    }
}

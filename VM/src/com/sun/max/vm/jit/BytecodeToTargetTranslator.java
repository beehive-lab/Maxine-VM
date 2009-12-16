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
package com.sun.max.vm.jit;

import static com.sun.max.vm.bytecode.Bytecode.*;
import static com.sun.max.vm.bytecode.Bytecode.Flags.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.snippet.ResolutionSnippet.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.hotpath.*;
import com.sun.max.vm.jit.Stop.*;
import com.sun.max.vm.jit.Stops.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.profile.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.template.*;
import com.sun.max.vm.template.TemplateChooser.*;
import com.sun.max.vm.template.source.*;
import com.sun.max.vm.type.*;

/**
 * Simplest bytecode to target template-based translator. This translator keeps minimal state about the compilation and
 * emits templates with no assumptions with respect to values produced on top of the stack, or state of the referenced
 * symbolic links. Values produced and consumed by each bytecode are pushed / popped off an evaluation stack, as
 * described in the JVM specification.
 * <p>
 * This can be used as an adapter for more sophisticated template-based code generators.
 * <p>
 *
 * If trace instrumentation is enabled the translator will use the most generic bytecode templates which are defined in
 * {@link TracedBytecodeTemplateSource}.
 *
 * @author Laurent Daynes
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Ben L. Titzer
 * @author Michael Bebenita
 * @author Paul Caprioli
 */
public abstract class BytecodeToTargetTranslator extends BytecodeVisitor {

    private final StopsBuilder stops;

    protected boolean emitBackwardEdgeSafepointAtTarget;

    private final TemplateTable templateTable;

    protected final JitStackFrameLayout jitStackFrameLayout;

    private final int bytecodeLength;

    public final CodeBuffer codeBuffer;

    protected final boolean isTraceInstrumented;

    protected final Set<Integer> branchTargets;

    protected final InlineDataRecorder inlineDataRecorder = new InlineDataRecorder();

    protected final MethodProfile.Builder methodProfileBuilder;

    /**
     * The actor of the method being compiled.
     */
    public final ClassMethodActor classMethodActor;

    /**
     * Constant pool of the compiled method.
     */
    private final ConstantPool constantPool;

    /**
     * Map of bytecode positions to target code positions. Entries in the table corresponding to the opcode of
     * a bytecode hold the offset in the code buffer where the first byte of the template was emitted. This map
     * includes an entry for the bytecode position one byte past the end of the bytecode array. This is useful
     * for determining the end of the code emitted for the last bytecode instruction. That is, the value at
     * {@code _bytecodeToTargetCodePositionMap[_bytecodeToTargetCodePositionMap.length - 1]} is the target code
     * position at which the epilogue (if any) starts.
     */
    protected final int[] bytecodeToTargetCodePositionMap;

    protected BytecodeInfo[] bytecodeInfos;

    protected boolean[] blockStarts;
    protected int numberOfBlocks;
    private Bytecode previousBytecode;

    private int[] catchRangePositions;

    private int[] catchBlockPositions;

    /**
     * List of forward branches that need to be fixed up.
     */
    private AppendableSequence<ForwardBranch> forwardBranches = new LinkSequence<ForwardBranch>();

    /**
     * List of tableswitch and lookupswitch instructions that need to be fixed up.
     */
    private AppendableSequence<Switch> switches = new LinkSequence<Switch>();

    public BytecodeToTargetTranslator(ClassMethodActor classMethodActor, CodeBuffer codeBuffer, TemplateTable templateTable, JitStackFrameLayout jitStackFrameLayout, boolean trace) {
        if (classMethodActor.holder().kind == Kind.WORD || SignatureDescriptor.containsWord(classMethodActor.descriptor())) {
            FatalError.unexpected("Cannot JIT compile method that uses Word types: " + classMethodActor.format("%H.%n(%p)"));
        }
        this.isTraceInstrumented = trace;
        this.templateTable = templateTable;
        this.codeBuffer = codeBuffer;
        this.classMethodActor = classMethodActor;
        this.jitStackFrameLayout = jitStackFrameLayout;
        this.constantPool = classMethodActor.compilee().holder().constantPool();

        final CodeAttribute codeAttribute = classMethodActor.codeAttribute();
        this.bytecodeLength = codeAttribute.code().length;
        this.bytecodeToTargetCodePositionMap = new int[bytecodeLength + 1];
        this.bytecodeInfos = new BytecodeInfo[bytecodeLength];
        this.methodProfileBuilder = JitInstrumentation.createMethodProfile(classMethodActor);

        this.blockStarts = new boolean[bytecodeLength];
        startBlock(0);

        // Try to size the list of stops so that it should not need expanding when translating most methods
        this.stops = new StopsBuilder(bytecodeLength);

        this.emitBackwardEdgeSafepointAtTarget = false;

        // We need to discover branch targets in order to insert Hotpath counters.
        if (shouldInsertHotpathCounters()) {
            this.branchTargets = discoverBranchTargets();
        } else {
            this.branchTargets = null;
        }

    }

    public abstract TargetABI targetABI();

    private void beginBytecode(Bytecode info) {
        final int opcodePosition = currentOpcodePosition();
        final int targetCodePosition = codeBuffer.currentPosition();

        if (shouldInsertHotpathCounters() && branchTargets.contains(currentOpcodePosition())) {
            // _bytecodeToTargetCodePositionMap[opcodePosition] was already assigned by emitHotpathCounter().
        } else {
            bytecodeToTargetCodePositionMap[opcodePosition] = targetCodePosition;
        }
        bytecodeInfos[opcodePosition] = info;

        if (previousBytecode != null && previousBytecode.is(FALL_THROUGH_DELIMITER | CONDITIONAL_BRANCH | UNCONDITIONAL_BRANCH)) {
            startBlock(opcodePosition);
        }
        previousBytecode = info.bytecode();
    }

    private void recordBytecodeStart() {
        bytecodeToTargetCodePositionMap[currentOpcodePosition()] = codeBuffer.currentPosition();
    }

    private void startBlock(int bytecodePosition) {
        if (!blockStarts[bytecodePosition]) {
            numberOfBlocks++;
            blockStarts[bytecodePosition] = true;
        }
    }

    private void startBlocks(int[] bytecodePositions) {
        for (int bytecodePosition : bytecodePositions) {
            startBlock(bytecodePosition);
        }
    }

    protected void addForwardBranch(ForwardBranch branch) {
        forwardBranches.append(branch);
        startBlock(branch.targetBytecodePosition);
    }

    protected void addSwitch(Switch aSwitch) {
        switches.append(aSwitch);
        startBlock(aSwitch.defaultTargetBytecodePosition);
        startBlocks(aSwitch.targetBytecodePositions);
    }

    /**
     * Only call this after emitting all code.
     * @return the code position to which the JIT method returns in its optimized-to-JIT adapter
     */
    public abstract int adapterReturnPosition();

    /**
     * Indicates whether hotpath counters should be inserted at backward branch targets. We only insert these
     * if the Hotpath Compiler is enabled and we're not trace instrumenting.
     * @return {@code true} if hotpath instrumentation should be inserted
     */
    private boolean shouldInsertHotpathCounters() {
        return HotpathConfiguration.isEnabled() && !isTraceInstrumented;
    }

    /**
     * Identifies branch targets by scanning bytecodes using a {@link ControlFlowAdapter}. The identified branch target
     * locations will be used to insert {@link TreeAnchor} instrumentation.
     * @return a set of integers that denote branch targets
     */
    private Set<Integer> discoverBranchTargets() {
        final Set<Integer> targets = new HashSet<Integer>();
        final BytecodeScanner branchScanner = new BytecodeScanner(new ControlFlowAdapter() {
            @Override
            public void fallThrough(int address) {
                // Ignore
            }

            @Override
            public void jump(int address) {
                if (currentOpcodePosition() >= address) {
                    targets.add(address);
                }
            }

            @Override
            public void terminate() {
                // Ignore
            }
        });
        branchScanner.scan(classMethodActor);
        return targets;
    }

    private boolean shouldProfileMethodCall(MethodActor methodActor) {
        if (methodProfileBuilder != null && false) {
            // TODO: profiling of receivers is disabled for now
            if (methodActor instanceof InterfaceMethodActor) {
                return true;
            } else if (methodActor instanceof VirtualMethodActor) {
                final VirtualMethodActor virtualMethodActor = (VirtualMethodActor) methodActor;
                return !virtualMethodActor.isFinal() && !virtualMethodActor.holder().isFinal();
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the offset in the code buffer corresponding to a given bytecode offset.
     *
     * @param bytecodePosition the address of an instruction in the bytecode array
     * @return the offset (in the code buffer) of the code emitted for the bytecode instruction
     */
    protected int bytecodeToTargetCodePosition(int bytecodePosition) {
        return bytecodeToTargetCodePositionMap[bytecodePosition];
    }

    /**
     * Copies the code from a given template into the code buffer and updates the set of stops for the method being
     * translated with those derived from the template.
     *
     * @param template the compiled code to emit
     */
    protected void emitAndRecordStops(CompiledBytecodeTemplate template) {
        stops.add(template, codeBuffer.currentPosition(), currentOpcodePosition());
        codeBuffer.emit(template);
    }

    protected void emitSafepoint(BackwardBranchBytecodeSafepoint safepoint) {
        stops.add(safepoint);
    }

    /**
     * Register a direct (i.e. invokestatic or invokespecial) bytecode call that is not a runtime call.
     * @param template the compiled code
     * @param callee the method called
     */
    protected void recordDirectBytecodeCall(CompiledBytecodeTemplate template, ClassMethodActor callee) {
        assert template.targetMethod.numberOfDirectCalls() == 1;
        assert template.targetMethod.numberOfIndirectCalls() == 0;
        assert template.targetMethod.numberOfSafepoints() == 0;
        assert template.targetMethod.numberOfStopPositions() == 1;
        assert template.targetMethod.referenceMaps() == null || Bytes.areClear(template.targetMethod.referenceMaps());
        final int stopPosition = codeBuffer.currentPosition() + template.targetMethod.stopPosition(0);
        stops.add(new BytecodeDirectCall(stopPosition, currentOpcodePosition(), callee));
    }

    Stops packStops() {
        int firstTemplateSlot = jitStackFrameLayout.numberOfNonParameterSlots() + jitStackFrameLayout.numberOfOperandStackSlots();
        final int firstTemplateSlotIndexInFrameReferenceMap = firstTemplateSlot * JitStackFrameLayout.STACK_SLOTS_PER_JIT_SLOT;
        return stops.pack(frameReferenceMapSize(), registerReferenceMapSize(), firstTemplateSlotIndexInFrameReferenceMap);
    }

    /**
     * The information required to patch the target of a forward branch.
     */
    public static class ForwardBranch {

        public final BranchCondition condition;

        /**
         * The code buffer address of the branch instruction.
         */
        public final int targetCodePosition;

        /**
         * The bytecode position of the branch target.
         */
        public final int targetBytecodePosition;

        public ForwardBranch(BranchCondition condition, int targetCodePosition, int targetBytecodePosition) {
            this.condition = condition;
            this.targetCodePosition = targetCodePosition;
            this.targetBytecodePosition = targetBytecodePosition;
        }
    }

    /**
     * The information required to patch the targets of a tableswitch or lookupswitch.
     */
    public abstract static class Switch {

        /**
         * The bytecode position of the switch opcode.
         */
        public final int opcodePosition;

        /**
         * The bytecode position of the default switch target.
         */
        public final int defaultTargetBytecodePosition;

        /**
         * The bytecode positions of the non-default switch targets.
         */
        public final int[] targetBytecodePositions;

        public Switch(int opcodePosition, int defaultTargetBytecodePosition, int[] targetBytecodePositions) {
            this.opcodePosition = opcodePosition;
            this.defaultTargetBytecodePosition = defaultTargetBytecodePosition;
            this.targetBytecodePositions = targetBytecodePositions;
        }

        abstract void fixup(BytecodeToTargetTranslator translator);
    }

    /**
     * The information required to patch the targets of a tableswitch.
     */
    public static class TableSwitch extends Switch {

        public final int templateIndex;
        public final int templatePrefixSize;

        public TableSwitch(int opcodePosition, int templateIndex, int defaultTargetBytecodePosition, int[] targetBytecodePositions) {
            this(opcodePosition, templateIndex, defaultTargetBytecodePosition, targetBytecodePositions, 0);
        }

        public TableSwitch(int opcodePosition, int templateIndex, int defaultTargetBytecodePosition, int[] targetBytecodePositions, int templatePrefixSize) {
            super(opcodePosition, defaultTargetBytecodePosition, targetBytecodePositions);
            this.templateIndex = templateIndex;
            this.templatePrefixSize = templatePrefixSize;
        }

        public int templatePrefixSize() {
            return templatePrefixSize;
        }

        @Override
        void fixup(BytecodeToTargetTranslator translator) {
            translator.fixTableSwitch(this);
        }
    }

    /**
     * The information required to patch the targets of a lookupswitch.
     */
    public static class LookupSwitch extends Switch {

        public final int[] matches;

        public LookupSwitch(int opcodePosition, int defaultTargetBytecodePosition, int[] matches, int[] targetBytecodePositions) {
            super(opcodePosition, defaultTargetBytecodePosition, targetBytecodePositions);
            this.matches = matches;
        }

        @Override
        void fixup(BytecodeToTargetTranslator translator) {
            translator.fixLookupSwitch(this);
        }
    }

    protected abstract void fixForwardBranch(ForwardBranch forwardBranch);

    protected abstract void fixTableSwitch(TableSwitch tableSwitch);

    protected abstract void fixLookupSwitch(LookupSwitch lookupSwitch);

    protected void setGenerated(
            TargetMethod targetMethod,
            Stops stops,
            byte[] compressedJavaFrameDescriptors,
            byte[] scalarLiteralBytes,
            Object[] referenceLiterals,
            Object codeOrCodeBuffer,
            int optimizedCallerAdapterFrameCodeSize,
            int adapterReturnPosition,
            TargetABI abi) {

        final JitTargetMethod jitTargetMethod = (JitTargetMethod) targetMethod;
        jitTargetMethod.setGenerated(
            this.catchRangePositions,
            this.catchBlockPositions,
            stops.stopPositions,
            stops.bytecodeStopsIterator,
            compressedJavaFrameDescriptors,
            stops.directCallees,
            stops.numberOfIndirectCalls,
            stops.numberOfSafepoints,
            stops.referenceMaps,
            scalarLiteralBytes,
            referenceLiterals,
            codeOrCodeBuffer,
            optimizedCallerAdapterFrameCodeSize,
            adapterReturnPosition,
            inlineDataRecorder.encodedDescriptors(),
            stops.isDirectCallToRuntime,
            bytecodeToTargetCodePositionMap,
            bytecodeInfos,
            numberOfBlocks,
            blockStarts,
            jitStackFrameLayout,
            abi);
        if (methodProfileBuilder != null) {
            methodProfileBuilder.finish();
        }
    }

    public int numberOfCatchRanges() {
        return catchRangePositions == null ? 0 : catchRangePositions.length;
    }

    /**
     * Built catch and block position tables. The table covers the minimum contiguous ranges that covers all the handled
     * exceptions. Search from a target code position starts from the end of the catch table and ends when an entry that
     * contains a position smaller than the throw position is found. The last entry of the catch table always contains
     * the position to the instructions following the last instruction of the last catch range.
     *
     * @see TargetMethod#throwAddressToCatchAddress(boolean, com.sun.max.unsafe.Address, Class)
     */
    public void buildExceptionHandlingInfo() {
        final Sequence<ExceptionHandlerEntry> exceptionHandlers = classMethodActor.codeAttribute().exceptionHandlerTable();
        if (exceptionHandlers.isEmpty()) {
            return;
        }

        // Deal with simple, single-handler, case first.
        if (exceptionHandlers.length() == 1) {
            final ExceptionHandlerEntry einfo = exceptionHandlers.first();
            catchRangePositions = new int[] {bytecodeToTargetCodePosition(einfo.startPosition()), bytecodeToTargetCodePosition(einfo.endPosition())};
            catchBlockPositions = new int[] {bytecodeToTargetCodePosition(einfo.handlerPosition()), 0};
            return;
        }
        // Over-allocate to the maximum possible size (i.e., when no two ranges are contiguous)
        // The arrays will be trimmed later at the end.
        final int[] catchRangePosns = new int[exceptionHandlers.length() * 2 + 1];
        final int[] catchBlockPosns = new int[catchRangePosns.length];
        int index = 0;
        int nextRange = exceptionHandlers.first().startPosition();
        for (ExceptionHandlerEntry einfo : exceptionHandlers) {
            if (nextRange < einfo.startPosition()) {
                // There's a gap between the two catch ranges. Insert a range with no handler.
                catchRangePosns[index] = bytecodeToTargetCodePosition(nextRange);
                catchBlockPosns[index++] = 0;
            }
            catchRangePosns[index] = bytecodeToTargetCodePosition(einfo.startPosition());
            catchBlockPosns[index++] = bytecodeToTargetCodePosition(einfo.handlerPosition());
            nextRange = einfo.endPosition();
        }
        if (nextRange < classMethodActor.codeAttribute().code().length) {
            catchRangePosns[index] = bytecodeToTargetCodePosition(nextRange);
            catchBlockPosns[index++] = 0;
        }
        // Trim the arrays now.
        if (index < catchRangePosns.length) {
            catchRangePositions = java.util.Arrays.copyOf(catchRangePosns, index);
            catchBlockPositions = java.util.Arrays.copyOf(catchBlockPosns, index);
        }
    }

    protected abstract void emitBranch(BranchCondition branchCondition, int fromBytecodePosition, int toBytecodePosition);

    /**
     * Emits the code for a {@link Bytecode#TABLESWITCH} instruction.
     *
     * @param low the lower bound (inclusive) of the switch table case values
     * @param high the upper bound (inclusive) of the switch table case values
     * @param opcodePosition the bytecode position of the TABLESWITCH opcode
     * @param defaultTargetOffset the offset from {@code opcodePosition} for the default case
     * @param numberOfCases the number of cases in the switch
     */
    protected abstract void emitTableSwitch(int low, int high, int opcodePosition, int defaultTargetOffset, int numberOfCases);

    /**
     * Emits the code for a {@link Bytecode#LOOKUPSWITCH} instruction.
     *
     * @param opcodePosition the bytecode position of the LOOKUPSWITCH opcode
     * @param defaultTargetOffset the offset from {@code opcodePosition} for the default case
     * @param numberOfCases the number of the cases in the switch
     */
    protected abstract void emitLookupSwitch(int opcodePosition, int defaultTargetOffset, int numberOfCases);

    /**
     * @return the size of the adapter frame code found at the
     *         {@linkplain CallEntryPoint#OPTIMIZED_ENTRY_POINT entry point} for a call from a method compiled with the
     *         optimizing compiler
     */
    protected abstract int emitPrologue();

    /**
     * Does any fix up of branches and emit the epilogue (if any).
     */
    public void emitEpilogue() {
        // Record the end of the target code emitted for the last bytecode instruction
        final int targetCodePosition = codeBuffer.currentPosition();
        bytecodeToTargetCodePositionMap[bytecodeLength] = targetCodePosition;

        for (ForwardBranch forwardBranch : forwardBranches) {
            fixForwardBranch(forwardBranch);
        }

        for (Switch aSwitch : switches) {
            aSwitch.fixup(this);
        }
    }

    protected abstract void emitReturn();

    protected CompiledBytecodeTemplate getTemplate(Bytecode bytecode) {
        if (isTraceInstrumented) {
            return templateTable.get(bytecode, TemplateChooser.Selector.TRACED);
        }
        return templateTable.get(bytecode);
    }

    protected CompiledBytecodeTemplate getTemplate(Bytecode bytecode, TemplateChooser.Selector selector) {
        if (isTraceInstrumented) {
            return templateTable.get(bytecode, selector.copyAndModifySelector(Traced.YES));
        }
        return templateTable.get(bytecode, selector);
    }

    protected CompiledBytecodeTemplate getExactTemplate(Bytecode bytecode, TemplateChooser.Selector selector) {
        return templateTable.get(bytecode, selector);
    }

    protected CompiledBytecodeTemplate getTemplate(Bytecode bytecode, Kind kind) {
        if (isTraceInstrumented) {
            return templateTable.get(bytecode, kind, TemplateChooser.Selector.TRACED);
        }
        return templateTable.get(bytecode, kind);
    }

    protected CompiledBytecodeTemplate getTemplate(Bytecode bytecode, Kind kind, TemplateChooser.Selector selector) {
        if (isTraceInstrumented) {
            return templateTable.get(bytecode, kind, selector.copyAndModifySelector(Traced.YES));
        }
        return templateTable.get(bytecode, kind, selector);
    }

    public void emitEntrypointInstrumentation() {
        if (methodProfileBuilder != null) {
            methodProfileBuilder.addEntryCounter(JitInstrumentation.DEFAULT_ENTRY_INITIAL_COUNT);
            final CompiledBytecodeTemplate template = getExactTemplate(NOP, TemplateChooser.Selector.INSTRUMENTED);
            assignReferenceLiteralTemplateArgument(0, methodProfileBuilder.methodProfileObject());
            emitAndRecordStops(template);
        }
    }

    /**
     * Emit template for bytecode instruction with no operands. These bytecode have no dependencies, so emitting the
     * template just consists of copying the target instruction into the code buffer.
     * @param bytecode the bytecode for which to emit the template
     */
    protected void emitTemplateFor(Bytecode bytecode) {
        final CompiledBytecodeTemplate template = getTemplate(bytecode);
        beginBytecode(bytecode);
        emitAndRecordStops(template);
    }

    private void emitReturnFor(Bytecode returnInstruction) {
        emitTemplateFor(returnInstruction);
        emitReturn();
    }

    /**
     * Emits a template for a bytecode operating on a local variable (operand is an index to a local variable). The
     * template is customized so that the emitted code uses a specific local variable index.
     *
     * @param bytecode One of iload, istore, dload, dstore, fload, fstore, lload, lstore
     * @param localVariableIndex the local variable index to customize the template with.
     * @param kind the kind of the value in the local
     */
    protected void emitTemplateWithIndexFor(Bytecode bytecode, int localVariableIndex, Kind kind) {
        beginBytecode(bytecode);
        assignLocalDisplacementTemplateArgument(0, localVariableIndex, kind);
        final CompiledBytecodeTemplate template = getTemplate(bytecode);
        emitAndRecordStops(template);
    }

    protected abstract void assignIntTemplateArgument(int parameterIndex, int argument);

    protected abstract void assignFloatTemplateArgument(int parameterIndex, float argument);

    protected abstract void assignLongTemplateArgument(int parameterIndex, long argument);

    protected abstract void assignDoubleTemplateArgument(int parameterIndex, double argument);

    protected void assignLocalDisplacementTemplateArgument(int parameterIndex, int localIndex, Kind kind) {
        // Long locals (ones that use two slots in the locals area) take two slots,
        // as required by the jvm spec. The value of the long local is stored in
        // the second slot so that it can be loaded/stored without further adjustments
        // to the stack/base pointer offsets.
        final int slotIndex = kind.isCategory2() ? (localIndex + 1) : localIndex;
        final int slotOffset = jitStackFrameLayout.localVariableOffset(slotIndex) + JitStackFrameLayout.offsetInStackSlot(kind);
        assignIntTemplateArgument(parameterIndex, slotOffset);
    }

    protected abstract void loadTemplateArgumentRelativeToInstructionPointer(Kind kind, int parameterIndex, int offsetFromInstructionPointer);

    private PrependableSequence<Object> referenceLiterals = new LinkSequence<Object>();

    public final Object[] packReferenceLiterals() {
        if (referenceLiterals.isEmpty()) {
            return null;
        }
        if (MaxineVM.isHosted()) {
            return Sequence.Static.toArray(referenceLiterals, new Object[referenceLiterals.length()]);
        }
        // Must not cause checkcast here, since some reference literals may be static tuples.
        final Object[] result = new Object[referenceLiterals.length()];
        int i = 0;
        for (Object literal : referenceLiterals) {
            ArrayAccess.setObject(result, i, literal);
            i++;
        }
        return result;
    }

    /**
     * Compute the offset to a literal reference being created. The current position in the code buffer must be
     * that of the instruction loading the literal.
     *
     * @param numReferenceLiterals number of created reference literal (including the one being created).
     * @return an offset, in byte, to the base used to load literal.
     */
    protected abstract int computeReferenceLiteralOffset(int numReferenceLiterals);

    /**
     * Return the relative offset of the literal to the current code buffer position. (negative number since literals
     * are placed before code in the bundle)
     * @param literal the object
     * @return the offset of the literal relative to the current code position
     */
    protected int createReferenceLiteral(Object literal) {
        int literalOffset = computeReferenceLiteralOffset(1 + referenceLiterals.length());
        referenceLiterals.prepend(literal);
        if (MaxineVM.isDebug()) {
            // Account for the DebugHeap tag in front of the code object:
            literalOffset += VMConfiguration.target().wordWidth().numberOfBytes;
        }
        return -literalOffset;
    }

    protected void assignReferenceLiteralTemplateArgument(int parameterIndex, Object argument) {
        loadTemplateArgumentRelativeToInstructionPointer(Kind.REFERENCE, parameterIndex, createReferenceLiteral(argument));
    }

    protected boolean emitTemplateWithClassConstant(Bytecode bytecode, int index, boolean isArray) {
        final ClassConstant classConstant = constantPool.classAt(index);

        if (isResolved(classConstant)) {
            final CompiledBytecodeTemplate template = getTemplate(bytecode, TemplateChooser.Selector.RESOLVED);
            beginBytecode(bytecode);
            ClassActor resolvedClassActor = classConstant.resolve(constantPool, index);
            if (isArray) {
                resolvedClassActor = ArrayClassActor.forComponentClassActor(resolvedClassActor);
            }
            assignReferenceLiteralTemplateArgument(0, resolvedClassActor);
            emitAndRecordStops(template);
            return true;
        }
        final CompiledBytecodeTemplate template = getTemplate(bytecode);
        beginBytecode(bytecode);
        final ResolutionSnippet snippet = isArray ? ResolutionSnippet.ResolveArrayClass.SNIPPET : ResolutionSnippet.ResolveClass.SNIPPET;
        assignReferenceLiteralTemplateArgument(0, constantPool.makeResolutionGuard(index, snippet));
        emitAndRecordStops(template);
        return true;
    }

    /**
     * Emit template for a bytecode with a constant operand. The template is customized so that the emitted code use a
     * specific constant value.
     * @param bytecode the bytecode for which to emit the template
     * @param operand the integer argument to the template
     */
    protected void emitTemplateWithOperandFor(Bytecode bytecode, int operand) {
        beginBytecode(bytecode);
        assignIntTemplateArgument(0, operand);
        emitAndRecordStops(getTemplate(bytecode));
    }

    /**
     * Emit template for a bytecode operating on a (static or dynamic) field.
     *
     * @param bytecode one of getfield, putfield, getstatic, putstatic
     * @param index Index to the field ref constant.
     * @param snippet the resolution snippet to call
     */
    protected void emitTemplateForFieldAccess(Bytecode bytecode, int index, ResolutionSnippet snippet) {
        final FieldRefConstant fieldRefConstant = constantPool.fieldAt(index);
        final Kind fieldKind = fieldRefConstant.type(constantPool).toKind();
        if (isResolved(fieldRefConstant)) {
            try {
                final FieldActor fieldActor = fieldRefConstant.resolve(constantPool, index);
                CompiledBytecodeTemplate template;
                TemplateChooser.Selector selector;
                if (fieldActor.isStatic()) {
                    if (fieldActor.holder().isInitialized()) {
                        selector = TemplateChooser.Selector.INITIALIZED;
                        template = getTemplate(bytecode, fieldKind, selector);
                        beginBytecode(bytecode);
                        assignReferenceLiteralTemplateArgument(0, fieldActor.holder().staticTuple());
                        assignIntTemplateArgument(1, fieldActor.offset());
                        emitAndRecordStops(template);
                        return;
                    }
                } else {
                    selector = TemplateChooser.Selector.RESOLVED;
                    template = getTemplate(bytecode, fieldKind, selector);
                    beginBytecode(bytecode);
                    assignIntTemplateArgument(0, fieldActor.offset());
                    emitAndRecordStops(template);
                    return;
                }
            } catch (LinkageError e) {
                // This should not happen since the field ref constant is resolvable without class loading (i.e., it
                // has already been resolved). If it were to happen, the safe thing to do is to fall off to the
                // "no assumption" case, where a template for an unresolved class is taken instead. So do nothing here.
            }
        }
        final CompiledBytecodeTemplate template = templateTable.get(bytecode, fieldKind);
        beginBytecode(bytecode);
        assignReferenceLiteralTemplateArgument(0, constantPool.makeResolutionGuard(index, snippet));
        // Emit the template unmodified now. It will be modified in the end once all labels to literals are fixed.
        emitAndRecordStops(template);
    }

    /**
     * Gets the number of slots to be reserved as spill space for the templates from which this translated method is
     * composed. Note that this value may be conservative. That is, it may be greater than number of slots actually used
     * by any template in the translated method.
     * @return the number of template slots
     */
    protected int numberOfTemplateFrameSlots() {
        return jitStackFrameLayout.numberOfTemplateSlots();
    }

    /**
     * Gets the size of the reference maps covering the parts of the stack accessed by the method being translated.
     * This includes the parameter slots even though the space for them is allocated by the caller. These slots
     * are potentially reused by the method and thus may subsequently change the GC type.
     * @return the size of reference maps for this frame
     */
    public int frameReferenceMapSize() {
        return jitStackFrameLayout.frameReferenceMapSize();
    }

    protected abstract int registerReferenceMapSize();

    // Template generation
    // --------------------------

    @Override
    protected void aaload() {
        emitTemplateFor(AALOAD);
    }

    @Override
    protected void aastore() {
        emitTemplateFor(AASTORE);
    }

    @Override
    protected void aconst_null() {
        emitTemplateFor(ACONST_NULL);
    }

    @Override
    protected void aload(int index) {
        emitTemplateWithIndexFor(ALOAD, index, Kind.REFERENCE);
    }

    // Look ahead/behind helpers
    protected byte getByte(int byteAddress) {
        final BytecodeScanner bytecodeScanner = bytecodeScanner();
        return bytecodeScanner.bytecodeBlock().code()[byteAddress];
    }

    public int getUnsigned1(int byteAddress) {
        return getByte(byteAddress) & 0xff;
    }

    public int getUnsigned2(int byteAddress) {
        final int high = getByte(byteAddress) & 0xff;
        final int low = getByte(byteAddress + 1) & 0xff;
        return (high << 8) | low;
    }

    public Bytecode getNextBytecode() {
        return getBytecodeAt(bytecodeScanner().currentBytePosition());
    }

    public Bytecode getBytecodeAt(int opcodeAddress) {
        return Bytecode.from(getUnsigned1(opcodeAddress));
    }

    @Override
    protected void aload_0() {
        emitTemplateWithIndexFor(ALOAD, 0, Kind.REFERENCE);
    }

    @Override
    protected void aload_1() {
        emitTemplateWithIndexFor(ALOAD, 1, Kind.REFERENCE);
    }

    @Override
    protected void aload_2() {
        emitTemplateWithIndexFor(ALOAD, 2, Kind.REFERENCE);
    }

    @Override
    protected void aload_3() {
        emitTemplateWithIndexFor(ALOAD, 3, Kind.REFERENCE);
    }

    @Override
    protected void anewarray(int index) {
        emitTemplateWithClassConstant(ANEWARRAY, index, true);
    }

    @Override
    protected void areturn() {
        emitReturnFor(ARETURN);
    }

    @Override
    protected void arraylength() {
        emitTemplateFor(ARRAYLENGTH);
    }

    @Override
    protected void astore(int index) {
        emitTemplateWithIndexFor(ASTORE, index, Kind.REFERENCE);
    }

    @Override
    protected void astore_0() {
        emitTemplateWithIndexFor(ASTORE, 0, Kind.REFERENCE);
    }

    @Override
    protected void astore_1() {
        emitTemplateWithIndexFor(ASTORE, 1, Kind.REFERENCE);
    }

    @Override
    protected void astore_2() {
        emitTemplateWithIndexFor(ASTORE, 2, Kind.REFERENCE);
    }

    @Override
    protected void astore_3() {
        emitTemplateWithIndexFor(ASTORE, 3, Kind.REFERENCE);
    }

    @Override
    protected void athrow() {
        emitTemplateFor(ATHROW);
    }

    @Override
    protected void baload() {
        emitTemplateFor(BALOAD);
    }

    @Override
    protected void bastore() {
        emitTemplateFor(BASTORE);
    }

    @Override
    protected void bipush(int operand) {
        emitTemplateWithOperandFor(BIPUSH, operand);
    }

    @Override
    protected void breakpoint() {
        emitTemplateFor(BREAKPOINT);
    }

    @Override
    protected void caload() {
        emitTemplateFor(CALOAD);
    }

    @Override
    protected void castore() {
        emitTemplateFor(CASTORE);
    }

    @Override
    protected void checkcast(int index) {
        emitTemplateWithClassConstant(CHECKCAST, index, false);
    }

    @Override
    protected void d2f() {
        emitTemplateFor(D2F);
    }

    @Override
    protected void d2i() {
        emitTemplateFor(D2I);
    }

    @Override
    protected void d2l() {
        emitTemplateFor(D2L);
    }

    @Override
    protected void dadd() {
        emitTemplateFor(DADD);
    }

    @Override
    protected void daload() {
        emitTemplateFor(DALOAD);
    }

    @Override
    protected void dastore() {
        emitTemplateFor(DASTORE);
    }

    @Override
    protected void dcmpg() {
        emitTemplateFor(DCMPG);
    }

    @Override
    protected void dcmpl() {
        emitTemplateFor(DCMPL);
    }

    @Override
    protected void dconst_0() {
        emitDoubleConstantTemplate(DCONST_0, 0D);
    }

    @Override
    protected void dconst_1() {
        emitDoubleConstantTemplate(DCONST_1, 1D);
    }

    private void emitDoubleConstantTemplate(Bytecode bytecode, final double doubleValue) {
        beginBytecode(bytecode);
        assignDoubleTemplateArgument(0, doubleValue);
        final CompiledBytecodeTemplate template = getTemplate(bytecode);
        emitAndRecordStops(template);
    }

    @Override
    protected void ddiv() {
        emitTemplateFor(DDIV);
    }

    @Override
    protected void dload(int index) {
        emitTemplateWithIndexFor(DLOAD, index, Kind.DOUBLE);
    }

    @Override
    protected void dload_0() {
        emitTemplateWithIndexFor(DLOAD, 0, Kind.DOUBLE);
    }

    @Override
    protected void dload_1() {
        emitTemplateWithIndexFor(DLOAD, 1, Kind.DOUBLE);
    }

    @Override
    protected void dload_2() {
        emitTemplateWithIndexFor(DLOAD, 2, Kind.DOUBLE);
    }

    @Override
    protected void dload_3() {
        emitTemplateWithIndexFor(DLOAD, 3, Kind.DOUBLE);
    }

    @Override
    protected void dmul() {
        emitTemplateFor(DMUL);
    }

    @Override
    protected void dneg() {
        final CompiledBytecodeTemplate template = getTemplate(DNEG);
        beginBytecode(DNEG);
        assignDoubleTemplateArgument(0, 0D);
        emitAndRecordStops(template);
    }

    @Override
    protected void drem() {
        emitTemplateFor(DREM);
    }

    @Override
    protected void dreturn() {
        emitReturnFor(DRETURN);
    }

    @Override
    protected void dstore(int index) {
        emitTemplateWithIndexFor(DSTORE, index, Kind.DOUBLE);
    }

    @Override
    protected void dstore_0() {
        emitTemplateWithIndexFor(DSTORE, 0, Kind.DOUBLE);
    }

    @Override
    protected void dstore_1() {
        emitTemplateWithIndexFor(DSTORE, 1, Kind.DOUBLE);
    }

    @Override
    protected void dstore_2() {
        emitTemplateWithIndexFor(DSTORE, 2, Kind.DOUBLE);
    }

    @Override
    protected void dstore_3() {
        emitTemplateWithIndexFor(DSTORE, 3, Kind.DOUBLE);
    }

    @Override
    protected void dsub() {
        emitTemplateFor(DSUB);
    }

    @Override
    protected void dup() {
        emitTemplateFor(DUP);
    }

    @Override
    protected void dup2() {
        emitTemplateFor(DUP2);
    }

    @Override
    protected void dup2_x1() {
        emitTemplateFor(DUP2_X1);
    }

    @Override
    protected void dup2_x2() {
        emitTemplateFor(DUP2_X2);
    }

    @Override
    protected void dup_x1() {
        emitTemplateFor(DUP_X1);
    }

    @Override
    protected void dup_x2() {
        emitTemplateFor(DUP_X2);
    }

    @Override
    protected void f2d() {
        emitTemplateFor(F2D);
    }

    @Override
    protected void f2i() {
        emitTemplateFor(F2I);
    }

    @Override
    protected void f2l() {
        emitTemplateFor(F2L);
    }

    @Override
    protected void fadd() {
        emitTemplateFor(FADD);
    }

    @Override
    protected void faload() {
        emitTemplateFor(FALOAD);
    }

    @Override
    protected void fastore() {
        emitTemplateFor(FASTORE);
    }

    @Override
    protected void fcmpg() {
        emitTemplateFor(FCMPG);
    }

    @Override
    protected void fcmpl() {
        emitTemplateFor(FCMPL);
    }

    @Override
    protected void fconst_0() {
        emitFloatConstantTemplate(FCONST_0, 0F);
    }

    @Override
    protected void fconst_1() {
        emitFloatConstantTemplate(FCONST_1, 1F);
    }

    @Override
    protected void fconst_2() {
        emitFloatConstantTemplate(FCONST_2, 2F);
    }

    private void emitFloatConstantTemplate(final Bytecode bytecode, final float floatConstant) {
        beginBytecode(bytecode);
        assignFloatTemplateArgument(0, floatConstant);
        final CompiledBytecodeTemplate template = getTemplate(bytecode);
        emitAndRecordStops(template);
    }

    @Override
    protected void fdiv() {
        emitTemplateFor(FDIV);
    }

    @Override
    protected void fload(int index) {
        emitTemplateWithIndexFor(FLOAD, index, Kind.FLOAT);
    }

    @Override
    protected void fload_0() {
        emitTemplateWithIndexFor(FLOAD, 0, Kind.FLOAT);
    }

    @Override
    protected void fload_1() {
        emitTemplateWithIndexFor(FLOAD, 1, Kind.FLOAT);
    }

    @Override
    protected void fload_2() {
        emitTemplateWithIndexFor(FLOAD, 2, Kind.FLOAT);
    }

    @Override
    protected void fload_3() {
        emitTemplateWithIndexFor(FLOAD, 3, Kind.FLOAT);
    }

    @Override
    protected void fmul() {
        emitTemplateFor(FMUL);
    }

    @Override
    protected void fneg() {
        final CompiledBytecodeTemplate template = getTemplate(FNEG);
        beginBytecode(FNEG);
        assignFloatTemplateArgument(0, 0F);
        emitAndRecordStops(template);
    }

    @Override
    protected void frem() {
        emitTemplateFor(FREM);
    }

    @Override
    protected void freturn() {
        emitReturnFor(FRETURN);
    }

    @Override
    protected void fstore(int index) {
        emitTemplateWithIndexFor(FSTORE, index, Kind.FLOAT);
    }

    @Override
    protected void fstore_0() {
        emitTemplateWithIndexFor(FSTORE, 0, Kind.FLOAT);
    }

    @Override
    protected void fstore_1() {
        emitTemplateWithIndexFor(FSTORE, 1, Kind.FLOAT);
    }

    @Override
    protected void fstore_2() {
        emitTemplateWithIndexFor(FSTORE, 2, Kind.FLOAT);
    }

    @Override
    protected void fstore_3() {
        emitTemplateWithIndexFor(FSTORE, 3, Kind.FLOAT);
    }

    @Override
    protected void fsub() {
        emitTemplateFor(FSUB);
    }

    @Override
    protected void getfield(int index) {
        emitTemplateForFieldAccess(GETFIELD, index, ResolveInstanceFieldForReading.SNIPPET);
    }

    @Override
    protected void getstatic(int index) {
        emitTemplateForFieldAccess(GETSTATIC, index, ResolveStaticFieldForReading.SNIPPET);
    }

    private void emitConditionalBranch(BranchCondition condition, Bytecode bytecode, int offset) {
        final int currentBytecodePosition = currentOpcodePosition();
        final int targetBytecodePosition = currentBytecodePosition + offset;
        startBlock(targetBytecodePosition);
        // emit prefix of the bytecodeinstruction.
        final CompiledBytecodeTemplate template = getTemplate(bytecode);
        beginBytecode(bytecode);
        assert template.targetMethod.directCallees() == null || isTraceInstrumented;
        emitAndRecordStops(template);
        emitBranch(condition, currentBytecodePosition, targetBytecodePosition);
    }

    private void emitUncondtionalBranch(Bytecode bytecode, int offset) {
        int currentBytecodePosition = currentOpcodePosition();
        int targetBytecodePosition = currentBytecodePosition + offset;
        startBlock(targetBytecodePosition);
        beginBytecode(bytecode);
        emitBranch(BranchCondition.NONE, currentBytecodePosition, targetBytecodePosition);
    }

    @Override
    protected void goto_(int offset) {
        emitUncondtionalBranch(GOTO, offset);
    }

    @Override
    protected void goto_w(int offset) {
        emitUncondtionalBranch(GOTO_W, offset);
    }

    @Override
    protected void jsr_w(int offset) {
        jsr(offset);
    }

    @Override
    protected void i2b() {
        emitTemplateFor(I2B);
    }

    @Override
    protected void i2c() {
        emitTemplateFor(I2C);
    }

    @Override
    protected void i2d() {
        emitTemplateFor(I2D);
    }

    @Override
    protected void i2f() {
        emitTemplateFor(I2F);
    }

    @Override
    protected void i2l() {
        emitTemplateFor(I2L);
    }

    @Override
    protected void i2s() {
        emitTemplateFor(I2S);
    }

    @Override
    protected void iadd() {
        emitTemplateFor(IADD);
    }

    @Override
    protected void iaload() {
        emitTemplateFor(IALOAD);
    }

    @Override
    protected void iand() {
        emitTemplateFor(IAND);
    }

    @Override
    protected void iastore() {
        emitTemplateFor(IASTORE);
    }

    @Override
    protected void iconst_0() {
        emitTemplateFor(ICONST_0);
    }

    @Override
    protected void iconst_1() {
        emitTemplateFor(ICONST_1);
    }

    @Override
    protected void iconst_2() {
        emitTemplateFor(ICONST_2);
    }

    @Override
    protected void iconst_3() {
        emitTemplateFor(ICONST_3);
    }

    @Override
    protected void iconst_4() {
        emitTemplateFor(ICONST_4);
    }

    @Override
    protected void iconst_5() {
        emitTemplateFor(ICONST_5);
    }

    @Override
    protected void iconst_m1() {
        emitTemplateFor(ICONST_M1);
    }

    @Override
    protected void idiv() {
        emitTemplateFor(IDIV);
    }

    @Override
    protected void if_acmpeq(int offset) {
        emitConditionalBranch(BranchCondition.EQ, IF_ACMPEQ, offset);
    }

    @Override
    protected void if_acmpne(int offset) {
        emitConditionalBranch(BranchCondition.NE, IF_ACMPNE, offset);
    }

    @Override
    protected void if_icmpeq(int offset) {
        emitConditionalBranch(BranchCondition.EQ, IF_ICMPEQ, offset);
    }

    @Override
    protected void if_icmpge(int offset) {
        emitConditionalBranch(BranchCondition.GE, IF_ICMPGE, offset);
    }

    @Override
    protected void if_icmpgt(int offset) {
        emitConditionalBranch(BranchCondition.GT, IF_ICMPGT, offset);
    }

    @Override
    protected void if_icmple(int offset) {
        emitConditionalBranch(BranchCondition.LE, IF_ICMPLE, offset);
    }

    @Override
    protected void if_icmplt(int offset) {
        emitConditionalBranch(BranchCondition.LT, IF_ICMPLT, offset);
    }

    @Override
    protected void if_icmpne(int offset) {
        emitConditionalBranch(BranchCondition.NE, IF_ICMPNE, offset);
    }

    @Override
    protected void ifeq(int offset) {
        emitConditionalBranch(BranchCondition.EQ, IFEQ, offset);
    }

    @Override
    protected void ifge(int offset) {
        emitConditionalBranch(BranchCondition.GE, IFGE, offset);
    }

    @Override
    protected void ifgt(int offset) {
        emitConditionalBranch(BranchCondition.GT, IFGT, offset);
    }

    @Override
    protected void ifle(int offset) {
        emitConditionalBranch(BranchCondition.LE, IFLE, offset);
    }

    @Override
    protected void iflt(int offset) {
        emitConditionalBranch(BranchCondition.LT, IFLT, offset);
    }

    @Override
    protected void ifne(int offset) {
        emitConditionalBranch(BranchCondition.NE, IFNE, offset);
    }

    @Override
    protected void ifnonnull(int offset) {
        emitConditionalBranch(BranchCondition.NE, IFNONNULL, offset);
    }

    @Override
    protected void ifnull(int offset) {
        emitConditionalBranch(BranchCondition.EQ, IFNULL, offset);
    }

    @Override
    protected void iinc(int index, int increment) {
        beginBytecode(IINC);
        final CompiledBytecodeTemplate template = getTemplate(IINC);
        assignLocalDisplacementTemplateArgument(0, index, Kind.INT);
        assignIntTemplateArgument(1, increment);
        emitAndRecordStops(template);
    }

    @Override
    protected void iload(int index) {
        emitTemplateWithIndexFor(ILOAD, index, Kind.INT);
    }

    @Override
    protected void iload_0() {
        emitTemplateWithIndexFor(ILOAD, 0, Kind.INT);
    }

    @Override
    protected void iload_1() {
        emitTemplateWithIndexFor(ILOAD, 1, Kind.INT);
    }

    @Override
    protected void iload_2() {
        emitTemplateWithIndexFor(ILOAD, 2, Kind.INT);
    }

    @Override
    protected void iload_3() {
        emitTemplateWithIndexFor(ILOAD, 3, Kind.INT);
    }

    @Override
    protected void imul() {
        emitTemplateFor(IMUL);
    }

    @Override
    protected void ineg() {
        emitTemplateFor(INEG);
    }

    @Override
    protected void instanceof_(int index) {
        emitTemplateWithClassConstant(INSTANCEOF, index, false);
    }

    private static int receiverStackIndex(SignatureDescriptor signatureDescriptor) {
        int index = 0;
        for (int i = 0; i < signatureDescriptor.numberOfParameters(); i++) {
            final Kind kind = signatureDescriptor.parameterDescriptorAt(i).toKind();
            index += kind.stackSlots();
        }
        return index;
    }

    @Override
    protected void invokevirtual(int index) {
        final ClassMethodRefConstant classMethodRef = constantPool.classMethodAt(index);
        final SignatureDescriptor signatureDescriptor = classMethodRef.signature(constantPool);
        final Kind selectorKind = invokeSelectorKind(signatureDescriptor);

        try {
            if (isResolved(classMethodRef)) {
                try {
                    final VirtualMethodActor virtualMethodActor = classMethodRef.resolveVirtual(constantPool, index);
                    if (shouldProfileMethodCall(virtualMethodActor)) {
                        final CompiledBytecodeTemplate template = getTemplate(INVOKEVIRTUAL, selectorKind, TemplateChooser.Selector.RESOLVED_INSTRUMENTED);
                        beginBytecode(INVOKEVIRTUAL);
                        final int vtableIndex = virtualMethodActor.vTableIndex();
                        assignIntTemplateArgument(0, vtableIndex);
                        assignIntTemplateArgument(1, receiverStackIndex(signatureDescriptor));
                        assignReferenceLiteralTemplateArgument(2, methodProfileBuilder.methodProfileObject());
                        assignIntTemplateArgument(3, methodProfileBuilder.addMethodProfile(index, JitInstrumentation.DEFAULT_RECEIVER_METHOD_PROFILE_ENTRIES));
                        emitAndRecordStops(template);
                    } else {
                        final CompiledBytecodeTemplate template = getTemplate(INVOKEVIRTUAL, selectorKind, TemplateChooser.Selector.RESOLVED);
                        beginBytecode(INVOKEVIRTUAL);
                        assignIntTemplateArgument(0, virtualMethodActor.vTableIndex());
                        assignIntTemplateArgument(1, receiverStackIndex(signatureDescriptor));
                        emitAndRecordStops(template);
                    }
                    return;
                } catch (LinkageError e) {
                    // fall through
                }
            }
        } catch (LinkageError error) {
            // Fall back on unresolved template that will cause the error to be rethrown at runtime.
        }
        final CompiledBytecodeTemplate template = getTemplate(INVOKEVIRTUAL, selectorKind, TemplateChooser.Selector.NO_ASSUMPTION);
        beginBytecode(INVOKEVIRTUAL);
        assignReferenceLiteralTemplateArgument(0, constantPool.makeResolutionGuard(index, ResolveVirtualMethod.SNIPPET));
        assignIntTemplateArgument(1, receiverStackIndex(signatureDescriptor));
        emitAndRecordStops(template);
    }

    @Override
    protected void invokeinterface(int index, int count) {
        final InterfaceMethodRefConstant interfaceMethodRef = constantPool.interfaceMethodAt(index);
        final SignatureDescriptor signatureDescriptor = interfaceMethodRef.signature(constantPool);
        final Kind selectorKind = invokeSelectorKind(signatureDescriptor);
        try {
            if (isResolved(interfaceMethodRef)) {
                try {
                    final InterfaceMethodActor interfaceMethodActor = (InterfaceMethodActor) interfaceMethodRef.resolve(constantPool, index);
                    if (shouldProfileMethodCall(interfaceMethodActor)) {
                        final CompiledBytecodeTemplate template = getTemplate(INVOKEINTERFACE, selectorKind, TemplateChooser.Selector.RESOLVED_INSTRUMENTED);
                        beginBytecode(INVOKEINTERFACE);
                        assignReferenceLiteralTemplateArgument(0, interfaceMethodActor);
                        assignIntTemplateArgument(1, receiverStackIndex(signatureDescriptor));
                        assignReferenceLiteralTemplateArgument(2, methodProfileBuilder.methodProfileObject());
                        assignIntTemplateArgument(3, methodProfileBuilder.addMethodProfile(index, JitInstrumentation.DEFAULT_RECEIVER_METHOD_PROFILE_ENTRIES));
                        emitAndRecordStops(template);
                    } else {
                        final CompiledBytecodeTemplate template = getTemplate(INVOKEINTERFACE, selectorKind, TemplateChooser.Selector.RESOLVED);
                        beginBytecode(INVOKEINTERFACE);
                        assignReferenceLiteralTemplateArgument(0, interfaceMethodActor);
                        assignIntTemplateArgument(1, receiverStackIndex(signatureDescriptor));
                        emitAndRecordStops(template);
                    }
                    return;
                } catch (LinkageError e) {
                    // fall through
                }
            }
        } catch (LinkageError error) {
            // Fall back on unresolved template that will cause the error to be rethrown at runtime.
        }
        final CompiledBytecodeTemplate template = getTemplate(INVOKEINTERFACE, selectorKind);
        beginBytecode(INVOKEINTERFACE);
        assignReferenceLiteralTemplateArgument(0, constantPool.makeResolutionGuard(index, ResolveInterfaceMethod.SNIPPET));
        assignIntTemplateArgument(1, receiverStackIndex(signatureDescriptor));
        emitAndRecordStops(template);
    }

    @Override
    protected void callnative(int nativeFunctionDescriptorIndex) {
        // Native method stubs must be compiled with the optimizing compiler.
        throw ProgramError.unexpected();
    }

    private Kind invokeSelectorKind(SignatureDescriptor signatureDescriptor) {
        final Kind resultKind = signatureDescriptor.resultKind();
        switch (resultKind.asEnum) {
            case DOUBLE:
            case LONG:
            case FLOAT:
            case VOID:
                return resultKind;
            default:
                return Kind.WORD;
        }
    }

    @Override
    protected void invokespecial(int index) {
        final ClassMethodRefConstant classMethodRef = constantPool.classMethodAt(index);
        final Kind selectorKind = invokeSelectorKind(classMethodRef.signature(constantPool));

        try {
            if (isResolved(classMethodRef)) {
                final VirtualMethodActor virtualMethodActor = classMethodRef.resolveVirtual(constantPool, index);
                final CompiledBytecodeTemplate template = getTemplate(INVOKESPECIAL, selectorKind, TemplateChooser.Selector.RESOLVED);
                beginBytecode(INVOKESPECIAL);
                recordDirectBytecodeCall(template, virtualMethodActor);
                codeBuffer.emit(template);
                return;
            }
        } catch (LinkageError error) {
            // Fall back on unresolved template that will cause the error to be rethrown at runtime.
        }
        final CompiledBytecodeTemplate template = getTemplate(INVOKESPECIAL, selectorKind, TemplateChooser.Selector.NO_ASSUMPTION);
        beginBytecode(INVOKESPECIAL);
        assignReferenceLiteralTemplateArgument(0, constantPool.makeResolutionGuard(index, ResolveSpecialMethod.SNIPPET));
        emitAndRecordStops(template);
    }

    @Override
    protected void invokestatic(int index) {

        final ClassMethodRefConstant classMethodRef = constantPool.classMethodAt(index);
        final Kind selectorKind = invokeSelectorKind(classMethodRef.signature(constantPool));
        try {
            if (isResolved(classMethodRef)) {
                final StaticMethodActor staticMethodActor = classMethodRef.resolveStatic(constantPool, index);
                if (staticMethodActor.holder().isInitialized()) {
                    final CompiledBytecodeTemplate template = getTemplate(INVOKESTATIC, selectorKind, TemplateChooser.Selector.INITIALIZED);
                    beginBytecode(INVOKESTATIC);
                    recordDirectBytecodeCall(template, staticMethodActor);
                    codeBuffer.emit(template);
                    return;
                }
            }
        } catch (LinkageError error) {
            // Fall back on unresolved template that will cause the error to be rethrown at runtime.
        }
        final CompiledBytecodeTemplate template = getTemplate(INVOKESTATIC, selectorKind, TemplateChooser.Selector.NO_ASSUMPTION);
        beginBytecode(INVOKESTATIC);
        assignReferenceLiteralTemplateArgument(0, constantPool.makeResolutionGuard(index, ResolveStaticMethod.SNIPPET));
        emitAndRecordStops(template);
    }

    @Override
    protected void ior() {
        emitTemplateFor(IOR);
    }

    @Override
    protected void irem() {
        emitTemplateFor(IREM);
    }

    @Override
    protected void ireturn() {
        emitReturnFor(IRETURN);
    }

    @Override
    protected void ishl() {
        emitTemplateFor(ISHL);
    }

    @Override
    protected void ishr() {
        emitTemplateFor(ISHR);
    }

    @Override
    protected void istore(int index) {
        emitTemplateWithIndexFor(ISTORE, index, Kind.INT);
    }

    @Override
    protected void istore_0() {
        emitTemplateWithIndexFor(ISTORE, 0, Kind.INT);
    }

    @Override
    protected void istore_1() {
        emitTemplateWithIndexFor(ISTORE, 1, Kind.INT);
    }

    @Override
    protected void istore_2() {
        emitTemplateWithIndexFor(ISTORE, 2, Kind.INT);
    }

    @Override
    protected void istore_3() {
        emitTemplateWithIndexFor(ISTORE, 3, Kind.INT);
    }

    @Override
    protected void isub() {
        emitTemplateFor(ISUB);
    }

    @Override
    protected void iushr() {
        emitTemplateFor(IUSHR);
    }

    @Override
    protected void ixor() {
        emitTemplateFor(IXOR);
    }

    @Override
    protected void jsr(int offset) {
        ProgramError.unexpected();
    }

    @Override
    protected void l2d() {
        emitTemplateFor(L2D);
    }

    @Override
    protected void l2f() {
        emitTemplateFor(L2F);
    }

    @Override
    protected void l2i() {
        emitTemplateFor(L2I);
    }

    @Override
    protected void ladd() {
        emitTemplateFor(LADD);
    }

    @Override
    protected void laload() {
        emitTemplateFor(LALOAD);
    }

    @Override
    protected void land() {
        emitTemplateFor(LAND);
    }

    @Override
    protected void lastore() {
        emitTemplateFor(LASTORE);
    }

    @Override
    protected void lcmp() {
        emitTemplateFor(LCMP);
    }

    @Override
    protected void lconst_0() {
        beginBytecode(LCONST_0);
        assignLongTemplateArgument(0, 0L);
        final CompiledBytecodeTemplate template = getTemplate(LCONST_0);
        emitAndRecordStops(template);
    }

    @Override
    protected void lconst_1() {
        beginBytecode(LCONST_1);
        assignLongTemplateArgument(0, 1L);
        final CompiledBytecodeTemplate template = getTemplate(LCONST_1);
        emitAndRecordStops(template);
    }

    @Override
    protected void ldc(int index) {
        final PoolConstant constant = constantPool.at(index);
        switch (constant.tag()) {
            case CLASS: {
                final ClassConstant classConstant = (ClassConstant) constant;
                if (isResolved(classConstant)) {
                    final CompiledBytecodeTemplate template = getTemplate(LDC, Kind.REFERENCE, TemplateChooser.Selector.RESOLVED);
                    beginBytecode(LDC);
                    final Object mirror = ((ClassActor) classConstant.value(constantPool, index).asObject()).mirror();
                    assignReferenceLiteralTemplateArgument(0, mirror);
                    emitAndRecordStops(template);
                } else {
                    final CompiledBytecodeTemplate template = getTemplate(LDC, Kind.REFERENCE, TemplateChooser.Selector.NO_ASSUMPTION);
                    beginBytecode(LDC);
                    assignReferenceLiteralTemplateArgument(0, constantPool.makeResolutionGuard(index, ResolveClass.SNIPPET));
                    emitAndRecordStops(template);
                }
                break;
            }
            case INTEGER: {
                final CompiledBytecodeTemplate template = getTemplate(LDC, Kind.INT, TemplateChooser.Selector.NO_ASSUMPTION);
                beginBytecode(LDC);
                final IntegerConstant integerConstant = (IntegerConstant) constant;
                assignIntTemplateArgument(0, integerConstant.value());
                emitAndRecordStops(template);
                break;
            }
            case LONG: {
                final CompiledBytecodeTemplate template = getTemplate(LDC, Kind.LONG, TemplateChooser.Selector.NO_ASSUMPTION);
                beginBytecode(LDC);
                final LongConstant longConstant = (LongConstant) constant;
                assignLongTemplateArgument(0, longConstant.value());
                emitAndRecordStops(template);
                break;
            }
            case FLOAT: {
                final CompiledBytecodeTemplate template = getTemplate(LDC, Kind.FLOAT, TemplateChooser.Selector.NO_ASSUMPTION);
                beginBytecode(LDC);
                final FloatConstant floatConstant = (FloatConstant) constant;
                assignFloatTemplateArgument(0, floatConstant.value());
                emitAndRecordStops(template);
                break;
            }
            case DOUBLE: {
                final CompiledBytecodeTemplate template = getTemplate(LDC, Kind.DOUBLE, TemplateChooser.Selector.NO_ASSUMPTION);
                beginBytecode(LDC);
                final DoubleConstant doubleConstant = (DoubleConstant) constant;
                assignDoubleTemplateArgument(0, doubleConstant.value());
                emitAndRecordStops(template);
                break;
            }
            case STRING: {
                final CompiledBytecodeTemplate template = getTemplate(LDC, Kind.REFERENCE, TemplateChooser.Selector.RESOLVED);
                beginBytecode(LDC);
                final StringConstant stringConstant = (StringConstant) constant;
                assignReferenceLiteralTemplateArgument(0, stringConstant.value);
                emitAndRecordStops(template);
                break;
            }
            default: {
                assert false : "ldc for unexpected constant tag: " + constant.tag();
                break;
            }
        }
    }

    @Override
    protected void ldc2_w(int index) {
        ldc(index);
    }

    @Override
    protected void ldc_w(int index) {
        ldc(index);
    }

    @Override
    protected void ldiv() {
        emitTemplateFor(LDIV);
    }

    @Override
    protected void lload(int index) {
        emitTemplateWithIndexFor(LLOAD, index, Kind.LONG);
    }

    @Override
    protected void lload_0() {
        emitTemplateWithIndexFor(LLOAD, 0, Kind.LONG);
    }

    @Override
    protected void lload_1() {
        emitTemplateWithIndexFor(LLOAD, 1, Kind.LONG);
    }

    @Override
    protected void lload_2() {
        emitTemplateWithIndexFor(LLOAD, 2, Kind.LONG);
    }

    @Override
    protected void lload_3() {
        emitTemplateWithIndexFor(LLOAD, 3, Kind.LONG);
    }

    @Override
    protected void lmul() {
        emitTemplateFor(LMUL);
    }

    @Override
    protected void lneg() {
        emitTemplateFor(LNEG);
    }

    @Override
    protected void lookupswitch(int defaultOffset, int numberOfCases) {
        final int opcodePosition = currentOpcodePosition();
        beginBytecode(LOOKUPSWITCH);
        emitLookupSwitch(opcodePosition, defaultOffset, numberOfCases);
    }

    @Override
    protected void lor() {
        emitTemplateFor(LOR);
    }

    @Override
    protected void lrem() {
        emitTemplateFor(LREM);
    }

    @Override
    protected void lreturn() {
        emitReturnFor(LRETURN);
    }

    @Override
    protected void lshl() {
        emitTemplateFor(LSHL);
    }

    @Override
    protected void lshr() {
        emitTemplateFor(LSHR);
    }

    @Override
    protected void lstore(int index) {
        emitTemplateWithIndexFor(LSTORE, index, Kind.LONG);
    }

    @Override
    protected void lstore_0() {
        emitTemplateWithIndexFor(LSTORE, 0, Kind.LONG);
    }

    @Override
    protected void lstore_1() {
        emitTemplateWithIndexFor(LSTORE, 1, Kind.LONG);
    }

    @Override
    protected void lstore_2() {
        emitTemplateWithIndexFor(LSTORE, 2, Kind.LONG);
    }

    @Override
    protected void lstore_3() {
        emitTemplateWithIndexFor(LSTORE, 3, Kind.LONG);
    }

    @Override
    protected void lsub() {
        emitTemplateFor(LSUB);
    }

    @Override
    protected void lushr() {
        emitTemplateFor(LUSHR);
    }

    @Override
    protected void lxor() {
        emitTemplateFor(LXOR);
    }

    @Override
    protected void monitorenter() {
        emitTemplateFor(MONITORENTER);
    }

    @Override
    protected void monitorexit() {
        emitTemplateFor(MONITOREXIT);
    }

    @Override
    protected void multianewarray(int index, final int numberOfDimensions) {
        final ClassConstant classRef = constantPool.classAt(index);
        if (isResolved(classRef)) {
            final CompiledBytecodeTemplate template = getTemplate(MULTIANEWARRAY, TemplateChooser.Selector.RESOLVED);
            beginBytecode(MULTIANEWARRAY);
            final ClassActor arrayClassActor = classRef.resolve(constantPool, index);
            assert arrayClassActor.isArrayClassActor();
            assert arrayClassActor.numberOfDimensions() >= numberOfDimensions : "dimensionality of array class constant smaller that dimension operand";
            assignReferenceLiteralTemplateArgument(0, arrayClassActor);
            assignReferenceLiteralTemplateArgument(1, new int[numberOfDimensions]);
            // Emit the template
            emitAndRecordStops(template);
            return; // we're done.
        }
        // Unresolved case
        final CompiledBytecodeTemplate template = getTemplate(MULTIANEWARRAY);
        beginBytecode(MULTIANEWARRAY);
        assignReferenceLiteralTemplateArgument(0, constantPool.makeResolutionGuard(index, ResolveClass.SNIPPET));
        assignReferenceLiteralTemplateArgument(1, new int[numberOfDimensions]);
        emitAndRecordStops(template);
    }

    @Override
    protected void new_(int index) {
        final ClassConstant classRef = constantPool.classAt(index);
        if (isResolved(classRef)) {
            final ClassActor classActor = classRef.resolve(constantPool, index);
            if (classActor.isInitialized()) {
                final CompiledBytecodeTemplate template = getTemplate(NEW, TemplateChooser.Selector.INITIALIZED);
                beginBytecode(NEW);
                assignReferenceLiteralTemplateArgument(0, classActor);
                emitAndRecordStops(template);
                return;
            }
        }
        final CompiledBytecodeTemplate template = getTemplate(NEW, TemplateChooser.Selector.NO_ASSUMPTION);
        beginBytecode(NEW);
        assignReferenceLiteralTemplateArgument(0, constantPool.makeResolutionGuard(index, ResolveClassForNew.SNIPPET));
        emitAndRecordStops(template);
    }

    @Override
    protected void newarray(int tag) {
        final CompiledBytecodeTemplate template = getTemplate(NEWARRAY);
        beginBytecode(NEWARRAY);
        final Kind arrayElementKind = Kind.fromNewArrayTag(tag);
        assignReferenceLiteralTemplateArgument(0, arrayElementKind);
        emitAndRecordStops(template);
    }

    @Override
    protected void nop() {
        recordBytecodeStart();
    }

    @Override
    protected void pop() {
        emitTemplateFor(POP);
    }

    @Override
    protected void pop2() {
        emitTemplateFor(POP2);
    }

    @Override
    protected void putfield(int index) {
        emitTemplateForFieldAccess(PUTFIELD, index, ResolveInstanceFieldForWriting.SNIPPET);
    }

    @Override
    protected void putstatic(int index) {
        emitTemplateForFieldAccess(PUTSTATIC, index, ResolveStaticFieldForWriting.SNIPPET);
    }

    @Override
    protected void ret(int index) {
        throw ProgramError.unexpected();
    }

    @Override
    protected void saload() {
        emitTemplateFor(SALOAD);
    }

    @Override
    protected void sastore() {
        emitTemplateFor(SASTORE);
    }

    @Override
    protected void sipush(int operand) {
        emitTemplateWithOperandFor(SIPUSH, operand);
    }

    @Override
    protected void swap() {
        emitTemplateFor(SWAP);
    }

    @Override
    protected void tableswitch(int defaultOffset, int lowMatch, int highMatch, int numberOfCases) {
        final int opcodePosition = currentOpcodePosition();
        beginBytecode(TABLESWITCH);
        emitTableSwitch(lowMatch, highMatch, opcodePosition, defaultOffset, numberOfCases);
    }

    @Override
    protected void vreturn() {
        emitReturnFor(RETURN);
    }

    @Override
    protected void wide() {
        // NOTE: we do not need to emit code for WIDE because BytecodeScanner automatically widens opcodes
        recordBytecodeStart();
    }

    /**
     * Gets the assembled code from an assembler without throwing a checked exception. Any exception thrown is wrapped
     * in a ExceptionInInitializerError as this method is only called during class initialization.
     * <p>
     * The assembler is {@linkplain Assembler#reset() reset} before returning.
     * @param assembler the assembler
     * @return the machine code returned by the assembler
     */
    @HOSTED_ONLY
    protected static byte[] toByteArrayAndReset(Assembler assembler) {
        try {
            final byte[] code = assembler.toByteArray();
            assembler.reset();
            return code;
        } catch (AssemblyException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Extracts the position from a label without throwing a checked exception. Any exception thrown is wrapped in a
     * ExceptionInInitializerError as this method is only called during class initialization.
     * @param label the label
     * @return the position for the label
     */
    @HOSTED_ONLY
    protected static int toPosition(Label label) {
        try {
            return label.position();
        } catch (AssemblyException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    protected void opcodeDecoded() {
        if (shouldInsertHotpathCounters() && branchTargets.contains(currentOpcodePosition())) {
            emitHotpathCounter(currentOpcodePosition());
        }
    }

    /**
     * Emits a {@link TreeAnchor} incrementor for the current branch target.
     * @param position the position in the bytecode for which to emit the counter
     */
    protected void emitHotpathCounter(int position) {
        if (methodProfileBuilder != null) {
            ProgramWarning.message("HotpathCounters disabled for now: " + position);
        }
    }

    private boolean isResolved(ClassMethodRefConstant classMethodRef) {
        return classMethodRef.isResolvableWithoutClassLoading(constantPool) && !isTraceInstrumented;
    }

    private boolean isResolved(InterfaceMethodRefConstant interfaceMethodRef) {
        return interfaceMethodRef.isResolvableWithoutClassLoading(constantPool) && !isTraceInstrumented;
    }

    private boolean isResolved(ClassConstant classConstant) {
        return classConstant.isResolvableWithoutClassLoading(constantPool) && !isTraceInstrumented;
    }

    private boolean isResolved(FieldRefConstant fieldRefConstant) {
        return fieldRefConstant.isResolvableWithoutClassLoading(constantPool) && !isTraceInstrumented;
    }

}

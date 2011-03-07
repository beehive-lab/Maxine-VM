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
package com.sun.max.vm.cps.jit;

import static com.sun.max.vm.classfile.ErrorContext.*;
import static com.sun.max.vm.cps.template.BytecodeTemplate.*;

import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.lang.Bytes;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveClassForNew;
import com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveInstanceFieldForReading;
import com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveInstanceFieldForWriting;
import com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveInterfaceMethod;
import com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveSpecialMethod;
import com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveStaticFieldForReading;
import com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveStaticFieldForWriting;
import com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveStaticMethod;
import com.sun.max.vm.compiler.snippet.ResolutionSnippet.ResolveVirtualMethod;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.hotpath.*;
import com.sun.max.vm.cps.jit.Stop.BackwardBranchBytecodeSafepoint;
import com.sun.max.vm.cps.jit.Stop.BytecodeDirectCall;
import com.sun.max.vm.cps.jit.Stops.StopsBuilder;
import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.cps.template.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.profile.*;
import com.sun.max.vm.runtime.VMRegister.Role;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.type.*;

/**
 * Bytecodes to target template-based translator. This translator keeps minimal state about the compilation and
 * emits templates with no assumptions with respect to values produced on top of the stack, or state of the referenced
 * symbolic links. Values produced and consumed by each bytecode are pushed / popped off an evaluation stack, as
 * described in the JVM specification.
 * <p>
 * This can be used as an adapter for more sophisticated template-based code generators.
 * <p>
 *
 * @author Laurent Daynes
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Ben L. Titzer
 * @author Michael Bebenita
 * @author Paul Caprioli
 */
public abstract class BytecodeToTargetTranslator {

    private final StopsBuilder stops;

    protected boolean emitBackwardEdgeSafepointAtTarget;

    private final TemplateTable templateTable;

    protected final JVMSFrameLayout jitStackFrameLayout;

    public final CodeBuffer codeBuffer;

    protected final boolean isTraceInstrumented;

    protected final Set<Integer> branchTargets;

    protected final InlineDataRecorder inlineDataRecorder = new InlineDataRecorder();

    protected final MethodProfile.Builder methodProfileBuilder;

    /**
     * The actor of the method being compiled.
     */
    public final ClassMethodActor classMethodActor;

    protected final byte[] code;
    protected int opcodeBci;
    protected int bci;

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

    protected final boolean[] blockStarts;
    protected boolean[] exceptionHandlers;
    protected int numberOfBlocks;
    private int previousBytecode = -1;

    private int[] catchRangePositions;

    private int[] catchBlockPositions;

    /**
     * List of forward branches that need to be fixed up.
     */
    private List<ForwardBranch> forwardBranches = new LinkedList<ForwardBranch>();

    /**
     * List of tableswitch and lookupswitch instructions that need to be fixed up.
     */
    private List<Switch> switches = new LinkedList<Switch>();

    /**
     * Adapter generator when using both an optimizing and jit compiler.
     */
    protected final AdapterGenerator adapterGenerator;

    public BytecodeToTargetTranslator(ClassMethodActor classMethodActor, CodeBuffer codeBuffer, TemplateTable templateTable, JVMSFrameLayout jitStackFrameLayout, boolean trace) {
        this.isTraceInstrumented = trace;
        this.templateTable = templateTable;
        this.codeBuffer = codeBuffer;
        this.classMethodActor = classMethodActor;
        this.jitStackFrameLayout = jitStackFrameLayout;
        this.constantPool = classMethodActor.compilee().holder().constantPool();

        CodeAttribute codeAttribute = classMethodActor.codeAttribute();
        this.code = codeAttribute.code();
        this.bytecodeToTargetCodePositionMap = new int[code.length + 1];
        this.methodProfileBuilder = MethodInstrumentation.createMethodProfile(classMethodActor);

        this.blockStarts = new boolean[code.length];
        startBlock(0);

        // Try to size the list of stops so that it should not need expanding when translating most methods
        this.stops = new StopsBuilder(code.length);

        this.emitBackwardEdgeSafepointAtTarget = false;

        // We need to discover branch targets in order to insert Hotpath counters.
        if (shouldInsertHotpathCounters()) {
            this.branchTargets = discoverBranchTargets();
        } else {
            this.branchTargets = null;
        }
        adapterGenerator = AdapterGenerator.forCallee(classMethodActor, CallEntryPoint.JIT_ENTRY_POINT);

        ExceptionHandlerEntry[] exceptionHandlers = codeAttribute.exceptionHandlerTable();
        if (exceptionHandlers.length != 0) {
            this.exceptionHandlers = new boolean[code.length];
            for (ExceptionHandlerEntry e : exceptionHandlers) {
                this.exceptionHandlers[e.handlerBCI()] = true;
            }
        }

    }

    public abstract TargetABI targetABI();

    private void beginBytecode(int representativeOpcode) {
        int opcodePosition = opcodeBci;
        int targetCodePosition = codeBuffer.currentPosition();

        if (shouldInsertHotpathCounters() && branchTargets.contains(opcodeBci)) {
            // bytecodeToTargetCodePositionMap[opcodePosition] was already assigned by emitHotpathCounter().
        } else {
            bytecodeToTargetCodePositionMap[opcodePosition] = targetCodePosition;
        }

        if (Bytecodes.isBlockEnd(previousBytecode)) {
            startBlock(opcodePosition);
            if (exceptionHandlers != null && exceptionHandlers[opcodePosition]) {
                emitHandlerEntry();
            }
        }
        previousBytecode = representativeOpcode;
    }

    private void recordBytecodeStart() {
        bytecodeToTargetCodePositionMap[opcodeBci] = codeBuffer.currentPosition();
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
        forwardBranches.add(branch);
        startBlock(branch.targetBytecodePosition);
    }

    protected void addSwitch(Switch aSwitch) {
        switches.add(aSwitch);
        startBlock(aSwitch.defaultTargetBytecodePosition);
        startBlocks(aSwitch.targetBytecodePositions);
    }

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
        BytecodeScanner branchScanner = new BytecodeScanner(new ControlFlowAdapter() {
            @Override
            public void fallThrough(int address) {
                // Ignore
            }

            @Override
            public void jump(int address) {
                if (opcodeBci >= address) {
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
                VirtualMethodActor virtualMethodActor = (VirtualMethodActor) methodActor;
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
    protected void emitAndRecordStops(TargetMethod template) {
//        if (template.numberOfDirectCalls() > 0) {
//            alignTemplateWithPatchableSite(template);
//        }
        stops.add(template, codeBuffer.currentPosition(), opcodeBci);
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
    protected void recordDirectBytecodeCall(TargetMethod template, ClassMethodActor callee) {
        assert template.numberOfDirectCalls() == 1;
        assert template.numberOfIndirectCalls() == 0;
        assert template.numberOfSafepoints() == 0;
        assert template.numberOfStopPositions() == 1;
        assert template.referenceMaps() == null || Bytes.areClear(template.referenceMaps());
        alignDirectBytecodeCall(template, callee);
        int stopPosition = codeBuffer.currentPosition() + template.stopPosition(0);
        stops.add(new BytecodeDirectCall(stopPosition, opcodeBci, callee));
    }

    public Stops packStops() {
        int firstTemplateSlot = jitStackFrameLayout.numberOfNonParameterSlots() + jitStackFrameLayout.numberOfOperandStackSlots();
        int firstTemplateSlotIndexInFrameReferenceMap = firstTemplateSlot * JVMSFrameLayout.STACK_SLOTS_PER_JVMS_SLOT;
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

    protected abstract void alignDirectBytecodeCall(TargetMethod template, ClassMethodActor callee);

    public void setGenerated(
            TargetMethod targetMethod,
            Stops stops,
            byte[] compressedJavaFrameDescriptors,
            byte[] scalarLiteralBytes,
            Object[] referenceLiterals,
            byte[] codeBuffer,
            TargetABI abi) {

        JitTargetMethod jitTargetMethod = (JitTargetMethod) targetMethod;
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
            codeBuffer,
            inlineDataRecorder.encodedDescriptors(),
            stops.isDirectCallToRuntime,
            bytecodeToTargetCodePositionMap,
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
        ExceptionHandlerEntry[] exceptionHandlers = classMethodActor.codeAttribute().exceptionHandlerTable();
        if (exceptionHandlers.length == 0) {
            return;
        }

        // Deal with simple, single-handler, case first.
        if (exceptionHandlers.length == 1) {
            ExceptionHandlerEntry einfo = exceptionHandlers[0];
            catchRangePositions = new int[] {bytecodeToTargetCodePosition(einfo.startBCI()), bytecodeToTargetCodePosition(einfo.endBCI())};
            catchBlockPositions = new int[] {bytecodeToTargetCodePosition(einfo.handlerBCI()), 0};
            return;
        }
        // Over-allocate to the maximum possible size (i.e., when no two ranges are contiguous)
        // The arrays will be trimmed later at the end.
        int[] catchRangePosns = new int[exceptionHandlers.length * 2 + 1];
        int[] catchBlockPosns = new int[catchRangePosns.length];
        int index = 0;
        int nextRange = exceptionHandlers[0].startBCI();
        for (ExceptionHandlerEntry einfo : exceptionHandlers) {
            if (nextRange < einfo.startBCI()) {
                // There's a gap between the two catch ranges. Insert a range with no handler.
                catchRangePosns[index] = bytecodeToTargetCodePosition(nextRange);
                catchBlockPosns[index++] = 0;
            }
            catchRangePosns[index] = bytecodeToTargetCodePosition(einfo.startBCI());
            catchBlockPosns[index++] = bytecodeToTargetCodePosition(einfo.handlerBCI());
            nextRange = einfo.endBCI();
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

    protected TargetMethod getCode(BytecodeTemplate template) {
        assert template != null;
        assert templateTable.templates[template.ordinal()] != null;
        return templateTable.templates[template.ordinal()];
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
        int slotIndex = (!kind.isCategory1) ? (localIndex + 1) : localIndex;
        int slotOffset = jitStackFrameLayout.localVariableOffset(slotIndex) + JVMSFrameLayout.offsetInStackSlot(kind);
        assignIntTemplateArgument(parameterIndex, slotOffset);
    }

    protected abstract void loadTemplateArgumentRelativeToInstructionPointer(Kind kind, int parameterIndex, int offsetFromInstructionPointer);

    private LinkedList<Object> referenceLiterals = new LinkedList<Object>();

    public final Object[] packReferenceLiterals() {
        if (referenceLiterals.isEmpty()) {
            return null;
        }
        if (MaxineVM.isHosted()) {
            return referenceLiterals.toArray();
        }
        // Must not cause checkcast here, since some reference literals may be static tuples.
        Object[] result = new Object[referenceLiterals.size()];
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
        int literalOffset = computeReferenceLiteralOffset(1 + referenceLiterals.size());
        referenceLiterals.addFirst(literal);
        if (DebugHeap.isTagging()) {
            // Account for the DebugHeap tag in front of the code object:
            literalOffset += Word.size();
        }
        return -literalOffset;
    }

    protected void assignReferenceLiteralTemplateArgument(int parameterIndex, Object argument) {
        loadTemplateArgumentRelativeToInstructionPointer(Kind.REFERENCE, parameterIndex, createReferenceLiteral(argument));
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

    private void align4() {
        int remainder = bci & 0x3;
        if (remainder != 0) {
            bci = bci + 4 - remainder;
        }
    }

    protected final void skip(int amount) {
        bci += amount;
    }

    @INLINE
    protected final byte readByte() {
        return code[bci++];
    }

    protected final int readU1() {
        return readByte() & 0xff;
    }

    protected final int readU2() {
        int high = readByte() & 0xff;
        int low = readByte() & 0xff;
        return (high << 8) | low;
    }

    protected final int readVarIndex(boolean wide) {
        if (wide) {
            return readU2();
        }
        return readU1();
    }

    protected final int readS2() {
        int high = readByte();
        int low = readByte() & 0xff;
        return (high << 8) | low;
    }

    protected final int readS4() {
        int b3 = readByte() << 24;
        int b2 = (readByte() & 0xff) << 16;
        int b1 = (readByte() & 0xff) << 8;
        int b0 = readByte() & 0xff;
        return b3 | b2 | b1 | b0;
    }

    private String errorSuffix() {
        int opcode = code[opcodeBci] & 0xff;
        String name = Bytecodes.nameOf(opcode);
        return " [bci=" + opcodeBci + ", opcode=" + opcode + "(" + name + ")]";
    }

    public void generate() {
        bci = 0;
        while (bci < code.length) {
            opcodeBci = bci;
            int opcode = readU1();
            boolean wide = false;
            if (opcode == Bytecodes.WIDE) {
                opcode = code[bci++] & 0xff;
                wide = true;
            }

            if (shouldInsertHotpathCounters() && branchTargets.contains(opcodeBci)) {
                emitHotpathCounter(opcodeBci);
            }

            switch (opcode) {
                // Checkstyle: stop

                case Bytecodes.NOP                : recordBytecodeStart(); break;
                case Bytecodes.AALOAD             : emit(AALOAD); break;
                case Bytecodes.AASTORE            : emit(AASTORE); break;
                case Bytecodes.ACONST_NULL        : emit(ACONST_NULL); break;
                case Bytecodes.ARRAYLENGTH        : emit(ARRAYLENGTH); break;
                case Bytecodes.ATHROW             : emit(ATHROW); break;
                case Bytecodes.BALOAD             : emit(BALOAD); break;
                case Bytecodes.BASTORE            : emit(BASTORE); break;
                case Bytecodes.CALOAD             : emit(CALOAD); break;
                case Bytecodes.CASTORE            : emit(CASTORE); break;
                case Bytecodes.D2F                : emit(D2F); break;
                case Bytecodes.D2I                : emit(D2I); break;
                case Bytecodes.D2L                : emit(D2L); break;
                case Bytecodes.DADD               : emit(DADD); break;
                case Bytecodes.DALOAD             : emit(DALOAD); break;
                case Bytecodes.DASTORE            : emit(DASTORE); break;
                case Bytecodes.DCMPG              : emit(DCMPG); break;
                case Bytecodes.DCMPL              : emit(DCMPL); break;
                case Bytecodes.DDIV               : emit(DDIV); break;
                case Bytecodes.DMUL               : emit(DMUL); break;
                case Bytecodes.DREM               : emit(DREM); break;
                case Bytecodes.DSUB               : emit(DSUB); break;
                case Bytecodes.DUP                : emit(DUP); break;
                case Bytecodes.DUP2               : emit(DUP2); break;
                case Bytecodes.DUP2_X1            : emit(DUP2_X1); break;
                case Bytecodes.DUP2_X2            : emit(DUP2_X2); break;
                case Bytecodes.DUP_X1             : emit(DUP_X1); break;
                case Bytecodes.DUP_X2             : emit(DUP_X2); break;
                case Bytecodes.F2D                : emit(F2D); break;
                case Bytecodes.F2I                : emit(F2I); break;
                case Bytecodes.F2L                : emit(F2L); break;
                case Bytecodes.FADD               : emit(FADD); break;
                case Bytecodes.FALOAD             : emit(FALOAD); break;
                case Bytecodes.FASTORE            : emit(FASTORE); break;
                case Bytecodes.FCMPG              : emit(FCMPG); break;
                case Bytecodes.FCMPL              : emit(FCMPL); break;
                case Bytecodes.FDIV               : emit(FDIV); break;
                case Bytecodes.FMUL               : emit(FMUL); break;
                case Bytecodes.FREM               : emit(FREM); break;
                case Bytecodes.FSUB               : emit(FSUB); break;
                case Bytecodes.I2B                : emit(I2B); break;
                case Bytecodes.I2C                : emit(I2C); break;
                case Bytecodes.I2D                : emit(I2D); break;
                case Bytecodes.I2F                : emit(I2F); break;
                case Bytecodes.I2L                : emit(I2L); break;
                case Bytecodes.I2S                : emit(I2S); break;
                case Bytecodes.IADD               : emit(IADD); break;
                case Bytecodes.IALOAD             : emit(IALOAD); break;
                case Bytecodes.IAND               : emit(IAND); break;
                case Bytecodes.IASTORE            : emit(IASTORE); break;
                case Bytecodes.ICONST_0           : emit(ICONST_0); break;
                case Bytecodes.ICONST_1           : emit(ICONST_1); break;
                case Bytecodes.ICONST_2           : emit(ICONST_2); break;
                case Bytecodes.ICONST_3           : emit(ICONST_3); break;
                case Bytecodes.ICONST_4           : emit(ICONST_4); break;
                case Bytecodes.ICONST_5           : emit(ICONST_5); break;
                case Bytecodes.ICONST_M1          : emit(ICONST_M1); break;
                case Bytecodes.IDIV               : emit(IDIV); break;
                case Bytecodes.IMUL               : emit(IMUL); break;
                case Bytecodes.INEG               : emit(INEG); break;
                case Bytecodes.IOR                : emit(IOR); break;
                case Bytecodes.IREM               : emit(IREM); break;
                case Bytecodes.ISHL               : emit(ISHL); break;
                case Bytecodes.ISHR               : emit(ISHR); break;
                case Bytecodes.ISUB               : emit(ISUB); break;
                case Bytecodes.IUSHR              : emit(IUSHR); break;
                case Bytecodes.IXOR               : emit(IXOR); break;
                case Bytecodes.L2D                : emit(L2D); break;
                case Bytecodes.L2F                : emit(L2F); break;
                case Bytecodes.L2I                : emit(L2I); break;
                case Bytecodes.LADD               : emit(LADD); break;
                case Bytecodes.LALOAD             : emit(LALOAD); break;
                case Bytecodes.LAND               : emit(LAND); break;
                case Bytecodes.LASTORE            : emit(LASTORE); break;
                case Bytecodes.LCMP               : emit(LCMP); break;
                case Bytecodes.LDIV               : emit(LDIV); break;
                case Bytecodes.LMUL               : emit(LMUL); break;
                case Bytecodes.LNEG               : emit(LNEG); break;
                case Bytecodes.LOR                : emit(LOR); break;
                case Bytecodes.LREM               : emit(LREM); break;
                case Bytecodes.LSHL               : emit(LSHL); break;
                case Bytecodes.LSHR               : emit(LSHR); break;
                case Bytecodes.LSUB               : emit(LSUB); break;
                case Bytecodes.LUSHR              : emit(LUSHR); break;
                case Bytecodes.LXOR               : emit(LXOR); break;
                case Bytecodes.MONITORENTER       : emit(MONITORENTER); break;
                case Bytecodes.MONITOREXIT        : emit(MONITOREXIT); break;
                case Bytecodes.POP                : emit(POP); break;
                case Bytecodes.POP2               : emit(POP2); break;
                case Bytecodes.SALOAD             : emit(SALOAD); break;
                case Bytecodes.SASTORE            : emit(SASTORE); break;
                case Bytecodes.SWAP               : emit(SWAP); break;
                case Bytecodes.LCONST_0           : emitLong(LCONST, 0L); break;
                case Bytecodes.LCONST_1           : emitLong(LCONST, 1L); break;
                case Bytecodes.DCONST_0           : emitDouble(DCONST, 0D); break;
                case Bytecodes.DCONST_1           : emitDouble(DCONST, 1D); break;
                case Bytecodes.DNEG               : emitDouble(DNEG, 0D); break;
                case Bytecodes.FCONST_0           : emitFloat(FCONST, 0F); break;
                case Bytecodes.FCONST_1           : emitFloat(FCONST, 1F); break;
                case Bytecodes.FCONST_2           : emitFloat(FCONST, 2F); break;
                case Bytecodes.FNEG               : emitFloat(FNEG, 0F); break;
                case Bytecodes.ARETURN            : emitReturn(ARETURN); break;
                case Bytecodes.DRETURN            : emitReturn(DRETURN); break;
                case Bytecodes.FRETURN            : emitReturn(FRETURN); break;
                case Bytecodes.IRETURN            : emitReturn(IRETURN); break;
                case Bytecodes.LRETURN            : emitReturn(LRETURN); break;
                case Bytecodes.RETURN             : emitReturn(RETURN); break;
                case Bytecodes.ALOAD              : emitVarAccess(ALOAD, readVarIndex(wide), Kind.REFERENCE); break;
                case Bytecodes.ALOAD_0            : emitVarAccess(ALOAD, 0, Kind.REFERENCE); break;
                case Bytecodes.ALOAD_1            : emitVarAccess(ALOAD, 1, Kind.REFERENCE); break;
                case Bytecodes.ALOAD_2            : emitVarAccess(ALOAD, 2, Kind.REFERENCE); break;
                case Bytecodes.ALOAD_3            : emitVarAccess(ALOAD, 3, Kind.REFERENCE); break;
                case Bytecodes.ASTORE             : emitVarAccess(ASTORE, readVarIndex(wide), Kind.REFERENCE); break;
                case Bytecodes.ASTORE_0           : emitVarAccess(ASTORE, 0, Kind.REFERENCE); break;
                case Bytecodes.ASTORE_1           : emitVarAccess(ASTORE, 1, Kind.REFERENCE); break;
                case Bytecodes.ASTORE_2           : emitVarAccess(ASTORE, 2, Kind.REFERENCE); break;
                case Bytecodes.ASTORE_3           : emitVarAccess(ASTORE, 3, Kind.REFERENCE); break;
                case Bytecodes.DLOAD              : emitVarAccess(DLOAD, readVarIndex(wide), Kind.DOUBLE); break;
                case Bytecodes.DLOAD_0            : emitVarAccess(DLOAD, 0, Kind.DOUBLE); break;
                case Bytecodes.DLOAD_1            : emitVarAccess(DLOAD, 1, Kind.DOUBLE); break;
                case Bytecodes.DLOAD_2            : emitVarAccess(DLOAD, 2, Kind.DOUBLE); break;
                case Bytecodes.DLOAD_3            : emitVarAccess(DLOAD, 3, Kind.DOUBLE); break;
                case Bytecodes.DSTORE             : emitVarAccess(DSTORE, readVarIndex(wide), Kind.DOUBLE); break;
                case Bytecodes.DSTORE_0           : emitVarAccess(DSTORE, 0, Kind.DOUBLE); break;
                case Bytecodes.DSTORE_1           : emitVarAccess(DSTORE, 1, Kind.DOUBLE); break;
                case Bytecodes.DSTORE_2           : emitVarAccess(DSTORE, 2, Kind.DOUBLE); break;
                case Bytecodes.DSTORE_3           : emitVarAccess(DSTORE, 3, Kind.DOUBLE); break;
                case Bytecodes.FLOAD              : emitVarAccess(FLOAD, readVarIndex(wide), Kind.FLOAT); break;
                case Bytecodes.FLOAD_0            : emitVarAccess(FLOAD, 0, Kind.FLOAT); break;
                case Bytecodes.FLOAD_1            : emitVarAccess(FLOAD, 1, Kind.FLOAT); break;
                case Bytecodes.FLOAD_2            : emitVarAccess(FLOAD, 2, Kind.FLOAT); break;
                case Bytecodes.FLOAD_3            : emitVarAccess(FLOAD, 3, Kind.FLOAT); break;
                case Bytecodes.FSTORE             : emitVarAccess(FSTORE, readVarIndex(wide), Kind.FLOAT); break;
                case Bytecodes.FSTORE_0           : emitVarAccess(FSTORE, 0, Kind.FLOAT); break;
                case Bytecodes.FSTORE_1           : emitVarAccess(FSTORE, 1, Kind.FLOAT); break;
                case Bytecodes.FSTORE_2           : emitVarAccess(FSTORE, 2, Kind.FLOAT); break;
                case Bytecodes.FSTORE_3           : emitVarAccess(FSTORE, 3, Kind.FLOAT); break;
                case Bytecodes.ILOAD              : emitVarAccess(ILOAD, readVarIndex(wide), Kind.INT); break;
                case Bytecodes.ILOAD_0            : emitVarAccess(ILOAD, 0, Kind.INT); break;
                case Bytecodes.ILOAD_1            : emitVarAccess(ILOAD, 1, Kind.INT); break;
                case Bytecodes.ILOAD_2            : emitVarAccess(ILOAD, 2, Kind.INT); break;
                case Bytecodes.ILOAD_3            : emitVarAccess(ILOAD, 3, Kind.INT); break;
                case Bytecodes.ISTORE             : emitVarAccess(ISTORE, readVarIndex(wide), Kind.INT); break;
                case Bytecodes.ISTORE_0           : emitVarAccess(ISTORE, 0, Kind.INT); break;
                case Bytecodes.ISTORE_1           : emitVarAccess(ISTORE, 1, Kind.INT); break;
                case Bytecodes.ISTORE_2           : emitVarAccess(ISTORE, 2, Kind.INT); break;
                case Bytecodes.ISTORE_3           : emitVarAccess(ISTORE, 3, Kind.INT); break;
                case Bytecodes.LSTORE             : emitVarAccess(LSTORE, readVarIndex(wide), Kind.LONG); break;
                case Bytecodes.LLOAD              : emitVarAccess(LLOAD, readVarIndex(wide), Kind.LONG); break;
                case Bytecodes.LLOAD_0            : emitVarAccess(LLOAD, 0, Kind.LONG); break;
                case Bytecodes.LLOAD_1            : emitVarAccess(LLOAD, 1, Kind.LONG); break;
                case Bytecodes.LLOAD_2            : emitVarAccess(LLOAD, 2, Kind.LONG); break;
                case Bytecodes.LLOAD_3            : emitVarAccess(LLOAD, 3, Kind.LONG); break;
                case Bytecodes.LSTORE_0           : emitVarAccess(LSTORE, 0, Kind.LONG); break;
                case Bytecodes.LSTORE_1           : emitVarAccess(LSTORE, 1, Kind.LONG); break;
                case Bytecodes.LSTORE_2           : emitVarAccess(LSTORE, 2, Kind.LONG); break;
                case Bytecodes.LSTORE_3           : emitVarAccess(LSTORE, 3, Kind.LONG); break;
                case Bytecodes.IFEQ               : emitBranch(BranchCondition.EQ, IFEQ, readS2()); break;
                case Bytecodes.IFNE               : emitBranch(BranchCondition.NE, IFNE, readS2()); break;
                case Bytecodes.IFLE               : emitBranch(BranchCondition.LE, IFLE, readS2()); break;
                case Bytecodes.IFLT               : emitBranch(BranchCondition.LT, IFLT, readS2()); break;
                case Bytecodes.IFGE               : emitBranch(BranchCondition.GE, IFGE, readS2()); break;
                case Bytecodes.IFGT               : emitBranch(BranchCondition.GT, IFGT, readS2()); break;
                case Bytecodes.IF_ICMPEQ          : emitBranch(BranchCondition.EQ, IF_ICMPEQ, readS2()); break;
                case Bytecodes.IF_ICMPNE          : emitBranch(BranchCondition.NE, IF_ICMPNE, readS2()); break;
                case Bytecodes.IF_ICMPGE          : emitBranch(BranchCondition.GE, IF_ICMPGE, readS2()); break;
                case Bytecodes.IF_ICMPGT          : emitBranch(BranchCondition.GT, IF_ICMPGT, readS2()); break;
                case Bytecodes.IF_ICMPLE          : emitBranch(BranchCondition.LE, IF_ICMPLE, readS2()); break;
                case Bytecodes.IF_ICMPLT          : emitBranch(BranchCondition.LT, IF_ICMPLT, readS2()); break;
                case Bytecodes.IF_ACMPEQ          : emitBranch(BranchCondition.EQ, IF_ACMPEQ, readS2()); break;
                case Bytecodes.IF_ACMPNE          : emitBranch(BranchCondition.NE, IF_ACMPNE, readS2()); break;
                case Bytecodes.IFNULL             : emitBranch(BranchCondition.EQ, IFNULL, readS2()); break;
                case Bytecodes.IFNONNULL          : emitBranch(BranchCondition.NE, IFNONNULL, readS2()); break;
                case Bytecodes.GOTO               : emitGoto(Bytecodes.GOTO, readS2()); break;
                case Bytecodes.GOTO_W             : emitGoto(Bytecodes.GOTO_W, readS4()); break;
                case Bytecodes.GETFIELD           : emitFieldAccess(GETFIELDS, readU2(), ResolveInstanceFieldForReading.SNIPPET); break;
                case Bytecodes.GETSTATIC          : emitFieldAccess(GETSTATICS, readU2(), ResolveStaticFieldForReading.SNIPPET); break;
                case Bytecodes.PUTFIELD           : emitFieldAccess(PUTFIELDS, readU2(), ResolveInstanceFieldForWriting.SNIPPET); break;
                case Bytecodes.PUTSTATIC          : emitFieldAccess(PUTSTATICS, readU2(), ResolveStaticFieldForWriting.SNIPPET); break;
                case Bytecodes.ANEWARRAY          : emitTemplateWithClassConstant(ANEWARRAY, readU2()); break;
                case Bytecodes.CHECKCAST          : emitTemplateWithClassConstant(CHECKCAST, readU2()); break;
                case Bytecodes.INSTANCEOF         : emitTemplateWithClassConstant(INSTANCEOF, readU2()); break;
                case Bytecodes.BIPUSH             : emitInt(BIPUSH, readByte()); break;
                case Bytecodes.SIPUSH             : emitInt(SIPUSH, readS2()); break;
                case Bytecodes.NEW                : emitNew(readU2()); break;
                case Bytecodes.INVOKESPECIAL      : emitInvokespecial(readU2()); break;
                case Bytecodes.INVOKESTATIC       : emitInvokestatic(readU2()); break;
                case Bytecodes.INVOKEVIRTUAL      : emitInvokevirtual(readU2()); break;
                case Bytecodes.INVOKEINTERFACE    : emitInvokeinterface(readU2(), readU2() >> 8); break;
                case Bytecodes.NEWARRAY           : emitNewarray(readU1()); break;
                case Bytecodes.LDC                : emitConstant(readU1()); break;
                case Bytecodes.LDC_W              : emitConstant(readU2()); break;
                case Bytecodes.LDC2_W             : emitConstant(readU2()); break;
                case Bytecodes.TABLESWITCH        : emitTableswitch(); break;
                case Bytecodes.LOOKUPSWITCH       : emitLookupswitch(); break;
                case Bytecodes.IINC               : emitIinc(readVarIndex(wide), wide ? readS2() : readByte()); break;
                case Bytecodes.MULTIANEWARRAY     : emitMultianewarray(readU2(), readU1()); break;


                case Bytecodes.UNSAFE_CAST        : skip(2); break;
                case Bytecodes.WLOAD              : emitVarAccess(WLOAD, readVarIndex(wide), Kind.WORD); break;
                case Bytecodes.WLOAD_0            : emitVarAccess(WLOAD, 0, Kind.WORD); break;
                case Bytecodes.WLOAD_1            : emitVarAccess(WLOAD, 1, Kind.WORD); break;
                case Bytecodes.WLOAD_2            : emitVarAccess(WLOAD, 2, Kind.WORD); break;
                case Bytecodes.WLOAD_3            : emitVarAccess(WLOAD, 3, Kind.WORD); break;
                case Bytecodes.WSTORE             : emitVarAccess(WSTORE, readVarIndex(wide), Kind.WORD); break;
                case Bytecodes.WSTORE_0           : emitVarAccess(WSTORE, 0, Kind.WORD); break;
                case Bytecodes.WSTORE_1           : emitVarAccess(WSTORE, 1, Kind.WORD); break;
                case Bytecodes.WSTORE_2           : emitVarAccess(WSTORE, 2, Kind.WORD); break;
                case Bytecodes.WSTORE_3           : emitVarAccess(WSTORE, 3, Kind.WORD); break;

                case Bytecodes.WCONST_0           : emit(WCONST_0); skip(2); break;
                case Bytecodes.WDIV               : emit(WDIV); skip(2); break;
                case Bytecodes.WDIVI              : emit(WDIVI); skip(2); break;
                case Bytecodes.WREM               : emit(WREM); skip(2); break;
                case Bytecodes.WREMI              : emit(WREMI); skip(2); break;

                case Bytecodes.MEMBAR:
                case Bytecodes.PCMPSWP:
                case Bytecodes.PGET:
                case Bytecodes.PSET:
                case Bytecodes.PREAD:
                case Bytecodes.PWRITE: {
                    opcode = opcode | (readU2() << 8);
                    switch (opcode) {
                        case Bytecodes.PREAD_BYTE         : emit(PREAD_BYTE); break;
                        case Bytecodes.PREAD_CHAR         : emit(PREAD_CHAR); break;
                        case Bytecodes.PREAD_SHORT        : emit(PREAD_SHORT); break;
                        case Bytecodes.PREAD_INT          : emit(PREAD_INT); break;
                        case Bytecodes.PREAD_LONG         : emit(PREAD_LONG); break;
                        case Bytecodes.PREAD_FLOAT        : emit(PREAD_FLOAT); break;
                        case Bytecodes.PREAD_DOUBLE       : emit(PREAD_DOUBLE); break;
                        case Bytecodes.PREAD_WORD         : emit(PREAD_WORD); break;
                        case Bytecodes.PREAD_REFERENCE    : emit(PREAD_REFERENCE); break;

                        case Bytecodes.PREAD_BYTE_I       : emit(PREAD_BYTE_I); break;
                        case Bytecodes.PREAD_CHAR_I       : emit(PREAD_CHAR_I); break;
                        case Bytecodes.PREAD_SHORT_I      : emit(PREAD_SHORT_I); break;
                        case Bytecodes.PREAD_INT_I        : emit(PREAD_INT_I); break;
                        case Bytecodes.PREAD_LONG_I       : emit(PREAD_LONG_I); break;
                        case Bytecodes.PREAD_FLOAT_I      : emit(PREAD_FLOAT_I); break;
                        case Bytecodes.PREAD_DOUBLE_I     : emit(PREAD_DOUBLE_I); break;
                        case Bytecodes.PREAD_WORD_I       : emit(PREAD_WORD_I); break;
                        case Bytecodes.PREAD_REFERENCE_I  : emit(PREAD_REFERENCE_I); break;

                        case Bytecodes.PWRITE_BYTE        : emit(PWRITE_BYTE); break;
                        case Bytecodes.PWRITE_SHORT       : emit(PWRITE_SHORT); break;
                        case Bytecodes.PWRITE_INT         : emit(PWRITE_INT); break;
                        case Bytecodes.PWRITE_LONG        : emit(PWRITE_LONG); break;
                        case Bytecodes.PWRITE_FLOAT       : emit(PWRITE_FLOAT); break;
                        case Bytecodes.PWRITE_DOUBLE      : emit(PWRITE_DOUBLE); break;
                        case Bytecodes.PWRITE_WORD        : emit(PWRITE_WORD); break;
                        case Bytecodes.PWRITE_REFERENCE   : emit(PWRITE_REFERENCE); break;

                        case Bytecodes.PWRITE_BYTE_I      : emit(PWRITE_BYTE_I); break;
                        case Bytecodes.PWRITE_SHORT_I     : emit(PWRITE_SHORT_I); break;
                        case Bytecodes.PWRITE_INT_I       : emit(PWRITE_INT_I); break;
                        case Bytecodes.PWRITE_LONG_I      : emit(PWRITE_LONG_I); break;
                        case Bytecodes.PWRITE_FLOAT_I     : emit(PWRITE_FLOAT_I); break;
                        case Bytecodes.PWRITE_DOUBLE_I    : emit(PWRITE_DOUBLE_I); break;
                        case Bytecodes.PWRITE_WORD_I      : emit(PWRITE_WORD_I); break;
                        case Bytecodes.PWRITE_REFERENCE_I : emit(PWRITE_REFERENCE_I); break;

                        case Bytecodes.PGET_BYTE          : emit(PGET_BYTE); break;
                        case Bytecodes.PGET_CHAR          : emit(PGET_CHAR); break;
                        case Bytecodes.PGET_SHORT         : emit(PGET_SHORT); break;
                        case Bytecodes.PGET_INT           : emit(PGET_INT); break;
                        case Bytecodes.PGET_LONG          : emit(PGET_LONG); break;
                        case Bytecodes.PGET_FLOAT         : emit(PGET_FLOAT); break;
                        case Bytecodes.PGET_DOUBLE        : emit(PGET_DOUBLE); break;
                        case Bytecodes.PGET_WORD          : emit(PGET_WORD); break;
                        case Bytecodes.PGET_REFERENCE     : emit(PGET_REFERENCE); break;

                        case Bytecodes.PSET_BYTE          : emit(PSET_BYTE); break;
                        case Bytecodes.PSET_SHORT         : emit(PSET_SHORT); break;
                        case Bytecodes.PSET_INT           : emit(PSET_INT); break;
                        case Bytecodes.PSET_LONG          : emit(PSET_LONG); break;
                        case Bytecodes.PSET_FLOAT         : emit(PSET_FLOAT); break;
                        case Bytecodes.PSET_DOUBLE        : emit(PSET_DOUBLE); break;
                        case Bytecodes.PSET_WORD          : emit(PSET_WORD); break;
                        case Bytecodes.PSET_REFERENCE     : emit(PSET_REFERENCE); break;

                        case Bytecodes.PCMPSWP_INT        : emit(PCMPSWP_INT); break;
                        case Bytecodes.PCMPSWP_WORD       : emit(PCMPSWP_WORD); break;
                        case Bytecodes.PCMPSWP_REFERENCE  : emit(PCMPSWP_REFERENCE); break;
                        case Bytecodes.PCMPSWP_INT_I      : emit(PCMPSWP_INT_I); break;
                        case Bytecodes.PCMPSWP_WORD_I     : emit(PCMPSWP_WORD_I); break;
                        case Bytecodes.PCMPSWP_REFERENCE_I: emit(PCMPSWP_REFERENCE_I); break;

                        case Bytecodes.MEMBAR_LOAD_LOAD   : emit(MEMBAR_LOAD_LOAD); break;
                        case Bytecodes.MEMBAR_LOAD_STORE  : emit(MEMBAR_LOAD_STORE); break;
                        case Bytecodes.MEMBAR_STORE_LOAD  : emit(MEMBAR_STORE_LOAD); break;
                        case Bytecodes.MEMBAR_STORE_STORE : emit(MEMBAR_STORE_STORE); break;

                        default                           : throw new InternalError("Unsupported opcode" + errorSuffix());
                    }
                    break;
                }

                case Bytecodes.MOV_I2F            : emit(MOV_I2F); skip(2); break;
                case Bytecodes.MOV_F2I            : emit(MOV_F2I); skip(2); break;
                case Bytecodes.MOV_L2D            : emit(MOV_L2D); skip(2); break;
                case Bytecodes.MOV_D2L            : emit(MOV_D2L); skip(2); break;


                case Bytecodes.WRETURN            : emitReturn(WRETURN); break;
                case Bytecodes.PAUSE              : emit(PAUSE); skip(2); break;
                case Bytecodes.LSB                : emit(LSB); skip(2); break;
                case Bytecodes.MSB                : emit(MSB); skip(2); break;

                case Bytecodes.READREG            : emit(READREGS.get(Role.VALUES.get(readU2()))); break;
                case Bytecodes.WRITEREG           : emit(WRITEREGS.get(Role.VALUES.get(readU2()))); break;

                case Bytecodes.ADD_SP             :
                case Bytecodes.INFOPOINT          :
                case Bytecodes.FLUSHW             :
                case Bytecodes.ALLOCA             :
                case Bytecodes.ALLOCSTKVAR        :
                case Bytecodes.JNICALL            :
                case Bytecodes.TEMPLATE_CALL               :
                case Bytecodes.ICMP               :
                case Bytecodes.WCMP               :
                case Bytecodes.RET                :
                case Bytecodes.JSR_W              :
                case Bytecodes.JSR                :
                default                           : throw new InternalError("Unsupported opcode" + errorSuffix());
                // Checkstyle: resume
            }
            assert Bytecodes.lengthOf(code, opcodeBci) + opcodeBci == bci;
        }
    }

    protected abstract void emitBranch(BranchCondition branchCondition, int fromBytecodePosition, int toBytecodePosition);

    /**
     * Emits the code for a {@link Bytecodes#TABLESWITCH} instruction.
     *
     * @param low the lower bound (inclusive) of the switch table case values
     * @param high the upper bound (inclusive) of the switch table case values
     * @param opcodePosition the bytecode position of the TABLESWITCH opcode
     * @param defaultTargetOffset the offset from {@code opcodePosition} for the default case
     * @param numberOfCases the number of cases in the switch
     */
    protected abstract void emitTableSwitch(int low, int high, int opcodePosition, int defaultTargetOffset, int numberOfCases);

    /**
     * Emits the code for a {@link Bytecodes#LOOKUPSWITCH} instruction.
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
    public abstract Adapter emitPrologue();

    /**
     * Does any fix up of branches and emit the epilogue (if any).
     */
    public void emitEpilogue() {
        // Record the end of the target code emitted for the last bytecode instruction
        final int targetCodePosition = codeBuffer.currentPosition();
        bytecodeToTargetCodePositionMap[code.length] = targetCodePosition;

        for (ForwardBranch forwardBranch : forwardBranches) {
            fixForwardBranch(forwardBranch);
        }

        for (Switch aSwitch : switches) {
            aSwitch.fixup(this);
        }
    }

    protected abstract void emitReturn();

    public void emitEntrypointInstrumentation() {
        if (methodProfileBuilder != null) {
            methodProfileBuilder.addEntryCounter(MethodInstrumentation.initialEntryCount);
            final TargetMethod template = getCode(NOP$instrumented$MethodEntry);
            assignReferenceLiteralTemplateArgument(0, methodProfileBuilder.methodProfileObject());
            emitAndRecordStops(template);
        }
    }

    static boolean TraceMethods;
    static {
        VMOptions.addFieldOption("-JIT:", "TraceMethods", "Trace calls to JIT'ed methods.");
    }

    public void emitMethodTrace() {
        if (TraceMethods) {
            TargetMethod template = getCode(NOP$instrumented$TraceMethod);
            assignReferenceLiteralTemplateArgument(0, classMethodActor.toString());
            emitAndRecordStops(template);
        }
    }

    public void emitHandlerEntry() {
        final TargetMethod template = getCode(LOAD_EXCEPTION);
        emitAndRecordStops(template);
    }

    /**
     * Emit template for bytecode instruction with no operands. These bytecode have no dependencies, so emitting the
     * template just consists of copying the target instruction into the code buffer.
     * @param template the bytecode for which to emit the template
     */
    private void emit(BytecodeTemplate template) {
        TargetMethod code = getCode(template);
        beginBytecode(template.opcode);
        emitAndRecordStops(code);
    }

    private void emitReturn(BytecodeTemplate template) {
        emit(template);
        emitReturn();
    }

    /**
     * Emits a template for a bytecode operating on a local variable (operand is an index to a local variable). The
     * template is customized so that the emitted code uses a specific local variable index.
     *
     * @param opcode One of iload, istore, dload, dstore, fload, fstore, lload, lstore
     * @param localVariableIndex the local variable index to customize the template with.
     * @param kind the kind of the value in the local
     */
    private void emitVarAccess(BytecodeTemplate template, int localVariableIndex, Kind kind) {
        beginBytecode(template.opcode);
        assignLocalDisplacementTemplateArgument(0, localVariableIndex, kind);
        TargetMethod code = getCode(template);
        emitAndRecordStops(code);
    }

    private void emitIinc(int index, int increment) {
        beginBytecode(IINC.opcode);
        final TargetMethod code = getCode(IINC);
        assignLocalDisplacementTemplateArgument(0, index, Kind.INT);
        assignIntTemplateArgument(1, increment);
        emitAndRecordStops(code);
    }

    /**
     * Emits code for an instruction that references a {@link ClassConstant}.
     *
     * @param template the instruction template
     * @param index a constant pool index
     * @return
     */
    private void emitTemplateWithClassConstant(BytecodeTemplate template, int index) {
        ClassConstant classConstant = constantPool.classAt(index);
        boolean isArray = template == ANEWARRAY;
        TargetMethod code;
        if (isResolved(classConstant, index)) {
            code = getCode(template.resolved);
            beginBytecode(template.opcode);
            ClassActor resolvedClassActor = classConstant.resolve(constantPool, index);
            if (isArray) {
                resolvedClassActor = ArrayClassActor.forComponentClassActor(resolvedClassActor);
            }
            assignReferenceLiteralTemplateArgument(0, resolvedClassActor);
        } else {
            code = getCode(template);
            beginBytecode(template.opcode);
            ResolutionSnippet snippet = isArray ? ResolutionSnippet.ResolveArrayClass.SNIPPET : ResolutionSnippet.ResolveClass.SNIPPET;
            assignReferenceLiteralTemplateArgument(0, constantPool.makeResolutionGuard(index, snippet));
        }
        emitAndRecordStops(code);
    }

    /**
     * Emit template for a bytecode operating on a (static or dynamic) field.
     *
     * @param template one of getfield, putfield, getstatic, putstatic
     * @param index Index to the field ref constant.
     * @param snippet the resolution snippet to call
     */
    private void emitFieldAccess(EnumMap<KindEnum, BytecodeTemplate> templates, int index, ResolutionSnippet snippet) {
        FieldRefConstant fieldRefConstant = constantPool.fieldAt(index);
        Kind fieldKind = fieldRefConstant.type(constantPool).toKind();
        BytecodeTemplate template = templates.get(fieldKind.asEnum);
        if (isResolved(fieldRefConstant, index)) {
            try {
                FieldActor fieldActor = fieldRefConstant.resolve(constantPool, index);
                TargetMethod code;
                if (fieldActor.isStatic()) {
                    if (fieldActor.holder().isInitialized()) {
                        code = getCode(template.initialized);
                        beginBytecode(template.opcode);
                        assignReferenceLiteralTemplateArgument(0, fieldActor.holder().staticTuple());
                        assignIntTemplateArgument(1, fieldActor.offset());
                        emitAndRecordStops(code);
                        return;
                    }
                } else {
                    code = getCode(template.resolved);
                    beginBytecode(template.opcode);
                    assignIntTemplateArgument(0, fieldActor.offset());
                    emitAndRecordStops(code);
                    return;
                }
            } catch (LinkageError e) {
                // This should not happen since the field ref constant is resolvable without class loading (i.e., it
                // has already been resolved). If it were to happen, the safe thing to do is to fall off to the
                // "no assumption" case, where a template for an unresolved class is taken instead. So do nothing here.
            }
        }
        TargetMethod code = getCode(template);
        beginBytecode(template.opcode);
        assignReferenceLiteralTemplateArgument(0, constantPool.makeResolutionGuard(index, snippet));
        // Emit the template unmodified now. It will be modified in the end once all labels to literals are fixed.
        emitAndRecordStops(code);
    }

    /**
     * Emits code for an instruction with a constant integer operand.
     *
     * @param template the instruction template
     * @param operand the integer argument to be passed to {@code template}
     */
    private void emitInt(BytecodeTemplate template, int operand) {
        beginBytecode(template.opcode);
        assignIntTemplateArgument(0, operand);
        emitAndRecordStops(getCode(template));
    }

    /**
     * Emits code for an instruction with a constant ilong operand.
     *
     * @param template the instruction template
     * @param operand the long argument to be passed to {@code template}
     */
    private void emitDouble(BytecodeTemplate template, double doubleValue) {
        beginBytecode(template.opcode);
        assignDoubleTemplateArgument(0, doubleValue);
        TargetMethod code = getCode(template);
        emitAndRecordStops(code);
    }

    /**
     * Emits code for an instruction with a constant float operand.
     *
     * @param template the instruction template
     * @param operand the float argument to be passed to {@code template}
     */
    private void emitFloat(BytecodeTemplate template, float value) {
        beginBytecode(template.opcode);
        assignFloatTemplateArgument(0, value);
        TargetMethod code = getCode(template);
        emitAndRecordStops(code);
    }

    /**
     * Emits code for an instruction with a constant double operand.
     *
     * @param template the instruction template
     * @param operand the double argument to be passed to {@code template}
     */
    private void emitLong(BytecodeTemplate template, long value) {
        beginBytecode(template.opcode);
        assignLongTemplateArgument(0, value);
        TargetMethod code = getCode(template);
        emitAndRecordStops(code);
    }

    private void emitBranch(BranchCondition condition, BytecodeTemplate template, int offset) {
        int currentBytecodePosition = opcodeBci;
        int targetBytecodePosition = currentBytecodePosition + offset;
        startBlock(targetBytecodePosition);
        // emit prefix of the bytecodeinstruction.
        TargetMethod code = getCode(template);
        beginBytecode(template.opcode);
        assert code.directCallees() == null || isTraceInstrumented;
        emitAndRecordStops(code);
        emitBranch(condition, currentBytecodePosition, targetBytecodePosition);
    }

    private void emitGoto(int bytecode, int offset) {
        int currentBytecodePosition = opcodeBci;
        int targetBytecodePosition = currentBytecodePosition + offset;
        startBlock(targetBytecodePosition);
        beginBytecode(bytecode);
        emitBranch(BranchCondition.NONE, currentBytecodePosition, targetBytecodePosition);
    }

    /**
     * Gets the kind used to select an INVOKE... bytecode template.
     */
    private Kind invokeKind(SignatureDescriptor signature) {
        Kind resultKind = signature.resultKind();
        if (resultKind.isWord || resultKind.isReference || resultKind.stackKind == Kind.INT) {
            return Kind.WORD;
        }
        return resultKind;
    }

    private static int receiverStackIndex(SignatureDescriptor signatureDescriptor) {
        int index = 0;
        for (int i = 0; i < signatureDescriptor.numberOfParameters(); i++) {
            Kind kind = signatureDescriptor.parameterDescriptorAt(i).toKind();
            index += kind.stackSlots;
        }
        return index;
    }

    private void emitInvokevirtual(int index) {
        ClassMethodRefConstant classMethodRef = constantPool.classMethodAt(index);
        SignatureDescriptor signature = classMethodRef.signature(constantPool);
        Kind kind = invokeKind(signature);
        BytecodeTemplate template = INVOKEVIRTUALS.get(kind.asEnum);
        try {
            if (isResolved(classMethodRef, index)) {
                try {
                    VirtualMethodActor virtualMethodActor = classMethodRef.resolveVirtual(constantPool, index);
                    if (virtualMethodActor.isPrivate() || virtualMethodActor.isFinal()) {
                        // this is an invokevirtual to a private or final method, treat it like invokespecial
                        emitInvokespecial(index);
                    } else if (shouldProfileMethodCall(virtualMethodActor)) {
                        // emit a profiled call
                        TargetMethod code = getCode(template.instrumented);
                        beginBytecode(template.opcode);
                        int vtableIndex = virtualMethodActor.vTableIndex();
                        assignIntTemplateArgument(0, vtableIndex);
                        assignIntTemplateArgument(1, receiverStackIndex(signature));
                        assignReferenceLiteralTemplateArgument(2, methodProfileBuilder.methodProfileObject());
                        assignIntTemplateArgument(3, methodProfileBuilder.addMethodProfile(index, MethodInstrumentation.DEFAULT_RECEIVER_METHOD_PROFILE_ENTRIES));
                        emitAndRecordStops(code);
                    } else {
                        // emit an unprofiled virtual dispatch
                        TargetMethod code = getCode(template.resolved);
                        beginBytecode(template.opcode);
                        assignIntTemplateArgument(0, virtualMethodActor.vTableIndex());
                        assignIntTemplateArgument(1, receiverStackIndex(signature));
                        emitAndRecordStops(code);
                    }
                    return;
                } catch (LinkageError e) {
                    // fall through
                }
            }
        } catch (LinkageError error) {
            // Fall back on unresolved template that will cause the error to be rethrown at runtime.
        }
        TargetMethod code = getCode(template);
        beginBytecode(template.opcode);
        assignReferenceLiteralTemplateArgument(0, constantPool.makeResolutionGuard(index, ResolveVirtualMethod.SNIPPET));
        assignIntTemplateArgument(1, receiverStackIndex(signature));
        emitAndRecordStops(code);
    }

    private void emitInvokeinterface(int index, int count) {
        InterfaceMethodRefConstant interfaceMethodRef = constantPool.interfaceMethodAt(index);
        SignatureDescriptor signature = interfaceMethodRef.signature(constantPool);
        Kind kind = invokeKind(signature);
        BytecodeTemplate template = INVOKEINTERFACES.get(kind.asEnum);
        try {
            if (isResolved(interfaceMethodRef, index)) {
                try {
                    InterfaceMethodActor interfaceMethodActor = (InterfaceMethodActor) interfaceMethodRef.resolve(constantPool, index);
                    if (shouldProfileMethodCall(interfaceMethodActor)) {
                        TargetMethod code = getCode(template.instrumented);
                        beginBytecode(template.opcode);
                        assignReferenceLiteralTemplateArgument(0, interfaceMethodActor);
                        assignIntTemplateArgument(1, receiverStackIndex(signature));
                        assignReferenceLiteralTemplateArgument(2, methodProfileBuilder.methodProfileObject());
                        assignIntTemplateArgument(3, methodProfileBuilder.addMethodProfile(index, MethodInstrumentation.DEFAULT_RECEIVER_METHOD_PROFILE_ENTRIES));
                        emitAndRecordStops(code);
                    } else {
                        TargetMethod code = getCode(template.resolved);
                        beginBytecode(template.opcode);
                        assignReferenceLiteralTemplateArgument(0, interfaceMethodActor);
                        assignIntTemplateArgument(1, receiverStackIndex(signature));
                        emitAndRecordStops(code);
                    }
                    return;
                } catch (LinkageError e) {
                    // fall through
                }
            }
        } catch (LinkageError error) {
            // Fall back on unresolved template that will cause the error to be rethrown at runtime.
        }
        TargetMethod code = getCode(template);
        beginBytecode(template.opcode);
        assignReferenceLiteralTemplateArgument(0, constantPool.makeResolutionGuard(index, ResolveInterfaceMethod.SNIPPET));
        assignIntTemplateArgument(1, receiverStackIndex(signature));
        emitAndRecordStops(code);
    }

    private void emitInvokespecial(int index) {
        ClassMethodRefConstant classMethodRef = constantPool.classMethodAt(index);
        Kind kind = invokeKind(classMethodRef.signature(constantPool));
        BytecodeTemplate template = INVOKESPECIALS.get(kind.asEnum);
        try {
            if (isResolved(classMethodRef, index)) {
                VirtualMethodActor virtualMethodActor = classMethodRef.resolveVirtual(constantPool, index);
                TargetMethod code = getCode(template.resolved);
                beginBytecode(template.opcode);
                recordDirectBytecodeCall(code, virtualMethodActor);
                codeBuffer.emit(code);
                return;
            }
        } catch (LinkageError error) {
            // Fall back on unresolved template that will cause the error to be rethrown at runtime.
        }
        TargetMethod code = getCode(template);
        beginBytecode(template.opcode);
        assignReferenceLiteralTemplateArgument(0, constantPool.makeResolutionGuard(index, ResolveSpecialMethod.SNIPPET));
        emitAndRecordStops(code);
    }

    private void emitInvokestatic(int index) {
        ClassMethodRefConstant classMethodRef = constantPool.classMethodAt(index);
        Kind kind = invokeKind(classMethodRef.signature(constantPool));
        BytecodeTemplate template = INVOKESTATICS.get(kind.asEnum);
        try {
            if (isResolved(classMethodRef, index)) {
                StaticMethodActor staticMethodActor = classMethodRef.resolveStatic(constantPool, index);
                if (staticMethodActor.holder().isInitialized()) {
                    TargetMethod code = getCode(template.initialized);
                    beginBytecode(template.opcode);
                    recordDirectBytecodeCall(code, staticMethodActor);
                    codeBuffer.emit(code);
                    return;
                }
            }
        } catch (LinkageError error) {
            // Fall back on unresolved template that will cause the error to be rethrown at runtime.
        }
        TargetMethod code = getCode(template);
        beginBytecode(template.opcode);
        assignReferenceLiteralTemplateArgument(0, constantPool.makeResolutionGuard(index, ResolveStaticMethod.SNIPPET));
        emitAndRecordStops(code);
    }

    /**
     * @param index
     */
    private void emitConstant(int index) {
        PoolConstant constant = constantPool.at(index);
        int bytecode = Bytecodes.LDC;
        switch (constant.tag()) {
            case CLASS: {
                ClassConstant classConstant = (ClassConstant) constant;
                if (isResolved(classConstant, index)) {
                    TargetMethod code = getCode(LDC$reference$resolved);
                    beginBytecode(bytecode);
                    Object mirror = ((ClassActor) classConstant.value(constantPool, index).asObject()).javaClass();
                    assignReferenceLiteralTemplateArgument(0, mirror);
                    emitAndRecordStops(code);
                } else {
                    TargetMethod code = getCode(LDC$reference);
                    beginBytecode(bytecode);
                    assignReferenceLiteralTemplateArgument(0, constantPool.makeResolutionGuard(index, ResolutionSnippet.ResolveClass.SNIPPET));
                    emitAndRecordStops(code);
                }
                break;
            }
            case INTEGER: {
                TargetMethod code = getCode(LDC$int);
                beginBytecode(bytecode);
                IntegerConstant integerConstant = (IntegerConstant) constant;
                assignIntTemplateArgument(0, integerConstant.value());
                emitAndRecordStops(code);
                break;
            }
            case LONG: {
                TargetMethod code = getCode(LDC$long);
                beginBytecode(bytecode);
                LongConstant longConstant = (LongConstant) constant;
                assignLongTemplateArgument(0, longConstant.value());
                emitAndRecordStops(code);
                break;
            }
            case FLOAT: {
                TargetMethod code = getCode(LDC$float);
                beginBytecode(bytecode);
                FloatConstant floatConstant = (FloatConstant) constant;
                assignFloatTemplateArgument(0, floatConstant.value());
                emitAndRecordStops(code);
                break;
            }
            case DOUBLE: {
                TargetMethod code = getCode(LDC$double);
                beginBytecode(bytecode);
                DoubleConstant doubleConstant = (DoubleConstant) constant;
                assignDoubleTemplateArgument(0, doubleConstant.value());
                emitAndRecordStops(code);
                break;
            }
            case STRING: {
                TargetMethod code = getCode(LDC$reference$resolved);
                beginBytecode(bytecode);
                StringConstant stringConstant = (StringConstant) constant;
                assignReferenceLiteralTemplateArgument(0, stringConstant.value);
                emitAndRecordStops(code);
                break;
            }
            default: {
                assert false : "ldc for unexpected constant tag: " + constant.tag();
                break;
            }
        }
    }

    private void emitLookupswitch() {
        align4();
        int defaultOffset = readS4();
        int numberOfCases = readS4();
        if (numberOfCases < 0) {
            throw verifyError("Number of keys in LOOKUPSWITCH less than 0");
        }
        final int start = bci;
        beginBytecode(Bytecodes.LOOKUPSWITCH);
        emitLookupSwitch(opcodeBci, defaultOffset, numberOfCases);
        final int caseBytesRead = bci - start;
        if ((caseBytesRead % 8) != 0 || (caseBytesRead >> 3) != numberOfCases) {
            ProgramError.unexpected("Bytecodes visitor did not consume exactly the offset operands of the tableswitch instruction at " + opcodeBci);
        }
    }

    private void emitMultianewarray(int index, int numberOfDimensions) {
        ClassConstant classRef = constantPool.classAt(index);
        if (isResolved(classRef, index)) {
            TargetMethod code = getCode(MULTIANEWARRAY$resolved);
            beginBytecode(Bytecodes.MULTIANEWARRAY);
            ClassActor arrayClassActor = classRef.resolve(constantPool, index);
            assert arrayClassActor.isArrayClass();
            assert arrayClassActor.numberOfDimensions() >= numberOfDimensions : "dimensionality of array class constant smaller that dimension operand";
            assignReferenceLiteralTemplateArgument(0, arrayClassActor);
            assignReferenceLiteralTemplateArgument(1, new int[numberOfDimensions]);
            // Emit the template
            emitAndRecordStops(code);
            return; // we're done.
        }
        // Unresolved case
        TargetMethod code = getCode(MULTIANEWARRAY);
        beginBytecode(Bytecodes.MULTIANEWARRAY);
        assignReferenceLiteralTemplateArgument(0, constantPool.makeResolutionGuard(index, ResolutionSnippet.ResolveClass.SNIPPET));
        assignReferenceLiteralTemplateArgument(1, new int[numberOfDimensions]);
        emitAndRecordStops(code);
    }

    private void emitNew(int index) {
        ClassConstant classRef = constantPool.classAt(index);
        if (isResolved(classRef, index)) {
            ClassActor classActor = classRef.resolve(constantPool, index);
            if (classActor.isInitialized()) {
                TargetMethod code = getCode(NEW$init);
                beginBytecode(Bytecodes.NEW);
                assignReferenceLiteralTemplateArgument(0, classActor);
                emitAndRecordStops(code);
                return;
            }
        }
        TargetMethod code = getCode(NEW);
        beginBytecode(Bytecodes.NEW);
        assignReferenceLiteralTemplateArgument(0, constantPool.makeResolutionGuard(index, ResolveClassForNew.SNIPPET));
        emitAndRecordStops(code);
    }

    private void emitNewarray(int tag) {
        TargetMethod code = getCode(NEWARRAY);
        beginBytecode(Bytecodes.NEWARRAY);
        Kind arrayElementKind = Kind.fromNewArrayTag(tag);
        assignReferenceLiteralTemplateArgument(0, arrayElementKind);
        emitAndRecordStops(code);
    }

    private void emitTableswitch() {
        align4();
        int defaultOffset = readS4();
        int lowMatch = readS4();
        int highMatch = readS4();
        if (lowMatch > highMatch) {
            throw verifyError("Low must be less than or equal to high in TABLESWITCH");
        }
        int numberOfCases = highMatch - lowMatch + 1;
        int start = bci;
        beginBytecode(Bytecodes.TABLESWITCH);
        emitTableSwitch(lowMatch, highMatch, opcodeBci, defaultOffset, numberOfCases);
        int caseBytesRead = bci - start;
        if ((caseBytesRead % 4) != 0 || (caseBytesRead >> 2) != numberOfCases) {
            ProgramError.unexpected("Bytecodes visitor did not consume exactly the offset operands of the tableswitch instruction at " + opcodeBci);
        }
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
            byte[] code = assembler.toByteArray();
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

    /**
     * Emits a {@link TreeAnchor} incrementor for the current branch target.
     * @param position the position in the bytecode for which to emit the counter
     */
    protected void emitHotpathCounter(int position) {
        if (methodProfileBuilder != null) {
            ProgramWarning.message("HotpathCounters disabled for now: " + position);
        }
    }

    private static final VMBooleanXXOption eagerResolutionOption = VMOptions.register(new VMBooleanXXOption(
                    "-XX:-EagerResolutionByJIT", "Force JIT to perform symbolic resolution instead of using lazy resolution templates."), MaxineVM.Phase.STARTING);

    private final boolean eagerResolution = eagerResolutionOption.getValue();

    @INLINE
    private boolean isResolved(ClassMethodRefConstant classMethodRef, int index) {
        if (eagerResolution) {
            classMethodRef.resolve(constantPool, index);
            return true;
        }
        return classMethodRef.isResolvableWithoutClassLoading(constantPool);
    }

    @INLINE
    private boolean isResolved(InterfaceMethodRefConstant interfaceMethodRef, int index) {
        if (eagerResolution) {
            interfaceMethodRef.resolve(constantPool, index);
            return true;
        }
        return interfaceMethodRef.isResolvableWithoutClassLoading(constantPool);
    }

    @INLINE
    private boolean isResolved(ClassConstant classConstant, int index) {
        if (eagerResolution) {
            classConstant.resolve(constantPool, index);
            return true;
        }
        return classConstant.isResolvableWithoutClassLoading(constantPool);
    }

    @INLINE
    private boolean isResolved(FieldRefConstant fieldRefConstant, int index) {
        if (eagerResolution) {
            fieldRefConstant.resolve(constantPool, index);
            return true;
        }
        return fieldRefConstant.isResolvableWithoutClassLoading(constantPool);
    }

}

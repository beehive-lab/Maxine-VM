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
package com.sun.max.vm.compiler.eir.allocate.linearscan.parts;

import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.profile.*;
import com.sun.max.program.*;
import com.sun.max.util.timer.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.EirAssignment.*;
import com.sun.max.vm.compiler.eir.EirOperand.*;
import com.sun.max.vm.compiler.eir.allocate.linearscan.*;
import com.sun.max.vm.compiler.ir.IrBlock.*;

/**
 * Resolves data flow that is violated by the linear scan allocator splits. Inserts additional moves at block edges.
 *
 * @author Thomas Wuerthinger
 */
public class ResolveDataFlow extends AlgorithmPart {

    // Enable this flag to make sure variables are always spilled across exception edges!
    private static final boolean SPILL_ALL_EXCEPTION_EDGES = false;

    // Timers
    private final Timer _splitsTimer = createTimer("Resolve splits");
    private final Timer _exceptionsTimer = createTimer("Resolve exception edges");
    private final Timer _dataFlowTimer = createTimer("Resolve normal data flow");
    private final Timer _mergeTimer = createTimer("Merge");
    private final Timer _substituteVariablesTimer = createTimer("Substitute variables");
    private final Timer _spillSlotOptimizationTimer = createTimer("Spill slot optimization");

    // Counters
    private final Metrics.Counter _removedExceptionAdapterEdgesCounter = createCounter("Removed exception edges");
    private final Metrics.Counter _catchBlockCounter = createCounter("Catch blocks");
    private final Metrics.Counter _createdExceptionEdgesCounter = createCounter("Created exception edges");
    private final Metrics.Counter _splitsCounter = createCounter("Number of split positions");
    private final Metrics.Counter _assignmentCounter = createCounter("Number of inserted assignments");
    private final Metrics.Counter _splitSlotDefinitionCounter = createCounter("Inserted spills after variable definition");
    private final Metrics.Counter _savedSpillSlotStoresCounter = createCounter("Saved spill slot stores");

    private AppendableSequence<BlockEdge> _blockEdges;

    public ResolveDataFlow() {
        super(10);
    }

    private class ExceptionAdapterEdge extends Edge {

        private EirBlock _catchBlock;
        private EirBlock _block;

        public ExceptionAdapterEdge(EirBlock catchBlock) {
            assert catchBlock.role() == Role.EXCEPTION_DISPATCHER;
            _catchBlock = catchBlock;
        }

        @Override
        protected Type assignmentType() {
            return Type.EXCEPTION_EDGE_RESOLVED;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ExceptionAdapterEdge) {

                final ExceptionAdapterEdge other = (ExceptionAdapterEdge) o;
                if (other._catchBlock != _catchBlock) {
                    return false;
                }

                return super.equals(o);
            }

            return false;
        }

        @Override
        public int hashCode() {
            return super.hashCode() * 31 + _catchBlock.hashCode();
        }

        @Override
        protected EirInstruction determineInsertPosition() {
            final EirBlock block = this.block();
            assert block.instructions().last() instanceof EirJump;
            return block.instructions().last();
        }

        public EirBlock block() {
            if (_block == null) {
                _block = createAdapterBlock(_catchBlock);
            }

            return _block;
        }

        public EirBlock catchBlock() {
            return _catchBlock;
        }
    }

    private class PositionEdge extends Edge {

        private int _position;

        public PositionEdge(int position) {
            _position = position;
        }

        @Override
        protected Type assignmentType() {
            return EirAssignment.Type.INTERVAL_SPLIT;
        }

        @Override
        protected EirInstruction determineInsertPosition() {

            final EirBlock eirBlock = findBlock(_position);
            final EirInstruction eirInstruction = findInstruction(_position);

            EirInstruction result = null;
            if (eirInstruction.number() < _position) {
                result = eirBlock.instructions().get(eirInstruction.index() + 1);
            } else {
                assert eirInstruction.block().instructions().first() == eirInstruction;
                result = eirInstruction;
            }

            if (result instanceof EirCatch) {
                assert result == eirInstruction.block().instructions().first();
                result = eirInstruction.block().instructions().get(1);
            }

            assert !(result instanceof EirCatch);
            return result;
        }
    }

    private class BlockEdge extends Edge {

        private EirBlock _from;
        private EirBlock _to;

        @Override
        protected Type assignmentType() {
            return EirAssignment.Type.DATA_FLOW_RESOLVED;
        }

        public BlockEdge(EirBlock from, EirBlock to) {
            _from = from;
            _to = to;
            assert _to.predecessors().contains(_from);
        }

        @Override
        protected EirInstruction determineInsertPosition() {

            EirInstruction beforeInstruction = null;

            if (_to.predecessors().length() == 1) {

                // Insert position is clear => at the beginning of to, before first instruction
                beforeInstruction = _to.instructions().first();

            } else if (_from.instructions().last() instanceof EirJump && (((EirJump) _from.instructions().last()).target() == _to)) {

                // Insert position is clear => before the jump at the end of _from
                beforeInstruction = _from.instructions().last();

            } else {

                // Unclear position => introduce intermediate block

                final EirBlock newBlock = generation().createEirBlock(_to.role());
                newBlock.setMoveResolverBlock(true);
                generation().addJump(newBlock, _to);
                newBlock.addPredecessor(_from);

                // Substitute blocks
                final VariableMapping<EirBlock, EirBlock> mapping = new ChainedHashMapping<EirBlock, EirBlock>();
                mapping.put(_to, newBlock);
                for (EirInstruction<?, ?> instruction : _from.instructions()) {
                    instruction.substituteSuccessorBlocks(mapping);
                }

                // Subsitute predecessors
                _to.substitutePredecessorBlocks(mapping);

                assert newBlock.predecessors().length() == 1 && newBlock.predecessors().first() == _from;
                assert Sequence.Static.containsIdentical(_from.allUniqueSuccessors(), newBlock);
                assert _to.predecessors().contains(newBlock);
                assert newBlock.allUniqueSuccessors().length() == 1 && newBlock.allUniqueSuccessors().first() == _to;
                assert newBlock.instructions().length() == 1;

                // Insert position is clear => before the jump at the end of the new block
                beforeInstruction = newBlock.instructions().first();
            }

            return beforeInstruction;
        }
    }

    private abstract class Edge {

        private VariableSequence<Pair<Interval, Interval>> _pairs;
        private VariableSequence<Pair<Interval, Interval>> _spilledPairs;

        protected abstract EirInstruction determineInsertPosition();

        protected abstract EirAssignment.Type assignmentType();

        public Edge() {
            _pairs = new ArrayListSequence<Pair<Interval, Interval>>(8);
            _spilledPairs = new ArrayListSequence<Pair<Interval, Interval>>(4);
        }

        @Override
        public int hashCode() {
            return _pairs.hashCode() + 31 * _spilledPairs.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Edge) {
                final Edge other = (Edge) o;
                return other._pairs.equals(_pairs) && other._spilledPairs.equals(_spilledPairs);
            }
            return false;
        }

        public void resolve() {

            if (_spilledPairs.length() == 0 && _pairs.length() == 0) {
                // Nothing to do!
                return;
            }

            final EirInstruction beforeInstruction = determineInsertPosition();

            final EirVariable[] spillVariables = new EirVariable[_spilledPairs.length()];

            int insertedAssignmentsCount = 0;

            // Spill variables to stack
            int z = 0;
            for (Pair<Interval, Interval> spilledPair : _spilledPairs) {
                final Interval first = spilledPair.first();

                assert spilledPair.first().parent() == spilledPair.second().parent();

                final EirVariable spillVariable = first.parent().slotVariable(generation());
                assert spillVariable != first.variable();

                final EirInstruction newInstruction = generation().createAssignment(beforeInstruction.block(), first.variable().kind(), spillVariable, first.variable());
                assert newInstruction instanceof EirAssignment;
                ((EirAssignment) newInstruction).setType(assignmentType());
                insertedAssignmentsCount++;

                generation().introduceInstructionBefore(beforeInstruction, newInstruction);
                spillVariables[z] = spillVariable;
                z++;
            }

            // Insert moves in correct order vor non-spilled variables
            for (Pair<Interval, Interval> pair : _pairs) {
                final Interval first = pair.first();
                final Interval second = pair.second();

                assert first.variable().kind() == second.variable().kind();

                assert first.variable() != second.variable();
                final EirInstruction newInstruction = generation().createAssignment(beforeInstruction.block(), first.variable().kind(), second.variable(), first.variable());
                assert newInstruction instanceof EirAssignment;
                ((EirAssignment) newInstruction).setType(assignmentType());
                insertedAssignmentsCount++;

                generation().introduceInstructionBefore(beforeInstruction, newInstruction);
            }

            // Load spilled variables from stack
            z = 0;
            for (Pair<Interval, Interval> spilledPair : _spilledPairs) {
                final Interval second = spilledPair.second();
                final EirVariable spillVariable = spillVariables[z];
                assert second.variable().kind() == spillVariable.kind();

                assert spillVariable != second.variable();
                final EirInstruction newInstruction = generation().createAssignment(beforeInstruction.block(), second.variable().kind(), second.variable(), spillVariable);
                assert newInstruction instanceof EirAssignment;
                ((EirAssignment) newInstruction).setType(assignmentType());
                insertedAssignmentsCount++;

                generation().introduceInstructionBefore(beforeInstruction, newInstruction);
                z++;
            }

            if (LinearScanRegisterAllocator.DETAILED_COUNTING) {
                _assignmentCounter.accumulate(insertedAssignmentsCount);
            }

            _spilledPairs.clear();
            _pairs.clear();
        }

        public void processIntervalPair(Interval from, Interval to) {
            assert from != to;
            assert from.variable() != to.variable();

            final Pair<Interval, Interval> newPair = new Pair<Interval, Interval>(from, to);

            if (to.parent() == from.parent() && to.parent().hasSlotVariable() && to.parent().spillSlotDefined() && to.variable().location().asStackSlot() != null) {
                if (LinearScanRegisterAllocator.DETAILED_COUNTING) {
                    _savedSpillSlotStoresCounter.increment();
                }
                return;
            }

            int minInsertPosition = 0;
            int maxInsertPosition = _pairs.length();

            int z = 0;
            for (Pair<Interval, Interval> pair : _pairs) {
                if (pair.second().variable().location().equals(newPair.first().variable().location())) {
                    maxInsertPosition = Math.min(maxInsertPosition, z);
                }

                if (pair.first().variable().location().equals(newPair.second().variable().location())) {
                    minInsertPosition = Math.max(minInsertPosition, z + 1);
                }

                z++;
            }

            if (minInsertPosition > maxInsertPosition) {
                // A cycle was detected => we must spill!
                _spilledPairs.append(newPair);
            } else {
                assert minInsertPosition <= maxInsertPosition : "dependency cycle detected!";
                // _pairs.insert(minInsertPosition, newPair);

                // TODO (tw): Check why the following code does not work...

                for (int i = minInsertPosition; i <= maxInsertPosition; i++) {

                    int compareLocations = -1;
                    if (i < _pairs.length() && i < maxInsertPosition) {
                        final Pair<Interval, Interval> otherPair = _pairs.get(i);

                        compareLocations = otherPair.first().compareLocation(from);
                        if (compareLocations == 0) {
                            compareLocations = otherPair.second().compareLocation(to);
                        }
                    }

                    if (compareLocations < 0) {
                        _pairs.insert(i, newPair);
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void doit() {

        _blockEdges = new ArrayListSequence<BlockEdge>(data().generation().eirBlocks().length());

        if (LinearScanRegisterAllocator.DETAILED_TIMING) {
            _spillSlotOptimizationTimer.start();
        }

        // Optimization if variable has only 1 definition and is spilled somewhere
        final AppendableSequence<EirOperand> definitions = new ArrayListSequence<EirOperand>();
        for (ParentInterval parentInterval : data().parentIntervals()) {

            if (parentInterval.hasSlotVariable()) {

                // This variable is spilled somewhere...

                int modificationCount = 0;
                EirOperand modificationOperand = null;

                for (Interval interval : parentInterval.children()) {
                    // This is an interval where the variable does not reside in the spill slot
                    for (EirOperand operand : interval.variable().operands()) {
                        if (operand.effect() == Effect.DEFINITION || operand.effect() == Effect.UPDATE) {
                            modificationCount++;
                            modificationOperand = operand;
                        }
                    }
                }

                if (modificationCount == 1) {
                    parentInterval.setSpillSlotDefined(true);
                    definitions.append(modificationOperand);

                    if (LinearScanRegisterAllocator.DETAILED_COUNTING) {
                        _splitSlotDefinitionCounter.increment();
                    }
                }
            }
        }

        if (LinearScanRegisterAllocator.DETAILED_TIMING) {
            _spillSlotOptimizationTimer.stop();
            _splitsTimer.start();
        }

        if (LinearScanRegisterAllocator.DETAILED_COUNTING) {
            _catchBlockCounter.accumulate(data().splitMoves().keys().length());
        }

        if (LinearScanRegisterAllocator.DETAILED_COUNTING) {
            _splitsCounter.accumulate(data().splitMoves().keys().length());
        }

        for (Integer i : data().splitMoves().keys()) {
            processSplitPosition(i, data().splitMoves().get(i));
        }

        if (LinearScanRegisterAllocator.DETAILED_TIMING) {
            _splitsTimer.stop();
            _dataFlowTimer.start();
        }

        for (final EirBlock block : data().linearScanOrder()) {
            for (final EirBlock succ : block.normalUniqueSuccessors()) {
                assert succ.role() != Role.EXCEPTION_DISPATCHER;
                processBlockPair(block, succ);
            }
        }

        if (LinearScanRegisterAllocator.DETAILED_TIMING) {
            _dataFlowTimer.stop();
            _exceptionsTimer.start();
        }

        for (final EirBlock block : data().linearScanOrder()) {
            if (block.role() == Role.EXCEPTION_DISPATCHER) {
                if (SPILL_ALL_EXCEPTION_EDGES) {
                    processExceptionBlockSpillAll(block);
                } else {
                    processExceptionBlock(block);
                }
            }
        }

        if (LinearScanRegisterAllocator.DETAILED_TIMING) {
            _exceptionsTimer.stop();
            _spillSlotOptimizationTimer.start();
        }

        for (EirOperand modificationOperand : definitions) {
            assert modificationOperand != null;
            final EirInstruction afterInstruction = modificationOperand.instruction();
            final EirVariable variable = modificationOperand.eirValue().asVariable();
            assert variable != null;
            assert variable.interval().parent().hasSlotVariable();
            assert modificationOperand.eirValue() == variable;

            final EirInstruction newInstruction = generation().createAssignment(afterInstruction.block(), variable.kind(), variable.interval().parent().slotVariable(generation()), variable);
            assert newInstruction instanceof EirAssignment;
            ((EirAssignment) newInstruction).setType(EirAssignment.Type.SPILL_SLOT_DEFINITION);
            variable.interval().parent().setSpillSlotDefined(true);
            generation().introduceInstructionAfter(afterInstruction, newInstruction);
        }

        if (LinearScanRegisterAllocator.DETAILED_TIMING) {
            _spillSlotOptimizationTimer.stop();
            _mergeTimer.start();
        }

        // Merge variables such that there is only one variable for one location
        merge();

        if (LinearScanRegisterAllocator.DETAILED_TIMING) {
            _mergeTimer.stop();
        }
    }

    private void merge() {

        final VariableMapping<EirLocation, EirVariable> variableMapping = new ChainedHashMapping<EirLocation, EirVariable>();

        for (EirVariable variable : generation().variables()) {

            assert variable.location() != null;

            if (variable.isLocationFixed()) {
                variableMapping.put(variable.location(), variable);
            } else if (!variableMapping.containsKey(variable.location())) {
                variableMapping.put(variable.location(), variable);
            }
        }

        if (LinearScanRegisterAllocator.DETAILED_TIMING) {
            _substituteVariablesTimer.start();
        }

        for (EirVariable variable : generation().variables()) {
            final EirVariable other = variableMapping.get(variable.location());
            if (other != variable) {
                variable.substituteWith(other);
                assert variable.operands().length() == 0;
            }
        }

        if (LinearScanRegisterAllocator.DETAILED_TIMING) {
            _substituteVariablesTimer.stop();
        }

        generation().clearVariablePool();
        generation().setVariables(variableMapping.values());
    }

    private EirBlock findBlock(int position) {
        for (EirBlock block : generation().eirBlocks()) {
            if (position >= block.beginNumber() && position < block.endNumber()) {
                return block;
            }
        }

        assert false : "position " + position + " is out of range";
        return null;
    }

    private EirInstruction findInstruction(int position) {
        final EirBlock block = findBlock(position);
        for (EirInstruction instruction : block.instructions()) {
            if (position >> 1 == instruction.number() >> 1) {
                return instruction;
            }
        }

        // We are at block start => return first instruction
        return block.instructions().first();
    }

    private void processSplitPosition(int pos, Sequence<Pair<Interval, Interval>> splits) {
        final PositionEdge edge = new PositionEdge(pos);

        for (Pair<Interval, Interval> p : splits) {
            edge.processIntervalPair(p.first(), p.second());
        }

        edge.resolve();
    }

    private EirBlock createAdapterBlock(EirBlock exceptionBlock) {
        assert exceptionBlock.role() == Role.EXCEPTION_DISPATCHER;
        final EirBlock result = generation().createEirBlock(Role.EXCEPTION_DISPATCHER);
        final EirInstruction firstInstruction = exceptionBlock.instructions().first();
        assert firstInstruction instanceof EirCatch;
        final EirCatch eirCatch = (EirCatch) firstInstruction;
        result.insertInstruction(0, new EirCatch(result, eirCatch.catchParameterOperand().eirValue(), eirCatch.catchParameterOperand().location()));
        generation().addJump(result, exceptionBlock);
        return result;
    }

    private void substituteCatchBlocks(Sequence<EirTry> tries, EirBlock from, EirBlock to) {
        final VariableMapping<EirBlock, EirBlock> mapping = new ChainedHashMapping<EirBlock, EirBlock>();
        mapping.put(from, to);
        for (EirTry<?, ?> curTry : tries) {
            assert curTry.catchBlock() == from;
            curTry.substituteSuccessorBlocks(mapping);
            assert curTry.catchBlock() == to;
            to.addPredecessor(curTry.block());
        }

    }

    private boolean traceIntervalParent(ParentInterval interval) {
        Trace.line(1, interval.detailedToString());

        final IndentWriter writer = IndentWriter.traceStreamWriter();
        for (EirBlock block : generation().eirBlocks()) {
            block.printTo(writer);
        }
        writer.flush();
        return false;
    }

    private void processExceptionBlock(EirBlock block) {
        assert block.role() == Role.EXCEPTION_DISPATCHER;

        if (block.liveIn().length() == 0) {
            return;
        }

        if (LinearScanRegisterAllocator.DETAILED_COUNTING) {
            _catchBlockCounter.increment();
        }

        final AppendableSequence<EirTry> tries = new ArrayListSequence<EirTry>();
        for (EirBlock pred : block.predecessors()) {
            for (EirInstruction i : pred.instructions()) {
                if (i instanceof EirTry) {
                    final EirTry curTry = (EirTry) i;
                    if (curTry.catchBlock() == block) {
                        tries.append(curTry);
                    }
                }
            }
        }

        final Sequence<EirBlock> predecessorBackup = new ArrayListSequence<EirBlock>(block.predecessors());
        block.clearPredecessors();

        final VariableMapping<EirTry, ExceptionAdapterEdge> adapterBlockMapping = new ChainedHashMapping<EirTry, ExceptionAdapterEdge>();

        for (EirVariable variable : block.liveIn()) {
            final Interval interval = variable.interval().parent().getChildAt(block.beginNumber());
            assert interval != null || traceIntervalParent(variable.interval().parent());
            final EirVariable variableAtExceptionBlock = interval.variable();

            for (EirTry curTry : tries) {

                assert curTry.catchBlock() == block;

                // Assumption: Assignments cannot cause exceptions!
                int index = curTry.index() + 1;
                EirInstruction next = curTry.block().instructions().get(index);
                while (next instanceof EirAssignment && !(next instanceof EirTry) && next != curTry.block().instructions().last()) {
                    index++;
                    next = curTry.block().instructions().get(index);
                }

                final Interval intervalAtTry = interval.parent().getChildAt(next.number());
                assert intervalAtTry != null || traceIntervalParent(interval.parent());
                final EirVariable variableAtTry = intervalAtTry.variable();

                if (variableAtTry.location() != variableAtExceptionBlock.location()) {

                    if (!adapterBlockMapping.containsKey(curTry)) {
                        adapterBlockMapping.put(curTry, new ExceptionAdapterEdge(block));
                    }

                    adapterBlockMapping.get(curTry).processIntervalPair(intervalAtTry, interval);
                }
            }
        }

        if (LinearScanRegisterAllocator.DETAILED_COUNTING) {
            _catchBlockCounter.accumulate(adapterBlockMapping.keys().length());
        }

        if (adapterBlockMapping.keys().length() > 0) {

            // We have some try instructions that do not have adapter blocks => create empty adapter block
            if (tries.length() > adapterBlockMapping.keys().length()) {

                final AppendableSequence<EirTry> defaultTries = new ArrayListSequence<EirTry>();
                for (EirTry<?, ?> curTry : tries) {
                    if (!adapterBlockMapping.containsKey(curTry)) {
                        defaultTries.append(curTry);
                    }
                }

                assert defaultTries.length() > 0;
                final EirBlock defaultAdapterBlock = createAdapterBlock(block);
                substituteCatchBlocks(defaultTries, block, defaultAdapterBlock);
            }

            final VariableMapping<ExceptionAdapterEdge, AppendableSequence<EirTry>> uniqueEdges = new ChainedHashMapping<ExceptionAdapterEdge, AppendableSequence<EirTry>>(10);
            for (EirTry curTry : adapterBlockMapping.keys()) {
                final ExceptionAdapterEdge edge = adapterBlockMapping.get(curTry);

                final AppendableSequence<EirTry> trySequence = uniqueEdges.get(edge);
                if (trySequence != null) {
                    if (LinearScanRegisterAllocator.DETAILED_COUNTING) {
                        _removedExceptionAdapterEdgesCounter.increment();
                    }
                    trySequence.append(curTry);
                } else {
                    final AppendableSequence<EirTry> newSequence = new ArrayListSequence<EirTry>(4);
                    newSequence.append(curTry);
                    uniqueEdges.put(edge, newSequence);
                }
            }

            for (ExceptionAdapterEdge edge : uniqueEdges.keys()) {
                substituteCatchBlocks(uniqueEdges.get(edge), block, edge.block());
            }

            for (ExceptionAdapterEdge edge : uniqueEdges.keys()) {
                edge.resolve();
            }

            // We have adapter blocks => make this block a normal block
            assert block.instructions().first() instanceof EirCatch;
            assert block.role() == Role.EXCEPTION_DISPATCHER;
            block.setInstruction(0, new EirFiller(block));
            block.setRole(Role.NORMAL);

        } else {

            // Restore predecessors
            for (EirBlock pred : predecessorBackup) {
                block.addPredecessor(pred);
            }
        }
    }

    private void processExceptionBlockSpillAll(EirBlock block) {

        // All registers are cleared when an exception is thrown => determine variables that must be on the stack
        // before an exception can happen.
        final VariableMapping<EirVariable, EirVariable> variableToRescue = new ChainedHashMapping<EirVariable, EirVariable>();
        for (EirVariable variable : block.liveIn()) {
            final Interval interval = variable.interval().parent().getChildAt(block.beginNumber());
            assert interval != null || traceIntervalParent(variable.interval().parent());
            final EirVariable realVariable = interval.variable();
            if (realVariable.location().asRegister() != null) {
                final EirVariable stackVariable = realVariable.interval().parent().slotVariable(generation());
                variableToRescue.put(realVariable, stackVariable);
            } else {
                assert realVariable.location().asStackSlot() != null;
                variableToRescue.put(realVariable, realVariable);
            }
        }

        if (variableToRescue.length() == 0) {
            // Nothing to do
            return;
        }

        // Insert rescuing moves in previous blocks
        for (EirBlock pred : block.predecessors()) {
            final PoolSet<EirVariable> variablesUpdated = PoolSet.noneOf(generation().variablePool());
            for (EirVariable variable : variableToRescue.keys()) {
                variablesUpdated.add(variable);
            }
            final Sequence<EirInstruction> instructions = new ArrayListSequence<EirInstruction>(pred.instructions());

            for (EirInstruction instruction : instructions) {
                if (instruction instanceof EirTry) {
                    final EirTry tryInstruction = (EirTry) instruction;
                    if (tryInstruction.catchBlock() == block) {
                        for (EirVariable variable : variableToRescue.keys()) {
                            if (variablesUpdated.contains(variable)) {
                                final EirVariable stackVariable = variableToRescue.get(variable);
                                final Interval interval = variable.interval().parent().getChildAt(instruction.number());
                                final EirVariable currentVariable = interval.variable();
                                if (currentVariable.location() != stackVariable.location()) {
                                    final EirInstruction newInstruction = generation().createAssignment(pred, variable.kind(), stackVariable, currentVariable);
                                    assert newInstruction instanceof EirAssignment;
                                    ((EirAssignment) newInstruction).setType(EirAssignment.Type.EXCEPTION_EDGE_RESCUED);
                                    generation().introduceInstructionBefore(instruction, newInstruction);
                                    variablesUpdated.remove(variable);
                                }
                            }
                        }
                    }
                }

                instruction.visitOperands(new EirOperand.Procedure() {

                    @Override
                    public void run(EirOperand operand) {
                        if (operand.eirValue() instanceof EirVariable && (operand.effect() == EirOperand.Effect.DEFINITION || operand.effect() == EirOperand.Effect.UPDATE)) {
                            variablesUpdated.add((EirVariable) operand.eirValue());
                        }
                    }
                });
            }
        }

        generation().clearVariablePool();

        // Insert moves in block (exception block)
        for (EirVariable variable : variableToRescue.keys()) {
            final EirVariable stackVariable = variableToRescue.get(variable);
            assert stackVariable.location() instanceof EirStackSlot;
            if (variable.location() != stackVariable.location()) {
                final EirInstruction newInstruction = generation().createAssignment(block, variable.kind(), variable, stackVariable);
                assert newInstruction instanceof EirAssignment;
                ((EirAssignment) newInstruction).setType(EirAssignment.Type.EXCEPTION_EDGE_RESOLVED);
                assert block.instructions().first() instanceof EirCatch;
                generation().introduceInstructionAfter(block.instructions().first(), newInstruction);
            }
        }
    }

    private void processBlockPair(EirBlock block, EirBlock succ) {

        assert succ.role() != Role.EXCEPTION_DISPATCHER;

        final BlockEdge edge = new BlockEdge(block, succ);
        assert assertUniqueBlockEdges(edge);
        _blockEdges.append(edge);

        final VariableMapping<ParentInterval, Interval> mapping = new ChainedHashMapping<ParentInterval, Interval>();
        for (EirVariable variable : block.liveOut()) {
            mapping.put(variable.interval().parent(), variable.interval());
        }

        for (EirVariable variable : succ.liveIn()) {
            final ParentInterval parentInterval = variable.interval().parent();

            // TODO (tw): Check why -2 is necessary; problem can occur when split is exactly at block.endNumber() - 1
            final Interval fromInterval = parentInterval.getChildAt(block.endNumber() - 2);
            assert fromInterval != null;

            assert block.liveOut().contains(variable);

            final Interval toInterval = parentInterval.getChildAt(succ.beginNumber());
            assert toInterval != null;

            if (fromInterval != toInterval && fromInterval.variable().location() != toInterval.variable().location()) {
                edge.processIntervalPair(fromInterval, toInterval);
            }
        }

        edge.resolve();
    }

    private boolean assertUniqueBlockEdges(BlockEdge newEdge) {

        for (BlockEdge e : _blockEdges) {
            assert e != newEdge;
        }

        return true;
    }
}

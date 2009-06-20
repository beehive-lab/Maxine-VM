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

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.profile.*;
import com.sun.max.util.timer.Timer;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.allocate.linearscan.*;
import com.sun.max.vm.type.*;

/**
 * Walk intervals in ascending order and allocate registers for the intervals. If a register cannot be allocated, some
 * intervals need to be split and spilled to stack.
 *
 * @author Thomas Wuerthinger
 */
public class WalkIntervals extends AlgorithmPart {

    private AppendableSequence<Interval> allIntervals;

    private VariableSequence<Interval> unhandled;
    private VariableSequence<Interval> active;
    private VariableSequence<Interval> inactive;
    private AppendableSequence<Interval> handled;

    // Timers
    private final Timer blockedAllocateTimer = createTimer("Blocked allocate");
    private final Timer allocateTimer = createTimer("Allocate");
    private final Timer _searchFreePosTimer = createTimer("Search free pos");
    private final Timer normalSplitTimer = createTimer("Normal split");

    // Counters
    private final Metrics.Counter splitCounter = createCounter("Split count");
    private final Metrics.Counter activeIntervalsCounter = createCounter("Active intervals");
    private final Metrics.Counter _inactiveIntervalsCounter = createCounter("Inactive intervals");
    private final Metrics.Counter samePositionCounter = createCounter("Same Position");
    private final Metrics.Counter floatingIntervalsCounter = createCounter("Floating intervals");
    private final Metrics.Counter _integerIntervalsCounter = createCounter("Integer intervals");
    private final Metrics.Counter fixedIntervalsCounter = createCounter("Fixed intervals");
    private final Metrics.Counter handledIntervalsCounter = createCounter("Handled intervals");
    private final Metrics.Counter unhandledIntervalsCounter = createCounter("Unhandled intervals");


    public WalkIntervals() {
        super(9);
    }

    @Override
    protected boolean assertPreconditions() {
        assert data().sortedIntervals() != null;
        return super.assertPreconditions();
    }

    @Override
    protected boolean assertPostconditions() {

        for (EirVariable variable : generation().variables()) {
            for (EirOperand operand : variable.operands()) {

                if (operand.requiredLocation() != null) {
                    assert variable.location() == operand.requiredLocation();
                }

                if (operand.requiredRegister() != null) {
                    assert variable.location() == operand.requiredRegister();
                }
            }

            final EirRegister register = variable.location().asRegister();
            if (register != null && !variable.isLocationFixed()) {
                if (register.category() == EirLocationCategory.FLOATING_POINT_REGISTER && register.ordinal() < data().floatingPointRegisters().length) {
                    assert data().floatingPointRegisters()[register.ordinal()] != null;
                } else if (register.ordinal() < data().integerRegisters().length) {
                    assert data().integerRegisters()[register.ordinal()] != null;
                }
            }
        }

        return super.assertPostconditions();
    }

    @Override
    protected void doit() {

        final AppendableSequence<Interval> floatIntervals = new LinkSequence<Interval>();
        final AppendableSequence<Interval> integerIntervals = new LinkSequence<Interval>();
        for (Interval interval : data().sortedIntervals()) {
            if (interval.variable().kind() == Kind.FLOAT || interval.variable().kind() == Kind.DOUBLE) {
                floatIntervals.append(interval);
            } else {
                integerIntervals.append(interval);
            }
        }

        if (LinearScanRegisterAllocator.DETAILED_COUNTING) {
            floatingIntervalsCounter.accumulate(floatIntervals.length());
            _integerIntervalsCounter.accumulate(integerIntervals.length());
        }

        doit(floatIntervals, data().floatingPointRegisters());
        doit(integerIntervals, data().integerRegisters());
    }

    private int[] freePos;
    private int[] usePos;
    private int[] blockPos;
    private EirRegister[] registers;

    protected void doit(Sequence<Interval> intervals, EirRegister[] registers) {

        this.registers = registers;

        // Preallocate arrays for performance reasons

        int length = registers.length;
        for (EirRegister reg : registers) {
            if (reg != null) {
                length = Math.max(length, reg.ordinal() + 1);
            }
        }

        freePos = new int[length];
        usePos = new int[length];
        blockPos = new int[length];

        allIntervals = new ArrayListSequence<Interval>(intervals);

        unhandled = new ArrayListSequence<Interval>(intervals.length());
        active = new ArrayListSequence<Interval>(intervals.length());
        inactive = new ArrayListSequence<Interval>(intervals.length());
        handled = new ArrayListSequence<Interval>(intervals.length());

        walk();
    }

    private boolean assertListsCorrect(int outsideOfLists) {

        if (!LinearScanRegisterAllocator.DETAILED_ASSERTIONS) {
            return true;
        }

        // Assert unhandled + active + inactive + handled == all
        Sequence<Interval> total = new LinkSequence<Interval>();
        total = Sequence.Static.concatenated(total, unhandled);
        total = Sequence.Static.concatenated(total, active);
        total = Sequence.Static.concatenated(total, inactive);
        total = Sequence.Static.concatenated(total, handled);
        assert total.length() + outsideOfLists == allIntervals.length();

        // Assert unhandled is sorted ascending
        Interval prev = null;
        for (Interval cur : unhandled) {
            if (prev != null) {
                assert prev.getFirstRangeStart() <= cur.getFirstRangeStart();
            }

            prev = cur;
        }

        // Assert active and inactive intervals have register assigned
        for (Interval interval : active) {
            assert interval.register() != null;
        }
        for (Interval interval : inactive) {
            assert interval.register() != null;
        }

        // Assert handled intervals have location assigned
        for (Interval interval : handled) {
            assert interval.register() != null || interval.stackSlot() != null;
        }

        return true;
    }

    private void walk() {

        assert unhandled.length() == 0;

        for (Interval interval : allIntervals) {

            if (interval.isFixed()) {

                final EirRegister fixedReg = interval.variable().location().asRegister();

                if (fixedReg != null && registers.length > fixedReg.ordinal() && registers[fixedReg.ordinal()] != null) {
                    // Fixed interval with register => make it active!
                    assert interval.register() != null;
                    active.append(interval);
                } else {
                    // Fixed interval with stack slot or unallocatable register assigned => ignore
                    handled.append(interval);
                }
            } else {
                unhandled.append(interval);
            }
        }

        if (LinearScanRegisterAllocator.DETAILED_COUNTING) {
            fixedIntervalsCounter.accumulate(allIntervals.length() - unhandled.length());
        }

        assert assertListsCorrect(0);

        int lastPosition = -1;
        while (unhandled.length() > 0) {

            final Interval current = unhandled.removeFirst();
            final int position = current.getFirstRangeStart();

            if (position != lastPosition) {

                if (LinearScanRegisterAllocator.DETAILED_COUNTING) {
                    activeIntervalsCounter.accumulate(active.length());
                    _inactiveIntervalsCounter.accumulate(inactive.length());
                    handledIntervalsCounter.accumulate(handled.length());
                    unhandledIntervalsCounter.accumulate(unhandled.length());
                }

                lastPosition = position;

                int newIndex = 0;
                for (int i = 0; i < active.length(); i++) {
                    final Interval interval = active.get(i);
                    if (interval.getLastRangeEnd() < position) {
                        // Remove from active, interval is handled
                        handled.append(interval);
                    } else if (!interval.coversIncremental(position)) {
                        // Remove from active, add to inactive
                        inactive.append(interval);
                    } else {
                        active.set(newIndex, interval);
                        newIndex++;
                    }
                }

                while (active.length() > newIndex) {
                    active.removeLast();
                }

                newIndex = 0;
                for (int i = 0; i < inactive.length(); i++) {
                    final Interval interval = inactive.get(i);
                    if (interval.getLastRangeEnd() < position) {
                        // Remove from inactive, interval is handled
                        handled.append(interval);
                    } else if (interval.coversIncremental(position)) {
                        assert interval.register() != null;
                        active.append(interval);
                    } else {
                        inactive.set(newIndex, interval);
                        newIndex++;
                    }
                }

                while (inactive.length() > newIndex) {
                    inactive.removeLast();
                }

            } else {
                if (LinearScanRegisterAllocator.DETAILED_COUNTING) {
                    samePositionCounter.increment();
                }
            }

            if (current.insertMoveWhenActivated()) {
                assert current.parent().children().length() > 1;
                insertMove(current.getFirstRangeStart(), current.parent().previousChild(current), current);
                current.setInsertMoveWhenActivated(false);
            }

            assert !LinearScanRegisterAllocator.DETAILED_ASSERTIONS || assertListsCorrect(1);

            current.setPreferredLocation(findPreferredLocation(current));
            allocateRegister(position, current);

            if (current.stackSlot() != null) {
                // Interval was completely spilled to stack => it is immediately handled
                handled.append(current);
            } else {
                // Interval has an register assigned => it is active
                assert current.register() != null;
                active.append(current);
            }

            assert !LinearScanRegisterAllocator.DETAILED_ASSERTIONS || assertListsCorrect(0);
        }
    }

    private EirLocation findPreferredLocation(Interval interval) {

        final EirVariable variable = interval.variable();

        for (EirOperand operand : variable.operands()) {
            if (operand.instruction() instanceof EirAssignment) {
                final EirAssignment assignment = (EirAssignment) operand.instruction();
                EirOperand other = assignment.sourceOperand();
                if (other == operand) {
                    other = assignment.destinationOperand();
                }

                if (other.eirValue() instanceof EirVariable) {
                    final EirVariable otherVariable = (EirVariable) other.eirValue();
                    if (otherVariable.location() != null) {
                        return otherVariable.location();
                    }
                }
            }
        }

        for (Interval otherIntervals : variable.interval().parent().children()) {
            if (otherIntervals.variable().location() != null) {
                return otherIntervals.variable().location();
            }
        }

        for (EirOperand operand : variable.operands()) {
            if (operand.preferredRegister() != null) {
                return operand.preferredRegister();
            }
        }

        return null;
    }

    private void insertMove(int position, Interval previousChild, Interval current) {
        assert previousChild != null : "current most have a previous split child";
        assert previousChild.variable().location() != null : "location must be already assigned";
        assert current.variable().kind() == previousChild.variable().kind() : "must be of same kind";
        assert previousChild != current;
        assert previousChild.variable() != current.variable();

        if (isBlockBoundary(position)) {
            // No moves needed at block boundary!
            return;
        }

        assert position % 2 == 1;

        // Resolve those moves later on
        data().addSplitMove(position, previousChild, current);
    }

    private void allocateBlockedRegister(int position, Interval current) {
        Arrays.fill(usePos, Integer.MAX_VALUE);
        Arrays.fill(blockPos, Integer.MAX_VALUE);

        for (Interval interval : active) {
            final EirRegister register = interval.register();
            if (interval.isFixed()) {
                // Fixed active
                blockPos[register.ordinal()] = 0;
            } else {
                // Non-fixed active
                final int nextUsage = interval.nextUsageAfter(current.getFirstRangeStart());
                usePos[register.ordinal()] = Math.min(usePos[register.ordinal()], nextUsage);
            }
        }

        for (Interval interval : inactive) {
            final EirRegister register = interval.register();
            final int nextIntersection = interval.firstIntersectionIncremental(position, current);

            if (nextIntersection != -1) {
                assert nextIntersection >= position;

                if (interval.isFixed()) {
                    // Fixed inactive
                    blockPos[register.ordinal()] = Math.min(blockPos[register.ordinal()], nextIntersection);
                } else {
                    // Non-fixed inactive
                    final int nextUsage = interval.nextUsageAfter(current.getFirstRangeStart());
                    usePos[register.ordinal()] = Math.min(usePos[register.ordinal()], nextUsage);
                }
            }
        }

        for (int i = 0; i < usePos.length; i++) {
            usePos[i] = Math.min(usePos[i], blockPos[i]);
        }

        int highestUsePos = Integer.MIN_VALUE;
        int bestRegister = -1;
        for (int i = 0; i < usePos.length; i++) {
            if (registers[i] != null && usePos[i] > highestUsePos) {
                highestUsePos = usePos[i];
                bestRegister = i;
            }
        }

        final int firstCurrentUsage = current.firstUsage();

        if (highestUsePos <= firstCurrentUsage) {
            // all active and inactive intervals are used before current, so it is best to spill current iself
            splitAndSpill(position, current);
        } else {
            if (blockPos[bestRegister] > current.getLastRangeEnd()) {
                // spilling makes a register free for whole current

            } else {
                // spilling made a register free for first part of current
                splitBefore(current, position + 1, blockPos[bestRegister]);
            }

            final Sequence<Interval> active = new ArrayListSequence<Interval>(this.active);
            for (Interval interval : active) {
                if (interval.register() == registers[bestRegister] && interval.intersectsIncremental(current)) {
                    splitAndSpill(position, interval);
                }
            }

            final Sequence<Interval> inactive = new ArrayListSequence<Interval>(this.inactive);
            for (Interval interval : inactive) {
                if (interval.register() == registers[bestRegister] && interval.intersectsIncremental(current)) {
                    splitAndSpill(position, interval);
                }
            }

            assignRegister(current, registers[bestRegister]);
        }
    }

    private void splitAndSpill(int position, Interval interval) {

        assert !interval.isFixed() : "cannot split fixed interval!";

        if (interval.coversIncremental(position)) {
            // Interval is active
            final int nextMustHaveRegister = interval.nextMustHaveRegister(position);

            if (nextMustHaveRegister == Integer.MAX_VALUE) {
                // No register needed for the rest of the lifetime

            } else {
                splitBefore(interval, position + 1, nextMustHaveRegister);
            }

            splitForSpilling(interval, position);

        } else {

            assert !interval.coversIncremental(position);
            splitBefore(interval, position + 1, position + 1);
            assert interval.getLastRangeEnd() <= position;
        }

    }

    private void splitForSpilling(Interval interval, int position) {

        if (position == interval.getFirstRangeStart() || (position % 2 == 0 && position == interval.getFirstRangeStart() + 1)) {
            assert interval.variable().locationCategories().contains(EirLocationCategory.STACK_SLOT) || traceBlocks();
            assignStackSlot(interval);
            int index = Sequence.Static.indexOfIdentical(active, interval);
            if (index != -1) {
                active.remove(index);
                handled.append(interval);
            } else {
                index = Sequence.Static.indexOfIdentical(active, interval);
                if (index != -1) {
                    inactive.remove(index);
                    handled.append(interval);
                }
            }
            return;
        }

        final int maxSplitPos = position;
        final int minSplitPos = Math.max(interval.previousShouldHaveRegister(maxSplitPos) + 1, interval.getFirstRangeStart() + 1);

        assert interval.coversIncremental(position) : "interval must cover position";
        assert minSplitPos <= maxSplitPos : "wrong order";

        int optimalSplitPos = this.findOptimalSplitPos(interval, minSplitPos, maxSplitPos);
        assert minSplitPos <= optimalSplitPos && optimalSplitPos <= maxSplitPos : "out of range";
        assert optimalSplitPos < interval.getLastRangeEnd() : "cannot split after end of interval";
        assert optimalSplitPos >= interval.getFirstRangeStart() : "cannot split before start of interval";

        // move to odd position
        optimalSplitPos = (optimalSplitPos - 1) | 1;

        final Interval spilledPart = split(interval, optimalSplitPos);
        assignStackSlot(spilledPart);

        // Spilled part can be neglected

        insertMove(optimalSplitPos, interval, spilledPart);
    }

    private void allocateRegister(int position, Interval current) {

        if (current.needsFloatingPointRegister()) {
            assert registers == data().floatingPointRegisters();
            allocateRegisterHelper(position, current);
        } else if (current.needsIntegerRegister()) {
            assert registers == data().integerRegisters();
            allocateRegisterHelper(position, current);
        } else {
            assert current.needsStackSlot();
            assignStackSlot(current);
        }
    }

    private void assignStackSlot(Interval interval) {

        assert interval.nextMustHaveRegister(0) == Integer.MAX_VALUE;

        // Reuse stack slot of other intervals; only one stack slot per interval parent.
        interval.assignStackSlot((EirStackSlot) interval.parent().slotVariable(generation()).location());
    }

    private void allocateRegisterHelper(int position, Interval current) {

        if (LinearScanRegisterAllocator.DETAILED_TIMING) {
            allocateTimer.start();
        }
        final boolean ok = allocateFreeRegister(position, current);
        if (LinearScanRegisterAllocator.DETAILED_TIMING) {
            allocateTimer.stop();
        }
        if (!ok) {
            if (LinearScanRegisterAllocator.DETAILED_TIMING) {
                blockedAllocateTimer.start();
            }
            allocateBlockedRegister(position, current);
            if (LinearScanRegisterAllocator.DETAILED_TIMING) {
                blockedAllocateTimer.stop();
            }
        }
    }

    private boolean allocateFreeRegister(int position, Interval current) {

        if (LinearScanRegisterAllocator.DETAILED_TIMING) {
            _searchFreePosTimer.start();
        }

        assert registers.length > 0;

        Arrays.fill(freePos, Integer.MAX_VALUE);

        for (Interval interval : active) {
            assert interval.register() != null;
            freePos[interval.register().ordinal()] = 0;
        }

        for (Interval interval : inactive) {

            final EirRegister register = interval.register();
            if (register != null) {
                final int nextIntersection = interval.firstIntersectionIncremental(position, current);

                if (nextIntersection != -1) {
                    assert nextIntersection >= position;
                    final int ordial = register.ordinal();
                    freePos[ordial] = Math.min(freePos[ordial], nextIntersection);
                }
            }
        }

        int highestFreePos = Integer.MIN_VALUE;
        int bestRegister = -1;
        for (int i = 0; i < freePos.length; i++) {
            if (registers[i] != null && freePos[i] > highestFreePos) {
                highestFreePos = freePos[i];
                bestRegister = i;
            }
        }

        if (current.preferredLocation() != null && current.preferredLocation().asRegister() != null) {

            final int preferredRegister = current.preferredLocation().asRegister().ordinal();
            if (preferredRegister != bestRegister && preferredRegister < registers.length && registers[preferredRegister] != null && freePos[preferredRegister] > position + 1) {
                bestRegister = preferredRegister;
                highestFreePos = freePos[preferredRegister];
            }
        }

        if (LinearScanRegisterAllocator.DETAILED_TIMING) {
            _searchFreePosTimer.stop();
        }

        if (current.register() != null) {
            // This variable has already a register assigned! Make sure this is no problem!
            assert freePos[current.register().ordinal()] > position;
            return true;
        }

        if (highestFreePos == 0 || highestFreePos == position || highestFreePos == position + 1) {
            // Allocation failed
            return false;
        } else if (highestFreePos > current.getLastRangeEnd()) {
            // Register available for whole interval
            assignRegister(current, registers[bestRegister]);
            return true;
        } else {
            if (LinearScanRegisterAllocator.DETAILED_TIMING) {
                normalSplitTimer.start();
            }
            splitBefore(current, position + 1, highestFreePos);
            if (LinearScanRegisterAllocator.DETAILED_TIMING) {
                normalSplitTimer.stop();
            }
            assert current.getLastRangeEnd() <= highestFreePos;
            assignRegister(current, registers[bestRegister]);
            return true;
        }
    }

    private void assignRegister(Interval interval, EirRegister register) {
        interval.assignRegister(register);
        assert assertAssignRegister(interval, register);
    }

    private boolean traceBlocks() {
        final IndentWriter writer = IndentWriter.traceStreamWriter();
        for (EirBlock block : data().linearScanOrder()) {
            block.printTo(writer);
        }
        writer.flush();
        return false;
    }

    private boolean assertAssignRegister(Interval current, EirRegister register) {
        for (Interval interval : allIntervals) {
            if (interval != current && interval.register() != null && interval.register() == register) {
                assert !interval.intersects(current) || traceBlocks();
            }
        }

        return true;
    }

    private void splitBefore(Interval interval, int currentPosition, int highestFreePos) {

        final int minSplitPos = Math.max(interval.previousShouldHaveRegister(highestFreePos), currentPosition);
        final int maxSplitPos = highestFreePos;
        split(interval, minSplitPos, maxSplitPos);
    }

    private void split(Interval interval, int minPos, int maxPos) {
        assert interval.getFirstRangeStart() < minPos : "cannot split before start of interval";
        assert minPos <= maxPos : "invalid order between minPos and maxPos";
        assert maxPos <= interval.getLastRangeEnd() : "cannot split after end of interval";

        int optimalPos = findOptimalSplitPos(interval, minPos, maxPos);

        assert minPos <= optimalPos && optimalPos <= maxPos : "out of range";
        assert !(optimalPos == interval.getLastRangeEnd() && interval.nextMustHaveRegister(minPos) == Integer.MAX_VALUE) : "No split necessary at end of interval!";

        // Move position before actual instruction (odd id)
        optimalPos = (optimalPos - 1) | 1;
        optimalPos = Math.max(optimalPos, minPos);

        // (tw) does not hold?
        // assert optimalPos % 2 == 1 || isBlockBoundary(optimalPos);
        splitAndSchedule(interval, optimalPos);
    }

    private boolean isBlockBoundary(int optimalPos) {
        for (EirBlock block : this.generation().eirBlocks()) {
            if (block.beginNumber() == optimalPos || block.endNumber() == optimalPos + 1 || block.endNumber() == optimalPos) {
                return true;
            }
        }
        return false;
    }

    private int nearBlockBoundary(int optimalPos) {
        for (EirBlock block : this.generation().eirBlocks()) {
            if (block.beginNumber() == optimalPos) {
                return block.beginNumber();
            } else if (block.endNumber() == optimalPos + 1 || block.endNumber() == optimalPos) {
                return block.endNumber();
            }
        }

        assert false;
        return -1;
    }

    private void addToUnhandled(Interval interval) {

        int index = 0;
        for (; index < unhandled.length(); index++) {
            final Interval cur = unhandled.get(index);
            if (interval.getFirstRangeStart() < cur.getFirstRangeStart()) {
                break;
            }
        }
        unhandled.insert(index, interval);
        assert assertListsCorrect(0);
    }

    private Interval split(Interval interval, int pos) {

        assert pos > interval.getFirstRangeStart() || traceBlocks();
        assert pos < interval.getLastRangeEnd() || traceBlocks();
        final Interval splitPart = interval.split(pos, generation().createEirVariable(interval.variable().kind()));

        // Replace occurrence of all variable to new variable after split position
        final EirVariable oldVariable = interval.variable();
        final EirVariable newVariable = splitPart.variable();

        // Find affected operands
        final AppendableSequence<EirOperand> operands = new LinkSequence<EirOperand>();
        for (EirOperand operand : oldVariable.operands()) {

            // Special handling for calls!
            if (operand.instruction().number() >= pos) {
                operands.append(operand);
            }
        }

        // Clear old variable
        for (EirOperand operand : operands) {
            operand.clearEirValue();
        }

        // Set new variable
        for (EirOperand operand : operands) {
            operand.setEirValue(newVariable);
        }

        return splitPart;
    }

    private void splitAndSchedule(Interval interval, int inputPos) {

        int pos = inputPos;

        boolean needsMove = interval.covers(pos);
        if (needsMove && isBlockBoundary(pos)) {

            // Suppress move!
            pos = nearBlockBoundary(pos);
            needsMove = false;
        }

        final Interval splitPart = split(interval, pos);
        assert interval.getLastRangeEnd() <= pos;
        addToUnhandled(splitPart);
        allIntervals.append(splitPart);
        if (needsMove) {
            assert splitPart.getFirstRangeStart() % 2 == 1 || traceBlocks();
            splitPart.setInsertMoveWhenActivated(true);
        } else {
            // No move necessary
        }
    }

    // TODO: Implement better strategy!
    private int findOptimalSplitPos(Interval interval, int minPos, int maxPos) {

        int minLoopDepth = Integer.MAX_VALUE;
        int result = maxPos;

        EirBlock prev = null;
        for (EirBlock block : data().linearScanOrder()) {
            if (minPos <= block.beginNumber() && maxPos >= block.beginNumber()) {
                int loopDepth = block.loopNestingDepth();
                if (prev != null) {
                    loopDepth = Math.min(loopDepth, prev.loopNestingDepth());
                }

                if (loopDepth <= minLoopDepth) {
                    minLoopDepth = loopDepth;
                    result = block.beginNumber();
                }
            }

            prev = block;
        }

        return result;
    }
}

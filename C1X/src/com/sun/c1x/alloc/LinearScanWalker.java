/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.alloc;

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.alloc.Interval.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.debug.*;
import com.sun.c1x.gen.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.util.*;

/**
 *
 * @author Thomas Wuerthinger
 */
final class LinearScanWalker extends IntervalWalker {

    private int firstReg; // the reg. number of the first phys. register
    private int lastReg; // the reg. nmber of the last phys. register
    private int numPhysRegs; // required by current interval
    private boolean adjacentRegs; // have lo/hi words of phys. regs be adjacent

    private final int[] usePos = new int[allocator.nofRegs];
    private final int[] blockPos = new int[allocator.nofRegs];

    private List<Interval>[] spillIntervals = Util.uncheckedCast(new List[allocator.nofRegs]);

    private MoveResolver moveResolver; // for ordering spill moves

    // accessors mapped to same functions in class LinearScan
    int blockCount() {
        return allocator.blockCount();
    }

    BlockBegin blockAt(int idx) {
        return allocator.blockAt(idx);
    }

    BlockBegin blockOfOpWithId(int opId) {
        return allocator.blockOfOpWithId(opId);
    }

    LinearScanWalker(LinearScan allocator, Interval unhandledFixedFirst, Interval unhandledAnyFirst) {
        super(allocator, unhandledFixedFirst, unhandledAnyFirst);
        moveResolver = new MoveResolver(allocator);
        for (int i = 0; i < allocator.nofRegs; i++) {
            spillIntervals[i] = new ArrayList<Interval>(2);
        }
    }

    void initUseLists(boolean onlyProcessUsePos) {
        for (int i = firstReg; i <= lastReg; i++) {
            usePos[i] = Integer.MAX_VALUE;

            if (!onlyProcessUsePos) {
                blockPos[i] = Integer.MAX_VALUE;
                spillIntervals[i].clear();
            }
        }
    }

    void excludeFromUse(int reg) {
        assert reg < allocator.nofRegs : "interval must have a register assigned (stack slots not allowed)";
        if (reg >= firstReg && reg <= lastReg) {
            usePos[reg] = 0;
        }
    }

    void excludeFromUse(Interval i) {
        assert i.assignedReg() != LinearScan.getAnyreg() : "interval has no register assigned";

        excludeFromUse(i.assignedReg());
        excludeFromUse(i.assignedRegHi());
    }

    void setUsePos(int reg, Interval i, int usePos, boolean onlyProcessUsePos) {
        assert usePos != 0 : "must use excludeFromUse to set usePos to 0";

        if (reg >= firstReg && reg <= lastReg) {
            if (this.usePos[reg] > usePos) {
                this.usePos[reg] = usePos;
            }
            if (!onlyProcessUsePos) {
                spillIntervals[reg].add(i);
            }
        }
    }

    void setUsePos(Interval i, int usePos, boolean onlyProcessUsePos) {
        assert i.assignedReg() != LinearScan.getAnyreg() : "interval has no register assigned";
        if (usePos != -1) {
            setUsePos(i.assignedReg(), i, usePos, onlyProcessUsePos);
            setUsePos(i.assignedRegHi(), i, usePos, onlyProcessUsePos);
        }
    }

    void setBlockPos(int reg, Interval i, int blockPos) {
        if (reg >= firstReg && reg <= lastReg) {
            if (this.blockPos[reg] > blockPos) {
                this.blockPos[reg] = blockPos;
            }
            if (usePos[reg] > blockPos) {
                usePos[reg] = blockPos;
            }
        }
    }

    void setBlockPos(Interval i, int blockPos) {
        assert i.assignedReg() != LinearScan.getAnyreg() : "interval has no register assigned";
        if (blockPos != -1) {
            setBlockPos(i.assignedReg(), i, blockPos);
            setBlockPos(i.assignedRegHi(), i, blockPos);
        }
    }

    void freeExcludeActiveFixed() {
        Interval list = activeFirst(IntervalKind.fixedKind);
        while (list != Interval.EndMarker) {
            assert list.assignedReg() < allocator.nofRegs : "active interval must have a register assigned";
            excludeFromUse(list);
            list = list.next();
        }
    }

    void freeExcludeActiveAny() {
        Interval list = activeFirst(IntervalKind.anyKind);
        while (list != Interval.EndMarker) {
            excludeFromUse(list);
            list = list.next();
        }
    }

    void freeCollectInactiveFixed(Interval cur) {
        Interval list = inactiveFirst(IntervalKind.fixedKind);
        while (list != Interval.EndMarker) {
            if (cur.to() <= list.currentFrom()) {
                assert list.currentIntersectsAt(cur) == -1 : "must not intersect";
                setUsePos(list, list.currentFrom(), true);
            } else {
                setUsePos(list, list.currentIntersectsAt(cur), true);
            }
            list = list.next();
        }
    }

    void freeCollectInactiveAny(Interval cur) {
        Interval list = inactiveFirst(IntervalKind.anyKind);
        while (list != Interval.EndMarker) {
            setUsePos(list, list.currentIntersectsAt(cur), true);
            list = list.next();
        }
    }

    void freeCollectUnhandled(IntervalKind kind, Interval cur) {
        Interval list = unhandledFirst(kind);
        while (list != Interval.EndMarker) {
            setUsePos(list, list.intersectsAt(cur), true);
            if (kind == IntervalKind.fixedKind && cur.to() <= list.from()) {
                setUsePos(list, list.from(), true);
            }
            list = list.next();
        }
    }

    void spillExcludeActiveFixed() {
        Interval list = activeFirst(IntervalKind.fixedKind);
        while (list != Interval.EndMarker) {
            excludeFromUse(list);
            list = list.next();
        }
    }

    void spillBlockUnhandledFixed(Interval cur) {
        Interval list = unhandledFirst(IntervalKind.fixedKind);
        while (list != Interval.EndMarker) {
            setBlockPos(list, list.intersectsAt(cur));
            list = list.next();
        }
    }

    void spillBlockInactiveFixed(Interval cur) {
        Interval list = inactiveFirst(IntervalKind.fixedKind);
        while (list != Interval.EndMarker) {
            if (cur.to() > list.currentFrom()) {
                setBlockPos(list, list.currentIntersectsAt(cur));
            } else {
                assert list.currentIntersectsAt(cur) == -1 : "invalid optimization: intervals intersect";
            }

            list = list.next();
        }
    }

    void spillCollectActiveAny() {
        Interval list = activeFirst(IntervalKind.anyKind);
        while (list != Interval.EndMarker) {
            setUsePos(list, Math.min(list.nextUsage(IntervalUseKind.loopEndMarker, currentPosition), list.to()), false);
            list = list.next();
        }
    }

    void spillCollectInactiveAny(Interval cur) {
        Interval list = inactiveFirst(IntervalKind.anyKind);
        while (list != Interval.EndMarker) {
            if (list.currentIntersects(cur)) {
                setUsePos(list, Math.min(list.nextUsage(IntervalUseKind.loopEndMarker, currentPosition), list.to()), false);
            }
            list = list.next();
        }
    }

    void insertMove(int opId, Interval srcIt, Interval dstIt) {
        // output all moves here. When source and target are equal, the move is
        // optimized away later in assignRegNums

        opId = (opId + 1) & ~1;
        BlockBegin opBlock = allocator.blockOfOpWithId(opId);
        assert opId > 0 && allocator.blockOfOpWithId(opId - 2) == opBlock : "cannot insert move at block boundary";

        // calculate index of instruction inside instruction list of current block
        // the minimal index (for a block with no spill moves) can be calculated because the
        // numbering of instructions is known.
        // When the block already contains spill moves, the index must be increased until the
        // correct index is reached.
        List<LIRInstruction> list = opBlock.lir().instructionsList();
        int index = (opId - list.get(0).id()) / 2;
        assert list.get(index).id() <= opId : "error in calculation";

        while (list.get(index).id() != opId) {
            index++;
            assert 0 <= index && index < list.size() : "index out of bounds";
        }
        assert 1 <= index && index < list.size() : "index out of bounds";
        assert list.get(index).id() == opId : "error in calculation";

        // insert new instruction before instruction at position index
        moveResolver.moveInsertPosition(opBlock.lir(), index - 1);
        moveResolver.addMapping(srcIt, dstIt);
    }

    int findOptimalSplitPos(BlockBegin minBlock, BlockBegin maxBlock, int maxSplitPos) {
        int fromBlockNr = minBlock.linearScanNumber();
        int toBlockNr = maxBlock.linearScanNumber();

        assert 0 <= fromBlockNr && fromBlockNr < blockCount() : "out of range";
        assert 0 <= toBlockNr && toBlockNr < blockCount() : "out of range";
        assert fromBlockNr < toBlockNr : "must cross block boundary";

        // Try to split at end of maxBlock. If this would be after
        // maxSplitPos, then use the begin of maxBlock
        int optimalSplitPos = maxBlock.lastLirInstructionId() + 2;
        if (optimalSplitPos > maxSplitPos) {
            optimalSplitPos = maxBlock.firstLirInstructionId();
        }

        int minLoopDepth = maxBlock.loopDepth();
        for (int i = toBlockNr - 1; i >= fromBlockNr; i--) {
            BlockBegin cur = blockAt(i);

            if (cur.loopDepth() < minLoopDepth) {
                // block with lower loop-depth found . split at the end of this block
                minLoopDepth = cur.loopDepth();
                optimalSplitPos = cur.lastLirInstructionId() + 2;
            }
        }
        assert optimalSplitPos > allocator.maxLirOpId() || allocator.isBlockBegin(optimalSplitPos) : "algorithm must move split pos to block boundary";

        return optimalSplitPos;
    }

    int findOptimalSplitPos(Interval it, int minSplitPos, int maxSplitPos, boolean doLoopOptimization) {
        int optimalSplitPos = -1;
        if (minSplitPos == maxSplitPos) {
            // trivial case, no optimization of split position possible
            // Util.traceLinearScan(4, "      min-pos and max-pos are equal, no optimization possible");
            optimalSplitPos = minSplitPos;

        } else {
            assert minSplitPos < maxSplitPos : "must be true then";
            assert minSplitPos > 0 : "cannot access minSplitPos - 1 otherwise";

            // reason for using minSplitPos - 1: when the minimal split pos is exactly at the
            // beginning of a block, then minSplitPos is also a possible split position.
            // Use the block before as minBlock, because then minBlock.lastLirInstructionId() + 2 == minSplitPos
            BlockBegin minBlock = allocator.blockOfOpWithId(minSplitPos - 1);

            // reason for using maxSplitPos - 1: otherwise there would be an assert on failure
            // when an interval ends at the end of the last block of the method
            // (in this case, maxSplitPos == allocator().maxLirOpId() + 2, and there is no
            // block at this opId)
            BlockBegin maxBlock = allocator.blockOfOpWithId(maxSplitPos - 1);

            assert minBlock.linearScanNumber() <= maxBlock.linearScanNumber() : "invalid order";
            if (minBlock == maxBlock) {
                // split position cannot be moved to block boundary : so split as late as possible
                // Util.traceLinearScan(4, "      cannot move split pos to block boundary because minPos and maxPos are in same block");
                optimalSplitPos = maxSplitPos;

            } else {
                if (it.hasHoleBetween(maxSplitPos - 1, maxSplitPos) && !allocator.isBlockBegin(maxSplitPos)) {
                    // Do not move split position if the interval has a hole before maxSplitPos.
                    // Intervals resulting from Phi-Functions have more than one definition (marked
                    // as mustHaveRegister) with a hole before each definition. When the register is needed
                    // for the second definition : an earlier reloading is unnecessary.
                    // Util.traceLinearScan(4, "      interval has hole just before maxSplitPos, so splitting at maxSplitPos");
                    optimalSplitPos = maxSplitPos;

                } else {
                    // seach optimal block boundary between minSplitPos and maxSplitPos
                    // Util.traceLinearScan(4, "      moving split pos to optimal block boundary between block B%d and B%d", minBlock.blockID, maxBlock.blockID);

                    if (doLoopOptimization) {
                        // Loop optimization: if a loop-end marker is found between min- and max-position :
                        // then split before this loop
                        int loopEndPos = it.nextUsageExact(IntervalUseKind.loopEndMarker, minBlock.lastLirInstructionId() + 2);
                        // Util.traceLinearScan(4, "      loop optimization: loop end found at pos %d", loopEndPos);

                        assert loopEndPos > minSplitPos : "invalid order";
                        if (loopEndPos < maxSplitPos) {
                            // loop-end marker found between min- and max-position
                            // if it is not the end marker for the same loop as the min-position : then move
                            // the max-position to this loop block.
                            // Desired result: uses tagged as shouldHaveRegister inside a loop cause a reloading
                            // of the interval (normally, only mustHaveRegister causes a reloading)
                            BlockBegin loopBlock = allocator.blockOfOpWithId(loopEndPos);

                            // Util.traceLinearScan(4, "      interval is used in loop that ends in block B%d, so trying to move maxBlock back from B%d to B%d", loopBlock.blockID, maxBlock.blockID, loopBlock.blockID);
                            assert loopBlock != minBlock : "loopBlock and minBlock must be different because block boundary is needed between";

                            optimalSplitPos = findOptimalSplitPos(minBlock, loopBlock, loopBlock.lastLirInstructionId() + 2);
                            if (optimalSplitPos == loopBlock.lastLirInstructionId() + 2) {
                                optimalSplitPos = -1;
                                // Util.traceLinearScan(4, "      loop optimization not necessary");
                            } else {
                                // Util.traceLinearScan(4, "      loop optimization successful");
                            }
                        }
                    }

                    if (optimalSplitPos == -1) {
                        // not calculated by loop optimization
                        optimalSplitPos = findOptimalSplitPos(minBlock, maxBlock, maxSplitPos);
                    }
                }
            }
        }
        // Util.traceLinearScan(4, "      optimal split position: %d", optimalSplitPos);

        return optimalSplitPos;
    }

// split an interval at the optimal position between minSplitPos and
// maxSplitPos in two parts:
// 1 the left part has already a location assigned
// 2) the right part is sorted into to the unhandled-list
    void splitBeforeUsage(Interval it, int minSplitPos, int maxSplitPos) {
        // Util.traceLinearScan(2, "----- splitting interval: ");
        if (C1XOptions.TraceLinearScanLevel >= 4) {
            it.print(TTY.out, allocator);
        }
        // Util.traceLinearScan(2, "      between %d and %d", minSplitPos, maxSplitPos);

        assert it.from() < minSplitPos : "cannot split at start of interval";
        assert currentPosition() < minSplitPos : "cannot split before current position";
        assert minSplitPos <= maxSplitPos : "invalid order";
        assert maxSplitPos <= it.to() : "cannot split after end of interval";

        int optimalSplitPos = findOptimalSplitPos(it, minSplitPos, maxSplitPos, true);

        assert minSplitPos <= optimalSplitPos && optimalSplitPos <= maxSplitPos : "out of range";
        assert optimalSplitPos <= it.to() : "cannot split after end of interval";
        assert optimalSplitPos > it.from() : "cannot split at start of interval";

        if (optimalSplitPos == it.to() && it.nextUsage(IntervalUseKind.mustHaveRegister, minSplitPos) == Integer.MAX_VALUE) {
            // the split position would be just before the end of the interval
            // . no split at all necessary
            // Util.traceLinearScan(4, "      no split necessary because optimal split position is at end of interval");
            return;
        }

        // must calculate this before the actual split is performed and before split position is moved to odd opId
    boolean moveNecessary = !allocator.isBlockBegin(optimalSplitPos) && !it.hasHoleBetween(optimalSplitPos - 1, optimalSplitPos);

    if (!allocator.isBlockBegin(optimalSplitPos)) {
            // move position before actual instruction (odd opId)
            optimalSplitPos = (optimalSplitPos - 1) | 1;
        }

        // Util.traceLinearScan(4, "      splitting at position %d", optimalSplitPos);
    assert allocator.isBlockBegin(optimalSplitPos) || (optimalSplitPos % 2 == 1) : "split pos must be odd when not on block boundary";
    assert !allocator.isBlockBegin(optimalSplitPos) || (optimalSplitPos % 2 == 0) : "split pos must be even on block boundary";

        Interval splitPart = it.split(optimalSplitPos);

    allocator.appendInterval(splitPart);
    allocator.copyRegisterFlags(it, splitPart);
        splitPart.setInsertMoveWhenActivated(moveNecessary);
        unhandledFirst[IntervalKind.anyKind.ordinal()] = appendToUnhandled(unhandledFirst(IntervalKind.anyKind), splitPart);

        // Util.traceLinearScan(2, "      split interval in two parts (insertMoveWhenActivated: %b)", moveNecessary);
        if (C1XOptions.TraceLinearScanLevel >= 2) {
            TTY.print("      ");
            it.print(TTY.out, allocator);
            TTY.print("      ");
            splitPart.print(TTY.out, allocator);
        }
    }

// split an interval at the optimal position between minSplitPos and
// maxSplitPos in two parts:
// 1) the left part has already a location assigned
// 2) the right part is always on the stack and therefore ignored in further processing

    void splitForSpilling(Interval it) {
        // calculate allowed range of splitting position
        int maxSplitPos = currentPosition();
        int minSplitPos = Math.max(it.previousUsage(IntervalUseKind.shouldHaveRegister, maxSplitPos) + 1, it.from());

        if (C1XOptions.TraceLinearScanLevel >= 2) {
            TTY.print("----- splitting and spilling interval: ");

            it.print(TTY.out, allocator);
            TTY.println("      between %d and %d", minSplitPos, maxSplitPos);
        }

        assert it.state() == IntervalState.activeState : "why spill interval that is not active?";
        assert it.from() <= minSplitPos : "cannot split before start of interval";
        assert minSplitPos <= maxSplitPos : "invalid order";
        assert maxSplitPos < it.to() : "cannot split at end end of interval";
        assert currentPosition() < it.to() : "interval must not end before current position";

        if (minSplitPos == it.from()) {
            // the whole interval is never used, so spill it entirely to memory
            // Util.traceLinearScan(2, "      spilling entire interval because split pos is at beginning of interval");
            assert it.firstUsage(IntervalUseKind.shouldHaveRegister) > currentPosition() : "interval must not have use position before currentPosition";

            allocator.assignSpillSlot(it);
            allocator.changeSpillState(it, minSplitPos);

            // Also kick parent intervals out of register to memory when they have no use
            // position. This avoids short interval in register surrounded by intervals in
            // memory . avoid useless moves from memory to register and back
            Interval parent = it;
            while (parent != null && parent.isSplitChild()) {
                parent = parent.splitChildBeforeOpId(parent.from());

                if (parent.assignedReg() < allocator.nofRegs) {
                    if (parent.firstUsage(IntervalUseKind.shouldHaveRegister) == Integer.MAX_VALUE) {
                        // parent is never used, so kick it out of its assigned register
                        // Util.traceLinearScan(4, "      kicking out interval %d out of its register because it is never used", parent.regNum());
                        allocator.assignSpillSlot(parent);
                    } else {
                        // do not go further back because the register is actually used by the interval
                        parent = null;
                    }
                }
            }

        } else {
            // search optimal split pos, split interval and spill only the right hand part
            int optimalSplitPos = findOptimalSplitPos(it, minSplitPos, maxSplitPos, false);

            assert minSplitPos <= optimalSplitPos && optimalSplitPos <= maxSplitPos : "out of range";
            assert optimalSplitPos < it.to() : "cannot split at end of interval";
            assert optimalSplitPos >= it.from() : "cannot split before start of interval";

            if (!allocator.isBlockBegin(optimalSplitPos)) {
                // move position before actual instruction (odd opId)
                optimalSplitPos = (optimalSplitPos - 1) | 1;
            }

            // Util.traceLinearScan(4, "      splitting at position %d", optimalSplitPos);
            assert allocator.isBlockBegin(optimalSplitPos) || (optimalSplitPos % 2 == 1) : "split pos must be odd when not on block boundary";
            assert !allocator.isBlockBegin(optimalSplitPos) || (optimalSplitPos % 2 == 0) : "split pos must be even on block boundary";

            Interval spilledPart = it.split(optimalSplitPos);
            allocator.appendInterval(spilledPart);
            allocator.assignSpillSlot(spilledPart);
            allocator.changeSpillState(spilledPart, optimalSplitPos);

            if (!allocator.isBlockBegin(optimalSplitPos)) {
                // Util.traceLinearScan(4, "      inserting move from interval %d to %d", it.regNum(), spilledPart.regNum());
                insertMove(optimalSplitPos, it, spilledPart);
            }

            // the currentSplitChild is needed later when moves are inserted for reloading
            assert spilledPart.currentSplitChild() == it : "overwriting wrong currentSplitChild";
            spilledPart.makeCurrentSplitChild();

            if (C1XOptions.TraceLinearScanLevel >= 2) {
                TTY.println("      split interval in two parts");
                TTY.print("      ");
                it.print(TTY.out, allocator);
                TTY.print("      ");
                spilledPart.print(TTY.out, allocator);
            }
        }
    }

    void splitStackInterval(Interval it) {
        int minSplitPos = currentPosition() + 1;
        int maxSplitPos = Math.min(it.firstUsage(IntervalUseKind.shouldHaveRegister), it.to());

        splitBeforeUsage(it, minSplitPos, maxSplitPos);
    }

    void splitWhenPartialRegisterAvailable(Interval it, int registerAvailableUntil) {
        int minSplitPos = Math.max(it.previousUsage(IntervalUseKind.shouldHaveRegister, registerAvailableUntil), it.from() + 1);
        splitBeforeUsage(it, minSplitPos, registerAvailableUntil);
    }

    void splitAndSpillInterval(Interval it) {
        assert it.state() == IntervalState.activeState || it.state() == IntervalState.inactiveState : "other states not allowed";

        int currentPos = currentPosition();
        if (it.state() == IntervalState.inactiveState) {
            // the interval is currently inactive, so no spill slot is needed for now.
            // when the split part is activated, the interval has a new chance to get a register,
            // so in the best case no stack slot is necessary
            assert it.hasHoleBetween(currentPos - 1, currentPos + 1) : "interval can not be inactive otherwise";
            splitBeforeUsage(it, currentPos + 1, currentPos + 1);

        } else {
            // search the position where the interval must have a register and split
            // at the optimal position before.
            // The new created part is added to the unhandled list and will get a register
            // when it is activated
            int minSplitPos = currentPos + 1;
            int maxSplitPos = Math.min(it.nextUsage(IntervalUseKind.mustHaveRegister, minSplitPos), it.to());

            splitBeforeUsage(it, minSplitPos, maxSplitPos);

            assert it.nextUsage(IntervalUseKind.mustHaveRegister, currentPos) == Integer.MAX_VALUE : "the remaining part is spilled to stack and therefore has no register";
            splitForSpilling(it);
        }
    }

    int findFreeReg(int regNeededUntil, int intervalTo, int hintReg, int ignoreReg, boolean[] needSplit) {
        int minFullReg = LinearScan.getAnyreg();
        int maxPartialReg = LinearScan.getAnyreg();

        for (int i = firstReg; i <= lastReg; i++) {
            // TODO: for performance reasons we need something different than a call to isProcessedRegNum
            if (i == ignoreReg || !allocator.isProcessedRegNum(i)) {
                // this register must be ignored

            } else if (usePos[i] >= intervalTo) {
                // this register is free for the full interval
                if (minFullReg == LinearScan.getAnyreg() || i == hintReg || (usePos[i] < usePos[minFullReg] && minFullReg != hintReg)) {
                    minFullReg = i;
                }
            } else if (usePos[i] > regNeededUntil) {
                // this register is at least free until regNeededUntil
                if (maxPartialReg == LinearScan.getAnyreg() || i == hintReg || (usePos[i] > usePos[maxPartialReg] && maxPartialReg != hintReg)) {
                    maxPartialReg = i;
                }
            }
        }

        if (minFullReg != LinearScan.getAnyreg()) {
            return minFullReg;
        } else if (maxPartialReg != LinearScan.getAnyreg()) {
            needSplit[0] = true;
            return maxPartialReg;
        } else {
            return LinearScan.getAnyreg();
        }
    }

    int findFreeDoubleReg(int regNeededUntil, int intervalTo, int hintReg, boolean[] needSplit) {
        assert (lastReg - firstReg + 1) % 2 == 0 : "adjust algorithm";

        int minFullReg = LinearScan.getAnyreg();
        int maxPartialReg = LinearScan.getAnyreg();

        for (int i = firstReg; i < lastReg; i += 2) {
            if (usePos[i] >= intervalTo && usePos[i + 1] >= intervalTo) {
                // this register is free for the full interval
                if (minFullReg == LinearScan.getAnyreg() || i == hintReg || (usePos[i] < usePos[minFullReg] && minFullReg != hintReg)) {
                    minFullReg = i;
                }
            } else if (usePos[i] > regNeededUntil && usePos[i + 1] > regNeededUntil) {
                // this register is at least free until regNeededUntil
                if (maxPartialReg == LinearScan.getAnyreg() || i == hintReg || (usePos[i] > usePos[maxPartialReg] && maxPartialReg != hintReg)) {
                    maxPartialReg = i;
                }
            }
        }

        if (minFullReg != LinearScan.getAnyreg()) {
            return minFullReg;
        } else if (maxPartialReg != LinearScan.getAnyreg()) {
            needSplit[0] = true;
            return maxPartialReg;
        } else {
            return LinearScan.getAnyreg();
        }
    }

    boolean allocFreeReg(Interval cur) {
        if (C1XOptions.TraceLinearScanLevel >= 2) {
            TTY.print("trying to find free register for ");
            cur.print(TTY.out, allocator);
        }

        initUseLists(true);
        freeExcludeActiveFixed();
        freeExcludeActiveAny();
        freeCollectInactiveFixed(cur);
        freeCollectInactiveAny(cur);
        // freeCollectUnhandled(fixedKind, cur);
        assert unhandledFirst(IntervalKind.fixedKind) == Interval.EndMarker : "must not have unhandled fixed intervals because all fixed intervals have a use at position 0";

        // usePos contains the start of the next interval that has this register assigned
        // (either as a fixed register or a normal allocated register in the past)
        // only intervals overlapping with cur are processed, non-overlapping invervals can be ignored safely
        // Util.traceLinearScan(4, "      state of registers:");
        if (C1XOptions.TraceLinearScanLevel >= 4) {
            for (int i = firstReg; i <= lastReg; i++) {
                if (allocator.isProcessedRegNum(i)) {
                    TTY.println("      reg %d: usePos: %d", i, usePos[i]);
                }
            }
        }

        int hintReg;
        int hintRegHi;
        Interval registerHint = cur.registerHint(true, allocator);
        if (registerHint != null) {
            hintReg = registerHint.assignedReg();
            hintRegHi = registerHint.assignedRegHi();

            if (allocator.isPrecoloredInterval.apply(registerHint)) {
                assert hintReg != LinearScan.getAnyreg() && hintRegHi == LinearScan.getAnyreg() : "must be for fixed intervals";
                hintRegHi = hintReg + 1; // connect e.g. eax-edx
            }
            if (C1XOptions.TraceLinearScanLevel >= 4) {
                TTY.print("      hint registers %d, %d from interval ", hintReg, hintRegHi);
                registerHint.print(TTY.out, allocator);
            }

        } else {
            hintReg = LinearScan.getAnyreg();
            hintRegHi = LinearScan.getAnyreg();
        }
        assert hintReg == LinearScan.getAnyreg() || hintReg != hintRegHi : "hint reg and regHi equal";
        assert cur.assignedReg() == LinearScan.getAnyreg() && cur.assignedRegHi() == LinearScan.getAnyreg() : "register already assigned to interval";

        // the register must be free at least until this position
        int regNeededUntil = cur.from() + 1;
        int intervalTo = cur.to();

        boolean[] needSplit = new boolean[1];
        int splitPos = -1;
        int reg = LinearScan.getAnyreg();
        int regHi = LinearScan.getAnyreg();

        if (adjacentRegs) {
            reg = findFreeDoubleReg(regNeededUntil, intervalTo, hintReg, needSplit);
            regHi = reg + 1;
            if (reg == LinearScan.getAnyreg()) {
                return false;
            }
            splitPos = Math.min(usePos[reg], usePos[regHi]);

        } else {
            reg = findFreeReg(regNeededUntil, intervalTo, hintReg, LinearScan.getAnyreg(), needSplit);
            if (reg == LinearScan.getAnyreg()) {
                return false;
            }
            splitPos = usePos[reg];

            if (numPhysRegs == 2) {
                regHi = findFreeReg(regNeededUntil, intervalTo, hintRegHi, reg, needSplit);

                if (usePos[reg] < intervalTo && regHi == LinearScan.getAnyreg()) {
                    // do not split interval if only one register can be assigned until the split pos
                    // (when one register is found for the whole interval, split&spill is only
                    // performed for the hi register)
                    return false;

                } else if (regHi != LinearScan.getAnyreg()) {
                    splitPos = Math.min(splitPos, usePos[regHi]);

                    // sort register numbers to prevent e.g. a move from eax,ebx to ebx,eax
                    if (reg > regHi) {
                        int temp = reg;
                        reg = regHi;
                        regHi = temp;
                    }
                }
            }
        }

        cur.assignReg(reg, regHi);
        // Util.traceLinearScan(2, "selected register %d, %d", reg, regHi);

        assert splitPos > 0 : "invalid splitPos";
        if (needSplit[0]) {
            // register not available for full interval, so split it
            splitWhenPartialRegisterAvailable(cur, splitPos);
        }

        // only return true if interval is completely assigned
        return numPhysRegs == 1 || regHi != LinearScan.getAnyreg();
    }

    int findLockedReg(int regNeededUntil, int intervalTo, int hintReg, int ignoreReg, boolean[] needSplit) {
        int maxReg = LinearScan.getAnyreg();

        for (int i = firstReg; i <= lastReg; i++) {
            if (i == ignoreReg || !allocator.isProcessedRegNum(i)) {
                // this register must be ignored

            } else if (usePos[i] > regNeededUntil) {
                if (maxReg == LinearScan.getAnyreg() || i == hintReg || (usePos[i] > usePos[maxReg] && maxReg != hintReg)) {
                    maxReg = i;
                }
            }
        }

        if (maxReg != LinearScan.getAnyreg() && blockPos[maxReg] <= intervalTo) {
            needSplit[0] = true;
        }

        return maxReg;
    }

    int findLockedDoubleReg(int regNeededUntil, int intervalTo, int hintReg, boolean[] needSplit) {
        assert (lastReg - firstReg + 1) % 2 == 0 : "adjust algorithm";

        int maxReg = LinearScan.getAnyreg();

        for (int i = firstReg; i < lastReg; i += 2) {
            if (usePos[i] > regNeededUntil && usePos[i + 1] > regNeededUntil) {
                if (maxReg == LinearScan.getAnyreg() || usePos[i] > usePos[maxReg]) {
                    maxReg = i;
                }
            }
        }

        if (blockPos[maxReg] <= intervalTo || blockPos[maxReg + 1] <= intervalTo) {
            needSplit[0] = true;
        }

        return maxReg;
    }

    void splitAndSpillIntersectingIntervals(int reg, int regHi) {
        assert reg != LinearScan.getAnyreg() : "no register assigned";

        for (int i = 0; i < spillIntervals[reg].size(); i++) {
            Interval it = spillIntervals[reg].get(i);
            removeFromList(it);
            splitAndSpillInterval(it);
        }

        if (regHi != LinearScan.getAnyreg()) {
            List<Interval> processed = spillIntervals[reg];
            for (int i = 0; i < spillIntervals[regHi].size(); i++) {
                Interval it = spillIntervals[regHi].get(i);
                if (!processed.contains(it)) {
                    removeFromList(it);
                    splitAndSpillInterval(it);
                }
            }
        }
    }

    // Split an Interval and spill it to memory so that cur can be placed in a register
    void allocLockedReg(Interval cur) {
      if (C1XOptions.TraceLinearScanLevel >= 2) {
          TTY.print("need to split and spill to get register for "); cur.print(TTY.out, allocator);
      }

      // collect current usage of registers
      initUseLists(false);
      spillExcludeActiveFixed();
    //  spillBlockUnhandledFixed(cur);
      assert unhandledFirst(IntervalKind.fixedKind) == Interval.EndMarker :  "must not have unhandled fixed intervals because all fixed intervals have a use at position 0";
      spillBlockInactiveFixed(cur);
      spillCollectActiveAny();
      spillCollectInactiveAny(cur);

      if (C1XOptions.TraceLinearScanLevel >= 4) {
        TTY.println("      state of registers:");
        for (int i = firstReg; i <= lastReg; i++) {
            if (allocator.isProcessedRegNum(i)) {
              TTY.print("      reg %d: usePos: %d, blockPos: %d, intervals: ", i, usePos[i], blockPos[i]);
              for (int j = 0; j < spillIntervals[i].size(); j++) {
                TTY.print("%d ", spillIntervals[i].get(j).registerNumber());
              }
              TTY.println();
            }
        }
      }

      // the register must be free at least until this position
      int regNeededUntil = Math.min(cur.firstUsage(IntervalUseKind.mustHaveRegister), cur.from() + 1);
      int intervalTo = cur.to();
      assert regNeededUntil > 0 && regNeededUntil < Integer.MAX_VALUE : "interval has no use";

      int splitPos = 0;
      int usePos = 0;
      boolean[] needSplit = new boolean[1];
      int reg;
      int regHi;

      if (adjacentRegs) {
        reg = findLockedDoubleReg(regNeededUntil, intervalTo, LinearScan.getAnyreg(), needSplit);
        regHi = reg + 1;

        if (reg != LinearScan.getAnyreg()) {
          usePos = Math.min(this.usePos[reg], this.usePos[regHi]);
          splitPos = Math.min(blockPos[reg], blockPos[regHi]);
        }
      } else {
        reg = findLockedReg(regNeededUntil, intervalTo, LinearScan.getAnyreg(), cur.assignedReg(), needSplit);
        regHi = LinearScan.getAnyreg();

        if (reg != LinearScan.getAnyreg()) {
          usePos = this.usePos[reg];
          splitPos = blockPos[reg];

          if (numPhysRegs == 2) {
            if (cur.assignedReg() != LinearScan.getAnyreg()) {
              regHi = reg;
              reg = cur.assignedReg();
            } else {
              regHi = findLockedReg(regNeededUntil, intervalTo, LinearScan.getAnyreg(), reg, needSplit);
              if (regHi != LinearScan.getAnyreg()) {
                usePos = Math.min(usePos, this.usePos[regHi]);
                splitPos = Math.min(splitPos, blockPos[regHi]);
              }
            }

            if (regHi != LinearScan.getAnyreg() && reg > regHi) {
              // sort register numbers to prevent e.g. a move from eax : ebx to ebx : eax
              int temp = reg;
              reg = regHi;
              regHi = temp;
            }
          }
        }
      }

      if (reg == LinearScan.getAnyreg() || (numPhysRegs == 2 && regHi == LinearScan.getAnyreg()) || usePos <= cur.firstUsage(IntervalUseKind.mustHaveRegister)) {
        // the first use of cur is later than the spilling position . spill cur
        // Util.traceLinearScan(4, "able to spill current interval. firstUsage(register): %d, usePos: %d", cur.firstUsage(IntervalUseKind.mustHaveRegister), usePos);

        if (cur.firstUsage(IntervalUseKind.mustHaveRegister) <= cur.from() + 1) {
          assert false : "cannot spill interval that is used in first instruction (possible reason: no register found)";
          // assign a reasonable register and do a bailout in product mode to avoid errors
            allocator.assignSpillSlot(cur);
          throw new CiBailout("LinearScan: no register found");
        }

        splitAndSpillInterval(cur);
      } else {
        // Util.traceLinearScan(4, "decided to use register %d, %d", reg, regHi);
        assert reg != LinearScan.getAnyreg() && (numPhysRegs == 1 || regHi != LinearScan.getAnyreg()) : "no register found";
        assert splitPos > 0 : "invalid splitPos";
        assert !needSplit[0] == false || splitPos > cur.from() : "splitting interval at from";

        cur.assignReg(reg, regHi);
        if (needSplit[0]) {
          // register not available for full interval :  so split it
          splitWhenPartialRegisterAvailable(cur, splitPos);
        }

        // perform splitting and spilling for all affected intervalls
        splitAndSpillIntersectingIntervals(reg, regHi);
      }
    }

    boolean noAllocationPossible(Interval cur) {

        if (compilation.target.arch.isX86()) {
            // fast calculation of intervals that can never get a register because the
            // the next instruction is a call that blocks all registers
            // Note: this does not work if callee-saved registers are available (e.g. on Sparc)

            // check if this interval is the result of a split operation
            // (an interval got a register until this position)
            int pos = cur.from();
            if ((pos & 1) == 1) {
                // the current instruction is a call that blocks all registers
                if (pos < allocator.maxLirOpId() && allocator.hasCall(pos + 1)) {
                    // Util.traceLinearScan(4, "      free register cannot be available because all registers blocked by following call");

                    // safety check that there is really no register available
                    assert !allocFreeReg(cur) : "found a register for this interval";
                    return true;
                }

            }
        }
        return false;
    }

    void initVarsForAlloc(Interval cur) {
        CiKind type = cur.type();
        numPhysRegs = allocator.numPhysicalRegs(type);
        adjacentRegs = allocator.requiresAdjacentRegs(type);

        if (pdInitRegsForAlloc(cur)) {
            // the appropriate register range was selected.
        } else if (type == CiKind.Float || type == CiKind.Double) {
            assert false : "should not reach here!";
        } else {
            firstReg = allocator.pdFirstCpuReg;
            lastReg = allocator.pdLastCpuReg;
        }

        assert 0 <= firstReg && firstReg < allocator.nofRegs : "out of range";
        assert 0 <= lastReg && lastReg < allocator.nofRegs : "out of range";
    }
    // TODO: Platform specific!

    private boolean pdInitRegsForAlloc(Interval cur) {
        assert compilation.target.arch.isX86();
        if (allocator.gen().isVregFlagSet(cur.registerNumber(), LIRGenerator.VregFlag.ByteReg)) {
            assert cur.type() != CiKind.Float && cur.type() != CiKind.Double : "cpu regs only";
            firstReg = allocator.pdFirstByteReg;
            lastReg = allocator.pdLastByteReg;
            return true;
        } else if (cur.type() == CiKind.Float || cur.type() == CiKind.Double) {
            firstReg = allocator.pdFirstXmmReg;
            lastReg = allocator.pdLastXmmReg;
            return true;
        }

        return false;
    }

    boolean isMove(LIRInstruction op, Interval from, Interval to) {
        if (op.code != LIROpcode.Move) {
            return false;
        }
        assert op instanceof LIROp1 : "move must be LIROp1";

        LIROperand in = ((LIROp1) op).operand();
        LIROperand res = ((LIROp1) op).result();
        return in.isVirtual() && res.isVirtual() && in.vregNumber() == from.registerNumber() && res.vregNumber() == to.registerNumber();
    }

    // optimization (especially for phi functions of nested loops):
    // assign same spill slot to non-intersecting intervals
    void combineSpilledIntervals(Interval cur) {
        if (cur.isSplitChild()) {
            // optimization is only suitable for split parents
            return;
        }

        Interval registerHint = cur.registerHint(false, allocator);
        if (registerHint == null) {
            // cur is not the target of a move : otherwise registerHint would be set
            return;
        }
        assert registerHint.isSplitParent() : "register hint must be split parent";

        if (cur.spillState() != IntervalSpillState.noOptimization || registerHint.spillState() != IntervalSpillState.noOptimization) {
            // combining the stack slots for intervals where spill move optimization is applied
            // is not benefitial and would cause problems
            return;
        }

        int beginPos = cur.from();
        int endPos = cur.to();
        if (endPos > allocator.maxLirOpId() || (beginPos & 1) != 0 || (endPos & 1) != 0) {
            // safety check that lirOpWithId is allowed
            return;
        }

        if (!isMove(allocator.lirOpWithId(beginPos), registerHint, cur) || !isMove(allocator.lirOpWithId(endPos), cur, registerHint)) {
            // cur and registerHint are not connected with two moves
            return;
        }

        Interval beginHint = registerHint.splitChildAtOpId(beginPos, LIRInstruction.OperandMode.InputMode, allocator);
        Interval endHint = registerHint.splitChildAtOpId(endPos, LIRInstruction.OperandMode.OutputMode, allocator);
        if (beginHint == endHint || beginHint.to() != beginPos || endHint.from() != endPos) {
            // registerHint must be split : otherwise the re-writing of use positions does not work
            return;
        }

        assert beginHint.assignedReg() != LinearScan.getAnyreg() : "must have register assigned";
        assert endHint.assignedReg() == LinearScan.getAnyreg() : "must not have register assigned";
        assert cur.firstUsage(IntervalUseKind.mustHaveRegister) == beginPos : "must have use position at begin of interval because of move";
        assert endHint.firstUsage(IntervalUseKind.mustHaveRegister) == endPos : "must have use position at begin of interval because of move";

        if (beginHint.assignedReg() < allocator.nofRegs) {
            // registerHint is not spilled at beginPos : so it would not be benefitial to immediately spill cur
            return;
        }
        assert registerHint.canonicalSpillSlot() != -1 : "must be set when part of interval was spilled";

        // modify intervals such that cur gets the same stack slot as registerHint
        // delete use positions to prevent the intervals to get a register at beginning
        cur.setCanonicalSpillSlot(registerHint.canonicalSpillSlot());
        cur.removeFirstUsePos();
        endHint.removeFirstUsePos();
    }

    // allocate a physical register or memory location to an interval
    @Override
    boolean activateCurrent() {
        Interval cur = current();
        boolean result = true;

        if (C1XOptions.TraceLinearScanLevel >= 2) {
            TTY.print("+++++ activating interval ");
            cur.print(TTY.out, allocator);
        }

        if (C1XOptions.TraceLinearScanLevel >= 4) {
            TTY.println("      splitParent: %d, insertMoveWhenActivated: %b", cur.splitParent().registerNumber(), cur.insertMoveWhenActivated());
        }

        if (cur.assignedReg() >= allocator.nofRegs) {
            // activating an interval that has a stack slot assigned . split it at first use position
            // used for method parameters
            // Util.traceLinearScan(4, "      interval has spill slot assigned (method parameter) . split it before first use");

            splitStackInterval(cur);
            result = false;

        } else {
            if (allocator.gen().isVregFlagSet(cur.registerNumber(), LIRGenerator.VregFlag.MustStartInMemory)) {
                // activating an interval that must start in a stack slot : but may get a register later
                // used for lirRoundfp: rounding is done by store to stack and reload later
                // Util.traceLinearScan(4, "      interval must start in stack slot . split it before first use");
                assert cur.assignedReg() == LinearScan.getAnyreg() && cur.assignedRegHi() == LinearScan.getAnyreg() : "register already assigned";

                allocator.assignSpillSlot(cur);
                splitStackInterval(cur);
                result = false;

            } else if (cur.assignedReg() == LinearScan.getAnyreg()) {
                // interval has not assigned register . normal allocation
                // (this is the normal case for most intervals)
                // Util.traceLinearScan(4, "      normal allocation of register");

                // assign same spill slot to non-intersecting intervals
                combineSpilledIntervals(cur);

                initVarsForAlloc(cur);
                if (noAllocationPossible(cur) || !allocFreeReg(cur)) {
                    // no empty register available.
                    // split and spill another interval so that this interval gets a register
                    allocLockedReg(cur);
                }

                // spilled intervals need not be move to active-list
                if (cur.assignedReg() >= allocator.nofRegs) {
                    result = false;
                }
            }
        }

        // load spilled values that become active from stack slot to register
        if (cur.insertMoveWhenActivated()) {
            assert cur.isSplitChild() : "must be";
            assert cur.currentSplitChild() != null : "must be";
            assert cur.currentSplitChild().registerNumber() != cur.registerNumber() : "cannot insert move between same interval";
            // Util.traceLinearScan(4, "Inserting move from interval %d to %d because insertMoveWhenActivated is set", cur.currentSplitChild().regNum(), cur.regNum());

            insertMove(cur.from(), cur.currentSplitChild(), cur);
        }
        cur.makeCurrentSplitChild();

        return result; // true = interval is moved to active list
    }

    public void finishAllocation() {
        // must be called when all intervals are allocated
        moveResolver.resolveAndAppendMoves();
    }
}

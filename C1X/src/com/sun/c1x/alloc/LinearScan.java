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
import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.ir.BlockBegin.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;

/**
 * The linear scan register allocator from Christian Wimmer.
 * @author Thomas Wuerthinger
 */
public class LinearScan {

    final C1XCompilation compilation;
    final IR ir;
    final LIRGenerator gen;
    final FrameMap frameMap;

    final CiRegister.AllocationSet allocatableRegisters;

    /**
     * List of blocks in linear-scan order. This is only correct as long as the CFG does not change.
     */
    final BlockBegin[] sortedBlocks;

    /**
     * Number of variables (without new registers introduced because of splitting intervals).
     */
    final int numVariables;

    /**
     * Number of calls in the method.
     */
    int numCalls;

    /**
     * Number of stack slots used for intervals allocated to memory.
     */
    int maxSpills;

    /**
     * Unused spill slot for a single-word value because of alignment of a double-word value.
     */
    int unusedSpillSlot;

    /**
     * Mapping from register number to interval.
     */
    Interval[] intervalsMap;

    /**
     * The number of valid entries in {@link #intervalsMap}.
     */
    int intervalsMapSize;

    /**
     * List of intervals created during allocation when an existing interval is split.
     */
    final List<Interval> newIntervalsFromAllocation;

    /**
     * Intervals sorted by {@link Interval#from()}.
     */
    Interval[] sortedIntervals;

    /**
     * Map from {@link LIRInstruction#id} to {@link LIRInstruction} node.
     */
    LIRInstruction[] idToInstructionMap;
    BlockBegin[] instructionToBlockMap; // mapping from LIRInstruction id to the BlockBegin containing this instruction
    BitMap hasInfo; // bit set for each LIRInstruction id that has a CodeEmitInfo
    BitMap hasCall; // bit set for each LIRInstruction id that destroys all caller save registers
    BitMap2D intervalInLoop; // bit set for each variable that is contained in each loop
    private final int numRegs;

    // Implementation of LinearScan

    public LinearScan(C1XCompilation compilation, IR ir, LIRGenerator gen, FrameMap frameMap) {
        this.compilation = compilation;
        this.ir = ir;
        this.gen = gen;
        this.frameMap = frameMap;
        this.numVariables = gen.maxVirtualRegisterNumber();
        this.numCalls = -1;
        this.maxSpills = 0;
        this.unusedSpillSlot = -1;
        this.newIntervalsFromAllocation = new ArrayList<Interval>();
        this.sortedBlocks = ir.linearScanOrder().toArray(new BlockBegin[ir.linearScanOrder().size()]);
        this.allocatableRegisters = compilation.target.allocatableRegs;
        this.numRegs = allocatableRegisters.nofRegs;
    }

    // * functions for converting LIR-Operands to register numbers
    //
    // Emulate a flat register file comprising physical integer registers,
    // physical floating-point registers and variables, in that order.
    // variables already have appropriate numbers, since V0 is
    // the number of physical registers.
    // Returns -1 for hi word if opr is a single word operand.
    //
    // Note: the inverse operation (calculating an operand for register numbers)
    // is done in calcOperandForInterval()

    int regNum(CiValue opr) {
        assert opr.isVariableOrRegister() : "should not call this otherwise";

        if (opr.isVariable()) {
            assert opr.variableNumber() >= numRegs : "found a variable with a fixed-register number";
            return opr.variableNumber();
        }
        return opr.asRegister().number;
    }

    int regNumHi(CiValue opr) {
        assert opr.isVariableOrRegister() : "should not call this otherwise";
        return -1;
    }

    // * functions for classification of intervals

    boolean isPrecoloredInterval(Interval i) {
        return i.registerNumber() < numRegs;
    }

    final IntervalClosure isPrecoloredInterval = new IntervalClosure() {
        @Override
        public boolean apply(Interval i) {
            return isCpu(i.registerNumber()) || isXmm(i.registerNumber());
        }
    };

    final IntervalClosure isVirtualInterval = new IntervalClosure() {
        @Override
        public boolean apply(Interval i) {
            return i.registerNumber() >= CiRegister.LowestVirtualRegisterNumber;
        }
    };

    final IntervalClosure isOopInterval = new IntervalClosure() {
        @Override
        public boolean apply(Interval i) {
            // fixed intervals never contain oops
            return i.registerNumber() >= numRegs && i.kind() == CiKind.Object;
        }
    };

    // * General helper functions

    // compute next unused stack index that can be used for spilling
    int allocateSpillSlot(boolean doubleWord) {
        int spillSlot;
        if (doubleWord) {
            if ((maxSpills & 1) == 1) {
                // alignment of double-word values
                // the hole because of the alignment is filled with the next single-word value
                assert unusedSpillSlot == -1 : "wasting a spill slot";
                unusedSpillSlot = maxSpills;
                maxSpills++;
            }
            spillSlot = maxSpills;
            maxSpills += 2;
        } else if (unusedSpillSlot != -1) {
            // re-use hole that was the result of a previous double-word alignment
            spillSlot = unusedSpillSlot;
            unusedSpillSlot = -1;
        } else {
            spillSlot = maxSpills;
            maxSpills++;
        }

        return spillSlot + numRegs;
    }

    void assignSpillSlot(Interval it) {
        // assign the canonical spill slot of the parent (if a part of the interval
        // is already spilled) or allocate a new spill slot
        if (it.canonicalSpillSlot() >= 0) {
            it.assignReg(it.canonicalSpillSlot());
        } else {
            int spill = allocateSpillSlot(numberOfSpillSlots(it.kind()) == 2);
            it.setCanonicalSpillSlot(spill);
            it.assignReg(spill);
        }
    }

    // create a new interval with a predefined regNum
    // (only used for parent intervals that are created during the building phase)
    Interval createInterval(int regNum) {
        assert intervalsMap[regNum] == null : "overwriting exisiting interval";
        assert isProcessedRegNum(regNum);

        Interval interval = new Interval(regNum);
        intervalsMap[regNum] = interval;

        // assign register number for precolored intervals
        if (regNum < CiRegister.LowestVirtualRegisterNumber) {
            interval.assignReg(regNum);
        }
        return interval;
    }

    // assign a new regNum to the interval and append it to the list of intervals
    // (only used for child intervals that are created during register allocation)
    void appendInterval(Interval it) {
        newIntervalsFromAllocation.add(it);
        if (intervalsMapSize >= intervalsMap.length) {
            intervalsMap = Arrays.copyOf(intervalsMap, intervalsMap.length * 2);
        }
        intervalsMap[intervalsMapSize] = it;
        it.setRegisterNumber(intervalsMapSize++);
    }

    // copy the variable flags if an interval is split
    void copyRegisterFlags(Interval from, Interval to) {
        if (gen.isVarFlagSet(from.registerNumber(), LIRGenerator.VariableFlag.MustBeByteReg)) {
            gen.setVarFlag(to.registerNumber(), LIRGenerator.VariableFlag.MustBeByteReg);
        }

        // Note: do not copy the mustStartInMemory flag because it is not necessary for child
        // intervals (only the very beginning of the interval must be in memory)
    }

    // access to block list (sorted in linear scan order)
    int blockCount() {
        assert sortedBlocks.length == ir.linearScanOrder().size() : "invalid cached block list";
        return sortedBlocks.length;
    }

    BlockBegin blockAt(int idx) {
        assert sortedBlocks[idx] == ir.linearScanOrder().get(idx) : "invalid cached block list";
        return sortedBlocks[idx];
    }

    // size of liveIn and liveOut sets of BasicBlocks (BitMap needs rounded size for iteration)
    int liveSetSize() {
        return Util.roundUp(numVariables, compilation.target.arch.wordSize * Byte.SIZE);
    }

    int numLoops() {
        return ir.numLoops();
    }

    boolean isIntervalInLoop(int interval, int loop) {
        return intervalInLoop.at(interval, loop);
    }

    // access to interval list
    int intervalCount() {
        return intervalsMapSize;
    }

    Interval intervalAt(int regNum) {
        assert regNum >= 0 && regNum < intervalsMapSize : "attempt to access non existant interval";
        return intervalsMap[regNum];
    }

    List<Interval> newIntervalsFromAllocation() {
        return newIntervalsFromAllocation;
    }

    // access to LIROps and Blocks indexed by opId
    int maxLirOpId() {
        assert idToInstructionMap.length > 0 : "no operations";
        return (idToInstructionMap.length - 1) << 1;
    }

    /**
     * Retrieves the {@link LIRInstruction} based on its {@linkplain LIRInstruction#id id}.
     *
     * @param id the id of an instruction
     * @return the instruction whose {@linkplain LIRInstruction#id} {@code == id}
     */
    LIRInstruction instructionForId(int id) {
        assert id >= 0 && id <= maxLirOpId() && id % 2 == 0 : "id out of range or not even";
        assert idToInstructionMap[id >> 1].id == id;
        return idToInstructionMap[id >> 1];
    }

    BlockBegin blockOfOpWithId(int opId) {
        assert instructionToBlockMap.length > 0 && opId >= 0 && opId <= maxLirOpId() + 1 : "opId out of range";
        return instructionToBlockMap[opId >> 1];
    }

    boolean isBlockBegin(int opId) {
        return opId == 0 || blockOfOpWithId(opId) != blockOfOpWithId(opId - 1);
    }

    boolean coversBlockBegin(int opId1, int opId2) {
        return blockOfOpWithId(opId1) != blockOfOpWithId(opId2);
    }

    boolean hasCall(int opId) {
        assert opId % 2 == 0 : "must be even";
        return hasCall.get(opId >> 1);
    }

    // functions for converting LIR-Operands to register numbers
    static boolean isValidRegNum(int regNum) {
        return regNum >= 0;
    }

    public int numCalls() {
        assert numCalls >= 0 : "not set";
        return numCalls;
    }

    // * spill move optimization
    // eliminate moves from register to stack if stack slot is known to be correct

    // called during building of intervals
    void changeSpillDefinitionPos(Interval interval, int defPos) {
        assert interval.isSplitParent() : "can only be called for split parents";

        switch (interval.spillState()) {
            case NoDefinitionFound:
                assert interval.spillDefinitionPos() == -1 : "must no be set before";
                interval.setSpillDefinitionPos(defPos);
                interval.setSpillState(IntervalSpillState.NoSpillStore);
                break;

            case NoSpillStore:
                assert defPos <= interval.spillDefinitionPos() : "positions are processed in reverse order when intervals are created";
                if (defPos < interval.spillDefinitionPos() - 2) {
                    // second definition found, so no spill optimization possible for this interval
                    interval.setSpillState(IntervalSpillState.NoOptimization);
                } else {
                    // two consecutive definitions (because of two-operand LIR form)
                    assert blockOfOpWithId(defPos) == blockOfOpWithId(interval.spillDefinitionPos()) : "block must be equal";
                }
                break;

            case NoOptimization:
                // nothing to do
                break;

            default:
                throw new CiBailout("other states not allowed at this time");
        }
    }

    // called during register allocation
    void changeSpillState(Interval interval, int spillPos) {
        switch (interval.spillState()) {
            case NoSpillStore: {
                int defLoopDepth = blockOfOpWithId(interval.spillDefinitionPos()).loopDepth();
                int spillLoopDepth = blockOfOpWithId(spillPos).loopDepth();

                if (defLoopDepth < spillLoopDepth) {
                    // the loop depth of the spilling position is higher then the loop depth
                    // at the definition of the interval . move write to memory out of loop
                    // by storing at definitin of the interval
                    interval.setSpillState(IntervalSpillState.StoreAtDefinition);
                } else {
                    // the interval is currently spilled only once, so for now there is no
                    // reason to store the interval at the definition
                    interval.setSpillState(IntervalSpillState.OneSpillStore);
                }
                break;
            }

            case OneSpillStore: {
                // the interval is spilled more then once, so it is better to store it to
                // memory at the definition
                interval.setSpillState(IntervalSpillState.StoreAtDefinition);
                break;
            }

            case StoreAtDefinition:
            case StartInMemory:
            case NoOptimization:
            case NoDefinitionFound:
                // nothing to do
                break;

            default:
                throw new CiBailout("other states not allowed at this time");
        }
    }

    abstract static class IntervalClosure {
        abstract boolean apply(Interval i);
    }

    private final IntervalClosure mustStoreAtDefinition = new IntervalClosure() {
        @Override
        public boolean apply(Interval i) {
            return i.isSplitParent() && i.spillState() == IntervalSpillState.StoreAtDefinition;
        }
    };

    // called once before asignment of register numbers
    void eliminateSpillMoves() {
        // Util.traceLinearScan(3, " Eliminating unnecessary spill moves");

        // collect all intervals that must be stored after their definition.
        // the list is sorted by Interval.spillDefinitionPos
        Interval interval;
        Interval[] result = createUnhandledLists(mustStoreAtDefinition, null);
        interval = result[0];
        if (C1XOptions.DetailedAsserts) {
            checkIntervals(interval);
        }

        LIRInsertionBuffer insertionBuffer = new LIRInsertionBuffer();
        int numBlocks = blockCount();
        for (int i = 0; i < numBlocks; i++) {
            BlockBegin block = blockAt(i);
            List<LIRInstruction> instructions = block.lir().instructionsList();
            int numInst = instructions.size();
            boolean hasNew = false;

            // iterate all instructions of the block. skip the first because it is always a label
            for (int j = 1; j < numInst; j++) {
                LIRInstruction op = instructions.get(j);
                int opId = op.id;

                if (opId == -1) {
                    // remove move from register to stack if the stack slot is guaranteed to be correct.
                    // only moves that have been inserted by LinearScan can be removed.
                    assert op.code == LIROpcode.Move : "only moves can have a opId of -1";
                    assert op.result().isVariable() : "LinearScan inserts only moves to variables";

                    LIROp1 op1 = (LIROp1) op;
                    Interval curInterval = intervalAt(op1.result().variableNumber());

                    if (curInterval.assignedReg() >= numRegs && curInterval.alwaysInMemory()) {
                        // move target is a stack slot that is always correct, so eliminate instruction
                        // Util.traceLinearScan(4, "eliminating move from interval %d to %d", op1.inOpr().variableNumber(), op1.resultOpr().variableNumber());
                        instructions.set(j, null); // null-instructions are deleted by assignRegNum
                    }

                } else {
                    // insert move from register to stack just after the beginning of the interval
                    assert interval == Interval.EndMarker || interval.spillDefinitionPos() >= opId : "invalid order";
                    assert interval == Interval.EndMarker || (interval.isSplitParent() && interval.spillState() == IntervalSpillState.StoreAtDefinition) : "invalid interval";

                    while (interval != Interval.EndMarker && interval.spillDefinitionPos() == opId) {
                        if (!hasNew) {
                            // prepare insertion buffer (appended when all instructions of the block are processed)
                            insertionBuffer.init(block.lir());
                            hasNew = true;
                        }

                        CiValue fromOpr = operandForInterval(interval);
                        CiValue toOpr = canonicalSpillOpr(interval);
                        assert fromOpr.isRegister() : "from operand must be a register";
                        assert toOpr.isStackSlot() : "to operand must be a stack slot";

                        insertionBuffer.move(j, fromOpr, toOpr, null);

                        // Util.traceLinearScan(4, "inserting move after definition of interval %d to stack slot %d at opId %d", interval.registerNumber(), interval.canonicalSpillSlot() - numRegs, opId);

                        interval = interval.next;
                    }
                }
            } // end of instruction iteration

            if (hasNew) {
                block.lir().append(insertionBuffer);
            }
        } // end of block iteration

        assert interval == Interval.EndMarker : "missed an interval";
    }

    private void checkIntervals(Interval interval) {
        Interval prev = null;
        Interval temp = interval;
        while (temp != Interval.EndMarker) {
            assert temp.spillDefinitionPos() > 0 : "invalid spill definition pos";
            if (prev != null) {
                assert temp.from() >= prev.from() : "intervals not sorted";
                assert temp.spillDefinitionPos() >= prev.spillDefinitionPos() : "when intervals are sorted by from :  then they must also be sorted by spillDefinitionPos";
            }

            assert temp.canonicalSpillSlot() >= numRegs : "interval has no spill slot assigned";
            assert temp.spillDefinitionPos() >= temp.from() : "invalid order";
            assert temp.spillDefinitionPos() <= temp.from() + 2 : "only intervals defined once at their start-pos can be optimized";

            // Util.traceLinearScan(4, "interval %d (from %d to %d) must be stored at %d", temp.regNum(), temp.from(), temp.to(), temp.spillDefinitionPos());

            prev = temp;
            temp = temp.next;
        }
    }

    // * Phase 1: number all instructions in all blocks
    // Compute depth-first and linear scan block orders, and number LIRInstruction nodes for linear scan.

    void numberInstructions() {
        // Assign IDs to LIR nodes and build a mapping, lirOps, from ID to LIRInstruction node.
        int numBlocks = blockCount();
        int numInstructions = 0;
        int i;
        for (i = 0; i < numBlocks; i++) {
            numInstructions += blockAt(i).lir().instructionsList().size();
        }

        // initialize with correct length
        idToInstructionMap = new LIRInstruction[numInstructions];
        instructionToBlockMap = new BlockBegin[numInstructions];

        int id = 0;
        int index = 0;

        for (i = 0; i < numBlocks; i++) {
            BlockBegin block = blockAt(i);
            block.setFirstLirInstructionId(id);
            List<LIRInstruction> instructions = block.lir().instructionsList();

            int numInst = instructions.size();
            for (int j = 0; j < numInst; j++) {
                LIRInstruction op = instructions.get(j);
                op.id = id;

                idToInstructionMap[index] = op;
                instructionToBlockMap[index] = block;
                assert instructionForId(id) == op : "must match";

                index++;
                id += 2; // numbering of lirOps by two
            }
            block.setLastLirInstructionId(id - 2);
        }
        assert index == numInstructions : "must match";
        assert index * 2 == id : "must match";

        hasCall = new BitMap(numInstructions);
    }

    // * Phase 2: compute local live sets separately for each block
    // (sets liveGen and liveKill for each block)

    void setLiveGenKill(Value value, LIRInstruction op, BitMap liveGen, BitMap liveKill) {
        CiValue opr = value.operand();
        if (opr.isVariable()) {
            int reg = opr.variableNumber();
            if (!liveKill.get(reg)) {
                liveGen.set(reg);
                // Util.traceLinearScan(4, "  Setting liveGen for value %c%d, LIR opId %d, register number %d", value.kind.typeChar, value.id, op.id, reg);
            }
        } else {
            assert opr.isConstant() || opr.isIllegal() : "invalid operand for deoptimization value";
        }
    }

    void computeLocalLiveSets() {
        int numBlocks = blockCount();
        int liveSize = liveSetSize();
        int localNumCalls = 0;

        BitMap2D localIntervalInLoop = new BitMap2D(numVariables, numLoops());

        // iterate all blocks
        for (int i = 0; i < numBlocks; i++) {
            BlockBegin block = blockAt(i);
            BitMap liveGen = new BitMap(liveSize);
            BitMap liveKill = new BitMap(liveSize);

            if (block.isExceptionEntry()) {
                // Phi functions at the begin of an exception handler are
                // implicitly defined (= killed) at the beginning of the block.
                for (Phi phi : block.allLivePhis()) {
                    liveKill.set(phi.operand().variableNumber());
                }
            }

            List<LIRInstruction> instructions = block.lir().instructionsList();
            int numInst = instructions.size();

            // iterate all instructions of the block. skip the first because it is always a label
            assert !instructions.get(0).hasOperands() : "first operation must always be a label";
            for (int j = 1; j < numInst; j++) {
                LIRInstruction op = instructions.get(j);

                if (op.hasCall()) {
                    hasCall.set(op.id >> 1);
                    localNumCalls++;
                }

                // iterate input operands of instruction
                int k;
                int n;
                int reg;
                n = op.oprCount(LIRInstruction.OperandMode.InputMode);
                for (k = 0; k < n; k++) {
                    CiValue opr = op.oprAt(LIRInstruction.OperandMode.InputMode, k);

                    if (opr.isVariable()) {
                        assert regNum(opr) == opr.variableNumber() && !isValidRegNum(regNumHi(opr)) : "invalid optimization below";
                        reg = opr.variableNumber();
                        if (!liveKill.get(reg)) {
                            liveGen.set(reg);
                            // Util.traceLinearScan(4, "  Setting liveGen for register %d at instruction %d", reg, op.id());
                        }
                        if (block.loopIndex() >= 0) {
                            localIntervalInLoop.setBit(reg, block.loopIndex());
                        }
                    }

                    if (C1XOptions.DetailedAsserts) {
                        assert opr.isVariableOrRegister() : "visitor should only return register operands";
                        verifyInput(block, liveKill, opr);
                    }
                }

                // Add uses of live locals from interpreter's point of view for proper debug information generation
                n = op.infoCount();
                for (k = 0; k < n; k++) {
                    LIRDebugInfo info = op.infoAt(k);
                    FrameState state = info.state;
                    for (Value value : state.allLiveStateValues()) {
                        setLiveGenKill(value, op, liveGen, liveKill);
                    }
                }

                // iterate temp operands of instruction
                n = op.oprCount(LIRInstruction.OperandMode.TempMode);
                for (k = 0; k < n; k++) {
                    CiValue opr = op.oprAt(LIRInstruction.OperandMode.TempMode, k);

                    if (opr.isVariable()) {
                        assert regNum(opr) == opr.variableNumber() && !isValidRegNum(regNumHi(opr)) : "invalid optimization below";
                        reg = opr.variableNumber();
                        liveKill.set(reg);
                        if (block.loopIndex() >= 0) {
                            localIntervalInLoop.setBit(reg, block.loopIndex());
                        }
                    }

                    if (C1XOptions.DetailedAsserts) {
                        assert opr.isVariableOrRegister() : "visitor should only return register operands";
                        verifyTemp(liveKill, opr);
                    }
                }

                // iterate output operands of instruction
                n = op.oprCount(LIRInstruction.OperandMode.OutputMode);
                for (k = 0; k < n; k++) {
                    CiValue opr = op.oprAt(LIRInstruction.OperandMode.OutputMode, k);

                    if (opr.isVariable()) {
                        assert regNum(opr) == opr.variableNumber() && !isValidRegNum(regNumHi(opr)) : "invalid optimization below";
                        reg = opr.variableNumber();
                        liveKill.set(reg);
                        if (block.loopIndex() >= 0) {
                            localIntervalInLoop.setBit(reg, block.loopIndex());
                        }
                    }

                    if (C1XOptions.DetailedAsserts) {
                        assert opr.isVariableOrRegister() : "visitor should only return register operands";
                        // fixed intervals are never live at block boundaries, so
                        // they need not be processed in live sets
                        // process them only in debug mode so that this can be checked
                        verifyTemp(liveKill, opr);
                    }
                }
            } // end of instruction iteration

            LIRBlock lirBlock = block.lirBlock();
            lirBlock.liveGen = liveGen;
            lirBlock.liveKill = liveKill;
            lirBlock.liveIn = new BitMap(liveSize);
            lirBlock.liveOut = new BitMap(liveSize);

            if (C1XOptions.TraceLinearScanLevel >= 4) {
                traceLiveness(block);
            }
        } // end of block iteration

        numCalls = localNumCalls;
        intervalInLoop = localIntervalInLoop;
    }

    private void traceLiveness(BlockBegin block) {
        Util.traceLinearScan(4, "liveGen  B%d ", block.blockID);
        TTY.println(block.lirBlock.liveGen.toString());
        Util.traceLinearScan(4, "liveKill B%d ", block.blockID);
        TTY.println(block.lirBlock.liveKill.toString());
    }

    private void verifyTemp(BitMap liveKill, CiValue opr) {
        int reg;
        // fixed intervals are never live at block boundaries, so
        // they need not be processed in live sets
        // process them only in debug mode so that this can be checked
        if (!opr.isVariable()) {
            reg = regNum(opr);
            if (isProcessedRegNum(reg)) {
                liveKill.set(regNum(opr));
            }
            reg = regNumHi(opr);
            if (isValidRegNum(reg) && isProcessedRegNum(reg)) {
                liveKill.set(reg);
            }
        }
    }

    private void verifyInput(BlockBegin block, BitMap liveKill, CiValue opr) {
        int reg;
        // fixed intervals are never live at block boundaries, so
        // they need not be processed in live sets.
        // this is checked by these assertions to be sure about it.
        // the entry block may have incoming
        // values in registers, which is ok.
        if (!opr.isVariable() && block != ir.startBlock) {
            reg = regNum(opr);
            if (isProcessedRegNum(reg)) {
                assert liveKill.get(reg) : "using fixed register that is not defined in this block";
            }
            reg = regNumHi(opr);
            if (isValidRegNum(reg) && isProcessedRegNum(reg)) {
                assert liveKill.get(reg) : "using fixed register that is not defined in this block";
            }
        }
    }

    // * Phase 3: perform a backward dataflow analysis to compute global live sets
    // (sets liveIn and liveOut for each block)

    void computeGlobalLiveSets() {
        int numBlocks = blockCount();
        boolean changeOccurred;
        boolean changeOccurredInBlock;
        int iterationCount = 0;
        BitMap liveOut = new BitMap(liveSetSize()); // scratch set for calculations

        // Perform a backward dataflow analysis to compute liveOut and liveIn for each block.
        // The loop is executed until a fixpoint is reached (no changes in an iteration)
        // Exception handlers must be processed because not all live values are
        // present in the state array, e.g. because of global value numbering
        do {
            changeOccurred = false;

            // iterate all blocks in reverse order
            for (int i = numBlocks - 1; i >= 0; i--) {
                BlockBegin block = blockAt(i);
                LIRBlock lirBlock = block.lirBlock();

                changeOccurredInBlock = false;

                // liveOut(block) is the union of liveIn(sux), for successors sux of block
                int n = block.numberOfSux();
                int e = block.numberOfExceptionHandlers();
                if (n + e > 0) {
                    // block has successors
                    if (n > 0) {
                        liveOut.setFrom(block.suxAt(0).lirBlock.liveIn);
                        for (int j = 1; j < n; j++) {
                            liveOut.setUnion(block.suxAt(j).lirBlock.liveIn);
                        }
                    } else {
                        liveOut.clearAll();
                    }
                    for (int j = 0; j < e; j++) {
                        liveOut.setUnion(block.exceptionHandlerAt(j).lirBlock.liveIn);
                    }

                    if (!lirBlock.liveOut.isSame(liveOut)) {
                        // A change occurred. Swap the old and new live out sets to avoid copying.
                        BitMap temp = lirBlock.liveOut;
                        lirBlock.liveOut = liveOut;
                        liveOut = temp;

                        changeOccurred = true;
                        changeOccurredInBlock = true;
                    }
                }

                if (iterationCount == 0 || changeOccurredInBlock) {
                    // liveIn(block) is the union of liveGen(block) with (liveOut(block) & !liveKill(block))
                    // note: liveIn has to be computed only in first iteration or if liveOut has changed!
                    BitMap liveIn = lirBlock.liveIn;
                    liveIn.setFrom(lirBlock.liveOut);
                    liveIn.setDifference(lirBlock.liveKill);
                    liveIn.setUnion(lirBlock.liveGen);
                }

                if (C1XOptions.TraceLinearScanLevel >= 4) {
                    traceLiveness(changeOccurredInBlock, iterationCount, block);
                }
            }
            iterationCount++;

            if (changeOccurred && iterationCount > 50) {
                throw new CiBailout("too many iterations in computeGlobalLiveSets");
            }
        } while (changeOccurred);

        if (C1XOptions.DetailedAsserts) {
            verifyLiveness(numBlocks);
        }

        // check that the liveIn set of the first block is empty
        BitMap liveInArgs = new BitMap(ir.startBlock.lirBlock.liveIn.size());
        if (!ir.startBlock.lirBlock.liveIn.isSame(liveInArgs)) {
            if (C1XOptions.DetailedAsserts) {
                reportFailure(numBlocks);
            }

            // bailout of if this occurs in product mode.
            throw new CiBailout("liveIn set of first block must be empty");
        }
    }

    private void reportFailure(int numBlocks) {
        TTY.println("Error: liveIn set of first block must be empty (when this fails, variables are used before they are defined)");
        TTY.print("affected registers:");
        TTY.println(ir.startBlock.lirBlock.liveIn.toString());

        // print some additional information to simplify debugging
        for (int i = 0; i < ir.startBlock.lirBlock.liveIn.size(); i++) {
            if (ir.startBlock.lirBlock.liveIn.get(i)) {
                Value instr = gen.instructionForVariable(i);
                TTY.println(" var %d (HIR instruction %s)", i, instr == null ? " " : instr.toString());

                for (int j = 0; j < numBlocks; j++) {
                    BlockBegin block = blockAt(j);
                    if (block.lirBlock.liveGen.get(i)) {
                        TTY.println("  used in block B%d", block.blockID);
                    }
                    if (block.lirBlock.liveKill.get(i)) {
                        TTY.println("  defined in block B%d", block.blockID);
                    }
                }
            }
        }
    }

    private void verifyLiveness(int numBlocks) {
        // check that fixed intervals are not live at block boundaries
        // (live set must be empty at fixed intervals)
        for (int i = 0; i < numBlocks; i++) {
            BlockBegin block = blockAt(i);
            for (int j = 0; j < CiRegister.LowestVirtualRegisterNumber; j++) {
                assert !block.lirBlock.liveIn.get(j) : "liveIn  set of fixed register must be empty";
                assert !block.lirBlock.liveOut.get(j) : "liveOut set of fixed register must be empty";
                assert !block.lirBlock.liveGen.get(j) : "liveGen set of fixed register must be empty";
            }
        }
    }

    private void traceLiveness(boolean changeOccurredInBlock, int iterationCount, BlockBegin block) {
        char c = ' ';
        if (iterationCount == 0 || changeOccurredInBlock) {
            c = '*';
        }
        TTY.print("(%d) liveIn%c  B%d ", iterationCount, c, block.blockID);
        TTY.println(block.lirBlock.liveIn.toString());
        TTY.print("(%d) liveOut%c B%d ", iterationCount, c, block.blockID);
        TTY.println(block.lirBlock.liveOut.toString());
    }

    // * Phase 4: build intervals
    // (fills the list intervals)

    void addUse(Value value, int from, int to, IntervalUseKind useKind) {
        assert !value.isIllegal() : "if this value is used by the interpreter it shouldn't be of indeterminate type";
        CiValue opr = value.operand();
        Constant con = null;
        if (value instanceof Constant) {
            con = (Constant) value;
        }

        if ((con == null || con.isLive()) && opr.isVariableOrRegister()) {
            assert regNum(opr) == opr.variableNumber() && !isValidRegNum(regNumHi(opr)) : "invalid optimization below";
            CiKind registerKind = registerKind(opr);
            addUse((CiLocation) opr, from, to, useKind, registerKind);
        }
    }

    void addDef(CiLocation opr, int defPos, IntervalUseKind useKind, CiKind registerKind) {
        if (C1XOptions.TraceLinearScanLevel >= 2) {
            TTY.println(" def %s defPos %d (%s)", opr, defPos, useKind.name());
        }
        assert opr.isVariableOrRegister() : "should not be called otherwise";

        if (opr.isVariable()) {
            assert regNum(opr) == opr.variableNumber() && !isValidRegNum(regNumHi(opr)) : "invalid optimization below";
            addDef(opr.variableNumber(), defPos, useKind, registerKind);
        } else {
            int reg = regNum(opr);
            if (isProcessedRegNum(reg)) {
                addDef(reg, defPos, useKind, registerKind);
            }
            reg = regNumHi(opr);
            if (isValidRegNum(reg) && isProcessedRegNum(reg)) {
                addDef(reg, defPos, useKind, registerKind);
            }
        }
    }

    private CiKind registerKind(CiValue operand) {
        assert operand.isVariableOrRegister();

        if (operand.kind == CiKind.Boolean || operand.kind == CiKind.Char || operand.kind == CiKind.Byte) {
            return CiKind.Int;
        }

        return operand.kind;
    }

    void addUse(CiLocation opr, int from, int to, IntervalUseKind useKind, CiKind registerKind) {
        if (C1XOptions.TraceLinearScanLevel >= 2) {
            TTY.print(" use %s from %d to %d (%s)", opr, from, to, useKind.name());
        }
        assert opr.isVariableOrRegister() : "should not be called otherwise";

        if (opr.isVariable()) {
            assert regNum(opr) == opr.variableNumber() && !isValidRegNum(regNumHi(opr)) : "invalid optimization below";
            addUse(opr.variableNumber(), from, to, useKind, registerKind);
        } else {
            int reg = regNum(opr);
            if (isProcessedRegNum(reg)) {
                addUse(reg, from, to, useKind, registerKind);
            }
            reg = regNumHi(opr);
            if (isValidRegNum(reg) && isProcessedRegNum(reg)) {
                addUse(reg, from, to, useKind, registerKind);
            }
        }
    }

    void addTemp(CiLocation opr, int tempPos, IntervalUseKind useKind, CiKind registerKind) {
        if (C1XOptions.TraceLinearScanLevel >= 2) {
            TTY.println(" temp %s tempPos %d (%s)", opr, tempPos, useKind.name());
        }
        assert opr.isVariableOrRegister() : "should not be called otherwise";

        if (opr.isVariable()) {
            assert regNum(opr) == opr.variableNumber() && !isValidRegNum(regNumHi(opr)) : "invalid optimization below";
            addTemp(opr.variableNumber(), tempPos, useKind, registerKind);

        } else {
            int reg = regNum(opr);
            if (isProcessedRegNum(reg)) {
                addTemp(reg, tempPos, useKind, registerKind);
            }
            reg = regNumHi(opr);
            if (isValidRegNum(reg) && isProcessedRegNum(reg)) {
                addTemp(reg, tempPos, useKind, registerKind);
            }
        }
    }

    boolean isProcessedRegNum(int reg) {
        return reg > CiRegister.LowestVirtualRegisterNumber
               || reg >= allocatableRegisters.registerMapping.length
               || (reg >= 0 && reg < allocatableRegisters.registerMapping.length
                   && allocatableRegisters.registerMapping[reg] != null
                   && allocatableRegisters.allocatableRegister[reg]);
    }

    void addDef(int regNum, int defPos, IntervalUseKind useKind, CiKind kind) {
        Interval interval = intervalAt(regNum);
        if (interval != null) {
            assert interval.registerNumber() == regNum : "wrong interval";

            if (kind != CiKind.Illegal) {
                interval.setKind(kind);
            }

            Range r = interval.first();
            if (r.from <= defPos) {
                // Update the starting point (when a range is first created for a use, its
                // start is the beginning of the current block until a def is encountered.)
                r.from = defPos;
                interval.addUsePos(defPos, useKind);

            } else {
                // Dead value - make vacuous interval
                // also add useKind for dead intervals
                interval.addRange(defPos, defPos + 1);
                interval.addUsePos(defPos, useKind);
                // Util.traceLinearScan(2, "Warning: def of reg %d at %d occurs without use", regNum, defPos);
            }

        } else {
            // Dead value - make vacuous interval
            // also add useKind for dead intervals
            interval = createInterval(regNum);
            if (kind != CiKind.Illegal) {
                interval.setKind(kind);
            }

            interval.addRange(defPos, defPos + 1);
            interval.addUsePos(defPos, useKind);
            // Util.traceLinearScan(2, "Warning: dead value %d at %d in live intervals", regNum, defPos);
        }

        changeSpillDefinitionPos(interval, defPos);
        if (useKind == IntervalUseKind.NoUse && interval.spillState().ordinal() <= IntervalSpillState.StartInMemory.ordinal()) {
            // detection of method-parameters and roundfp-results
            // TODO: move this directly to position where use-kind is computed
            interval.setSpillState(IntervalSpillState.StartInMemory);
        }
    }

    void addUse(int regNum, int from, int to, IntervalUseKind useKind, CiKind kind) {
        Interval interval = intervalAt(regNum);
        if (interval == null) {
            interval = createInterval(regNum);
        }
        assert interval.registerNumber() == regNum : "wrong interval";

        if (kind != CiKind.Illegal) {
            interval.setKind(kind);
        }

        interval.addRange(from, to);
        interval.addUsePos(to, useKind);
    }

    void addTemp(int regNum, int tempPos, IntervalUseKind useKind, CiKind kind) {
        Interval interval = intervalAt(regNum);
        if (interval == null) {
            interval = createInterval(regNum);
        }
        assert interval.registerNumber() == regNum : "wrong interval";

        if (kind != CiKind.Illegal) {
            interval.setKind(kind);
        }

        interval.addRange(tempPos, tempPos + 1);
        interval.addUsePos(tempPos, useKind);
    }

    // the results of this functions are used for optimizing spilling and reloading
    // if the functions return shouldHaveRegister and the interval is spilled,
    // it is not reloaded to a register.
    IntervalUseKind useKindOfOutputOperand(LIRInstruction op, CiValue opr) {
        if (op.code == LIROpcode.Move) {
            LIROp1 move = (LIROp1) op;
            CiValue res = move.result();
            boolean resultInMemory = res.isVariable() && gen.isVarFlagSet(res.variableNumber(), LIRGenerator.VariableFlag.MustStartInMemory);

            if (resultInMemory) {
                // Begin of an interval with mustStartInMemory set.
                // This interval will always get a stack slot first, so return noUse.
                return IntervalUseKind.NoUse;

            } else if (move.operand().isStackSlot()) {
                // method argument (condition must be equal to handleMethodArguments)
                return IntervalUseKind.NoUse;

            } else if (move.operand().isVariableOrRegister() && move.result().isVariableOrRegister()) {
                // Move from register to register
                if (blockOfOpWithId(op.id).checkBlockFlag(BlockBegin.BlockFlag.OsrEntry)) {
                    // special handling of phi-function moves inside osr-entry blocks
                    // input operand must have a register instead of output operand (leads to better register
                    // allocation)
                    return IntervalUseKind.ShouldHaveRegister;
                }
            }
        }

        if (opr.isVariable() && gen.isVarFlagSet(opr.variableNumber(), LIRGenerator.VariableFlag.MustStartInMemory)) {
            // result is a stack-slot, so prevent immediate reloading
            return IntervalUseKind.NoUse;
        }

        // all other operands require a register
        return IntervalUseKind.MustHaveRegister;
    }

    IntervalUseKind useKindOfInputOperand(LIRInstruction op, CiValue opr) {
        if (op.code == LIROpcode.Move) {
            LIROp1 move = (LIROp1) op;
            CiValue res = move.result();
            boolean resultInMemory = res.isVariable() && gen.isVarFlagSet(res.variableNumber(), LIRGenerator.VariableFlag.MustStartInMemory);

            if (resultInMemory) {
                // Move to an interval with mustStartInMemory set.
                // To avoid moves from stack to stack (not allowed) force the input operand to a register
                return IntervalUseKind.MustHaveRegister;

            } else if (move.operand().isVariableOrRegister() && move.result().isVariableOrRegister()) {
                // Move from register to register
                if (blockOfOpWithId(op.id).checkBlockFlag(BlockBegin.BlockFlag.OsrEntry)) {
                    // special handling of phi-function moves inside osr-entry blocks
                    // input operand must have a register instead of output operand (leads to better register
                    // allocation)
                    return IntervalUseKind.MustHaveRegister;
                }

                // The input operand is not forced to a register (moves from stack to register are allowed),
                // but it is faster if the input operand is in a register
                return IntervalUseKind.ShouldHaveRegister;
            }
        }

        if (compilation.target.arch.isX86()) {
            if (op.code == LIROpcode.Cmove) {
                // conditional moves can handle stack operands
                assert op.result().isVariableOrRegister() : "result must always be in a register";
                return IntervalUseKind.ShouldHaveRegister;
            }

            // optimizations for second input operand of arithmehtic operations on Intel
            // this operand is allowed to be on the stack in some cases
            CiKind oprType = registerKind(opr);
            if (oprType == CiKind.Float || oprType == CiKind.Double) {
                if ((C1XOptions.SSEVersion == 1 && oprType == CiKind.Float) || C1XOptions.SSEVersion >= 2) {
                    // SSE float instruction (CiKind.Double only supported with SSE2)
                    switch (op.code) {
                        case Cmp:
                        case Add:
                        case Sub:
                        case Mul:
                        case Div: {
                            LIROp2 op2 = (LIROp2) op;
                            if (op2.opr1() != op2.opr2() && op2.opr2() == opr) {
                                assert (op2.result().isVariableOrRegister() || op.code == LIROpcode.Cmp) && op2.opr1().isVariableOrRegister() : "cannot mark second operand as stack if others are not in register";
                                return IntervalUseKind.ShouldHaveRegister;
                            }
                        }
                    }
                } else {
                    // FPU stack float instruction
                    switch (op.code) {
                        case Add:
                        case Sub:
                        case Mul:
                        case Div: {
                            LIROp2 op2 = (LIROp2) op;
                            if (op2.opr1() != op2.opr2() && op2.opr2() == opr) {
                                assert (op2.result().isVariableOrRegister() || op.code == LIROpcode.Cmp) && op2.opr1().isVariableOrRegister() : "cannot mark second operand as stack if others are not in register";
                                return IntervalUseKind.ShouldHaveRegister;
                            }
                        }
                    }
                }

            } else if (oprType != CiKind.Long) {
                // integer instruction (note: long operands must always be in register)
                switch (op.code) {
                    case Cmp:
                    case Add:
                    case Sub:
                    case LogicAnd:
                    case LogicOr:
                    case LogicXor: {
                        LIROp2 op2 = (LIROp2) op;
                        if (op2.opr1() != op2.opr2() && op2.opr2() == opr) {
                            assert (op2.result().isVariableOrRegister() || op.code == LIROpcode.Cmp) && op2.opr1().isVariableOrRegister() : "cannot mark second operand as stack if others are not in register";
                            return IntervalUseKind.ShouldHaveRegister;
                        }
                    }
                }
            }
        } // X86

        // all other operands require a register
        return IntervalUseKind.MustHaveRegister;
    }

    void handleMethodArguments(LIRInstruction op) {
        // special handling for method arguments (moves from stack to variable):
        // the interval gets no register assigned, but the stack slot.
        // it is split before the first use by the register allocator.

        if (op.code == LIROpcode.Move) {
            LIROp1 move = (LIROp1) op;

            if (move.operand().isStackSlot()) {
                CiStackSlot o = (CiStackSlot) move.operand();
                if (C1XOptions.DetailedAsserts) {
                    int argSlots = compilation.method.signatureType().argumentSlots(!compilation.method.isStatic());
                    assert o.index >= 0 && o.index < argSlots;
                    assert move.id > 0 : "invalid id";
                    assert blockOfOpWithId(move.id).numberOfPreds() == 0 : "move from stack must be in first block";
                    assert move.result().isVariable() : "result of move must be a variable";

                    // Util.traceLinearScan(4, "found move from stack slot %d to var %d", o.isSingleStack() ? o.singleStackIx() : o.doubleStackIx(), regNum(move.resultOpr()));
                }

                Interval interval = intervalAt(regNum(move.result()));

                int stackSlot = numRegs + (o.index);
                interval.setCanonicalSpillSlot(stackSlot);
                interval.assignReg(stackSlot);
            }
        }
    }

//    void handleDoublewordMoves(LIRInstruction op) {
//        // special handling for doubleword move from memory to register:
//        // in this case the registers of the input Pointer and the result
//        // registers must not overlap . add a temp range for the input registers
//        if (op.code == LIROpcode.Move) {
//            LIROp1 move = (LIROp1) op;
//
//            CiValue inOpr = move.operand();
//
//            if (move.result().isDoubleRegister() && CiValue.isLocation(inOpr)) {
//                if (inOpr instanceof CiAddress) {
//                    final CiAddress pointer = (CiAddress) inOpr;
//                    CiLocation base = pointer.base;
//                    if (CiValue.isLegal(base)) {
//                        addTemp(base, op.id, IntervalUseKind.NoUse, registerKind(base));
//                    }
//                    CiLocation index = pointer.index;
//                    if (CiValue.isLegal(index)) {
//                        addTemp(index, op.id, IntervalUseKind.NoUse, registerKind(index));
//                    }
//                }
//            }
//        }
//    }

    void addRegisterHints(LIRInstruction op) {
        switch (op.code) {
            case Move: // fall through
            case Convert: {
                LIROp1 move = (LIROp1) op;

                CiValue moveFrom = move.operand();
                CiValue moveTo = move.result();

                if (moveTo.isVariableOrRegister() && moveFrom.isVariableOrRegister()) {
                    Interval from = intervalAt(regNum(moveFrom));
                    Interval to = intervalAt(regNum(moveTo));
                    if (from != null && to != null) {
                        to.setRegisterHint(from);
                        // Util.traceLinearScan(4, "operation at opId %d: added hint from interval %d to %d", move.id(), from.regNum(), to.regNum());
                    }
                }
                break;
            }
            case Cmove: {
                LIROp2 cmove = (LIROp2) op;

                CiValue moveFrom = cmove.opr1();
                CiValue moveTo = cmove.result();

                if (moveTo.isVariableOrRegister() && moveFrom.isVariableOrRegister()) {
                    Interval from = intervalAt(regNum(moveFrom));
                    Interval to = intervalAt(regNum(moveTo));
                    if (from != null && to != null) {
                        to.setRegisterHint(from);
                        // Util.traceLinearScan(4, "operation at opId %d: added hint from interval %d to %d", cmove.id(), from.regNum(), to.regNum());
                    }
                }
                break;
            }
        }
    }

    void buildIntervals() {
        intervalsMap = new Interval[numVariables + 32];
        intervalsMapSize = numVariables;

        // create a list with all caller-save registers (cpu, fpu, xmm)
        CiRegister[] callerSaveRegs = compilation.target.allocatableRegs.callerSaveAllocatableRegisters;

        // iterate all blocks in reverse order
        for (int i = blockCount() - 1; i >= 0; i--) {
            BlockBegin block = blockAt(i);
            List<LIRInstruction> instructions = block.lir().instructionsList();
            int blockFrom = block.firstLirInstructionId();
            int blockTo = block.lastLirInstructionId();

            assert blockFrom == instructions.get(0).id : "must be";
            assert blockTo == instructions.get(instructions.size() - 1).id : "must be";

            // Update intervals for registers live at the end of this block;
            BitMap live = block.lirBlock.liveOut;
            int size = live.size();
            for (int number = live.getNextOneOffset(0, size); number < size; number = live.getNextOneOffset(number + 1, size)) {
                assert live.get(number) : "should not stop here otherwise";
                assert number >= CiRegister.LowestVirtualRegisterNumber : "fixed intervals must not be live on block bounds";
                // Util.traceLinearScan(2, "live in %d to %d", number, blockTo + 2);

                addUse(number, blockFrom, blockTo + 2, IntervalUseKind.NoUse, CiKind.Illegal);

                // add special use positions for loop-end blocks when the
                // interval is used anywhere inside this loop. It's possible
                // that the block was part of a non-natural loop, so it might
                // have an invalid loop index.
                if (block.checkBlockFlag(BlockBegin.BlockFlag.LinearScanLoopEnd) && block.loopIndex() != -1 && isIntervalInLoop(number, block.loopIndex())) {
                    intervalAt(number).addUsePos(blockTo + 1, IntervalUseKind.LoopEndMarker);
                }
            }

            // iterate all instructions of the block in reverse order.
            // skip the first instruction because it is always a label
            // definitions of intervals are processed before uses
            assert !instructions.get(0).hasOperands() : "first operation must always be a label";
            for (int j = instructions.size() - 1; j >= 1; j--) {
                LIRInstruction op = instructions.get(j);
                int opId = op.id;

                // visit operation to collect all operands

                // add a temp range for each register if operation destroys caller-save registers
                if (op.hasCall()) {
                    for (CiRegister r : callerSaveRegs) {
                        addTemp(r.number, opId, IntervalUseKind.NoUse, CiKind.Illegal);
                    }
                    // Util.traceLinearScan(4, "operation destroys all caller-save registers");
                }

                // Add any platform dependent temps
                pdAddTemps(op);

                // visit definitions (output and temp operands)
                int k;
                int n;
                n = op.oprCount(LIRInstruction.OperandMode.OutputMode);
                for (k = 0; k < n; k++) {
                    CiLocation opr = op.oprAt(LIRInstruction.OperandMode.OutputMode, k);
                    assert opr.isVariableOrRegister() : "visitor should only return register operands";
                    addDef(opr, opId, useKindOfOutputOperand(op, opr), registerKind(opr));
                }

                n = op.oprCount(LIRInstruction.OperandMode.TempMode);
                for (k = 0; k < n; k++) {
                    CiLocation opr = op.oprAt(LIRInstruction.OperandMode.TempMode, k);
                    assert opr.isVariableOrRegister() : "visitor should only return register operands";
                    addTemp(opr, opId, IntervalUseKind.MustHaveRegister, registerKind(opr));
                }

                // visit uses (input operands)
                n = op.oprCount(LIRInstruction.OperandMode.InputMode);
                for (k = 0; k < n; k++) {
                    CiLocation opr = op.oprAt(LIRInstruction.OperandMode.InputMode, k);
                    assert opr.isVariableOrRegister() : "visitor should only return register operands";
                    addUse(opr, blockFrom, opId, useKindOfInputOperand(op, opr), registerKind(opr));
                }

                // Add uses of live locals from interpreter's point of view for proper
                // debug information generation
                // Treat these operands as temp values (if the live range is extended
                // to a call site, the value would be in a register at the call otherwise)
                n = op.infoCount();
                for (k = 0; k < n; k++) {
                    LIRDebugInfo info = op.infoAt(k);
                    FrameState state = info.state;
                    for (Value value : state.allLiveStateValues()) {
                        addUse(value, blockFrom, opId + 1, IntervalUseKind.NoUse);
                    }
                }

                // special steps for some instructions (especially moves)
                handleMethodArguments(op);
//                handleDoublewordMoves(op);
                addRegisterHints(op);

            } // end of instruction iteration

        } // end of block iteration

        // add the range [0, 1] to all fixed intervals.
        // the register allocator need not handle unhandled fixed intervals
        for (int n = 0; n < numRegs; n++) {
            Interval interval = intervalAt(n);
            if (interval != null) {
                interval.addRange(0, 1);
            }
        }
    }

    // * Phase 5: actual register allocation

    private void pdAddTemps(LIRInstruction op) {
        // TODO Platform dependent!
        assert compilation.target.arch.isX86();

        switch (op.code) {
            case Tan:
            case Sin:
            case Cos: {
                // The slow path for these functions may need to save and
                // restore all live registers but we don't want to save and
                // restore everything all the time, so mark the xmms as being
                // killed. If the slow path were explicit or we could propagate
                // live register masks down to the assembly we could do better
                // but we don't have any easy way to do that right now. We
                // could also consider not killing all xmm registers if we
                // assume that slow paths are uncommon but it's not clear that
                // would be a good idea.
                if (C1XOptions.TraceLinearScanLevel >= 2) {
                    TTY.println("killing XMMs for trig");
                }
                int opId = op.id;

                for (CiRegister r : compilation.target.allocatableRegs.callerSaveRegisters) {
                    if (r.isXmm()) {
                        addTemp(r.number, opId, IntervalUseKind.NoUse, CiKind.Illegal);
                    }
                }
                break;
            }
        }

    }

    boolean isSorted(Interval[] intervals) {
        int from = -1;
        int i;
        int j;
        for (i = 0; i < intervals.length; i++) {
            Interval it = intervals[i];
            if (it != null) {
                assert from <= it.from();
                from = it.from();
            }
        }

        // check in both directions if sorted list and unsorted list contain same intervals
        for (i = 0; i < intervalCount(); i++) {
            if (intervalAt(i) != null) {
                int numFound = 0;
                for (j = 0; j < intervals.length; j++) {
                    if (intervalAt(i) == intervals[j]) {
                        numFound++;
                    }
                }
                assert numFound == 1 : "lists do not contain same intervals";
            }
        }
        for (j = 0; j < intervals.length; j++) {
            int numFound = 0;
            for (i = 0; i < intervalCount(); i++) {
                if (intervalAt(i) == intervals[j]) {
                    numFound++;
                }
            }
            assert numFound == 1 : "lists do not contain same intervals";
        }

        return true;
    }

    Interval addToList(Interval first, Interval prev, Interval interval) {
        Interval newFirst = first;
        if (prev != null) {
            prev.next = interval;
        } else {
            newFirst = interval;
        }
        return newFirst;
    }

    Interval[] createUnhandledLists(IntervalClosure isList1, IntervalClosure isList2) {
        assert isSorted(sortedIntervals) : "interval list is not sorted";

        Interval list1 = Interval.EndMarker;
        Interval list2 = Interval.EndMarker;

        Interval list1Prev = null;
        Interval list2Prev = null;
        Interval v;

        int n = sortedIntervals.length;
        for (int i = 0; i < n; i++) {
            v = sortedIntervals[i];
            if (v == null) {
                continue;
            }

            if (isList1.apply(v)) {
                list1 = addToList(list1, list1Prev, v);
                list1Prev = v;
            } else if (isList2 == null || isList2.apply(v)) {
                list2 = addToList(list2, list2Prev, v);
                list2Prev = v;
            }
        }

        if (list1Prev != null) {
            list1Prev.next = Interval.EndMarker;
        }
        if (list2Prev != null) {
            list2Prev.next = Interval.EndMarker;
        }

        assert list1Prev == null || list1Prev.next == Interval.EndMarker : "linear list ends not with sentinel";
        assert list2Prev == null || list2Prev.next == Interval.EndMarker : "linear list ends not with sentinel";

        return new Interval[] {list1, list2};
    }

    void sortIntervalsBeforeAllocation() {
        Interval[] unsortedList = intervalsMap;
        int unsortedLen = unsortedList.length;
        int sortedLen = 0;
        int unsortedIdx;
        int sortedIdx = 0;
        int sortedFromMax = -1;

        // calc number of items for sorted list (sorted list must not contain null values)
        for (unsortedIdx = 0; unsortedIdx < unsortedLen; unsortedIdx++) {
            if (unsortedList[unsortedIdx] != null) {
                sortedLen++;
            }
        }
        Interval[] sortedList = new Interval[sortedLen];

        // special sorting algorithm: the original interval-list is almost sorted,
        // only some intervals are swapped. So this is much faster than a complete QuickSort
        for (unsortedIdx = 0; unsortedIdx < unsortedLen; unsortedIdx++) {
            Interval curInterval = unsortedList[unsortedIdx];

            if (curInterval != null) {
                int curFrom = curInterval.from();

                if (sortedFromMax <= curFrom) {
                    sortedList[sortedIdx++] = curInterval;
                    sortedFromMax = curInterval.from();
                } else {
                    // the asumption that the intervals are already sorted failed,
                    // so this interval must be sorted in manually
                    int j;
                    for (j = sortedIdx - 1; j >= 0 && curFrom < sortedList[j].from(); j--) {
                        sortedList[j + 1] = sortedList[j];
                    }
                    sortedList[j + 1] = curInterval;
                    sortedIdx++;
                }
            }
        }
        sortedIntervals = sortedList;
    }

    void sortIntervalsAfterAllocation() {
        Interval[] oldList = sortedIntervals;
        List<Interval> newList = newIntervalsFromAllocation;
        int oldLen = oldList.length;
        int newLen = newList.size();

        if (newLen == 0) {
            // no intervals have been added during allocation, so sorted list is already up to date
            return;
        }

        // conventional sort-algorithm for new intervals
        Collections.sort(newList, intervalCmp);

        // merge old and new list (both already sorted) into one combined list
        Interval[] combinedList = new Interval[oldLen + newLen];
        int oldIdx = 0;
        int newIdx = 0;

        while (oldIdx + newIdx < oldLen + newLen) {
            if (newIdx >= newLen || (oldIdx < oldLen && oldList[oldIdx].from() <= newList.get(newIdx).from())) {
                combinedList[oldIdx + newIdx] = oldList[oldIdx];
                oldIdx++;
            } else {
                combinedList[oldIdx + newIdx] = newList.get(newIdx);
                newIdx++;
            }
        }

        sortedIntervals = combinedList;
    }

    private final Comparator<Interval> intervalCmp = new Comparator<Interval>() {

        public int compare(Interval a, Interval b) {
            if (a != null) {
                if (b != null) {
                    return a.from() - b.from();
                } else {
                    return -1;
                }
            } else {
                if (b != null) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }
    };

    public void allocateRegisters() {
        Interval precoloredCpuIntervals;
        Interval notPrecoloredCpuIntervals;

        Interval[] result = createUnhandledLists(isPrecoloredInterval, isVirtualInterval);
        precoloredCpuIntervals = result[0];
        notPrecoloredCpuIntervals = result[1];

        // allocate cpu registers
        LinearScanWalker cpuLsw = new LinearScanWalker(this, precoloredCpuIntervals, notPrecoloredCpuIntervals);
        cpuLsw.walk();
        cpuLsw.finishAllocation();
    }

    // * Phase 6: resolve data flow
    // (insert moves at edges between blocks if intervals have been split)

    // wrapper for Interval.splitChildAtOpId that performs a bailout in product mode
    // instead of returning null
    Interval splitChildAtOpId(Interval interval, int opId, LIRInstruction.OperandMode mode) {
        Interval result = interval.getSplitChildAtOpId(opId, mode, this);

        if (result != null) {
            if (C1XOptions.TraceLinearScanLevel >= 4) {
                TTY.println("Split child at pos " + opId + " of interval " + interval.toString() + " is " + result.toString());
            }
            return result;
        }

        throw new CiBailout("LinearScan: interval is null");
    }

    Interval intervalAtBlockBegin(BlockBegin block, int regNum) {
        assert numRegs <= regNum && regNum < numVariables : "register number out of bounds";
        assert intervalAt(regNum) != null : "no interval found";

        return splitChildAtOpId(intervalAt(regNum), block.firstLirInstructionId(), LIRInstruction.OperandMode.OutputMode);
    }

    Interval intervalAtBlockEnd(BlockBegin block, int regNum) {
        assert numRegs <= regNum && regNum < numVariables : "register number out of bounds";
        assert intervalAt(regNum) != null : "no interval found";

        return splitChildAtOpId(intervalAt(regNum), block.lastLirInstructionId() + 1, LIRInstruction.OperandMode.OutputMode);
    }

    Interval intervalAtOpId(int regNum, int opId) {
        assert numRegs <= regNum && regNum < numVariables : "register number out of bounds";
        assert intervalAt(regNum) != null : "no interval found";

        return splitChildAtOpId(intervalAt(regNum), opId, LIRInstruction.OperandMode.InputMode);
    }

    void resolveCollectMappings(BlockBegin fromBlock, BlockBegin toBlock, MoveResolver moveResolver) {
        assert moveResolver.checkEmpty();

        int numRegs = numVariables;
        int size = liveSetSize();
        BitMap liveAtEdge = toBlock.lirBlock.liveIn;

        // visit all registers where the liveAtEdge bit is set
        for (int r = liveAtEdge.getNextOneOffset(0, size); r < size; r = liveAtEdge.getNextOneOffset(r + 1, size)) {
            assert r < numRegs : "live information set for not exisiting interval";
            assert fromBlock.lirBlock.liveOut.get(r) && toBlock.lirBlock.liveIn.get(r) : "interval not live at this edge";

            Interval fromInterval = intervalAtBlockEnd(fromBlock, r);
            Interval toInterval = intervalAtBlockBegin(toBlock, r);

            if (fromInterval != toInterval && (fromInterval.assignedReg() != toInterval.assignedReg() || fromInterval.assignedRegHi() != toInterval.assignedRegHi())) {
                // need to insert move instruction
                moveResolver.addMapping(fromInterval, toInterval);
            }
        }
    }

    void resolveFindInsertPos(BlockBegin fromBlock, BlockBegin toBlock, MoveResolver moveResolver) {
        if (fromBlock.numberOfSux() <= 1) {
            // Util.traceLinearScan(4, "inserting moves at end of fromBlock B%d", fromBlock.blockID);

            List<LIRInstruction> instructions = fromBlock.lir().instructionsList();
            LIRInstruction instr = instructions.get(instructions.size() - 1);
            if (instr instanceof LIRBranch) {
                LIRBranch branch = (LIRBranch) instr;
                // insert moves before branch
                assert branch.cond() == Condition.TRUE : "block does not end with an unconditional jump";
                moveResolver.setInsertPosition(fromBlock.lir(), instructions.size() - 2);
            } else {
                moveResolver.setInsertPosition(fromBlock.lir(), instructions.size() - 1);
            }

        } else {
            // Util.traceLinearScan(4, "inserting moves at beginning of toBlock B%d", toBlock.blockID);

            if (C1XOptions.DetailedAsserts) {
                assert fromBlock.lir().instructionsList().get(0) instanceof LIRLabel : "block does not start with a label";

                // because the number of predecessor edges matches the number of
                // successor edges, blocks which are reached by switch statements
                // may have be more than one predecessor but it will be guaranteed
                // that all predecessors will be the same.
                for (int i = 0; i < toBlock.numberOfPreds(); i++) {
                    assert fromBlock == toBlock.predAt(i) : "all critical edges must be broken";
                }
            }

            moveResolver.setInsertPosition(toBlock.lir(), 0);
        }
    }

    // insert necessary moves (spilling or reloading) at edges between blocks if interval has been split
    void resolveDataFlow() {
        int numBlocks = blockCount();
        MoveResolver moveResolver = new MoveResolver(this);
        BitMap blockCompleted = new BitMap(numBlocks);
        BitMap alreadyResolved = new BitMap(numBlocks);

        int i;
        for (i = 0; i < numBlocks; i++) {
            BlockBegin block = blockAt(i);

            // check if block has only one predecessor and only one successor
            if (block.numberOfPreds() == 1 && block.numberOfSux() == 1 && block.numberOfExceptionHandlers() == 0 && !block.isExceptionEntry()) {
                List<LIRInstruction> instructions = block.lir().instructionsList();
                assert instructions.get(0).code == LIROpcode.Label : "block must start with label";
                assert instructions.get(instructions.size() - 1).code == LIROpcode.Branch : "block with successors must end with branch";
                assert ((LIRBranch) instructions.get(instructions.size() - 1)).cond() == Condition.TRUE : "block with successor must end with unconditional branch";

                // check if block is empty (only label and branch)
                if (instructions.size() == 2) {
                    BlockBegin pred = block.predAt(0);
                    BlockBegin sux = block.suxAt(0);

                    // prevent optimization of two consecutive blocks
                    if (!blockCompleted.get(pred.linearScanNumber()) && !blockCompleted.get(sux.linearScanNumber())) {
                        // Util.traceLinearScan(3, " optimizing empty block B%d (pred: B%d, sux: B%d)", block.blockID, pred.blockID, sux.blockID);
                        blockCompleted.set(block.linearScanNumber());

                        // directly resolve between pred and sux (without looking at the empty block between)
                        resolveCollectMappings(pred, sux, moveResolver);
                        if (moveResolver.hasMappings()) {
                            moveResolver.setInsertPosition(block.lir(), 0);
                            moveResolver.resolveAndAppendMoves();
                        }
                    }
                }
            }
        }

        for (i = 0; i < numBlocks; i++) {
            if (!blockCompleted.get(i)) {
                BlockBegin fromBlock = blockAt(i);
                alreadyResolved.setFrom(blockCompleted);

                int numSux = fromBlock.numberOfSux();
                for (int s = 0; s < numSux; s++) {
                    BlockBegin toBlock = fromBlock.suxAt(s);

                    // check for duplicate edges between the same blocks (can happen with switch blocks)
                    if (!alreadyResolved.get(toBlock.linearScanNumber())) {
                        // Util.traceLinearScan(3, " processing edge between B%d and B%d", fromBlock.blockID, toBlock.blockID);
                        alreadyResolved.set(toBlock.linearScanNumber());

                        // collect all intervals that have been split between fromBlock and toBlock
                        resolveCollectMappings(fromBlock, toBlock, moveResolver);
                        if (moveResolver.hasMappings()) {
                            resolveFindInsertPos(fromBlock, toBlock, moveResolver);
                            moveResolver.resolveAndAppendMoves();
                        }
                    }
                }
            }
        }
    }

    void resolveExceptionEntry(BlockBegin block, int regNum, MoveResolver moveResolver) {
        if (intervalAt(regNum) == null) {
            // if a phi function is never used, no interval is created . ignore this
            return;
        }

        Interval interval = intervalAtBlockBegin(block, regNum);
        int reg = interval.assignedReg();
        int regHi = interval.assignedRegHi();

        if (reg < numRegs && interval.alwaysInMemory()) {
            // the interval is split to get a short range that is located on the stack
            // in the following two cases:
            // * the interval started in memory (e.g. method parameter), but is currently in a register
            // this is an optimization for exception handling that reduces the number of moves that
            // are necessary for resolving the states when an exception uses this exception handler
            // * the interval would be on the fpu stack at the begin of the exception handler
            // this is not allowed because of the complicated fpu stack handling on Intel

            // range that will be spilled to memory
            int fromOpId = block.firstLirInstructionId();
            int toOpId = fromOpId + 1; // short live range of length 1
            assert interval.from() <= fromOpId && interval.to() >= toOpId : "no split allowed between exception entry and first instruction";

            if (interval.from() != fromOpId) {
                // the part before fromOpId is unchanged
                interval = interval.split(fromOpId);
                interval.assignReg(reg, regHi);
                appendInterval(interval);
            }
            assert interval.from() == fromOpId : "must be true now";

            Interval spilledPart = interval;
            if (interval.to() != toOpId) {
                // the part after toOpId is unchanged
                spilledPart = interval.splitFromStart(toOpId);
                appendInterval(spilledPart);
                moveResolver.addMapping(spilledPart, interval);
            }
            assignSpillSlot(spilledPart);

            assert spilledPart.from() == fromOpId && spilledPart.to() == toOpId : "just checking";
        }
    }

    void resolveExceptionEntry(BlockBegin block, MoveResolver moveResolver) {
        assert block.checkBlockFlag(BlockBegin.BlockFlag.ExceptionEntry) : "should not call otherwise";
        assert moveResolver.checkEmpty();

        // visit all registers where the liveIn bit is set
        int size = liveSetSize();
        for (int r = block.lirBlock.liveIn.getNextOneOffset(0, size); r < size; r = block.lirBlock.liveIn.getNextOneOffset(r + 1, size)) {
            resolveExceptionEntry(block, r, moveResolver);
        }

        // the liveIn bits are not set for phi functions of the xhandler entry, so iterate them separately
        for (Phi phi : block.allLivePhis()) {
            resolveExceptionEntry(block, phi.operand().variableNumber(), moveResolver);
        }

        if (moveResolver.hasMappings()) {
            // insert moves after first instruction
            moveResolver.setInsertPosition(block.lir(), 0);
            moveResolver.resolveAndAppendMoves();
        }
    }

    void resolveExceptionEdge(ExceptionHandler handler, int throwingOpId, int regNum, Phi phi, MoveResolver moveResolver) {
        if (intervalAt(regNum) == null) {
            // if a phi function is never used, no interval is created . ignore this
            return;
        }

        // the computation of toInterval is equal to resolveCollectMappings,
        // but fromInterval is more complicated because of phi functions
        BlockBegin toBlock = handler.entryBlock();
        Interval toInterval = intervalAtBlockBegin(toBlock, regNum);

        if (phi != null) {
            // phi function of the exception entry block
            // no moves are created for this phi function in the LIRGenerator, so the
            // interval at the throwing instruction must be searched using the operands
            // of the phi function
            Value fromValue = phi.operandAt(handler.phiOperand());

            // with phi functions it can happen that the same fromValue is used in
            // multiple mappings, so notify move-resolver that this is allowed
            moveResolver.setMultipleReadsAllowed();

            Constant con = null;
            if (fromValue instanceof Constant) {
                con = (Constant) fromValue;
            }
            if (con != null && (con.operand().isIllegal() || con.operand().isConstant())) {
                // unpinned constants may have no register, so add mapping from constant to interval
                moveResolver.addMapping(con.asConstant(), toInterval);
            } else {
                // search split child at the throwing opId
                Interval fromInterval = intervalAtOpId(fromValue.operand().variableNumber(), throwingOpId);
                moveResolver.addMapping(fromInterval, toInterval);
            }

        } else {
            // no phi function, so use regNum also for fromInterval
            // search split child at the throwing opId
            Interval fromInterval = intervalAtOpId(regNum, throwingOpId);
            if (fromInterval != toInterval) {
                // optimization to reduce number of moves: when toInterval is on stack and
                // the stack slot is known to be always correct, then no move is necessary
                if (!fromInterval.alwaysInMemory() || fromInterval.canonicalSpillSlot() != toInterval.assignedReg()) {
                    moveResolver.addMapping(fromInterval, toInterval);
                }
            }
        }
    }

    void resolveExceptionEdge(ExceptionHandler handler, int throwingOpId, MoveResolver moveResolver) {
        // Util.traceLinearScan(4, "resolving exception handler B%d: throwingOpId=%d", handler.entryBlock().blockID, throwingOpId);

        assert moveResolver.checkEmpty();
        assert handler.lirOpId() == -1 : "already processed this xhandler";
        handler.setLirOpId(throwingOpId);
        assert handler.entryCode() == null : "code already present";

        // visit all registers where the liveIn bit is set
        BlockBegin block = handler.entryBlock();
        int size = liveSetSize();
        for (int r = block.lirBlock.liveIn.getNextOneOffset(0, size); r < size; r = block.lirBlock.liveIn.getNextOneOffset(r + 1, size)) {
            resolveExceptionEdge(handler, throwingOpId, r, null, moveResolver);
        }

        // the liveIn bits are not set for phi functions of the xhandler entry, so iterate them separately
        for (Phi phi : block.allLivePhis()) {
            resolveExceptionEdge(handler, throwingOpId, phi.operand().variableNumber(), phi, moveResolver);
        }
        if (moveResolver.hasMappings()) {
            LIRList entryCode = new LIRList(gen);
            moveResolver.setInsertPosition(entryCode, 0);
            moveResolver.resolveAndAppendMoves();

            entryCode.jump(handler.entryBlock());
            handler.setEntryCode(entryCode);
        }
    }

    void resolveExceptionHandlers() {
        MoveResolver moveResolver = new MoveResolver(this);
        //LIRVisitState visitor = new LIRVisitState();
        int numBlocks = blockCount();

        int i;
        for (i = 0; i < numBlocks; i++) {
            BlockBegin block = blockAt(i);
            if (block.checkBlockFlag(BlockFlag.ExceptionEntry)) {
                resolveExceptionEntry(block, moveResolver);
            }
        }

        for (i = 0; i < numBlocks; i++) {
            BlockBegin block = blockAt(i);
            LIRList ops = block.lir();
            int numOps = ops.length();

            // iterate all instructions of the block. skip the first because it is always a label
            assert !ops.at(0).hasOperands() : "first operation must always be a label";
            for (int j = 1; j < numOps; j++) {
                LIRInstruction op = ops.at(j);
                int opId = op.id;

                if (opId != -1 && op.hasInfo()) {
                    // visit operation to collect all operands
                    //visitor.visit(op);

                    for (ExceptionHandler h : op.exceptionEdges()) {
                        resolveExceptionEdge(h, opId, moveResolver);
                    }

                } else if (C1XOptions.DetailedAsserts) {
                    //visitor.visit(op);
                    assert op.exceptionEdges().size() == 0 : "missed exception handler";
                }
            }
        }
    }

    // * Phase 7: assign register numbers back to LIR
    // (includes computation of debug information and oop maps)

    CiLocation vmRegForInterval(Interval interval) {
        CiLocation reg = interval.cachedVmReg();
        if (reg == null) {
            reg = vmRegForOperand(operandForInterval(interval));
            interval.setCachedVmReg(reg);
        }
        assert reg.equals(vmRegForOperand(operandForInterval(interval))) : "wrong cached value";
        return reg;
    }

    CiLocation vmRegForOperand(CiValue opr) {
        return frameMap.toLocation(opr);
    }

    CiValue operandForInterval(Interval interval) {
        CiValue opr = interval.cachedOpr();
        if (opr.isIllegal()) {
            opr = calcOperandForInterval(interval);
            interval.setCachedOpr(opr);
        }

        assert opr.equals(calcOperandForInterval(interval)) : "wrong cached value";
        return opr;
    }

    CiValue calcOperandForInterval(Interval interval) {
        int assignedReg = interval.assignedReg();
        CiKind kind = interval.kind();

        if (assignedReg >= numRegs) {
            // stack slot
            assert interval.assignedRegHi() == getAnyreg() : "must not have hi register";
            return CiStackSlot.get(kind, (assignedReg - numRegs));

        } else {
            // register
            switch (kind) {
                case Object: {
                    assert isCpu(assignedReg) : "no cpu register";
                    assert interval.assignedRegHi() == getAnyreg() : "must not have hi register";
                    return toRegister(assignedReg).asLocation(CiKind.Object);
                }

                case Word: {
                    assert isCpu(assignedReg) : "no cpu register";
                    assert interval.assignedRegHi() == getAnyreg() : "must not have hi register";
                    return toRegister(assignedReg).asLocation(CiKind.Word);
                }

                case Byte:
                case Char:
                case Short:
                case Jsr:
                case Int: {
                    assert isCpu(assignedReg) : "no cpu register";
                    assert interval.assignedRegHi() == getAnyreg() : "must not have hi register";
                    return toRegister(assignedReg).asLocation(kind);
                }

                case Long: {
                    int assignedRegHi = interval.assignedRegHi();
                    assert isCpu(assignedReg) : "no cpu register";
                    assert numPhysicalRegs(CiKind.Long) == 1 || (isCpu(assignedRegHi)) : "no cpu register";

                    assert assignedReg != assignedRegHi : "invalid allocation";
                    assert numPhysicalRegs(CiKind.Long) == 1 || assignedReg < assignedRegHi : "register numbers must be sorted (ensure that e.g. a move from eax,ebx to ebx,eax can not occur)";
                    assert (assignedRegHi != getAnyreg()) ^ (numPhysicalRegs(CiKind.Long) == 1) : "must be match";
                    if (requiresAdjacentRegs(CiKind.Long)) {
                        assert assignedReg % 2 == 0 && assignedReg + 1 == assignedRegHi : "must be sequential and even";
                    }

                    if (compilation.target.arch.is64bit()) {
                        return toRegister(assignedReg).asLocation(CiKind.Long);
                    } else {
                        throw Util.unimplemented("32-bit not supported");
                    }
                }

                case Float: {
                    if (compilation.target.arch.isX86()) {
                        assert isXmm(assignedReg) : "no xmm register";
                        assert interval.assignedRegHi() == getAnyreg() : "must not have hi register";
                        return toRegister(assignedReg).asLocation(CiKind.Float);
                    }

                    assert interval.assignedRegHi() == getAnyreg() : "must not have hi register";
                    return toRegister(assignedReg).asLocation(CiKind.Float);
                }

                case Double: {
                    if (compilation.target.arch.is64bit()) {
                        assert isXmm(assignedReg) : "no xmm register";
                        assert interval.assignedRegHi() == getAnyreg() : "must not have hi register (double xmm values are stored in one register)";
                        return toRegister(assignedReg).asLocation(CiKind.Double);
                    } else {
                        throw Util.unimplemented("32-bit not supported");
                    }
                }

                default: {
                    Util.shouldNotReachHere();
                    return CiValue.IllegalLocation;
                }
            }
        }
    }

    boolean isXmm(int assignedReg) {
        return assignedReg >= 0 && assignedReg < allocatableRegisters.registerMapping.length && allocatableRegisters.registerMapping[assignedReg] != null && this.allocatableRegisters.registerMapping[assignedReg].isXmm();
    }

    CiRegister toRegister(int assignedReg) {
        final CiRegister result = allocatableRegisters.registerMapping[assignedReg];
        assert result != null : "register not found!";
        return result;
    }

    boolean isCpu(int assignedReg) {

        return assignedReg >= 0 && assignedReg < allocatableRegisters.registerMapping.length && allocatableRegisters.registerMapping[assignedReg] != null && allocatableRegisters.registerMapping[assignedReg].isCpu();
    }

    CiValue canonicalSpillOpr(Interval interval) {
        assert interval.canonicalSpillSlot() >= numRegs : "canonical spill slot not set";
        return CiStackSlot.get(interval.kind(), (interval.canonicalSpillSlot() - numRegs));
    }

    CiLocation colorLirOpr(CiValue opr, int opId, LIRInstruction.OperandMode mode) {
        assert opr.isVariable() : "should not call this otherwise";

        Interval interval = intervalAt(opr.variableNumber());
        assert interval != null : "interval must exist";

        if (opId != -1) {
            if (C1XOptions.DetailedAsserts) {
                BlockBegin block = blockOfOpWithId(opId);
                if (block.numberOfSux() <= 1 && opId == block.lastLirInstructionId()) {
                    // check if spill moves could have been appended at the end of this block, but
                    // before the branch instruction. So the split child information for this branch would
                    // be incorrect.
                    LIRInstruction instr = block.lir().instructionsList().get(block.lir().instructionsList().size() - 1);
                    if (instr instanceof LIRBranch) {
                        LIRBranch branch = (LIRBranch) instr;
                        if (block.lirBlock.liveOut.get(opr.variableNumber())) {
                            assert branch.cond() == Condition.TRUE : "block does not end with an unconditional jump";
                            throw new CiBailout("can't get split child for the last branch of a block because the information would be incorrect (moves are inserted before the branch in resolveDataFlow)");
                        }
                    }
                }
            }

            // operands are not changed when an interval is split during allocation,
            // so search the right interval here
            interval = splitChildAtOpId(interval, opId, mode);
        }

        CiLocation res = (CiLocation) operandForInterval(interval);
        return res;
    }

    void assertEqual(CiValue m1, CiValue m2) {
        if (m1 == null) {
            assert m2 == null;
        } else {
            assert m1.equals(m2);
        }
    }

    void assertEqual(IRScopeDebugInfo d1, IRScopeDebugInfo d2) {
    }

    IntervalWalker initComputeOopMaps() {
        // setup lists of potential oops for walking
        Interval oopIntervals;
        Interval nonOopIntervals;

        Interval[] result = createUnhandledLists(isOopInterval, null);
        oopIntervals = result[0];

        // intervals that have no oops inside need not to be processed
        // to ensure a walking until the last instruction id, add a dummy interval
        // with a high operation id
        nonOopIntervals = new Interval(getAnyreg());
        nonOopIntervals.addRange(Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1);

        return new IntervalWalker(this, oopIntervals, nonOopIntervals);
    }

    void computeOopMap(IntervalWalker iw, LIRInstruction op, LIRDebugInfo info, boolean isCallSite) {
        // Util.traceLinearScan(3, "creating oop map at opId %d", op.id());

        // walk before the current operation . intervals that start at
        // the operation (= output operands of the operation) are not
        // included in the oop map
        iw.walkBefore(op.id);

        info.allocateDebugInfo(compilation.target.allocatableRegs.registerRefMapSize, compilation.frameMap().frameSize(), compilation.target);

        // Iterate through active intervals
        for (Interval interval = iw.activeFirst(IntervalKind.FixedKind); interval != Interval.EndMarker; interval = interval.next) {
            int assignedReg = interval.assignedReg();

            assert interval.currentFrom() <= op.id && op.id <= interval.currentTo() : "interval should not be active otherwise";
            assert interval.assignedRegHi() == getAnyreg() : "oop must be single word";
            assert interval.registerNumber() >= CiRegister.LowestVirtualRegisterNumber : "fixed interval found";

            // Check if this range covers the instruction. Intervals that
            // start or end at the current operation are not included in the
            // oop map, except in the case of patching moves. For patching
            // moves, any intervals which end at this instruction are included
            // in the oop map since we may safepoint while doing the patch
            // before we've consumed the inputs.
            if (op.id < interval.currentTo()) {
                // caller-save registers must not be included into oop-maps at calls
                assert !isCallSite || assignedReg >= numRegs || !isCallerSave(assignedReg) : "interval is in a caller-save register at a call . register will be overwritten";

                info.setOop(vmRegForInterval(interval), compilation.target);

                // Spill optimization: when the stack value is guaranteed to be always correct,
                // then it must be added to the oop map even if the interval is currently in a register
                if (interval.alwaysInMemory() && op.id > interval.spillDefinitionPos() && interval.assignedReg() != interval.canonicalSpillSlot()) {
                    assert interval.spillDefinitionPos() > 0 : "position not set correctly";
                    assert interval.canonicalSpillSlot() >= numRegs : "no spill slot assigned";
                    assert interval.assignedReg() < numRegs : "interval is on stack :  so stack slot is registered twice";

                    info.setOop(frameMap.toStackLocation(CiKind.Object, interval.canonicalSpillSlot() - numRegs), compilation.target);
                }
            }
        }
    }

    private boolean isCallerSave(int assignedReg) {
        return Util.nonFatalUnimplemented(false);
    }

    void computeOopMap(IntervalWalker iw, LIRInstruction op) {
        assert op.hasInfo() : "no oop map needed";

        for (int i = 0; i < op.infoCount(); i++) {
            LIRDebugInfo info = op.infoAt(i);
            assert !info.hasDebugInfo() : "oop map already computed for info";
            computeOopMap(iw, op, info, op.hasCall());
        }
    }

    int appendScopeValueForConstant(CiValue opr, List<CiValue> scopeValues) {
        assert opr.isConstant() : "should not be called otherwise";

        CiConstant c = (CiConstant) opr;
        switch (c.kind) {
            case Object: // fall through
            case Int: // fall through
            case Float: {
                scopeValues.add(c);
                return 1;
            }

            case Long: // fall through
            case Double: {
                long longBits = Double.doubleToRawLongBits(c.asDouble());
                if (compilation.target.arch.highWordOffset > compilation.target.arch.lowWordOffset) {
                    scopeValues.add(CiConstant.forInt((int) (longBits >> 32)));
                    scopeValues.add(CiConstant.forInt((int) longBits));
                } else {
                    scopeValues.add(CiConstant.forInt((int) longBits));
                    scopeValues.add(CiConstant.forInt((int) (longBits >> 32)));
                }
                return 2;
            }

            default:
                Util.shouldNotReachHere();
                return -1;
        }
    }

    int appendScopeValueForOperand(CiValue opr, List<CiValue> scopeValues) {
        if (opr.kind.jvmSlots == 2) {
            // The convention the interpreter uses is that the second local
            // holds the first raw word of the native double representation.
            // This is actually reasonable, since locals and stack arrays
            // grow downwards in all implementations.
            // (If, on some machine, the interpreter's Java locals or stack
            // were to grow upwards, the embedded doubles would be word-swapped.)
            scopeValues.add(null);
        }
        scopeValues.add(opr);
        return opr.kind.jvmSlots;
    }

    int appendScopeValue(int opId, Value value, List<CiValue> scopeValues) {
        if (value != null) {
            CiValue opr = value.operand();
            Constant con = null;
            if (value instanceof Constant) {
                con = (Constant) value;
            }

            assert con == null || opr.isVariable() || opr.isConstant() || opr.isIllegal() : "asumption: Constant instructions have only constant operands (or illegal if constant is optimized away)";
            assert con != null || opr.isVariable() : "asumption: non-Constant instructions have only virtual operands";

            if (con != null && !con.isLive() && !opr.isConstant()) {
                // Unpinned constants may have a virtual operand for a part of the lifetime
                // or may be illegal when it was optimized away,
                // so always use a constant operand
                opr = con.asConstant();
            }
            assert opr.isVariable() || opr.isConstant() : "other cases not allowed here";

            if (opr.isVariable()) {
                BlockBegin block = blockOfOpWithId(opId);
                if (block.numberOfSux() == 1 && opId == block.lastLirInstructionId()) {
                    // generating debug information for the last instruction of a block.
                    // if this instruction is a branch, spill moves are inserted before this branch
                    // and so the wrong operand would be returned (spill moves at block boundaries are not
                    // considered in the live ranges of intervals)
                    // Solution: use the first opId of the branch target block instead.
                    final LIRInstruction instr = block.lir().instructionsList().get(block.lir().instructionsList().size() - 1);
                    if (instr instanceof LIRBranch) {
                        if (block.lirBlock.liveOut.get(opr.variableNumber())) {
                            opId = block.suxAt(0).firstLirInstructionId();
                        }
                    }
                }

                // Get current location of operand
                // The operand must be live because debug information is considered when building the intervals
                // if the interval is not live, colorLirOpr will cause an assert on failure opr = colorLirOpr(opr, opId,
                // mode);
                assert !hasCall(opId) || opr.isStackSlot() || !isCallerSave(regNum(opr)) : "can not have caller-save register operands at calls";

                // Append to ScopeValue array
                return appendScopeValueForOperand(opr, scopeValues);

            } else {
                assert value instanceof Constant : "all other instructions have only virtual operands";
                assert opr.isConstant() : "operand must be constant";

                return appendScopeValueForConstant(opr, scopeValues);
            }
        } else {
            // append a dummy value because real value not needed
            scopeValues.add(CiValue.IllegalLocation);
            return 1;
        }
    }

    IRScopeDebugInfo computeDebugInfoForScope(int opId, IRScope curScope, FrameState curState, FrameState innermostState, int curBci, int stackEnd, int locksEnd) {
        if (true) {
            return null;
        }
        IRScopeDebugInfo callerDebugInfo = null;
        int stackBegin;
        int locksBegin;

        FrameState callerState = curScope.callerState();
        if (callerState != null) {
            // process recursively to compute outermost scope first
            stackBegin = callerState.stackSize();
            locksBegin = callerState.locksSize();
            callerDebugInfo = computeDebugInfoForScope(opId, curScope.caller, callerState, innermostState, curScope.callerBCI(), stackBegin, locksBegin);
        } else {
            stackBegin = 0;
            locksBegin = 0;
        }

        // initialize these to null.
        // If we don't need deopt info or there are no locals, expressions or monitors,
        // then these get recorded as no information and avoids the allocation of 0 length arrays.
        List<CiValue> locals = null;
        List<CiValue> expressions = null;
        List<CiLocation> monitors = null;

        // describe local variable values
        int nofLocals = curScope.method.maxLocals();
        if (nofLocals > 0) {
            locals = new ArrayList<CiValue>(nofLocals);

            int pos = 0;
            while (pos < nofLocals) {
                assert pos < curState.localsSize() : "why not?";

                Value local = curState.localAt(pos);
                pos += appendScopeValue(opId, local, locals);

                assert locals.size() == pos : "must match";
            }
            assert locals.size() == curScope.method.maxLocals() : "wrong number of locals";
            assert locals.size() == curState.localsSize() : "wrong number of locals";
        }

        // describe expression stack
        //
        // When we inline methods containing exception handlers, the
        // "lockStacks" are changed to preserve expression stack values
        // in caller scopes when exception handlers are present. This
        // can cause callee stacks to be smaller than caller stacks.
        if (stackEnd > innermostState.stackSize()) {
            stackEnd = innermostState.stackSize();
        }

        int nofStack = stackEnd - stackBegin;
        if (nofStack > 0) {
            expressions = new ArrayList<CiValue>(nofStack);

            int pos = stackBegin;
            while (pos < stackEnd) {
                Value expression = innermostState.stackAt(pos);
                appendScopeValue(opId, expression, expressions);

                assert expressions.size() + stackBegin == pos : "must match";
                pos++;
            }
        }

        // describe monitors
        assert locksBegin <= locksEnd : "error in scope iteration";
        int nofLocks = locksEnd - locksBegin;
        if (nofLocks > 0) {
            monitors = new ArrayList<CiLocation>(nofLocks);
            for (int i = locksBegin; i < locksEnd; i++) {
                monitors.add(frameMap.toMonitorLocation(i));
            }
        }
        return null;
        // TODO:
    }

    void computeDebugInfo(IntervalWalker iw, LIRInstruction op) {
        assert iw != null : "interval walker needed for debug information";
        computeOopMap(iw, op);
        for (int i = 0; i < op.infoCount(); i++) {
            LIRDebugInfo info = op.infoAt(i);
            computeDebugInfo(info, op.id);
        }
    }

    void computeDebugInfo(LIRDebugInfo info, int opId) {
        if (!compilation.needsDebugInformation()) {
            return;
        }
        // Util.traceLinearScan(3, "creating debug information at opId %d", opId);

        FrameState innermostState = info.state;
        assert innermostState != null : "why is it missing?";

        IRScope innermostScope = innermostState.scope();

        assert innermostScope != null : "why is it missing?";

        int stackEnd = innermostState.stackSize();
        int locksEnd = innermostState.locksSize();

        IRScopeDebugInfo debugInfo = computeDebugInfoForScope(opId, innermostScope, innermostState, innermostState, info.bci, stackEnd, locksEnd);
        if (info.scopeDebugInfo == null) {
            // compute debug information
            info.scopeDebugInfo = debugInfo;
        } else {
            // debug information already set. Check that it is correct from the current point of view
            assertEqual(info.scopeDebugInfo, debugInfo);
        }
    }

    void assignRegNum(List<LIRInstruction> instructions, IntervalWalker iw) {
        int numInst = instructions.size();
        boolean hasDead = false;

        for (int j = 0; j < numInst; j++) {
            LIRInstruction op = instructions.get(j);
            if (op == null) { // this can happen when spill-moves are removed in eliminateSpillMoves
                hasDead = true;
                continue;
            }

            // iterate all modes of the visitor and process all virtual operands
            for (LIRInstruction.OperandMode mode : LIRInstruction.OPERAND_MODES) {
                int n = op.oprCount(mode);
                for (int k = 0; k < n; k++) {
                    CiLocation opr = op.oprAt(mode, k);
                    if (opr.isVariable()) {
                        op.setOprAt(mode, k, colorLirOpr(opr, op.id, mode));
                    }
                }
            }

            if (op.hasInfo()) {
                // exception handling
                if (compilation.hasExceptionHandlers()) {
                    for (ExceptionHandler handler : op.exceptionEdges()) {
                        if (handler.entryCode() != null) {
                            assignRegNum(handler.entryCode().instructionsList(), null);
                        }
                    }
                }

                // compute reference map and debug information
                computeDebugInfo(iw, op);
            }

            // make sure we haven't made the op invalid.
            assert op.verify();

            // remove useless moves
            if (op.code == LIROpcode.Move) {
                LIROp1 move = (LIROp1) op;
                CiValue src = move.operand();
                CiValue dst = move.result();
                if (dst == src || src.equals(dst)) {
                    // TODO: what about o.f = o.f and exceptions?
                    instructions.set(j, null);
                    hasDead = true;
                }
            }
        }

        if (hasDead) {
            // iterate all instructions of the block and remove all null-values.
            int insertPoint = 0;
            for (int j = 0; j < numInst; j++) {
                LIRInstruction op = instructions.get(j);
                if (op != null) {
                    if (insertPoint != j) {
                        instructions.set(insertPoint, op);
                    }
                    insertPoint++;
                }
            }
            Util.truncate(instructions, insertPoint);
        }
    }

    void assignRegNum() {
        IntervalWalker iw = initComputeOopMaps();
        for (BlockBegin block : sortedBlocks) {
            assignRegNum(block.lir().instructionsList(), iw);
        }
    }

    public void allocate() {
        if (C1XOptions.PrintTimers) {
            C1XTimers.LIFETIME_ANALYSIS.start();
        }

        numberInstructions();

        printLir("Before register allocation", true);

        computeLocalLiveSets();
        computeGlobalLiveSets();

        buildIntervals();
        sortIntervalsBeforeAllocation();

        if (C1XOptions.PrintTimers) {
            C1XTimers.LIFETIME_ANALYSIS.stop();
            C1XTimers.LINEAR_SCAN.start();
        }

        printIntervals("Before register allocation");

        allocateRegisters();

        if (C1XOptions.PrintTimers) {
            C1XTimers.LINEAR_SCAN.stop();
            C1XTimers.RESOLUTION.start();
        }

        resolveDataFlow();
        if (compilation.hasExceptionHandlers()) {
            resolveExceptionHandlers();
        }

        if (C1XOptions.PrintTimers) {
            C1XTimers.RESOLUTION.stop();
            C1XTimers.DEBUG_INFO.start();
        }

        C1XMetrics.LSRASpills += maxSpills;

        // fill in number of spill slots into frameMap
        frameMap.finalizeFrame(maxSpills);

        printIntervals("After register allocation");
        printLir("After register allocation", true);

        sortIntervalsAfterAllocation();

        assert verify();

        eliminateSpillMoves();
        assignRegNum();

        if (C1XOptions.PrintTimers) {
            C1XTimers.DEBUG_INFO.stop();
            C1XTimers.CODE_CREATE.start();
        }

        printLir("After register number assignment", true);

        EdgeMoveOptimizer.optimize(ir.linearScanOrder());
        if (C1XOptions.OptControlFlow) {
            ControlFlowOptimizer.optimize(ir);
        }

        printLir("After control flow optimization", false);
    }

    void printIntervals(String label) {
        if (C1XOptions.TraceLinearScanLevel >= 1) {
            int i;
            TTY.println();
            TTY.println(label);

            for (i = 0; i < intervalCount(); i++) {
                Interval interval = intervalAt(i);
                if (interval != null) {
                    interval.print(TTY.out(), this);
                }
            }

            TTY.println();
            TTY.println("--- Basic Blocks ---");
            for (i = 0; i < blockCount(); i++) {
                BlockBegin block = blockAt(i);
                TTY.print("B%d [%d, %d, %d, %d] ", block.blockID, block.firstLirInstructionId(), block.lastLirInstructionId(), block.loopIndex(), block.loopDepth());
            }
            TTY.println();
            TTY.println();
        }

        if (compilation.cfgPrinter() != null) {
            compilation.cfgPrinter().printIntervals(this, intervalsMap, label);
        }
    }

    void printLir(String label, boolean hirValid) {
        if (C1XOptions.TraceLinearScanLevel >= 1) {
            TTY.println();
            TTY.println(label);
            LIRList.printLIR(ir.linearScanOrder());
            TTY.println();
        }

        if (compilation.cfgPrinter() != null) {
            compilation.cfgPrinter().printCFG(compilation.hir().startBlock, label, hirValid, true);
        }
    }

    boolean verify() {
        // (check that all intervals have a correct register and that no registers are overwritten)
        // Util.traceLinearScan(2, " verifying intervals *");
        verifyIntervals();

        // Util.traceLinearScan(2, " verifying that no oops are in fixed intervals *");
        //verifyNoOopsInFixedIntervals();

        // Util.traceLinearScan(2, " verifying that unpinned constants are not alive across block boundaries");
        verifyConstants();

        // Util.traceLinearScan(2, " verifying register allocation *");
        verifyRegisters();

        // Util.traceLinearScan(2, " no errors found *");

        return true;
    }

    private void verifyRegisters() {
        RegisterVerifier verifier = new RegisterVerifier(this);
        verifier.verify(blockAt(0));
    }

    void verifyIntervals() {
        int len = intervalCount();

        for (int i = 0; i < len; i++) {
            Interval i1 = intervalAt(i);
            if (i1 == null) {
                continue;
            }

            i1.checkSplitChildren();

            if (i1.registerNumber() != i) {
                TTY.println("Interval %d is on position %d in list", i1.registerNumber(), i);
                i1.print(TTY.out(), this);
                TTY.println();
                throw new CiBailout("");
            }

            if (i1.registerNumber() >= CiRegister.LowestVirtualRegisterNumber && i1.kind() == CiKind.Illegal) {
                TTY.println("Interval %d has no type assigned", i1.registerNumber());
                i1.print(TTY.out(), this);
                TTY.println();
                throw new CiBailout("");
            }

            if (i1.assignedReg() == getAnyreg()) {
                TTY.println("Interval %d has no register assigned", i1.registerNumber());
                i1.print(TTY.out(), this);
                TTY.println();
                throw new CiBailout("");
            }

            if (i1.assignedReg() == i1.assignedRegHi()) {
                TTY.println("Interval %d: low and high register equal", i1.registerNumber());
                i1.print(TTY.out(), this);
                TTY.println();
                throw new CiBailout("");
            }

            if (!isProcessedRegNum(i1.assignedReg())) {
                TTY.println("Can not have an Interval for an ignored register " + i1.assignedReg());
                i1.print(TTY.out(), this);
                TTY.println();
                throw new CiBailout("");
            }

            if (i1.first() == Range.EndMarker) {
                TTY.println("Interval %d has no Range", i1.registerNumber());
                i1.print(TTY.out(), this);
                TTY.println();
                throw new CiBailout("");
            }

            for (Range r = i1.first(); r != Range.EndMarker; r = r.next) {
                if (r.from >= r.to) {
                    TTY.println("Interval %d has zero length range", i1.registerNumber());
                    i1.print(TTY.out(), this);
                    TTY.println();
                    throw new CiBailout("");
                }
            }

            for (int j = i + 1; j < len; j++) {
                Interval i2 = intervalAt(j);
                if (i2 == null) {
                    continue;
                }

                // special intervals that are created in MoveResolver
                // . ignore them because the range information has no meaning there
                if (i1.from() == 1 && i1.to() == 2) {
                    continue;
                }
                if (i2.from() == 1 && i2.to() == 2) {
                    continue;
                }

                int r1 = i1.assignedReg();
                int r1Hi = i1.assignedRegHi();
                int r2 = i2.assignedReg();
                int r2Hi = i2.assignedRegHi();
                if (i1.intersects(i2) && (r1 == r2 || r1 == r2Hi || (r1Hi != getAnyreg() && (r1Hi == r2 || r1Hi == r2Hi)))) {
                    if (C1XOptions.DetailedAsserts) {
                        TTY.println("Intervals %d and %d overlap and have the same register assigned", i1.registerNumber(), i2.registerNumber());
                        i1.print(TTY.out(), this);
                        TTY.println();
                        i2.print(TTY.out(), this);
                        TTY.println();
                    }
                    throw new CiBailout("");
                }
            }
        }
    }

    void verifyNoOopsInFixedIntervals() {
        Interval fixedIntervals;
        Interval otherIntervals;
        Interval[] result = createUnhandledLists(isPrecoloredInterval, null);
        fixedIntervals = result[0];
        // to ensure a walking until the last instruction id, add a dummy interval
        // with a high operation id
        otherIntervals = new Interval(getAnyreg());
        otherIntervals.addRange(Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1);
        IntervalWalker iw = new IntervalWalker(this, fixedIntervals, otherIntervals);

        for (int i = 0; i < blockCount(); i++) {
            BlockBegin block = blockAt(i);

            List<LIRInstruction> instructions = block.lir().instructionsList();

            for (int j = 0; j < instructions.size(); j++) {
                LIRInstruction op = instructions.get(j);

                if (op.hasInfo()) {
                    iw.walkBefore(op.id);
                    boolean checkLive = true;
                    LIRBranch branch = null;
                    if (op instanceof LIRBranch) {
                        branch = (LIRBranch) op;
                    }
                    if (branch != null && branch.stub != null && branch.stub.isExceptionThrowStub()) {
                        // Don't bother checking the stub in this case since the
                        // exception stub will never return to normal control flow.
                        checkLive = false;
                    }

                    // Make sure none of the fixed registers is live across an
                    // oopmap since we can't handle that correctly.
                    if (checkLive) {
                        for (Interval interval = iw.activeFirst(IntervalKind.FixedKind); interval != Interval.EndMarker; interval = interval.next) {
                            if (interval.currentTo() > op.id + 1) {
                                // This interval is live out of this op so make sure
                                // that this interval represents some value that's
                                // referenced by this op either as an input or output.
                                boolean ok = false;
                                for (LIRInstruction.OperandMode mode : LIRInstruction.OPERAND_MODES) {
                                    int n = op.oprCount(mode);
                                    for (int k = 0; k < n; k++) {
                                        CiLocation opr = op.oprAt(mode, k);
                                        if (opr.isRegister()) {
                                            if (intervalAt(regNum(opr)) == interval) {
                                                ok = true;
                                                break;
                                            }
                                            int hi = regNumHi(opr);
                                            if (hi != -1 && intervalAt(hi) == interval) {
                                                ok = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                                assert ok : "fixed intervals should never be live across an oopmap point";
                            }
                        }
                    }
                }
            }
        }
    }

    void verifyConstants() {
        int size = liveSetSize();
        int numBlocks = blockCount();

        for (int i = 0; i < numBlocks; i++) {
            BlockBegin block = blockAt(i);
            BitMap liveAtEdge = block.lirBlock.liveIn;

            // visit all registers where the liveAtEdge bit is set
            for (int r = liveAtEdge.getNextOneOffset(0, size); r < size; r = liveAtEdge.getNextOneOffset(r + 1, size)) {
                // Util.traceLinearScan(4, "checking interval %d of block B%d", r, block.blockID);

                Value value = gen.instructionForVariable(r);

                assert value != null : "all intervals live across block boundaries must have Value";
                assert value.operand().isVariableOrRegister() && value.operand().isVariable() : "value must have virtual operand";
                assert value.operand().variableNumber() == r : "register number must match";
                // TKR assert value.asConstant() == null || value.isPinned() :
                // "only pinned constants can be alive accross block boundaries";
            }
        }
    }

    public int numberOfSpillSlots(CiKind kind) {
        return compilation.target.spillSlots(kind);
    }

    // TODO: Platform specific!!
    public int numPhysicalRegs(CiKind kind) {
        if (kind == CiKind.Double && compilation.target.arch.is32bit()) {
            return 2;
        } else {
            return 1;
        }
    }

    // TODO: Platform specific!!
    public boolean requiresAdjacentRegs(CiKind kind) {
        return Util.nonFatalUnimplemented(false);
    }

    static int getAnyreg() {
        return CiRegister.None.number;
    }
}

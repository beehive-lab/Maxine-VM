package com.sun.c1x.alloc;

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.util.*;

public class RegisterVerifier {

    LinearScan allocator;
    List<BlockBegin> workList; // all blocks that must be processed
    ArrayMap<List<Interval>> savedStates; // saved information of previous check

    // simplified access to methods of LinearScan
    C1XCompilation compilation() {
        return allocator.compilation();
    }

    Interval intervalAt(int regNum) {
        return allocator.intervalAt(regNum);
    }

    int regNum(LIROperand opr) {
        return allocator.regNum(opr);
    }

    // currently, only registers are processed
    int stateSize() {
        return allocator.nofRegs;
    }

    // accessors
    List<Interval> stateForBlock(BlockBegin block) {
        return savedStates.get(block.blockID());
    }

    void setStateForBlock(BlockBegin block, List<Interval> savedState) {
        savedStates.put(block.blockID(), savedState);
    }

    void addToWorkList(BlockBegin block) {
        if (!workList.contains(block)) {
            workList.add(block);
        }
    }

    RegisterVerifier(LinearScan allocator) {
        this.allocator = allocator;
        workList = new ArrayList<BlockBegin>(16);
        this.savedStates = new ArrayMap<List<Interval>>();

    }

    void verify(BlockBegin start) {
        // setup input registers (method arguments) for first block
        List<Interval> inputState = new ArrayList<Interval>();
        for (int i = 0; i < stateSize(); i++) {
            inputState.add(null);
        }
        CallingConvention args = compilation().frameMap().incomingArguments();
        for (int n = 0; n < args.length(); n++) {
            LIROperand opr = args.at(n);
            if (opr.isRegister()) {
                Interval interval = intervalAt(regNum(opr));

                if (interval.assignedReg() < stateSize()) {
                    inputState.set(interval.assignedReg(), interval);
                }
                if (interval.assignedRegHi() != LinearScan.getAnyreg() && interval.assignedRegHi() < stateSize()) {
                    inputState.set(interval.assignedRegHi(), interval);
                }
            }
        }

        setStateForBlock(start, inputState);
        addToWorkList(start);

        // main loop for verification
        do {
            BlockBegin block = workList.get(0);
            workList.remove(0);

            processBlock(block);
        } while (!workList.isEmpty());
    }

    void processBlock(BlockBegin block) {
        if (C1XOptions.TraceLinearScanLevel >= 2) {
            TTY.println();
            TTY.println("processBlock B%d", block.blockID());
        }

        // must copy state because it is modified
        List<Interval> inputState = copy(stateForBlock(block));

        if (C1XOptions.TraceLinearScanLevel >= 4) {
            TTY.println("Input-State of intervals:");
            TTY.print("    ");
            for (int i = 0; i < stateSize(); i++) {
                if (inputState.get(i) != null) {
                    TTY.print(" %4d", inputState.get(i).regNum());
                } else {
                    TTY.print("   lir().");
                }
            }
            TTY.cr();
            TTY.cr();
        }

        // process all operations of the block
        processOperations(block.lir(), inputState);

        // iterate all successors
        for (int i = 0; i < block.numberOfSux(); i++) {
            processSuccessor(block.suxAt(i), inputState);
        }
    }

    void processXhandler(ExceptionHandler xhandler, List<Interval> inputState) {
        Util.traceLinearScan(2, "processXhandler B%d", xhandler.entryBlock().blockID());

        // must copy state because it is modified
        inputState = copy(inputState);

        if (xhandler.entryCode() != null) {
            processOperations(xhandler.entryCode(), inputState);
        }
        processSuccessor(xhandler.entryBlock(), inputState);
    }

    void processSuccessor(BlockBegin block, List<Interval> inputState) {
        List<Interval> savedState = stateForBlock(block);

        if (savedState != null) {
            // this block was already processed before.
            // check if new inputState is consistent with savedState

            boolean savedStateCorrect = true;
            for (int i = 0; i < stateSize(); i++) {
                if (inputState.get(i) != savedState.get(i)) {
                    // current inputState and previous savedState assume a different
                    // interval in this register . assume that this register is invalid
                    if (savedState.get(i) != null) {
                        // invalidate old calculation only if it assumed that
                        // register was valid. when the register was already invalid,
                        // then the old calculation was correct.
                        savedStateCorrect = false;
                        savedState.set(i, null);

                        Util.traceLinearScan(4, "processSuccessor B%d: invalidating slot %d", block.blockID(), i);
                    }
                }
            }

            if (savedStateCorrect) {
                // already processed block with correct inputState
                Util.traceLinearScan(2, "processSuccessor B%d: previous visit already correct", block.blockID());
            } else {
                // must re-visit this block
                Util.traceLinearScan(2, "processSuccessor B%d: must re-visit because input state changed", block.blockID());
                addToWorkList(block);
            }

        } else {
            // block was not processed before, so set initial inputState
            Util.traceLinearScan(2, "processSuccessor B%d: initial visit", block.blockID());

            setStateForBlock(block, copy(inputState));
            addToWorkList(block);
        }
    }

    List<Interval> copy(List<Interval> inputState) {
        List<Interval> copyState = new ArrayList<Interval>(inputState.size());
        copyState.addAll(inputState);
        return copyState;
    }

    void statePut(List<Interval> inputState, int reg, Interval interval) {
        if (reg != LinearScan.getAnyreg() && reg < stateSize()) {
            if (interval != null) {
                Util.traceLinearScan(4, "        reg[%d] = %d", reg, interval.regNum());
            } else if (inputState.get(reg) != null) {
                Util.traceLinearScan(4, "        reg[%d] = null", reg);
            }

            inputState.set(reg, interval);
        }
    }

    boolean checkState(List<Interval> inputState, int reg, Interval interval) {
        if (reg != LinearScan.getAnyreg() && reg < stateSize()) {
            if (inputState.get(reg) != interval) {
                TTY.println("!! Error in register allocation: register %d does not contain interval %d", reg, interval.regNum());
                return true;
            }
        }
        return false;
    }

    void processOperations(LIRList ops, List<Interval> inputState) {
        // visit all instructions of the block
        LIRVisitState visitor = new LIRVisitState();
        boolean hasError = false;

        for (int i = 0; i < ops.length(); i++) {
            LIRInstruction op = ops.at(i);
            visitor.visit(op);

            if (C1XOptions.TraceLinearScanLevel >= 4) {
                op.printOn(TTY.out);
            }

            // check if input operands are correct
            int j;
            int n = visitor.oprCount(LIRVisitState.OperandMode.InputMode);
            for (j = 0; j < n; j++) {
                LIROperand opr = visitor.oprAt(LIRVisitState.OperandMode.InputMode, j);
                if (opr.isRegister() && allocator.isProcessedRegNum(regNum(opr))) {
                    Interval interval = intervalAt(regNum(opr));
                    if (op.id() != -1) {
                        interval = interval.splitChildAtOpId(op.id(), LIRVisitState.OperandMode.InputMode);
                    }

                    hasError |= checkState(inputState, interval.assignedReg(), interval.splitParent());
                    hasError |= checkState(inputState, interval.assignedRegHi(), interval.splitParent());

                    // When an operand is marked with isLastUse, then the fpu stack allocator
                    // removes the register from the fpu stack . the register contains no value
                    if (opr.isLastUse()) {
                        statePut(inputState, interval.assignedReg(), null);
                        statePut(inputState, interval.assignedRegHi(), null);
                    }
                }
            }

            // invalidate all caller save registers at calls
            if (visitor.hasCall()) {
                for (Register r : allocator.frameMap.callerSavedRegisters()) {
                    statePut(inputState, r.number, null);
                }
            }

            // process xhandler before output and temp operands
            List<ExceptionHandler> xhandlers = visitor.allXhandler();
            n = xhandlers.size();
            for (int k = 0; k < n; k++) {
                processXhandler(xhandlers.get(k), inputState);
            }

            // set temp operands (some operations use temp operands also as output operands, so can't set them null)
            n = visitor.oprCount(LIRVisitState.OperandMode.TempMode);
            for (j = 0; j < n; j++) {
                LIROperand opr = visitor.oprAt(LIRVisitState.OperandMode.TempMode, j);
                if (opr.isRegister() && allocator.isProcessedRegNum(regNum(opr))) {
                    Interval interval = intervalAt(regNum(opr));
                    if (op.id() != -1) {
                        interval = interval.splitChildAtOpId(op.id(), LIRVisitState.OperandMode.TempMode);
                    }

                    statePut(inputState, interval.assignedReg(), interval.splitParent());
                    statePut(inputState, interval.assignedRegHi(), interval.splitParent());
                }
            }

            // set output operands
            n = visitor.oprCount(LIRVisitState.OperandMode.OutputMode);
            for (j = 0; j < n; j++) {
                LIROperand opr = visitor.oprAt(LIRVisitState.OperandMode.OutputMode, j);
                if (opr.isRegister() && allocator.isProcessedRegNum(regNum(opr))) {
                    Interval interval = intervalAt(regNum(opr));
                    if (op.id() != -1) {
                        interval = interval.splitChildAtOpId(op.id(), LIRVisitState.OperandMode.OutputMode);
                    }

                    statePut(inputState, interval.assignedReg(), interval.splitParent());
                    statePut(inputState, interval.assignedRegHi(), interval.splitParent());
                }
            }
        }
        assert hasError == false : "Error in register allocation";
    }
}

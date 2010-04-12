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
import com.sun.c1x.debug.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.util.*;
import com.sun.cri.ci.*;

/**
 *
 * @author Thomas Wuerthinger
 */
final class RegisterVerifier {

    LinearScan allocator;
    List<BlockBegin> workList; // all blocks that must be processed
    ArrayMap<Interval[]> savedStates; // saved information of previous check

    // simplified access to methods of LinearScan
    C1XCompilation compilation() {
        return allocator.compilation;
    }

    Interval intervalAt(CiLocation operand) {
        return allocator.intervalFor(operand);
    }

    // currently, only registers are processed
    int stateSize() {
        return allocator.operands.maxRegisterNumber() + 1;
    }

    // accessors
    Interval[] stateForBlock(BlockBegin block) {
        return savedStates.get(block.blockID);
    }

    void setStateForBlock(BlockBegin block, Interval[] savedState) {
        savedStates.put(block.blockID, savedState);
    }

    void addToWorkList(BlockBegin block) {
        if (!workList.contains(block)) {
            workList.add(block);
        }
    }

    RegisterVerifier(LinearScan allocator) {
        this.allocator = allocator;
        workList = new ArrayList<BlockBegin>(16);
        this.savedStates = new ArrayMap<Interval[]>();

    }

    void verify(BlockBegin start) {
        // setup input registers (method arguments) for first block
        Interval[] inputState = new Interval[stateSize()];
        CallingConvention args = compilation().frameMap().incomingArguments();
        for (int n = 0; n < args.operands.length; n++) {
            CiValue operand = args.operands[n];
            if (operand.isRegister()) {
                CiLocation reg = operand.asLocation();
                Interval interval = intervalAt(reg);
                inputState[reg.asRegister().number] = interval;
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
            TTY.println("processBlock B%d", block.blockID);
        }

        // must copy state because it is modified
        Interval[] inputState = copy(stateForBlock(block));

        if (C1XOptions.TraceLinearScanLevel >= 4) {
            TTY.println("Input-State of intervals:");
            TTY.print("    ");
            for (int i = 0; i < stateSize(); i++) {
                if (inputState[i] != null) {
                    TTY.print(" %4d", inputState[i].operandNumber);
                } else {
                    TTY.print("   __");
                }
            }
            TTY.println();
            TTY.println();
        }

        // process all operations of the block
        processOperations(block.lir(), inputState);

        // iterate all successors
        for (BlockBegin succ : block.end().successors()) {
            processSuccessor(succ, inputState);
        }
    }

    void processXhandler(ExceptionHandler xhandler, Interval[] inputState) {
        if (C1XOptions.TraceLinearScanLevel >= 2) {
            TTY.println("processXhandler B%d", xhandler.entryBlock().blockID);
        }

        // must copy state because it is modified
        inputState = copy(inputState);

        if (xhandler.entryCode() != null) {
            processOperations(xhandler.entryCode(), inputState);
        }
        processSuccessor(xhandler.entryBlock(), inputState);
    }

    void processSuccessor(BlockBegin block, Interval[] inputState) {
        Interval[] savedState = stateForBlock(block);

        if (savedState != null) {
            // this block was already processed before.
            // check if new inputState is consistent with savedState

            boolean savedStateCorrect = true;
            for (int i = 0; i < stateSize(); i++) {
                if (inputState[i] != savedState[i]) {
                    // current inputState and previous savedState assume a different
                    // interval in this register . assume that this register is invalid
                    if (savedState[i] != null) {
                        // invalidate old calculation only if it assumed that
                        // register was valid. when the register was already invalid,
                        // then the old calculation was correct.
                        savedStateCorrect = false;
                        savedState[i] = null;

                        if (C1XOptions.TraceLinearScanLevel >= 4) {
                            TTY.println("processSuccessor B%d: invalidating slot %d", block.blockID, i);
                        }
                    }
                }
            }

            if (savedStateCorrect) {
                // already processed block with correct inputState
                if (C1XOptions.TraceLinearScanLevel >= 2) {
                    TTY.println("processSuccessor B%d: previous visit already correct", block.blockID);
                }
            } else {
                // must re-visit this block
                if (C1XOptions.TraceLinearScanLevel >= 2) {
                    TTY.println("processSuccessor B%d: must re-visit because input state changed", block.blockID);
                }
                addToWorkList(block);
            }

        } else {
            // block was not processed before, so set initial inputState
            if (C1XOptions.TraceLinearScanLevel >= 2) {
                TTY.println("processSuccessor B%d: initial visit", block.blockID);
            }

            setStateForBlock(block, copy(inputState));
            addToWorkList(block);
        }
    }

    Interval[] copy(Interval[] inputState) {
        return inputState.clone();
    }

    void statePut(Interval[] inputState, CiLocation location, Interval interval) {
        if (location != null && location.isRegister()) {
            CiRegister reg = location.asRegister();
            int regNum = reg.number;
            if (interval != null) {
                if (C1XOptions.TraceLinearScanLevel >= 4) {
                    TTY.println("        %s = %s", reg, interval.operand);
                }
            } else if (inputState[regNum] != null) {
                if (C1XOptions.TraceLinearScanLevel >= 4) {
                    TTY.println("        %s = null", reg);
                }
            }

            inputState[regNum] = interval;
        }
    }

    boolean checkState(Interval[] inputState, CiLocation reg, Interval interval) {
        if (reg != null && reg.isRegister()) {
            if (inputState[reg.asRegister().number] != interval) {
                throw new CiBailout("!! Error in register allocation: register " + reg + " does not contain interval " + interval.operand + " but interval " + inputState[reg.asRegister().number]);
            }
        }
        return true;
    }

    void processOperations(LIRList ops, Interval[] inputState) {
        // visit all instructions of the block
        for (int i = 0; i < ops.length(); i++) {
            LIRInstruction op = ops.at(i);

            if (C1XOptions.TraceLinearScanLevel >= 4) {
                op.printOn(TTY.out());
            }

            // check if input operands are correct
            int n = op.operandCount(LIRInstruction.OperandMode.InputMode);
            for (int j = 0; j < n; j++) {
                CiLocation operand = op.operandAt(LIRInstruction.OperandMode.InputMode, j);
                if (allocator.isProcessed(operand)) {
                    Interval interval = intervalAt(operand);
                    if (op.id != -1) {
                        interval = interval.getSplitChildAtOpId(op.id, LIRInstruction.OperandMode.InputMode, allocator);
                    }

                    assert checkState(inputState, interval.location(), interval.splitParent());
                }
            }

            // invalidate all caller save registers at calls
            if (op.hasCall()) {
                for (CiRegister r : allocator.compilation.target.allocationSpec.callerSaveRegisters) {
                    statePut(inputState, r.asValue(), null);
                }
            }

            // process xhandler before output and temp operands
            List<ExceptionHandler> xhandlers = op.exceptionEdges();
            n = xhandlers.size();
            for (int k = 0; k < n; k++) {
                processXhandler(xhandlers.get(k), inputState);
            }

            // set temp operands (some operations use temp operands also as output operands, so can't set them null)
            n = op.operandCount(LIRInstruction.OperandMode.TempMode);
            for (int j = 0; j < n; j++) {
                CiValue operand = op.operandAt(LIRInstruction.OperandMode.TempMode, j);
                if (allocator.isProcessed(operand.asLocation())) {
                    Interval interval = intervalAt(operand.asLocation());
                    if (op.id != -1) {
                        interval = interval.getSplitChildAtOpId(op.id, LIRInstruction.OperandMode.TempMode, allocator);
                    }

                    statePut(inputState, interval.location(), interval.splitParent());
                }
            }

            // set output operands
            n = op.operandCount(LIRInstruction.OperandMode.OutputMode);
            for (int j = 0; j < n; j++) {
                CiValue operand = op.operandAt(LIRInstruction.OperandMode.OutputMode, j);
                if (allocator.isProcessed(operand.asLocation())) {
                    Interval interval = intervalAt(operand.asLocation());
                    if (op.id != -1) {
                        interval = interval.getSplitChildAtOpId(op.id, LIRInstruction.OperandMode.OutputMode, allocator);
                    }

                    statePut(inputState, interval.location(), interval.splitParent());
                }
            }
        }
    }
}

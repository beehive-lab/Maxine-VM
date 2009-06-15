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

import com.sun.max.profile.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.allocate.linearscan.*;
import com.sun.max.vm.compiler.ir.IrBlock.*;

/**
 * Remove redundant moves where the destination and source location are the same.
 *
 * @author Thomas Wuerthinger
 */
public class RemoveRedundantMoves extends AlgorithmPart {
    private final Metrics.Counter _movedAssignmentsCounter = createCounter("Moved assignments");


    public RemoveRedundantMoves() {
        super(13);
    }

    @Override
    protected void doit() {

        for (EirBlock block : generation().eirBlocks()) {
            int index = 0;
            for (EirInstruction<?, ?> instruction : block.instructions()) {
                if (instruction instanceof EirAssignment) {
                    final EirAssignment assignment = (EirAssignment) instruction;
                    if (assignment.sourceOperand().location() == assignment.destinationOperand().location()) {
                        // This assignment is redundant
                        block.setInstruction(index, new EirFiller(block));
                    }
                }
                index++;
            }
        }

        for (EirBlock block : generation().eirBlocks()) {
            optimizeBlock(block);
        }
    }

    private void optimizeBlock(EirBlock block) {

        if (block.role() == Role.EXCEPTION_DISPATCHER) {
            return;
        }

        if (block.predecessors().length() <= 1) {
            return;
        }

        for (EirBlock pred : block.predecessors()) {
            final EirInstruction lastInstruction = pred.instructions().last();
            if (lastInstruction instanceof EirJump) {
                final EirJump jump = (EirJump) lastInstruction;
                if (jump.target() != block) {
                    assert false : "Should not reach here!";
                    return;
                }
            } else {
                return;
            }
        }

        // Skip the last instruction as this must be a jump anyway
        int offset = 1;
        while (true) {

            offset++;

            EirAssignment lastAssignment = null;

            boolean ok = true;


            boolean allFiller = true;
            for (EirBlock pred : block.predecessors()) {

                final int length = pred.instructions().length();
                final int curIndex = length - offset;
                if (curIndex < 0) {
                    ok = false;
                    break;
                }

                final EirInstruction cur = pred.instructions().get(curIndex);
                if (!(cur instanceof EirFiller)) {
                    allFiller = false;
                }
            }

            if (allFiller && ok) {
                continue;
            }

            for (EirBlock pred : block.predecessors()) {

                final int length = pred.instructions().length();
                final int curIndex = length - offset;
                if (curIndex < 0) {
                    ok = false;
                    break;
                }

                final EirInstruction cur = pred.instructions().get(curIndex);
                if (cur instanceof EirAssignment) {
                    final EirAssignment curAssignment = (EirAssignment) cur;
                    if (lastAssignment == null) {
                        lastAssignment = curAssignment;
                    } else if (!lastAssignment.sourceOperand().location().equals(curAssignment.sourceOperand().location()) ||
                                    !lastAssignment.destinationOperand().location().equals(curAssignment.destinationOperand().location())) {

                        ok = false;
                        break;
                    }

                } else {
                    ok = false;
                    break;
                }
            }

            if (!ok) {
                break;
            }
        }

        if (LinearScanRegisterAllocator.DETAILED_COUNTING) {
            _movedAssignmentsCounter.accumulate((offset - 2) * block.predecessors().length());
        }

        for (int i = 2; i < offset; i++) {

            final EirBlock firstPred = block.predecessors().first();
            final EirInstruction currentInstruction = firstPred.instructions().get(firstPred.instructions().length() - i);

            if (currentInstruction instanceof EirFiller) {
                continue;
            }

            assert currentInstruction instanceof EirAssignment;
            block.insertInstruction(0, currentInstruction);

            for (EirBlock pred : block.predecessors()) {
                pred.setInstruction(pred.instructions().length() - i, new EirFiller(pred));
            }
        }
    }
}

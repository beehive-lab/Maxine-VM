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
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.allocate.linearscan.*;
import com.sun.max.vm.compiler.ir.IrBlock.*;

/**
 * Computes an optimized linear scan block order.
 *
 * @author Thomas Wuerthinger
 */
public class ComputeBlockOrder extends AlgorithmPart {

    public ComputeBlockOrder() {
        super(3);
    }

    private boolean traceBlocks() {
        final IndentWriter writer = IndentWriter.traceStreamWriter();
        for (EirBlock block : generation().eirBlocks()) {
            block.printTo(writer);
        }
        writer.flush();
        return false;
    }

    @Override
    protected void doit() {

        final Sequence<EirBlock> eirBlocks = generation().eirBlocks();
        final AppendableSequence<EirBlock> result = new ArrayListSequence<EirBlock>(eirBlocks.length());

        final VariableSequence<EirBlock> workList = new ArrayListSequence<EirBlock>(eirBlocks.length());
        final EirBlock startBlock = generation().eirBlocks().first();
        assert startBlock.predecessors().length() == 0 : "must be start block";

        final VariableMapping<EirBlock, Integer> leftOverPredecessors = new ChainedHashMapping<EirBlock, Integer>();
        for (EirBlock block : eirBlocks) {
            leftOverPredecessors.put(block, block.predecessors().length());
        }

        final PoolSet<EirBlock> blockPoolSet = PoolSet.noneOf(generation().eirBlockPool());
        detectBackedges(leftOverPredecessors, blockPoolSet, startBlock);

        // Special handling if epilogue block is unconnected
        EirBlock epilogueBlock = generation().epilogueBlock();
        if (epilogueBlock != null && epilogueBlock.predecessors().length() == 0) {
            blockPoolSet.add(epilogueBlock);
        }
        assert blockPoolSet.length() == blockPoolSet.pool().length() || traceBlocks() : "every block must be visited";

        blockPoolSet.clear();
        blockPoolSet.add(startBlock);
        workList.append(startBlock);

        while (!workList.isEmpty()) {
            final EirBlock cur = workList.removeFirst();
            assert leftOverPredecessors.get(cur) == 0 : "all predecessors must be already processed";

            // Add cur to ordered result list
            result.append(cur);

            for (EirBlock succ : cur.allUniqueSuccessors()) {
                assert assertBlockStructure(cur, succ);

                if (!blockPoolSet.contains(succ)) {
                    int predecessorCount = leftOverPredecessors.get(succ);
                    assert predecessorCount > 0 : "must have at least one predecessor left";
                    predecessorCount--;
                    leftOverPredecessors.put(succ, predecessorCount);

                    if (predecessorCount == 0) {
                        // All predecessors already process => put block into worklist
                        insertInWorkList(workList, succ);
                        blockPoolSet.add(succ);
                    }
                } else {
                    assert leftOverPredecessors.get(succ) == 0 : "must not have any predecessor left";
                }
            }

        }

        // Special handling if epilogue block is unconnected
        epilogueBlock = generation().epilogueBlock();
        if (epilogueBlock != null && epilogueBlock.predecessors().length() == 0) {
            result.append(epilogueBlock);
        }

        assert result.length() == generation().eirBlocks().length();

        data().setLinearScanOrder(result);
    }

    private void insertInWorkList(VariableSequence<EirBlock> workList, EirBlock succ) {
        final int nesting = succ.loopNestingDepth();

        for (int i = 0; i < workList.length(); i++) {

            final int otherNesting = workList.get(i).loopNestingDepth();
            if (nesting > otherNesting || (nesting == otherNesting && workList.get(i).role() == Role.EXCEPTION_DISPATCHER)) {
                workList.insert(i, succ);
                return;
            }
        }

        workList.append(succ);
    }

    private boolean assertBlockStructure(EirBlock cur, final EirBlock succ) {

        int successorCount = 0;
        boolean foundSucc = false;
        for (EirBlock s : cur.allUniqueSuccessors()) {
            if (succ == s) {
                foundSucc = true;
            }
            successorCount++;
        }

        boolean foundPred = false;
        for (EirBlock block : succ.predecessors()) {
            if (block == cur) {
                foundPred = true;
            }
        }

        assert foundPred;
        assert foundSucc;
        assert successorCount >= 1;
        return true;
    }

    private void detectBackedges(final VariableMapping<EirBlock, Integer> leftOverPredecessors, final PoolSet<EirBlock> visited, EirBlock startBlock) {

        final VariableSequence<EirBlock> stack = new ArrayListSequence<EirBlock>(16);
        stack.append(startBlock);
        visited.add(startBlock);

        while (!stack.isEmpty()) {
            final EirBlock block = stack.removeLast();

            for (EirBlock succ : block.allUniqueSuccessors()) {

                assert PoolSet.allOf(visited.pool()).contains(succ);

                if (visited.contains(succ)) {
                    // Back edge found!
                    int value = leftOverPredecessors.get(succ);
                    value--;
                    assert value > 0 : "must have at least one left over predecessor";
                    leftOverPredecessors.put(succ, value);
                } else {
                    visited.add(succ);
                    stack.append(succ);
                }
            }
        }

    }

    @Override
    protected boolean assertPostconditions() {
        final PoolSet<EirVariable> definedVariables = PoolSet.noneOf(generation().variablePool());

        for (EirBlock block : data().linearScanOrder()) {
            for (EirInstruction instruction : block.instructions()) {
                instruction.visitOperands(new EirOperand.Procedure() {

                    public void run(EirOperand operand) {

                        final EirVariable variable = operand.eirValue().asVariable();
                        if (variable != null) {
                            if (operand.effect() == EirOperand.Effect.DEFINITION) {
                                definedVariables.add(variable);
                            } else if (operand.effect() == EirOperand.Effect.UPDATE || operand.effect() == EirOperand.Effect.USE) {
                                assert definedVariables.contains(variable) : "must be defined before use";
                            } else {
                                assert false : "unknown effect on operand";
                            }
                        }
                    }
                });
            }
        }

        return super.assertPostconditions();
    }
}

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
package com.sun.c1x.opt;

import com.sun.c1x.*;
import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.value.*;

/**
 * This class implements block merging, which combines adjacent basic blocks into a larger
 * basic block, and block skipping, which removes empty blocks that end in a Goto with
 * their target.
 *
 * @author Ben L. Titzer
 */
public class BlockMerger implements BlockClosure {

    private final BlockBegin startBlock;

    public BlockMerger(IR ir) {
        startBlock = ir.startBlock;
        startBlock.iteratePreOrder(this);
    }

    public void apply(BlockBegin block) {
        while (tryMerge(block)) {
            // keep trying to merge the block with its successor
        }
    }

    private boolean tryMerge(BlockBegin block) {
        BlockEnd oldEnd = block.end();
        BlockEnd newEnd = oldEnd;
        if (oldEnd instanceof Goto && block != startBlock) {
            BlockBegin sux = oldEnd.defaultSuccessor();

            assert oldEnd.successors().size() == 1 : "end must have exactly one successor";
            assert !sux.isExceptionEntry() : "should not have Goto to exception entry";

            if (!oldEnd.isSafepoint()) {
                if (sux.numberOfPreds() == 1) {
                    // the successor has only one predecessor, merge it into this block
                    if (C1XOptions.DetailedAsserts) {
                        verifyStates(block, sux);
                    }

                    // find instruction before oldEnd & append first instruction of sux block
                    Instruction prev = oldEnd.prev(block);
                    Instruction next = sux.next();
                    assert !(prev instanceof BlockEnd) : "must not be a BlockEnd";
                    prev.setNext(next, next.bci());
                    BlockUtil.disconnectFromGraph(sux);
                    newEnd = sux.end();
                    block.setEnd(newEnd);
                    // add exception handlers of deleted block, if any
                    for (BlockBegin xhandler : sux.exceptionHandlerBlocks()) {
                        block.addExceptionHandler(xhandler);

                        // also substitute predecessor of exception handler
                        assert xhandler.isPredecessor(sux) : "missing predecessor";
                        xhandler.removePredecessor(sux);
                        if (!xhandler.isPredecessor(block)) {
                            xhandler.addPredecessor(block);
                        }
                    }

                    C1XMetrics.BlocksMerged++;
                } else if (C1XOptions.DoBlockSkipping && block.next() == oldEnd) {
                    // the successor has multiple predecessors, but this block is empty
                    final ValueStack oldState = oldEnd.stateAfter();
                    assert sux.stateBefore().scope() == oldState.scope();
                    if (block.stateBefore().hasPhisFor(block)) {
                        // can't skip a block that has phis
                        return false;
                    }
                    for (BlockBegin pred : block.predecessors()) {
                        final ValueStack predState = pred.end().stateAfter();
                        if (predState.scope() != oldState.scope() || predState.stackSize() != oldState.stackSize()) {
                            // scopes would not match after skipping this block
                            // XXX: if phi's were smarter about scopes, this would not be necessary
                            return false;
                        }
                        if (sux.stateBefore().hasPhisFor(sux)) {
                            Iterable<Phi> suxPhis = sux.stateBefore().allPhis(sux);
                            for (Phi phi : suxPhis) {
                                if (phi.operandIn(block.end().stateAfter()) != phi.operandIn(pred.end().stateAfter())) {
                                    return false;
                                }
                            }
                        }
                    }
                    sux.removePredecessor(block); // remove this block from the successor
                    for (BlockBegin pred : block.predecessors()) {
                        // substitute the new successor for this block in each predecessor
                        pred.end().substituteSuccessor(block, sux);
                        // and add each predecessor to the successor
                        sux.addPredecessor(pred);
                    }
                    // this block is now disconnected; remove all its incoming and outgoing edges
                    block.predecessors().clear();
                    oldEnd.successors().clear();
                    C1XMetrics.BlocksSkipped++;
                }
            }
        }
        return newEnd != oldEnd;
    }

    private void verifyStates(BlockBegin block, BlockBegin sux) {
        // verify that state at the end of block and at the beginning of sux are equal
        // no phi functions must be present at beginning of sux
        ValueStack suxState = sux.stateBefore();
        ValueStack endState = block.end().stateAfter();
        while (endState.scope() != suxState.scope()) {
            // match up inlining level
            endState = endState.popScope();
        }
        assert endState.stackSize() == suxState.stackSize() : "stack not equal";
        assert endState.localsSize() == suxState.localsSize() : "locals not equal";

        for (int i = 0; i < endState.localsSize(); i++) {
            assert endState.localAt(i) == suxState.localAt(i);
        }
        for (int i = 0; i < endState.stackSize(); i++) {
            assert endState.stackAt(i) == suxState.stackAt(i);
        }
    }
}

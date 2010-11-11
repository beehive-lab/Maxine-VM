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
import com.sun.c1x.value.FrameState.*;

/**
 * This class implements block merging, which combines adjacent basic blocks into a larger
 * basic block, and block skipping, which removes empty blocks that end in a Goto with
 * their target.
 *
 * @author Ben L. Titzer
 */
public class BlockMerger implements BlockClosure {

    private final BlockBegin startBlock;
    private final IR ir;

    public BlockMerger(IR ir) {
        this.ir = ir;
        startBlock = ir.startBlock;
        startBlock.iteratePreOrder(this);
    }

    public void apply(BlockBegin block) {
        while (block.end() instanceof Goto && block != startBlock) {
            BlockEnd end = block.end();
            BlockBegin sux = end.defaultSuccessor();

            assert end.successors().size() == 1 : "end must have exactly one successor";
            assert !sux.isExceptionEntry() : "should not have Goto to exception entry";

            if (!end.isSafepoint()) {
                if (sux.numberOfPreds() == 1) {
                    // the successor has only one predecessor, merge it into this block
                    mergeBlocks(block, sux, end);
                    C1XMetrics.BlocksMerged++;
                    continue;
                } else if (C1XOptions.OptBlockSkipping && block.next() == end && !block.isExceptionEntry()) {
                    // the successor has multiple predecessors, but this block is empty
                    skipBlock(block, sux, end);
                    break;
                }
            }
            break;
        }
    }

    private void skipBlock(BlockBegin block, final BlockBegin sux, BlockEnd oldEnd) {
        final FrameState oldState = oldEnd.stateAfter();
        assert sux.stateBefore().scope() == oldState.scope();
        boolean hasAtLeastOnePhi = block.stateBefore().forEachPhi(block, new PhiProcedure() {
            public boolean doPhi(Phi phi) {
                return false;
            }
        });

        if (hasAtLeastOnePhi) {
            // can't skip a block that has phis
            return;
        }
        for (final BlockBegin pred : block.predecessors()) {
            final FrameState predState = pred.end().stateAfter();
            if (predState.scope() != oldState.scope() || predState.stackSize() != oldState.stackSize()) {
                // scopes would not match after skipping this block
                // XXX: if phi's were smarter about scopes, this would not be necessary
                return;
            }
            boolean atLeastOneSuxPhiMergesFromAnotherBlock = !sux.stateBefore().forEachPhi(sux, new PhiProcedure() {
                public boolean doPhi(Phi phi) {
                    if (phi.inputIn(sux.stateBefore()) != phi.inputIn(pred.end().stateAfter())) {
                        return false;
                    }
                    return true;
                }
            });

            if (atLeastOneSuxPhiMergesFromAnotherBlock) {
                return;
            }
        }
        ir.replaceBlock(block, sux);
        C1XMetrics.BlocksSkipped++;
    }

    private void mergeBlocks(BlockBegin block, BlockBegin sux, BlockEnd oldEnd) {
        BlockEnd newEnd;
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
    }
}

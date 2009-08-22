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
import com.sun.c1x.ci.*;
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

    public BlockMerger(IR ir) {
        ir.startBlock.iteratePreOrder(this);
    }

    public void apply(BlockBegin block) {
        while (tryMerge(block)) {
            // keep trying to merge the block with its successor
        }
    }

    private boolean tryMerge(BlockBegin block) {
        BlockEnd oldEnd = block.end();
        BlockEnd newEnd = oldEnd;
        if (oldEnd instanceof Goto) {
            BlockBegin sux = oldEnd.defaultSuccessor();

            assert oldEnd.successors().size() == 1 : "end must have exactly one successor";
            assert !sux.isExceptionEntry() : "should not have Goto to exception entry";

            if (!oldEnd.isSafepoint() && !sux.isEntryBlock()) {
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
                    // go through all phis in the successor block
                    assert sux.stateBefore().scope() == oldEnd.stateAfter().scope();
                    if (block.stateBefore().hasPhisFor(block)) {
                        // can't skip a block that has phis
                        return false;
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
        if (newEnd instanceof If) {
            reduceNestedIfOp(block, (If) newEnd);
        }
        return newEnd != oldEnd;
    }

    private void reduceNestedIfOp(BlockBegin block, If newEnd) {
        IfOp ifOp = asIfOp(newEnd.x());
        CiConstant k1 = newEnd.y().asConstant();
        Condition cond = newEnd.condition();

        if (k1 == null || ifOp == null) {
            ifOp = asIfOp(newEnd.y());
            k1 = newEnd.x().asConstant();
            cond = cond.mirror();
        }
        if (k1 != null && ifOp != null) {
            // this matches:
            // if (cond, ifOp(a, b, c, d), k1, tsux, fsux)   -or-
            // if (cond, k1, ifOp(a, b, c, d), tsux, fsux)

            if (ifOp.trueValue().isConstant() && ifOp.falseValue().isConstant()) {
                // this matches:
                // if (cond, ifOp(a, b, kT, kF), k1, tsux, fsux)  -or-
                // if (cond, k1, ifOp(a, b, kT, kF), tsux, fsux)
                CiConstant kT = ifOp.trueValue().asConstant();
                CiConstant kF = ifOp.falseValue().asConstant();
                // Find the instruction before newEnd, starting with ifOp.
                // When newEnd and ifOp are not in the same block, prev
                // becomes null. In such (rare) cases it is not
                // profitable to perform the optimization.
                Instruction prev = ifOp;
                while (prev != null && prev.next() != newEnd) {
                    prev = prev.next();
                }

                if (prev != null) {
                    BlockBegin tsux = newEnd.trueSuccessor();
                    BlockBegin fsux = newEnd.falseSuccessor();

                    // see where we would go in the true and false cases
                    Boolean tres = cond.foldCondition(kT, k1);
                    Boolean fres = cond.foldCondition(kF, k1);

                    if (tres == null || fres == null) {
                        // could not fold the comparison for some reason
                        return;
                    }

                    BlockBegin tblock = tres ? tsux : fsux;
                    BlockBegin fblock = fres ? fsux : tsux;

                    if (tblock != fblock && !newEnd.isSafepoint()) {
                        // remove the IfOp and move its comparison into the if at the end
                        If newIf = new If(ifOp.x(), ifOp.condition(), false, ifOp.y(), tblock, fblock, null, newEnd.isSafepoint());
                        newIf.setStateAfter(newEnd.stateAfter().copy());

                        assert prev.next() == newEnd : "must be guaranteed by above search";
                        prev.setNext(newIf, newEnd.bci());
                        block.setEnd(newIf);
                        C1XMetrics.NestedIfOpsRemoved++;
                    }
                }
            }
        }
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

    private IfOp asIfOp(Instruction x) {
        return x instanceof IfOp ? (IfOp) x : null;
    }
}

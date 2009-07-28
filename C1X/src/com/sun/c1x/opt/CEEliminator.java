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

import com.sun.c1x.graph.IR;
import com.sun.c1x.graph.BlockUtil;
import com.sun.c1x.ir.*;
import com.sun.c1x.value.ValueType;
import com.sun.c1x.value.ValueStack;
import com.sun.c1x.C1XMetrics;

/**
 * This class implements conditional-expression elimination, which replaces some
 * branching constructs with conditional moves.
 *
 * @author Ben L. Titzer
 */
public class CEEliminator implements BlockClosure {

    final IR ir;
    private boolean hasSubstitution;

    public CEEliminator(IR ir) {
        this.ir = ir;
        ir.startBlock.iteratePreOrder(this);
        if (hasSubstitution) {
            new SubstitutionResolver(ir.startBlock);
        }
    }

    void adjustExceptionEdges(BlockBegin block, BlockBegin sux) {
        int e = sux.numberOfExceptionHandlers();
        for (int i = 0; i < e; i++) {
            BlockBegin xhandler = sux.exceptionHandlerAt(i);
            block.addExceptionHandler(xhandler);

            assert xhandler.isPredecessor(sux) : "missing predecessor";
            if (sux.numberOfPreds() == 0) {
                // sux is disconnected from graph so disconnect from exception handlers
                xhandler.removePredecessor(sux);
            }
            if (!xhandler.isPredecessor(block)) {
                xhandler.addPredecessor(block);
            }
        }
    }

    public void apply(BlockBegin block) {
        // 1) find conditional expression
        // check if block ends with an If
        if (!(block.end() instanceof If)) {
            return;
        }
        If if_ = (If) block.end();

        // check if If works on int or object types
        // (we cannot handle If's working on long, float or doubles yet,
        // since IfOp doesn't support them - these If's show up if cmp
        // operations followed by If's are eliminated)
        ValueType ifType = if_.x().type();
        if (!ifType.isInt() && !ifType.isObject()) return;

        BlockBegin tBlock = if_.trueSuccessor();
        BlockBegin fBlock = if_.falseSuccessor();
        Instruction tCur = tBlock.next();
        Instruction fCur = fBlock.next();

        // one Constant may be present between BlockBegin and BlockEnd
        Instruction tConst = null;
        Instruction fConst = null;
        if (tCur instanceof Constant) {
            tConst = tCur;
            tCur = tCur.next();
        }
        if (fCur instanceof Constant) {
            fConst = fCur;
            fCur = fCur.next();
        }

        // check if both branches end with a goto
        if (!(tCur instanceof Goto) || !(fCur instanceof Goto)) {
            return;
        }
        Goto tGoto = (Goto) tCur;
        Goto fGoto = (Goto) fCur;

        // check if both gotos merge into the same block
        BlockBegin sux = tGoto.defaultSuccessor();
        if (sux != fGoto.defaultSuccessor()) return;

        // check if at least one word was pushed on suxState
        ValueStack suxState = sux.state();
        if (suxState.stackSize() <= if_.state().stackSize()) return;

        // check if phi function is present at end of successor stack and that
        // only this phi was pushed on the stack
        Instruction suxPhi = suxState.stackAt(if_.state().stackSize());
        if (suxPhi == null || !(suxPhi instanceof Phi) || ((Phi) suxPhi).block() != sux) return;
        if (suxPhi.type().size() != suxState.stackSize() - if_.state().stackSize()) return;

        // get the values that were pushed in the true- and false-branch
        Instruction tValue = tGoto.state().stackAt(if_.state().stackSize());
        Instruction fValue = fGoto.state().stackAt(if_.state().stackSize());

        assert tValue.type().base() == fValue.type().base() : "incompatible types";

        if (tValue.type().isFloat() || tValue.type().isDouble()) {
            // backend does not support conditional moves on floats
            return;
        }

        // check that successor has no other phi functions but suxPhi
        // this can happen when tBlock or fBlock contained additional stores to local variables
        // that are no longer represented by explicit instructions

        for (Instruction i : sux.state().allPhis()) {
            if (i != suxPhi) {
                return;
            }
        }
        // true and false blocks can't have phis
        for (Instruction i : tBlock.state().allPhis()) {
            return;
        }
        for (Instruction i : fBlock.state().allPhis()) {
            return;
        }

        // 2) substitute conditional expression
        //    with an IfOp followed by a Goto
        // cut if_ away and get node before
        Instruction ifPrev = if_.prev(block);
        int bci = if_.bci();

        // append constants of true- and false-block if necessary
        // clone constants because original block must not be destroyed
        assert (tValue != fConst && fValue != tConst) || tConst == fConst : "mismatch";
        if (tValue == tConst) {
            tValue = new Constant(tConst.type().asConstant());
            ifPrev = ifPrev.setNext(tValue, bci);
        }
        if (fValue == fConst) {
            fValue = new Constant(fConst.type().asConstant());
            ifPrev = ifPrev.setNext(fValue, bci);
        }

        // it is very unlikely that the condition can be statically decided
        // (this was checked previously by the Canonicalizer), so always
        // append IfOp
        Instruction result = new IfOp(if_.x(), if_.condition(), if_.y(), tValue, fValue);
        ifPrev = ifPrev.setNext(result, bci);

        // append Goto to successor
        ValueStack state_before = if_.isSafepoint() ? if_.stateBefore() : null;
        Goto goto_ = new Goto(sux, state_before, if_.isSafepoint() || tGoto.isSafepoint() || fGoto.isSafepoint());

        // prepare state for Goto
        ValueStack goto_state = if_.state();
        while (suxState.scope() != goto_state.scope()) {
            goto_state = goto_state.popScope();
            assert goto_state != null : "states do not match up";
        }
        goto_state = goto_state.copy();
        goto_state.push(result.type().basicType, result);
        assert goto_state.isSameAcrossScopes(suxState) : "states must match now";
        goto_.setState(goto_state);

        // Steal the bci for the goto from the sux
        ifPrev = ifPrev.setNext(goto_, sux.bci());

        // Adjust control flow graph
        BlockUtil.disconnectEdge(block, tBlock);
        BlockUtil.disconnectEdge(block, fBlock);
        if (tBlock.numberOfPreds() == 0) {
            BlockUtil.disconnectEdge(tBlock, sux);
        }
        adjustExceptionEdges(block, tBlock);
        if (fBlock.numberOfPreds() == 0) {
            BlockUtil.disconnectEdge(fBlock, sux);
        }
        adjustExceptionEdges(block, fBlock);

        // update block end
        block.setEnd(goto_);

        // substitute the phi if possible
        Phi sux_phi2 = (Phi) suxPhi;
        if (sux_phi2.operandCount() == 1) {
            assert sux_phi2.operandAt(0) == result : "screwed up phi";
            suxPhi.setSubst(result);
            hasSubstitution = true;
        }

        // 3) successfully eliminated a conditional expression
        C1XMetrics.ConditionalEliminations++;
    }


}

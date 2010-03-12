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
package com.sun.c1x.ir;

import com.sun.c1x.ci.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;

/**
 * The {@code If} instruction represents a branch that can go one of two directions
 * depending on the outcome of a comparison.
 *
 * @author Ben L. Titzer
 */
public final class If extends BlockEnd {

    Value x;
    Value y;
    Condition condition;

    /**
     * Constructs a new If instruction.
     * @param x the instruction producing the first input to the instruction
     * @param cond the condition (comparison operation)
     * @param unorderedIsTrue {@code true} if unordered is treated as true (floating point operations)
     * @param y the instruction that produces the second input to this instruction
     * @param trueSucc the block representing the true successor
     * @param falseSucc the block representing the false successor
     * @param stateBefore the state before the branch
     * @param isSafepoint {@code true} if this branch should be considered a safepoint
     */
    public If(Value x, Condition cond, boolean unorderedIsTrue, Value y,
              BlockBegin trueSucc, BlockBegin falseSucc, FrameState stateBefore, boolean isSafepoint) {
        super(CiKind.Illegal, stateBefore, isSafepoint);
        this.x = x;
        this.y = y;
        condition = cond;
        assert Util.equalKinds(x, y);
        initFlag(Flag.UnorderedIsTrue, unorderedIsTrue);
        successors.add(trueSucc);
        successors.add(falseSucc);
    }

    /**
     * Gets the instruction that produces the first input to this comparison.
     * @return the instruction producing the first input
     */
    public Value x() {
        return x;
    }

    /**
     * Gets the instruction that produces the second input to this comparison.
     * @return the instruction producing the second input
     */
    public Value y() {
        return y;
    }

    /**
     * Gets the condition (comparison operation) for this instruction.
     * @return the condition
     */
    public Condition condition() {
        return condition;
    }

    /**
     * Checks whether unordered inputs mean true or false.
     * @return {@code true} if unordered inputs produce true
     */
    public boolean unorderedIsTrue() {
        return checkFlag(Flag.UnorderedIsTrue);
    }

    /**
     * Gets the block corresponding to the true successor.
     * @return the true successor
     */
    public BlockBegin trueSuccessor() {
        return successors.get(0);
    }

    /**
     * Gets the block corresponding to the false successor.
     * @return the false successor
     */
    public BlockBegin falseSuccessor() {
        return successors.get(1);
    }

    /**
     * Gets the block corresponding to the specified outcome of the branch.
     * @param istrue {@code true} if the true successor is requested, {@code false} otherwise
     * @return the corresponding successor
     */
    public BlockBegin successor(boolean istrue) {
        return successors.get(istrue ? 0 : 1);
    }

    /**
     * Gets the successor of this instruction for the unordered case.
     * @return the successor for unordered inputs
     */
    public BlockBegin unorderedSuccessor() {
        return successor(unorderedIsTrue());
    }

    /**
     * Swaps the operands to this if and reverses the condition (e.g. > goes to <=).
     * @see Condition#mirror()
     */
    public void swapOperands() {
        condition = condition.mirror();
        Value t = x;
        x = y;
        y = t;
    }

    /**
     * Swaps the successor blocks to this if and negates the condition (e.g. == goes to !=)
     * @see Condition#negate()
     */
    public void swapSuccessors() {
        setFlag(Flag.UnorderedIsTrue, !unorderedIsTrue());
        condition = condition.negate();
        BlockBegin t = successors.get(0);
        BlockBegin f = successors.get(1);
        successors.set(0, f);
        successors.set(1, t);
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply
     */
    @Override
    public void inputValuesDo(ValueClosure closure) {
        x = closure.apply(x);
        y = closure.apply(y);
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(ValueVisitor v) {
        v.visitIf(this);
    }
}

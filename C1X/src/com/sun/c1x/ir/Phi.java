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
import com.sun.c1x.value.*;

/**
 * The {@code Phi} instruction represents the merging of dataflow
 * in the instruction graph. It refers to a join block and a variable.
 *
 * @author Ben L. Titzer
 */
public final class Phi extends Value {

    private final BlockBegin block;
    private final int index;

    /**
     * Create a new Phi for the specified join block and local variable (or operand stack) slot.
     * @param kind the type of the variable
     * @param block the join point
     * @param index the index into the stack (if < 0) or local variables
     */
    public Phi(CiKind kind, BlockBegin block, int index) {
        super(kind);
        this.block = block;
        this.index = index;
    }

    /**
     * Get the join block for this phi.
     * @return the join block of this phi
     */
    public BlockBegin block() {
        return block;
    }

    /**
     * Check whether this phi corresponds to a local variable.
     * @return {@code true} if this phi refers to a local variable
     */
    public boolean isLocal() {
        return index >= 0;
    }

    /**
     * Check whether this phi corresponds to a stack location.
     * @return {@code true} if this phi refers to a stack location
     */
    public boolean isOnStack() {
        return index < 0;
    }

    /**
     * Get the local index of this phi.
     * @return the local index
     */
    public int localIndex() {
        assert isLocal();
        return index;
    }

    /**
     * Get the stack index of this phi.
     * @return the stack index of this phi
     */
    public int stackIndex() {
        assert isOnStack();
        return -(index + 1);
    }

    /**
     * Get the instruction that produces the value associated with the i'th predecessor
     * of the join block.
     * @param i the index of the predecessor
     * @return the instruction that produced the value in the i'th predecessor
     */
    public Value operandAt(int i) {
        FrameState state;
        if (block.isExceptionEntry()) {
            state = block.exceptionHandlerStates().get(i);
        } else {
            state = block.predecessors().get(i).end().stateAfter();
        }
        return operandIn(state);
    }

    /**
     * Gets the instruction that produces the value for this phi in the specified state.
     * @param state the state to access
     * @return the instruction producing the value
     */
    public Value operandIn(FrameState state) {
        if (isLocal()) {
            return state.localAt(localIndex());
        } else {
            return state.stackAt(stackIndex());
        }
    }

    /**
     * Get the number of operands to this phi (i.e. the number of predecessors to the
     * join block).
     * @return the number of operands in this phi
     */
    public int operandCount() {
        if (block.isExceptionEntry()) {
            return block.exceptionHandlerStates().size();
        } else {
            return block.predecessors().size();
        }
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to dispatch to
     */
    @Override
    public void accept(ValueVisitor v) {
        v.visitPhi(this);
    }

    /**
     * Make this phi illegal if types were not merged correctly.
     */
    public void makeDead() {
        setFlag(Flag.PhiCannotSimplify);
        setFlag(Flag.PhiDead);
    }
}

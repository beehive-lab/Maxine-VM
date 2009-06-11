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

import com.sun.c1x.util.InstructionVisitor;
import com.sun.c1x.util.InstructionClosure;
import com.sun.c1x.ci.CiMethod;
import com.sun.c1x.value.ValueStack;
import com.sun.c1x.value.ValueType;

/**
 * The <code>If</code> instruction represents a branch that can go one of two directions
 * depending on the outcome of a comparison.
 *
 * @author Ben L. Titzer
 */
public class If extends BlockEnd {

    Instruction _x;
    Instruction _y;
    Condition _condition;
    CiMethod _profiledMethod;
    int _profiledBCI;

    /**
     * Constructs a new If instruction.
     * @param x the instruction producing the first input to the instruction
     * @param cond the condition (comparison operation)
     * @param unorderedIsTrue <code>true</code> if unordered is treated as true (floating point operations)
     * @param y the instruction that produces the second input to this instruction
     * @param trueSucc the block representing the true successor
     * @param falseSucc the block representing the false successor
     * @param stateBefore the state before the branch
     * @param isSafepoint <code>true</code> if this branch should be considered a safepoint
     */
    public If(Instruction x, Condition cond, boolean unorderedIsTrue, Instruction y,
              BlockBegin trueSucc, BlockBegin falseSucc, ValueStack stateBefore, boolean isSafepoint) {
        super(ValueType.ILLEGAL_TYPE, stateBefore, isSafepoint);
        _x = x;
        _y = y;
        _condition = cond;
        assert x.typeCheck(y);
        setFlag(Flag.UnorderedIsTrue, unorderedIsTrue);
        _successors.add(trueSucc);
        _successors.add(falseSucc);
    }

    /**
     * Gets the instruction that produces the first input to this comparison.
     * @return the instruction producing the first input
     */
    public Instruction x() {
        return _x;
    }

    /**
     * Gets the instruction that produces the second input to this comparison.
     * @return the instruction producing the second input
     */
    public Instruction y() {
        return _y;
    }

    /**
     * Gets the condition (comparison operation) for this instruction.
     * @return the condition
     */
    public Condition condition() {
        return _condition;
    }

    /**
     * Checks whether unordered inputs mean true or false.
     * @return <code>true</code> if unordered inputs produce true
     */
    public boolean unorderedIsTrue() {
        return checkFlag(Flag.UnorderedIsTrue);
    }

    /**
     * Gets the block corresponding to the true successor.
     * @return the true successor
     */
    public BlockBegin trueSuccessor() {
        return _successors.get(0);
    }

    /**
     * Gets the block corresponding to the false successor.
     * @return the false successor
     */
    public BlockBegin falseSuccessor() {
        return _successors.get(1);
    }

    /**
     * Gets the block corresponding to the specified outcome of the branch.
     * @param istrue <code>true</code> if the true successor is requested, <code>false</code> otherwise
     * @return the corresponding successor
     */
    public BlockBegin successor(boolean istrue) {
        return _successors.get(istrue ? 0 : 1);
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
        _condition = _condition.mirror();
        Instruction t = _x;
        _x = _y;
        _y = t;
    }

    /**
     * Swaps the successor blocks to this if and negates the condition (e.g. == goes to !=)
     * @see Condition#negate()
     */
    public void swapSuccessors() {
        setFlag(Flag.UnorderedIsTrue, !unorderedIsTrue());
        _condition = _condition.negate();
        BlockBegin t = _successors.get(0);
        BlockBegin f = _successors.get(1);
        _successors.set(0, f);
        _successors.set(1, t);
    }

    /**
     * Gets the profiled method for this instruction.
     * @return the profiled method
     */
    public CiMethod profiledMethod() {
        return _profiledMethod;
    }

    /**
     * Gets the profiled bytecode index for this instruction.
     * @return the profiled bytecode index
     */
    public int profiledBCI() {
        return _profiledBCI;
    }

    /**
     * Checks whether profiling should be added to this instruction.
     * @return <code>true</code> if profiling should be added to this instruction
     */
    public boolean shouldProfile() {
        return _profiledMethod != null;
    }

    /**
     * Sets the profiled method and bytecode index for this instruction.
     * @param method the profiled method
     * @param bci the bytecode index
     */
    public void setProfile(CiMethod method, int bci) {
        _profiledMethod = method;
        _profiledBCI = bci;
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply
     */
    public void inputValuesDo(InstructionClosure closure) {
        _x = closure.apply(_x);
        _y = closure.apply(_y);
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    public void accept(InstructionVisitor v) {
        v.visitIf(this);
    }
}

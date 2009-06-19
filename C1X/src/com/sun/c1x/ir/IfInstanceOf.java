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
import com.sun.c1x.ci.CiType;
import com.sun.c1x.value.ValueType;

/**
 * The <code>IfInstanceOf</code> instruction represents a branch on the outcome of an instanceof test.
 *
 * @author Ben L. Titzer
 */
public class IfInstanceOf extends BlockEnd {

    final CiType _targetClass;
    Instruction _object;
    boolean _testIsInstance;
    final int _instanceofBCI;

    /**
     * Constructs a new IfInstanceOf instruction.
     * @param targetClass the class to check against
     * @param object the instruction which produces the object value
     * @param testIsInstance <code>true</code> if positive instanceof check implies going to true successor
     * @param instanceofBCI the bytecode index of the instanceof instruction
     * @param trueSucc the block representing the true successor
     * @param falseSucc the block representing the false successor
     */
    public IfInstanceOf(CiType targetClass, Instruction object, boolean testIsInstance, int instanceofBCI, BlockBegin trueSucc, BlockBegin falseSucc) {
        super(ValueType.ILLEGAL_TYPE, null, false); // XXX: why don't we need the state before??
        _targetClass = targetClass;
        _object = object;
        _testIsInstance = testIsInstance;
        _instanceofBCI = instanceofBCI;
        _successors.add(trueSucc);
        _successors.add(falseSucc);
    }

    /**
     * Gets the target class of the instanceof operation in this branch.
     * @return the target class
     */
    public CiType targetClass() {
        return _targetClass;
    }

    /**
     * Gets the instruction that produces the object that is input to the instanceof.
     * @return the instruction producing the object
     */
    public Instruction object() {
        return _object;
    }

    /**
     * Gets the bytecode index of the instanceof bytecode.
     * @return the bytecode index
     */
    public int instanceofBCI() {
        return _instanceofBCI;
    }

    /**
     * Checks whether this instruction succeeds if the object is or is not an instance of the target class.
     * @return <code>true</code> if a successful instanceof implies going to the true successor
     */
    public boolean testIsInstance() {
        return _testIsInstance;
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
     * Swaps the successor blocks to this if and negates the condition (e.g. == goes to !=)
     * @see Condition#negate()
     */
    public void swapSuccessors() {
        BlockBegin t = _successors.get(0);
        BlockBegin f = _successors.get(1);
        _successors.set(0, f);
        _successors.set(1, t);
        _testIsInstance = !_testIsInstance;
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply
     */
    @Override
    public void inputValuesDo(InstructionClosure closure) {
        _object = closure.apply(_object);
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(InstructionVisitor v) {
        v.visitIfInstanceOf(this);
    }
}

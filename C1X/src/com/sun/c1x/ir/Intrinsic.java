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
import com.sun.c1x.value.ValueStack;
import com.sun.c1x.value.ValueType;
import com.sun.c1x.C1XIntrinsic;

/**
 * The <code>Intrinsic</code> instruction represents a call to a JDK method that has been intrinsified,
 * e.g. some math operations.
 *
 * @author Ben L. Titzer
 */
public class Intrinsic extends StateSplit {

    final C1XIntrinsic _intrinsic;
    final boolean _hasReceiver;
    Instruction[] _arguments;
    ValueStack _lockStack;

    /**
     * Creates a new Intrinsic instruction.
     * @param type the result type of the instruction
     * @param intrinsic the actual intrinsic
     * @param arguments the arguments to the call (including the receiver object)
     * @param hasReceiver <code>true</code> if this method takes a receiver object
     * @param lockStack the lock stack
     * @param preservesState <code>true</code> if the implementation of this intrinsic preserves register state
     * @param canTrap <code>true</code> if this intrinsic can cause a trap
     */
    public Intrinsic(ValueType type, C1XIntrinsic intrinsic, Instruction[] arguments, boolean hasReceiver,
                     ValueStack lockStack, boolean preservesState, boolean canTrap) {
        super(type);
        _intrinsic = intrinsic;
        _arguments = arguments;
        _lockStack = lockStack;
        _hasReceiver = hasReceiver;
        // Preserves state means that the intrinsic preserves register state across all cases,
        // including slow cases--even if it causes a trap. If so, it can still be a candidate
        // for load elimination and common subexpression elimination
        setFlag(Flag.PreservesState, preservesState);
        setFlag(Flag.CanTrap, canTrap);
        if (!canTrap) {
            // some intrinsics cannot trap, so unpin them
            unpin(PinReason.PinStateSplitConstructor);
        }
    }

    /**
     * Gets the intrinsic represented by this instruction.
     * @return the intrinsic
     */
    public C1XIntrinsic intrinsic() {
        return _intrinsic;
    }

    /**
     * Gets the list of instructions that produce input for this instruction.
     * @return the list of instructions that produce input
     */
    public Instruction[] arguments() {
        return _arguments;
    }

    /**
     * Gets the lock stack for this instruction.
     * @return the lock stack
     */
    public ValueStack lockStack() {
        return _lockStack;
    }

    /**
     * Checks whether this intrinsic has a receiver object.
     * @return <code>true</code> if this intrinsic has a receiver object
     */
    public boolean hasReceiver() {
        return _hasReceiver;
    }

    /**
     * Gets the instruction which produces the receiver object for this intrinsic.
     * @return the instruction producing the receiver object
     */
    public Instruction receiver() {
        assert _hasReceiver;
        return _arguments[0];
    }

    /**
     * Checks whether this intrinsic preserves the state of registers across all cases.
     * @return <code>true</code> if this intrinsic always preserves register state
     */
    public boolean preservesState() {
        return checkFlag(Flag.PreservesState);
    }

    /**
     * Checks whether this intrinsic can cause a trap.
     * @return <code>true</code> if this intrinsic can cause a trap
     */
    public boolean canTrap() {
        return checkFlag(Flag.CanTrap);
    }

    /**
     * Iterates over the state values of this instruction.
     * @param closure the closure to apply
     */
    public void stateValuesDo(InstructionClosure closure) {
        if (_lockStack != null) {
            _lockStack.valuesDo(closure);
        }
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply
     */
    public void inputValuesDo(InstructionClosure closure) {
        for (int i = 0; i < _arguments.length; i++) {
            _arguments[i] = closure.apply(_arguments[i]);
        }
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    public void accept(InstructionVisitor v) {
        v.visitIntrinsic(this);
    }
}

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

import com.sun.c1x.value.*;

/**
 * The <code>Throw</code> instruction represents a throw of an exception.
 *
 * @author Ben L. Titzer
 */
public class Throw extends BlockEnd {

    Instruction exception;

    /**
     * Creates a new Throw instruction.
     * @param exception the instruction that generates the exception to throw
     * @param stateBefore the state before the exception is thrown
     */
    public Throw(Instruction exception, ValueStack stateBefore) {
        super(BasicType.Illegal, stateBefore, true);
        this.exception = exception;
    }

    /**
     * Gets the instruction which produces the exception to throw.
     * @return the instruction producing the exception
     */
    public Instruction exception() {
        return exception;
    }

    /**
     * Checks whether this instruction can trap.
     * @return <code>true</code> because this instruction definitely throws an exception!
     */
    @Override
    public boolean canTrap() {
        return true;
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply
     */
    @Override
    public void inputValuesDo(InstructionClosure closure) {
        exception = closure.apply(exception);
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(InstructionVisitor v) {
        v.visitThrow(this);
    }
}

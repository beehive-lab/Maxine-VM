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
import com.sun.c1x.bytecode.Bytecodes;

/**
 * The <code>RoundFP</code> class instruction is used for rounding on Intel.
 *
 * @author Ben L. Titzer
 */
public class RoundFP extends Instruction {

    Instruction _value;

    /**
     * Creates a new RoundFP instruction.
      * @param value the instruction generating the input value
     */
    public RoundFP(Instruction value) {
        super(value.type());
        _value = value;
    }

    /**
     * Gets the instruction producing the input value to this round instruction.
     * @return the instruction that generates the input value
     */
    public Instruction value() {
        return _value;
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply
     */
    public void inputValuesDo(InstructionClosure closure) {
        _value = closure.apply(_value);
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    public void accept(InstructionVisitor v) {
        v.visitRoundFP(this);
    }

    @Override
    public int valueNumber() {
        return hash1(Bytecodes.D2F, _value); // just use d2f for the hash code
    }

    @Override
    public boolean valueEqual(Instruction i) {
        if (i instanceof RoundFP) {
            RoundFP o = (RoundFP) i;
            return _value == o._value;
        }
        return false;
    }

}

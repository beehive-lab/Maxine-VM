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
import com.sun.c1x.value.ValueType;

/**
 * The <code>ProfileCounter</code> instruction represents instrumentation inserted by the compiler
 * that increments a counter each time executed.
 *
 * @author Ben L. Titzer
 */
public class ProfileCounter extends Instruction {

    Instruction _mdo;
    final int _offset;
    final int _increment;

    /**
     * Creates a new ProfileCounter instruction.
     * @param mdo the instruction that generates the method data object
     * @param offset the offset into the method data object
     * @param increment the increment by which to increase the counter
     */
    public ProfileCounter(Instruction mdo, int offset, int increment) {
        super(ValueType.VOID_TYPE);
        _mdo = mdo;
        _offset = offset;
        _increment = increment;
        pin();
    }

    /**
     * Gets the instruction which produces the method data object.
     * @return the instruction generating the mdo
     */
    public Instruction mdo() {
        return _mdo;
    }

    /**
     * Gets the offset in the method data object where the counter resides.
     * @return the offset into the method data object
     */
    public int offset() {
        return _offset;
    }

    /**
     * Gets the increment that is added to the counter for each execution.
     * @return the increment
     */
    public int increment() {
        return _increment;
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply
     */
    @Override
    public void inputValuesDo(InstructionClosure closure) {
        _mdo = closure.apply(_mdo);
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(InstructionVisitor v) {
        v.visitProfileCounter(this);
    }
}

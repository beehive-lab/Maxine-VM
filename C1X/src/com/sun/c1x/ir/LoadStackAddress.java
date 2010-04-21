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

import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;

/**
 * Instruction implementing the semantics of {@link Bytecodes#LSA}.
 *
 * @author Doug Simon
 */
public final class LoadStackAddress extends Instruction {

    private Value value;

    /**
     * Creates a new LoadStackAddress instance.
     */
    public LoadStackAddress(Value value) {
        super(CiKind.Word);
        this.value = value;
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(ValueVisitor v) {
        v.visitLoadStackAddress(this);
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply to each instruction
     */
    @Override
    public void inputValuesDo(ValueClosure closure) {
        value = closure.apply(value);
    }

    /**
     * Gets the instruction that produced the size argument.
     */
    public Value value() {
        return value;
    }
}

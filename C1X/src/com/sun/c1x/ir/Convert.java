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

/**
 * The {@code Convert} class represents a conversion between primitive types.
 *
 * @author Ben L. Titzer
 */
public final class Convert extends Instruction {

    final int opcode;
    Value value;

    /**
     * Constructs a new Convert instance.
     * @param opcode the bytecode representing the operation
     * @param value the instruction producing the input value
     * @param kind the result type of this instruction
     */
    public Convert(int opcode, Value value, CiKind kind) {
        super(kind);
        this.opcode = opcode;
        this.value = value;
    }

    /**
     * Gets the opcode for this conversion operation.
     * @return the opcode of this conversion operation
     */
    public int opcode() {
        return opcode;
    }

    /**
     * Gets the instruction which produces the input value to this instruction.
     * @return the input value instruction
     */
    public Value value() {
        return value;
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply to each input value
     */
    @Override
    public void inputValuesDo(ValueClosure closure) {
        value = closure.apply(value);
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(ValueVisitor v) {
        v.visitConvert(this);
    }

    @Override
    public int valueNumber() {
        return Util.hash1(opcode, value);
    }

    @Override
    public boolean valueEqual(Instruction i) {
        if (i instanceof Convert) {
            Convert o = (Convert) i;
            return opcode == o.opcode && value == o.value;
        }
        return false;
    }
}

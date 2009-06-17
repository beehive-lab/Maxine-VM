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
 * The <code>Convert</code> class represents a conversion between primitive types.
 *
 * @author Ben L. Titzer
 */
public class Convert extends Instruction {

    final int _opcode;
    Instruction _value;

    /**
     * Constructs a new Convert instance.
     * @param opcode the bytecode representing the operation
     * @param value the instruction producing the input value
     * @param type the result type of this instruction
     */
    public Convert(int opcode, Instruction value, ValueType type) {
        super(type);
        _opcode = opcode;
        _value = value;
    }

    /**
     * Gets the opcode for this conversion operation.
     * @return the opcode of this conversion operation
     */
    public int opcode() {
        return _opcode;
    }

    /**
     * Gets the instruction which produces the input value to this instruction.
     * @return the input value instruction
     */
    public Instruction value() {
        return _value;
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply to each input value
     */
    @Override
    public void inputValuesDo(InstructionClosure closure) {
        _value = closure.apply(_value);
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(InstructionVisitor v) {
        v.visitConvert(this);
    }

    @Override
    public int valueNumber() {
        return hash1(_opcode, _value);
    }

    @Override
    public boolean valueEqual(Instruction i) {
        if (i instanceof Convert) {
            Convert o = (Convert) i;
            return _opcode == o._opcode && _value == o._value;
        }
        return false;
    }
}

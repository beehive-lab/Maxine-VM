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

import com.sun.c1x.bytecode.*;

/**
 * The <code>LogicOp</code> class definition.
 *
 * @author Ben L. Titzer
 */
public class LogicOp extends Op2 {

    /**
     * Constructs a new logic operation instruction.
     * @param opcode the opcode of the logic operation
     * @param x the first input into this instruction
     * @param s the second input into this instruction
     */
    public LogicOp(int opcode, Instruction x, Instruction s) {
        super(x.type().base(), opcode, x, s);
    }

    /**
     * Checks whether this operation is commutative.
     * @return <code>true</code> if this operation is commutative
     */
    public boolean isCommutative() {
        return Bytecodes.isCommutative(opcode);
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(InstructionVisitor v) {
        v.visitLogicOp(this);
    }
}

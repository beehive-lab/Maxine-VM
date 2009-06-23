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
import com.sun.c1x.value.ValueStack;
import com.sun.c1x.value.ValueType;

/**
 * The <code>Goto</code> instruction represents the end of a block with an unconditional jump to another block.
 *
 * @author Ben L. Titzer
 */
public class Goto extends BlockEnd {

    /**
     * Constructs a new Goto instruction.
     * @param succ the successor block of the goto
     * @param stateBefore the state before the goto
     * @param isSafepoint <code>true</code> if the goto should be considered a safepoint (e.g. backward branch)
     */
    public Goto(BlockBegin succ, ValueStack stateBefore, boolean isSafepoint) {
        super(ValueType.ILLEGAL_TYPE, stateBefore, isSafepoint);
        successors.add(succ);
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(InstructionVisitor v) {
        v.visitGoto(this);
    }
}

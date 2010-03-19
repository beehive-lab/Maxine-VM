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
import com.sun.c1x.value.*;

/**
 * The {@code WCompareOp} instruction represents comparisons between word types,
 * for the {@code UWLT} etc., bytecodes. These have different result semantics to the standard compare ops.
 *
 * @author Mick Jordan
 */
public final class WCompareOp extends Op2 {

    FrameState stateBefore;

    /**
     * Creates a new compare operation.
     * @param opcode the bytecode opcode
     * @param x the first input
     * @param y the second input
     * @param stateBefore the state before the comparison is performed
     */
    public WCompareOp(int opcode, Value x, Value y, FrameState stateBefore) {
        super(CiKind.Int, opcode, x, y);
        this.stateBefore = stateBefore;
    }

    /**
     * Gets the value stack representing the state before the comparison is performed.
     * @return the state before the comparison is performed
     */
    @Override
    public FrameState stateBefore() {
        return stateBefore;
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(ValueVisitor v) {
        v.visitWCompareOp(this);
    }
}

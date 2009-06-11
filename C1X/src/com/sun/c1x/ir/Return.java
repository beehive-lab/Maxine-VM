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
 * The <code>Return</code> class definition.
 *
 * @author Ben L. Titzer
 */
public class Return extends BlockEnd {

    Instruction _result;

    /**
     * Constructs a new Return instruction.
     * @param result the instruction producing the result for this return; <code>null</code> if this
     * is a void return
     */
    public Return(Instruction result) {
        super(result == null ? ValueType.VOID_TYPE : result.type(), null, true);
        _result = result;
    }

    /**
     * Gets the instruction that produces the result for the return.
     * @return the instruction producing the result
     */
    public Instruction result() {
        return _result;
    }

    /**
     * Checks whether this return returns a result.
     * @return <code>true</code> if this instruction returns a result
     */
    public boolean hasResult() {
        return _result != null;
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply
     */
    public void inputValuesDo(InstructionClosure closure) {
        _result = closure.apply(_result);
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    public void accept(InstructionVisitor v) {
        v.visitReturn(this);
    }
}

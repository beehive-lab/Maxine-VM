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
import com.sun.c1x.ci.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;

/**
 * The <code>ArrayLength</code> instruction gets the length of an array.
 *
 * @author Ben L. Titzer
 */
public final class ArrayLength extends AccessArray {

    /**
     * Constructs a new ArrayLength instruction.
     * @param array the instruction producing the array
     * @param stateBefore the state before executing this instruction
     */
    public ArrayLength(Value array, ValueStack stateBefore) {
        super(CiKind.Int, array, stateBefore);
        setFlag(Flag.NoBoundsCheck);
        if (array.isNonNull()) {
            redundantNullCheck();
        }
    }

    /**
     * Gets the object representing an explicit null check for this instruction.
     * @return the explicit null check object
     */
    public NullCheck explicitNullCheck() {
        return null;
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(ValueVisitor v) {
        v.visitArrayLength(this);
    }

    @Override
    public int valueNumber() {
        return Util.hash1(Bytecodes.ARRAYLENGTH, array);
    }

    @Override
    public boolean valueEqual(Instruction i) {
        if (i instanceof ArrayLength) {
            ArrayLength o = (ArrayLength) i;
            return array == o.array;
        }
        return false;
    }
}

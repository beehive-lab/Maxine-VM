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

import com.sun.c1x.ci.CiType;
import com.sun.c1x.value.ValueStack;
import com.sun.c1x.value.BasicType;
import com.sun.c1x.util.InstructionVisitor;

/**
 * The <code>LoadIndexed</code> instruction represents a read from an element of an array.
 *
 * @author Ben L. Titzer
 */
public class LoadIndexed extends AccessIndexed {

    NullCheck _explicitNullCheck;

    /**
     * Creates a new LoadIndexed instruction.
     * @param array the instruction producing the array
     * @param index the instruction producing the index
     * @param length the instruction producing the length
     * @param elementType the element type
     * @param lockStack the lock stack
     */
    public LoadIndexed(Instruction array, Instruction index, Instruction length, BasicType elementType, ValueStack lockStack) {
        super(array, index, length, elementType, lockStack);
    }

    /**
     * Gets the instruction representing an explicit null check for this instruction.
     * @return the explicit null check instruction
     */
    public Object explicitNullCheck() {
        return _explicitNullCheck;
    }

    /**
     * Sets the instruction representing an explicit null check for this instruction.
     * @param explicitNullCheck the object
     */
    public void setExplicitNullCheck(NullCheck explicitNullCheck) {
        _explicitNullCheck = explicitNullCheck;
    }

    /**
     * Gets the declared type of this instruction's result.
     * @return the declared type
     */
    public CiType declaredType() {
        CiType arrayType = array().declaredType();
        if (arrayType == null) {
            return null;
        }
        return arrayType.elementType();
    }

    /**
     * Gets the exact type of this instruction's result.
     * @return the exact type
     */
    public CiType exactType() {
        CiType declared = declaredType();
        return declared != null ? declared.exactType() : null;
    }

    public void accept(InstructionVisitor v) {
        v.visitLoadIndexed(this);
    }

    @Override
    public int valueNumber() {
        return hash2(124, _array, _index);
    }

    @Override
    public boolean valueEqual(Instruction i) {
        if (i instanceof LoadIndexed) {
            LoadIndexed o = (LoadIndexed) i;
            return _array == o._array && _index == o._index;
        }
        return false;
    }

}

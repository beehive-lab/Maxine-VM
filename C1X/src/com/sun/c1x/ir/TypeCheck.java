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
 * The <code>TypeCheck</code> instruction is the base class of casts and instanceof tests.
 *
 * @author Ben L. Titzer
 */
public abstract class TypeCheck extends StateSplit {

    final RiType targetClass;
    Instruction object;

    /**
     * Creates a new TypeCheck instruction.
     * @param targetClass the class which is being casted to or checked against
     * @param object the instruction which produces the object
     * @param type the result type of this instruction
     * @param stateBefore the state before this instruction is executed
     */
    public TypeCheck(RiType targetClass, Instruction object, BasicType type, ValueStack stateBefore) {
        super(type, stateBefore);
        this.targetClass = targetClass;
        this.object = object;
        this.stateBefore = stateBefore;
    }

    /**
     * Gets the target class, i.e. the class being cast to, or the class being tested against.
     * @return the target class
     */
    public RiType targetClass() {
        return targetClass;
    }

    /**
     * Gets the instruction which produces the object input.
     * @return the instruction producing the object
     */
    public Instruction object() {
        return object;
    }

    /**
     * Checks whether the target class of this instruction is loaded.
     * @return <code>true</code> if the target class is loaded
     */
    public boolean isLoaded() {
        return targetClass != null;
    }

    /**
     * Checks whether this instruction is a direct compare.
     * @return <code>true</code> if this cast or check is a direct compare
     */
    public boolean directCompare() {
        // XXX: what does direct compare mean? leaf class?
        return checkFlag(Flag.DirectCompare);
    }

    /**
     * Checks whether this instruction can trap.
     * @return <code>true</code>, conservatively assuming the cast may fail
     */
    @Override
    public boolean canTrap() {
        return true;
    }

    @Override
    public boolean internalClearNullCheck() {
        return true;
    }
    
    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply
     */
    @Override
    public void inputValuesDo(InstructionClosure closure) {
        object = closure.apply(object);
    }

    /**
     * Sets this type check operation to be a direct compare.
     */
    public void setDirectCompare() {
        setFlag(Flag.DirectCompare);
    }

    /**
     * Checks where this comparison is a direct compare.
     * @return <code>true</code> if this typecheck is a direct compare
     */
    public boolean isDirectCompare() {
        return checkFlag(Flag.DirectCompare);
    }
}

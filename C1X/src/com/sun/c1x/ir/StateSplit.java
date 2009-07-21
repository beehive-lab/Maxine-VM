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

import com.sun.c1x.util.*;
import com.sun.c1x.value.*;

/**
 * The <code>StateSplit</code> class is the abstract base class of all instructions
 * that store a copy of the value stack state.
 *
 * @author Ben L. Titzer
 */
public abstract class StateSplit extends Instruction {

    private ValueStack state;

    /**
     * Creates a new state split with the specified value type.
     * @param type the type of the value that this instruction produces
     */
    public StateSplit(ValueType type) {
        super(type);
    }

    /**
     * Sets the state for this instruction.
     * @param state the state
     */
    public void setState(ValueStack state) {
        this.state = state;
    }

    /**
     * Gets the state for this instruction.
     * @return the state
     */
    public ValueStack state() {
        return state;
    }

    /**
     * Gets the IR scope for this instruction.
     * @return the IR scope
     */
    public IRScope scope() {
        return state.scope();
    }

    /**
     * Iterates over the values in this instruction. This implementation
     * iterates over all the values in the value stack.
     * @param closure the closure to apply to each value
     */
    @Override
    public void stateValuesDo(InstructionClosure closure) {
        if (state != null) {
            state.valuesDo(closure);
        }
    }
}

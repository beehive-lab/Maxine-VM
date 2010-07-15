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

import com.sun.c1x.value.*;
import com.sun.cri.ci.*;

/**
 * The {@code StateSplit} class is the abstract base class of all instructions
 * that store a copy of the value stack state.
 *
 * @author Ben L. Titzer
 */
public abstract class StateSplit extends Instruction {

    protected NewFrameState stateBefore;

    /**
     * Creates a new state split with the specified value type.
     * @param kind the type of the value that this instruction produces
     */
    public StateSplit(CiKind kind, NewFrameState stateBefore2) {
        super(kind);
        this.stateBefore = stateBefore2;
    }

    /**
     * Sets the state after this instruction has executed.
     * @param stateBefore the state
     */
    public void setStateBefore(NewFrameState stateBefore) {
        this.stateBefore = stateBefore;
    }

    /**
     * Gets the state for this instruction.
     * @return the state
     */
    @Override
    public NewFrameState stateBefore() {
        return stateBefore;
    }
}

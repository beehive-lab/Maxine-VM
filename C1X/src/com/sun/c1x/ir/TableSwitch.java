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

import com.sun.c1x.value.ValueStack;

import java.util.List;

/**
 * The <code>TableSwitch</code> instruction represents a table switch.
 *
 * @author Ben L. Titzer
 */
public class TableSwitch extends Switch {

    final int lowKey;

    /**
     * Constructs a new TableSwitch instruction.
     * @param value the instruction producing the value being switched on
     * @param successors the list of successors
     * @param lowKey the lowest integer key in the table
     * @param stateBefore the state before the switch
     * @param isSafepoint <code>true</code> if this instruction is a safepoint
     */
    public TableSwitch(Instruction value, List<BlockBegin> successors, int lowKey, ValueStack stateBefore, boolean isSafepoint) {
        super(value, successors, stateBefore, isSafepoint);
        this.lowKey = lowKey;
    }

    /**
     * Gets the lowest key in the table switch (inclusive).
     * @return the low key
     */
    public int lowKey() {
        return lowKey;
    }

    /**
     * Gets the highest key in the table switch (exclusive).
     * @return the high key
     */
    public int highKey() {
        return lowKey + numberOfCases();
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(InstructionVisitor v) {
        v.visitTableSwitch(this);
    }
}

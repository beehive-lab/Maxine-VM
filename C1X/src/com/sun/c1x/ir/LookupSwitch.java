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

import java.util.*;

import com.sun.c1x.util.*;
import com.sun.c1x.value.*;

/**
 * The <code>LookupSwitch</code> instruction represents a lookup switch bytecode, which has a sorted
 * array of key values.
 *
 * @author Ben L. Titzer
 */
public class LookupSwitch extends Switch {

    final int[] keys;

    /**
     * Constructs a new TableSwitch instruction.
     * @param value the instruction producing the value being switched on
     * @param successors the list of successors
     * @param keys the list of keys, sorted
     * @param stateBefore the state before the switch
     * @param isSafepoint <code>true</code> if this instruction is a safepoint
     */
    public LookupSwitch(Instruction value, List<BlockBegin> successors, int[] keys, ValueStack stateBefore, boolean isSafepoint) {
        super(value, successors, stateBefore, isSafepoint);
        this.keys = keys;
    }

    /**
     * Gets the key at the specified index.
     * @param i the index
     * @return the key at that index
     */
    public int keyAt(int i) {
        return keys[i];
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(InstructionVisitor v) {
        v.visitLookupSwitch(this);
    }
}

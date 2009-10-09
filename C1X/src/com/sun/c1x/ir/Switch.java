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

import com.sun.c1x.ci.*;
import com.sun.c1x.value.*;

/**
 * The <code>Switch</code> class is the base of both lookup and table switches.
 *
 * @author Ben L. Titzer
 */
public abstract class Switch extends BlockEnd {

    Value value;

    /**
     * Constructs a new Switch.
     * @param value the instruction that provides the value to be switched over
     * @param successors the list of successors of this switch
     * @param stateBefore the state before the switch
     * @param isSafepoint <code>true</code> if this switch is a safepoint
     */
    public Switch(Value value, List<BlockBegin> successors, ValueStack stateBefore, boolean isSafepoint) {
        super(CiKind.Illegal, stateBefore, isSafepoint);
        this.successors = successors;
        this.value = value;
    }

    /**
     * Gets the instruction that provides the input value to this switch.
     * @return the instruction producing the input value
     */
    public Value value() {
        return value;
    }

    /**
     * Gets the number of cases that this switch covers (excluding the default case).
     * @return the number of cases
     */
    public int numberOfCases() {
        return successors.size() - 1;
    }

    /**
     * Iterates over the inputs to this instruction.
     * @param closure the closure to apply
     */
    @Override
    public void inputValuesDo(ValueClosure closure) {
        value = closure.apply(value);
    }
}

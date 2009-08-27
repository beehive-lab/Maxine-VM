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
 * The <code>AccessMonitor</code> instruction is the base class of both monitor acquisition and release.
 *
 * @author Ben L. Titzer
 */
public abstract class AccessMonitor extends StateSplit {

    Instruction object;
    final int lockNumber;

    /**
     * Creates a new AccessMonitor instruction.
     * @param object the instruction producing the object
     * @param stateBefore the state before executing the monitor operation
     * @param lockNumber the number of the lock being acquired
     */
    public AccessMonitor(Instruction object, ValueStack stateBefore, int lockNumber) {
        super(CiKind.Illegal, stateBefore);
        this.object = object;
        this.lockNumber = lockNumber;
    }

    /**
     * Gets the instruction producing the object input to this instruction.
     * @return the instruction producing the object
     */
    public Instruction object() {
        return object;
    }

    /**
     * Gets the lock number of this monitor access.
     * @return the lock number
     */
    public int lockNumber() {
        return lockNumber;
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply
     */
    @Override
    public void inputValuesDo(InstructionClosure closure) {
        object = closure.apply(object);
    }

    @Override
    public boolean internalClearNullCheck() {
        return true;
    }
}
